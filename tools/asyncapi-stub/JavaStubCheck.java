import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import de.tudresden.fgdh.querybroker.spec.BrokerTransportSpec;

/**
 * Java-side spec/implementation compatibility check: compares the constants
 * of the AsyncAPI-generated {@code BrokerTransportSpec} (via the official
 * {@code @asyncapi/cli} + the in-repo qb-transport-stub template) against the
 * connector SDK's hand-maintained {@code BrokerProtocol}. Exits non-zero on
 * any drift — wired into the asyncapi-contract-test CI workflow.
 *
 * <p>Compile & run (after generation):
 * {@code javac -d out connector-sdk/src/main/java/de/tudresden/fgdh/querybroker/sdk/BrokerProtocol.java
 * tools/asyncapi-stub/generated/BrokerTransportSpec.java tools/asyncapi-stub/JavaStubCheck.java
 * && java -cp out:tools/asyncapi-stub JavaStubCheck}
 */
public final class JavaStubCheck {

  private static int failures = 0;

  public static void main(String[] args) {
    check("content type", BrokerTransportSpec.CONTENT_TYPE, BrokerProtocol.FHIR_JSON_CONTENT_TYPE);
    check(
        "broadcast exchange",
        BrokerTransportSpec.BROADCAST_EXCHANGE,
        BrokerProtocol.BROADCAST_EXCHANGE);
    check("broadcast exchange type", BrokerTransportSpec.BROADCAST_EXCHANGE_TYPE, "fanout");
    check(
        "request queue for PDS-X",
        BrokerTransportSpec.requestQueue("PDS-X"),
        BrokerProtocol.REQUEST_QUEUE_PREFIX + "PDS-X");
    check(
        "response queue for portal",
        BrokerTransportSpec.responseQueue("portal"),
        BrokerProtocol.RESPONSE_QUEUE_PREFIX + "portal");
    check(
        "default response queue is a responses.* queue",
        BrokerProtocol.DEFAULT_RESPONSE_QUEUE.startsWith(BrokerProtocol.RESPONSE_QUEUE_PREFIX),
        true);
    check("dead letter queue", BrokerTransportSpec.DEAD_LETTER_QUEUE, "pds.dlq");
    check("request delivery mode persistent", BrokerTransportSpec.DELIVERY_MODE_REQUEST, 2);
    check(
        "connector response delivery mode persistent",
        BrokerTransportSpec.DELIVERY_MODE_CONNECTOR_RESPONSE,
        2);

    if (failures > 0) {
      System.err.println(failures + " spec/implementation mismatch(es) — see above.");
      System.exit(1);
    }
    System.out.println("JavaStubCheck: spec and BrokerProtocol agree on all transport facts.");
  }

  private static void check(String what, Object generated, Object implemented) {
    if (!generated.equals(implemented)) {
      System.err.printf("MISMATCH %s: spec=%s implementation=%s%n", what, generated, implemented);
      failures++;
    }
  }

  private JavaStubCheck() {}
}
