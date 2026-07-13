package de.tudresden.fgdh.querybroker.broker;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.MessageHeader;
import org.junit.jupiter.api.Test;

class QueryBrokerServiceTest {

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
