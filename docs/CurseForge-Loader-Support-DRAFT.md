# Project description updates — draft for the multi-loader release

Publish these replacements only after the Fabric and Forge release files have
passed build and runtime validation. They describe the intended dependency layout;
they do not announce that the current public Fabric-only files work on Forge.

## Smooth Progression — replacement compatibility/dependencies section

### Choose Your Mod Loader

Download the **Fabric** or **Forge** build for **Minecraft 1.20.1**.
Install the build that matches your instance, and use the same loader on your
client and dedicated server. Install only one Smooth Progression JAR.

| Dependency | Fabric build | Forge build |
| --- | --- | --- |
| Mod loader | Fabric Loader | Minecraft Forge 47 |
| API | Fabric API | Forgified Fabric API |
| Pufferfish's Skills | Fabric version | Forge version |
| Smooth Classes | Matching Fabric version | Matching Forge version |

**Fabric API is required only by the Fabric build.** The Forge build uses
**Forgified Fabric API**, the Forge implementation of those shared API hooks.
The Forge bootstrap is a native Forge mod and does not require Sinytra Connector.

Smooth Classes also has its own required dependencies; install their matching
loader versions. Jade is an optional integration, not a required dependency.

## Smooth Classes — replacement compatibility/dependencies section

### Fabric & Forge

Choose the **Fabric** or **Forge** download for **Minecraft 1.20.1**.
Both builds share the same class, progression-tree and Origin resources, with
loader-specific registration and integration code.

| Required dependency | Fabric build | Forge build |
| --- | --- | --- |
| Mod loader | Fabric Loader | Minecraft Forge 47 |
| API | Fabric API | Forgified Fabric API |
| Pufferfish's Skills | Fabric version | Forge version |
| Pufferfish's Attributes | Fabric version | Forge version |
| Spell Engine | Compatible Fabric version | Compatible Forge version |
| Spell Power Attributes | Compatible Fabric version | Compatible Forge version |
| Accessories | Compatible Fabric version | Compatible Forge version for 1.20.1 |

The Forge build does not require Sinytra Connector. Dependencies may require
additional libraries of their own; use the files selected for your loader.

### Optional Integrations

Smooth Progression is an optional companion to Smooth Classes. Other integrations
include Simply Swords, Paladins & Priests, Archers, Immersive Melodies, Open Parties
and Claims, Patchouli, and other supported RPG mods.

**Optional integration availability depends on the loader and compatible mod
versions.** Installing a Forge build of Smooth Classes does not make Fabric-only
third-party mods available on Forge. Bewitchment-specific compatibility mixins
remain Fabric-only; their absence does not remove Smooth Classes' own Vampire system.

Use matching client/server versions and keep only one loader build of each mod.
