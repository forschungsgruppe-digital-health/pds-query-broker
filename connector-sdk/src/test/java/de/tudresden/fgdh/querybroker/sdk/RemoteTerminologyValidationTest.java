package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ADR-013: with a remote FHIR terminology server configured, terminology
 * validation is ACTIVE and delegated to the server — proven against a mock
 * server that speaks the standard {@code $validate-code} operation (the same
 * interface SU-TermServ and Ontoserver expose).
 */
class RemoteTerminologyValidationTest {

  private static final String KDS_DIAGNOSE =
      "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";

  private static HttpServer mockServer;
  private static final AtomicBoolean CODE_IS_VALID = new AtomicBoolean(true);
  private static final List<String> REQUESTS = new CopyOnWriteArrayList<>();
  private static CatalogProfileValidator validator;

  @BeforeAll
  static void startMockTerminologyServer() throws IOException {
    mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    mockServer.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          REQUESTS.add(exchange.getRequestMethod() + " " + path);
          String body = mockResponse(path, exchange.getRequestURI().getQuery());
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/fhir+json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    mockServer.start();

    String baseUrl = "http://127.0.0.1:" + mockServer.getAddress().getPort() + "/fhir";
    validator =
        new CatalogProfileValidator(
            FhirContext.forR4(),
            CatalogProfileValidator.defaultCatalogDir(),
            TerminologyServerConfig.of(baseUrl));
  }

  @AfterAll
  static void stopMockTerminologyServer() {
    mockServer.stop(0);
  }

  /**
   * A minimal but protocol-correct FHIR terminology server, mirroring what
   * HAPI's remote support actually calls (verified empirically): search-by-url
   * fetches of CodeSystem/ValueSet, then {@code POST .../$validate-code}.
   */
  static String mockResponse(String path, String query) {
    if (path.contains("$validate-code")) {
      return CODE_IS_VALID.get()
          ? """
            {"resourceType":"Parameters","parameter":[
              {"name":"result","valueBoolean":true},
              {"name":"display","valueString":"Mock display"}]}"""
          : """
            {"resourceType":"Parameters","parameter":[
              {"name":"result","valueBoolean":false},
              {"name":"message","valueString":"Unknown code (mock terminology server)"}]}""";
    }
    if (path.endsWith("/CodeSystem") && query != null && query.startsWith("url=")) {
      return """
          {"resourceType":"Bundle","type":"searchset","total":1,"entry":[{"resource":
            {"resourceType":"CodeSystem","url":"http://fhir.de/CodeSystem/bfarm/icd-10-gm",
             "status":"active","content":"not-present"}}]}""";
    }
    if (path.endsWith("/ValueSet") && query != null && query.startsWith("url=")) {
      return """
          {"resourceType":"Bundle","type":"searchset","total":1,"entry":[{"resource":
            {"resourceType":"ValueSet","status":"active",
             "url":"https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/ValueSet/mii-vs-diagnose-icd10gm"}}]}""";
    }
    return """
        {"resourceType":"CapabilityStatement","status":"active","date":"2026-01-01",
         "kind":"instance","fhirVersion":"4.0.1","format":["json"]}""";
  }

  @Test
  void validCodeAccordingToTheServerPasses() {
    CODE_IS_VALID.set(true);

    List<String> violations = kdsValidate("C34.1");

    assertThat(violations).as("server says valid, got: %s", violations).isEmpty();
    assertThat(REQUESTS).as("the terminology server must actually be consulted").isNotEmpty();
  }

  @Test
  void invalidCodeAccordingToTheServerFails() {
    CODE_IS_VALID.set(false);

    List<String> violations = kdsValidate("NOT-A-REAL-CODE");

    assertThat(violations)
        .as("terminology validation must be ACTIVE with a server configured")
        .isNotEmpty();
  }

  private static List<String> kdsValidate(String icdCode) {
    Condition condition = new Condition();
    condition
        .getCode()
        .addCoding(
            new Coding("http://fhir.de/CodeSystem/bfarm/icd-10-gm", icdCode, "synthetic")
                .setVersion("2026"));
    condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
    condition.setRecordedDateElement(new org.hl7.fhir.r4.model.DateTimeType("2026-01-15"));
    return validator.validate(condition, KDS_DIAGNOSE);
  }
}
