# 12. Glossary

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** The shared vocabulary — every domain and technical term used across these docs.

| Term | Definition |
|---------|------------|
| **BFF** | Backend for Frontend — dedicated service layer between the portal UI and the broker |
| **Connector** | Standalone microservice per PDS site; adapter between the broker protocol and the local data system |
| **PDS** | Primary data source (German: Primärdatenquelle) — stores primary medical data (e.g. data integration center, hospital IT, laboratory information system) |
| **E-PIX** | Enterprise Patient Identifier Cross-referencing — MOSAiC component for ID management |
| **gPAS** | generic Pseudonym Administration Service — MOSAiC component for pseudonym administration |
| **gICS** | generic Informed Consent Service — MOSAiC component for consent management |
| **GraphDefinition** | FHIR resource; describes the resource graph of a response message with profile binding |
| **MessageDefinition** | FHIR resource; formalizes the message contract (mandatory payloads, allowed responses) |
| **MII** | Medical Informatics Initiative (Medizininformatik-Initiative) — an application context for which MII KDS profiles can be configured as `targetProfile` |
| **MII KDS** | MII core data set (MII-Kerndatensatz) — an example of project-specific FHIR profiles; not inherent to the architecture |
| **MOSAiC** | Modular Open Source Architecture for Identity and Consent — trusted-third-party software from the University of Greifswald |
| **Message catalog** | FHIR server holding OperationDefinitions, MessageDefinitions, GraphDefinitions, and project-specific profiles |
| **OperationDefinition** | FHIR resource; describes the semantics of an operation (parameters, types, `targetProfile`) |
| **OperationOutcome** | FHIR resource for a standardized error model |
| **Partial result** | Aggregated result when individual PDS time out; contains `OperationOutcome` |
| **Provenance** | FHIR resource; documents the origin of a resource — who (agent), when, from which source (entity) |
| **AuditEvent** | FHIR resource; documents a processing step — action, period, outcome, systems involved |
| **Self-filtering** | The connector decides autonomously whether it processes a broadcast message |
| **THS** | Trusted third party (German: Treuhandstelle) — mediates between pseudonyms and identities |
