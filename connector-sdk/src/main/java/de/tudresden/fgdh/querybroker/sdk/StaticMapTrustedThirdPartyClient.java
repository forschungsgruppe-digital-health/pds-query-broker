package de.tudresden.fgdh.querybroker.sdk;

import java.util.Map;
import java.util.Optional;

/**
 * Static synthetic pseudonym map — the increment-1 stand-in for a real trusted
 * third party (THS — <i>Treuhandstelle</i>).
 */
public final class StaticMapTrustedThirdPartyClient implements TrustedThirdPartyClient {

  private final Map<String, String> pseudonymToInternalId;

  public StaticMapTrustedThirdPartyClient(Map<String, String> pseudonymToInternalId) {
    this.pseudonymToInternalId = Map.copyOf(pseudonymToInternalId);
  }

  @Override
  public Optional<String> resolveToInternalId(String pseudonym) {
    return Optional.ofNullable(pseudonymToInternalId.get(pseudonym));
  }
}
