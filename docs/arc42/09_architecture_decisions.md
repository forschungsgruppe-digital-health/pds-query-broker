# 9. Architecture Decisions

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** The significant architecture decisions, each recorded as an ADR (context → decision → rationale). Terms are defined in the [glossary](12_glossary.md).

## ADR-001: Adapter Pattern Instead of Proxy for Connectors

**Context:** PDS data systems do not speak the same interface as the broker.
**Decision:** Adapter pattern — structural translation on both sides.
**Rationale:** A proxy presupposes an identical interface. PDS with heterogeneous systems require translation.

## ADR-002: FHIR Message Bundles Instead of a Proprietary JSON Envelope

**Context:** Messages between broker and connectors need a defined format.
**Decision:** FHIR Message Bundles with MessageHeader, Parameters, OperationOutcome.
**Rationale:** One format, one parser. The FHIR spec permits operation invocation via messaging.

## ADR-003: Triple of OperationDefinition + MessageDefinition + GraphDefinition

**Context:** An OperationDefinition alone does not describe the complete message contract.
**Decision:** MessageDefinition for mandatory payloads + allowed responses. GraphDefinition for payload structure.
**Rationale:** Validatable contracts instead of implicit convention.

## ADR-004: CapabilityStatement.messaging Instead of Proprietary Discovery

**Context:** Connectors must declare which operations they support.
**Decision:** `CapabilityStatement.messaging.supportedMessage`.
**Rationale:** FHIR-native, standardized, queryable.

## ADR-005: Pseudonyms as Parameters Identifiers (Not a MessageHeader Extension)

**Context:** Pseudonyms must be transmitted in the request.
**Decision:** `parameter` of type `Identifier`, `system` = gPAS domain.
**Rationale:** Pseudonyms are operation parameters, not control information.

## ADR-006: Fanout Exchange to Start, Topic as the Target Architecture

**Context:** Simple start vs. precise routing.
**Decision:** Fanout for the prototype, Topic with `pds.{pdsId}.*` as the system grows. 

**Revision (2026-07, topic-exchange increment):** the topic routing is now implemented and the default. The broker derives the addressed sites from the pseudonym gPAS domains (`primaryDataSourceIdOf` = last path segment of the domain) and publishes each request only to those sites on the topic exchange `pds.topic` with routing key `pds.{pdsId}.request`; a connector binds its queue with `pds.{pdsId}.request` and receives only requests addressed to it. **Unaddressed sites receive nothing** — a confidentiality improvement over the fanout broadcast (pinned by an integration test). The migration is additive: connectors DUAL-bind their queue (fanout `pds.broadcast` AND topic `pds.topic`), self-filtering by gPAS domain remains as a safety net, and `broker.routing-mode` (`topic` default | `fanout`) selects the topology so either works during rollout. The AsyncAPI spec (`pds.topic` channel) and `docker/rabbitmq/definitions.json` were updated in lockstep.

**Revision (2026-07, per-site pseudonym filtering):** in topic routing mode the broker now publishes to each addressed site a request bundle whose Parameters contain **only that site's pseudonym(s)** — selected via `primaryDataSourceIdOf(Identifier.system) == pdsId`, the same domain→site mapping that derives the addressed sites (`BrokerMessages.requestBundleForSite`). The MessageHeader, its `focus` reference, and all non-pseudonym operation parameters are preserved; the source bundle is never mutated (each site gets its own deep copy). A primary data source therefore **never receives another site's pseudonym** — data minimization / need-to-know, pinned by unit tests and an integration test (`eachAddressedSiteReceivesOnlyItsOwnPseudonym`). The IG already permitted this (the `BrokerRequestParameters` pseudonym slice is `1..*`, min 1), so no profile change was required. Connector-side self-filtering by exact gPAS domain is **retained as defense-in-depth** and remains the confidentiality mechanism for **fanout mode**, where a single broadcast message cannot be differentiated per destination and still carries all pseudonyms (per-site trimming is topic-only). Assumption: one gPAS domain per `pdsId` (distinct last path segments); if two domains ever shared a `pdsId`, both would reach that site's copy and the connector's exact-domain filter would drop the non-owned one — no functional leak, but the absolute "never sees another's pseudonym" claim relies on this convention.

## ADR-007: PascalCase Names for OperationDefinitions

**Context:** FHIR constraint opd-0 requires `[A-Z]([A-Za-z0-9_]){1,254}`.
**Decision:** PascalCase without underscores (e.g. `GetConditions`), analogous to the FHIR core specification.
**Rationale:** Consistency with HL7 practice. Underscores are valid but uncommon.

## ADR-008: Provenance + AuditEvent Instead of Proprietary Logging

**Context:** Data origin and the processing log must be traceable in the aggregated Bundle.
**Decision:** `Provenance` for data origin per resource (created by the connector), `AuditEvent` for processing steps (created by connector + broker). `Resource.meta.source` as a lightweight short reference. Everything transported as regular Bundle entries.
**Rationale:** FHIR-native resources, no proprietary log format. Provenance and AuditEvent are standardized FHIR R4 resources with defined semantics ([FHIR R4 Provenance](https://hl7.org/fhir/R4/provenance.html), [FHIR R4 AuditEvent](https://hl7.org/fhir/R4/auditevent.html)). The path of every resource from source to display is reconstructible.

## ADR-009: MessageHeader.destination for Multi-Client Routing

**Context:** Multiple requesting systems (portal, CDSS, research portal) can submit requests through the broker concurrently. Without a discriminator at the message and routing level, the broker cannot assign the aggregated response to the correct requesting system.
**Decision:** `MessageHeader.destination.endpoint` is set in the request by the requesting system to a system-specific response queue (e.g. `amqp://.../responses.portal`). The broker reads this value and publishes the aggregated Bundle to the corresponding queue. At the AMQP level, `replyTo` correlates in parallel with `destination.endpoint`. For requests without `destination`, a default response queue is used (`responses.default`).
**Rationale:** FHIR `MessageHeader.destination` is the standardized mechanism for message routing ([FHIR R4 MessageHeader.destination](https://hl7.org/fhir/R4/messageheader-definitions.html#MessageHeader.destination)). The combination with AMQP `replyTo` ensures consistency at both levels (FHIR semantics + transport).

## ADR-010: Implementation as Gradle Multi-Module Monorepo (Java 21 / Spring Boot 3 / HAPI FHIR 8)

**Context:** The repository is specification-first; the broker, connector SDK, and reference connector must now be implemented. The specification artifacts (IG, AsyncAPI, catalog) and the code that realizes them evolve in lockstep — an operation change touches the triple, the handler contract, and the conformance tests together. The first consumer is the MiHUB patient portal, whose backend toolchain is Java 21 + HAPI FHIR 8.
**Decision:** The implementation lives in this repository as a Gradle multi-module build (`broker/`, `connector-sdk/`, `connectors/pds-example/`, `conformance/`) on Java 21 (LTS), Spring Boot 3.x with Spring AMQP, and HAPI FHIR 8.x; tests use JUnit 5 + AssertJ + Testcontainers. All modules share the repository version (`version.txt`, managed by release-please); broker and reference connector are released as container images, and the connector SDK is published to a Maven registry only once an external PDS consumer exists. The BFF is a patient-portal component and is implemented in the portal repository — this repository stays consumer-agnostic.
**Rationale:** Co-locating contract and implementation makes spec/code drift impossible to merge unnoticed (one PR changes triple + code + conformance test) and keeps the conformance harness next to the artifacts it enforces. A single org-wide stack (matching the portal) minimizes cognitive and operational overhead; a single repo version keeps release automation trivial until independent SDK versioning is actually demanded by external consumers.

**Revision (2026-07, Spring Boot 4 migration, #41):** the stack was upgraded to **Spring Boot 4.1.0 (Spring Framework 7)**, and the test toolchain to **JUnit 6.1 (junit-bom 6.1.2)** and **Testcontainers 2.0 (testcontainers-bom 2.0.5)**; the Gradle wrapper is 9.6.1. Java 21 (LTS), HAPI FHIR 8.2.0 (used as plain libraries), AssertJ 3.27.7, the four-module layout, single repo-wide `version.txt`, and release-please are unchanged. The heading's "Spring Boot 3" reflects the original decision only.

## ADR-011: Staged Pilot Implementation — Walking Skeleton First

**Context:** The architecture describes registry, router, aggregator, profile validation, provenance/audit, THS integration, and multi-client routing. Implementing all of it before anything runs end-to-end would defer integration risk (correlation, timeouts, routing) to the end. The pilot consumer is the patient portal; data-protection constraints (DPIA, no real THS access) gate several capabilities.
**Decision:** Implementation proceeds in staged increments. **Increment 1, work package 0 (communication profiles):** before any code, the IG's communication-profile layer is completed — Bundle profiles for the request/response message envelopes (`BrokerRequestBundle`, `BrokerResponseBundle`), a `BrokerOperationOutcome` profile with a broker error CodeSystem/ValueSet pinning the § 8.6 error model, and the hitherto dangling `OperationError` MessageDefinition — compiled at 0/0 and mirrored into `catalog/`; skeleton tests assert conformance against these profiles. **Increment 1 (walking skeleton):** one vertical slice of `$GetConditions` over the existing fanout topology — broker (catalog load, publish, correlation, timeout → `OperationOutcome`) and reference connector (synthetic in-memory data, static pseudonym map behind a `TrustedThirdPartyClient` port); done when `docker compose up` runs the full loop and an integration test proves it; no validation, no BFF, no real THS. **Increment 2:** conformance harness as a Gradle module (catalog-driven golden tests, CI gate), SDK runtime validation active only when a `targetProfile` is configured, two synthetic connector instances with distinct PDS IDs/pseudonym domains/datasets (proving fan-out, self-filtering, aggregation, partial responses, per-PDS provenance), and MOSAiC gPAS/E-PIX dev containers replacing the static map. **Increments 2–3:** migration from fanout to the `pds.{pdsId}.*` topic exchange as a named increment with its own revision of ADR-006 (additive, dual binding during migration). **Portal integration:** BFF read-through with ephemeral responses — no portal-side persistence; any cache/import layer requires its own ADR and DPIA assessment. **Security staging:** compose-internal for the skeleton; for the pilot, the BFF (portal Keycloak JWT) is the sole entry point, RabbitMQ uses dedicated per-service users/vhost with TLS at deployment, messages carry pseudonyms only, and user context stays in BFF audit events; end-to-end token propagation is deferred to a future security ADR. **Out of pilot scope:** SMART third-party app access, IHE mCSD service directory, any real hospital PDS site (program-level, governance-gated).
**Rationale:** A thin end-to-end slice validates the riskiest, most novel mechanics (correlation, aggregation, timeout semantics) earliest and cheapest; every later capability lands on a proven transport. Staging THS, validation, and security keeps the DPIA surface minimal during development while preserving the documented target architecture — each deferred capability has a defined landing slot instead of being silently dropped. Full details per increment: [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md).

## ADR-012: Content-Profile Strategy — MII KDS First, IPA-Aligned Facade Boundary

**Context:** Profile binding in this architecture is deliberately project-specific (ADR-003, § 8.2), and the pilot now needs concrete bindings. A dedicated research pass over European and international patient-access FHIR IGs ([research report 2026-07-16](../reports/patient-access-ig-research-2026-07-16.md), all findings source-verified) established: (1) the data integration centers (DICs — this project's primary-data-source sites) natively hold **MII Kerndatensatz (KDS)**-shaped data, and no MII patient-access IG exists; (2) **HL7 International Patient Access (IPA) 1.1.0** is the universal patient-access baseline, and the EHDS-track **EU Health Data API** (1.0.0-ballot, candidate spec for the EHDS Art. 15/Annex II Implementing Acts) hard-depends on it; (3) **no published patient-access IG uses FHIR messaging** — all prescribe synchronous REST with OAuth2/SMART-family authorization; the closest precedent to this architecture is IHE MHD acting as a synchronous facade over federated XDS/XCA; (4) the EU Health Data API models patient access as a trusted "Health Data Access Service" (system-to-system after eID login, SMART App Launch out of scope) — matching this project's BFF pattern.
**Decision:** A two-layer content-profile strategy. **Layer 1 (DIC-facing, now):** MII KDS module profiles are the `targetProfile`/`focus.profile`/`target.profile` bindings of the operation catalog, starting with `GetConditions` → MII KDS Diagnose (Condition); KDS packages become IG dependencies and are made available to the SDK/harness validators via FHIR package loading. **Layer 2 (portal-facing, staged):** IPA 1.1.0 is the minimum content bar — broker responses are additionally validated against IPA profiles in the conformance harness as the operation set grows, and the future BFF (portal repository) is built as the IPA-shaped synchronous facade; IPA/EU-HDAA REST conformance is claimed only at that facade, never by the broker. The EU Health Data API and HL7 EU Patient Summary ballots go on a watch list; this ADR is re-evaluated when the EU Health Data API reaches its first non-ballot release.
**Rationale:** Mapping cost is lowest at the connector when the wire profiles match what DICs already hold (KDS); IPA's deliberately minimal "minimum expectations" philosophy makes dual KDS+IPA validation cheap; anchoring the facade on IPA keeps the system on the EHDS convergence path (the EU Health Data API composes IPA + MHD) without betting on a ballot-stage spec; and the facade-over-federation split has standards precedent (MHD-over-XDS), giving the messaging backbone a defensible conformance story: *IPA-aligned content over a federated messaging backbone, with the portal-facing facade as the conformance boundary.*

## ADR-013: Generic Remote Terminology-Server Integration — SU-TermServ for the Pilot

**Context:** ADR-012 deliberately parked terminology/binding validation: the national code systems bound by the MII KDS profiles (ICD-10-GM, ATC, …) live in terminology packages too large to vendor, and the pilot had no terminology server. The MII operates a central terminology service — the SU-TermServ (an Ontoserver-based service; access requires an approved application and an mTLS client certificate) — which hosts exactly those national terminologies. The integration must be generic so other FHIR terminology servers (e.g. a project-run CSIRO Ontoserver) are configuration, not code.
**Decision:** Terminology validation is delegated to a remote FHIR terminology server via HAPI's `RemoteTerminologyServiceValidationSupport` (standard `$validate-code`/`$expand`/`$lookup` operations — server-agnostic by construction), configured through a single `TerminologyServerConfig` (FHIR base URL + optional PKCS12 client keystore/truststore for mutual TLS). The remote support is placed BEFORE the in-memory expander so server answers are not shadowed by local expansion failures. Activation is strictly opt-in: connector configuration (`pds.connector.terminology.*`) or environment variables (`TERMINOLOGY_SERVER_URL` + mTLS variables) for the conformance harness; with no server configured, the ADR-012 structural-only behavior stands unchanged. Certificates and keys are never committed — they are deployment-supplied (the SU-TermServ onboarding artifacts). Implementation note: HAPI's `GenericClient` resolves its HTTP client per request via `FhirContext.getRestfulClientFactory()`, so the mTLS-capable client factory is installed on a dedicated client `FhirContext` (empirically verified; a factory merely passed to the support constructor is bypassed).
**Rationale:** The standard FHIR terminology operations make the integration server-neutral — SU-TermServ vs. Ontoserver vs. any other conformant server is a URL and a certificate. The SU-TermServ is the natural pilot choice because it authoritatively hosts the German national terminologies the KDS bindings require. mTLS is proven by tests against a client-auth-enforcing mock (key material generated at test time, nothing committed); real-server connectivity follows once the application is approved and the certificate issued.
