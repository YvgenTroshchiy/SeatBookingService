---
name: commit
description: Use whenever the user asks to commit.
argument-hint: "[optional commit message]"
---

## Steps

1. **Inspect changes** — run in parallel: `git status`, `git diff`, `git log --oneline -5`.

2. **Commit**:
    - `git add <paths>` — stage specific files, never `-A` / `.`.
    - `git commit -m '[Area] Short imperative summary'` — single line, single quotes, no body, no `Co-Authored-By`.
    - Use the arg as message verbatim if provided.
    - `git status` to confirm clean.

3. **Report** one line: `Committed <sha> — <subject>`.
