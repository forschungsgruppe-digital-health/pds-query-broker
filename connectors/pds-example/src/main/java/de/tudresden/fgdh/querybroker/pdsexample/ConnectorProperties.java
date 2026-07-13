package de.tudresden.fgdh.querybroker.pdsexample;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connector configuration (see CONTRIBUTING § 2).
 *
 * @param pdsId the PDS identifier, e.g. PDS-EXAMPLE
 * @param gpasDomain the gPAS pseudonymization domain used for self-filtering
 * @param pseudonyms static pseudonym -> internal-id map (increment-1 THS stand-in)
 */
@ConfigurationProperties(prefix = "pds.connector")
public record ConnectorProperties(String pdsId, String gpasDomain, Map<String, String> pseudonyms) {

  public ConnectorProperties {
    pseudonyms = pseudonyms == null ? Map.of() : Map.copyOf(pseudonyms);
  }
}
