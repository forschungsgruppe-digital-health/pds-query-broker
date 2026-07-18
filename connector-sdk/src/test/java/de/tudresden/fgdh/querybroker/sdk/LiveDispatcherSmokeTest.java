package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Live smoke against a REAL fTTP FHIR dispatcher (gPAS $dePseudonymize). Opt-in
 * and safe to commit: SKIPS unless {@code DISPATCHER_BASE_URL} is set, so CI
 * never runs it.
 *
 * <p>Run it against the compose dispatcher (or any TTP-FHIR gateway):
 *
 * <pre>
 *   docker compose --profile ths up -d ttp-dispatcher   # or your gateway
 *   DISPATCHER_BASE_URL=http://localhost:8086 \
 *   DISPATCHER_TARGET_DOMAIN=psn \
 *   DISPATCHER_KNOWN_PSEUDONYM=psn_DEMO0001 \
 *   DISPATCHER_EXPECTED_ORIGINAL=1001000000022 \
 *   ./gradlew :connector-sdk:test --tests '*LiveDispatcherSmokeTest'
 * </pre>
 */
class LiveDispatcherSmokeTest {

  @Test
  void resolvesAKnownPseudonymAgainstTheRealDispatcher() {
    String baseUrl = System.getenv("DISPATCHER_BASE_URL");
    assumeTrue(
        baseUrl != null && !baseUrl.isBlank(),
        "DISPATCHER_BASE_URL unset — live dispatcher smoke skipped (expected in CI).");

    String targetDomain = envOrDefault("DISPATCHER_TARGET_DOMAIN", "psn");
    String pseudonym = envOrDefault("DISPATCHER_KNOWN_PSEUDONYM", "psn_DEMO0001");
    String expectedOriginal = envOrDefault("DISPATCHER_EXPECTED_ORIGINAL", "1001000000022");

    DispatcherTrustedThirdPartyClient client =
        new DispatcherTrustedThirdPartyClient(FhirContext.forR4(), baseUrl, targetDomain);

    Optional<String> resolved = client.resolveToInternalId(pseudonym);

    assertThat(resolved)
        .as("de-pseudonymize %s in domain %s against %s", pseudonym, targetDomain, baseUrl)
        .contains(expectedOriginal);
    assertThat(client.resolveToInternalId("psn_DEFINITELY_NOT_REAL")).isEmpty();
  }

  private static String envOrDefault(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
