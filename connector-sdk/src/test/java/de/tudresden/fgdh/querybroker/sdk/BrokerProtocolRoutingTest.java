package de.tudresden.fgdh.querybroker.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The topic-routing helpers (ADR-006 rev.). */
class BrokerProtocolRoutingTest {

  @Test
  void requestRoutingKeyFollowsThePattern() {
    assertThat(BrokerProtocol.requestRoutingKey("PDS-EXAMPLE")).isEqualTo("pds.PDS-EXAMPLE.request");
  }

  @Test
  void primaryDataSourceIdIsTheLastSegmentOfTheGpasDomain() {
    assertThat(
            BrokerProtocol.primaryDataSourceIdOf(
                "https://ths.example.org/gpas/domain/PDS-EXAMPLE"))
        .isEqualTo("PDS-EXAMPLE");
    assertThat(
            BrokerProtocol.primaryDataSourceIdOf(
                "https://ths.example.org/gpas/domain/PDS-EXAMPLE-B/"))
        .isEqualTo("PDS-EXAMPLE-B");
    assertThat(BrokerProtocol.primaryDataSourceIdOf("")).isEmpty();
    assertThat(BrokerProtocol.primaryDataSourceIdOf(null)).isEmpty();
  }

  @Test
  void routingKeyOfDerivedIdRoundTrips() {
    String domain = "https://ths.example.org/gpas/domain/PDS-XYZ";
    assertThat(BrokerProtocol.requestRoutingKey(BrokerProtocol.primaryDataSourceIdOf(domain)))
        .isEqualTo("pds.PDS-XYZ.request");
  }
}
