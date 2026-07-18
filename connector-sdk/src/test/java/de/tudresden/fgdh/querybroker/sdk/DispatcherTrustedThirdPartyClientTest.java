package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The fTTP dispatcher client resolves a pseudonym through the gPAS
 * {@code $dePseudonymize} operation. Verified against a mock that speaks the
 * exact TTP-FHIR gateway request/response shape (target + pseudonym in;
 * an "original" part with the resolved valueIdentifier out; an "error" part
 * for unknown pseudonyms) — the same contract the real dispatcher and the
 * THS Greifswald gateway expose.
 */
class DispatcherTrustedThirdPartyClientTest {

  private static final String GPAS_SYSTEM = "https://ths-greifswald.de/gpas";
  private static final String DOMAIN = "PDS-EXAMPLE";

  private static HttpServer server;
  private static final List<String> REQUESTS = new CopyOnWriteArrayList<>();
  private static DispatcherTrustedThirdPartyClient client;

  @BeforeAll
  static void startMockDispatcher() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          REQUESTS.add(exchange.getRequestMethod() + " " + path);
          String response;
          if (path.endsWith("/ttp-fhir/fhir/gpas/$dePseudonymize")) {
            // Echo the resolution: PSN-KNOWN -> internal-42; anything else = error.
            response =
                body.contains("PSN-KNOWN")
                    ? success("internal-42", "PSN-KNOWN")
                    : error("PSN-UNKNOWN");
          } else if (path.endsWith("/metadata")) {
            response =
                "{\"resourceType\":\"CapabilityStatement\",\"status\":\"active\",\"date\":\"2026-01-01\","
                    + "\"kind\":\"instance\",\"fhirVersion\":\"4.0.1\",\"format\":[\"json\"]}";
          } else {
            response = "{\"resourceType\":\"OperationOutcome\"}";
          }
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/fhir+json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();

    client =
        new DispatcherTrustedThirdPartyClient(
            FhirContext.forR4(),
            "http://127.0.0.1:" + server.getAddress().getPort(),
            DOMAIN);
  }

  @AfterAll
  static void stopMockDispatcher() {
    server.stop(0);
  }

  @Test
  void resolvesAKnownPseudonymToItsOriginal() {
    Optional<String> internalId = client.resolveToInternalId("PSN-KNOWN");

    assertThat(internalId).contains("internal-42");
    assertThat(REQUESTS)
        .anyMatch(r -> r.endsWith("/ttp-fhir/fhir/gpas/$dePseudonymize"));
  }

  @Test
  void returnsEmptyForAnUnknownPseudonym() {
    assertThat(client.resolveToInternalId("PSN-DOES-NOT-EXIST")).isEmpty();
  }

  private static String success(String original, String pseudonym) {
    return """
        {"resourceType":"Parameters","parameter":[
          {"name":"original","part":[
            {"name":"target","valueIdentifier":{"system":"%s","value":"%s"}},
            {"name":"original","valueIdentifier":{"system":"%s","value":"%s"}},
            {"name":"pseudonym","valueIdentifier":{"system":"%s","value":"%s"}}]}]}"""
        .formatted(GPAS_SYSTEM, DOMAIN, GPAS_SYSTEM, original, GPAS_SYSTEM, pseudonym);
  }

  private static String error(String pseudonym) {
    return """
        {"resourceType":"Parameters","parameter":[
          {"name":"error","part":[
            {"name":"pseudonym","valueIdentifier":{"system":"%s","value":"%s"}},
            {"name":"error-code","valueCoding":{"code":"not-found","display":"Not Found"}}]}]}"""
        .formatted(GPAS_SYSTEM, pseudonym);
  }
}
