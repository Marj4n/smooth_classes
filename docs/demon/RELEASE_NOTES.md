# Demon Lord development delivery

This is a development snapshot for Minecraft 1.20.1. The project version remains `1.0.5-1.20.1`; no automatic release, upload, push or version bump was performed.

## Contents

- `smooth_classes-1.0.5-1.20.1-fabric.jar`: the Fabric artifact used for runtime acceptance checks.
- `Smooth-Classes-Demon-Lord-source.zip`: complete current repository source/assets/build files, without Git internals or disposable game worlds.
- `demon-feature.patch`: tracked and new Demon changes relative to checkpoint `9599d34c31d2f4ca503925176a817e416abed1b4`.
- `combined-from-main.patch`: the combined changes relative to `b46c06d0e5e2a89e9f329000a43630dbd5c135a5`, including the pre-existing shared Vampire graveyard work preserved in that checkpoint.
- Main implementation document, changelog, test guide, runtime results, benchmarks, profiler reports and SHA-256 manifest.

The actual working source is on branch `codex/demon-world-ender-1.20.1`. Demon changes are reviewable working-tree changes. A private temporary Git index is used only to export the complete patch; the user's staging area is not replaced.

## Installation and dependencies

Use a copied test profile with the same supported Minecraft/loader/dependency versions. Replace its previous Smooth Classes JAR rather than loading two copies. This delivery does not modify existing player profiles, worlds or Smooth Bridge source.

Core runtime checks use Fabric Loader 0.19.5, Fabric API 0.92.12, Pufferfish's Skills 0.19.1, Pufferfish's Attributes 0.8.2, Spell Engine 1.10.7, Spell Power 1.6.0, Accessories beta.48, Cloth Config 11.1.136, Player Animator and Open Parties and Claims 0.31.6. The compatibility lab also loads Smooth Bridge 0.7.0 with the original Cult of Azazel 1.1.4.12, Cataclysm 3.31 and Skarrier Mobs 1.1.1 JARs, plus BOMD 1.7.5 and Soulslike Weaponry 1.4.10 and their resolved dependencies.

Original third-party gameplay JARs and their worlds are not bundled here. On Fabric, keep only the intended Smooth Bridge version. The lab selected 0.7.0; it did not copy the older duplicate Bridge version from the existing user profile.

The opt-in test harness is disabled during ordinary play. Do not add `-Dsmooth_classes.demonTest=true` to a public or survival profile.

## Forge status

The existing multiloader project compiles both Fabric and Forge and validates both JAR layouts. Native Forge runtime acceptance has not been completed for this feature. Its build output remains in the repository's `build/libs`; it is not presented as a tested Forge release in this delivery. Smooth Bridge-on-Fabric testing is not a native Forge test.

## Suggested commit and versioning

Suggested feature commit:

```text
feat: add Demon Origin progression through Demon Lord
```

Recommend the next unused **minor** release because this adds a playable Origin and several systems. Determine its exact number only after checking the official project listing and obtaining release approval. Keep this development snapshot distinct from the currently published build even though its internal version remains unchanged.

Read `TEST_RESULTS.md` for actual verification coverage and remaining limits. Development evidence is not a claim that every possible modpack, boss AI, protected region or multiplayer timing has been tested.
