package de.tudresden.fgdh.querybroker.pdsexample;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes request bundles from this connector's queue (req.{pdsId}) and
 * publishes the response to the AMQP replyTo queue, echoing the
 * correlationId. Self-filtered requests produce no response (fanout +
 * self-filtering, ADR-006).
 */
@Component
public class RequestListener {

  private static final Logger log = LoggerFactory.getLogger(RequestListener.class);

  private final ExampleConnector connector;
  private final RabbitTemplate rabbitTemplate;
  private final FhirContext fhirContext;

  public RequestListener(
      ExampleConnector connector, RabbitTemplate rabbitTemplate, FhirContext fhirContext) {
    this.connector = connector;
    this.rabbitTemplate = rabbitTemplate;
    this.fhirContext = fhirContext;
  }

  @RabbitListener(queues = "${pds.connector.request-queue:req.PDS-EXAMPLE}")
  void onRequest(Message message) {
    String replyTo = message.getMessageProperties().getReplyTo();
    String correlationId = message.getMessageProperties().getCorrelationId();
    Bundle request =
        (Bundle)
            fhirContext
                .newJsonParser()
                .parseResource(new String(message.getBody(), StandardCharsets.UTF_8));

    connector
        .handle(request)
        .ifPresentOrElse(
            response -> {
              if (replyTo == null || replyTo.isBlank()) {
                log.warn("{}: request without replyTo, dropping response", connector.getPrimaryDataSourceId());
                return;
              }
              String json = fhirContext.newJsonParser().encodeResourceToString(response);
              Message out =
                  MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                      .setContentType(BrokerProtocol.FHIR_JSON_CONTENT_TYPE)
                      .setCorrelationId(correlationId)
                      .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                      .build();
              rabbitTemplate.send("", replyTo, out);
              log.info("{}: responded to {} via {}", connector.getPrimaryDataSourceId(), correlationId, replyTo);
            },
            () ->
                log.debug(
                    "{}: self-filtered request {} (silent)", connector.getPrimaryDataSourceId(), correlationId));
  }
}
