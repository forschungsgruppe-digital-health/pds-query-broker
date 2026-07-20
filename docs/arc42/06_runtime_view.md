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

    Note over QB: AuditEvent: request received

    QB->>CAT: GET /MessageDefinition/GetConditionsRequest
    CAT-->>QB: MessageDefinition + OperationDefinition

    QB->>MQ: Publish pds.topic (key pds.PDS-A.request)
    QB->>MQ: Publish pds.topic (key pds.PDS-B.request)
    Note over QB: AuditEvent: fan-out to 2 PDS<br/>(each request trimmed to that site's pseudonym)

    par Topic routing — each site receives ONLY its own pseudonym
        MQ->>CA: FHIR Message Bundle (PSN-A only)
        MQ->>CB: FHIR Message Bundle (PSN-B only)
    end

    CA->>CA: gPAS domain PDS-A ✓ (self-filter, defense-in-depth) → handler
    Note over CA: AuditEvent: query start (OMOP CDM)
    CA->>CA: DB query → FHIR Conditions
    Note over CA: Create Provenance per Condition<br/>(agent=PDS-A, source=OMOP)
    CA->>CA: Profile validation ✓
    Note over CA: AuditEvent: validation passed
    CA-->>MQ: Response (Conditions + Provenances + AuditEvents)

    CB->>CB: gPAS domain PDS-B ✓ → handler
    CB-->>MQ: Response (Conditions + Provenances + AuditEvents)

    MQ-->>QB: 2 responses
    QB->>QB: Aggregation + GraphDefinition check
    Note over QB: AuditEvent: aggregation (2/2 complete)<br/>Provenance: aggregation step
    QB-->>B: Aggregated Bundle<br/>(Conditions + all Provenances + all AuditEvents)
    B-->>P: JSON
```

## 6.2 Scenario: PDS Does Not Support an Operation

The connector responds with `MessageHeader.response.code = fatal-error` and an `OperationOutcome` resource (`issue.code = not-supported`). The aggregator counts this response as complete but excludes it from the result Bundle.
