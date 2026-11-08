# Emanate for AI assistants

A skill to copy into **your** project so your coding assistant knows how to use Emanate and the recommended practices.

Copy the folder [`skill/emanate`](skill/emanate) to the skills folder of your tool:

| Your tool | Copy to |
|-----------|---------|
| Claude Code | `.claude/skills/emanate/` |
| Other tools with Agent Skills | `.agents/skills/emanate/`, or the skills folder of your tool |

Only the short description of the skill is always in the context. The rest, including `references/`, is read when the
task needs it.

It was written for the version of Emanate in the root `README.md` of this repository. Copy it again when you update
the library.
