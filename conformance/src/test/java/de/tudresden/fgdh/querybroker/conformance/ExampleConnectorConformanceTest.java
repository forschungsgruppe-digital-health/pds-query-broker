package de.tudresden.fgdh.querybroker.conformance;

import de.tudresden.fgdh.querybroker.sdk.AbstractPdsConnector;
import de.tudresden.fgdh.querybroker.sdk.OperationHandler;
import de.tudresden.fgdh.querybroker.sdk.StaticMapThsClient;
import de.tudresden.fgdh.querybroker.sdk.ThsClient;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;

/**
 * The reference connector's own conformance run — also the living example of
 * how a PDS team wires {@link PdsConnectorConformanceTest} to its connector
 * (CONTRIBUTING § 3). Uses a self-contained synthetic connector equivalent to
 * connectors/pds-example so the harness stays free of Spring dependencies.
 */
class ExampleConnectorConformanceTest extends PdsConnectorConformanceTest {

  private static final String DOMAIN = "https://ths.example.org/gpas/domain/PDS-CONFORMANCE";

  private final AbstractPdsConnector connector =
      new AbstractPdsConnector() {
        private final ThsClient ths =
            new StaticMapThsClient(
                Map.of(
                    "PSN-CONF-0001", "internal-0001",
                    "PSN-CONF-EMPTY", "internal-nodata"));

        @Override
        public String getPdsId() {
          return "PDS-CONFORMANCE";
        }

        @Override
        public String getGpasDomain() {
          return DOMAIN;
        }

        @Override
        public Map<String, OperationHandler> getHandlers() {
          return Map.of(
              "GetConditions",
              (internalId, params) -> {
                Bundle bundle = new Bundle();
                bundle.setType(Bundle.BundleType.COLLECTION);
                if ("internal-0001".equals(internalId)) {
                  Condition condition = new Condition();
                  condition.setId(UUID.randomUUID().toString());
                  condition.setCode(
                      new CodeableConcept()
                          .addCoding(
                              new Coding(
                                  "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
                                  "C34.1",
                                  "Synthetic conformance condition")));
                  condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
                  bundle.addEntry().setResource(condition);
                }
                return bundle;
              });
        }

        @Override
        protected ThsClient thsClient() {
          return ths;
        }
      };

  @Override
  protected AbstractPdsConnector connector() {
    return connector;
  }

  @Override
  protected String knownPseudonym() {
    return "PSN-CONF-0001";
  }

  @Override
  protected String emptyResultPseudonym() {
    return "PSN-CONF-EMPTY";
  }

  @Override
  protected String unresolvablePseudonym() {
    return "PSN-CONF-UNKNOWN";
  }

  @Override
  protected String supportedOperation() {
    return CatalogValidator.CANONICAL_BASE + "/OperationDefinition/GetConditions";
  }
}
