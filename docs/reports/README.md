# Skill-run reports

Every analysis-skill run persists its result here as **`<skill-name>-<YYYY-MM-DD>.md`** so everyone
involved can read it — not only whoever happened to run the skill.

## The convention

- **Filename:** `docs/reports/<skill-name>-<date>.md`. When one skill runs against different scopes on
  the same day, append a short scope slug: `<skill-name>-<date>-<scope>.md`.
- **Provenance header (first block of every report):** the audited commit
  (`git rev-parse --short HEAD`), the date, the scope/inputs, the method (which specialist skills or
  subagents ran), and — where a previous report exists — a pointer to it, so the diff between two dated
  reports shows the trend.
- **Immutable snapshots:** a report is never edited after its day; a re-run creates a NEW dated file.
  (A same-day re-run on the same scope may overwrite its own file.)
- **Write scope:** for the read-only analysis skills (e.g. [`docs-auditor`](../../skills/docs-auditor/SKILL.md)),
  the dated report is the ONLY repository write a run makes — its findings and suggested fixes stay
  proposals the human applies.
- **Delivery:** like any change — via a PR into `main`, never a direct push.

Living, curated documents — the arc42 architecture docs (`docs/arc42/`), `CONTRIBUTING.md`,
`PDS_INTEGRATION.md` — are **not** reports: skills draft input for them here, and humans merge the
accepted parts into the living doc. The [`arc42-generator`](../../skills/arc42-generator/SKILL.md) skill is the
exception that writes the arc42 docs directly, using `docs-auditor`'s report as its quality gate.
