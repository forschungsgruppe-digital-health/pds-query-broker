# 11. Risks and Technical Debt

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** Known risks and technical debt, with mitigations. Terms are defined in the [glossary](12_glossary.md).

| Risk | Impact | Mitigation |
|--------|------------|----------|
| The configured profiles change | Handler output becomes invalid | Pin profile versions in the catalog, re-certification on update |
| AsyncAPI `allOf` tooling gaps | Stub generation fragile when the spec is extended | Keep the AsyncAPI spec minimal (transport only), semantics in FHIR |
| No authorization implemented | Third-party applications could access arbitrary data | Define SMART on FHIR scopes before production use |
| Fanout scaling | With 50+ PDS: every connector receives every message | Migrate to a Topic Exchange with `pds.{pdsId}.*` |
