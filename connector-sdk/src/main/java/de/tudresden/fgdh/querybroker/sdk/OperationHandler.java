package de.tudresden.fgdh.querybroker.sdk;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;

/**
 * Contract for a PDS-local operation implementation.
 *
 * <p>Receives the site-local pseudonym (already filtered to this connector's
 * pseudonymization domain) and the request {@link Parameters}; returns a
 * collection {@link Bundle} of result resources. The bundle entries are
 * unwrapped into the response message bundle by {@link AbstractPdsConnector}.
 */
@FunctionalInterface
public interface OperationHandler {

  Bundle execute(String pseudonym, Parameters parameters);
}
