package de.tudresden.fgdh.querybroker.sdk;

/**
 * Canonical URLs and AMQP names of the Query Broker protocol.
 *
 * <p>Single place where code and the IG (ig/input/fsh/) meet — keep in sync
 * with the FSH sources, which are the source of truth for all canonicals.
 */
public final class BrokerProtocol {

  public static final String CANONICAL_BASE = "https://querybroker.example.org/fhir";

  public static final String ERROR_CODE_SYSTEM = CANONICAL_BASE + "/CodeSystem/BrokerErrorCodes";
  public static final String OPERATION_ERROR_EVENT = CANONICAL_BASE + "/event/operation-error";
  public static final String OPERATION_ERROR_MESSAGE_DEFINITION =
      CANONICAL_BASE + "/MessageDefinition/OperationError";

  /** Broker error codes (CodeSystem BrokerErrorCodes — generated from FSH). */
  public enum ErrorCode {
    TIMEOUT("timeout", "PDS response timeout"),
    UNSUPPORTED_OPERATION("unsupported-operation", "Operation not supported"),
    NO_CAPABLE_PDS("no-capable-pds", "No capable PDS"),
    PDS_ERROR("pds-error", "PDS-side processing error"),
    VALIDATION_ERROR("validation-error", "Profile validation failed");

    private final String code;
    private final String display;

    ErrorCode(String code, String display) {
      this.code = code;
      this.display = display;
    }

    public String code() {
      return code;
    }

    public String display() {
      return display;
    }
  }

  public static final String BROADCAST_EXCHANGE = "pds.broadcast";
  public static final String REQUEST_QUEUE_PREFIX = "req.";
  public static final String RESPONSE_QUEUE_PREFIX = "responses.";
  public static final String DEFAULT_RESPONSE_QUEUE = "responses.default";

  public static final String FHIR_JSON_CONTENT_TYPE = "application/fhir+json";

  private BrokerProtocol() {}
}
