package de.tudresden.fgdh.querybroker.broker;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Broker configuration. Property names match the environment variables the
 * compose stack already defines (BROKER_CATALOG_URL, BROKER_AGGREGATOR_TIMEOUT_MS).
 *
 * @param catalogUrl base URL of the FHIR catalog server
 * @param aggregatorTimeoutMs how long to wait for connector responses
 * @param replyQueue the broker's own response queue (responses.{systemId})
 * @param routingMode {@code topic} (default, ADR-006 rev.) routes each request
 *     only to the addressed sites via {@code pds.topic}; {@code fanout} keeps
 *     the legacy broadcast to {@code pds.broadcast}. Connectors dual-bind, so
 *     either mode works during migration.
 */
@ConfigurationProperties(prefix = "broker")
public record BrokerProperties(
    String catalogUrl, long aggregatorTimeoutMs, String replyQueue, RoutingMode routingMode) {

  /** Request-distribution topology. */
  public enum RoutingMode {
    TOPIC,
    FANOUT
  }

  public BrokerProperties {
    if (aggregatorTimeoutMs <= 0) {
      aggregatorTimeoutMs = 8000;
    }
    if (replyQueue == null || replyQueue.isBlank()) {
      replyQueue = "responses.broker";
    }
    if (routingMode == null) {
      routingMode = RoutingMode.TOPIC;
    }
  }
}
