# Emanate for AI assistants

A skill to copy into **your** project so your coding assistant knows how to use Emanate and the recommended practices.

Run this from the root of your project, with the version of Emanate that the project uses. The same command installs
the skill and updates it:

```bash
V=7.0.0; D=.agents/skills; rm -rf "$D/emanate"; mkdir -p "$D" && curl -fsSL "https://github.com/carmabs/Emanate/archive/refs/tags/$V.tar.gz" | tar -xz -C "$D" --strip-components=3 "Emanate-$V/ai/skill/emanate"
```

`D` is the skills folder of your tool. Check where your tool looks for skills and change it if needed: for Claude Code,
use `D=.claude/skills`.

If your tool doesn't support skills, add this line to the file it reads for project rules, such as `AGENTS.md`:

> For code that uses Emanate, read `.agents/skills/emanate/SKILL.md` first.

Only the short description of the skill is always in the context. The rest, including `references/`, is read when the
task needs it.

## Keeping it up to date

Each release of Emanate contains the skill for that version, so use the same version as the library, not the latest
one. **Run the command again when you update Emanate.**

Commit the skill in your project, so the whole team uses the same one and each update is a diff you can review. The
skill says which version it was written for: if it doesn't match the project, the assistant tells you to update it.
