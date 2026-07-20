# Architecture Documentation — Query Broker (arc42)

> Version 0.10.0 · 2026-07-20 · Structured according to the [arc42](https://arc42.org/) template v9.0 (July 2025). Not all sections are filled in at the current project stage.
> Split into one file per arc42 section. This index is the single version+date source.

> **New here?** Start with [about-arc42.md](about-arc42.md) (what arc42 is and how to read this), then
> [1. Introduction and Goals](01_introduction_and_goals.md). Every section opens with a plain-language
> lede for newcomers, then precise detail for experts.

## Sections

| Section | In brief |
|---------|----------|
| [1. Introduction and Goals](01_introduction_and_goals.md) | What the Query Broker is, the quality goals that drive its design, and who depends on it. |
| [2. Architecture Constraints](02_architecture_constraints.md) | The fixed technical and organizational constraints the architecture has to work within. |
| [3. Context and Scope](03_context_and_scope.md) | The system boundary — the portal, the primary data sources, and the trusted third party it talks to, and in which formats. |
| [4. Solution Strategy](04_solution_strategy.md) | The handful of fundamental decisions (FHIR messaging, the operation *triple*, adapter connectors) that shape everything else. |
| [5. Building Block View](05_building_block_view.md) | How the system decomposes into parts — broker, message catalog, connectors, RabbitMQ — each with a diagram and a role table. |
| [6. Runtime View](06_runtime_view.md) | How those parts collaborate at run time, shown as step-by-step sequence diagrams (e.g. retrieving diagnoses). |
| [7. Deployment View](07_deployment_view.md) | Where the software runs: central infrastructure vs. the per-site PDS networks, and why connections are outbound-only. |
| [8. Cross-cutting Concepts](08_concepts.md) | Concepts that cut across the whole system — FHIR messaging, profile conformance, the error model, provenance/audit, multi-client routing. |
| [9. Architecture Decisions](09_architecture_decisions.md) | The significant architecture decisions, each recorded as an ADR (context → decision → rationale). |
| [10. Quality Requirements](10_quality_requirements.md) | The quality requirements made concrete as measurable scenarios (stimulus → response → metric). |
| [11. Risks and Technical Debt](11_technical_risks.md) | Known risks and technical debt, with mitigations. |
| [12. Glossary](12_glossary.md) | The shared vocabulary — every domain and technical term used across these docs. |
| [about-arc42.md](about-arc42.md) | What arc42 is and how to read this documentation |

## Conventions

- One file per section, basenames matching the [arc42 golden master](https://github.com/arc42/arc42-template).
- **Novice + expert:** every section starts with a plain-language lede, then expert detail.
- **Diagrams:** Mermaid, no colors/styles.
- **ADRs:** recorded inline in [9. Architecture Decisions](09_architecture_decisions.md) (ADR-001 … ADR-013).
- Maintained with the `arc42-generator` skill, using `docs-auditor` as the quality gate.
