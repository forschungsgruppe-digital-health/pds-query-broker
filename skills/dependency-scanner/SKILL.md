---
name: dependency-scanner
description: Sets up (or changes) CI dependency, CVE, and supply-chain scanning for a repository. On the FIRST invocation it detects the stack (Gradle/npm/pip/Go/Cargo/Docker/Actions) and the repo visibility, presents the options — dependency updates (Dependabot vs Renovate), the dependency graph, container/dependency CVE scanning (Trivy/Grype/OSV-Scanner), supply-chain hygiene (OpenSSF Scorecard, SHA-pinned actions), and SBOM/provenance — recommends a default, asks the user, then scaffolds the chosen config (dependabot.yml + workflows) and records an ADR. Idempotent: if scanning is already configured it detects it and offers adjustments instead of re-asking. Public vs private aware (SARIF upload / Scorecard publish need GitHub Advanced Security on private repos). Use to bootstrap dependency/CVE monitoring or to change it.
license: Apache-2.0
compatibility: Works with Claude Code, OpenAI Codex, Cursor, Gemini CLI, and other agents supporting the Agent Skills standard (agentskills.io). Reads the repo, writes CI/config/docs; never publishes on its own.
metadata:
  author: tu-dresden-fgdh
  version: "1.0"
# Claude Code-specific extension fields (safely ignored by other agents):
tools: Read, Grep, Glob, Bash, Write, Edit
model: opus
---

# dependency-scanner

You set up **CI dependency / CVE / supply-chain scanning**. The **first time** this skill runs in a
repo you detect the stack, present the options, recommend a default, **ask the user**, then scaffold
their choice and record it as an ADR so it is never re-asked. If scanning already exists you detect it
and propose adjustments. (Secret scanning, SAST, and AI code review are the sibling `security-scanner`
skill's job — this skill is Software Composition Analysis + supply chain.)

## 1. First-run detection (idempotency)

```bash
ls .github/dependabot.yml renovate.json .github/renovate.json 2>/dev/null
grep -rl "trivy\|grype\|osv-scanner\|dependency-submission\|scorecard\|sbom\|syft" .github/workflows 2>/dev/null
ls docs/adr/*dependency* docs/adr/*supply-chain* 2>/dev/null   # a prior decision from this skill
```

- **Found** → do NOT re-run the wizard. Summarize the current setup, verify it is coherent, and offer
  specific adjustments. Point to the recorded ADR.
- **None** → detect the stack (§2) and run the wizard (§3 → §4).

## 2. Detect stack + visibility

- **Ecosystems** (drive the Dependabot blocks + which CVE scanners help): Gradle/Maven
  (`build.gradle*`, `pom.xml`), npm (`package.json` + lockfile), pip/poetry
  (`requirements*.txt`/`pyproject.toml`), Go (`go.mod`), Cargo (`Cargo.toml`), Docker (`Dockerfile*`),
  GitHub Actions (`.github/workflows/`).
- **Lockfiles** decide dependency-CVE depth: a lockfile (`package-lock.json`, `go.sum`, …) gives Trivy
  real dep-CVE coverage; **Gradle has no resolved graph without the dependency-submission action**.
- **Visibility** (`gh repo view --json isPrivate`): on a **private** repo, code scanning / SARIF upload
  and `Scorecard publish_results` require **GitHub Advanced Security** — if GHAS is off, scanners must
  gate via exit code and print to the job log instead of uploading SARIF; Dependabot + Trivy-gating
  still work free.

## 3. The decision — present, recommend, ask

Lead with the recommended default so the user can accept in one step; then the alternatives. On Claude
Code use the question tool; else print options and wait. **Do not scaffold before the user confirms.**

**Recommended default (public repo):** **Dependabot** (all detected ecosystems, grouped, weekly) +
**dependency graph** (Gradle → `gradle/actions/dependency-submission`) + **Trivy** (container/IaC +
image CVEs, first-party gated / upstream-base informational) + **OpenSSF Scorecard** + **SHA-pinned
actions** + **SBOM/provenance on release**. On a **private repo without GHAS**: same, minus Scorecard
publish and SARIF upload (Trivy gates via exit code and prints to the log).

| Decision | Options (rec first) |
|----------|---------------------|
| **Dependency updates** | **Dependabot** (native, best GitHub-alert integration) · **Renovate** (better for multi-module / monorepo grouping + auto-merge) |
| **Dependency graph** | native for most ecosystems; **Gradle needs `gradle/actions/dependency-submission`** to get the transitive graph → transitive-CVE alerts + SBOM |
| **Container / dependency CVEs** | **Trivy** (image + fs + IaC misconfig) · **Grype** (EPSS/KEV-prioritized gating) · **OSV-Scanner** (lockfile/SBOM, PR-diff mode) |
| **Supply-chain hygiene** | **OpenSSF Scorecard** (public) + **pin actions to commit SHAs** (Dependabot keeps them fresh) |
| **SBOM / provenance** | **Syft** (CycloneDX+SPDX) + **`actions/attest-build-provenance`** on the release/publish job |

## 4. Scaffold the chosen setup

Write only the pieces the user chose, adapted to the detected ecosystems + visibility.

- **`.github/dependabot.yml`** — one `updates:` block per detected ecosystem (for multi-module Gradle,
  one entry per module directory), grouped (`minor-and-patch`, framework groups) + weekly.
- **Dependency graph** — for Gradle, `.github/workflows/dependency-graph.yml` running
  `gradle/actions/dependency-submission` on push to the default branch (+ note: enable "Automatic
  dependency submission" in Settings).
- **`.github/workflows/trivy.yml`** — config/misconfig scan + (schedule/default-branch) image scan.
  **Public/GHAS:** gate first-party fixable HIGH/CRITICAL (`ignore-unfixed: true`) and upload SARIF for
  the rest (informational). **Private/no-GHAS:** `format: table`, gate via `exit-code`, no SARIF upload.
- **`.github/workflows/scorecard.yml`** — only when public (or GHAS present); informational.
- **SBOM/provenance** — add Syft + `attest-build-provenance` to the release/publish job (coordinate with
  `release-manager`).
- **Pin new actions to commit SHAs** (with a `# vX` comment); enable the `github-actions` Dependabot
  ecosystem so they stay current.
- **An ADR** `docs/adr/NNNN-dependency-scanning.md` (context → decision → rationale → consequences)
  recording the chosen tools + gate posture. This ADR is also the first-run marker (§1).

## 5. Verify + deliver

- Validate each YAML parses and references the right ecosystems/branch. Note the one-time repo settings
  the user must toggle in the UI (Dependency graph + Dependabot alerts/security updates; on public,
  secret scanning + push protection).
- Deliver as a PR per the repo's branching rules; do not merge autonomously.

## 6. Constraints

- **Ask on first run; never scaffold before the user confirms.** Re-runs adjust, they don't re-ask.
- **Never commit secrets;** any tokens live in CI/repo settings.
- **Never hand-edit** generated files afterward (`CHANGELOG.md`, version file, manifests).
- On a private repo, do NOT scaffold SARIF-upload / Scorecard-publish steps that require GHAS — gate via
  exit code and print instead, and say so in the ADR.

## Relationship to other skills

Pairs with **`security-scanner`** (secret scanning + SAST + AI code review) — together they cover the
CI security surface. Coordinates with **`release-manager`** (SBOM/provenance land on the release job)
and **`branching-strategist`** (which fixes the default/releasing branch). Records an ADR that
`arc42-generator` folds into section 9.

---
*Built on the Agent Skills open standard (agentskills.io). Portable core fields: `name`, `description`.
The fields after the `# Claude Code-specific extension fields` comment (`tools`, `model`) are
Claude-Code extensions and are ignored by agents that do not support them.*
