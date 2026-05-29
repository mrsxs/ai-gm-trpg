# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

This repo is **single-context**: one `CONTEXT.md` + `docs/adr/` at the root. All microservices (`gateway`, `user-service`, `scenario-service`, `game-service`, `ai-engine-service`, `memory-service`) share one domain vocabulary and one Single-Source-of-Truth baseline, so there is no per-context split.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The producer skill (`/grill-with-docs`) creates them lazily when terms or decisions actually get resolved.

> Note: until these are created, the design docs at the repo root remain the authoritative source — see `CLAUDE.md` ("最高裁决规则": the 实现规格书's baseline §1 is the SSOT, and its §五 术语表 is the current glossary).

## File structure

Single-context repo (this repo):

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-<decision>.md
│   └── 0002-<decision>.md
└── ... (Maven modules + frontend/)
```

(A multi-context repo would instead have `CONTEXT-MAP.md` at the root pointing at one `CONTEXT.md` per context, with `src/<context>/docs/adr/` for context-scoped decisions. This repo does not use that layout.)

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md` (and, until it exists, the 术语表 in the architecture design doc). Don't drift to synonyms the glossary explicitly avoids — e.g. use the locked `ResultCode` enum names, the `node_*`/`npc_*` id conventions, and the `transition` / 白名单校验 terminology, not ad-hoc aliases.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/grill-with-docs`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (…) — but worth reopening because…_
