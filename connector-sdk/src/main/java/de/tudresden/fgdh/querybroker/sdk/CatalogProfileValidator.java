package de.tudresden.fgdh.querybroker.sdk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A {@link ProfileValidator} backed by the {@code catalog/} mirror of the
 * ImplementationGuide — the SUSHI-generated StructureDefinitions, CodeSystems,
 * and ValueSets. Validating against the catalog (rather than hand-picked
 * fixtures) means the connector always validates against the profiles as
 * published.
 *
 * <p>The instance is thread-safe once constructed and should be reused (the
 * validation support chain is expensive to build).
 */
public final class CatalogProfileValidator implements ProfileValidator {

  private final FhirValidator validator;

  public CatalogProfileValidator(FhirContext fhirContext, Path catalogDir) {
    this.validator = newCatalogBackedValidator(fhirContext, catalogDir);
  }

  /**
   * Builds a {@link FhirValidator} whose profile/terminology support is loaded
   * from the {@code catalog/} mirror (StructureDefinitions, CodeSystems,
   * ValueSets). Shared with the conformance harness so both validate against
   * exactly the same published artifacts. The returned validator is thread-safe
   * and expensive to build — reuse it.
   */
  public static FhirValidator newCatalogBackedValidator(FhirContext fhirContext, Path catalogDir) {
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
    FhirValidator validator = fhirContext.newValidator();
    validator.registerValidatorModule(new FhirInstanceValidator(chain));
    return validator;
  }

  /** Locates the {@code catalog/} mirror from a typical module working directory. */
  public static Path defaultCatalogDir() {
    Path candidate = Path.of("catalog");
    return Files.isDirectory(candidate) ? candidate : Path.of("..", "catalog");
  }

  @Override
  public List<String> validate(IBaseResource resource, String profileUrl) {
    ValidationResult result =
        validator.validateWithResult(
            resource, new ValidationOptions().addProfile(profileUrl));
    return result.getMessages().stream()
        .filter(
            m ->
                m.getSeverity() == ResultSeverityEnum.ERROR
                    || m.getSeverity() == ResultSeverityEnum.FATAL)
        .map(m -> m.getLocationString() + ": " + m.getMessage())
        .toList();
  }
}
