# AGENTS.md — Query Broker (PDS)

Operational context for AI coding agents (OpenAI Codex, Cursor, GitHub Copilot, Gemini CLI, and
others that read the open AGENTS.md standard). This file is self-contained and **canonical**:
Claude Code reads `CLAUDE.md`, which imports this file via `@AGENTS.md` so both stay in sync.
On conflict, this file wins.

## Project

- **Project:** Federated Query Broker for distributed primary data sources (PDS), integrated via a
  patient portal and third-party applications (MiHUB / CAEHR context)
- **Owner:** TU Dresden / Forschungsgruppe Digital Health (FGDH)
- **Status:** **walking skeleton** (increment 1 per ADR-011 / `docs/IMPLEMENTATION_PLAN.md`) —
  broker, connector SDK, and a synthetic reference connector run `$GetConditions` end-to-end over
  the fanout topology (Gradle modules `broker/`, `connector-sdk/`, `connectors/pds-example/`).
  Still staged for later increments: profile validation, conformance harness, BFF, real THS, auth.

## Tech stack (current + planned)

- **Specs:** FHIR R4 (FSH/SUSHI ImplementationGuide), AsyncAPI 3.0 (AMQP 0-9-1)
- **Infrastructure (dev):** Docker Compose — RabbitMQ 3.13, HAPI FHIR catalog server, catalog seed,
  AsyncAPI Studio
- **Implementation:** Java 21 (Gradle toolchain), Spring Boot 3.x + Spring AMQP, HAPI FHIR 8.x,
  JUnit 5 + AssertJ + Testcontainers (ADR-010)
- **Pseudonymization:** MOSAiC (E-PIX, gPAS) via trusted third party (THS)

## Repository map

- `docs/arc42/` — arc42 v9.0 architecture documentation, split one file per section (12 sections, ADR-001…ADR-013); the legacy `docs/ARCHITECTURE.md` is a redirect stub
- `PDS_INTEGRATION.md` — language-agnostic implementation guide for PDS connector developers
- `CONTRIBUTING.md` — development setup, connector guide, conventions, branching & releases
- `specs/pds-connector-base.yaml` — AsyncAPI transport contract (AMQP topology only)
- `catalog/` — FHIR message catalog (OperationDefinition, MessageDefinition, GraphDefinition)
- `ig/` — FHIR ImplementationGuide project (FSH sources, sushi-config.yaml, page content)
- `docker/` — local dev stack (`cd docker && cp .env.example .env && docker compose up -d`)
- `skills/` — vendor-neutral agent skills (see catalog below)

## Common tasks

- **Compile the FHIR IG:** `cd ig && sushi build` — target: 0 errors, 0 warnings
- **Render the IG (HTML):** `cd ig && ./_updatePublisher.sh && ./_genonce.sh`
- **Start the dev stack:** `cd docker && cp .env.example .env && docker compose up -d`
  (RabbitMQ 5672/15672, catalog server 8090, AsyncAPI Studio)
- **Validate AsyncAPI:** `asyncapi validate specs/pds-connector-base.yaml`
- **AsyncAPI contract test:** `pytest tools/asyncapi-stub/test_contract.py` against a running
  compose stack — generates a Python connector stub from the spec and verifies it interoperates
  with the live broker (see `tools/asyncapi-stub/README.md`; CI: `asyncapi-contract-test.yml`)
- **Build + test the implementation:** `./gradlew build` (unit + Testcontainers integration tests;
  needs a Docker daemon). Single apps: `./gradlew :broker:bootRun`,
  `./gradlew :connectors:pds-example:bootRun`

CI (GitHub Actions): AsyncAPI validation, Docker validation (compose config + hadolint + JSON/shell
checks), SUSHI validation + IG Publisher + GitHub Pages deploy, release-please. **Security**:
gitleaks (secrets), CodeQL (SAST, Java+Python), Trivy (container/IaC), Dependabot (deps, all
ecosystems) + Gradle dependency-submission (transitive graph), OpenSSF Scorecard, and an optional
AI security review on PRs (`security-review.yml`, needs the `ANTHROPIC_API_KEY` secret).

## Conventions

- **Language:** documentation AND code/config in **English**
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/) — types drive SemVer via
  release-please. Scopes: `broker`, `connector-sdk`, `conformance`, `catalog`, `specs`, `ig`,
  `docs`, `docker`
- **FHIR profiling is FSH-only:** ALL FHIR conformance artifacts (profiles, CodeSystems, ValueSets,
  OperationDefinitions, MessageDefinitions, GraphDefinitions) are authored in FHIR Shorthand under
  `ig/input/fsh/` and compiled with SUSHI (`cd ig && sushi build`, target 0/0). The JSON under
  `catalog/` is **generated output** — regenerate it with `python3 ig/scripts/mirror-catalog.py`
  after every build; never edit catalog JSON by hand (CI fails on drift)
- **OperationDefinition names:** PascalCase, FHIR constraint opd-0 (e.g. `GetConditions`)
- **Prefix:** PDS (primary data source), never DIZ
- **AMQP topology:** `pds.broadcast`, `req.{pdsId}`, `responses.{systemId}`, `pds.dlq`
- **Response routing:** `MessageHeader.destination.endpoint` + AMQP `replyTo`
- **Mermaid diagrams:** no colors/styles, default rendering only
- **Code blocks:** only for real code/pseudocode/shell; structured content as tables/lists/Mermaid

## Branching & releases (trunk-based)

- `main` is the trunk and only long-lived branch; it must always be releasable.
- Short-lived branches (`feat/*`, `fix/*`, `docs/*`, `chore/*`) → PR into `main`, squash-merge with
  a Conventional-Commit title. No `develop`/`release/*` branches.
- Releases are automated by **release-please** (`.github/workflows/release-please.yml`,
  `release-please-config.json`, `.release-please-manifest.json`): merging the release PR tags
  `vX.Y.Z`, creates the GitHub release, and updates `version.txt`, `CHANGELOG.md`, and the version
  headers in `README.md`/`CONTRIBUTING.md`. Pre-1.0 (`bump-minor-pre-major`): `feat:` → minor,
  `fix:`/`perf:` → patch, breaking → minor.
- **Never edit by hand:** `CHANGELOG.md`, `version.txt`, `.release-please-manifest.json`.

## Architecture core concepts

1. **FHIR Messaging** — all messages are FHIR R4 Bundles (type `message`) over AMQP (RabbitMQ)
2. **Triple** — every operation = OperationDefinition + MessageDefinition + GraphDefinition
3. **Profile binding** — optional via `targetProfile`, `focus.profile`, `target.profile`;
   project-specific (MII KDS is an example, not a requirement)
4. **Adapter pattern** — connectors translate between broker protocol and local systems
5. **Broadcast + self-filtering** — fanout exchange; connectors filter by pseudonym domain
6. **Multi-client routing** — `MessageHeader.destination` + system-specific response queues
7. **Provenance + AuditEvent** — data provenance and processing log as FHIR resources

## Architecture invariants (do not violate)

- AsyncAPI describes ONLY the AMQP topology; message semantics live in FHIR resources.
- Profile binding is optional and project-specific — never architecturally required.
- New exchanges/queues must be registered in BOTH `docker/rabbitmq/definitions.json` and
  `specs/pds-connector-base.yaml`.
- Architecture decisions are recorded as ADRs in `docs/arc42/09_architecture_decisions.md` (sequential numbering).
- FSH (`ig/input/fsh/`) is the single source of truth for FHIR conformance artifacts; `catalog/`
  holds only SUSHI-generated mirrors (enforced by the CI drift gate in `ig-build.yml`).

## Hard boundaries (NEVER do these)

- NEVER commit secrets; `docker/.env` is gitignored, only `.env.example` is committed.
- NEVER put patient data — even synthetic data with realistic names — in the repo or a prompt;
  use obviously artificial test data only.
- NEVER interact with THS/TTP production systems (E-PIX/gPAS); local dev or public demo
  instances only.
- NEVER change the FHIR version (R4) without an ADR.

## Agent capabilities (skills) — vendor-neutral catalog

The portable form is **Agent Skills** (`skills/<name>/SKILL.md`,
[agentskills.io](https://agentskills.io) standard). `skills/` is the single source of truth; see
`skills/SKILLS_SETUP.md` for the wiring.

- **`arc42-generator`** — generate the arc42 v9.0 architecture docs from code (no speculation) OR
  consolidate scattered docs into a split `docs/arc42/` (one file per section, novice+expert
  readable), reusing `docs-auditor` as the before/after quality gate. The split `docs/arc42/` is
  canonical; the legacy `docs/ARCHITECTURE.md` is a redirect stub
- **`architecture-dev`** — evolve the architecture (AMQP topology, FHIR messaging semantics,
  catalog design, ADRs) consistently across `specs/`, `docker/`, `ig/`, `docs/`
- **`fhir-ig`** — FHIR R4 profiling with FSH/SUSHI (profiles, operation triples, examples,
  page content)
- **`docs-auditor`** — audit ALL repo documentation against five bars (complete, consistent,
  error-free vs. the live code/specs, understandable for novice+expert, usable), verify every
  link/anchor, and return a report plus ready-to-apply fix suggestions. Read-only on the docs (writes
  only its dated report under `docs/reports/`); reuses the specialist skills and is the gate
  `arc42-generator` runs against
- **`release-manager`** — set up (or change) CI-based release management; first run presents the
  options (SemVer/CalVer, release-please/semantic-release/…) and scaffolds the choice + an ADR. This
  repo is already release-please + SemVer, so it detects that and only offers adjustments
- **`branching-strategist`** — set up (or change) a branching strategy; first run presents the options
  (trunk-based/GitHub Flow/…) and scaffolds the choice + an ADR. This repo is already trunk-based, so
  it detects that and only offers adjustments
- **`security-reviewer`** — read-only, authorized security review of this system: STRIDE threat model
  + a fixed checklist (authz/tenancy isolation, secrets, injection, crypto, PII/pseudonym handling,
  deps/supply-chain, container, DoS) + triage of the scanner output (CodeQL/Trivy/Dependabot/gitleaks);
  returns a dated `docs/reports/` report + ready-to-apply fixes. Findings are leads a human confirms.
  Complements the PR-time `security-review.yml` (claude-code-security-review) Action
- **`dependency-scanner`** — setup wizard (write-capable): set up (or change) CI dependency/CVE/supply-chain
  scanning; first run detects the stack + visibility, presents options (Dependabot/Renovate, Trivy/Grype/OSV,
  Scorecard, SBOM) and scaffolds config + an ADR. Idempotent — detects this repo's existing Dependabot +
  dependency-submission + Trivy + Scorecard setup and only offers adjustments
- **`security-scanner`** — setup wizard (write-capable): set up (or change) CI security review (secret
  scanning, SAST, AI PR review); first run presents options (gitleaks, CodeQL/Semgrep,
  claude-code-security-review) and scaffolds workflows + installs `security-reviewer` + an ADR. Idempotent —
  detects this repo's existing gitleaks + CodeQL + `security-review.yml`

**How each tool accesses them:**

- **Claude Code** — via `.claude/skills/` (symlinks → `skills/`)
- **OpenAI Codex / Cursor / Gemini** — via `.codex/skills/` (symlinks → `skills/`) or the tool's
  own skills path
- **GitHub Copilot / anything else** — read this catalog and perform the skill's instructions
  inline; the SKILL.md files are plain Markdown role/workflow descriptions.

## Notes for non-Claude agents

- Treat **`AGENTS.md` as canonical**; `CLAUDE.md` only adds Claude-Code-specific supplements.
- If your tool lacks a native skills/subagent primitive, open the relevant `skills/<name>/SKILL.md`
  and follow it inline.
