package de.tudresden.fgdh.querybroker.sdk;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Configuration for a remote FHIR terminology server (ADR-013).
 *
 * <p>Generic: any server exposing the standard FHIR terminology operations
 * ({@code $validate-code}, {@code $expand}, {@code $lookup}) works — e.g. the
 * MII SU-TermServ (the pilot's server, reachable only with an approved
 * application and an mTLS client certificate) or a CSIRO Ontoserver instance.
 * The keystore/truststore fields are optional: leave them unset for servers
 * without mutual TLS.
 *
 * @param baseUrl FHIR base URL of the terminology server (e.g.
 *     {@code https://<su-termserv-host>/fhir})
 * @param clientKeystorePath PKCS12 keystore with the mTLS client certificate
 *     and key (from the SU-TermServ onboarding); {@code null} = no client
 *     certificate
 * @param clientKeystorePassword password of the client keystore
 * @param truststorePath optional PKCS12 truststore for the server's CA;
 *     {@code null} = JVM default trust
 * @param truststorePassword password of the truststore
 */
public record TerminologyServerConfig(
    String baseUrl,
    Path clientKeystorePath,
    String clientKeystorePassword,
    Path truststorePath,
    String truststorePassword) {

  public TerminologyServerConfig {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("terminology server baseUrl must not be blank");
    }
  }

  /** Plain configuration without mutual TLS. */
  public static TerminologyServerConfig of(String baseUrl) {
    return new TerminologyServerConfig(baseUrl, null, null, null, null);
  }

  /**
   * Reads the configuration from environment variables — {@code
   * TERMINOLOGY_SERVER_URL} (required for a non-empty result), {@code
   * TERMINOLOGY_CLIENT_KEYSTORE}, {@code TERMINOLOGY_CLIENT_KEYSTORE_PASSWORD},
   * {@code TERMINOLOGY_TRUSTSTORE}, {@code TERMINOLOGY_TRUSTSTORE_PASSWORD}.
   * Used by the conformance harness to opt in without code changes.
   */
  public static Optional<TerminologyServerConfig> fromEnvironment() {
    String url = System.getenv("TERMINOLOGY_SERVER_URL");
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    String keystore = System.getenv("TERMINOLOGY_CLIENT_KEYSTORE");
    String truststore = System.getenv("TERMINOLOGY_TRUSTSTORE");
    return Optional.of(
        new TerminologyServerConfig(
            url,
            keystore == null || keystore.isBlank() ? null : Path.of(keystore),
            System.getenv("TERMINOLOGY_CLIENT_KEYSTORE_PASSWORD"),
            truststore == null || truststore.isBlank() ? null : Path.of(truststore),
            System.getenv("TERMINOLOGY_TRUSTSTORE_PASSWORD")));
  }

  public boolean usesMutualTls() {
    return clientKeystorePath != null;
  }
}
