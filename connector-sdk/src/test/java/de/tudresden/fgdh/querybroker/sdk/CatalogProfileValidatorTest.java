package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Proves the real HAPI-backed {@link CatalogProfileValidator} both accepts a
 * conformant resource and rejects a non-conformant one — validated against the
 * existing {@code BrokerOperationOutcome} profile in the {@code catalog/}
 * mirror (no new fixture profiles needed).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogProfileValidatorTest {

  private static final String BROKER_OPERATION_OUTCOME =
      "https://querybroker.example.org/fhir/StructureDefinition/BrokerOperationOutcome";

  private static CatalogProfileValidator validator;

  @BeforeAll
  static void setUp() {
    validator =
        new CatalogProfileValidator(
            FhirContext.forR4(), CatalogProfileValidator.defaultCatalogDir());
  }

  @Test
  void acceptsAConformantBrokerOperationOutcome() {
    OperationOutcome conformant =
        BrokerMessages.operationOutcome(
            IssueSeverity.ERROR,
            IssueType.EXCEPTION,
            BrokerProtocol.ErrorCode.PDS_ERROR,
            "synthetic diagnostics");

    List<String> violations = validator.validate(conformant, BROKER_OPERATION_OUTCOME);

    assertThat(violations).as("conformant outcome, got: %s", violations).isEmpty();
  }

  @Test
  void rejectsAnOutcomeMissingTheMandatoryMachineReadableDetails() {
    // BrokerOperationOutcome requires issue.details 1..1; omit it.
    OperationOutcome invalid = new OperationOutcome();
    invalid.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.EXCEPTION);

    List<String> violations = validator.validate(invalid, BROKER_OPERATION_OUTCOME);

    assertThat(violations).as("missing issue.details must fail").isNotEmpty();
  }
}
