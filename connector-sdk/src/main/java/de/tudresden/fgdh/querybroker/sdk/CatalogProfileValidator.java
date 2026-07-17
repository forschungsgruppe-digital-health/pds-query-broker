package de.tudresden.fgdh.querybroker.sdk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
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
    this(fhirContext, catalogDir, null);
  }

  /**
   * @param terminologyServer optional remote FHIR terminology server
   *     (ADR-013). When present, terminology/binding validation is ENABLED and
   *     delegated to the server; when {@code null}, only structural validation
   *     runs (the ADR-012 pilot default).
   */
  public CatalogProfileValidator(
      FhirContext fhirContext, Path catalogDir, TerminologyServerConfig terminologyServer) {
    this.validator = newCatalogBackedValidator(fhirContext, catalogDir, terminologyServer);
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
    return newCatalogBackedValidator(fhirContext, catalogDir, null);
  }

  /** As {@link #newCatalogBackedValidator(FhirContext, Path)}, optionally with a terminology server. */
  public static FhirValidator newCatalogBackedValidator(
      FhirContext fhirContext, Path catalogDir, TerminologyServerConfig terminologyServer) {
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

    List<ca.uhn.fhir.context.support.IValidationSupport> supports = new ArrayList<>();
    supports.add(new DefaultProfileValidationSupport(fhirContext));
    supports.add(catalogArtifacts);
    supports.add(new SnapshotGeneratingValidationSupport(fhirContext));
    if (terminologyServer != null) {
      // Remote server FIRST among the terminology services, so its answers
      // are not shadowed by the in-memory expander failing on code systems
      // that only the server knows (e.g. ICD-10-GM).
      supports.add(remoteTerminologySupport(fhirContext, terminologyServer));
    }
    supports.add(new InMemoryTerminologyServerValidationSupport(fhirContext));
    supports.add(new CommonCodeSystemsTerminologyService(fhirContext));

    ValidationSupportChain chain =
        new ValidationSupportChain(supports.toArray(ca.uhn.fhir.context.support.IValidationSupport[]::new));
    FhirInstanceValidator instanceValidator = new FhirInstanceValidator(chain);
    if (terminologyServer == null) {
      // Without a terminology server the national code systems bound by
      // external profiles (e.g. ICD-10-GM for MII KDS Diagnose) cannot be
      // resolved. Terminology/binding checks are therefore disabled:
      // structural, cardinality, slicing (pattern-discriminated), and FHIRPath
      // invariant validation stay fully enforced (ADR-012). With a configured
      // server (ADR-013), full terminology validation is active.
      instanceValidator.setNoTerminologyChecks(true);
    }
    FhirValidator validator = fhirContext.newValidator();
    validator.registerValidatorModule(instanceValidator);
    return validator;
  }

  /**
   * A {@link RemoteTerminologyServiceValidationSupport} for any standard FHIR
   * terminology server (SU-TermServ, Ontoserver, …), with an mTLS client
   * certificate when the configuration provides one (ADR-013).
   */
  private static RemoteTerminologyServiceValidationSupport remoteTerminologySupport(
      FhirContext fhirContext, TerminologyServerConfig config) {
    // Dedicated client context: HAPI's GenericClient resolves the HTTP client
    // PER REQUEST via FhirContext.getRestfulClientFactory() (empirically
    // verified) — a factory merely passed into the support constructor is
    // bypassed. Installing the mTLS-capable factory on its own context keeps
    // the caller's shared context untouched.
    FhirContext clientContext = FhirContext.forR4();
    ApacheRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(clientContext);
    clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
    if (config.usesMutualTls() || config.truststorePath() != null) {
      clientFactory.setHttpClient(
          HttpClients.custom().setSSLContext(sslContext(config)).build());
    }
    clientContext.setRestfulClientFactory(clientFactory);
    return new RemoteTerminologyServiceValidationSupport(
        clientContext, config.baseUrl(), clientFactory);
  }

  private static SSLContext sslContext(TerminologyServerConfig config) {
    try {
      KeyManager[] keyManagers = null;
      if (config.usesMutualTls()) {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password =
            config.clientKeystorePassword() == null
                ? new char[0]
                : config.clientKeystorePassword().toCharArray();
        try (InputStream in = Files.newInputStream(config.clientKeystorePath())) {
          keyStore.load(in, password);
        }
        KeyManagerFactory kmf =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        keyManagers = kmf.getKeyManagers();
      }

      TrustManager[] trustManagers = null;
      if (config.truststorePath() != null) {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(config.truststorePath())) {
          trustStore.load(
              in,
              config.truststorePassword() == null
                  ? new char[0]
                  : config.truststorePassword().toCharArray());
        }
        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        trustManagers = tmf.getTrustManagers();
      }

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, null);
      return sslContext;
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException(
          "Cannot initialize mTLS context for terminology server " + config.baseUrl(), e);
    }
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
