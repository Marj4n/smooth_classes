# Smooth Classes — Performance Pass

This build keeps the existing class/skill gameplay and the Portal of Sovereignty visuals while reducing work performed on hot server/client paths.

## Cross-class runtime

- Added short-lived five-tick per-player Puffish Skills and talent lookup caches, with immediate invalidation when skills change.
- Clears runtime caches on disconnect/server shutdown and invalidates them immediately on skill changes.
- Routes periodic class passives only through the player's active class instead of probing every class tree.
- Periodic base/class passive work is batched to the cadence the effects actually require.
- Idle runtimes return immediately when they have no active casts/entities.

## Class/runtime-specific changes

- Avenger: reduced player/summon polling frequency, removed several stream/list allocations, optimized hostile/boss checks.
- Archer: optimized Arrow Rain proximity scans and Portal of Sovereignty server/client hot paths.
- Saber: direct nearest-target search instead of stream/min allocation.
- Ruler: Sacred Banner aura maintenance runs at 10 Hz with leases preserving continuous effects; reconciliation no longer allocates a temporary union set every update.
- Caster: direct target selection loops for Meteor/Ice Comet.
- Lancer: skips empty thrust/storm/impale tick passes, uses direct nearest-target scans, and only rewrites movement/knockback attribute modifiers when their value actually changes.
- Rider: the mount runtime skips its server tick completely when no Rider mount is active; mounted attack modifiers are updated only when state changes and lava/fire buffs are refreshed less often.
- Assassin/Berserker/base paths: periodic passive checks are routed and scheduled instead of evaluating all trees every server tick.

## Entities/effects

- Torment field contact scans and landed Arrow Rain proximity scans run at 10 Hz (maximum ~50 ms detection delay).
- Divine Ray light tracking reuses mutable state instead of allocating records every tick.
- Projectile runtime avoids repeated Identifier-to-String allocations and replaces stream target selection with loops.
- Effect behavior removes several temporary lists/sorts and caches talent checks within pulses.

## Client rendering

- Portal renderer keeps the v19 visual design but replaces per-segment trigonometry with incremental rotations in the heavy ring/disc/vortex paths.
- Projected dagger trail rendering uses primitive math instead of temporary Vec3d allocations each frame.

## Validation

- All 200 JSON files parse successfully.
- Java sources pass a syntax/parser sanity check. A full Gradle compile requires the normal project dependencies / Gradle distribution and could not be completed in the offline build environment used for this pass.
