package de.tudresden.fgdh.querybroker.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import de.tudresden.fgdh.querybroker.sdk.CatalogProfileValidator;
import java.nio.file.Path;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Bundle-level profile validator for the conformance harness, fed exclusively
 * from the {@code catalog/} mirror. Shares the catalog-backed validation
 * support chain with the SDK's {@link CatalogProfileValidator} (single source
 * of truth for how the IG artifacts are loaded), but exposes the full
 * {@link ValidationResult} so the harness can assert on individual issues
 * (e.g. the negative controls).
 */
public final class CatalogValidator {

  public static final String CANONICAL_BASE = "https://querybroker.example.org/fhir";
  public static final String REQUEST_BUNDLE_PROFILE =
      CANONICAL_BASE + "/StructureDefinition/BrokerRequestBundle";
  public static final String RESPONSE_BUNDLE_PROFILE =
      CANONICAL_BASE + "/StructureDefinition/BrokerResponseBundle";
  public static final String OPERATION_OUTCOME_PROFILE =
      CANONICAL_BASE + "/StructureDefinition/BrokerOperationOutcome";

  private final FhirContext fhirContext;
  private final FhirValidator validator;

  public CatalogValidator(FhirContext fhirContext, Path catalogDir) {
    this.fhirContext = fhirContext;
    this.validator = CatalogProfileValidator.newCatalogBackedValidator(fhirContext, catalogDir);
  }

  /** Locates the catalog/ mirror from a Gradle module working directory. */
  public static Path defaultCatalogDir() {
    return CatalogProfileValidator.defaultCatalogDir();
  }

  public ValidationResult validate(IBaseResource resource, String profileUrl) {
    return validator.validateWithResult(
        resource, new ValidationOptions().addProfile(profileUrl));
  }

  public FhirContext fhirContext() {
    return fhirContext;
  }
}
