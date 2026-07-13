package de.tudresden.fgdh.querybroker.sdk;

import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol.ErrorCode;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UriType;

/**
 * Builders and accessors for the message bundles of the Query Broker protocol
 * (profiles BrokerRequestBundle / BrokerResponseBundle — see ig/input/fsh/).
 */
public final class BrokerMessages {

  private BrokerMessages() {}

  public static Optional<MessageHeader> messageHeaderOf(Bundle bundle) {
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(MessageHeader.class::isInstance)
        .map(MessageHeader.class::cast)
        .findFirst();
  }

  public static Optional<Parameters> parametersOf(Bundle bundle) {
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Parameters.class::isInstance)
        .map(Parameters.class::cast)
        .findFirst();
  }

  /** All pseudonym identifiers of the request (profile slice `pseudonym`, 1..*). */
  public static List<Identifier> pseudonymsOf(Parameters parameters) {
    return parameters.getParameter().stream()
        .filter(p -> "pseudonym".equals(p.getName()))
        .map(Parameters.ParametersParameterComponent::getValue)
        .filter(Identifier.class::isInstance)
        .map(Identifier.class::cast)
        .toList();
  }

  /**
   * Builds a request message bundle (profile BrokerRequestBundle): MessageHeader
   * (no response element) + Parameters carrying pseudonyms and operation
   * parameters. {@code destinationEndpoint} is the requesting system's response
   * queue per ADR-009 (e.g. {@code amqp://rabbitmq/responses.portal}).
   */
  public static Bundle requestBundle(
      String operationCanonicalUrl,
      String requesterName,
      String requesterEndpoint,
      String destinationEndpoint,
      Parameters parameters) {
    MessageHeader header = new MessageHeader();
    header.setId(UUID.randomUUID().toString());
    header.setEvent(new UriType(operationCanonicalUrl));
    header.addDestination().setName(requesterName).setEndpoint(destinationEndpoint);
    header.getSource().setName(requesterName).setEndpoint(requesterEndpoint);
    if (parameters.getIdElement().isEmpty()) {
      parameters.setId(UUID.randomUUID().toString());
    }

    Bundle bundle = new Bundle();
    bundle.setType(BundleType.MESSAGE);
    bundle.setTimestamp(new Date());
    addEntry(bundle, header);
    String parametersUrl = addEntry(bundle, parameters);
    header.addFocus().setReference(parametersUrl);
    return bundle;
  }

  /**
   * Builds a response message bundle (profile BrokerResponseBundle): a
   * MessageHeader with the mandatory response element first, then the payload
   * resources as further entries.
   */
  public static Bundle responseBundle(
      MessageHeader requestHeader,
      String sourceName,
      String sourceEndpoint,
      ResponseType responseCode,
      List<Resource> payload) {
    MessageHeader header = new MessageHeader();
    header.setId(UUID.randomUUID().toString());
    if (ResponseType.FATALERROR == responseCode) {
      header.setEvent(new UriType(BrokerProtocol.OPERATION_ERROR_EVENT));
      header.setDefinition(BrokerProtocol.OPERATION_ERROR_MESSAGE_DEFINITION);
    } else {
      header.setEvent(requestHeader.getEventUriType().copy());
    }
    requestHeader.getDestination().forEach(d -> header.addDestination(d.copy()));
    header.getSource().setName(sourceName).setEndpoint(sourceEndpoint);
    header.getResponse().setIdentifier(plainId(requestHeader));
    header.getResponse().setCode(responseCode);

    Bundle bundle = new Bundle();
    bundle.setType(BundleType.MESSAGE);
    bundle.setTimestamp(new Date());
    addEntry(bundle, header);
    for (Resource resource : payload) {
      String fullUrl = addEntry(bundle, resource);
      header.addFocus().setReference(fullUrl);
    }
    return bundle;
  }

  /** An OperationOutcome issue per the BrokerOperationOutcome profile. */
  public static OperationOutcome operationOutcome(
      IssueSeverity severity, IssueType issueType, ErrorCode errorCode, String diagnostics) {
    OperationOutcome outcome = new OperationOutcome();
    outcome.setId(UUID.randomUUID().toString());
    OperationOutcome.OperationOutcomeIssueComponent issue = outcome.addIssue();
    issue.setSeverity(severity);
    issue.setCode(issueType);
    issue.setDetails(
        new CodeableConcept()
            .addCoding(
                new org.hl7.fhir.r4.model.Coding(
                    BrokerProtocol.ERROR_CODE_SYSTEM, errorCode.code(), errorCode.display())));
    issue.setDiagnostics(diagnostics);
    return outcome;
  }

  /**
   * The plain id of a resource. HAPI re-derives resource ids from the entry
   * fullUrl ({@code urn:uuid:...}) when parsing a bundle — the profile's
   * {@code response.identifier} and re-bundled resource ids/fullUrls must not
   * accumulate that prefix (FHIR ids may not contain colons).
   */
  public static String plainId(Resource resource) {
    String id = resource.getIdElement().getIdPart();
    return id != null && id.startsWith("urn:uuid:") ? id.substring("urn:uuid:".length()) : id;
  }

  private static String addEntry(Bundle bundle, Resource resource) {
    String plain = plainId(resource);
    if (plain == null) {
      plain = UUID.randomUUID().toString();
    }
    resource.setId(plain);
    String fullUrl = "urn:uuid:" + plain;
    bundle.addEntry().setFullUrl(fullUrl).setResource(resource);
    return fullUrl;
  }
}
