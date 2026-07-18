package de.tudresden.fgdh.querybroker.pdsexample;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.stereotype.Component;

/**
 * In-memory synthetic data source — the increment-1 stand-in for a local data
 * system. Obviously artificial test data only (no realistic names anywhere).
 */
@Component
public class SyntheticConditionStore {

  private static final String ICD10GM = "http://fhir.de/CodeSystem/bfarm/icd-10-gm";

  // Real ICD-10-GM codes (required for terminology validation, ADR-013) on
  // synthetic patients. Meanings: C34.1 = neoplasm upper lobe bronchus,
  // J44.9 = COPD unspecified, I10.90 = essential hypertension unspecified.
  private final Map<String, List<Condition>> byInternalId =
      Map.of(
          "internal-0001", List.of(condition("C34.1"), condition("J44.9")),
          "internal-0002", List.of(condition("I10.90")));

  public List<Condition> findByInternalId(String internalId) {
    return byInternalId.getOrDefault(internalId, List.of());
  }

  private static Condition condition(String icdCode) {
    Condition condition = new Condition();
    condition.setId(UUID.randomUUID().toString());
    // Shaped to conform to the MII KDS Diagnose profile (ADR-012): the
    // icd10-gm coding slice requires system + version + code, and the profile
    // requires subject and recordedDate. Version 2025 is the current
    // ICD-10-GM edition the pilot terminology server (SU-TermServ) holds; the
    // display is left to the terminology server (no brittle display string).
    condition.setCode(
        new CodeableConcept().addCoding(new Coding().setSystem(ICD10GM).setCode(icdCode)
            .setVersion("2025")));
    condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
    condition.setRecordedDateElement(new org.hl7.fhir.r4.model.DateTimeType("2025-01-15"));
    return condition;
  }
}
