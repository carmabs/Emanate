# Emanate for AI assistants

Files to copy into **your** project so your coding assistant knows how to use Emanate and the recommended practices.
Copy one of the two formats, not both.

| Your tool | Copy | To |
|-----------|------|----|
| Claude Code, or any tool with Agent Skills | the folder [`skill/emanate`](skill/emanate) | `.claude/skills/emanate/` (or `.agents/skills/emanate/`) |
| Claude Code (instructions) | [`instructions/emanate.md`](instructions/emanate.md) | append to `CLAUDE.md` or `AGENTS.md` |
| GitHub Copilot | [`instructions/emanate.md`](instructions/emanate.md) | `.github/copilot-instructions.md` |
| Cursor | [`instructions/emanate.md`](instructions/emanate.md) | `.cursor/rules/emanate.md` |
| Other tools | [`instructions/emanate.md`](instructions/emanate.md) | the file your tool reads for project rules |

**Skill or instructions?** The skill is the better choice when your tool supports it: only its short description is
always in the context, and the rest (`references/`) is read when the task needs it. The instructions are a single short
file that is always loaded, for tools without skills.

Both were written for the version of Emanate in the root `README.md` of this repository. Copy them again when you
update the library.
