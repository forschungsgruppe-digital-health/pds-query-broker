package de.tudresden.fgdh.querybroker.broker;

import ca.uhn.fhir.context.FhirContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Collects connector responses from the broker's reply queue and correlates
 * them to pending requests via the AMQP correlationId (= request
 * MessageHeader.id). A request completes when the expected number of
 * responses arrived or the timeout elapsed.
 */
@Component
public class ResponseAggregator {

  private static final Logger log = LoggerFactory.getLogger(ResponseAggregator.class);

  private final FhirContext fhirContext;
  private final Map<String, Pending> pending = new ConcurrentHashMap<>();

  public ResponseAggregator(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }

  /** Registers a request before it is published, to avoid a receive race. */
  public void expect(String correlationId, int expectedResponses) {
    pending.put(correlationId, new Pending(expectedResponses));
  }

  /** Waits for the registered responses; returns whatever arrived in time. */
  public List<Bundle> await(String correlationId, long timeoutMs) throws InterruptedException {
    Pending entry = pending.get(correlationId);
    if (entry == null) {
      return List.of();
    }
    try {
      boolean complete = entry.latch.await(timeoutMs, TimeUnit.MILLISECONDS);
      if (!complete) {
        log.info(
            "Aggregation timeout for {}: {}/{} responses",
            correlationId,
            entry.responses.size(),
            entry.expected);
      }
      return List.copyOf(entry.responses);
    } finally {
      pending.remove(correlationId);
    }
  }

  @RabbitListener(queues = "${broker.reply-queue:responses.broker}")
  void onResponse(Message message) {
    String correlationId = message.getMessageProperties().getCorrelationId();
    Pending entry = correlationId == null ? null : pending.get(correlationId);
    if (entry == null) {
      log.warn("Discarding uncorrelated response (correlationId={})", correlationId);
      return;
    }
    Bundle bundle =
        (Bundle)
            fhirContext
                .newJsonParser()
                .parseResource(new String(message.getBody(), StandardCharsets.UTF_8));
    entry.responses.add(bundle);
    entry.latch.countDown();
  }

  private static final class Pending {
    final int expected;
    final List<Bundle> responses = new CopyOnWriteArrayList<>();
    final CountDownLatch latch;

    Pending(int expected) {
      this.expected = expected;
      this.latch = new CountDownLatch(Math.max(expected, 1));
    }
  }
}
