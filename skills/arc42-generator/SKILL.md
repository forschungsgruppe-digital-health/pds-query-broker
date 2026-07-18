---
name: arc42-generator
description: Generate AND maintain the arc42 (v9.0) architecture documentation as a SPLIT directory docs/arc42/ — one file per section (01_introduction_and_goals … 12_glossary, matching the arc42 golden-master basenames) plus a README index and about-arc42 — written to be understood by a novice AND an expert. Two modes, auto-detected: BOOTSTRAP a first-draft skeleton from code/config (filling only what is unambiguously derivable, marking everything else ⚠️ HUMAN INPUT — no speculation); or CONSOLIDATE scattered/legacy architecture docs (including a single-file docs/ARCHITECTURE.md) into one canonical, de-duplicated, navigable arc42 set. Runs docs-auditor as the quality gate before and after. Use for any architecture-documentation change, or to create/migrate docs/arc42/.
license: Apache-2.0
compatibility: Works with Claude Code, OpenAI Codex, Cursor, Gemini CLI, and other agents supporting the Agent Skills standard (agentskills.io). Reads code/config/specs; writes only the architecture documentation and its inbound links.
metadata:
  author: tu-dresden-fgdh
  version: "2.0"
# Claude Code-specific extension fields (safely ignored by other agents):
tools: Read, Grep, Glob, Bash, Write, Edit, Task
model: opus
---

# arc42-generator

You generate and maintain the architecture documentation following the official **arc42 v9.0-EN
(July 2025)** 12-section structure. The docs are **canonical and split** — one file per section under
`docs/arc42/` — and must be understood by a **novice and an expert** alike. You reuse **`docs-auditor`**
as the quality gate that decides when the docs are done.

> **No speculation.** In BOOTSTRAP mode, fill in ONLY what is unambiguously derivable from code,
> configuration, and build files. When something needs guessing, insert the placeholder below instead —
> never invent goals, qualities, or rationale.
>
> ```
> > ⚠️ HUMAN INPUT REQUIRED — not derivable from code without speculation.
> ```
> …followed by a brief note on what kind of input is needed.

## 1. Two modes (auto-detected)

Inspect the repo first, then pick the mode:

- **BOOTSTRAP** — little or no architecture documentation exists. Derive a first-draft skeleton from
  code/config; fill only the derivable sections; mark the rest ⚠️ HUMAN INPUT. Output is a *draft to be
  verified*, never authoritative.
- **CONSOLIDATE** — architecture material already exists but is scattered (a single-file
  `docs/ARCHITECTURE.md`, plus bits in README/CONTRIBUTING/plan docs, inline ADRs, IG/spec pages).
  Consolidate it into one canonical, de-duplicated arc42 set and migrate the legacy single file.

Both modes produce the **same target layout** (§2), meet the **same novice+expert bar**, and use
**`docs-auditor`** as the gate (§4).

## 2. Target layout — `docs/arc42/` (one file per section)

`docs/arc42/` is the single source of truth for the architecture documentation. File basenames match
the [arc42 golden master](https://github.com/arc42/arc42-template) so anyone who knows arc42 recognizes
the layout at a glance:

| File | Section |
|------|---------|
| `docs/arc42/README.md` | Index / table of contents (navigation hub; the single version+date source) |
| `docs/arc42/about-arc42.md` | What arc42 is and how to read these docs (newcomer orientation) |
| `docs/arc42/01_introduction_and_goals.md` | 1. Introduction and Goals |
| `docs/arc42/02_architecture_constraints.md` | 2. Architecture Constraints |
| `docs/arc42/03_context_and_scope.md` | 3. Context and Scope |
| `docs/arc42/04_solution_strategy.md` | 4. Solution Strategy |
| `docs/arc42/05_building_block_view.md` | 5. Building Block View |
| `docs/arc42/06_runtime_view.md` | 6. Runtime View |
| `docs/arc42/07_deployment_view.md` | 7. Deployment View |
| `docs/arc42/08_concepts.md` | 8. Cross-cutting Concepts |
| `docs/arc42/09_architecture_decisions.md` | 9. Architecture Decisions (ADRs) |
| `docs/arc42/10_quality_requirements.md` | 10. Quality Requirements |
| `docs/arc42/11_technical_risks.md` | 11. Risks and Technical Debt |
| `docs/arc42/12_glossary.md` | 12. Glossary |

**Canonical, not duplicated:** `docs/arc42/` **supersedes** any legacy single-file
`docs/ARCHITECTURE.md`, which becomes a thin redirect stub (§4 step 6) — never a second, diverging copy.
Inline ADRs move into `docs/arc42/09_architecture_decisions.md`, which becomes the home for new ADRs.

## 3. The 12 sections — what each holds and how to derive it

For each section: in CONSOLIDATE mode, map existing content in; in BOOTSTRAP mode, derive what is listed
as *derivable* and mark the rest ⚠️ HUMAN INPUT.

1. **Introduction and Goals** — requirements overview, top quality goals, stakeholders. *Derivable:*
   only stakeholder hints from CODEOWNERS/CONTRIBUTORS ("detected, validate"). Goals/qualities are
   ⚠️ HUMAN INPUT.
2. **Architecture Constraints** — technical / organizational / convention constraints. *Derivable:* hard
   technical constraints visible in build files (language/runtime versions, pinned tool versions).
   Organizational constraints are ⚠️ HUMAN INPUT.
3. **Context and Scope** — business + technical context, each with a diagram and a partner/interface
   table. *Derivable:* technical context from detected external integrations (endpoints, issuers,
   gateways, DBs) → a Mermaid context diagram of detected systems. Business context is ⚠️ HUMAN INPUT.
4. **Solution Strategy** — the key decisions as a decision→rationale table (links into §9 ADRs). The
   *why* lives in ADRs; reference them. Otherwise ⚠️ HUMAN INPUT.
5. **Building Block View** — level 1 (whole system) + level 2 whiteboxes, each with a component table
   (component, responsibility, interfaces, technology). *Strongest derivable section:* top-level
   modules/apps/services from the build and directory structure → Mermaid diagrams. State which
   repo/module each block lives in.
6. **Runtime View** — the important scenarios as sequence diagrams. *Derivable:* only clearly traceable
   flows (a login sequence, a request path). Do NOT invent sequences; mark untraceable ones incomplete.
7. **Deployment View** — deployment diagram. *Derivable:* from compose/Dockerfiles/k8s manifests —
   services, ports, networks, volumes, dependencies → Mermaid diagram.
8. **Cross-cutting Concepts** — a concepts mindmap + subsections 8.x. *Derivable:* only evidenced
   concepts (auth mechanism, persistence, logging/monitoring, i18n, error model). Rationale is
   ⚠️ HUMAN INPUT.
9. **Architecture Decisions** — the ADRs (context → decision → rationale → **consequences**). *In
   CONSOLIDATE:* move existing ADRs here verbatim, preserving their numbers. *In BOOTSTRAP:* link
   `docs/adr/**` if present; do NOT reconstruct rationale from code.
10. **Quality Requirements** — 10.1 overview with [Q42](https://quality.arc42.org/) tags, 10.2
    measurable quality scenarios (stimulus → response → metric). Quality scenarios are domain
    decisions → ⚠️ HUMAN INPUT unless already documented.
11. **Risks and Technical Debt** — risk → impact → mitigation. *Derivable:* technically-evident debt
    (EOL deps, missing tests, TODO/FIXME density, dead code) "detected, severity needs human review".
12. **Glossary** — domain and technical terms (the novice's dictionary; every section links here on
    first use). *Derivable:* extract terms from code/config/`AGENTS.md`; mark "definitions need human
    confirmation" in BOOTSTRAP.

## 4. Workflow

Run in order. Steps 1 and the final step **use `docs-auditor`** — the docs are not done until the final
audit is clean.

1. **Audit first (use `docs-auditor`).** Run `docs-auditor` and read its report: current
   inconsistencies, stale claims, gaps, orphan/broken links. In CONSOLIDATE mode you **resolve** these
   during the work — never split content forward while propagating a known contradiction.
2. **Map content → sections (or derive it).** Assign every piece of architecture material to its arc42
   section. In CONSOLIDATE: split `docs/ARCHITECTURE.md` into the 12 files; pull relevant material from
   other sources by summary + cross-link (dedupe — one canonical statement, linked, not two). In
   BOOTSTRAP: fill derivable sections, mark the rest ⚠️ HUMAN INPUT.
3. **Write each section for novice AND expert.** Every section file opens with a one-to-three-sentence
   plain-language lede, then the expert detail:

   ~~~markdown
   # 6. Runtime View

   [Back to the architecture docs index](README.md)

   > In brief (for newcomers): how a request actually flows through the system at run time, shown as
   > step-by-step sequence diagrams. Terms are defined in the glossary (12_glossary.md).

   ## 6.1 Scenario: <name>
   …expert detail: sequence diagram, exact components, interfaces, values…
   ~~~

   Define or link every domain term on first use (→ `12_glossary.md`). Keep expert content precise
   (exact names, versions, values). Use the repo's canonical terms and conventions from `AGENTS.md`.
4. **Write the index + about page.** `docs/arc42/README.md` = a table of contents linking all 12
   sections + `about-arc42.md`, with a one-line "what's inside" per section and the **single**
   version+date header (every section refers back to it; no per-file version drift). `about-arc42.md`
   explains what arc42 is and how to read the set — the newcomer's entry point.
5. **Breadcrumbs.** Every section file starts with a breadcrumb link back to the index (`README.md`) so
   no section is a navigational dead end.
6. **Migrate + fix inbound links (CONSOLIDATE mode).** Handle the legacy single file and every link
   into it so nothing dangles:
   - Replace `docs/ARCHITECTURE.md` with a **redirect stub**: a short "moved to `docs/arc42/`" note
     that also **retains the old section/ADR anchors as redirect anchors** — one small heading per old
     `#…` slug that deep links used (e.g. `## ADR-012 …` and `## 9. Architecture Decisions`), each
     pointing to its new home (`arc42/09_architecture_decisions.md#adr-012-…`). This keeps **immutable**
     inbound links (frozen `docs/reports/**` snapshots that must NOT be edited) resolving without
     touching them.
   - Re-point every **mutable** inbound reference directly at the new files/slugs — including deep
     anchors. Enumerate at least: `README`, `CONTRIBUTING`, `PDS_INTEGRATION`/module guides,
     `docs/IMPLEMENTATION_PLAN` (and any plan/roadmap), `AGENTS.md` (repo map + any "ADRs in
     `docs/ARCHITECTURE.md §9`" invariant → `docs/arc42/09_…`), `CLAUDE.md`, sibling skills that name
     the ADR location, and cross-doc ADR references. Example remap:
     `docs/ARCHITECTURE.md#adr-012-…` → `docs/arc42/09_architecture_decisions.md#adr-012-…`.
   - Do NOT edit `docs/reports/**` (immutable) or generated files (`CHANGELOG.md`, `version.txt`,
     release manifests); the stub's redirect anchors cover the frozen links instead.
7. **Audit again (use `docs-auditor`) — the acceptance gate.** Re-run `docs-auditor` scoped to
   `docs/arc42/` and its inbound links. The work is **done only when it reports**: complete (all 12
   sections present), consistent (no drift, no diverged `ARCHITECTURE.md` copy), error-free (every link
   AND anchor resolves — the stub's redirect anchors included; domain facts match their source),
   understandable (novice lede + expert detail in every section), and usable (index + breadcrumbs +
   cross-links; no orphans). Iterate until clean.

## 5. Rules

1. **No speculation** (BOOTSTRAP) — derivable content only; everything else is the ⚠️ HUMAN INPUT
   placeholder. Mark DERIVED content as a draft requiring verification.
2. **Novice + expert in every section** — plain-language lede, then precise detail; every term linked
   to the glossary on first use (the shared bar `docs-auditor` enforces).
3. **One file per section**, golden-master basenames (§2). `docs/arc42/` is canonical; no diverging
   duplicate in `docs/ARCHITECTURE.md`.
4. **Diagrams:** Mermaid only, **no colors/styles**, UML notation where possible.
5. **Component diagrams:** always paired with a description table (component, role, interfaces,
   technology).
6. **Code blocks:** only for real code/shell/pseudocode. Structured content as tables/lists/Mermaid.
7. **ADRs:** numbered `ADR-NNN`, format **context → decision → rationale → consequences**; sequential
   numbering continues in `09_architecture_decisions.md`.
8. **Quality scenarios:** v9.0 format **stimulus → response → metric/acceptance criterion**; §10.1 =
   overview with Q42 tags, §10.2 = measurable scenarios.
9. **Domain-specific bindings are optional** — describe any project-specific convention as
   configurable, not architecturally inherent; follow the canonical terms/conventions in `AGENTS.md`.
10. **Cross-references** — between sections and to `CONTRIBUTING`/module guides; every section links
    back to the index.
11. **Facts must match source** — specs, schemas, generated contracts, build/compose. When they change,
    this skill reflects them.
12. **Single version+date source** in `docs/arc42/README.md`; release notes flow through the repo's
    release automation — never hand-edit `CHANGELOG.md`.

## 6. Output

- **The arc42 set** under `docs/arc42/` (the run's primary artifact). In BOOTSTRAP, add a provenance
  note atop the index: "Generated by arc42-generator on <date>. Code-derived sections are drafts;
  ⚠️ sections require human input. Verify all DERIVED content against the actual code before relying
  on it."
- **A run report** persisted to `docs/reports/arc42-generator-<YYYY-MM-DD>.md` per the
  [reports convention](../../docs/reports/README.md): what was derived vs. marked for human input, the
  before/after `docs-auditor` verdicts, and any inbound links remapped.

## 7. Constraints

- Changes ship as a PR (follow the repo's branching rules in `AGENTS.md`).
- Do not hand-edit generated files (`CHANGELOG.md`, `version.txt`, release manifests) or immutable
  `docs/reports/**`.
- Keep domain authority where it belongs (see `AGENTS.md`'s skills catalog): this skill keeps the arc42
  documentation correct, consolidated, and readable, and reuses `docs-auditor` to prove it.
- Do not modify source code. Writing is limited to the architecture docs and their inbound links.

## Relationship to other skills

Uses **`docs-auditor`** as the before/after quality gate (§4 steps 1 and 7) — the invocation edge is
one-directional (this skill calls `docs-auditor`, never the reverse). Complements the repo's
architecture/development specialists (which evolve the system — this skill documents it) and any
domain-IG skill. The arc42 set is where all architecture-doc impact lands.

---
*Built on the Agent Skills open standard (agentskills.io). Portable core fields: `name`, `description`.
The fields after the `# Claude Code-specific extension fields` comment (`tools`, `model`) are
Claude-Code extensions and are ignored by agents that do not support them.*
