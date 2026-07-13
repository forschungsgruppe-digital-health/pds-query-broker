package de.tudresden.fgdh.querybroker.pdsexample;

import de.tudresden.fgdh.querybroker.sdk.AbstractPdsConnector;
import de.tudresden.fgdh.querybroker.sdk.OperationHandler;
import de.tudresden.fgdh.querybroker.sdk.StaticMapThsClient;
import de.tudresden.fgdh.querybroker.sdk.ThsClient;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

/** Reference connector: serves GetConditions from the synthetic store. */
@Component
public class ExampleConnector extends AbstractPdsConnector {

  private final ConnectorProperties properties;
  private final ThsClient thsClient;
  private final SyntheticConditionStore store;

  public ExampleConnector(ConnectorProperties properties, SyntheticConditionStore store) {
    this.properties = properties;
    this.thsClient = new StaticMapThsClient(properties.pseudonyms());
    this.store = store;
  }

  @Override
  public String getPdsId() {
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
  protected ThsClient thsClient() {
    return thsClient;
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
