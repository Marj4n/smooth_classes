# Multi-loader build validation

Status: **Fabric and Forge compile/package checks passed.**

No Minecraft client or dedicated server was launched. Gameplay, world generation,
multiplayer, rendering lifecycle and optional mod integration behavior still need
runtime testing. These artifacts are development candidates, not a tested release.

`./gradlew build` creates the Fabric and Forge JARs in `build/libs/`.
The build also verifies loader metadata, mixin classes/refmaps and all shared
asset/data files. Both builds target Java 17 bytecode with Gradle JVM Java 21.

The Fabric build requires Fabric API; the Forge build requires Forgified Fabric API.
No Sinytra Connector is required by these native Forge entrypoints.
Required dependencies must use matching loader variants. Progression retains its
required dependency on Smooth Classes; it was not converted to standalone leveling.

The source is on `codex/multiloader-1.20.1`; previous branch: `dev-origin`.
Original source backup: `C:\Users\kevin\OneDrive\Documents\ChatGPT\Minecraft Project\multiloader-work\smooth_classes-before-multiloader.zip`.
Existing uncommitted gradle.properties contents were included in the backup.
Public CurseForge descriptions have not been edited; draft replacements are in docs/.

Build was also executed successfully in this installed project directory.
Compiler deprecations and remapping notices remain; no runtime test is claimed.

- smooth_classes-1.0.5-1.20.1-fabric.jar: SHA-256 `827041ada890f3f66ab35797fecf29a4a321a709b33f594cccb3196417b67a84`.
- smooth_classes-1.0.5-1.20.1-forge.jar: SHA-256 `ffc5ffd90aa72b2c5ccc5c9b9c919877b8e8bd3c9e78e244a4380278e1a455d3`.
