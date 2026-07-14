package de.tudresden.fgdh.querybroker.pdsexample;

import de.tudresden.fgdh.querybroker.sdk.AbstractPrimaryDataSourceConnector;
import de.tudresden.fgdh.querybroker.sdk.OperationHandler;
import de.tudresden.fgdh.querybroker.sdk.StaticMapTrustedThirdPartyClient;
import de.tudresden.fgdh.querybroker.sdk.TrustedThirdPartyClient;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

/** Reference connector: serves GetConditions from the synthetic store. */
@Component
public class ExampleConnector extends AbstractPrimaryDataSourceConnector {

  private final ConnectorProperties properties;
  private final TrustedThirdPartyClient trustedThirdPartyClient;
  private final SyntheticConditionStore store;

  public ExampleConnector(ConnectorProperties properties, SyntheticConditionStore store) {
    this.properties = properties;
    this.trustedThirdPartyClient = new StaticMapTrustedThirdPartyClient(properties.pseudonyms());
    this.store = store;
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

  private OperationHandler getConditionsHandler() {
    return (internalId, parameters) -> {
      Bundle bundle = new Bundle();
      bundle.setType(Bundle.BundleType.COLLECTION);
      store.findByInternalId(internalId).forEach(c -> bundle.addEntry().setResource(c));
      return bundle;
    };
  }
}
