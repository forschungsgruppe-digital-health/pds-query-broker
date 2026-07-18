---
name: security-scanner
description: Sets up (or changes) CI security review for a repository — secret scanning, SAST, and AI-assisted PR review — and installs the read-only security-reviewer skill. On the FIRST invocation it detects the stack + visibility, presents the options (secret scanning via gitleaks / GitHub push protection; SAST via CodeQL or Semgrep; AI PR review via anthropics/claude-code-security-review), recommends a default, asks the user, then scaffolds the chosen workflows + a security-review instructions file + the security-reviewer skill, and records an ADR. Idempotent: if security scanning is already configured it detects it and offers adjustments instead of re-asking. Public vs private aware (CodeQL / SARIF upload need GitHub Advanced Security on private repos). Use to bootstrap security review or to change it.
license: Apache-2.0
compatibility: Works with Claude Code, OpenAI Codex, Cursor, Gemini CLI, and other agents supporting the Agent Skills standard (agentskills.io). Reads the repo, writes CI/config/docs/skills; never runs live exploitation.
metadata:
  author: tu-dresden-fgdh
  version: "1.0"
# Claude Code-specific extension fields (safely ignored by other agents):
tools: Read, Grep, Glob, Bash, Write, Edit
model: opus
---

# security-scanner

You set up **CI security review** — secret scanning, SAST, and AI-assisted PR review — and install the
read-only **`security-reviewer`** skill for deeper, report-persisting reviews. The **first time** this
skill runs in a repo you detect the stack + visibility, present the options, recommend a default, **ask
the user**, then scaffold the choice and record an ADR so it is never re-asked. If security scanning
already exists you detect it and propose adjustments. (Dependency/CVE/supply-chain scanning is the
sibling `dependency-scanner` skill's job.)

## 1. First-run detection (idempotency)

```bash
ls .gitleaks.toml .github/workflows/gitleaks.yml .github/workflows/codeql.yml \
   .github/workflows/semgrep.yml .github/workflows/security-review.yml 2>/dev/null
ls skills/security-reviewer/SKILL.md docs/adr/*security* 2>/dev/null
```

- **Found** → do NOT re-run the wizard. Summarize the current setup, verify it is coherent, and offer
  specific adjustments. Point to the recorded ADR.
- **None** → detect the stack + visibility (as in `dependency-scanner` §2) and run the wizard (§2 → §3).

## 2. The decision — present, recommend, ask

Lead with the recommended default; then the alternatives. On Claude Code use the question tool; else
print options and wait. **Do not scaffold before the user confirms.**

**Recommended default (public repo):** **gitleaks** (secret scanning, CI + opt-in pre-commit) +
**GitHub push protection** + **CodeQL** (SAST for the detected languages) + **claude-code-security-review**
(AI PR review) + install the **`security-reviewer`** skill. On a **private repo without GHAS**: swap
CodeQL for **Semgrep** (SARIF upload needs GHAS; Semgrep gates via exit code / prints), keep the rest.

| Decision | Options (rec first) |
|----------|---------------------|
| **Secret scanning** | **gitleaks** (custom rules, full history, pre-commit + CI) **+ GitHub push protection** (public/GHAS) · TruffleHog (verification-first, fewer FPs) |
| **SAST** | **CodeQL** (public / GHAS; `build-mode: none` where possible) · **Semgrep** (private-friendly — gates/prints without GHAS) · none |
| **AI PR review** | **`anthropics/claude-code-security-review`** (official; auth/data-exposure/injection review with FP filtering). Needs the `ANTHROPIC_API_KEY` secret; run on **same-repo PRs only** (not hardened against prompt injection) |
| **Deep review** | install the read-only **`security-reviewer`** skill (STRIDE + checklist + scanner triage → dated `docs/reports/` report; findings are human-confirmed leads) |

## 3. Scaffold the chosen setup

Write only the chosen pieces, adapted to the detected languages + visibility.

- **Secret scanning** — `.gitleaks.toml` (`useDefault = true` + scoped allowlists for documented test
  values), `.github/workflows/gitleaks.yml` (pinned gitleaks), and an opt-in `.githooks/pre-commit`
  (activate with `git config core.hooksPath .githooks`). **First verify the history is clean**
  (`gitleaks git`) so the new gate is not born red.
- **SAST** — `.github/workflows/codeql.yml` (public/GHAS; matrix over the detected languages,
  `build-mode: none`, `security-extended`) OR `.github/workflows/semgrep.yml` (private/no-GHAS;
  `semgrep scan --config auto`, gate on ERROR or print).
- **AI PR review** — `.github/workflows/security-review.yml` using
  `anthropics/claude-code-security-review` with: a `same-repo PRs only` guard
  (`github.event.pull_request.head.repo.full_name == github.repository`), an **API-key guard** that
  WARNS + skips when `ANTHROPIC_API_KEY` is unset (so it is safe to merge before the secret exists),
  and `custom-security-scan-instructions: .github/security-review-instructions.md`.
- **`.github/security-review-instructions.md`** — a project-tailored focus list (auth/authorization,
  sensitive-data handling, injection, crypto, the domain's high-value classes) appended to the audit
  prompt. Seed it from `AGENTS.md`.
- **`skills/security-reviewer/`** — copy in the read-only security-reviewer skill + its `.claude`/`.codex`
  symlinks + an `AGENTS.md` catalog entry.
- **An ADR** `docs/adr/NNNN-security-scanning.md` (context → decision → rationale → consequences)
  recording the chosen tools + visibility handling. This ADR is also the first-run marker (§1).

Pin new actions to commit SHAs. **Never commit the API key** — it is a repo secret; the workflow only
references `${{ secrets.ANTHROPIC_API_KEY }}`.

## 4. Verify + deliver

- Validate each YAML parses; confirm the AI-review guards (same-repo + API-key) are present; confirm
  gitleaks history is clean. Note the one-time repo settings (push protection; the `ANTHROPIC_API_KEY`
  secret; on private, that CodeQL/SARIF need GHAS).
- Deliver as a PR per the repo's branching rules; do not merge autonomously.

## 5. Constraints

- **Ask on first run; never scaffold before the user confirms.** Re-runs adjust, they don't re-ask.
- **Never commit secrets / API keys** — they are repo/CI settings.
- **AI review runs on same-repo PRs only** (prompt-injection + key-exfiltration safety) and its findings
  are **leads a human confirms**, never verdicts.
- On a private repo without GHAS, do NOT scaffold CodeQL/SARIF-upload steps that will fail — use Semgrep
  (gate/print) instead, and say so in the ADR.

## Relationship to other skills

Pairs with **`dependency-scanner`** (SCA + supply chain) to cover the CI security surface, and
**installs `security-reviewer`** — the read-only deep-review skill that this workflow's PR-time review
complements. Records an ADR that `arc42-generator` folds into section 9.

---
*Built on the Agent Skills open standard (agentskills.io). Portable core fields: `name`, `description`.
The fields after the `# Claude Code-specific extension fields` comment (`tools`, `model`) are
Claude-Code extensions and are ignored by agents that do not support them.*
