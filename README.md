# Smooth Classes

RPG classes and origins for Minecraft 1.20.1, including class skills, Avenger summons and origin progression. Fabric and Forge share gameplay and assets, with separate loader entrypoints.

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

By Marj4n. Third-party art and integration notices remain with their assets, including `FORBIDDEN_TREE_CREDITS.txt`. See [LICENSE.txt](LICENSE.txt).
