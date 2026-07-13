package de.tudresden.fgdh.querybroker.broker;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loads the operation catalog (OperationDefinitions) from the catalog server
 * and answers whether a requested event URI is a known operation. Lookups are
 * cached; a catalog miss is re-checked on every request so newly seeded
 * operations become visible without a broker restart.
 */
@Component
public class MessageDefinitionRegistry {

  private static final Logger log = LoggerFactory.getLogger(MessageDefinitionRegistry.class);

  private final FhirContext fhirContext;
  private final String catalogUrl;
  private final HttpClient http = HttpClient.newHttpClient();
  private final Map<String, OperationDefinition> cache = new ConcurrentHashMap<>();

  public MessageDefinitionRegistry(FhirContext fhirContext, BrokerProperties properties) {
    this.fhirContext = fhirContext;
    this.catalogUrl = properties.catalogUrl();
  }

  public Optional<OperationDefinition> findOperation(String canonicalUrl) {
    OperationDefinition cached = cache.get(canonicalUrl);
    if (cached != null) {
      return Optional.of(cached);
    }
    String searchUrl =
        catalogUrl
            + "/OperationDefinition?url="
            + URLEncoder.encode(canonicalUrl, StandardCharsets.UTF_8);
    try {
      HttpResponse<String> response =
          http.send(
              HttpRequest.newBuilder(URI.create(searchUrl))
                  .header("Accept", "application/fhir+json")
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new CatalogUnavailableException(
            "Catalog search returned HTTP " + response.statusCode() + " for " + searchUrl);
      }
      Bundle result = (Bundle) fhirContext.newJsonParser().parseResource(response.body());
      Optional<OperationDefinition> operation =
          result.getEntry().stream()
              .map(Bundle.BundleEntryComponent::getResource)
              .filter(OperationDefinition.class::isInstance)
              .map(OperationDefinition.class::cast)
              .findFirst();
      if (operation.isPresent()) {
        cache.put(canonicalUrl, operation.get());
        log.info("Catalog: loaded operation {} ({})", operation.get().getName(), canonicalUrl);
      } else {
        log.info(
            "Catalog: no OperationDefinition for {} (search total={})",
            canonicalUrl,
            result.hasTotal() ? result.getTotal() : "n/a");
      }
      return operation;
    } catch (IOException e) {
      throw new CatalogUnavailableException("Catalog not reachable: " + searchUrl, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CatalogUnavailableException("Interrupted while querying the catalog", e);
    }
  }

  /** The catalog server cannot be queried — a 5xx condition, not a client error. */
  public static final class CatalogUnavailableException extends RuntimeException {
    CatalogUnavailableException(String message) {
      super(message);
    }

    CatalogUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
