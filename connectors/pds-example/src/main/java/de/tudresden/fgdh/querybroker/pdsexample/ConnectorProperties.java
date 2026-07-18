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
 * @param terminology optional remote FHIR terminology server (ADR-013);
 *     unset = terminology/binding checks stay disabled (structural
 *     validation only)
 * @param ths trusted-third-party (pseudonym resolution) settings; the mode
 *     toggle selects the static map (default) or the fTTP dispatcher
 */
@ConfigurationProperties(prefix = "pds.connector")
public record ConnectorProperties(
    String pdsId,
    String gpasDomain,
    Map<String, String> pseudonyms,
    Map<String, String> targetProfiles,
    String validationCatalogDir,
    TerminologyProperties terminology,
    ThsProperties ths) {

  public ConnectorProperties {
    pseudonyms = pseudonyms == null ? Map.of() : Map.copyOf(pseudonyms);
    targetProfiles = targetProfiles == null ? Map.of() : Map.copyOf(targetProfiles);
    ths = ths == null ? ThsProperties.staticDefault() : ths;
  }

  /**
   * Generic terminology-server settings — works for the MII SU-TermServ (mTLS
   * client certificate from the onboarding) as well as any other FHIR
   * terminology server such as a CSIRO Ontoserver (leave the keystore fields
   * unset when the server does not use mutual TLS).
   */
  public record TerminologyProperties(
      String serverUrl,
      String clientKeystore,
      String clientKeystorePassword,
      String truststore,
      String truststorePassword) {}

  /**
   * Trusted-third-party (THS) pseudonym-resolution settings. Feature toggle
   * {@code mode}: {@code STATIC} (default — the synthetic increment-1 map) or
   * {@code DISPATCHER} (the fTTP FHIR dispatcher / THS Greifswald TTP-FHIR
   * gateway gPAS module). The dispatcher fields are required only in
   * {@code DISPATCHER} mode.
   *
   * @param mode STATIC | DISPATCHER
   * @param dispatcherBaseUrl TTP-FHIR gateway base (e.g. http://ttp-dispatcher:8080)
   * @param targetDomain the gPAS domain name pseudonyms belong to
   */
  public record ThsProperties(Mode mode, String dispatcherBaseUrl, String targetDomain) {

    public enum Mode {
      STATIC,
      DISPATCHER
    }

    public ThsProperties {
      if (mode == null) {
        mode = Mode.STATIC;
      }
    }

    static ThsProperties staticDefault() {
      return new ThsProperties(Mode.STATIC, null, null);
    }
  }
}
