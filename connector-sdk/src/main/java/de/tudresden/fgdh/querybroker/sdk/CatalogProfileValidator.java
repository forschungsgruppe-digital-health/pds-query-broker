package de.tudresden.fgdh.querybroker.sdk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import org.hl7.fhir.utilities.npm.NpmPackage;

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
   * ValueSets) plus any vendored external FHIR NPM packages under
   * {@code catalog/packages/*.tgz} (e.g. the MII Kerndatensatz modules bound
   * via {@code targetProfile} — ADR-012). Shared with the conformance harness
   * so both validate against exactly the same published artifacts. The
   * returned validator is thread-safe and expensive to build — reuse it.
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

    loadVendoredPackages(fhirContext, catalogDir.resolve("packages"), catalogArtifacts);

    ValidationSupportChain chain =
        new ValidationSupportChain(
            new DefaultProfileValidationSupport(fhirContext),
            catalogArtifacts,
            new SnapshotGeneratingValidationSupport(fhirContext),
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext));
    FhirInstanceValidator instanceValidator = new FhirInstanceValidator(chain);
    // The pilot runs WITHOUT a terminology server, and the national code
    // systems bound by external profiles (e.g. ICD-10-GM for MII KDS Diagnose)
    // are not vendored. Terminology/binding checks are therefore disabled:
    // structural, cardinality, slicing (pattern-discriminated), and FHIRPath
    // invariant validation stay fully enforced. Terminology validation returns
    // with a terminology-server integration (ADR-012).
    instanceValidator.setNoTerminologyChecks(true);
    FhirValidator validator = fhirContext.newValidator();
    validator.registerValidatorModule(instanceValidator);
    return validator;
  }

  /**
   * Loads the conformance artifacts (StructureDefinition/CodeSystem/ValueSet)
   * of every vendored FHIR NPM package ({@code *.tgz}) in {@code packagesDir}
   * into the given support. Same effect as HAPI's
   * {@code NpmPackageValidationSupport}, which in this HAPI version only loads
   * from the classpath — these packages live in the repository instead.
   */
  private static void loadVendoredPackages(
      FhirContext fhirContext, Path packagesDir, PrePopulatedValidationSupport support) {
    if (!Files.isDirectory(packagesDir)) {
      return;
    }
    try (Stream<Path> files = Files.list(packagesDir)) {
      for (Path tgz : files.filter(f -> f.toString().endsWith(".tgz")).toList()) {
        try (InputStream in = Files.newInputStream(tgz)) {
          NpmPackage npm = NpmPackage.fromPackage(in);
          for (String resourceFile :
              npm.listResources("StructureDefinition", "CodeSystem", "ValueSet")) {
            try (InputStream resource = npm.loadResource(resourceFile)) {
              support.addResource(
                  fhirContext
                      .newJsonParser()
                      .parseResource(new String(resource.readAllBytes(), StandardCharsets.UTF_8)));
            }
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
