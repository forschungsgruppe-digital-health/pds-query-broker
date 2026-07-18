package de.tudresden.fgdh.querybroker.pdsexample;

import static org.assertj.core.api.Assertions.assertThat;

import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

/**
 * ADR-012 end-to-end proof on the REAL reference connector: with the
 * MII KDS Diagnose targetProfile configured and the catalog-backed validator
 * wired, the synthetic store's output passes runtime validation — and a
 * deliberately wrong profile binding is rejected with the machine-readable
 * validation-error code before anything reaches the wire.
 */
class ExampleConnectorKdsValidationTest {

  private static final String DOMAIN = "https://ths.example.org/gpas/domain/PDS-EXAMPLE";
  private static final String GET_CONDITIONS =
      "https://querybroker.example.org/fhir/OperationDefinition/GetConditions";
  private static final String KDS_DIAGNOSE =
      "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";
  private static final String CATALOG_DIR = "../../catalog";

  @Test
  void syntheticStoreOutputConformsToKdsDiagnoseAtRuntime() {
    ExampleConnector connector =
        connector(Map.of("GetConditions", KDS_DIAGNOSE));

    Optional<Bundle> response = connector.handle(request("PSN-EXAMPLE-0001"));

    MessageHeader header = BrokerMessages.messageHeaderOf(response.orElseThrow()).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(
            response.get().getEntry().stream()
                .filter(e -> "Condition".equals(e.getResource().fhirType()))
                .count())
        .isEqualTo(2);
  }

  @Test
  void wrongProfileBindingIsRejectedBeforeSending() {
    // Conditions cannot conform to the BrokerOperationOutcome profile.
    ExampleConnector connector =
        connector(
            Map.of(
                "GetConditions",
                "https://querybroker.example.org/fhir/StructureDefinition/BrokerOperationOutcome"));

    Optional<Bundle> response = connector.handle(request("PSN-EXAMPLE-0001"));

    MessageHeader header = BrokerMessages.messageHeaderOf(response.orElseThrow()).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.FATALERROR);
    OperationOutcome outcome =
        response.get().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(OperationOutcome.class::isInstance)
            .map(OperationOutcome.class::cast)
            .findFirst()
            .orElseThrow();
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getCode())
        .isEqualTo(BrokerProtocol.ErrorCode.VALIDATION_ERROR.code());
    // No Condition may be on the wire after a validation failure.
    assertThat(
            response.get().getEntry().stream()
                .filter(e -> "Condition".equals(e.getResource().fhirType())))
        .isEmpty();
  }

  private static ExampleConnector connector(Map<String, String> targetProfiles) {
    return new ExampleConnector(
        new ConnectorProperties(
            "PDS-EXAMPLE",
            DOMAIN,
            Map.of("PSN-EXAMPLE-0001", "internal-0001"),
            targetProfiles,
            CATALOG_DIR,
            null,
            null),
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
        "KDS Validation Test",
        "amqp://rabbitmq/responses.portal",
        "amqp://rabbitmq/responses.portal",
        parameters);
  }
}
