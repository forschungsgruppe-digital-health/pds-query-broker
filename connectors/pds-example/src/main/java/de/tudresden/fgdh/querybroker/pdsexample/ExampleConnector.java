package de.tudresden.fgdh.querybroker.pdsexample;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.sdk.AbstractPrimaryDataSourceConnector;
import de.tudresden.fgdh.querybroker.sdk.CatalogProfileValidator;
import de.tudresden.fgdh.querybroker.sdk.DispatcherTrustedThirdPartyClient;
import de.tudresden.fgdh.querybroker.sdk.OperationHandler;
import de.tudresden.fgdh.querybroker.sdk.ProfileValidator;
import de.tudresden.fgdh.querybroker.sdk.StaticMapTrustedThirdPartyClient;
import de.tudresden.fgdh.querybroker.sdk.TerminologyServerConfig;
import de.tudresden.fgdh.querybroker.sdk.TrustedThirdPartyClient;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

/** Reference connector: serves GetConditions from the synthetic store. */
@Component
public class ExampleConnector extends AbstractPrimaryDataSourceConnector {

  private final ConnectorProperties properties;
  private final TrustedThirdPartyClient trustedThirdPartyClient;
  private final SyntheticConditionStore store;
  private final ProfileValidator profileValidator;

  public ExampleConnector(ConnectorProperties properties, SyntheticConditionStore store) {
    this.properties = properties;
    this.trustedThirdPartyClient = trustedThirdPartyClient(properties);
    this.store = store;
    this.profileValidator =
        properties.validationCatalogDir() == null || properties.validationCatalogDir().isBlank()
            ? null
            : new CatalogProfileValidator(
                FhirContext.forR4(),
                Path.of(properties.validationCatalogDir()),
                terminologyServerConfig(properties));
  }

  /**
   * Feature toggle (pds.connector.ths.mode): STATIC uses the synthetic
   * pseudonym map; DISPATCHER resolves pseudonyms through the fTTP FHIR
   * dispatcher (gPAS $dePseudonymize).
   */
  private static TrustedThirdPartyClient trustedThirdPartyClient(ConnectorProperties properties) {
    ConnectorProperties.ThsProperties ths = properties.ths();
    if (ths.mode() == ConnectorProperties.ThsProperties.Mode.DISPATCHER) {
      if (ths.dispatcherBaseUrl() == null || ths.dispatcherBaseUrl().isBlank()) {
        throw new IllegalStateException(
            "pds.connector.ths.mode=DISPATCHER requires pds.connector.ths.dispatcher-base-url");
      }
      String targetDomain =
          ths.targetDomain() == null || ths.targetDomain().isBlank()
              ? properties.pdsId()
              : ths.targetDomain();
      return new DispatcherTrustedThirdPartyClient(
          FhirContext.forR4(), ths.dispatcherBaseUrl(), targetDomain);
    }
    return new StaticMapTrustedThirdPartyClient(properties.pseudonyms());
  }

  private static TerminologyServerConfig terminologyServerConfig(ConnectorProperties properties) {
    ConnectorProperties.TerminologyProperties terminology = properties.terminology();
    if (terminology == null
        || terminology.serverUrl() == null
        || terminology.serverUrl().isBlank()) {
      return null;
    }
    return new TerminologyServerConfig(
        terminology.serverUrl(),
        terminology.clientKeystore() == null || terminology.clientKeystore().isBlank()
            ? null
            : Path.of(terminology.clientKeystore()),
        terminology.clientKeystorePassword(),
        terminology.truststore() == null || terminology.truststore().isBlank()
            ? null
            : Path.of(terminology.truststore()),
        terminology.truststorePassword());
  }

  @Override
  public String getPrimaryDataSourceId() {
    // properties.pdsId() mirrors the config key pds.connector.pds-id (kept).
    return properties.pdsId();
  }

  @Override
  public String getGpasDomain() {
    return properties.gpasDomain();
  }

  @Override
  public Map<String, OperationHandler> getHandlers() {
    return Map.of("GetConditions", getConditionsHandler());
  }

  @Override
  protected TrustedThirdPartyClient trustedThirdPartyClient() {
    return trustedThirdPartyClient;
  }

  @Override
  protected Optional<String> targetProfile(String operation) {
    return Optional.ofNullable(properties.targetProfiles().get(operation));
  }

  @Override
  protected ProfileValidator profileValidator() {
    return profileValidator;
  }

  private OperationHandler getConditionsHandler() {
    return (internalId, parameters) -> {
      Bundle bundle = new Bundle();
      bundle.setType(Bundle.BundleType.COLLECTION);
      store.findByInternalId(internalId).forEach(c -> bundle.addEntry().setResource(c));
      return bundle;
    };
  }
}
