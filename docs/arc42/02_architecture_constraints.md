# 2. Architecture Constraints

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** The fixed technical and organizational constraints the architecture has to work within. Terms are defined in the [glossary](12_glossary.md).

## 2.1 Technical Constraints

| Constraint | Explanation |
|---------------|-------------|
| Data in PDS not stored as FHIR | PDS data systems are heterogeneous (i2b2, OMOP CDM, SQL, HL7 v2). Connectors must translate as adapters. |
| Pseudonymization via MOSAiC | Patient identities are resolved via the federated trusted third party (THS) (E-PIX/gPAS). Each PDS has its own gPAS domain. |
| FHIR R4 as the canonical format | All outputs are FHIR R4, optionally profiled according to project-specific StructureDefinitions. |
| Authorization deferred | End-to-end authentication/authorization is out of scope for the walking-skeleton pilot and staged to a future security ADR (ADR-011 'Security staging'); the broker's `$process-message` ingress is currently compose-internal and unauthenticated. |
| Data protection by design (active) | Messages carry pseudonyms only (never real identities); in topic mode the broker trims each request to the site's own pseudonym (per-site filtering, ADR-006 rev.) with connector self-filtering as defense-in-depth; the remote terminology client supports mTLS (ADR-013). |

## 2.2 Organizational Constraints

| Constraint | Explanation |
|---------------|-------------|
| Profile context | Profiles, terminologies, and governance are defined per project. Example: MII core data set in the MII context, US Core in the US context, custom project profiles. |
| Decentralized PDS responsibility | Each PDS site is independently responsible for its connector. |

## 2.3 Conventions

| Convention | Rule |
|------------|-------|
| OperationDefinition names | PascalCase, regex `[A-Z]([A-Za-z0-9_]){1,254}` (FHIR constraint opd-0). Example: `GetConditions`. |
| Canonical URLs | `https://{project}.example.org/fhir/{ResourceType}/{Name}` |
| Profile URLs | Project-specific. Example MII KDS: `https://www.medizininformatik-initiative.de/fhir/core/modul-{name}/StructureDefinition/{Ressource}` |
| Pseudonym identifier | `system` = gPAS domain (`https://ths.example.org/gpas/domain/{PDS-ID}`), `value` = pseudonym |
