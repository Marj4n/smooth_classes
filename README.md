# Smooth Classes

RPG classes and origins for Minecraft 1.20.1, including class skills, Avenger summons and origin progression. Fabric and Forge share gameplay and assets, with separate loader entrypoints.

Skill menus use custom class artwork and Vampire HUD icons. Avenger soul orders include a sound toggle for individual summons. Retired Homunculus equipment slots return stored items to the player's inventory.

## Build

Use JDK 21; generated mods target Java 17.

```sh
./gradlew build
./gradlew :fabric:build
./gradlew :forge:build
```

Both-loader output is collected in `build/libs/`. Single-loader output is in that module's `build/libs/`. Required integrations include Pufferfish's Skills, Pufferfish's Attributes, Spell Engine, Spell Power and Accessories; use each dependency's matching loader build. Fabric uses Fabric API; Forge uses Forgified Fabric API. See module dependencies and metadata for exact versions.

## Source layout

- `common/src/main`: shared gameplay, data and assets.
- `common/src/client`: shared client rendering and UI.
- `common/src/test`: regression tests.
- `fabric/src` and `forge/src`: loader entrypoints, adapters and metadata.

Build-time resource and packaging checks are part of the existing build.

## Credits and license

Smooth Classes is developed and maintained by **Marj4n**. Its original code and assets whose rights are held by Marj4n are proprietary: **All Rights Reserved**. See [LICENSE.txt](LICENSE.txt) for the permissions to play official releases and the written-permission requirement for reuse, modification, ports and redistribution.

Minecraft, loaders, libraries and other mod integrations retain their own rights and licenses. Applicable third-party and historical notices are documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), including the structure credits in `FORBIDDEN_TREE_CREDITS.txt`. Existing valid rights remain applicable.

## Reuse permissions

Reusing or modifying Marj4n-owned source code or assets requires prior written permission. Contact Marj4n through [nivekts.vercel.app](https://nivekts.vercel.app/) with the intended use. See [LICENSE.txt](LICENSE.txt) for gameplay permissions and exceptions. Third-party licenses, existing grants and GitHub platform rights remain applicable.

## Current replacement validation

The current original asset replacements include effect models/materials, equipped animation clips, Dreadglare models, the Rider mount and Siren tail, Dread Steed materials, pixel artwork, sounds and the Forbidden Tree. The Rider surface-motion and flight calculations have separate behavior-preserving implementations.

The current engineering checks pass **227 Fabric +116 Forge tests (343 total)**. Fluid adapter comparisons cover 937,280 test-double steps; flight comparisons cover 71,752 cases/sequence steps with Minecraft's vector/math API. These scoped checks preserve the frozen calculation contracts and resource packages. They do not establish universal mounted gameplay, Forge runtime, shader or multiplayer coverage, or a legal determination of exclusive rights.
