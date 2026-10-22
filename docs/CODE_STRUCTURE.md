# Code structure — Smooth Classes

Based on **Smooth Classes**' Marj4n conventions, with project-specific feature folders.

Java namespace: `org.marj4n.smooth_classes`.

## Project layout

- `src/main/java/org/marj4n/smooth_classes/` — Shared/server Java code
- `src/client/java/org/marj4n/smooth_classes/` — Client-only Java code (when Loom splits source sets)
- `src/main/resources/` — Fabric mod metadata, mixins, assets and data

## Existing feature folders

- `src/main/java/org/marj4n/smooth_classes/`: `api/`, `block/`, `compat/`, `content/`, `effects/`, `entity/`, `gameplay/`, `integration/`, `mixin/`, `network/`, `origin/`, `registry/`, `runtime/`
- `src/client/java/org/marj4n/smooth_classes/`: `client/`, `mixin/`

## Rules

1. Keep `org.marj4n` lowercase and match Java packages to actual folders.
2. Entrypoints stay wired in `fabric.mod.json`; mixin config paths must continue to resolve.
3. Use `api/`, `compat/`, `config/`, `content/`, `integration/`, `mixin/`, `network/`, `registry/`, `runtime/` only when the mod needs them; no empty folder padding.
4. Keep gameplay behavior, mod IDs, public dependency versions, resource namespace and mixin target names unchanged in a layout-only refactor.
5. Shared/server code belongs under `src/main`; client code goes under `src/client` only if this Gradle project uses split source sets.
6. Run `python scripts/check_structure.py` before committing, then build with the repo's Gradle wrapper when dependencies are available.

