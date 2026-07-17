package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ADR-013 mTLS proof: the terminology client presents a client certificate
 * (as the SU-TermServ onboarding requires) against an HTTPS mock that
 * REQUIRES client authentication. All key material is generated at test time
 * with the JDK's keytool — nothing is committed.
 */
class MutualTlsTerminologyServerTest {

  private static final String KDS_DIAGNOSE =
      "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";
  private static final String STOREPASS = "synthetic-test-only";

  @TempDir static Path certs;

  static HttpsServer server;
  static String baseUrl;
  private static final List<String> REQUESTS = new CopyOnWriteArrayList<>();

  @BeforeAll
  static void startMutualTlsServer() throws Exception {
    // Server identity (SAN for 127.0.0.1) + client identity, cross-trusted.
    keytool("-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "30", "-storetype", "PKCS12", "-keystore", p("server.p12"),
        "-storepass", STOREPASS, "-dname", "CN=localhost",
        "-ext", "SAN=ip:127.0.0.1,dns:localhost");
    keytool("-exportcert", "-alias", "server", "-keystore", p("server.p12"),
        "-storepass", STOREPASS, "-file", p("server.crt"));
    keytool("-importcert", "-noprompt", "-alias", "server", "-file", p("server.crt"),
        "-storetype", "PKCS12", "-keystore", p("client-truststore.p12"), "-storepass", STOREPASS);
    keytool("-genkeypair", "-alias", "client", "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "30", "-storetype", "PKCS12", "-keystore", p("client.p12"),
        "-storepass", STOREPASS, "-dname", "CN=qb-synthetic-test-client");
    keytool("-exportcert", "-alias", "client", "-keystore", p("client.p12"),
        "-storepass", STOREPASS, "-file", p("client.crt"));
    keytool("-importcert", "-noprompt", "-alias", "client", "-file", p("client.crt"),
        "-storetype", "PKCS12", "-keystore", p("server-truststore.p12"), "-storepass", STOREPASS);

    server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setHttpsConfigurator(
        new HttpsConfigurator(serverSslContext()) {
          @Override
          public void configure(HttpsParameters params) {
            SSLContext context = getSSLContext();
            var sslParams = context.getDefaultSSLParameters();
            sslParams.setNeedClientAuth(true);
            params.setSSLParameters(sslParams);
          }
        });
    server.createContext(
        "/",
        exchange -> {
          REQUESTS.add(
              exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
          byte[] bytes =
              RemoteTerminologyValidationTest.mockResponse(
                      exchange.getRequestURI().getPath(), exchange.getRequestURI().getQuery())
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/fhir+json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
    baseUrl = "https://127.0.0.1:" + server.getAddress().getPort() + "/fhir";
  }

  @AfterAll
  static void stop() {
    server.stop(0);
  }

  @Test
  void clientCertificateAuthenticatedValidationSucceeds() {
    CatalogProfileValidator validator =
        new CatalogProfileValidator(
            FhirContext.forR4(),
            CatalogProfileValidator.defaultCatalogDir(),
            new TerminologyServerConfig(
                baseUrl,
                certs.resolve("client.p12"),
                STOREPASS,
                certs.resolve("client-truststore.p12"),
                STOREPASS));

    List<String> violations = validator.validate(kdsCondition(), KDS_DIAGNOSE);

    assertThat(violations).as("mTLS handshake + validation, got: %s", violations).isEmpty();
    assertThat(REQUESTS)
        .as("the mTLS-protected terminology server must have been reached")
        .anyMatch(r -> r.contains("$validate-code"));
  }

  @Test
  void withoutClientCertificateTheServerIsNeverReached() {
    REQUESTS.clear();
    CatalogProfileValidator validator =
        new CatalogProfileValidator(
            FhirContext.forR4(),
            CatalogProfileValidator.defaultCatalogDir(),
            new TerminologyServerConfig(
                baseUrl, null, null, certs.resolve("client-truststore.p12"), STOREPASS));

    validator.validate(kdsCondition(), KDS_DIAGNOSE);

    assertThat(REQUESTS)
        .as("without a client certificate the handshake must fail — client auth is enforced")
        .noneMatch(r -> r.contains("$validate-code"));
  }

  private static Condition kdsCondition() {
    Condition condition = new Condition();
    condition
        .getCode()
        .addCoding(
            new Coding("http://fhir.de/CodeSystem/bfarm/icd-10-gm", "C34.1", "synthetic")
                .setVersion("2026"));
    condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
    condition.setRecordedDateElement(new org.hl7.fhir.r4.model.DateTimeType("2026-01-15"));
    return condition;
  }

  private static SSLContext serverSslContext() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(certs.resolve("server.p12"))) {
      keyStore.load(in, STOREPASS.toCharArray());
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, STOREPASS.toCharArray());

    KeyStore trustStore = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(certs.resolve("server-truststore.p12"))) {
      trustStore.load(in, STOREPASS.toCharArray());
    }
    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);

    SSLContext context = SSLContext.getInstance("TLS");
    context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return context;
  }

  private static void keytool(String... args) throws Exception {
    String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
    String[] cmd = new String[args.length + 1];
    cmd[0] = keytool;
    System.arraycopy(args, 0, cmd, 1, args.length);
    Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) {
      throw new IllegalStateException("keytool failed: " + output);
    }
  }

  private static String p(String file) {
    return certs.resolve(file).toString();
  }
}
