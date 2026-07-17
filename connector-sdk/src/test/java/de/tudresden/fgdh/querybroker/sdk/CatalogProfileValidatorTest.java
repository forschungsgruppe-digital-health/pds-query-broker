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

  // --- vendored external package: MII KDS Diagnose (ADR-012, catalog/packages) ---

  private static final String KDS_DIAGNOSE =
      "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";

  @Test
  void acceptsAKdsDiagnoseConformantCondition() {
    org.hl7.fhir.r4.model.Condition condition = new org.hl7.fhir.r4.model.Condition();
    condition
        .getCode()
        .addCoding(
            new org.hl7.fhir.r4.model.Coding(
                    "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
                    "C34.1",
                    "Synthetic neoplasm (obviously artificial)")
                .setVersion("2026"));
    condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
    condition.setRecordedDateElement(new org.hl7.fhir.r4.model.DateTimeType("2026-01-15"));

    List<String> violations = validator.validate(condition, KDS_DIAGNOSE);

    assertThat(violations).as("KDS-conformant condition, got: %s", violations).isEmpty();
  }

  @Test
  void rejectsAConditionMissingKdsMandatoryElements() {
    // Missing recordedDate (1..1) and icd10-gm coding.version (1..1 in the slice).
    org.hl7.fhir.r4.model.Condition invalid = new org.hl7.fhir.r4.model.Condition();
    invalid
        .getCode()
        .addCoding(
            new org.hl7.fhir.r4.model.Coding(
                "http://fhir.de/CodeSystem/bfarm/icd-10-gm", "C34.1", "no version, no recordedDate"));
    invalid.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");

    List<String> violations = validator.validate(invalid, KDS_DIAGNOSE);

    assertThat(violations).as("missing recordedDate/coding.version must fail").isNotEmpty();
  }
}
