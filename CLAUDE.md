# Query Broker — Claude Code Context

@AGENTS.md

> Project context, repository map, conventions, branching/release rules, architecture invariants,
> and the vendor-neutral skills catalog are in the `AGENTS.md` imported above (canonical — also
> applies to Codex, Cursor, Gemini, Copilot). This file supplements **only** Claude-Code-specific
> working instructions.

## Working with Claude Code

- Skills are auto-discovered via `.claude/skills/` (symlinks into `skills/` — edit `skills/` only):
  `arc42-generator`, `architecture-dev`, `fhir-ig`, `docs-auditor`, `release-manager`,
  `branching-strategist`.
- To check documentation health (completeness, consistency, accuracy vs. the code, novice+expert
  readability, navigability) use `docs-auditor` — it returns a report plus ready-to-apply fixes and
  is read-only on the docs. `arc42-generator` runs it as the quality gate when generating/consolidating
  `docs/arc42/`.
- Use the `fhir-ig` skill for ALL changes under `ig/` and `catalog/`; always run
  `cd ig && sushi build` afterwards (0 errors, 0 warnings), then regenerate the catalog mirror
  with `python3 ig/scripts/mirror-catalog.py`. Never author or edit FHIR conformance JSON by
  hand — FSH is the only source (CI enforces catalog/FSH sync).
- Any architecture-level change: use `architecture-dev` and keep
  `specs/` + `docker/rabbitmq/definitions.json` + `docs/ARCHITECTURE.md` in sync; document
  decisions as ADRs (`arc42-generator`).
- All changes via short-lived branch → PR into `main` (trunk-based, squash-merge,
  Conventional-Commit title). Do not hand-edit release-please-owned files
  (`CHANGELOG.md`, `version.txt`, `.release-please-manifest.json`).
