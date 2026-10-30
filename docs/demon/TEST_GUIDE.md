# Demon acceptance tests

Use a separate copied profile and disposable world. Never run the destructive fixture harness in a survival save.

## Ordinary gameplay

1. Select Demon through the Origin menu. Confirm one safe initial Nether spawn, then reconnect and confirm it does not repeat.
2. Touch fire/lava. Check immediate protection, Cinder Reflex's 666-second active timer and subsequent 666-second recovery.
3. Choose World-Ender. Check the three Coming Soon paths cannot be selected.
4. Eat each allowed infernal food. Try bread and both golden apples; check inventory and effects. Plant the same Nether plants normally.
5. Loot an actual generated Fortress and Bastion. Opening a player-placed chest must not grant either objective.
6. Use Slot I against a mob, recall during recovery, drink milk, save/reload and use the cleansing hook in an integration test.
7. Complete Hot Tourist Destinations. Check permanent fire protection, Slot II, existing/new/waterlogged water and permanent changes after expiry. On a dedicated test server explicitly opt in to terrain changes.
8. Defeat the actual required bosses. Check Nether Azazel versus human Azazel. Missing dependencies must be reported without granting credit.
9. Exercise Recognition retaliation and Sovereign immunity against vanilla and modded natives. Bring an ordinary Overworld mob into the Nether and a native into the Overworld.
10. Use every army order. Repeat with two players and owned/tamed mobs. Check ranged, large, flying and brain-driven mobs separately.
11. Build all four throne rotations. Try wrong blocks, obstructed clearance, another player's throne and an intact second throne. Sit, stand, disconnect and restart.
12. Test real fatal damage with a sacrifice, without one, in the Nether, with a broken throne and during cooldown. Check six HP, inventory and XP.
13. Test existing reciprocal portals, blocked exits, too-small frames, unloaded source troops, destroyed frames, logout and replacement orders. Check entity UUIDs across both worlds.

Read-only player feedback commands:

```text
/smoothclasses demon status
/smoothclasses demon path
/smoothclasses demon path world_ender
/smoothclasses demon order rally
/smoothclasses demon order march
/smoothclasses demon order attack
/smoothclasses demon order guard
/smoothclasses demon order withdraw
/smoothclasses demon order backup
```

These commands still enforce the player's actual unlocks. They are not progression cheats.

## Opt-in runtime harness

JVM flag: `-Dsmooth_classes.demonTest=true`. The command is otherwise absent. It requires operator permission and an explicitly named disposable online player.

```text
/smoothclasses_demon_test base DemonTester
/smoothclasses_demon_test curse DemonTester
/smoothclasses_demon_test conversion DemonTester
/smoothclasses_demon_test respect DemonTester
/smoothclasses_demon_test domain_prepare DemonTester
# wait several server ticks
/smoothclasses_demon_test domain_check DemonTester
/smoothclasses_demon_test throne DemonTester
/smoothclasses_demon_test seat_prepare DemonTester
# wait several server ticks
/smoothclasses_demon_test seat_check DemonTester
/smoothclasses_demon_test modded DemonTester
/smoothclasses_demon_test trial_fortress DemonTester
/smoothclasses_demon_test trial_bastion DemonTester
/smoothclasses_demon_test multiplayer DemonTester
```

`modded` requires the audited original mods and Smooth Bridge on Fabric. `multiplayer` requires a second real client named DemonPeer. Fixtures explicitly grant isolated test flags to exercise later abilities; they do not demonstrate a complete Survival boss run.

Fatal scenarios:

```text
/smoothclasses_demon_test tribute_prepare DemonTester
/damage DemonTester 100 minecraft:generic
/smoothclasses_demon_test tribute_check DemonTester
/smoothclasses_demon_test fatal_prepare DemonTester
/damage DemonTester 100 minecraft:generic
# wait at least 20 server ticks
/smoothclasses_demon_test fatal_check DemonTester
```

Allow spawn/teleport protection and damage invulnerability to expire before each fatal hit. `fatal_nether_prepare` and `fatal_invalid_prepare` test ordinary death. Follow with `fatal_dead_check`, then `respawn`, and let the actual client process the respawn before continuing. The harness rejects ordinary gameplay scenarios on a dead test player.

Portal fixtures require their Nether source chunk to remain loaded. Use the second player there or a temporary vanilla force-load ticket in the disposable world. This is test setup; gameplay portal searches do not add chunk tickets.

Performance fixtures:

```text
/smoothclasses_demon_test water_prepare DemonTester
# wait for incremental processing
/smoothclasses_demon_test water_check DemonTester
/smoothclasses_demon_test army_prepare DemonTester
# wait for entity/chunk initialization
/smoothclasses_demon_test army_start DemonTester
/jfr start
# collect at least 200 real server ticks; preferably 600+
/smoothclasses_demon_test army_finish DemonTester
/jfr stop
/smoothclasses_demon_test cleanup DemonTester
```

Reports are written to the test server's working directory. Client FPS is sampled by the real opt-in client when the benchmark flag is synchronized. FPS and tick timings depend on hardware, background work, renderer settings, loaded chunks and installed mods. A queue fairness unit test is not an in-game 500-mob benchmark.

Compile/resource/unit checks:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.12'
.\gradlew.bat build --no-daemon
```

Review `TEST_RESULTS.md` for the checks actually executed for the delivered artifact and the remaining limits.
