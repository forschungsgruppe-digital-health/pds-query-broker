# 5. Building Block View

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** How the system decomposes into parts — broker, message catalog, connectors, RabbitMQ — each with a diagram and a role table. Terms are defined in the [glossary](12_glossary.md).

## 5.1 Level 1 — Overall System Decomposition

```mermaid
graph TB
    subgraph "Query Broker System"
        BFF["BFF"]
        BROKER["Query Broker Service"]
        MQ[("RabbitMQ")]
        CATALOG[("Message catalog")]
    end

    FTHS["Fed. THS (MOSAiC)"]
    CONN_A["PDS-A Connector"]
    CONN_B["PDS-B Connector"]

    BFF --> BROKER --> MQ
    BROKER --> CATALOG
    MQ --> CONN_A
    MQ --> CONN_B
    BFF --> FTHS
```

| Building block | Responsibility | Interfaces | Technology |
|----------|-------------------|----------------|-------------|
| **BFF** | Session, PSN lookup, response shaping | REST ← portal, REST → broker, REST → THS | Spring Boot, HAPI FHIR |
| **Query Broker Service** | Validation (MessageDefinition), routing (CapabilityStatement), fan-out, aggregation, profile validation. Creates `AuditEvent` resources for request receipt, fan-out, aggregation, and response dispatch. Creates `Provenance` for the aggregation step. | AMQP → RabbitMQ, FHIR REST → catalog, REST → connector `/metadata` | Spring Boot, Spring AMQP, HAPI FHIR |
| **RabbitMQ** | Message transport (fanout/topic), queue isolation, DLQ | AMQP 0-9-1 | RabbitMQ 3.12+, AsyncAPI 3.0 |
| **Message catalog** | OperationDefinition, MessageDefinition, GraphDefinition, project-specific profiles | FHIR REST API | HAPI FHIR Server, FHIR profile packages |
| **PDS Connector** | Self-filtering, capability check, dispatch, adapter, profile validation before dispatch. Creates `Provenance` per business resource (origin: PDS, source system) and `AuditEvent` for query execution and validation result. | AMQP ← RabbitMQ, REST `/metadata`, REST → local THS | Connector SDK (generated from AsyncAPI), Spring Boot, HAPI FHIR |
| **Fed. THS** | Pseudonym resolution across PDS boundaries | REST API (E-PIX) | MOSAiC E-PIX, gPAS |

## 5.2 Level 2 — Message Catalog (Whitebox)

```mermaid
graph LR
    subgraph "Message catalog"
        OPDEF["OperationDefinition<br/><i>e.g. GetConditions</i>"]
        MSGDEF_REQ["MessageDefinition<br/><i>GetConditionsRequest</i>"]
        MSGDEF_RESP["MessageDefinition<br/><i>GetConditionsResponse</i>"]
        GRAPHDEF["GraphDefinition<br/><i>GetConditionsResponseGraph</i>"]
        KDS["Project profiles<br/><i>Diagnose · ObservationLab<br/>Procedure · Encounter · ...</i>"]
    end

    MSGDEF_REQ -->|"eventUri"| OPDEF
    MSGDEF_REQ -->|"allowedResponse"| MSGDEF_RESP
    MSGDEF_RESP -->|"graph"| GRAPHDEF
    OPDEF -->|"return.part.targetProfile"| KDS
    MSGDEF_RESP -->|"focus.profile"| KDS
    GRAPHDEF -->|"target.profile"| KDS
```

| Building block | Responsibility | FHIR reference |
|----------|-------------------|---------------|
| **OperationDefinition** | Semantics: parameters, types, cardinalities, `targetProfile` → project profile (optional) | [HL7 FHIR R4](https://hl7.org/fhir/R4/operationdefinition.html) |
| **MessageDefinition (Request)** | Message contract: `focus` (mandatory payloads), `allowedResponse` | [HL7 FHIR R4](https://hl7.org/fhir/R4/messagedefinition.html) |
| **MessageDefinition (Response)** | Response contract: `focus.profile` → project profile (optional) | [HL7 FHIR R4](https://hl7.org/fhir/R4/messagedefinition.html) |
| **GraphDefinition** | Payload structure: resource graph, `target.profile` → project profile (optional) | [HL7 FHIR R4](https://hl7.org/fhir/R4/graphdefinition.html) |
| **Project profiles** | FHIR StructureDefinitions for output resources (e.g. MII KDS, US Core, custom profiles) | [Project-specific] |

## 5.3 Level 2 — PDS Connector (Whitebox)

```mermaid
graph TB
    subgraph "Generated stub"
        ABS["AbstractPrimaryDataSourceConnector<br/><i>AMQP listener · pseudonym filter<br/>capability check · profile validation</i>"]
    end
    subgraph "PDS developer"
        MAP["OperationHandler map<br/><i>GetConditions → handler<br/>... → handler</i>"]
        H["Concrete handler<br/><i>local THS → DB query → FHIR</i>"]
    end
    subgraph "External"
        CAT["Message catalog"]
        LTHS["gPAS"]
        SRC[("Local system")]
    end

    ABS -->|"delegates"| MAP --> H
    H --> LTHS --> SRC
    ABS -->|"load targetProfile"| CAT
```

| Building block | Responsibility | Technology |
|----------|-------------------|-------------|
| **AbstractPrimaryDataSourceConnector** | FHIR message parsing, gPAS domain filtering, capability check, `targetProfile` validation, `Provenance` creation per resource, `AuditEvent` creation for query and validation | Connector SDK (generated), HAPI FHIR Validator |
| **OperationHandler** | Interface: `Bundle execute(String pseudonym, Parameters params)` | `@FunctionalInterface` |
| **Concrete handler** | Adapter: local system → FHIR (profile-conformant if `targetProfile` is declared). Sets `Resource.meta.source` to the connector URL. | Provided by the PDS developer, HAPI FHIR |
