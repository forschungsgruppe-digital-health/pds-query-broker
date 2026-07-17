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
 * @param targetProfiles operation code -&gt; canonical profile URL every result
 *     resource must conform to before sending (ADR-012); empty = validation
 *     skipped (the documented default)
 * @param validationCatalogDir directory holding the catalog mirror (+
 *     {@code packages/*.tgz}) that backs the profile validator; unset =
 *     no validator wired (a configured targetProfile then logs a warning
 *     and sends unvalidated)
 */
@ConfigurationProperties(prefix = "pds.connector")
public record ConnectorProperties(
    String pdsId,
    String gpasDomain,
    Map<String, String> pseudonyms,
    Map<String, String> targetProfiles,
    String validationCatalogDir) {

  public ConnectorProperties {
    pseudonyms = pseudonyms == null ? Map.of() : Map.copyOf(pseudonyms);
    targetProfiles = targetProfiles == null ? Map.of() : Map.copyOf(targetProfiles);
  }
}
