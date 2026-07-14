package de.tudresden.fgdh.querybroker.broker;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one federated query: validates the request against the
 * catalog, fans it out on the broadcast exchange, aggregates the connector
 * responses, and publishes the aggregated bundle to the requesting system's
 * response queue (ADR-009).
 */
@Service
public class QueryBrokerService {

  private static final Logger log = LoggerFactory.getLogger(QueryBrokerService.class);

  private final FhirContext fhirContext;
  private final MessageDefinitionRegistry registry;
  private final ResponseAggregator aggregator;
  private final RabbitTemplate rabbitTemplate;
  private final BrokerProperties properties;

  public QueryBrokerService(
      FhirContext fhirContext,
      MessageDefinitionRegistry registry,
      ResponseAggregator aggregator,
      RabbitTemplate rabbitTemplate,
      BrokerProperties properties) {
    this.fhirContext = fhirContext;
    this.registry = registry;
    this.aggregator = aggregator;
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public Bundle process(Bundle requestBundle) throws InterruptedException {
    MessageHeader requestHeader =
        BrokerMessages.messageHeaderOf(requestBundle)
            .orElseThrow(() -> new BadRequestException("Request bundle has no MessageHeader"));
    if (requestHeader.hasResponse()) {
      throw new BadRequestException("Request MessageHeader must not have a response element");
    }
    Parameters parameters =
        BrokerMessages.parametersOf(requestBundle)
            .orElseThrow(() -> new BadRequestException("Request bundle has no Parameters"));

    String eventUri = requestHeader.getEventUriType().getValue();
    registry
        .findOperation(eventUri)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "Unknown operation (not in catalog): " + eventUri,
                    ErrorCode.UNSUPPORTED_OPERATION));

    // Expected responders = distinct pseudonym domains (broadcast + self-filtering).
    Set<String> domains = new LinkedHashSet<>();
    BrokerMessages.pseudonymsOf(parameters).forEach(id -> domains.add(id.getSystem()));
    if (domains.isEmpty()) {
      throw new BadRequestException("Request Parameters contain no pseudonym");
    }

    if (requestHeader.getIdElement().isEmpty()) {
      requestHeader.setId(UUID.randomUUID().toString());
    }
    String correlationId = requestHeader.getIdElement().getIdPart();

    aggregator.expect(correlationId, domains.size());
    publish(
        BrokerProtocol.BROADCAST_EXCHANGE,
        "",
        requestBundle,
        correlationId,
        properties.replyQueue());
    log.info(
        "Published {} (correlationId={}, expecting {} response(s))",
        eventUri,
        correlationId,
        domains.size());

    List<Bundle> responses = aggregator.await(correlationId, properties.aggregatorTimeoutMs());
    Bundle aggregated = aggregate(requestHeader, domains.size(), responses);

    String responseQueue = responseQueueOf(requestHeader);
    publish("", responseQueue, aggregated, correlationId, null);
    log.info(
        "Aggregated {} response(s) for {} -> {}", responses.size(), correlationId, responseQueue);
    return aggregated;
  }

  Bundle aggregate(MessageHeader requestHeader, int expected, List<Bundle> responses) {
    List<Resource> payload = new java.util.ArrayList<>();
    for (Bundle response : responses) {
      response.getEntry().stream()
          .map(Bundle.BundleEntryComponent::getResource)
          .filter(r -> !(r instanceof MessageHeader))
          .forEach(payload::add);
    }

    boolean anyOk =
        responses.stream()
            .map(BrokerMessages::messageHeaderOf)
            .flatMap(java.util.Optional::stream)
            .anyMatch(h -> h.getResponse().getCode() == ResponseType.OK);

    ResponseType code;
    if (responses.isEmpty()) {
      code = ResponseType.FATALERROR;
      payload.add(
          BrokerMessages.operationOutcome(
              IssueSeverity.FATAL,
              IssueType.TIMEOUT,
              ErrorCode.TIMEOUT,
              "No addressed PDS responded within "
                  + properties.aggregatorTimeoutMs()
                  + " ms; the request produced no result."));
    } else if (!anyOk) {
      // Every responding PDS failed — an ok would misrepresent the result.
      code = ResponseType.FATALERROR;
    } else {
      code = ResponseType.OK;
      if (responses.size() < expected) {
        payload.add(
            BrokerMessages.operationOutcome(
                IssueSeverity.WARNING,
                IssueType.TIMEOUT,
                ErrorCode.TIMEOUT,
                (expected - responses.size())
                    + " of "
                    + expected
                    + " addressed PDS did not respond within "
                    + properties.aggregatorTimeoutMs()
                    + " ms; the aggregated result is potentially incomplete."));
      }
    }

    // responseBundle() already applies the OperationError event semantics for FATALERROR.
    return BrokerMessages.responseBundle(
        requestHeader, "Query Broker", brokerEndpoint(), code, payload);
  }

  private void publish(
      String exchange, String routingKey, Bundle bundle, String correlationId, String replyTo) {
    String json = fhirContext.newJsonParser().encodeResourceToString(bundle);
    org.springframework.amqp.core.Message message =
        MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
            .setContentType(BrokerProtocol.FHIR_JSON_CONTENT_TYPE)
            .setCorrelationId(correlationId)
            .setReplyTo(replyTo)
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
    rabbitTemplate.send(exchange, routingKey, message);
  }

  /** Response queue = last segment of MessageHeader.destination.endpoint (ADR-009). */
  static String responseQueueOf(MessageHeader requestHeader) {
    return requestHeader.getDestination().stream()
        .filter(d -> d.hasEndpoint())
        .map(d -> d.getEndpoint().substring(d.getEndpoint().lastIndexOf('/') + 1))
        .filter(q -> q.startsWith(BrokerProtocol.RESPONSE_QUEUE_PREFIX))
        .findFirst()
        .orElse(BrokerProtocol.DEFAULT_RESPONSE_QUEUE);
  }

  private String brokerEndpoint() {
    return "amqp://rabbitmq/" + BrokerProtocol.BROADCAST_EXCHANGE;
  }

  /** 400-level protocol violation, mapped by {@link MessagingController}. */
  public static final class BadRequestException extends RuntimeException {
    private final ErrorCode errorCode;

    public BadRequestException(String message) {
      this(message, ErrorCode.VALIDATION_ERROR);
    }

    public BadRequestException(String message, ErrorCode errorCode) {
      super(message);
      this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
      return errorCode;
    }
  }
}
