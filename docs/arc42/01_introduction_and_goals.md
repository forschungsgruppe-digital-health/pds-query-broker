# 1. Introduction and Goals

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** What the Query Broker is, the quality goals that drive its design, and who depends on it. Terms are defined in the [glossary](12_glossary.md).

## 1.1 Requirements Overview

The Query Broker distributes data requests from a patient portal (and potential third-party applications) to multiple primary data sources (PDS), aggregates their responses, and returns normalized, profile-conformant FHIR R4 Bundles.

## 1.2 Quality Goals

| Priority | Quality goal | Scenario |
|-----------|---------------|----------|
| 1 | **Interoperability** | All messages and responses are FHIR R4 conformant; response resources conform to the configured profiles. |
| 2 | **Extensibility** | A new operation can be added by creating FHIR resources in the catalog — without rebuilding existing connectors. |
| 3 | **Decoupling** | A new PDS site is onboarded by deploying a connector and setting up a RabbitMQ queue — without changing the broker. |
| 4 | **Fault tolerance** | The broker delivers partial results with `OperationOutcome` when individual PDS do not respond. |
| 5 | **Traceability** | Every resource in the aggregated Bundle carries its origin (PDS, source system) and a processing log (validation, aggregation). |

## 1.3 Stakeholders

| Role | Expectation |
|-------|-----------|
| PDS developers | Clear connector interface, SDK with generated stub, conformance tests |
| Project core developers | Extensible architecture, standards-based, maintainable |
| Patient portal team | Stable BFF API, FHIR-conformant responses |
| Data protection officers | Pseudonymized data processing, no central data store |
