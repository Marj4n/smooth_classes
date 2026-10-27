# Smooth Classes â€” multi-loader migration

Open this root folder in IntelliJ with Gradle JVM Java 21. Targets Java 17 / Minecraft 1.20.1.

`./gradlew build` builds both loader JARs and collects them in `build/libs/`.
`./gradlew :fabric:build` or `./gradlew :forge:build` builds one loader only.

Shared gameplay, client rendering and assets are under common/src. The two platform
modules supply their own loader discovery, entrypoints and registry integration.
Fabric requires Fabric API; Forge requires Forgified Fabric API (native Forge API port).
No Sinytra Connector is required by this project's Forge bootstrap.
Use matching loader builds of every required dependency. Optional integrations are
only available where that mod and compatible APIs exist. Bewitchment-specific mixins
are Fabric-only; they are omitted from the Forge JAR, with core Origin gameplay retained.

Both loader builds and packaging checks passed. Game/client and dedicated-server startup have NOT been tested. See docs/BUILD_VALIDATION.md before publishing a release.
