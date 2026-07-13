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

  private final Map<String, List<Condition>> byInternalId =
      Map.of(
          "internal-0001",
          List.of(
              condition("C34.1", "Malignant neoplasm, upper lobe bronchus (synthetic)"),
              condition("J44.9", "COPD, unspecified (synthetic)")),
          "internal-0002",
          List.of(condition("I10.90", "Essential hypertension (synthetic)")));

  public List<Condition> findByInternalId(String internalId) {
    return byInternalId.getOrDefault(internalId, List.of());
  }

  private static Condition condition(String icdCode, String display) {
    Condition condition = new Condition();
    condition.setId(UUID.randomUUID().toString());
    condition.setCode(
        new CodeableConcept().addCoding(new Coding(ICD10GM, icdCode, display)));
    condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
    return condition;
  }
}
