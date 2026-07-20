package de.tudresden.fgdh.querybroker.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.broker.BrokerProperties.RoutingMode;
import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class QueryBrokerServiceTest {

  private static final FhirContext FHIR = FhirContext.forR4();
  private static final String EXAMPLE_DOMAIN = "https://ths.example.org/gpas/domain/PDS-EXAMPLE";
  private static final String EXAMPLE_B_DOMAIN =
      "https://ths.example.org/gpas/domain/PDS-EXAMPLE-B";

  private final QueryBrokerService service =
      new QueryBrokerService(null, null, null, null, new BrokerProperties(null, 1000, null, null));

  @Test
  void allFatalResponsesAggregateAsFatalError() {
    MessageHeader request = requestHeader();
    Bundle fatalA = connectorResponse(request, ResponseType.FATALERROR);
    Bundle fatalB = connectorResponse(request, ResponseType.FATALERROR);

    Bundle aggregated = service.aggregate(request, 2, List.of(fatalA, fatalB));

    MessageHeader header = BrokerMessages.messageHeaderOf(aggregated).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.FATALERROR);
    assertThat(header.getEventUriType().getValue())
        .isEqualTo(BrokerProtocol.OPERATION_ERROR_EVENT);
  }

  @Test
  void mixedResultsStayOkAndCarryTheFailingSitesOutcomes() {
    MessageHeader request = requestHeader();
    Bundle ok = connectorResponse(request, ResponseType.OK);
    Bundle fatal = connectorResponse(request, ResponseType.FATALERROR);

    Bundle aggregated = service.aggregate(request, 2, List.of(ok, fatal));

    MessageHeader header = BrokerMessages.messageHeaderOf(aggregated).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(
            aggregated.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(OperationOutcome.class::isInstance)
                .count())
        .isEqualTo(1);
  }

  private static MessageHeader requestHeader() {
    MessageHeader header = new MessageHeader();
    header.setId(UUID.randomUUID().toString());
    header.setEvent(
        new org.hl7.fhir.r4.model.UriType(
            "https://querybroker.example.org/fhir/OperationDefinition/GetConditions"));
    header.addDestination().setEndpoint("amqp://rabbitmq/responses.portal");
    header.getSource().setName("test").setEndpoint("amqp://rabbitmq/responses.portal");
    return header;
  }

  private static Bundle connectorResponse(MessageHeader request, ResponseType code) {
    List<org.hl7.fhir.r4.model.Resource> payload =
        code == ResponseType.FATALERROR
            ? List.of(
                BrokerMessages.operationOutcome(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.EXCEPTION,
                    BrokerProtocol.ErrorCode.PDS_ERROR,
                    "synthetic failure"))
            : List.of();
    return BrokerMessages.responseBundle(
        request, "PDS-TEST Connector", "amqp://rabbitmq/req.PDS-TEST", code, payload);
  }

  @Test
  void responseQueueIsLastSegmentOfDestinationEndpoint() {
    MessageHeader header = new MessageHeader();
    header.addDestination().setEndpoint("amqp://rabbitmq.example.org/responses.portal");

    assertThat(QueryBrokerService.responseQueueOf(header)).isEqualTo("responses.portal");
  }

  @Test
  void endpointsOutsideTheResponsesNamespaceFallBackToDefault() {
    MessageHeader header = new MessageHeader();
    header.addDestination().setEndpoint("amqp://rabbitmq.example.org/some.other.queue");

    assertThat(QueryBrokerService.responseQueueOf(header)).isEqualTo("responses.default");
  }

  @Test
  void missingDestinationEndpointFallsBackToDefault() {
    MessageHeader header = new MessageHeader();
    header.addDestination().setName("no endpoint");

    assertThat(QueryBrokerService.responseQueueOf(header)).isEqualTo("responses.default");
  }

  @Test
  void topicModePublishesEachSiteOnlyItsOwnPseudonym() throws Exception {
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    QueryBrokerService svc = mockedService(RoutingMode.TOPIC, rabbit);

    svc.process(twoSiteRequest());

    Map<String, Bundle> perSite = capturedPublishesTo(rabbit, BrokerProtocol.TOPIC_EXCHANGE);
    assertThat(perSite)
        .containsOnlyKeys(
            BrokerProtocol.requestRoutingKey("PDS-EXAMPLE"),
            BrokerProtocol.requestRoutingKey("PDS-EXAMPLE-B"));
    // Each site receives ONLY its own pseudonym — no cross-site exposure.
    assertThat(pseudonymValues(perSite.get(BrokerProtocol.requestRoutingKey("PDS-EXAMPLE"))))
        .containsExactly("PSN-EXAMPLE-0001");
    assertThat(pseudonymValues(perSite.get(BrokerProtocol.requestRoutingKey("PDS-EXAMPLE-B"))))
        .containsExactly("PSN-B-0001");
    // The non-pseudonym operation parameter survives the trimming for every site.
    assertThat(hasParameter(perSite.get(BrokerProtocol.requestRoutingKey("PDS-EXAMPLE")), "since"))
        .isTrue();
  }

  @Test
  void fanoutModeBroadcastsTheFullBundleToEverySite() throws Exception {
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    QueryBrokerService svc = mockedService(RoutingMode.FANOUT, rabbit);

    svc.process(twoSiteRequest());

    Map<String, Bundle> broadcast = capturedPublishesTo(rabbit, BrokerProtocol.BROADCAST_EXCHANGE);
    // One broadcast on the fanout exchange (routing key "") carrying ALL pseudonyms;
    // connector self-filtering isolates the sites there (per-site trimming is TOPIC-only).
    assertThat(broadcast).containsOnlyKeys("");
    assertThat(pseudonymValues(broadcast.get("")))
        .containsExactlyInAnyOrder("PSN-EXAMPLE-0001", "PSN-B-0001");
  }

  private static QueryBrokerService mockedService(RoutingMode mode, RabbitTemplate rabbit)
      throws InterruptedException {
    MessageDefinitionRegistry registry = mock(MessageDefinitionRegistry.class);
    when(registry.findOperation(anyString())).thenReturn(Optional.of(new OperationDefinition()));
    ResponseAggregator aggregator = mock(ResponseAggregator.class);
    when(aggregator.await(anyString(), anyLong())).thenReturn(List.of());
    BrokerProperties properties = new BrokerProperties(null, 1000, "responses.broker", mode);
    return new QueryBrokerService(FHIR, registry, aggregator, rabbit, properties);
  }

  private static Bundle twoSiteRequest() {
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(new Identifier().setSystem(EXAMPLE_DOMAIN).setValue("PSN-EXAMPLE-0001"));
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(new Identifier().setSystem(EXAMPLE_B_DOMAIN).setValue("PSN-B-0001"));
    parameters.addParameter().setName("since").setValue(new StringType("2026-01-01"));
    return BrokerMessages.requestBundle(
        "https://querybroker.example.org/fhir/OperationDefinition/GetConditions",
        "Test",
        "amqp://rabbitmq/responses.portal",
        "amqp://rabbitmq/responses.portal",
        parameters);
  }

  /** Decodes each {@code send(exchange, key, msg)} to {@code exchange}, keyed by its routing key. */
  private static Map<String, Bundle> capturedPublishesTo(RabbitTemplate rabbit, String exchange) {
    ArgumentCaptor<String> ex = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
    verify(rabbit, atLeastOnce()).send(ex.capture(), key.capture(), msg.capture());
    Map<String, Bundle> byKey = new HashMap<>();
    for (int i = 0; i < ex.getAllValues().size(); i++) {
      if (exchange.equals(ex.getAllValues().get(i))) {
        byKey.put(key.getAllValues().get(i), decode(msg.getAllValues().get(i)));
      }
    }
    return byKey;
  }

  private static Bundle decode(Message message) {
    return (Bundle)
        FHIR.newJsonParser().parseResource(new String(message.getBody(), StandardCharsets.UTF_8));
  }

  private static List<String> pseudonymValues(Bundle bundle) {
    return BrokerMessages.pseudonymsOf(BrokerMessages.parametersOf(bundle).orElseThrow()).stream()
        .map(Identifier::getValue)
        .toList();
  }

  private static boolean hasParameter(Bundle bundle, String name) {
    return BrokerMessages.parametersOf(bundle).orElseThrow().getParameter().stream()
        .anyMatch(p -> name.equals(p.getName()));
  }
}
