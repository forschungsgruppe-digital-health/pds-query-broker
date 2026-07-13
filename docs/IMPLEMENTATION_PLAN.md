# Implementation Plan — PDS Query Broker

> Status: **agreed** (decision session 2026-07-13) · Governing ADRs: [ADR-010, ADR-011](ARCHITECTURE.md#9-architecture-decisions)

This plan turns the specification-first repository into a working broker + connector-SDK + reference-connector implementation. Decisions below were made explicitly; changes to them require revisiting the corresponding ADR.

## Goal and first consumer

The **MiHUB patient portal** is the first real consumer: the broker must federate at least `$GetConditions` into portal views, via a **BFF that lives in the portal repository** (this repo stays consumer-agnostic). Portal integration is **read-through and ephemeral** — federated results are never persisted portal-side; a TTL cache/import layer may come later only with its own ADR + DPIA assessment.

## Structure and stack (ADR-010)

- **Gradle multi-module in this repo:** `broker/`, `connector-sdk/`, `connectors/pds-example/`, `conformance/` next to `specs/`, `ig/`, `catalog/`.
- **Stack:** Java 21 (LTS) · Spring Boot 3.x + Spring AMQP · HAPI FHIR 8.x · JUnit 5 + AssertJ + Testcontainers.
- **Versioning/delivery:** one repo-wide SemVer (release-please, `version.txt` feeds Gradle). Broker + example connector as container images (GHCR) on release tags. `connector-sdk` published to a Maven registry only when the first external PDS consumer exists.

## Increments (ADR-011)

### Increment 1 — walking skeleton (start: now; ~2–3 working sessions)

One thin vertical slice of `$GetConditions` on the **existing fanout topology**:

1. Request message bundle → broker: loads the triple from the catalog server, validates well-formedness (no profile validation), publishes to `pds.broadcast`, tracks correlation.
2. Reference connector: consumes from `req.{pdsId}`, resolves the pseudonym via a **`ThsClient` port backed by a static synthetic map** (config file), serves synthetic in-memory data, replies to the response queue.
3. Broker aggregates, honors `MessageHeader.destination` + `replyTo`, and converts timeouts/missing responses into an `OperationOutcome`.

**Definition of done:** `docker compose up` runs the whole loop end-to-end, and an integration test (Testcontainers: RabbitMQ + HAPI catalog) proves request → aggregated response including the timeout path. CI builds and runs it.

**Explicitly not in increment 1:** profile validation, BFF, real THS, provenance completeness, auth.

### Increment 2 — conformance, multi-PDS, THS fidelity

- **Conformance harness** (`conformance/` module): catalog-driven golden tests with synthetic test data under `catalog/testdata/GetConditions/`, runnable against any connector via Testcontainers; becomes the CI gate for connector changes.
- **SDK runtime validation:** active only when a `targetProfile` is configured (exactly as documented in CONTRIBUTING/PDS_INTEGRATION), otherwise off.
- **Two synthetic connector instances** with distinct PDS IDs, pseudonym domains, and disjoint synthetic datasets — proving broadcast fan-out, self-filtering, aggregation, partial-response/timeout handling, and per-PDS `Provenance`.
- **MOSAiC gPAS/E-PIX dev containers** in the compose stack replace the static pseudonym map (config swap behind the `ThsClient` port — no code change). Real federated THS remains governance-gated.

### Increments 2–3 — topic-exchange migration (named increment)

Migration from fanout to the `pds.{pdsId}.*` **topic exchange** as its own increment with a revision of ADR-006 and synchronized updates to `specs/pds-connector-base.yaml`, `docker/rabbitmq/definitions.json`, and the IG. Additive rollout: connectors dual-bind during migration; self-filtering remains as a safety net.

### Portal integration increment (portal repo)

BFF in `cross-hub-patientportal`: validates portal-Keycloak JWTs (sole entry point), maps portal identity → pseudonyms, submits broker queries, shapes responses for portal views. Cross-repo integration tested via a compose overlay.

## Security staging (ADR-011)

| Stage | Measures |
|-------|----------|
| Skeleton | Compose-internal network, dev credentials, synthetic data only |
| Pilot | BFF as sole authenticated entry (portal Keycloak); dedicated per-service RabbitMQ users + vhost; TLS on AMQP at deployment; messages carry pseudonyms only; user context stays in BFF audit events |
| Deferred | End-to-end auth-context propagation in messages → future security ADR (multi-site trigger) |

## Out of pilot scope (non-goals)

- SMART on FHIR third-party app access (portal only)
- IHE mCSD service directory (discovery stays CapabilityStatement + static config)
- Any **real hospital PDS site** — program-level commitment, governance-gated (data-sharing agreements, real THS onboarding, DPIA)

## Working mode

AI-agent sessions in small, trunk-based PRs (Conventional Commits, squash-merge) with human review on every merge to `main` — matching the org's established portal workflow. Every increment lands with its tests; CI (build + test + existing spec validations) must be green before merge.
