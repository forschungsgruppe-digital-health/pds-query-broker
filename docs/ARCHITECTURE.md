# Architecture Documentation — Query Broker

> **Moved.** The architecture documentation is now split into one file per arc42 section under
> [docs/arc42/](arc42/README.md). This page is a redirect + anchor-compatibility stub so existing
> deep links (including dated reports) keep resolving.

**Start here → [architecture docs index](arc42/README.md).**

<!-- Anchor-compatibility: old `#…` deep links resolve to the headings below, which point onward. -->

## 1. Introduction and Goals
→ moved to [docs/arc42/01_introduction_and_goals.md](arc42/01_introduction_and_goals.md)

## 2. Architecture Constraints
→ moved to [docs/arc42/02_architecture_constraints.md](arc42/02_architecture_constraints.md)

## 3. Context and Scope
→ moved to [docs/arc42/03_context_and_scope.md](arc42/03_context_and_scope.md)

## 4. Solution Strategy
→ moved to [docs/arc42/04_solution_strategy.md](arc42/04_solution_strategy.md)

## 5. Building Block View
→ moved to [docs/arc42/05_building_block_view.md](arc42/05_building_block_view.md)

## 6. Runtime View
→ moved to [docs/arc42/06_runtime_view.md](arc42/06_runtime_view.md)

## 7. Deployment View
→ moved to [docs/arc42/07_deployment_view.md](arc42/07_deployment_view.md)

## 8. Cross-cutting Concepts
→ moved to [docs/arc42/08_concepts.md](arc42/08_concepts.md)

## 9. Architecture Decisions
→ moved to [docs/arc42/09_architecture_decisions.md](arc42/09_architecture_decisions.md)

## 10. Quality Requirements
→ moved to [docs/arc42/10_quality_requirements.md](arc42/10_quality_requirements.md)

## 11. Risks and Technical Debt
→ moved to [docs/arc42/11_technical_risks.md](arc42/11_technical_risks.md)

## 12. Glossary
→ moved to [docs/arc42/12_glossary.md](arc42/12_glossary.md)

### ADR-001: Adapter Pattern Instead of Proxy for Connectors
→ moved to [ADR-001](arc42/09_architecture_decisions.md#adr-001-adapter-pattern-instead-of-proxy-for-connectors)

### ADR-002: FHIR Message Bundles Instead of a Proprietary JSON Envelope
→ moved to [ADR-002](arc42/09_architecture_decisions.md#adr-002-fhir-message-bundles-instead-of-a-proprietary-json-envelope)

### ADR-003: Triple of OperationDefinition + MessageDefinition + GraphDefinition
→ moved to [ADR-003](arc42/09_architecture_decisions.md#adr-003-triple-of-operationdefinition--messagedefinition--graphdefinition)

### ADR-004: CapabilityStatement.messaging Instead of Proprietary Discovery
→ moved to [ADR-004](arc42/09_architecture_decisions.md#adr-004-capabilitystatementmessaging-instead-of-proprietary-discovery)

### ADR-005: Pseudonyms as Parameters Identifiers (Not a MessageHeader Extension)
→ moved to [ADR-005](arc42/09_architecture_decisions.md#adr-005-pseudonyms-as-parameters-identifiers-not-a-messageheader-extension)

### ADR-006: Fanout Exchange to Start, Topic as the Target Architecture
→ moved to [ADR-006](arc42/09_architecture_decisions.md#adr-006-fanout-exchange-to-start-topic-as-the-target-architecture)

### ADR-007: PascalCase Names for OperationDefinitions
→ moved to [ADR-007](arc42/09_architecture_decisions.md#adr-007-pascalcase-names-for-operationdefinitions)

### ADR-008: Provenance + AuditEvent Instead of Proprietary Logging
→ moved to [ADR-008](arc42/09_architecture_decisions.md#adr-008-provenance--auditevent-instead-of-proprietary-logging)

### ADR-009: MessageHeader.destination for Multi-Client Routing
→ moved to [ADR-009](arc42/09_architecture_decisions.md#adr-009-messageheaderdestination-for-multi-client-routing)

### ADR-010: Implementation as Gradle Multi-Module Monorepo (Java 21 / Spring Boot 3 / HAPI FHIR 8)
→ moved to [ADR-010](arc42/09_architecture_decisions.md#adr-010-implementation-as-gradle-multi-module-monorepo-java-21--spring-boot-3--hapi-fhir-8)

### ADR-011: Staged Pilot Implementation — Walking Skeleton First
→ moved to [ADR-011](arc42/09_architecture_decisions.md#adr-011-staged-pilot-implementation--walking-skeleton-first)

### ADR-012: Content-Profile Strategy — MII KDS First, IPA-Aligned Facade Boundary
→ moved to [ADR-012](arc42/09_architecture_decisions.md#adr-012-content-profile-strategy--mii-kds-first-ipa-aligned-facade-boundary)

### ADR-013: Generic Remote Terminology-Server Integration — SU-TermServ for the Pilot
→ moved to [ADR-013](arc42/09_architecture_decisions.md#adr-013-generic-remote-terminology-server-integration--su-termserv-for-the-pilot)
