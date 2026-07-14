package de.tudresden.fgdh.querybroker.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Profile validator fed exclusively from the {@code catalog/} mirror — the
 * SUSHI-generated conformance artifacts (StructureDefinitions, CodeSystems,
 * ValueSets). Validating against the catalog (not hand-picked fixtures)
 * guarantees the harness always tests against the IG as published.
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
    PrePopulatedValidationSupport catalogArtifacts =
        new PrePopulatedValidationSupport(fhirContext);
    for (String type : new String[] {"StructureDefinition", "CodeSystem", "ValueSet"}) {
      Path dir = catalogDir.resolve(type);
      if (!Files.isDirectory(dir)) {
        continue;
      }
      try (Stream<Path> files = Files.list(dir)) {
        files
            .filter(f -> f.toString().endsWith(".json"))
            .forEach(
                f -> {
                  try {
                    catalogArtifacts.addResource(
                        fhirContext.newJsonParser().parseResource(Files.readString(f)));
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                });
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    ValidationSupportChain chain =
        new ValidationSupportChain(
            new DefaultProfileValidationSupport(fhirContext),
            catalogArtifacts,
            new SnapshotGeneratingValidationSupport(fhirContext),
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext));
    this.validator = fhirContext.newValidator();
    this.validator.registerValidatorModule(new FhirInstanceValidator(chain));
  }

  /** Locates the catalog/ mirror from a Gradle module working directory. */
  public static Path defaultCatalogDir() {
    Path candidate = Path.of("catalog");
    if (Files.isDirectory(candidate)) {
      return candidate;
    }
    return Path.of("..", "catalog");
  }

  public ValidationResult validate(IBaseResource resource, String profileUrl) {
    return validator.validateWithResult(
        resource, new ValidationOptions().addProfile(profileUrl));
  }

  public FhirContext fhirContext() {
    return fhirContext;
  }
}
