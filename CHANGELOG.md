# Changelog

All notable changes to this project are documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
From v0.2.0 onward, releases and changelog entries are generated automatically by
[release-please](https://github.com/googleapis/release-please) from Conventional Commits.
Entries up to and including v0.2.0 were written manually following
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.10.1](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.10.0...v0.10.1) (2026-07-20)


### Documentation

* **arc42:** reconcile architecture docs with the current implementation ([#47](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/47)) ([551afa1](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/551afa1c105fc359782f92368fe0eb7985ff069a))

## [0.10.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.9.0...v0.10.0) (2026-07-20)


### Features

* **broker:** per-site pseudonym filtering in topic routing (ADR-006 rev.) ([#45](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/45)) ([caa719e](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/caa719edc2cf40998765d92f189da4d8c5de9d71))

## [0.9.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.8.0...v0.9.0) (2026-07-18)


### Features

* track arc42-project-template toolkit (drift notifier + template-updater) ([#40](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/40)) ([60a9468](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/60a946846e5b62441cc8b226c19389ef784960f5))


### Bug Fixes

* **ci:** run AsyncAPI contract test broker in fanout mode (regression from [#20](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/20)) ([#42](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/42)) ([e4af8cd](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/e4af8cd369bb9ea6e2f0d852a15023dcc2a9cb1d))


### Build & Dependencies

* **deps:** Bump gradle-wrapper from 8.14.2 to 9.6.1 ([#31](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/31)) ([e9e995c](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/e9e995c0f638486ee62c260f2199db281142213f))
* **deps:** Bump org.gradle.toolchains.foojay-resolver-convention from 0.8.0 to 1.0.0 ([#32](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/32)) ([a56d90a](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/a56d90a82185da1e83109c2d57676985f55b33a7))
* **deps:** Bump the actions group across 1 directory with 15 updates ([#36](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/36)) ([8157299](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/81572995fbfed7c8285d35a64c0982f2cd8a50fb))
* **deps:** Bump the minor-and-patch group across 3 directories with 2 updates ([#26](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/26)) ([39e90e6](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/39e90e64366a6ee0afb746e3ceb205047149cca2))
* **deps:** Bump the pip group across 1 directory with 4 updates ([#30](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/30)) ([eb5553f](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/eb5553f16d8dd1eb1438b6503b73783fe08f7adc))
* **deps:** Spring Boot 4.1 + JUnit 6.1 + Testcontainers 2.0 migration ([#41](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/41)) ([267c1f2](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/267c1f2a1a487a26976dffb293da3f350173a213))
* **docker:** Gradle build image 9.6.1 + ignore temurin majors (ADR-0010) ([#43](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/43)) ([4c21c06](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/4c21c06148692c2bb0f84b7d67580dd8ed34d690))

## [0.8.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.7.0...v0.8.0) (2026-07-18)


### Features

* **skills:** add dependency-scanner + security-scanner setup wizards ([#37](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/37)) ([a449207](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/a4492074fa5db47fb8581cf332b5a96f7dd9f4c1))

## [0.7.0](https://github.com/forschungsgruppe-digital-health/pds-query-broker/compare/v0.6.0...v0.7.0) (2026-07-18)


### Features

* **sdk:** fTTP dispatcher pseudonym resolution — optional, feature-toggled ([#21](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/21)) ([27070f1](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/27070f15a12569e35272b7f00c3d1711aa3f31f9))
* **skills:** docs-auditor + unified arc42-generator + release/branching setup wizards ([#22](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/22)) ([49d3ebb](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/49d3ebb92c11c8d52fd0440f2527f7f7dd50ae28))
* topic-exchange routing — publish only to addressed sites (ADR-006 rev.) ([#20](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/20)) ([6284070](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/62840706b151ff85eaad49d493f20d16fac512e1))


### Documentation

* consolidate architecture docs into split docs/arc42/ (apply arc42 template) ([#23](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/23)) ([57efee2](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/57efee2b44f1b9e0329320acda73a5dd9ccb348d))
* fTTP FHIR dispatcher plan update + secure mTLS certificate provisioning ([#17](https://github.com/forschungsgruppe-digital-health/pds-query-broker/issues/17)) ([e7ffe27](https://github.com/forschungsgruppe-digital-health/pds-query-broker/commit/e7ffe27b5ed56cde0783be004502e132ed808abd))

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
