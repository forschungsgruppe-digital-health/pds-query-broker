package de.tudresden.fgdh.querybroker.pdsexample;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connector configuration (see CONTRIBUTING § 2). The record component names
 * mirror the {@code pds.connector.*} configuration keys (an external interface)
 * and are therefore kept in their short form.
 *
 * @param pdsId the primary-data-source identifier, e.g. PDS-EXAMPLE
 * @param gpasDomain the gPAS pseudonymization domain used for self-filtering
 * @param pseudonyms static pseudonym -&gt; internal-id map (increment-1 stand-in
 *     for a trusted third party)
 */
@ConfigurationProperties(prefix = "pds.connector")
public record ConnectorProperties(String pdsId, String gpasDomain, Map<String, String> pseudonyms) {

  public ConnectorProperties {
    pseudonyms = pseudonyms == null ? Map.of() : Map.copyOf(pseudonyms);
  }
}
