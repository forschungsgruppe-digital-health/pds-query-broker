---
name: release-manager
description: Sets up (or reconfigures) CI-based release management for a repository. On the FIRST invocation in a repo it presents the options — versioning scheme (SemVer, CalVer, bundled single-version, independent per-package), automation tool (release-please, semantic-release, changesets, GoReleaser, manual tag, release-drafter), and commit/notes driver (Conventional Commits, PR labels) — recommends a default, asks the user, then scaffolds the chosen setup (CI workflow, config, version file, PR-title lint, CONTRIBUTING section) and records the decision as an ADR. Idempotent: if release automation is already configured it detects it and offers adjustments instead of re-asking. Use to bootstrap releases in a new repo or to change an existing release strategy.
license: Apache-2.0
compatibility: Works with Claude Code, OpenAI Codex, Cursor, Gemini CLI, and other agents supporting the Agent Skills standard (agentskills.io). Reads the repo, writes CI/config/docs; never publishes on its own.
metadata:
  author: tu-dresden-fgdh
  version: "1.0"
# Claude Code-specific extension fields (safely ignored by other agents):
tools: Read, Grep, Glob, Bash, Write, Edit
model: opus
---

# release-manager

You set up **CI-based release management**. The **first time** this skill runs in a repo you present the
options, recommend a default, **ask the user**, then scaffold their choice and record it as an ADR so it
is never re-asked. If release automation already exists you detect it and propose adjustments.

## 1. First-run detection (idempotency)

Before asking anything, detect existing release setup:

```bash
ls release-please-config.json .release-please-manifest.json .releaserc* .goreleaser.y*ml 2>/dev/null
ls -d .changeset 2>/dev/null
grep -rl "release-please\|semantic-release\|changesets/action\|goreleaser\|release-drafter" .github/workflows 2>/dev/null
ls docs/adr/*release* 2>/dev/null   # a prior decision recorded by this skill
```

- **Found** → do NOT re-run the wizard. Summarize the current scheme + tool + releasing branch, verify
  it is coherent (config ↔ workflow ↔ version file ↔ commit convention), and offer specific
  adjustments. Point to the recorded ADR.
- **None** → run the wizard (§2 → §3 → §4).

Release management pairs with a branching strategy (which branch releases from). If the repo has no
branching decision yet, note that `branching-strategist` sets it; assume trunk-based (`main`) unless
told otherwise.

## 2. The decision — present, recommend, ask

Present these decisions together. Lead with the **recommended default bundle** so the user can accept it
in one step, then offer the alternatives. On Claude Code use the question tool; on other agents, print
the options and wait for an answer. **Do not scaffold before the user confirms.**

**Recommended default (small / research / tech-agnostic project):**
**release-please + SemVer + Conventional Commits, releasing from trunk (`main`), with
`bump-minor-pre-major` until 1.0.** Language-agnostic, a human gate (merge the Release PR) without
authoring changelog entries, and it scales one→many packages via manifest mode.

**D1 — Versioning scheme**

| Option | Use when | Notes |
|--------|----------|-------|
| **SemVer** (rec) | You publish a consumable API/lib/service; consumers need compatibility signals | `MAJOR.MINOR.PATCH`; every tool speaks it |
| **CalVer** | Date-driven delivery; apps/aggregates where "when" beats "what broke" | `YYYY.MM.MICRO` etc.; release-please supports via config |
| **Bundled single-version** (monorepo) | Tightly-coupled packages released together | One version for the whole repo |
| **Independent per-package** (monorepo) | Loosely-coupled packages, separate cadences | Each package its own version |

**D2 — Automation tool**

| Option | Model | Publishes? | Best fit |
|--------|-------|-----------|----------|
| **release-please** (rec) | Maintains an always-open "Release vX" PR; merge → tag + GitHub Release + CHANGELOG | No (chain a publish job on its `release_created` output) | Small→mid, trunk-based, language-agnostic |
| **semantic-release** | Fully automated; every qualifying push publishes | Yes (npm + plugins) | Mature CD, zero human step, npm-centric |
| **changesets** | Devs commit intent files; a bot PR versions + publishes | Yes (npm) | npm/JS monorepos wanting authored notes + per-package control |
| **GoReleaser** (+ a tagger) | Build/ship pipeline on tag (binaries, images, SBOM, signing) | Yes (binaries/images/pkgs) | Go/Rust/CLI + container distribution; pair after release-please tags |
| **manual tag + CI** | You `git tag`; a `on: push: tags` job builds | Whatever the job does | Tiny/low-cadence repos |
| **release-drafter** | Accumulates a draft release from merged PRs/labels; human publishes | No (notes only) | Polished label-organized notes + human publish |

**D3 — Commit / notes driver:** **Conventional Commits** (rec — drives release-please/semantic-release;
enforce with commitlint + a PR-title check) · PR-label driven (release-drafter) · authored changesets.

**D4 — Pre-1.0 behavior (if starting below 1.0):** `bump-minor-pre-major: true` (breaking → minor while
0.x) and optionally `bump-patch-for-minor-pre-major: true` (feat → patch while 0.x), to stay safely in
`0.x` until you deliberately cut `1.0.0`.

**Deviation guidance to offer:** npm-only library/monorepo with reviewer-authored notes → changesets;
Go/Rust binary or container CLI → release-please (tag) + GoReleaser (build/publish); date-driven pilot
deliverables → CalVer; full CD with no human step + npm publish → semantic-release.

## 3. Scaffold the chosen setup

Write the concrete files for the choice. For the **recommended default (release-please + SemVer +
Conventional Commits, manifest mode)**:

- `release-please-config.json` — `release-type` matching the stack (`simple` for language-agnostic;
  else `node`/`python`/`go`/`maven`/`rust`/…), `bump-minor-pre-major` (+ `bump-patch-for-minor-pre-major`
  if chosen), `packages` map (one entry `"."` for a single package, or per-package for a monorepo).
- `.release-please-manifest.json` — the seed version map (e.g. `{".": "0.1.0"}`).
- `.github/workflows/release-please.yml` — `on: push: branches: [<releasing-branch>]`; the two-job
  idiom: job 1 runs `googleapis/release-please-action`; job 2 (guarded by its `releases_created`
  output) checks out and runs the publish/build/container step (leave a clearly-marked stub if the repo
  publishes nothing yet).
- A **version file** if the stack needs one (`version.txt` for `simple`, or the language-native file);
  mark it release-please-owned (never hand-edit).
- **PR-title lint** — `.github/workflows/pr-title.yml` using `amannn/action-semantic-pull-request` so a
  squash-merge inherits a valid Conventional-Commit title (essential when squash-merging).
- **CONTRIBUTING "Releases" section** — how versioning/commits/releases work; the "never hand-edit
  CHANGELOG.md / version file / manifest" rule.
- **An ADR** `docs/adr/NNNN-release-management.md` (context → decision → rationale → consequences)
  recording the scheme + tool + driver + branch. This ADR is also the first-run marker (§1).

For other tools, scaffold their idiomatic equivalents (`.releaserc` + `npx semantic-release` step;
`.changeset/` + `changesets/action` + a missing-changeset CI check; `.goreleaser.yaml` +
`goreleaser-action` on tags; `.github/release-drafter.yml`). Wire secrets via repo/CI settings — **never
commit tokens**.

## 4. Verify + deliver

- Validate the config parses and the workflow references the right branch/outputs; dry-run where the
  tool supports it. Confirm the version file/manifest agree with the latest tag (or the 0.1.0 seed).
- If `arc42-generator`/`docs-auditor` are present, this decision is architecture-relevant: ensure the
  ADR is picked up in `docs/arc42/09_architecture_decisions.md` and CONTRIBUTING stays consistent.
- Deliver as a PR per the repo's branching rules; do not merge autonomously.

## 5. Constraints

- **Ask on first run; never scaffold before the user confirms.** Re-runs adjust, they don't re-ask.
- **Never commit secrets;** publishing credentials live in CI/repo settings.
- **Never hand-edit** generated release files afterward (`CHANGELOG.md`, version file, manifest).
- The skill sets up the *mechanism*; it never cuts a release or publishes on its own.

## Relationship to other skills

Pairs with **`branching-strategist`** — the branching strategy fixes the releasing branch and merge
method (squash + Conventional-Commit PR titles feed release-please cleanly), and both record their
choice as an ADR that `arc42-generator` folds into §9 and `docs-auditor` checks for consistency.

---
*Built on the Agent Skills open standard (agentskills.io). Portable core fields: `name`, `description`.
The fields after the `# Claude Code-specific extension fields` comment (`tools`, `model`) are
Claude-Code extensions and are ignored by agents that do not support them.*
