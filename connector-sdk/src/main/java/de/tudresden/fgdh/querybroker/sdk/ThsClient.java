package de.tudresden.fgdh.querybroker.sdk;

import java.util.Optional;

/**
 * Port to the site-local trusted third party (THS).
 *
 * <p>Resolves a site pseudonym (gPAS) to the internal identifier of the local
 * data system. Increment 1 backs this with a static map
 * ({@link StaticMapThsClient}); increment 2 swaps in a gPAS dev-container
 * implementation behind the same port (ADR-011) — a configuration change,
 * never a code change.
 */
public interface ThsClient {

  Optional<String> resolveToInternalId(String pseudonym);
}
