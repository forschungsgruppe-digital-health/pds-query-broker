package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol.ErrorCode;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

class AbstractPdsConnectorTest {

  private static final String DOMAIN = "https://ths.example.org/gpas/domain/PDS-TEST";
  private static final String GET_CONDITIONS =
      "https://querybroker.example.org/fhir/OperationDefinition/GetConditions";

  private final TestConnector connector = new TestConnector();

  @Test
  void respondsWithResultsWhenAddressed() {
    Bundle request = request("PSN-1", DOMAIN);

    Optional<Bundle> response = connector.handle(request);

    assertThat(response).isPresent();
    MessageHeader header = BrokerMessages.messageHeaderOf(response.get()).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(header.getResponse().getIdentifier())
        .isEqualTo(BrokerMessages.messageHeaderOf(request).orElseThrow().getIdElement().getIdPart());
    assertThat(response.get().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(Condition.class::isInstance))
        .hasSize(1);
  }

  @Test
  void staysSilentWhenNotAddressed() {
    Bundle request = request("PSN-1", "https://ths.example.org/gpas/domain/OTHER");

    assertThat(connector.handle(request)).isEmpty();
  }

  @Test
  void staysSilentForUnsupportedOperation() {
    Bundle request = request("PSN-1", DOMAIN);
    BrokerMessages.messageHeaderOf(request)
        .orElseThrow()
        .setEvent(
            new org.hl7.fhir.r4.model.UriType(
                "https://querybroker.example.org/fhir/OperationDefinition/GetLabResults"));

    assertThat(connector.handle(request)).isEmpty();
  }

  @Test
  void handlerFailureProducesFatalErrorWithBrokerErrorCode() {
    Bundle request = request("PSN-UNRESOLVABLE", DOMAIN);

    Optional<Bundle> response = connector.handle(request);

    assertThat(response).isPresent();
    MessageHeader header = BrokerMessages.messageHeaderOf(response.get()).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.FATALERROR);
    assertThat(header.getEventUriType().getValue())
        .isEqualTo(BrokerProtocol.OPERATION_ERROR_EVENT);
    OperationOutcome outcome =
        response.get().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(OperationOutcome.class::isInstance)
            .map(OperationOutcome.class::cast)
            .findFirst()
            .orElseThrow();
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getCode())
        .isEqualTo(ErrorCode.PDS_ERROR.code());
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getSystem())
        .isEqualTo(BrokerProtocol.ERROR_CODE_SYSTEM);
  }

  private static Bundle request(String pseudonym, String domain) {
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("pseudonym").setValue(new Identifier()
        .setSystem(domain).setValue(pseudonym));
    return BrokerMessages.requestBundle(
        GET_CONDITIONS,
        "Test Portal",
        "amqp://rabbitmq/responses.portal",
        "amqp://rabbitmq/responses.portal",
        parameters);
  }

  private static final class TestConnector extends AbstractPdsConnector {

    private final ThsClient ths = new StaticMapThsClient(Map.of("PSN-1", "internal-1"));

    @Override
    public String getPdsId() {
      return "PDS-TEST";
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
            Condition condition = new Condition();
            condition.setId("cond-" + internalId);
            bundle.addEntry().setResource(condition);
            return bundle;
          });
    }

    @Override
    protected ThsClient thsClient() {
      return ths;
    }
  }
}
