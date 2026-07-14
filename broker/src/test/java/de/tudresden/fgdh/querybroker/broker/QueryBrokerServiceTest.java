package de.tudresden.fgdh.querybroker.broker;

import static org.assertj.core.api.Assertions.assertThat;

import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;

class QueryBrokerServiceTest {

  private final QueryBrokerService service =
      new QueryBrokerService(null, null, null, null, new BrokerProperties(null, 1000, null));

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
}
