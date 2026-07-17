package de.tudresden.fgdh.querybroker.sdk;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;

/**
 * Contract for an operation implementation local to a primary data source
 * (PDS — <i>Primärdatenquelle</i>).
 *
 * <p>Receives the site-local pseudonym (already filtered to this connector's
 * pseudonymization domain) and the request {@link Parameters}; returns a
 * collection {@link Bundle} of result resources. The bundle entries are
 * unwrapped into the response message bundle by
 * {@link AbstractPrimaryDataSourceConnector}.
 */
@FunctionalInterface
public interface OperationHandler {

  Bundle execute(String pseudonym, Parameters parameters);
}
