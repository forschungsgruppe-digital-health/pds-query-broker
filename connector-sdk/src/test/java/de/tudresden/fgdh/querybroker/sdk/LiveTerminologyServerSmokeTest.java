package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.junit.jupiter.api.Test;

/**
 * Live smoke test against a REAL FHIR terminology server (ADR-013) — for the
 * pilot the MII SU-TermServ (Ontoserver) over mutual TLS. It is opt-in and
 * therefore safe to commit: it SKIPS unless {@code TERMINOLOGY_SERVER_URL} is
 * set, so CI (which has no certificate) never runs it.
 *
 * <p>Run it locally after placing the issued certificate under {@code certs/}
 * and the password in {@code docker/.env}:
 *
 * <pre>
 *   set -a &amp;&amp; . docker/.env &amp;&amp; set +a
 *   TERMINOLOGY_SERVER_URL=https://ontoserver.mii-termserv.de/fhir \
 *   TERMINOLOGY_CLIENT_KEYSTORE=certs/funktionszertifikat_cert.p12 \
 *   ./gradlew :connector-sdk:test --tests '*LiveTerminologyServerSmokeTest'
 * </pre>
 */
class LiveTerminologyServerSmokeTest {

  private static final String KDS_DIAGNOSE =
      "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";
  // A real ICD-10-GM code (malignant neoplasm, upper lobe bronchus).
  private static final String REAL_ICD10GM_CODE = "C34.1";

  // Transport/handshake failure signatures — if any appears, the mTLS
  // connection to the terminology server did NOT work.
  private static final List<String> TRANSPORT_FAILURES =
      List.of(
          "SSLHandshake",
          "unable to find valid certification",
          "Failed to parse response",
          "Connection refused",
          "Connect timed out",
          "certificate_unknown",
          "handshake_failure");

  @Test
  void mutualTlsConnectionSucceedsAndTerminologyValidationIsLive() {
    Optional<TerminologyServerConfig> config = TerminologyServerConfig.fromEnvironment();
    assumeTrue(
        config.isPresent(),
        "TERMINOLOGY_SERVER_URL unset — live terminology smoke skipped (expected in CI).");

    CatalogProfileValidator validator =
        new CatalogProfileValidator(
            FhirContext.forR4(), CatalogProfileValidator.defaultCatalogDir(), config.get());

    // A real ICD-10-GM code: the mTLS transport must be healthy (no handshake/
    // connection errors), proving the client certificate authenticated.
    List<String> realCodeViolations = validator.validate(kdsCondition(REAL_ICD10GM_CODE), KDS_DIAGNOSE);
    assertThat(realCodeViolations)
        .as(
            "no mTLS/transport failure may appear when validating %s against %s",
            REAL_ICD10GM_CODE, config.get().baseUrl())
        .noneMatch(v -> TRANSPORT_FAILURES.stream().anyMatch(v::contains));

    // A bogus code: the LIVE server must actively reject it, and the rejection
    // must name the remote server — proving terminology validation is delegated
    // to the real terminology server over the authenticated channel.
    List<String> bogusViolations = validator.validate(kdsCondition("NOT-A-REAL-ICD-CODE"), KDS_DIAGNOSE);
    assertThat(bogusViolations)
        .as("bogus code must be rejected once terminology validation is live")
        .isNotEmpty();
    assertThat(bogusViolations)
        .as("the rejection must come from the remote terminology server")
        .anyMatch(v -> v.contains(config.get().baseUrl()) || v.contains("Remote Terminology"));
  }

  private static Condition kdsCondition(String icdCode) {
    Condition condition = new Condition();
    // No display (the terminology server holds the authoritative one); ICD-10-GM
    // edition 2025 is the version SU-TermServ resolves the codes in.
    condition
        .getCode()
        .addCoding(
            new Coding().setSystem("http://fhir.de/CodeSystem/bfarm/icd-10-gm").setCode(icdCode)
                .setVersion("2025"));
    condition.getSubject().setDisplay("Synthetic Testpatient (pseudonymized)");
    condition.setRecordedDateElement(new org.hl7.fhir.r4.model.DateTimeType("2025-01-15"));
    return condition;
  }
}
