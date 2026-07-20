# 3. Context and Scope

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** The system boundary — the portal, the primary data sources, and the trusted third party it talks to, and in which formats. Terms are defined in the [glossary](12_glossary.md).

## 3.1 Business Context

```mermaid
graph LR
    PAT(("Patient"))
    PORTAL["Patient Portal"]
    BROKER["Query Broker"]
    PDS_A["PDS A"]
    PDS_B["PDS B"]
    THS["Fed. THS<br/>(MOSAiC)"]

    PAT -->|"Login / data request"| PORTAL
    PORTAL -->|"Operation invocation"| BROKER
    BROKER -->|"FHIR Message"| PDS_A
    BROKER -->|"FHIR Message"| PDS_B
    PDS_A -->|"PSN → local ID"| THS
    PDS_B -->|"PSN → local ID"| THS
    PDS_A -->|"FHIR Response"| BROKER
    PDS_B -->|"FHIR Response"| BROKER
    BROKER -->|"Aggregated Bundle"| PORTAL
    PORTAL -->|"Display data"| PAT
```

| External partner | Interface | Format |
|------------------|---------------|--------|
| Patient portal (via BFF — planned, portal-side; today the broker's `$process-message` is the ingress) | REST (BFF API) | JSON (FHIR-based) |
| PDS connectors | AMQP (RabbitMQ) | FHIR Message Bundle (`application/fhir+json`) |
| Federated THS (gPAS) | FHIR REST (TTP-FHIR gateway, `$dePseudonymize`) | FHIR R4 Parameters (`application/fhir+json`) |
| Message catalog | FHIR REST API | FHIR R4 (OperationDefinition, MessageDefinition, GraphDefinition) |

## 3.2 Technical Context

```mermaid
graph TB
    subgraph "Integration layer"
        BFF["BFF<br/><i>planned — portal-side, ADR-011</i>"]
        BROKER["Query Broker<br/><i>Spring Boot · Spring AMQP</i>"]
        MQ[("RabbitMQ<br/><i>AMQP 0-9-1</i>")]
    end

    subgraph "Message catalog"
        CATALOG[("HAPI FHIR Server")]
        OPDEF["OperationDefinition<br/><i>GetConditions (only op so far)</i>"]
        MSGDEF["MessageDefinition"]
        GRAPHDEF["GraphDefinition"]
        KDS["Project profiles"]
        MSGDEF -->|"eventUri"| OPDEF
        OPDEF -->|"targetProfile"| KDS
        MSGDEF -->|"focus.profile"| KDS
        GRAPHDEF -->|"target.profile"| KDS
    end

    subgraph "PDS site"
        CONN["Connector<br/><i>Connector SDK</i>"]
        LTHS["gPAS"]
        SRC[("i2b2 / OMOP / SQL")]
    end

    BFF -->|"FHIR Message Bundle"| BROKER
    BROKER -->|"FHIR REST"| CATALOG
    BROKER -->|"Publish"| MQ
    MQ -->|"Per-site (topic)"| CONN
    CONN -->|"Response"| MQ
    CONN --> LTHS --> SRC
    CONN -->|"FHIR REST"| CATALOG
```
