package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

/**
 * Enforcement of the documented contract: "the stub validates before sending;
 * without a targetProfile, validation is skipped". Uses a fake
 * {@link ProfileValidator} so the paths are deterministic and fast (the real
 * HAPI-backed validator is covered by {@link CatalogProfileValidatorTest}).
 */
class TargetProfileValidationTest {

  private static final String DOMAIN = "https://ths.example.org/gpas/domain/PDS-TEST";
  private static final String GET_CONDITIONS =
      "https://querybroker.example.org/fhir/OperationDefinition/GetConditions";
  private static final String PROFILE =
      "https://querybroker.example.org/fhir/StructureDefinition/SomeConditionProfile";

  @Test
  void validResultIsSentAsOk() {
    RecordingValidator validator = new RecordingValidator(List.of()); // no violations
    Optional<Bundle> response = new ValidatingConnector(PROFILE, validator).handle(request());

    MessageHeader header = BrokerMessages.messageHeaderOf(response.orElseThrow()).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.OK);
    assertThat(conditionsOf(response.get())).hasSize(1);
    assertThat(validator.calls).isEqualTo(1);
  }

  @Test
  void invalidResultIsRejectedWithValidationErrorAndNeverSentAsData() {
    RecordingValidator validator = new RecordingValidator(List.of("Condition.code: minimum required = 1"));
    Optional<Bundle> response = new ValidatingConnector(PROFILE, validator).handle(request());

    MessageHeader header = BrokerMessages.messageHeaderOf(response.orElseThrow()).orElseThrow();
    assertThat(header.getResponse().getCode()).isEqualTo(ResponseType.FATALERROR);
    assertThat(header.getEventUriType().getValue()).isEqualTo(BrokerProtocol.OPERATION_ERROR_EVENT);
    // The non-conformant Condition must NOT be on the wire — only the OperationOutcome.
    assertThat(conditionsOf(response.get())).isEmpty();
    OperationOutcome outcome =
        response.get().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(OperationOutcome.class::isInstance)
            .map(OperationOutcome.class::cast)
            .findFirst()
            .orElseThrow();
    assertThat(outcome.getIssueFirstRep().getDetails().getCodingFirstRep().getCode())
        .isEqualTo(BrokerProtocol.ErrorCode.VALIDATION_ERROR.code());
  }

  @Test
  void targetProfileConfiguredButNoValidatorFailsOpen() {
    // profile set, validator == null -> unvalidated but sent (with a warning).
    Optional<Bundle> response = new ValidatingConnector(PROFILE, null).handle(request());

    assertThat(BrokerMessages.messageHeaderOf(response.orElseThrow()).orElseThrow().getResponse()
            .getCode())
        .isEqualTo(ResponseType.OK);
    assertThat(conditionsOf(response.get())).hasSize(1);
  }

  @Test
  void noTargetProfileSkipsValidationEntirely() {
    RecordingValidator validator = new RecordingValidator(List.of("should never be called"));
    Optional<Bundle> response = new ValidatingConnector(null, validator).handle(request());

    assertThat(BrokerMessages.messageHeaderOf(response.orElseThrow()).orElseThrow().getResponse()
            .getCode())
        .isEqualTo(ResponseType.OK);
    assertThat(validator.calls).isZero();
  }

  private static List<Condition> conditionsOf(Bundle bundle) {
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Condition.class::isInstance)
        .map(Condition.class::cast)
        .toList();
  }

  private static Bundle request() {
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("pseudonym")
        .setValue(new Identifier().setSystem(DOMAIN).setValue("PSN-1"));
    return BrokerMessages.requestBundle(
        GET_CONDITIONS,
        "Test",
        "amqp://rabbitmq/responses.portal",
        "amqp://rabbitmq/responses.portal",
        parameters);
  }

  private static final class RecordingValidator implements ProfileValidator {
    private final List<String> result;
    private int calls;

    RecordingValidator(List<String> result) {
      this.result = result;
    }

    @Override
    public List<String> validate(IBaseResource resource, String profileUrl) {
      calls++;
      return new ArrayList<>(result);
    }
  }

  private static final class ValidatingConnector extends AbstractPrimaryDataSourceConnector {
    private final String profile;
    private final ProfileValidator validator;
    private final TrustedThirdPartyClient trustedThirdParty =
        new StaticMapTrustedThirdPartyClient(Map.of("PSN-1", "internal-1"));

    ValidatingConnector(String profile, ProfileValidator validator) {
      this.profile = profile;
      this.validator = validator;
    }

    @Override
    public String getPrimaryDataSourceId() {
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
            bundle.addEntry().setResource(new Condition().setId("cond-" + internalId));
            return bundle;
          });
    }

    @Override
    protected TrustedThirdPartyClient trustedThirdPartyClient() {
      return trustedThirdParty;
    }

    @Override
    protected Optional<String> targetProfile(String operation) {
      return Optional.ofNullable(profile);
    }

    @Override
    protected ProfileValidator profileValidator() {
      return validator;
    }
  }
}
