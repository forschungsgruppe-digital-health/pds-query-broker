package de.tudresden.fgdh.querybroker.sdk;

import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for primary-data-source (PDS — <i>Primärdatenquelle</i>)
 * connectors: parses request message bundles, self-filters by pseudonymization
 * domain (broadcast + self-filtering, ADR-006), dispatches to the registered
 * {@link OperationHandler}, and assembles the response message bundle (profile
 * BrokerResponseBundle).
 *
 * <p>Self-filtering contract: a connector that is not addressed (no pseudonym
 * of its gPAS domain) or does not support the requested operation stays
 * silent — {@link #handle(Bundle)} returns {@link Optional#empty()} and no
 * response is published. Handler failures DO produce a fatal-error response
 * with a machine-readable BrokerOperationOutcome.
 */
public abstract class AbstractPrimaryDataSourceConnector {

  private static final Logger log =
      LoggerFactory.getLogger(AbstractPrimaryDataSourceConnector.class);

  /** The primary-data-source identifier, e.g. {@code PDS-EXAMPLE}. */
  public abstract String getPrimaryDataSourceId();

  /** The gPAS pseudonymization domain this connector self-filters on. */
  public abstract String getGpasDomain();

  /** Operation handlers keyed by the PascalCase OperationDefinition code. */
  public abstract Map<String, OperationHandler> getHandlers();

  /** The site-local trusted-third-party client used to resolve pseudonyms. */
  protected abstract TrustedThirdPartyClient trustedThirdPartyClient();

  /** AMQP endpoint reported as MessageHeader.source in responses. */
  protected String sourceEndpoint() {
    return "amqp://rabbitmq/" + BrokerProtocol.REQUEST_QUEUE_PREFIX + getPrimaryDataSourceId();
  }

  /**
   * Processes a request message bundle. Empty result = self-filtered (not
   * addressed or operation not supported) — protocol-conformant silence.
   */
  public Optional<Bundle> handle(Bundle requestBundle) {
    MessageHeader requestHeader =
        BrokerMessages.messageHeaderOf(requestBundle)
            .orElseThrow(() -> new IllegalArgumentException("Request bundle has no MessageHeader"));
    Parameters parameters =
        BrokerMessages.parametersOf(requestBundle)
            .orElseThrow(() -> new IllegalArgumentException("Request bundle has no Parameters"));

    Optional<Identifier> ownPseudonym =
        BrokerMessages.pseudonymsOf(parameters).stream()
            .filter(id -> getGpasDomain().equals(id.getSystem()))
            .findFirst();
    if (ownPseudonym.isEmpty()) {
      log.debug("{}: not addressed (no pseudonym for domain {}), staying silent",
          getPrimaryDataSourceId(), getGpasDomain());
      return Optional.empty();
    }

    String operation = operationCodeOf(requestHeader);
    OperationHandler handler = getHandlers().get(operation);
    if (handler == null) {
      log.debug("{}: operation {} not supported, staying silent",
          getPrimaryDataSourceId(), operation);
      return Optional.empty();
    }

    try {
      String internalId =
          trustedThirdPartyClient()
              .resolveToInternalId(ownPseudonym.get().getValue())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Pseudonym cannot be resolved by the local trusted third party"));
      Bundle result = handler.execute(internalId, parameters);
      List<Resource> payload =
          result.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
      return Optional.of(
          BrokerMessages.responseBundle(
              requestHeader,
              getPrimaryDataSourceId() + " Connector",
              sourceEndpoint(),
              ResponseType.OK,
              payload));
    } catch (Exception e) {
      log.warn("{}: handler for {} failed", getPrimaryDataSourceId(), operation, e);
      return Optional.of(
          BrokerMessages.responseBundle(
              requestHeader,
              getPrimaryDataSourceId() + " Connector",
              sourceEndpoint(),
              ResponseType.FATALERROR,
              List.of(
                  BrokerMessages.operationOutcome(
                      IssueSeverity.ERROR,
                      IssueType.EXCEPTION,
                      ErrorCode.PDS_ERROR,
                      getPrimaryDataSourceId() + ": " + e.getMessage()))));
    }
  }

  /** PascalCase operation code = last path segment of the eventUri canonical. */
  static String operationCodeOf(MessageHeader header) {
    String eventUri = header.getEventUriType().getValue();
    return eventUri.substring(eventUri.lastIndexOf('/') + 1);
  }
}
