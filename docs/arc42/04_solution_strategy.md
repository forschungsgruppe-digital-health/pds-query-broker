# 4. Solution Strategy

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** The handful of fundamental decisions (FHIR messaging, the operation *triple*, adapter connectors) that shape everything else. Terms are defined in the [glossary](12_glossary.md).

| Decision | Rationale |
|--------------|------------|
| **FHIR Messaging instead of a proprietary envelope** | One format, one parser (HAPI FHIR). According to the FHIR spec, OperationDefinitions can be invoked via messaging ([FHIR R4 Messaging](https://hl7.org/fhir/R4/messaging.html)). |
| **Triple of OperationDefinition + MessageDefinition + GraphDefinition** | An OperationDefinition alone does not describe the complete message contract. MessageDefinition formalizes mandatory payloads and allowed responses. GraphDefinition formalizes the payload graph. |
| **Profile binding via `targetProfile`** | FHIR-native mechanism. Profiles are selectable per project (e.g. MII KDS, US Core, custom profiles). Validation with standard FHIR tooling (HAPI Validator). Operations without `targetProfile` return base FHIR resources. |
| **AsyncAPI for transport only** | Stable AMQP topology. Message semantics live in FHIR resources — new operations require no rebuild. |
| **Adapter pattern for connectors** | PDS systems do not speak FHIR. Structural translation is needed on both sides (inbound and outbound). |
| **Broadcast with self-filtering** | Fanout Exchange minimizes configuration effort. The connector filters by gPAS domain and `CapabilityStatement.messaging`. |
| **CapabilityStatement.messaging instead of proprietary discovery** | FHIR-native mechanism for capability declaration ([FHIR R4 CapabilityStatement](https://hl7.org/fhir/R4/capabilitystatement.html)). |
| **Provenance + AuditEvent for origin and processing log** | `Provenance` documents data origin per resource (PDS, source system, transformation). `AuditEvent` documents processing steps (query, validation, aggregation). Both are FHIR R4 resources and are transported as Bundle entries — no proprietary logging ([FHIR R4 Provenance](https://hl7.org/fhir/R4/provenance.html), [FHIR R4 AuditEvent](https://hl7.org/fhir/R4/auditevent.html)). |
