package de.tudresden.fgdh.querybroker.broker;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Broker configuration. Property names match the environment variables the
 * compose stack already defines (BROKER_CATALOG_URL, BROKER_AGGREGATOR_TIMEOUT_MS).
 *
 * @param catalogUrl base URL of the FHIR catalog server
 * @param aggregatorTimeoutMs how long to wait for connector responses
 * @param replyQueue the broker's own response queue (responses.{systemId})
 */
@ConfigurationProperties(prefix = "broker")
public record BrokerProperties(String catalogUrl, long aggregatorTimeoutMs, String replyQueue) {

  public BrokerProperties {
    if (aggregatorTimeoutMs <= 0) {
      aggregatorTimeoutMs = 8000;
    }
    if (replyQueue == null || replyQueue.isBlank()) {
      replyQueue = "responses.broker";
    }
  }
}
