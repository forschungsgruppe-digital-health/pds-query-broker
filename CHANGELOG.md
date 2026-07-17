# Changelog

All notable changes to this project are documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
From v0.2.0 onward, releases and changelog entries are generated automatically by
[release-please](https://github.com/googleapis/release-please) from Conventional Commits.
Entries up to and including v0.2.0 were written manually following
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.6.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.5.0...v0.6.0) (2026-07-17)


### Features

* **catalog:** MII KDS content-profile strategy — research report, ADR-012, KDS Diagnose binding (S1) ([#14](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/14)) ([f77e774](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/f77e774209a1922abf078ed2f664980f91ae6409))
* **sdk:** enforce targetProfile validation of handler output before sending ([#13](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/13)) ([1ad35ca](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/1ad35ca18f7f94ac410a909dee79c31e06a134dc))
* **sdk:** generic FHIR terminology-server integration — SU-TermServ for the pilot (ADR-013) ([#15](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/15)) ([4c0689f](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/4c0689f5b3e483978327c3f5acd983812c5e32f5))


### Refactoring

* expand PDS/THS abbreviations in source identifiers and docs ([#12](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/12)) ([d7244b2](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/d7244b23a416aa92f1320ecad252e5bcb9a1f13d))

## [0.5.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.4.0...v0.5.0) (2026-07-14)


### Features

* **conformance:** catalog-driven conformance harness for PDS connectors (increment 2) ([#10](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/10)) ([167eafb](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/167eafbbfe0188c39fdf688d8194c903f4ef2705))


### Build & Dependencies

* **specs:** generate stubs with the official AsyncAPI Generator (Python + Java) ([#9](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/9)) ([b71625a](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/b71625af07a997d5b3788a359657a63a6698a7b9))

## [0.4.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.3.0...v0.4.0) (2026-07-14)


### Features

* multi-PDS fan-out — second synthetic connector site (increment 2) ([#6](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/6)) ([89ef488](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/89ef4885f5f189c389928d177d216a4e3ee438de))

## [0.3.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.2.0...v0.3.0) (2026-07-13)


### Features

* **ig:** communication profiles — message envelopes, error model, OperationError ([#4](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/4)) ([69fb79e](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/69fb79e60a1debcf9d40c5824f5fc581aad3911e))
* walking skeleton — broker, connector SDK, example connector (increment 1) ([#5](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/5)) ([d9e604a](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/d9e604ab2030d8c1987c1739e1fbc1697d68d74e))


### Documentation

* record implementation plan (ADR-010, ADR-011, IMPLEMENTATION_PLAN.md) ([#2](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/2)) ([260492b](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/260492bcbf8ef6de55758292c4aa0009fd1eed3f))

## [0.2.0] - 2026-05-04

### Changed

- Unified all examples onto a single exemplary operation (`GetConditions`); removed: `GetLabResults`, `GetProcedures`, `GetMedications`, `GetEncounters`, `GetPathwayStatus`, `GetProjectData`, `GetTumorBoardResult`
- Populated the `catalog/` directory with the FHIR JSON artifacts of the example operation (OperationDefinition, MessageDefinition, GraphDefinition — generated from SUSHI)
- Mermaid diagrams: removed all color directives (`fill`, `stroke`, `style`)
- Profile binding described as optional and project-specific throughout; MII KDS is now only an example
- PDS prefix (Primary Data Source) instead of DIZ in all files
- Multi-client routing via `MessageHeader.destination` + AMQP `replyTo` (ADR-009)

### Added

- FHIR ImplementationGuide project (`ig/`) with SUSHI-compilable FSH sources (0 errors, 0 warnings), IG Publisher configuration (`ig.ini`), build scripts, and page content
- AsyncAPI 3.0 base spec (`specs/pds-connector-base.yaml`) with multi-client routing channels
- Docker configuration (`docker/`) with RabbitMQ, HAPI FHIR catalog server, catalog seed, AsyncAPI Studio, broker Dockerfile
- GitHub Actions workflows: SUSHI validation + IG Publisher + GitHub Pages deploy, AsyncAPI validation, Docker validation
- Claude Code helper files (`CLAUDE.md`, `.claude/`) with sub-agents for FHIR IG profiling, arc42 documentation, and architecture development

## [0.1.0] - 2026-05-01

_Initial release — concept and architecture documentation._

### Added

- README.md with architecture overview, component diagram (Mermaid), base operations table, sequence diagram, and standards references
- ARCHITECTURE.md as arc42 architecture documentation (v9.0) with all 12 sections: introduction and goals, constraints, context (business + technical), solution strategy, building block view (3 levels with component description tables), runtime view (sequence diagram `$GetConditions`), deployment view, cross-cutting concepts (mindmap + 7 subsections), architecture decisions (8 ADRs), quality requirements (overview + 6 scenarios per arc42 v9.0 / ISO 25010:2023), risks and technical debt, glossary
- CONTRIBUTING.md with development setup, PDS connector implementation (Java example), conformance tests, guide for defining new operations (triple OperationDefinition + MessageDefinition + GraphDefinition), broker/SDK core classes, code conventions, and release process
- PDS_INTEGRATION.md as a language-agnostic implementation guide for PDS developers with 11 sections: stub generation (AsyncAPI CLI), configuration (YAML), OperationDefinition interpretation, handler contract (pseudocode), subtask flowchart, parameter extraction, FHIR mapping checklist, handler registration, RabbitMQ queue setup, startup flow (sequence diagram), conformance tests, procedure for new operations
- CHANGELOG.md per Keep a Changelog 1.1.0

### Architecture decisions in this version

- FHIR R4 Messaging as the message format (Bundle type `message`) instead of a proprietary JSON envelope (ADR-002)
- Triple OperationDefinition + MessageDefinition + GraphDefinition as the message contract with MII KDS profile binding via `targetProfile`, `focus.profile`, `target.profile` (ADR-003)
- CapabilityStatement.messaging as the capability-discovery mechanism instead of a proprietary `tools/list` (ADR-004)
- Pseudonyms as FHIR Identifier in the Parameters resource, not as a MessageHeader extension (ADR-005)
- Adapter pattern for PDS connectors (ADR-001)
- FHIR Provenance + AuditEvent for data provenance and processing log (ADR-008)
- OperationDefinition names in PascalCase (e.g. `GetConditions`) per FHIR constraint opd-0 (ADR-007)
- AsyncAPI base spec only for the AMQP topology; operation semantics entirely in FHIR resources
- FHIR IG lifecycle for catalog creation and provisioning (FSH → SUSHI → IG Publisher → FHIR package → catalog server)
- Fanout exchange as the entry architecture, topic exchange with `pds.{pdsId}.*` as the target architecture (ADR-006)

[0.2.0]: https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/forschungsgruppe-digital-health/pds-query-broker/releases/tag/v0.1.0
