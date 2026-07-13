package de.tudresden.fgdh.querybroker.broker;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.sdk.BrokerMessages;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol.ErrorCode;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FHIR messaging ingress: {@code POST /fhir/$process-message} accepts a
 * BrokerRequestBundle and synchronously returns the aggregated response
 * bundle (the same bundle is also published to the requester's response
 * queue per ADR-009 — the HTTP response is increment-1 convenience for the
 * BFF and for tests).
 */
@RestController
public class MessagingController {

  private static final String FHIR_JSON = "application/fhir+json";

  private final QueryBrokerService service;
  private final FhirContext fhirContext;

  public MessagingController(QueryBrokerService service, FhirContext fhirContext) {
    this.service = service;
    this.fhirContext = fhirContext;
  }

  @PostMapping(
      path = "/fhir/$process-message",
      consumes = {FHIR_JSON, "application/json"},
      produces = FHIR_JSON)
  public ResponseEntity<String> processMessage(@RequestBody String body)
      throws InterruptedException {
    Bundle request = (Bundle) fhirContext.newJsonParser().parseResource(body);
    Bundle aggregated = service.process(request);
    return ResponseEntity.ok(fhirContext.newJsonParser().encodeResourceToString(aggregated));
  }

  @ExceptionHandler({
    QueryBrokerService.BadRequestException.class,
    ca.uhn.fhir.parser.DataFormatException.class
  })
  ResponseEntity<String> badRequest(Exception e) {
    OperationOutcome outcome =
        BrokerMessages.operationOutcome(
            IssueSeverity.ERROR, IssueType.INVALID, ErrorCode.VALIDATION_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .header("Content-Type", FHIR_JSON)
        .body(fhirContext.newJsonParser().encodeResourceToString(outcome));
  }
}
