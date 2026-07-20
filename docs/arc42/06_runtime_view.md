# 6. Runtime View

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** How those parts collaborate at run time, shown as step-by-step sequence diagrams (e.g. retrieving diagnoses). Terms are defined in the [glossary](12_glossary.md).

## 6.1 Scenario: Retrieve Diagnoses (`$GetConditions`)

```mermaid
sequenceDiagram
    participant P as Portal
    participant B as BFF
    participant THS as Fed. THS
    participant QB as Query Broker
    participant CAT as Catalog
    participant MQ as RabbitMQ
    participant CA as PDS-A Connector
    participant CB as PDS-B Connector

    P->>B: GET /api/me/conditions
    B->>THS: resolve(patientId)
    THS-->>B: {PDS-A: PSN-A-8x3k, PDS-B: PSN-B-m9zq}
    B->>QB: FHIR Message (eventUri: .../GetConditions)

    Note over QB: (planned — ADR-008: AuditEvent on request receipt, not yet emitted)

    QB->>CAT: GET /OperationDefinition?url=.../GetConditions
    CAT-->>QB: searchset Bundle (OperationDefinition)

    QB->>MQ: Publish pds.topic (key pds.PDS-A.request)
    QB->>MQ: Publish pds.topic (key pds.PDS-B.request)
    Note over QB: Fan-out to 2 PDS (each request trimmed to that site's pseudonym)<br/>(planned — ADR-008: AuditEvent, not yet emitted)

    par Topic routing — each site receives ONLY its own pseudonym
        MQ->>CA: FHIR Message Bundle (PSN-A only)
        MQ->>CB: FHIR Message Bundle (PSN-B only)
    end

    CA->>CA: gPAS domain PDS-A ✓ (self-filter, defense-in-depth) → handler
    Note over CA: Query start (synthetic store — reference connector)<br/>(planned — ADR-008: AuditEvent, not yet emitted)
    CA->>CA: Synthetic store lookup → FHIR Conditions
    Note over CA: (planned — ADR-008: Provenance per Condition, source=PDS-A local store, not yet created)
    CA->>CA: Profile validation ✓
    Note over CA: Profile validation ✓ (planned — ADR-008: AuditEvent, not yet emitted)
    CA-->>MQ: Response (Conditions)

    CB->>CB: gPAS domain PDS-B ✓ → handler
    CB-->>MQ: Response (Conditions)

    MQ-->>QB: 2 responses
    QB->>QB: Aggregation (collect result resources; derive response code)
    Note over QB: (planned — ADR-008: AuditEvent + Provenance for the aggregation step, not yet emitted)
    QB-->>B: Aggregated Bundle (Conditions; OperationOutcome on partial/total failure)
    B-->>P: JSON
```

> **Note:** The Portal, BFF and federated-THS pseudonym-resolution hops (P/B/THS) are upstream components not yet built in this repository; the broker's real entry point is `POST /fhir/$process-message` with the site pseudonyms already resolved into the request Parameters (see §5 / ADR-011/012).

## 6.2 Scenario: PDS Does Not Support an Operation

If a PDS connector receives an operation it does not support, it stays silent — it publishes no response at all (`handle()` returns empty; see [§5 Building Blocks](05_building_block_view.md)). The aggregator waits until its timeout, then completes the query: if any other site answered, the aggregated Bundle carries an `OperationOutcome` (severity `warning`) noting that N of M addressed sites did not respond; if no site answered, the aggregated `MessageHeader` is `fatal-error`. (A request for an operation that is not in the catalog at all is instead rejected synchronously by the broker at ingress with an `OperationOutcome`, `issue.code = not-supported`.)
