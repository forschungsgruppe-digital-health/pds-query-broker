# Query Broker — Claude Code Context

@AGENTS.md

> Project context, repository map, conventions, branching/release rules, architecture invariants,
> and the vendor-neutral skills catalog are in the `AGENTS.md` imported above (canonical — also
> applies to Codex, Cursor, Gemini, Copilot). This file supplements **only** Claude-Code-specific
> working instructions.

## Working with Claude Code

- Skills are auto-discovered via `.claude/skills/` (symlinks into `skills/` — edit `skills/` only):
  `arc42-docs`, `architecture-dev`, `fhir-ig`.
- Use the `fhir-ig` skill for ALL changes under `ig/` and `catalog/`; always run
  `cd ig && sushi build` afterwards (0 errors, 0 warnings).
- Any architecture-level change: use `architecture-dev` and keep
  `specs/` + `docker/rabbitmq/definitions.json` + `docs/ARCHITECTURE.md` in sync; document
  decisions as ADRs (`arc42-docs`).
- All changes via short-lived branch → PR into `main` (trunk-based, squash-merge,
  Conventional-Commit title). Do not hand-edit release-please-owned files
  (`CHANGELOG.md`, `version.txt`, `.release-please-manifest.json`).
