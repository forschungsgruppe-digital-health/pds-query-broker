---
name: architecture-dev
description: Evolve the Query Broker architecture — AMQP topology, FHIR messaging semantics, catalog design, ADRs. Use when evaluating technologies/standards, extending the topology, or making any architecture-level change across specs/, docker/, ig/ and docs/ARCHITECTURE.md.
---

# Skill: architecture development

You are a specialist for evolving the Query Broker architecture.

## Core architecture concepts

- **FHIR Messaging** over AMQP (RabbitMQ)
- **Triple**: OperationDefinition + MessageDefinition + GraphDefinition
- **Message catalog**: FHIR server holding the operation definitions
- **PDS connectors**: adapter pattern, generated stub + handlers
- **Profile binding**: optional via `targetProfile` (project-specific)
- **Multi-client routing**: `MessageHeader.destination` + AMQP `replyTo`
- **Provenance + AuditEvent**: data provenance and processing log

## Artifacts and their roles

| Artifact | File | Role |
|----------|------|------|
| AsyncAPI spec | `specs/pds-connector-base.yaml` | AMQP topology, content type |
| Docker Compose | `docker/docker-compose.yaml` | Local infrastructure |
| RabbitMQ definitions | `docker/rabbitmq/definitions.json` | Exchanges, queues, bindings |
| Catalog seed | `docker/catalog-seed/seed.sh` | Catalog population |
| FSH profiles | `ig/input/fsh/profiles/` | FHIR infrastructure profiles |
| IG config | `ig/sushi-config.yaml` | IG metadata, dependencies |

## Rules

1. **Separate transport and semantics** — AsyncAPI only for the AMQP topology. Message semantics live in FHIR.
2. **Profile binding is optional** — never describe it as inherent to the architecture. MII KDS is an application example.
3. **PDS, not DIZ** — neutral abbreviation for primary data sources.
4. **FHIR conformance** — OperationDefinition names in PascalCase (opd-0). MessageDefinition requires `date`.
5. **Multi-client** — every architecture change must account for multi-client routing (destination + replyTo).
6. **New exchanges/queues** — register in `docker/rabbitmq/definitions.json` AND `specs/pds-connector-base.yaml`.
7. **New ADRs** — number sequentially (currently up to ADR-009), in `docs/ARCHITECTURE.md` section 9.
8. **Changelog** — do not edit `CHANGELOG.md` by hand; describe the change in Conventional-Commit messages (release-please generates the changelog).

## Typical tasks

- "Evaluate [technology/standard] for [purpose]" → analysis with pros/cons, possibly an ADR
- "Extend the topology by [channel]" → AsyncAPI + Docker + ARCHITECTURE.md
