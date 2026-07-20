# 10. Quality Requirements

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** The quality requirements made concrete as measurable scenarios (stimulus → response → metric). Terms are defined in the [glossary](12_glossary.md).

> Section 10 follows the arc42 v9.0 structure: 10.1 gives an overview of the quality requirements by category (aligned with [Q42](https://quality.arc42.org/) / ISO 25010:2023); 10.2 makes them concrete through measurable quality scenarios.

## 10.1 Overview

| Category | Quality requirement | Priority | Reference |
|-----------|---------------------|-----------|---------|
| **#interoperable** | All messages and response resources are FHIR R4 conformant and — where configured — conform to the profiles stored in the catalog. | High | → ADR-002, ADR-003 |
| **#flexible** | New operations can be added by creating FHIR resources in the catalog — without rebuilding existing connectors. | High | → ADR-003, Section 8.2 |
| **#flexible** | A new PDS site is onboarded by deploying a connector and setting up a RabbitMQ queue — without changing the broker. | High | → ADR-006, Section 7 |
| **#reliable** | The broker delivers partial results with `OperationOutcome` when individual PDS do not respond. | Medium | → QS-3, Section 6.2 |
| **#operable** | PDS developers receive an abstract connector base class (SDK) and a conformance test framework. | Medium | → Section 8.5, CONTRIBUTING.md |
| **#secure** | Data remains pseudonymized; no central data store. Authorization scopes for third-party applications are yet to be defined. | Low (currently) | → Section 11 |
| **#traceable** | Every resource in the aggregated Bundle carries its origin (PDS, source system) and a processing log. | High | → ADR-008, Section 8.7 |

## 10.2 Details (Quality Scenarios)

| ID | Stimulus | Response | Metric / acceptance criterion |
|----|----------|----------|---------------------------|
| QS-1 | A connector delivers Condition resources without an ICD-10-GM coding. | The `CatalogProfileValidator` (the SDK's HAPI-backed `ProfileValidator` implementation) in the connector detects the profile violation and sends an `OperationOutcome` instead of invalid data. | 0 non-profile-conformant resources reach the broker. |
| QS-2 | A new PDS is to be onboarded. | The PDS developer extends the SDK's abstract connector base class, implements handlers, declares the queue. | Broker code and existing connectors remain unchanged (0 changes). |
| QS-3 | A PDS does not respond within the configured timeout (default: 8s). | The aggregator produces a partial result with an `OperationOutcome` for the missing PDS. | The portal receives results from the responding PDS within 10s. |
| QS-4 | A new operation `GetNewData` is needed. | The project core creates OperationDefinition + MessageDefinition + GraphDefinition in the catalog. | 0 connector rebuilds required. Existing connectors respond with `not-supported`. |
| QS-5 | A configured profile is published in a new version. | Catalog update (update `targetProfile`), conformance tests per PDS, re-certification. | All connectors validate against the new profile version before the next release. |
| QS-6 | An auditor wants to trace which PDS delivered a specific Condition resource and whether profile validation passed. | `Provenance.agent.who` identifies the PDS, `Provenance.entity.what` the source system. `AuditEvent.entity.detail[profileValidation]` (whose `type` = `profile-validation`) documents the validation result. | Every business resource in the aggregated Bundle has exactly one associated `Provenance` and at least one `AuditEvent`. |
