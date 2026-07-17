# Vendored external FHIR packages

External FHIR NPM packages (`*.tgz`) that back **profile validation** against
the content profiles bound in the operation catalog (ADR-012). They are loaded
by `CatalogProfileValidator` (connector SDK) and thereby by the conformance
harness.

These are **vendored third-party dependencies, not authored artifacts** — the
FSH-only authoring policy applies to `ig/` and the generated mirror around this
directory, not to these packages. They are committed for deterministic, offline
builds; the pinned versions must match `ig/sushi-config.yaml`.

| Package | Version | Why |
|---------|---------|-----|
| `de.medizininformatikinitiative.kerndatensatz.diagnose` | 2025.0.1 | MII KDS Diagnose — `targetProfile` of `GetConditions` |
| `de.medizininformatikinitiative.kerndatensatz.meta` | 2025.0.3 | KDS meta dependency |
| `de.basisprofil.r4` | 1.5.4 | German base profiles (KDS parent profiles) |

Not vendored: the national terminology packages (e.g. `kbv.all.st`, which
carries ICD-10-GM) — too large, and the pilot validates structure/cardinality/
slicing/invariants only. Terminology binding validation returns with a
terminology-server integration (see ADR-012 and `CatalogProfileValidator`).

Update procedure: bump the version in `ig/sushi-config.yaml`, replace the
`.tgz` here (download from `https://packages.simplifier.net/<name>/<version>`),
run `sushi build` + the mirror script, and re-run the full Gradle build.
