# Agent Rules for This Repository

## Default Reading Order
1. Read `README.md`.
2. Read `docs/INDEX.md`.
3. Read additional docs only if required by the task.

## Markdown Scope Control
- Treat `docs/INDEX.md` as the source of truth for document precedence.
- Do not read `docs/archive/` by default.
- Read `docs/archive/` only when:
  - the task is explicitly about history, or
  - regression investigation requires historical context.

## Conflict Resolution
- If Markdown documents conflict, follow `docs/INDEX.md`.
- If there is still ambiguity, follow code behavior in `src/` and call out the mismatch.
