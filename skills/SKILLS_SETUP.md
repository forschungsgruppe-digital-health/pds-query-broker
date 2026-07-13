# Skills setup

`skills/` is the **single source of truth** for agent capabilities in this repo, using the
vendor-neutral [Agent Skills](https://agentskills.io) format (`skills/<name>/SKILL.md` with
`name`/`description` frontmatter).

Tool wiring:

- **Claude Code** discovers them via `.claude/skills/<name>` — symlinks into `skills/`.
- **OpenAI Codex / Cursor / Gemini** discover them via `.codex/skills/<name>` — the same symlinks —
  or the tool's own skills path.
- **Any other agent** (e.g. GitHub Copilot): read the catalog in [AGENTS.md](../AGENTS.md) and
  perform the skill's instructions inline.

Adding a skill: create `skills/<name>/SKILL.md`, then add both symlinks
(`ln -s ../../skills/<name> .claude/skills/<name>` and the `.codex` twin) and a catalog line in
`AGENTS.md`. Never edit the symlinked copies — edit `skills/` only.
