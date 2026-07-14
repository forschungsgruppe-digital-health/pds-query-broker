package de.tudresden.fgdh.querybroker.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ValidationResult;
import de.tudresden.fgdh.querybroker.sdk.AbstractPdsConnector;
import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Conformance test base for ANY {@link AbstractPdsConnector} implementation
 * (CONTRIBUTING § 3): extend it, provide the connector under test plus
 * synthetic pseudonyms, and the harness pins the protocol behavior —
 * profile-valid response envelopes (validated against the catalog/ mirror of
 * the IG), self-filtering silence, and the machine-readable error model.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PdsConnectorConformanceTest {

  protected static final FhirContext FHIR = FhirContext.forR4();
  private static CatalogValidator validator;

  /** The connector under test. */
  protected abstract AbstractPdsConnector connector();

  /** A pseudonym of the connector's own domain that resolves to data. */
  protected abstract String knownPseudonym();

  /** A pseudonym of the connector's own domain that resolves to NO data. */
  protected abstract String emptyResultPseudonym();

  /** A pseudonym of the connector's own domain the local THS cannot resolve. */
  protected abstract String unresolvablePseudonym();

  /** The canonical url of an operation the connector supports. */
  protected abstract String supportedOperation();

  @BeforeAll
  static void loadCatalog() {
    validator = new CatalogValidator(FHIR, CatalogValidator.defaultCatalogDir());
  }

  @Test
  void requestEnvelopeIsProfileValid() {
    assertProfileValid(request(knownPseudonym()), CatalogValidator.REQUEST_BUNDLE_PROFILE);
  }

  @Test
  void successResponseIsProfileValidAndCorrelated() {
    Bundle request = request(knownPseudonym());
    Bundle response = connector().handle(request).orElseThrow();

    assertProfileValid(response, CatalogValidator.RESPONSE_BUNDLE_PROFILE);
    MessageHeader header = BrokerMessages.messageHeaderOf(response).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(header.getResponse().getIdentifier())
        .isEqualTo(BrokerMessages.messageHeaderOf(request).orElseThrow().getIdElement().getIdPart());
  }

  @Test
  void emptyResultResponseIsStillProfileValid() {
    Bundle response = connector().handle(request(emptyResultPseudonym())).orElseThrow();

    assertProfileValid(response, CatalogValidator.RESPONSE_BUNDLE_PROFILE);
    assertThat(BrokerMessages.messageHeaderOf(response).orElseThrow().getResponse().getCode())
        .isEqualTo(ResponseType.OK);
  }

  @Test
  void errorResponseCarriesTheMachineReadableErrorModel() {
    Bundle response = connector().handle(request(unresolvablePseudonym())).orElseThrow();

    assertProfileValid(response, CatalogValidator.RESPONSE_BUNDLE_PROFILE);
    MessageHeader header = BrokerMessages.messageHeaderOf(response).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.FATALERROR);
    assertThat(header.getEventUriType().getValue())
        .isEqualTo(BrokerProtocol.OPERATION_ERROR_EVENT);
    OperationOutcome outcome =
        response.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(OperationOutcome.class::isInstance)
            .map(OperationOutcome.class::cast)
            .findFirst()
            .orElseThrow();
    assertProfileValid(outcome, CatalogValidator.OPERATION_OUTCOME_PROFILE);
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getSystem())
        .isEqualTo(BrokerProtocol.ERROR_CODE_SYSTEM);
  }

  @Test
  void staysSilentWhenNotAddressed() {
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(
            new Identifier()
                .setSystem("https://ths.example.org/gpas/domain/PDS-SOMEONE-ELSE")
                .setValue("PSN-FOREIGN-1"));
    Optional<Bundle> response =
        connector()
            .handle(
                BrokerMessages.requestBundle(
                    supportedOperation(),
                    "Conformance Harness",
                    "amqp://rabbitmq/responses.conformance",
                    "amqp://rabbitmq/responses.conformance",
                    parameters));

    assertThat(response).as("connector must self-filter foreign domains silently").isEmpty();
  }

  @Test
  void staysSilentForUnsupportedOperations() {
    Bundle request = request(knownPseudonym());
    BrokerMessages.messageHeaderOf(request)
        .orElseThrow()
        .setEvent(
            new org.hl7.fhir.r4.model.UriType(
                CatalogValidator.CANONICAL_BASE + "/OperationDefinition/NotSupportedAnywhere"));

    assertThat(connector().handle(request))
        .as("connector must self-filter unsupported operations silently")
        .isEmpty();
  }

  private Bundle request(String pseudonym) {
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(new Identifier().setSystem(connector().getGpasDomain()).setValue(pseudonym));
    return BrokerMessages.requestBundle(
        supportedOperation(),
        "Conformance Harness",
        "amqp://rabbitmq/responses.conformance",
        "amqp://rabbitmq/responses.conformance",
        parameters);
  }

  private void assertProfileValid(IBaseResource resource, String profile) {
    ValidationResult result = validator.validate(resource, profile);
    List<String> errors =
        result.getMessages().stream()
            .filter(
                m ->
                    m.getSeverity() == ca.uhn.fhir.validation.ResultSeverityEnum.ERROR
                        || m.getSeverity() == ca.uhn.fhir.validation.ResultSeverityEnum.FATAL)
            .map(m -> m.getSeverity() + " " + m.getLocationString() + ": " + m.getMessage())
            .toList();
    assertThat(errors)
        .as("validation against %s (see catalog/ mirror)", profile)
        .isEmpty();
  }
}
