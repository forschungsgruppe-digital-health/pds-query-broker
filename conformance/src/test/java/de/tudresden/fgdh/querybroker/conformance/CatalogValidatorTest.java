package de.tudresden.fgdh.querybroker.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import java.util.Date;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Negative controls: the harness must FAIL clearly non-conformant input —
 * otherwise every green conformance run would be worthless.
 */
class CatalogValidatorTest {

  private static CatalogValidator validator;

  @BeforeAll
  static void setUp() {
    validator = new CatalogValidator(FhirContext.forR4(), CatalogValidator.defaultCatalogDir());
  }

  @Test
  void responseBundleWithoutResponseElementFailsInvariantQbResponse1() {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.MESSAGE);
    bundle.setTimestamp(new Date());
    MessageHeader header = new MessageHeader();
    header.setEvent(
        new org.hl7.fhir.r4.model.UriType(
            CatalogValidator.CANONICAL_BASE + "/OperationDefinition/GetConditions"));
    header.addDestination().setEndpoint("amqp://rabbitmq/responses.portal");
    header.getSource().setName("negative control").setEndpoint("amqp://rabbitmq/x");
    bundle.addEntry().setFullUrl("urn:uuid:00000000-0000-4000-8000-000000000001")
        .setResource(header);
    // no MessageHeader.response -> must violate qb-response-1

    ValidationResult result =
        validator.validate(bundle, CatalogValidator.RESPONSE_BUNDLE_PROFILE);

    assertThat(
            result.getMessages().stream()
                .anyMatch(
                    m ->
                        m.getSeverity().ordinal() >= ResultSeverityEnum.ERROR.ordinal()
                            && m.getMessage().contains("qb-response-1")))
        .as(
            "expected qb-response-1 violation, got: %s",
            result.getMessages())
        .isTrue();
  }

  @Test
  void collectionBundleFailsTheRequestProfile() {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setTimestamp(new Date());

    ValidationResult result =
        validator.validate(bundle, CatalogValidator.REQUEST_BUNDLE_PROFILE);

    assertThat(
            result.getMessages().stream()
                .anyMatch(m -> m.getSeverity().ordinal() >= ResultSeverityEnum.ERROR.ordinal()))
        .as("a collection bundle must not validate as BrokerRequestBundle")
        .isTrue();
  }
}
