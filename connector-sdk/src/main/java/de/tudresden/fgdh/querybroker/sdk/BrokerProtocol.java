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

  /** Fanout exchange — the entry-architecture broadcast topology (ADR-006). */
  public static final String BROADCAST_EXCHANGE = "pds.broadcast";

  /**
   * Topic exchange — the target routing topology (ADR-006 rev.): the broker
   * publishes each request only to the addressed sites, keyed
   * {@code pds.{pdsId}.request}, instead of broadcasting to everyone.
   */
  public static final String TOPIC_EXCHANGE = "pds.topic";

  public static final String REQUEST_QUEUE_PREFIX = "req.";
  public static final String RESPONSE_QUEUE_PREFIX = "responses.";
  public static final String DEFAULT_RESPONSE_QUEUE = "responses.default";

  public static final String FHIR_JSON_CONTENT_TYPE = "application/fhir+json";

  /**
   * The topic routing key a request for {@code pdsId} is published with, and
   * the binding key a connector for {@code pdsId} subscribes with:
   * {@code pds.{pdsId}.request}.
   */
  public static String requestRoutingKey(String pdsId) {
    return "pds." + pdsId + ".request";
  }

  /**
   * Derives the primary-data-source identifier from a pseudonym's gPAS domain
   * system by convention (the last path segment): e.g.
   * {@code https://ths.example.org/gpas/domain/PDS-EXAMPLE} → {@code PDS-EXAMPLE}.
   * This is the domain→site mapping the topic router uses.
   */
  public static String primaryDataSourceIdOf(String gpasDomainSystem) {
    if (gpasDomainSystem == null || gpasDomainSystem.isBlank()) {
      return "";
    }
    String trimmed =
        gpasDomainSystem.endsWith("/")
            ? gpasDomainSystem.substring(0, gpasDomainSystem.length() - 1)
            : gpasDomainSystem;
    return trimmed.substring(trimmed.lastIndexOf('/') + 1);
  }

  private BrokerProtocol() {}
}
