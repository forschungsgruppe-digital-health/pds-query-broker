package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

class BrokerMessagesTest {

  private static final String EXAMPLE_DOMAIN = "https://ths.example.org/gpas/domain/PDS-EXAMPLE";
  private static final String EXAMPLE_B_DOMAIN =
      "https://ths.example.org/gpas/domain/PDS-EXAMPLE-B";
  private static final String OPERATION =
      "https://querybroker.example.org/fhir/OperationDefinition/GetConditions";

  private static Bundle requestWith(Parameters parameters) {
    return BrokerMessages.requestBundle(
        OPERATION,
        "Test",
        "amqp://rabbitmq/responses.portal",
        "amqp://rabbitmq/responses.portal",
        parameters);
  }

  private static Parameters pseudonym(Parameters p, String value, String domain) {
    p.addParameter().setName("pseudonym").setValue(new Identifier().setSystem(domain).setValue(value));
    return p;
  }

  private static String parametersFullUrl(Bundle bundle) {
    return bundle.getEntry().stream()
        .filter(e -> e.getResource() instanceof Parameters)
        .map(Bundle.BundleEntryComponent::getFullUrl)
        .findFirst()
        .orElseThrow();
  }

  @Test
  void requestBundleForSite_keepsOnlyOwnDomainPseudonymAndAllNonPseudonymParams() {
    Parameters parameters = new Parameters();
    pseudonym(parameters, "PSN-EXAMPLE-0001", EXAMPLE_DOMAIN);
    pseudonym(parameters, "PSN-B-0001", EXAMPLE_B_DOMAIN);
    parameters.addParameter().setName("since").setValue(new StringType("2026-01-01"));
    Bundle bundle = requestWith(parameters);

    Bundle copy = BrokerMessages.requestBundleForSite(bundle, "PDS-EXAMPLE");

    List<Identifier> kept = BrokerMessages.pseudonymsOf(BrokerMessages.parametersOf(copy).orElseThrow());
    assertThat(kept).hasSize(1);
    assertThat(kept.get(0).getSystem()).isEqualTo(EXAMPLE_DOMAIN);
    assertThat(kept.get(0).getValue()).isEqualTo("PSN-EXAMPLE-0001");
    // Non-pseudonym operation parameters must survive the trimming.
    assertThat(BrokerMessages.parametersOf(copy).orElseThrow().getParameter().stream()
            .anyMatch(pp -> "since".equals(pp.getName())))
        .isTrue();
  }

  @Test
  void requestBundleForSite_doesNotMutateTheSourceBundle() {
    Parameters parameters = new Parameters();
    pseudonym(parameters, "PSN-EXAMPLE-0001", EXAMPLE_DOMAIN);
    pseudonym(parameters, "PSN-B-0001", EXAMPLE_B_DOMAIN);
    Bundle bundle = requestWith(parameters);

    BrokerMessages.requestBundleForSite(bundle, "PDS-EXAMPLE");

    // Original still carries BOTH pseudonyms — the copy was filtered, not the source.
    assertThat(BrokerMessages.pseudonymsOf(BrokerMessages.parametersOf(bundle).orElseThrow()))
        .hasSize(2);
  }

  @Test
  void requestBundleForSite_preservesMessageHeaderAndFocusReference() {
    Parameters parameters = new Parameters();
    pseudonym(parameters, "PSN-EXAMPLE-0001", EXAMPLE_DOMAIN);
    pseudonym(parameters, "PSN-B-0001", EXAMPLE_B_DOMAIN);
    Bundle bundle = requestWith(parameters);

    Bundle copy = BrokerMessages.requestBundleForSite(bundle, "PDS-EXAMPLE");

    MessageHeader header = BrokerMessages.messageHeaderOf(copy).orElseThrow();
    // focus still points at the (now trimmed) Parameters entry — reference intact.
    assertThat(header.getFocusFirstRep().getReference()).isEqualTo(parametersFullUrl(copy));
    assertThat(header.getEventUriType().getValue()).isEqualTo(OPERATION);
  }

  @Test
  void requestBundleForSite_dropsMalformedAndNoDomainPseudonyms() {
    Parameters parameters = new Parameters();
    pseudonym(parameters, "PSN-EXAMPLE-0001", EXAMPLE_DOMAIN); // valid own-domain — kept
    // malformed: a "pseudonym" param whose value is not an Identifier
    parameters.addParameter().setName("pseudonym").setValue(new StringType("not-an-identifier"));
    // no-domain: blank system maps to "" and belongs to no site
    parameters.addParameter().setName("pseudonym").setValue(new Identifier().setValue("PSN-ORPHAN"));
    Bundle bundle = requestWith(parameters);

    Bundle copy = BrokerMessages.requestBundleForSite(bundle, "PDS-EXAMPLE");

    List<Identifier> kept = BrokerMessages.pseudonymsOf(BrokerMessages.parametersOf(copy).orElseThrow());
    assertThat(kept).hasSize(1);
    assertThat(kept.get(0).getValue()).isEqualTo("PSN-EXAMPLE-0001");
    // the malformed StringType "pseudonym" must not survive either
    assertThat(BrokerMessages.parametersOf(copy).orElseThrow().getParameter().stream()
            .filter(pp -> "pseudonym".equals(pp.getName()))
            .count())
        .isEqualTo(1);
  }
}
