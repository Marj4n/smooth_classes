# Smooth Classes — Demon Origin: World-Ender to Demon Lord

Minecraft 1.20.1. Implementation branch: `codex/demon-world-ender-1.20.1`.
This development build retains the existing project version; it is not a published release.

## Progression and missions

The Demon is selectable through the existing Origin selector and has its own Pufferfish's Skills category, `smooth_classes:origin_demon`. Class progression remains a separate system. Objectives grant their nodes on the server; spending ordinary class points does not bypass a Demon mission.

1. **Demon.** First legitimate Origin selection sends the player to a safe Nether location. A world ledger records the player's UUID so reconnecting, respawning or selecting again does not repeatedly generate a spawn. The search checks solid safe ground, headroom and nearby danger. It does not build a structure.
2. **Cinder Reflex.** First fire/lava contact unlocks the passive. The triggering damage is protected immediately. Fire Resistance lasts 666 gameplay seconds, followed by 666 seconds of recovery. Further fire does not refresh an active cycle or bypass recovery.
3. **Choose a path.** World-Ender is available after Cinder Reflex. Temptation/Succubus, Hellkeeper and Divine Demon are visibly disabled as Coming Soon. The server accepts World-Ender once; an ordinary packet cannot replace the choice.
4. **World-Ender / Infernal Diet.** The restricted diet begins when this path is selected, not at initial Demon selection.
5. **Trial of the Nether.** Enter a naturally generated Nether Fortress and a naturally generated Bastion Remnant, then actually take loot from a genuine structure chest in each. Merely opening a chest, placing a chest, visiting the dimension or transferring an unrelated item does not satisfy the mission. YUNG's Better Nether Fortresses is recognized through its actual registered structure and fortress loot tables.
6. **Brand of Ruin — Slot I.** Both trial objectives unlock the curse.
7. **Netherforged + Pandemonium — Slot II.** Complete vanilla **Hot Tourist Destinations** after the trial. A previously earned advancement counts. Cinder Reflex becomes permanent fire protection.
8. **Recognition of the Nether.** Credit for the Decaying King and Nether Gauntlet grants the first native-mob respect tier.
9. **Sovereign's Presence + All Hell Shall Kneel — Slot III.** Credit for all five required rulers grants full native respect, Last Tribute and the army wheel.
10. **Demon Lord.** Build the exact Black Throne in the Nether and interact with its Beacon in Survival while Sovereign and the army are unlocked. Binding creates a real seat, soul effects, title and advancement toast. Netherbound ends; Infernal Diet remains. Thronebound and cross-dimensional Hellgate Backup become available.

Progression ends at Demon Lord. Later World-Ender forms and apocalypse abilities are outside this implementation.

## Netherbound, sleep and food

Before Demon Lord, time outside the Nether accumulates on the server and survives logout/restart. Rain and water themselves impose no Demon weakness. Milk cannot erase the timer.

| Time outside Nether | Effect |
|---|---|
| 0–150 seconds | Stable |
| 150–210 seconds | HUD warning |
| 210–300 seconds | Slowness I, Mining Fatigue I, Hunger I |
| 300+ seconds | Slowness III, Mining Fatigue III, Hunger II; decay appearance |

The timer is capped at 300 seconds. Returning to the Nether removes ten accumulated ticks per gameplay tick: maximum exposure takes about 30 seconds to recover. Brief portal dipping does not reset it. Becoming Demon Lord clears the timer permanently.

Demon players can sleep in an unoccupied Nether bed through a Demon-only interaction hook. Other Origins retain vanilla bed behavior. Demons can eat normal food until choosing World-Ender.

| Infernal food | Hunger | Saturation points |
|---|---:|---:|
| Raw / cooked porkchop | Vanilla | Vanilla |
| Crimson / warped fungus | 2 | 1.2 |
| Crimson / warped roots, Nether sprouts, Nether wart | 1 | 0.6 |

Plants use the real 32-tick eating animation. Their normal planting interactions remain available. Other foods, including both golden apples, are refused before consumption and food effects. Default exhaustion multiplier is 0.5. Lord evolution does not remove the diet.

## Brand of Ruin

Aim at a visible living target within 32 blocks. Only one manually branded target is active per caster. A cast starts a 30-second cooldown. Activating Slot I again recalls the current cast even during that cooldown, cleanses its loaded targets and resets the cooldown to zero. Unloaded targets consult the cancelled token when they return.

The curse has no duration. It persists in entity NBT across saves and dimension transfer until death, recall or `DemonCurse.cleanse(LivingEntity)`, the public cleansing integration hook. Milk/status-effect clearing does not remove its ownership or attribute modifiers.

| Stage | Curse additions |
|---|---|
| Initial Brand | −30% movement speed, −25% armor, −20% outgoing damage |
| Netherforged | Default 1 damage every 40 ticks |
| Sovereign | Death triggers an entity-only infernal explosion and bounded propagation |
| Demon Lord | Default 2 DoT damage; death-burst damage rises from 4 to 7 |

Spread uses a shared finite budget per original cast, default 8 propagated mobs. Burst damage and propagation are separate: the propagation limit does not silently exempt other valid nearby entities from damage. A global hit-work budget schedules chain damage across ticks; pending hits and shared budgets survive a save/restart. Bursts do not destroy terrain. Offline caster UUID credit is retained for qualifying boss kills.

Demon players cannot be branded. PvP, teams, owned troops, registered protection adapters and Open Parties and Claims access are checked. Bosses can receive the manual Brand, but cannot be claimed as troops or selected for propagated curse ownership. Small glyph particles have a fixed per-entity frequency.

## Pandemonium

Slot II anchors a spherical domain at the activation location: radius **64**, duration **45 seconds**, then **60 seconds** of recovery. Moving away does not move the sphere; leaving its dimension ends it. The HUD uses server timers. Buffs apply while the owner is inside their domain and are removed when leaving/ending.

| Stage | Extra armor | Attack bonus | Continuous exposure before burning |
|---|---:|---:|---:|
| World-Ender | 4 | 3 | 8 seconds |
| Recognition | 6 | 3 | 6 seconds |
| Sovereign | 8 | 6 | 4 seconds |
| Demon Lord | 10 | 6 | 2 seconds |

Netherforged unlocks the domain before Recognition, so its initial scaling uses the World-Ender row. Attack bonuses correspond to Strength I/II rather than permanently granting a milk-removable status effect.

Existing sources/flowing water evaporate incrementally; waterlogged blocks retain the block and lose waterlogging. New water proposed inside an allowed active domain is intercepted immediately. Evaporation is permanent and is not rolled back on expiry. Fire placement scales in frequency by stage, checks actual placement legality and protection, and uses the same shared block-write budget.

The scanner visits nearby positions first, never loads chunks, and shares global scan/write budgets across active Demons. Exhausting a write budget leaves the current water position pending for the next tick. If operators greatly lower budgets or activate many domains, completing the whole sphere can take longer than the active duration. This is a deliberate work budget, not an assertion that every unloaded block is processed.

Nether natives, fire-immune entities and Fire Resistance are respected by exposure. PvP/team/protection checks apply to players. On dedicated servers, destructive terrain changes are **off by default** until `multiplayerTerrainChanges` is enabled. Domain combat buffs/exposure remain available. Single-player uses the ordinary protection checks without requiring that dedicated-server opt-in.

## Rulers and Nether mob compatibility

| Objective | Exact entity ID |
|---|---|
| Nether Gauntlet | `bosses_of_mass_destruction:gauntlet` |
| Decaying King | `soulsweapons:accursed_lord_boss` |
| Nether Azazel | `netherman:azazel` |
| Ignis | `cataclysm:ignis` |
| Netherite Monstrosity | `cataclysm:netherite_monstrosity` |

`netherman:azazel_human` is a different encounter and does not count. New Azazel credits require the Nether encounter. Previously recorded exact-type kill statistics and the persistent UUID kill ledger are recognized. A missing required registry entry is reported; progression does not invent or auto-complete an absent boss.

Fabric integration uses the user's Smooth Bridge route for original Forge mods. The code reads actual registered entity types and world data, without importing the bosses' Forge implementation classes into shared code. This preserves optional dependencies and avoids a missing-mod class-link crash.

Compatibility also covers ordinary Nether mobs:

- Entity types exclusively present in the actual Nether biome spawn pools qualify automatically. Types also present in other dimensions' biome pools are not globally reclassified.
- Vanilla natives include Blaze, Ghast, Magma Cube, Piglin, Piglin Brute, Zombified Piglin, Hoglin, Strider and Wither Skeleton.
- Optional entries audited against original local mod JAR data include Skarrier's Hellamander/Crimlin, Cult of Azazel's Ghastly and Cataclysm's Ignited Berserker.
- Cult of Azazel's Believer also occurs in the Overworld. Its first loaded provenance is recorded: only a qualifying generated Nether-only structure establishes native identity for that individual.
- Native identity survives dimension movement and mob conversion; moving an ordinary Overworld mob into the Nether does not make it a native.
- Boss tags, configured rulers, actual server boss-bar fields and Cataclysm's miniboss class family exclude bosses from armies and sacrifices. Ignited Revenant is explicitly excluded.
- Owned tameables, horses, leashed mobs, Avenger ownership tags and another Demon's commander UUID are respected.

Data-pack extension points are `smooth_classes:demon_nether_native`, `demon_nether_structure_native`, `demon_bosses` and `demon_command_excluded` entity-type tags. Optional foreign entries use `required:false`. Operators may also configure additional native/excluded IDs. A new mod with custom event-only or script-only spawning may need a tag/adapter; the implementation does not claim universal knowledge of arbitrary mod scripts.

Recognition prevents natives initiating combat, but allows retaliation against the particular Demon who attacked them. Several attackers are tracked independently. Sovereign prevents native retaliation as well. An enemy Demon's owned troop retains its command relationship rather than being neutralized by the other player's passive.

## All Hell Shall Kneel and Last Tribute

Slot III opens an independent six-section radial wheel. The existing Avenger wheel is preserved.

| Order | Behavior |
|---|---|
| RALLY | Gather eligible nearby natives around the commander |
| MARCH | Follow in separated positions using the mob's own movement controls |
| ATTACK | Engage the visible marked enemy within the 128-block command radius |
| GUARD | Hold the selected area and react to threats against the commander |
| WITHDRAW | Disengage and gather around the commander |
| BACKUP | Gather locally in the Nether; Lord can call existing troops through a valid portal pair outside it |

There is no hard army-size cap. Loaded mobs are serviced by a fair rotating queue, default 48 work items per tick with at least 10 ticks between an individual mob's navigation updates. Ground mobs retain pathfinding/brain movement; flying mobs retain flight controls. Native ranged/melee AI is not replaced by cloned generic minions. Commander UUID, order, guard location and target persist on the real mob. Offline commanders do not generate new armies.

Ground navigation uses bounded intermediate destinations within the mob's existing follow range. A bounded check consumes only path points already physically reached, accommodating a bridged Crimlin navigator that otherwise retained its starting point. It never teleports troops or substitutes generic movement/combat controllers. Real AI-enabled movement checks passed for Hellamander, Crimlin, Ghastly and Ignited Berserker through Smooth Bridge.

**Last Tribute** is available at Sovereign. Before a fatal hit becomes an actual player death, it selects the nearest eligible native within 24 blocks and verifies that it really dies. It restores the Demon to 6 HP and starts its own 180-second cooldown. Portable natives still count outside the Nether. Bosses, another player's troops and owned pets are excluded. A failed sacrifice does not grant a free rescue. Last Tribute is checked before Thronebound, so one fatal hit cannot consume both rescues.

## The Black Throne and Thronebound

Locked blueprint: **3×3 footprint, four blocks high, 18 Netherite Blocks and one Beacon**. `N` is Netherite, `B` Beacon, `.` required air. Rows below run front to back.

```text
Y=3       Y=2       Y=1       Y=0
...       ...       ...       NNN
...       ...       N.N       NNN
N.N       NBN       NNN       NNN
```

Beacon local coordinate is `(1,2,2)`. All four horizontal rotations are accepted; wrong materials, obstructed clearance or partial builds are rejected. The visible core does not promise a vanilla sky beam. A bound throne belongs to one UUID. An intact active throne cannot be replaced by binding another; a destroyed throne can be rebuilt and rebound. Overlapping ownership is refused. Integrity checks inspect only the small blueprint, without continuously loading its chunks.

The seat is a real transient entity with a passenger relationship, safe standing position and disconnect cleanup. It does not save an inventory or clone the player. Nether binding requires Survival and the completed Sovereign/army prerequisites.

**Thronebound** is Demon Lord's fallback after Last Tribute. Requirements: fatal damage outside the Nether, intact bound throne, cooldown ready. It intercepts fatal damage at vanilla's totem checkpoint, briefly floats the protected living player in soul fire, then returns them seated at their throne with exactly **6 HP**. Inventory and XP remain on the living player. Cooldown is **900 gameplay seconds**. It does not rescue a death in the Nether or with an invalid throne. Explicit administrative `/kill` retains its ordinary meaning. If the throne is destroyed during the brief return sequence, normal death follows rather than granting permanent immunity.

Only an actual fatal return may load the at-most-four chunks intersecting the known bound throne. Army/domain scans never force-load chunks. Pending return and reserved cooldown are persistent; transient seats are recreated only through a validated return.

## Hellgate Backup

Cross-dimensional Backup requires Demon Lord in the Overworld, an intact portal within 12 blocks, and the already loaded compatible Nether-side portal. The search uses actual vanilla portal POIs and a reciprocal coordinate-link check. It does not create a portal or summon replacement troops.

Eligible existing mobs within 128 blocks of the Nether portal are queued. Both portal rectangles must fit the mob's full hitbox; a small portal rejects a Ghast. Exit clearance includes blocks, fluids, other entities and ground support. Transfers retain entity UUID/commander data and remove the source entity through Minecraft's dimension-transfer path. Blocked exits wait rather than duplicating mobs. Destroying a portal, logging out, leaving the destination world or issuing a replacement order cancels the job. Unsupported dimensions report an actionable message.

## Configuration: provisional defaults

`config/smooth_classes-demon.json` is generated once and read at startup. Invalid JSON fails with the file location; non-finite/unbounded balancing values are clamped. Timing, mission order, diet membership and approved throne geometry above are locked gameplay rules. The following values are provisional balancing/performance defaults.

| Field | Default | Meaning |
|---|---:|---|
| exhaustionMultiplier | 0.5 | World-Ender exhaustion scaling |
| fungusSaturation / plantSaturation | 1.2 / 0.6 | Saturation points |
| curseDotInterval | 40 | Ticks between curse DoT |
| curseSpeedReduction / curseArmorReduction / curseOutgoingReduction | 0.30 / 0.25 / 0.20 | Brand debuff fractions |
| curseDotDamage / lordDotDamage | 1 / 2 | Raw damage before applicable mechanics |
| explosionDamage / lordExplosionDamage | 4 / 7 | Entity-only death burst |
| propagationBudget / propagationRadius | 8 / 6 | Shared propagated-target budget / radius |
| curseBurstWorkBudget | 64 | Queued burst hits per server tick |
| domainScanBudget | 8192 | Global visited domain positions per tick |
| domainBlockBudget | 2048 | Global domain block writes per tick |
| domainFire / domainFireInterval | true / 40 | Fire placement enable / interval in ticks |
| domainExposureSeconds | [8, 6, 4, 2] | Exposure threshold by evolution stage |
| domainArmor / domainAttack | [4, 6, 8, 10] / [3, 3, 6, 6] | Domain stat bonuses by evolution stage |
| domainFireChanceDivisors | [1024, 512, 341, 256] | Per-eligible-position fire roll denominator by stage |
| multiplayerTerrainChanges | false | Dedicated-server destructive terrain opt-in |
| armyUpdateBudget / armyTickInterval | 48 / 10 | Fair scheduler work / per-mob delay |
| teleportProtectionTicks | 60 | Brief post-rescue protection |
| nativeEntities / excludedEntities | empty lists | Extra exact registry IDs |
| bossEntities | five IDs listed above | Encounter-ID mapping |

Unknown claim systems can register `DemonProtection.Rule`; denials prevent relevant actions. Open Parties and Claims is detected through its public server API. API failure disables affected actions rather than silently bypassing protection. Fabric's block-break veto event is consulted for destructive domain changes. This cannot automatically implement every third-party region plugin's private policy.

## Multiplayer, persistence and verification

All progression, cooldowns, curse ownership, army orders and transfers are validated on the server. Custom client messages are rate-limited. Client HUD timers are snapshots of server counters, not a separate authoritative clock. Missing optional bosses do not crash core Demon initialization. Server code has no direct client-renderer initialization.

Automated tests cover full Cinder cycles, decay thresholds/recovery/overflow, exact throne geometry/rotations, sphere ordering, 10,000-entry queue fairness, ownership/curse/birth persistence, queued-burst restart data and bounded config values. Runtime results and limitations are documented separately in `TEST_RESULTS.md`; compilation is not treated as in-game verification.

Operator test harness: only register it with `-Dsmooth_classes.demonTest=true` in a disposable world. `/smoothclasses_demon_test <scenario> <player>` mutates fixtures for the explicitly selected lab player. Do not enable this property on a public or survival server. Ordinary gameplay commands are `/smoothclasses demon status`, `/smoothclasses demon path world_ender` and `/smoothclasses demon order <order>`.

Suggested manual acceptance route: select Demon → unlock Cinder in fire → choose World-Ender → loot Fortress/Bastion → earn Hot Tourist Destinations → defeat the five real required bosses → exercise respect and each army order → build/bind the approved throne → test Last Tribute/Thronebound/portal reinforcement. Repeat with two players, hostile teams, claimed chunks, logout/restart, unloaded troops and deliberately broken portals/thrones. Check both client and server logs. Use copies of old saves for migration checks.
