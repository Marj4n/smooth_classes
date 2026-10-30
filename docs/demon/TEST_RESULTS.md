# Demon Lord development verification

Test dates: 27–28 October 2026 (Asia/Bangkok). Minecraft 1.20.1. Branch `codex/demon-world-ender-1.20.1`. This is a development snapshot, not an uploaded release. Project version remains `1.0.5-1.20.1`.

## Implemented

Playable Demon selection, safe one-time Nether birth, Cinder Reflex, permanent World-Ender choice, Infernal Diet, persistent Netherbound decay, real structure-loot objectives, Brand of Ruin, Netherforged, Pandemonium, boss-credit progression, Recognition/Sovereign respect, native Nether army orders, Last Tribute, exact Black Throne binding, Demon Lord, Thronebound fatal rescue and reciprocal-portal reinforcement.

The other three Demon paths deliberately display Coming Soon, as requested. No later apocalypse progression is included. Gameplay rules, scaling, config and blueprint are in `DEMON_WORLD_ENDER_TO_DEMON_LORD.md`.

## Build and automated tests

- Final `gradlew build --no-daemon`: successful, 1 minute 34 seconds. Fabric and Forge JAR metadata, mixins and shared resources validated.
- 35 JUnit tests, zero failures/errors/skips: 20 Demon rules, 8 Demon persistence and 7 existing Vampire graveyard tests. Gradle reused unchanged passing test outputs on the final build.
- Tests cover complete Cinder cycles, decay boundaries/recovery, exact throne rotations, sphere ordering, 10,000-entry fair scheduling, same-UUID dimension-replacement safety, ownership/curse/birth persistence, pending burst serialization and malformed/bounded configuration.
- `git diff --check`: clean. Of 126 previously captured Origin/Avenger/tree files, 120 are byte-identical. The six changes are shared Origin dispatcher/progress/unlock/runtime/type files and the tree-category index. This is a bounded regression audit, not a complete gameplay regression run of every existing class.

Tested Fabric JAR SHA-256:

```text
92804b63b7511f827fad96fce8ef87fa4aae21154b93ee14caab2ed62f7f2372
```

The final built artifact and installed test-server JAR were compared byte for byte.

## Actual runtime acceptance

The final core run recorded **162 passing assertions, zero failures**. Raw records are in `reports/final-core-runtime-tests.json`; the ordered command transcript is `reports/final-acceptance.log`. These are actual dedicated-server checks with a connected Minecraft client. Later-stage fixtures grant explicit test-only progression flags, so this is not a claim of completing every mission in an uninterrupted Survival playthrough.

Verified in game:

- Safe initial birth and no repeated birth for the same UUID; normal food before path selection; immediate Cinder protection and recovery lockout; permanent path choice and disabled Coming Soon paths.
- Real food consumption/hunger/saturation, both golden apples rejected without effects or item loss, Nether recovery persistence and Demon Nether-bed interception. The full right-click planting/eating animation combination has not been exhaustively exercised with every client interaction mod.
- Real curse raycast, 30-second cast recovery, armor/outgoing damage modifiers, milk-proof state, recall during recovery, actual DoT, denied-region DoT suppression, public cleansing and finite death propagation.
- Actual conversion preserves one live mob, native identity, commander and order; Overworld visitors are excluded; Recognition retaliation, Sovereign immunity, claim/persistence, attack targeting and withdrawal.
- Domain duration/cooldown/buffs, existing/new/waterlogged water, permanent evaporation and modifier cleanup.
- All four approved throne rotations, wrong blocks and obstructed seat rejected, Adventure mode rejected, real Survival Beacon binding/seat, safe post-tick seating/dismount and refusal of a second intact active throne.
- Real `damage ... 100 minecraft:generic`: Last Tribute actually kills its sacrifice and uses its own cooldown; Thronebound returns the living player to the bound Nether seat with exactly 6 HP, a 15-minute cooldown and preserved inventory/XP.
- Normal deaths in Nether or with an invalid throne; active domain cleans up and a dead player cannot command an army.
- Existing reciprocal portals transfer three real mob UUIDs, preserving commanders, without source copies, duplicate UUIDs or solid clipping; a small frame rejects a Ghast. Vanilla portal cooldown and exit placement prevent immediate bounce-back.
- Actual generated Fortress and Bastion starts/chests/loot: a real container transfer grants each objective, while a replacement player chest inside the same structure does not.

The fixture script disables natural regeneration and fall damage in its disposable world; fatal-damage probes still use actual damage. It removes fixture item/XP drops before the inventory-preservation snapshot to avoid unrelated pickups affecting the comparison. No original player world is used.

## Native mod compatibility through Smooth Bridge

The dedicated server and primary client load Smooth Bridge 0.7.0 with the original Skarrier Mobs 1.1.1, Cult of Azazel 1.1.4.12 and Cataclysm 3.31 JARs, alongside BOMD 1.7.5 and Soulslike Weaponry 1.4.10. The original Bridge source and user profiles were not changed. See the dependency and spawn-evidence JSON reports.

This includes ordinary native mobs, not only boss integrations. Actual instantiated/claimed original entities and an eight-second, AI-enabled MARCH movement probe passed:

| Original entity | Distance moved |
|---|---:|
| `skarrier_mobs:hellamander` | 11.73 blocks |
| `skarrier_mobs:crimlin` | 13.32 blocks |
| `netherman:ghastly` | 5.42 blocks, including flight |
| `cataclysm:ignited_berserker` | 17.08 blocks |

Crimlin initially retained its first path point even though it was already at that position. The final implementation consumes at most four physically reached path points per service; the actual movement probe above passed after that fix. It retains native paths, movement/collision controllers and combat AI, without teleporting or replacing the mob.

The five required actual encounter IDs are registered and recognized; the actual boss/miniboss entity factories are excluded from army claiming. Human Azazel does not grant Nether Azazel credit. Ignited Revenant is excluded as a miniboss. These checks do not establish that a player completed full combat against all five bosses.

Nether identity uses actual biome spawn pools, audited native tags and per-instance generated-structure provenance for mixed-origin types. Moving an arbitrary Overworld mob into the Nether does not make it native. Additional custom mobs may need the documented entity tags/config; every mod's private custom spawning/AI cannot be inferred automatically.

## Client UI

Actually observed with the Minecraft client: Origin page toggles with Z and remains selected across server sync, all three ability slots render, H opens the six-order army wheel, and clicking RALLY produces the server response. The four-path screen renders World-Ender and three disabled Coming Soon choices. Screenshots are included under `reports/screenshots`.

The primary client's JAR predates server-only navigation diagnostics. All 1,027 final-JAR client/asset entries were compared byte for byte against that client and were identical. The dedicated test server runs the exact final JAR hashed above.

## Two-client multiplayer status

**Passed on 28 October 2026.** After the user cancelled the Windows Firewall dialog, two real Minecraft clients, `DemonTester` and `DemonPeer`, connected simultaneously to the dedicated Smooth Bridge server. The `multiplayer` scenario recorded five passing assertions: Demon curse immunity, no theft of another Demon's army, enemy-owned troop damage despite Sovereign immunity, own-army friendly-fire protection and preserved commander UUID in NBT. Raw assertions, online-player evidence and command output are in `reports/multiplayer-results.json` and `reports/multiplayer.log`.

Combined coverage is **162 core + 5 two-client = 167 passing runtime assertions**, with zero recorded failures in those final runs. Both clients and the server use Java 17 and the exact final JAR. Both clients use the unlimited FPS setting for this supplementary test; prior performance figures remain from the separately recorded 120-FPS-limited primary-client benchmark. The new peer also selected Demon through the ordinary Origin UI and visibly received its first Nether birth before the multiplayer fixture. This does not establish every multiplayer race, claim integration or public-server load pattern. No firewall setting was changed.

## Performance observations

Machine reported by the JVM: Intel Core i3-12100F, 8 logical processors, approximately 16 GB RAM, Windows 11. This was a local isolated compatibility profile, not a cloud machine or the entire Smooth Odyssey pack. Dedicated server: Java 17, 3 GB heap, view distance 4 and simulation distance 3. Primary client: Java 17, 2 GB heap, 1280×900, render distance 5, 120 FPS limit, no shaders. The peer was not connected during benchmarks; other desktop work and build activity can affect measurements.

| Scenario | Server callback sampler | Actual client FPS |
|---|---|---|
| 8,405 existing water sources | 241 ticks, mean 10.57 ms, p95 30.17 ms; all sources removed | 11 samples, mean 111.55, minimum/p05 92 |
| 500 real army mobs | 661 ticks, mean 32.06 ms, p95 45.34 ms; 11,835 navigation calls, 999.08 ms aggregate navigation CPU | 33 samples, mean 73.21, minimum 46, p05 51 |

The sampler includes Smooth Classes' END_SERVER_TICK work but is not the complete JVM frame. The accompanying **33.52-second JFR** reports full server ticks: mean **36.44 ms**, p90 **39.44 ms**, p99 **197.05 ms**, maximum **262.03 ms**. Ten collections consumed **4.497 seconds** (about 13.4% of the recording); allocation was about **133.8 MB/s**. Reported outbound packet payload was about **333.6 KB/s / 10,950 packets/s**, without claiming wire-protocol overhead is included.

These results show the 500-mob fixture runs, but also show substantial GC/tick spikes. The callback's `estimated_tps_ceiling` is a mathematical estimate, not an observed stable-TPS guarantee. There is no promise of smooth 500-mob play in the complete modpack or on every medium PC. Scheduler/navigation/block-write budgets are configurable; no hard army-size cap was introduced.

## Not verified

- Native Forge in-game runtime. Both loader builds pass, but Bridge-on-Fabric is not Forge runtime verification; the delivery includes the tested Fabric JAR only.
- An uninterrupted Survival run fighting all five bosses, full legacy-world migration, the entire Smooth Odyssey modpack, every native combat AI or every portal obstruction/race.
- Every external claim plugin. The generic denial adapter was exercised; Open Parties and Claims public API integration is present but an exhaustive live two-owner claimed-region matrix is not established.
- Multiplayer edge cases beyond the five recorded two-client assertions, especially larger public-server timing/race cases and exhaustive claimed-region interactions.

## Known issues and limits

- The 500-mob profiler shows meaningful allocation/GC and tick spikes as quantified above.
- Early background/title-screen client launches had native `glfw.dll` access-violation crashes. Retried clients connected successfully and completed the two-client test with the in-game FPS limit disabled. The native launch issue was not repaired in this mod; the exact external cause is not established.
- Third-party dependency startup logs contain Kilt optional-class/method warnings; startup and core runtime acceptance still completed. These dependency warnings have not all been repaired by this feature.
- Dedicated-server destructive terrain effects default to disabled; administrators must explicitly opt in. The test fixture opts in only in its disposable world.
- Other three Demon paths intentionally remain Coming Soon. No implemented World-Ender-to-Lord mechanic is represented by those placeholders.

Source, patch bases, dependency guidance, proposed commit and versioning recommendation are in `RELEASE_NOTES.md`. No version bump, push, CurseForge upload or public release was performed.
