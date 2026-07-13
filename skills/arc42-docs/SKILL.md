---
name: arc42-docs
description: Maintain the arc42 (v9.0) architecture documentation in docs/ARCHITECTURE.md — structure, diagrams, ADRs, quality scenarios. Use for any change to the architecture documentation or when a code/spec change must be reflected in it.
---

# Skill: arc42 architecture documentation

You are a specialist for arc42-conformant architecture documentation (v9.0).

## Working set

- Main document: `docs/ARCHITECTURE.md`
- Supporting documents: `README.md`, `CONTRIBUTING.md`, `PDS_INTEGRATION.md`
- Changelog: `CHANGELOG.md` (managed by release-please — do not edit by hand)

## arc42 v9.0 structure

The document has 12 sections:

1. Introduction and goals (quality goals, stakeholders)
2. Constraints (technical, organizational, conventions)
3. Context (business + technical, each with diagram + table)
4. Solution strategy (rationale table)
5. Building block view (level 1 whole system, level 2 whiteboxes, each with a component description table)
6. Runtime view (sequence diagrams)
7. Deployment view (deployment diagram)
8. Cross-cutting concepts (mindmap + subsections 8.1–8.8)
9. Architecture decisions (ADRs: context, decision, rationale)
10. Quality requirements (10.1 overview with Q42 tags, 10.2 details as scenarios)
11. Risks and technical debt
12. Glossary

## Rules

1. **Diagrams**: Mermaid, no colors/styles, UML notation where possible
2. **Component diagrams**: always with an accompanying description table (component, role, interfaces, technology)
3. **Code blocks**: only for real code. Structured content as tables/lists/Mermaid.
4. **ADRs**: numbered (ADR-NNN), format: context → decision → rationale
5. **Quality scenarios**: v9.0 format: stimulus → response → metric/acceptance criterion
6. **Section 10**: 10.1 = overview (categories with Q42 tags such as #interoperable, #flexible, #traceable), 10.2 = details (measurable scenarios)
7. **Profile binding**: always describe as optional and project-specific
8. **PDS, not DIZ**: use the neutral abbreviation
9. **Cross-references**: link between sections and to other documents
10. **Versioning**: update version and date in the header; release notes flow through Conventional Commits (release-please)

## Typical tasks

- "Document [decision] as an ADR" → new ADR-NNN in section 9
- "Update section [N] after [change]" → edit the section, keep diagrams + tables consistent
- "Add a quality scenario for [quality]" → 10.1 overview entry + 10.2 measurable scenario
