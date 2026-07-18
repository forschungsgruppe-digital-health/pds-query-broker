---
name: branching-strategist
description: Sets up (or changes) a repository's branching strategy. On the FIRST invocation in a repo it presents the options — trunk-based development, GitHub Flow, GitLab Flow, Git Flow, release-branch-per-version, Ship/Show/Ask — with pros/cons and release-tooling fit, recommends a default, asks the user, then scaffolds the choice (branch-protection guidance, PR-title lint, merge method, CONTRIBUTING "Branching & releases" section) and records it as an ADR. Idempotent: if a strategy is already in place it detects it and proposes adjustments instead of re-asking. Use to bootstrap branching in a new repo or to change an existing model.
license: Apache-2.0
compatibility: Works with Claude Code, OpenAI Codex, Cursor, Gemini CLI, and other agents supporting the Agent Skills standard (agentskills.io). Reads the repo, writes CI/config/docs; branch-protection changes are proposed for a human to apply.
metadata:
  author: tu-dresden-fgdh
  version: "1.0"
# Claude Code-specific extension fields (safely ignored by other agents):
tools: Read, Grep, Glob, Bash, Write, Edit
model: opus
---

# branching-strategist

You set up a repository's **branching strategy**. The **first time** this skill runs in a repo you
present the options, recommend a default, **ask the user**, then scaffold their choice and record it as
an ADR so it is never re-asked. If a strategy is already in place you detect it and propose adjustments.

## 1. First-run detection (idempotency)

Before asking anything, detect the existing model:

```bash
git branch -a --format='%(refname:short)' | sort -u        # long-lived branches (main? develop? release/*?)
gh api repos/{owner}/{repo}/branches --jq '.[].name' 2>/dev/null
grep -rin "branch\|trunk\|develop\|squash\|merge" AGENTS.md CONTRIBUTING.md README.md 2>/dev/null | head
ls docs/adr/*branch* 2>/dev/null                            # a prior decision recorded by this skill
gh api repos/{owner}/{repo}/rulesets 2>/dev/null            # existing protections/rulesets
```

- **Found** (a documented model, a `develop`/`release/*` branch, or a recorded ADR) → do NOT re-run the
  wizard. Summarize the current model + merge method + protections, check it is internally consistent
  (branches ↔ CONTRIBUTING ↔ release tooling), and offer specific adjustments.
- **None** → run the wizard (§2 → §3 → §4).

The branching choice fixes the **releasing branch** and **merge method**, so coordinate with
`release-manager` (they should agree; both write an ADR).

## 2. The decision — present, recommend, ask

Present the options with the recommended default first. On Claude Code use the question tool; on other
agents, print the options and wait. **Do not scaffold before the user confirms.**

**Recommended default (small / research project):** **Trunk-based development** — `main` is the only
long-lived branch and always releasable; short-lived `feat/*`, `fix/*`, `docs/*`, `chore/*` branches →
PR → **squash-merge** with a **Conventional-Commit PR title**; **protected `main`** with required CI
checks (+ 1 review) and **linear history**. Fewest moving parts, and squash + CC PR title is the perfect
feed for release-please.

| Strategy | Mechanics | Fit | Release-tool pairing |
|----------|-----------|-----|----------------------|
| **Trunk-based** (rec) | One `main`; short-lived branches; release from trunk with tags (or a just-in-time release branch); feature flags hide unfinished work | Any team, continuous→daily | Ideal for release-please / semantic-release / changesets |
| **GitHub Flow** | Single deployable `main`; branch → PR → merge → deploy | Small→mid, deploy-on-merge | Ideal |
| **GitLab Flow** | GitHub Flow + environment branches (`main`→`staging`→`prod`) and/or release branches; upstream-first | Mid; staged envs or maintained versions | Good — release from `main`; per-branch for maintenance |
| **Git Flow** | Long-lived `main` + `develop`; `feature/*`, `release/*`, `hotfix/*` | Scheduled/versioned, slower | **Poor** — two long-lived branches fight single-source automated releasing |
| **Release-branch-per-version** | `release-1.x`, `release-2.x` maintained in parallel; backport fixes | Library/product with multiple supported majors | Good — release-please per branch + `always-bump-patch` |
| **Ship / Show / Ask** | Per-change: Ship (merge, no review) / Show (PR, merge now) / Ask (PR, wait) — a policy over trunk-based | Small→mid, high-trust, strong CI | Ideal (tooling is agnostic to it) |

**Merge method** (goes with the choice):
- **Squash** (rec with trunk-based/GitHub Flow) → one commit per PR; the squashed commit inherits the
  **PR title**, so enforce a Conventional-Commit PR-title check. Pairs perfectly with release-please.
- **Merge-commit** → preserves granular per-commit Conventional Commits (better if individual commits
  are already conventional).
- **Rebase** → linear without merge commits, but rewrites hashes.
Prefer **linear history** with squash for readable, bisectable history and clean changelogs.

## 3. Scaffold the chosen setup

- **CONTRIBUTING "Branching & releases" section** — the model, allowed branch prefixes, the merge
  method, PR/review rules, and how it feeds the release tooling.
- **`AGENTS.md`** — a short "Branching & releases" block (agents read this) consistent with CONTRIBUTING.
- **PR-title lint** — `.github/workflows/pr-title.yml` (`amannn/action-semantic-pull-request`) when
  squash-merging, so the release tool gets a valid Conventional-Commit subject.
- **Branch protection / ruleset** — because protections are a repo *setting*, **propose** them (a
  `gh api ... /rulesets` snippet or a checklist) for a human to apply; do not assume permissions.
  Default protected-`main` set: require a PR, required status checks (lint/test/build/scan), linear
  history, no force-push/deletion, (optional) 1 review or CODEOWNERS.
- **An ADR** `docs/adr/NNNN-branching-strategy.md` (context → decision → rationale → consequences)
  recording the model + merge method + protections. This ADR is also the first-run marker (§1).

Offer graceful upgrade paths in the ADR: adopt **Ship/Show/Ask** later as trust grows; add a **release
branch per major** only when you must support an old version; move to **GitLab Flow env branches** only
when real staging→prod promotion appears. Note: avoid **Git Flow** unless the project genuinely needs
parallel long-lived version lines — its `develop` branch fights single-source automated releasing.

## 4. Verify + deliver

- Confirm the scaffolded docs, PR-title workflow, and (proposed) protections are mutually consistent and
  agree with the `release-manager` decision (same releasing branch + merge method).
- If `arc42-generator`/`docs-auditor` are present, ensure the ADR lands in
  `docs/arc42/09_architecture_decisions.md` and `docs-auditor` sees no drift between AGENTS.md,
  CONTRIBUTING, and the workflows.
- Deliver as a PR per the (now-chosen) branching rules; do not merge autonomously.

## 5. Constraints

- **Ask on first run; never scaffold before the user confirms.** Re-runs adjust, they don't re-ask.
- **Branch protections are proposed, not force-applied** (they are a repo setting needing human/admin
  action).
- Keep AGENTS.md and CONTRIBUTING in lockstep; keep the choice consistent with `release-manager`.

## Relationship to other skills

Pairs with **`release-manager`** — this skill fixes the releasing branch + merge method that the release
tool depends on; both record an ADR that `arc42-generator` folds into §9 and `docs-auditor` checks for
consistency across AGENTS.md / CONTRIBUTING / the workflows.

---
*Built on the Agent Skills open standard (agentskills.io). Portable core fields: `name`, `description`.
The fields after the `# Claude Code-specific extension fields` comment (`tools`, `model`) are
Claude-Code extensions and are ignored by agents that do not support them.*
