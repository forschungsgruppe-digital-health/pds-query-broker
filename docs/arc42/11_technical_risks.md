# 11. Risks and Technical Debt

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** Known risks and technical debt, with mitigations. Terms are defined in the [glossary](12_glossary.md).

| Risk | Impact | Mitigation |
|--------|------------|----------|
| The configured profiles change | Handler output becomes invalid | Pin profile versions in the catalog, re-certification on update |
| AsyncAPI schema tooling gaps (e.g. `allOf`) | Stub generation could become fragile *if* the spec is extended with composed schemas | Keep the AsyncAPI spec minimal (transport only); semantics stay in FHIR. The current spec uses no `allOf`. |
| No authorization implemented | Third-party applications could access arbitrary data | Define SMART on FHIR scopes before production use |
| Fanout scaling with many sites | In the legacy fanout mode every connector receives every message | **Resolved (ADR-006 rev.):** the topic exchange `pds.topic` with routing key `pds.{pdsId}.request` is now the default (`broker.routing-mode=topic`) — each request reaches only addressed sites, with per-site pseudonym trimming. Fanout remains a selectable fallback for rollout. |
