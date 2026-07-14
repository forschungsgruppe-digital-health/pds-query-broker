package de.tudresden.fgdh.querybroker.broker;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.pdsexample.PdsExampleApplication;
import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Increment-1 definition of done: the whole loop — HTTP ingress, catalog
 * lookup, fan-out over RabbitMQ, connector self-filtering, aggregation, and
 * the timeout path — against real RabbitMQ + HAPI catalog containers.
 */
@Testcontainers
class WalkingSkeletonIntegrationTest {

  private static final String EXAMPLE_DOMAIN = "https://ths.example.org/gpas/domain/PDS-EXAMPLE";
  private static final String EXAMPLE_B_DOMAIN =
      "https://ths.example.org/gpas/domain/PDS-EXAMPLE-B";
  private static final String UNKNOWN_DOMAIN = "https://ths.example.org/gpas/domain/PDS-GHOST";
  private static final String GET_CONDITIONS =
      "https://querybroker.example.org/fhir/OperationDefinition/GetConditions";
  private static final long TIMEOUT_MS = 3000;

  @Container
  private static final RabbitMQContainer RABBIT =
      new RabbitMQContainer("rabbitmq:3.13-management-alpine");

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> CATALOG =
      new GenericContainer<>("hapiproject/hapi:v7.4.0")
          .withExposedPorts(8080)
          .withEnv("hapi.fhir.fhir_version", "R4")
          .waitingFor(
              Wait.forHttp("/fhir/metadata")
                  .forStatusCode(200)
                  .withStartupTimeout(java.time.Duration.ofMinutes(5)));

  private static final FhirContext FHIR = FhirContext.forR4();
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  private static ConfigurableApplicationContext connectorContext;
  private static ConfigurableApplicationContext connectorBContext;
  private static ConfigurableApplicationContext brokerContext;
  private static String brokerUrl;

  @BeforeAll
  static void startStack() throws Exception {
    seedCatalog();

    // Command-line-arg style: highest precedence, beats application.yml
    // (builder .properties() would only set low-precedence defaults).
    // Both apps ship an application.yml; in a shared test JVM only one is on
    // the classpath first — so the connector's config is passed explicitly.
    connectorContext =
        new SpringApplicationBuilder(PdsExampleApplication.class)
            .run(
                "--spring.main.web-application-type=none",
                "--spring.rabbitmq.host=" + RABBIT.getHost(),
                "--spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
                "--spring.rabbitmq.username=" + RABBIT.getAdminUsername(),
                "--spring.rabbitmq.password=" + RABBIT.getAdminPassword(),
                "--pds.connector.pds-id=PDS-EXAMPLE",
                "--pds.connector.gpas-domain=" + EXAMPLE_DOMAIN,
                "--pds.connector.request-queue=req.PDS-EXAMPLE",
                "--pds.connector.pseudonyms.PSN-EXAMPLE-0001=internal-0001",
                "--pds.connector.pseudonyms.PSN-EXAMPLE-0002=internal-0002");

    // Second synthetic PDS site: own queue, own pseudonym domain, disjoint data.
    connectorBContext =
        new SpringApplicationBuilder(PdsExampleApplication.class)
            .run(
                "--spring.main.web-application-type=none",
                "--spring.rabbitmq.host=" + RABBIT.getHost(),
                "--spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
                "--spring.rabbitmq.username=" + RABBIT.getAdminUsername(),
                "--spring.rabbitmq.password=" + RABBIT.getAdminPassword(),
                "--pds.connector.pds-id=PDS-EXAMPLE-B",
                "--pds.connector.gpas-domain=" + EXAMPLE_B_DOMAIN,
                "--pds.connector.request-queue=req.PDS-EXAMPLE-B",
                "--pds.connector.pseudonyms.PSN-B-0001=internal-0002");

    brokerContext =
        new SpringApplicationBuilder(BrokerApplication.class)
            .run(
                "--server.port=0",
                "--management.server.port=-1",
                "--spring.rabbitmq.host=" + RABBIT.getHost(),
                "--spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
                "--spring.rabbitmq.username=" + RABBIT.getAdminUsername(),
                "--spring.rabbitmq.password=" + RABBIT.getAdminPassword(),
                "--broker.catalog-url=" + catalogUrl(),
                "--broker.aggregator-timeout-ms=" + TIMEOUT_MS);
    int port =
        ((ServletWebServerApplicationContext) brokerContext).getWebServer().getPort();
    brokerUrl = "http://localhost:" + port;
  }

  @AfterAll
  static void stopStack() {
    if (brokerContext != null) {
      brokerContext.close();
    }
    if (connectorBContext != null) {
      connectorBContext.close();
    }
    if (connectorContext != null) {
      connectorContext.close();
    }
  }

  @Test
  void federatedQueryAggregatesAcrossMultiplePdsSites() throws Exception {
    Bundle request =
        request(
            pseudonym("PSN-EXAMPLE-0001", EXAMPLE_DOMAIN), pseudonym("PSN-B-0001", EXAMPLE_B_DOMAIN));

    Bundle aggregated = postProcessMessage(request, 200);

    MessageHeader header = BrokerMessages.messageHeaderOf(aggregated).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    // 2 conditions from PDS-EXAMPLE (internal-0001) + 1 from PDS-EXAMPLE-B (internal-0002),
    // both sites answered -> no timeout warning.
    assertThat(resourcesOf(aggregated, Condition.class)).hasSize(3);
    assertThat(resourcesOf(aggregated, OperationOutcome.class)).isEmpty();
  }

  @Test
  void aggregatesConditionsFromAddressedConnector() throws Exception {
    // Unique response queue so the routed-delivery assertion below cannot
    // be satisfied by another test's message.
    String responseQueue = "responses.it-happy";
    org.springframework.amqp.rabbit.core.RabbitAdmin admin =
        brokerContext.getBean(org.springframework.amqp.rabbit.core.RabbitAdmin.class);
    admin.declareQueue(new org.springframework.amqp.core.Queue(responseQueue, true));

    Bundle request =
        request("amqp://rabbitmq/" + responseQueue, pseudonym("PSN-EXAMPLE-0001", EXAMPLE_DOMAIN));

    Bundle aggregated = postProcessMessage(request, 200);

    MessageHeader header = BrokerMessages.messageHeaderOf(aggregated).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(header.getResponse().getIdentifier())
        .isEqualTo(
            BrokerMessages.messageHeaderOf(request).orElseThrow().getIdElement().getIdPart());
    assertThat(resourcesOf(aggregated, Condition.class)).hasSize(2);
    assertThat(resourcesOf(aggregated, OperationOutcome.class)).isEmpty();
    // Re-bundled resources must not accumulate urn:uuid: prefixes on the wire.
    aggregated
        .getEntry()
        .forEach(entry -> assertThat(entry.getFullUrl()).doesNotContain("urn:uuid:urn:uuid:"));

    // ADR-009: the aggregated bundle must ALSO be delivered to the requester's
    // response queue, correlated via the AMQP correlationId.
    org.springframework.amqp.rabbit.core.RabbitTemplate rabbit =
        brokerContext.getBean(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
    org.springframework.amqp.core.Message routed = rabbit.receive(responseQueue, 5000);
    assertThat(routed).as("aggregated bundle routed to %s", responseQueue).isNotNull();
    String expectedCorrelation =
        "urn:uuid:"
            + BrokerMessages.messageHeaderOf(request).orElseThrow().getIdElement().getIdPart();
    assertThat(routed.getMessageProperties().getCorrelationId()).isEqualTo(expectedCorrelation);
    Bundle routedBundle =
        (Bundle)
            FHIR.newJsonParser()
                .parseResource(new String(routed.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    MessageHeader routedHeader = BrokerMessages.messageHeaderOf(routedBundle).orElseThrow();
    assertThat(routedHeader.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(resourcesOf(routedBundle, Condition.class)).hasSize(2);
  }

  @Test
  void flagsPartialResultWhenOneDomainStaysSilent() throws Exception {
    Bundle request =
        request(
            pseudonym("PSN-EXAMPLE-0002", EXAMPLE_DOMAIN),
            pseudonym("PSN-GHOST-1", UNKNOWN_DOMAIN));

    Bundle aggregated = postProcessMessage(request, 200);

    MessageHeader header = BrokerMessages.messageHeaderOf(aggregated).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(resourcesOf(aggregated, Condition.class)).hasSize(1);
    OperationOutcome outcome = resourcesOf(aggregated, OperationOutcome.class).get(0);
    assertThat(outcome.getIssueFirstRep().getSeverity())
        .isEqualTo(OperationOutcome.IssueSeverity.WARNING);
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getCode())
        .isEqualTo("timeout");
  }

  @Test
  void returnsFatalErrorWhenNoPdsIsAddressed() throws Exception {
    Bundle request = request(pseudonym("PSN-GHOST-1", UNKNOWN_DOMAIN));

    Bundle aggregated = postProcessMessage(request, 200);

    MessageHeader header = BrokerMessages.messageHeaderOf(aggregated).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.FATALERROR);
    assertThat(header.getEventUriType().getValue())
        .isEqualTo(BrokerProtocol.OPERATION_ERROR_EVENT);
    OperationOutcome outcome = resourcesOf(aggregated, OperationOutcome.class).get(0);
    assertThat(outcome.getIssueFirstRep().getSeverity())
        .isEqualTo(OperationOutcome.IssueSeverity.FATAL);
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getSystem())
        .isEqualTo(BrokerProtocol.ERROR_CODE_SYSTEM);
  }

  @Test
  void rejectsOperationsUnknownToTheCatalog() throws Exception {
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(new Identifier().setSystem(EXAMPLE_DOMAIN).setValue("PSN-EXAMPLE-0001"));
    Bundle request =
        BrokerMessages.requestBundle(
            "https://querybroker.example.org/fhir/OperationDefinition/NotInCatalog",
            "Integration Test",
            "amqp://rabbitmq/responses.portal",
            "amqp://rabbitmq/responses.portal",
            parameters);

    HttpResponse<String> response = post(request);

    assertThat(response.statusCode()).isEqualTo(400);
    OperationOutcome outcome =
        (OperationOutcome) FHIR.newJsonParser().parseResource(response.body());
    assertThat(outcome.getIssueFirstRep().getDiagnostics()).contains("NotInCatalog");
  }

  private static void seedCatalog() throws Exception {
    String json =
        Files.readString(Path.of("..", "catalog", "OperationDefinition", "GetConditions.json"));
    HttpResponse<String> response =
        HTTP.send(
            HttpRequest.newBuilder(URI.create(catalogUrl() + "/OperationDefinition"))
                .header("Content-Type", "application/fhir+json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("seed response: %s", response.body()).isBetween(200, 299);

    // Guard against silent seed failures: the operation must be findable by url.
    HttpResponse<String> search =
        HTTP.send(
            HttpRequest.newBuilder(
                    URI.create(
                        catalogUrl()
                            + "/OperationDefinition?url="
                            + java.net.URLEncoder.encode(
                                GET_CONDITIONS, java.nio.charset.StandardCharsets.UTF_8)))
                .header("Accept", "application/fhir+json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(search.body()).as("post-seed search: %s", search.body()).contains("GetConditions");
  }

  private static String catalogUrl() {
    return "http://" + CATALOG.getHost() + ":" + CATALOG.getMappedPort(8080) + "/fhir";
  }

  private static Bundle postProcessMessage(Bundle request, int expectedStatus) throws Exception {
    HttpResponse<String> response = post(request);
    assertThat(response.statusCode()).as("response body: %s", response.body())
        .isEqualTo(expectedStatus);
    return (Bundle) FHIR.newJsonParser().parseResource(response.body());
  }

  private static HttpResponse<String> post(Bundle request) throws Exception {
    return HTTP.send(
        HttpRequest.newBuilder(URI.create(brokerUrl + "/fhir/$process-message"))
            .header("Content-Type", "application/fhir+json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    FHIR.newJsonParser().encodeResourceToString(request)))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static Bundle request(Identifier... pseudonyms) {
    return request("amqp://rabbitmq/responses.portal", pseudonyms);
  }

  private static Bundle request(String destinationEndpoint, Identifier... pseudonyms) {
    Parameters parameters = new Parameters();
    for (Identifier pseudonym : pseudonyms) {
      parameters.addParameter().setName("pseudonym").setValue(pseudonym);
    }
    return BrokerMessages.requestBundle(
        GET_CONDITIONS,
        "Integration Test",
        destinationEndpoint,
        destinationEndpoint,
        parameters);
  }

  private static Identifier pseudonym(String value, String domain) {
    return new Identifier().setSystem(domain).setValue(value);
  }

  private static <T> java.util.List<T> resourcesOf(Bundle bundle, Class<T> type) {
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }
}
