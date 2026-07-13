package de.tudresden.fgdh.querybroker.sdk;

import java.util.Map;
import java.util.Optional;

/** Static synthetic pseudonym map — the increment-1 stand-in for a real THS. */
public final class StaticMapThsClient implements ThsClient {

  private final Map<String, String> pseudonymToInternalId;

  public StaticMapThsClient(Map<String, String> pseudonymToInternalId) {
    this.pseudonymToInternalId = Map.copyOf(pseudonymToInternalId);
  }

  @Override
  public Optional<String> resolveToInternalId(String pseudonym) {
    return Optional.ofNullable(pseudonymToInternalId.get(pseudonym));
  }
}
