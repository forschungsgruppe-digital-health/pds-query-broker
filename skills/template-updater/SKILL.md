---
name: template-updater
description: Reconciles a repo that tracks the arc42-project-template with the latest template release — pulling the SHARED TOOLING (the portable skills + the CI setup-wizard scaffolding + governance scaffolding) while PRESERVING this repo's own content and customizations. Reads the .arc42-template.json version stamp, fetches the template at its latest release, categorizes the delta, applies it the right way (re-copy shared skills, re-run the affected setup wizards, update catalogs — never touching docs/arc42 content, filled AGENTS sections, ADRs, or child-customized skills), updates the stamp, and opens a PR. Human-in-the-loop; triggered by the template-sync-check notifier issue or run manually. Use to bring a template child up to date with the shared FGDH toolkit.
license: Apache-2.0
compatibility: Works with Claude Code, OpenAI Codex, Cursor, Gemini CLI, and other agents supporting the Agent Skills standard (agentskills.io). Reads the template + this repo; writes only the shared tooling + the stamp, via a PR.
metadata:
  author: tu-dresden-fgdh
  version: "1.0"
# Claude Code-specific extension fields (safely ignored by other agents):
tools: Read, Grep, Glob, Bash, Write, Edit
model: opus
---

# template-updater

You reconcile a repo that tracks the **arc42-project-template** with the template's **latest release** —
pulling in the **shared tooling** while **preserving this repo's own content and customizations**. You are
write-capable but tightly scoped: you touch the shared skills, the CI setup scaffolding, the tooling
catalogs, and the version stamp — **never** the repo's content (`docs/arc42/` prose, filled `AGENTS.md`
sections, ADRs, project code) or a skill this repo has deliberately customized. Human-in-the-loop; deliver
as a PR.

> **The child owns its content; the template owns the tooling.** When in doubt, do NOT overwrite —
> flag it in the PR description for a human to reconcile.

## 1. Detect the drift

```bash
have=$(jq -r '.version' .arc42-template.json)
TEMPLATE=$(jq -r '.template' .arc42-template.json)        # forschungsgruppe-digital-health/arc42-project-template
latest=$(gh release view --repo "$TEMPLATE" --json tagName -q .tagName | sed 's/^v//')
```

If `have == latest`, there is nothing to do. Otherwise fetch the template at the latest release for
comparison (a shallow clone or `gh api .../contents` / raw file reads at the tag):

```bash
git clone --depth 1 --branch "v$latest" "https://github.com/$TEMPLATE" /tmp/arc42-template
```

Read the template's `CHANGELOG.md` between `have` and `latest` to know what changed.

## 2. Categorize the delta

Compare the template's tooling surface against this repo's:

- **Shared skills** (`skills/<name>/SKILL.md`) — the portable set: `arc42-generator`, `docs-auditor`,
  `release-manager`, `branching-strategist`, `dependency-scanner`, `security-scanner`, `security-reviewer`,
  `grilling`, `grill-me`, `template-updater`. New ones to add; changed ones to update.
- **Setup-wizard scaffolding** — if a wizard (`dependency-scanner` / `security-scanner` / `release-manager`
  / `branching-strategist`) changed *what it scaffolds* (e.g. learned a new scanner), the generated CI in
  this repo may want regenerating.
- **Governance scaffolding** — `skills/SKILLS_SETUP.md`, `.github/pull_request_template.md`,
  `docs/reports/README.md`, the drift files (`template-sync-check.yml`, `.templatesyncignore`).
- **arc42 structure** — only the *structure* (section files, index), never the filled-in content.

## 3. Apply it the right way (preserve customizations)

- **New shared skill** → copy the template's `skills/<name>/SKILL.md` in, add the `.claude`/`.codex`
  symlinks, and add a catalog entry to this repo's `AGENTS.md` (+ `CLAUDE.md` / copilot bridge / README /
  SKILLS_SETUP as the repo uses them). Match this repo's catalog *format*, not the template's.
- **Changed shared skill** → re-copy the template's canonical `SKILL.md` **UNLESS this repo has customized
  it.** A skill is *customized* if it is listed in `.templatesyncignore`, carries a
  `metadata.project` / a "portal-specialized"-style note, or its body has diverged from the canonical. For
  a customized skill (e.g. a repo-specialized `security-reviewer`): do a **3-way reconcile** — port the
  canonical's improvements into the customized file while keeping the repo's extensions — or, if that is
  not clearly safe, **leave it and flag it in the PR** for manual review. Never blind-overwrite a
  customized skill.
- **Setup-wizard scaffolding changed** → prefer **re-running the affected wizard** (it is idempotent and
  detects existing config) over hand-editing; or apply the specific diff. Keep this repo's stack-specific
  choices (public/private, Gradle vs npm, etc.).
- **Governance scaffolding** → update if this repo hasn't diverged; otherwise merge the change in.
- **arc42 structure changed** → run **`arc42-generator`** (it preserves content and re-checks with
  `docs-auditor`).
- **NEVER touch:** `docs/arc42/` content, project-specific `AGENTS.md`/`README.md` prose, `docs/adr/**`,
  `docs/reports/*.md` snapshots, project code, or a customized skill you cannot safely 3-way merge.

## 4. Update the stamp

Set `.arc42-template.json` `version` to the new `latest` and `reconciled_at` to today's date. Keep the
`template` field. (Do not invent a date on Claude Code where the clock is unavailable in scripts — read it
from `git log -1 --format=%cs` or pass it in.)

## 5. Deliver

- Open a PR per this repo's branching rules (`main`, or `dev` where that is the integration branch),
  titled e.g. `chore: reconcile shared tooling with arc42-project-template v<latest>`.
- In the PR body: list what was updated (skills added/changed), what was **skipped and why** (customized
  skills, content), and any items flagged for manual reconciliation.
- Reference and close the `template-sync-check` tracking issue.
- Do not merge autonomously.

## 6. Constraints

- **Preserve customizations.** Never overwrite content or a customized skill; when unsure, flag not clobber.
- **Human-in-the-loop.** Deliver a PR; a human reviews and merges.
- **Scope = tooling + stamp.** No project code, no content.
- **Stack-aware.** Keep this repo's language/visibility choices (a private repo without GHAS keeps its
  gate-and-print CI; a Gradle repo keeps its Gradle config).

## Relationship to other skills / tooling

Consumes the **`template-sync-check`** workflow's drift issue (detection → this skill reconciles). Reuses
the setup wizards (`dependency-scanner` / `security-scanner` / `release-manager` / `branching-strategist`)
and `arc42-generator` to apply changes idempotently rather than hand-editing. It is itself one of the
shared, portable skills kept identical across the FGDH repos (single source = the template).

---
*Built on the Agent Skills open standard (agentskills.io). Portable core fields: `name`, `description`.
The fields after the `# Claude Code-specific extension fields` comment (`tools`, `model`) are
Claude-Code extensions and are ignored by agents that do not support them.*
