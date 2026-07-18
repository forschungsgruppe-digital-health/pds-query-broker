package de.tudresden.fgdh.querybroker.pdsexample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tudresden.fgdh.querybroker.pdsexample.ConnectorProperties.ThsProperties;
import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

/** The pds.connector.ths.mode feature toggle (static map vs fTTP dispatcher). */
class ExampleConnectorThsToggleTest {

  private static final String DOMAIN = "https://ths.example.org/gpas/domain/PDS-EXAMPLE";
  private static final String GET_CONDITIONS =
      "https://querybroker.example.org/fhir/OperationDefinition/GetConditions";

  @Test
  void staticModeByDefaultResolvesViaTheSyntheticMap() {
    ExampleConnector connector = connector(ThsProperties.staticDefault());

    Bundle response = connector.handle(request("PSN-EXAMPLE-0001")).orElseThrow();

    assertThat(BrokerMessages.messageHeaderOf(response).orElseThrow().getResponse().getCode())
        .isEqualTo(ResponseType.OK);
    assertThat(
            response.getEntry().stream()
                .filter(e -> e.getResource() instanceof Condition))
        .isNotEmpty();
  }

  @Test
  void dispatcherModeRequiresABaseUrl() {
    assertThatThrownBy(
            () -> connector(new ThsProperties(ThsProperties.Mode.DISPATCHER, null, null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("dispatcher-base-url");
  }

  @Test
  void dispatcherModeIsWiredAndFailsGracefullyWhenUnreachable() {
    // Points at an unroutable dispatcher; the point is that the connector uses
    // the DISPATCHER path (not the static map) — an unreachable dispatcher
    // yields a fatal pds-error, not a static-map hit.
    ExampleConnector connector =
        connector(
            new ThsProperties(
                ThsProperties.Mode.DISPATCHER, "http://127.0.0.1:1", "PDS-EXAMPLE"));

    Bundle response = connector.handle(request("PSN-EXAMPLE-0001")).orElseThrow();

    assertThat(BrokerMessages.messageHeaderOf(response).orElseThrow().getResponse().getCode())
        .isEqualTo(ResponseType.FATALERROR);
    assertThat(response.getEntry().stream().filter(e -> e.getResource() instanceof Condition))
        .as("no data may be returned when the dispatcher cannot resolve the pseudonym")
        .isEmpty();
  }

  private static ExampleConnector connector(ThsProperties ths) {
    return new ExampleConnector(
        new ConnectorProperties(
            "PDS-EXAMPLE",
            DOMAIN,
            Map.of("PSN-EXAMPLE-0001", "internal-0001"),
            Map.of(),
            null,
            null,
            ths),
        new SyntheticConditionStore());
  }

  private static Bundle request(String pseudonym) {
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(new Identifier().setSystem(DOMAIN).setValue(pseudonym));
    return BrokerMessages.requestBundle(
        GET_CONDITIONS,
        "THS Toggle Test",
        "amqp://rabbitmq/responses.portal",
        "amqp://rabbitmq/responses.portal",
        parameters);
  }
}
