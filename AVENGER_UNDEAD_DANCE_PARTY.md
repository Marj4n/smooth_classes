# Avenger — Undead Jukebox Dance Party

Development build for **Minecraft Java 1.20.1 / Fabric**. Forbidden Apple Tree and Demon Birth are included in the same JAR. No CurseForge release, GitHub push or public version bump was performed. User visual/public-release approval is not inferred from test passes.

## Play it

1. Use Avenger's existing Death List / From Corpses We Arise summoning system.
2. Place a vanilla Jukebox and play a Music Disc within **16 blocks** of an eligible idle summon.
3. The original vanilla mob dances. Combat, damage, active navigation and attack orders interrupt it; an eligible idle summon can resume while the record is still playing.

There is no new skill, keybind, item, guitar, potion use, damage, stat bonus, XP bonus or combat advantage. No mob is forced to walk toward a Jukebox. The actual entity's AI, navigation, position, NoAI, command tags, owner UUID and faction are never changed by the production dance controller.

## Ownership and old saves

Eligibility requires the existing `AvengerReworkRuntime.isAvengerSummon` / owner helper **and** a server-issued per-entity binding. A successful Death List transaction issues that proof only after spawn, soul, charge and material costs succeed. It records the entity UUID, exact vanilla registry type and owner UUID in the overworld PersistentState `smooth_classes_avenger_dance_bindings`. The entity UUID comes from its normal entity NBT; command tags alone cannot create proof.

The controller also checks one matching owner tag, supported vanilla recipe/profile, awakened owner's Death List ledger/species history, actual tameable/horse owner where applicable, alive state, and the owner's verified Avenger role. The last verified owner role is saved so disconnecting cannot re-enable summons belonging to an owner who changed class. Ordinary chunk unload preserves proof; true destruction/discard removes it. Server stop clears volatile playback/candidate caches.

**Existing summons from before this feature need to be recalled and summoned again.** Their commands/ownership still work, but they have no issued proof. Automatic legacy migration was tested and rejected: existing Avenger maintenance itself adds persistence and hidden names to tagged mobs, so those fields cannot independently prove a historical Death List transaction. The delayed forged-tag regression caught this; the unsafe migration was removed. Recall and resummon were tested through the real existing ritual/cost path.

Custom Wraith, Dreadglare, Greater Dreadglare, modded registry IDs, indirectly owned Vex and unrelated pets/wild mobs receive no dance overlay. A Vex directly created by a genuine Death List transaction can use the flying adapter; indirect evoker children have no direct issued proof.

## Jukebox detection and synchronization

The implementation reads the actual vanilla Jukebox playing flag, inventory disc, `tickCount`, `recordStartTick` and `MusicDiscItem.getSongLengthInTicks()`. Start/stop/tick hooks maintain a per-world playback cache with spatial chunk buckets. Each candidate queries only the nearby 3×3 chunk buckets, staggered every five ticks. Immediate per-tick interruption checks handle combat/hurt/navigation/range; periodic loaded-chunk verification removes invalid/destroyed/unloaded/ended records without force-loading chunks.

Nearest Jukebox wins; exact distance ties use the packed BlockPos as a deterministic tie-breaker. Ownership is never merged between armies. Activation/stop/tracking snapshots send entity ID **plus UUID**, dimension, profile, playback origin/end and disc ID. Clients clear state on world/disconnect/expiry and discard unloaded/reused entity IDs. There is no client-to-server dance activation and no per-animation-frame network traffic.

Timing uses server world ticks, disc playback origin, client tick interpolation and a shared 40-tick loop. **120 BPM is a declared stable fallback**, not a claimed measured tempo of any disc: the verified 1.20.1 MusicDiscItem API has duration but no BPM metadata. Native multiplayer snapshots recorded the same origin and phase on both actual clients.

## Vanilla rendering and choreography

Vanilla `setAngles` runs first. A client LivingEntityRenderer injection applies authored ModelPart deltas afterward. All touched pitch/yaw/roll and pivot fields are snapshotted and restored after that entity render; root presentation uses a balanced matrix push/pop. Default transforms and texture resources are untouched. Weak model keys avoid retaining retired renderer models across reloads. Biped hat sibling tracks follow head motion; held-item/armor features continue using the real vanilla model parts. The existing soul emergence/recall presentation takes priority.

| Signature | Choreography |
|---|---|
| Villager | Folded-arm awkward sway, nod, stiff torso twist |
| Skeleton | Alternating arms/shoulders, head movement and light shuffle |
| Wither Skeleton | Headbang, forward torso pulse and imaginary-guitar arm gesture; no guitar item |
| Zombie | Original side shuffle layered onto vanilla forward-arm pose |
| Enderman | Stepped robotic head/long-arm groove, bounded optional portal particles |
| Creeper | Four-leg alternation, bounce, turn and cosmetic overlay flash; no fuse or ignition writes |
| Spider | All eight leg parts alternate, with body bob and turn |
| Drowned | Swimming-like arms/rocking; submerged amplitude variant, no navigation writes |
| Zombified Piglin | Nodding and shoulder/arm groove; original held Golden Sword follows its arm |
| Witch | Folded-arm potion-like gesture, head/hat sway, bounded cosmetic particles; no real potion use |

Particle budget is capped at eight spawned dance particles per client update interval, with Enderman particles less frequent. No texture is reuploaded per frame, no replacement entity/model is spawned, and no GeckoLib dependency was added for this feature.

**74 of the 78 current vanilla Death List recipes are allowlisted.** Biped/villager-compatible families reuse matching signatures. Quadruped, flying, aquatic and other ground models have separate anatomy-safe profiles; unusual AnimalModel families use whole-body sway rather than applying incompatible biped limbs. The full per-entity audit is `qa/vanilla-recipe-adapters.json`. Ten signatures and nine additional types were actually exercised: Cow, Cat, Horse, Bee, Ghast, Cod, Slime, Iron Golem and Evoker. The other allowed variants share the audited adapters but were not each runtime-tested individually.

Explicit exclusions:

| Entity | Reason |
|---|---|
| Parrot | Keep existing vanilla dance as its only choreography; no doubled overlay |
| Shulker | Anchored lid/face/peek adapter not verified |
| Warden | Special pose/brain/sonic-boom adapter not verified |
| Wither | Multi-head boss targeting and spawn/charged renderer adapter not verified |

Vanilla parrots/allays can retain their own pre-existing native dance behavior, including wild ones. “No wild mob dancing” here means **no wild mob receives this feature's overlay**; ordinary vanilla behavior is preserved.

## Renas and Jason asset evidence

Renas Blockbench MCP actually authored **14 clips** on isolated working reference rigs derived from vanilla ModelPart pivots and logical cuboid bounds. Editable `.bbmodel`, Bedrock authoring `.animation.json`, extracted Java-runtime track JSON and six-angle previews are included. These are **motion-authoring references**, not game replacement models: solid-color previews omit vanilla UV/alpha/deformation details, and logical bounds do not reproduce every rendered dilation. Native Minecraft screenshots are the authority for the real textured result.

Saved Renas keyframes are extracted with explicit Y-up/Y-down and degree-axis conversion into Java ModelPart tracks. Bedrock output is not assumed to run directly in Fabric. Runtime Java adds the tracks to the actual state-dependent vanilla base pose.

Jason ran geometry/self-tests and dense animation checks for every final working rig: **14 models, 0 geometry errors, 47 geometry warnings; 0 animation errors/warnings**. Every warning is retained by target name in `qa/qa-final-summary.json` and the complete per-profile reports. They concern reference-layer head/hat/body/robe overlaps, original joints/spider leg bases, thin fish appendages, Creeper edge contacts and naturally detached Ghast tentacles; they were not suppressed or turned into a claim of a warning-free mesh. Six actual headless views per profile were generated and reviewed. Rendering/geometry gates are separate from user artistic approval.

Runner-recorded authoring benchmark, including revisions/failed attempts:

| Stage | Attempted MCP tools | Local operation time |
|---|---:|---:|
| Renas geometry/reference setup | 126 | 3.000 s |
| Renas animation/export | 211 | 4.044 s |
| Final Jason validation batch | 42 | 4.496 s |
| Jason final six-angle renders | 14 | 60.417 s |

These times are local runner operations, **not total engineering/review/build/runtime-testing time**. Controller tool discovery, native inspection/image-view calls and earlier overwritten Jason runs are outside those scoped totals. Full runner logs are included. AI token telemetry is unavailable (`null`); tool counts/time are not token measurements. The older dummy benchmark is not an equivalent unbatched baseline, so no invented token-saving percentage is reported.

## Verification

- Fabric build and Forge compile/build succeeded offline. Actual runtime acceptance targets **Fabric only**, using two real Minecraft 1.20.1 clients on localhost.
- **69 unit tests, 0 failures, 0 errors, 0 skipped**. This includes binding persistence/owner isolation/role state, explicit profile exclusions, track interpolation, and existing Demon/Vampire suites.
- No disc → none; valid disc → ten signatures; wild and tag-only forgeries → none, including after existing maintenance. Owned and forged Skeletons used the same real model instance while only the owned one received an applied overlay.
- Both clients saw all ten applied profiles with the same start/phase. A separate final-JAR late-join test activated the cached party before any player joined, then verified both clients received all ten applied profiles without restarting/reinserting the record. An ordinary non-Avenger observer still saw another owner's party; two Avenger armies remained separate. Owner role changes/logout did not re-enable invalid ownership.
- Inside/outside radius, combat/damage threat interruption and resumption, real Attack order priority, Follow/Guard/Stay/recall behavior, active Follow navigation interruption, multiple Jukebox selection, eject and destruction were checked.
- Actual graceful save/server restart/reconnect preserved UUID/owner/type proof and STAY/NoAI. Actual unload/reload outside spawn chunks removed playback and restored eligible proof on return; `forceload query` reported none.
- Drowned submerged pose and nine anatomy fallback types were actually rendered. Creeper NBT remained unignited; the feature never changes fuse/explosion state.
- A Cat disc ended naturally after **3,700 game ticks** with 300 summons; actual Jukebox NBT read `IsPlaying: 0b`, all owners remained unchanged, and both clients cleared dance state. No clock fast-forward was used.
- The combined JAR was tested with a fresh actual Demon player in a separate copy of the passing Forbidden Tree world. The full cutscene completed; 8,271-cell source comparison (allowing natural grass aging)/tree palette checks, Born UUID, inventory/XP/identity, collision, single-tree placement count, no replay, restored control/protection and zero leaked chunk leases passed. Original Demon test state was preserved.
- The 31 frozen Demon source/resource files outside the extended mixin registration are byte-identical, and approved Appleborn assets were not edited. Existing Vampire unit tests pass. A new Vampire church placement and every Vampire ability were not re-tested in this dance run; the earlier conservative church-site limitation remains in the Demon report.

Fixed failures are not hidden: the initial unregistered Brain attack-memory crash was corrected with a presence check; unsafe legacy inference was removed after a forged-tag regression; the local RCON framing and disposable cleanup/camera harness issues were corrected before acceptance. Failure evidence is retained separately from final passes.

## Performance measurements

Machine: **12th Gen Intel(R) Core(TM) i3-12100F**, **NVIDIA GeForce GTX 1660 Ti**, approximately 15.8 GiB RAM. Two actual 1280×900 clients on the same PC, 120-FPS cap, VSync off; server heap 1,536 MiB, each client 2,048 MiB. Equal mixtures of the ten signature mobs were spawned through the real summon path with fixture-seeded resources, then left stationary using the existing STAY/NoAI command path; this is not a benchmark of 300 active combat AIs. Each record/no-record window was 15 seconds after warm-up. No headless authoring/QA workload ran during the clean measurements.

| Minions | Idle FPS A / B | Dance FPS A / B | Dance 1% low A / B | Whole-server mean tick ms | Dance callback ending tick ms | Steady custom packets |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 117.9 / 117.7 | 117.8 / 117.8 | 96.5 / 89.5 | 1.70 | 0.027 | 0 |
| 50 | 117.8 / 117.5 | 118.2 / 118.1 | 91.3 / 83.0 | 2.47 | 0.049 | 0 |
| 100 | 117.6 / 117.9 | 117.8 / 118.0 | 83.6 / 84.8 | 3.62 | 0.293 | 0 |
| 300 | 98.6 / 100.2 | 94.5 / 98.0 | 57.7 / 61.3 | 5.89 | 0.231 | 0 |

FPS/1% lows come from actual GameRenderer frame intervals; the low is the reciprocal of the mean of the slowest 1% frame intervals. Client overlay CPU measures **apply/snapshot only**, not vanilla rendering or the separate restore callback. Server callback is the ending sampled tick, not a percentile; whole-server tick mean uses vanilla's rolling tick history. Per-client GC collection time is recorded in the JSON. Allocation-byte telemetry and server GC attribution were unavailable and remain null/unreported. Network counters cover this feature's payloads, not total vanilla/TCP traffic. Zero steady packets does not mean zero activation/termination/tracking packets.

These are short observational windows with two clients sharing one CPU/GPU, not a universal FPS/TPS guarantee. The same entities and positions provide a real no-record baseline; no animation was removed to inflate results. Interrupted/QA-overlapped preliminary data is kept separately and excluded from this table.

## Deliverables and exact paths

- Repository: `D:\Development\minecraft\Smooth Classes`
- Common server implementation: `common/src/main/java/org/marj4n/smooth_classes/content/avenger/dance/`
- Client adapters/track sampling: `common/src/client/java/org/marj4n/smooth_classes/client/avenger/dance/`
- Vanilla render/Jukebox mixins: the `AvengerDance*` / `AvengerJukebox*` classes in the common/client mixin folders.
- Runtime clips: `common/src/main/resources/assets/smooth_classes/avenger_dance/`
- Working files/evidence: `C:\Users\kevin\OneDrive\Documents\ChatGPT\Minecraft Project\avenger-dance`
- Combined Fabric JAR: `D:\Development\minecraft\Smooth Classes\fabric\build\libs\smooth_classes-1.0.5-1.20.1-fabric.jar`
- JAR SHA-256: `65ea1022f7e1d0744c8e1615b41909704628258b66e7f677e59eb5ea7075b6c9`
- Native close-ups/recordings: `previews/` in the delivery bundle; originals remain under `avenger-dance/runtime/client-a/screenshots/` and `client-b/screenshots/`.
- Original Demon feature documentation and provenance: `FORBIDDEN_APPLE_TREE_BIRTH.md` and accompanying Demon assets/evidence in the bundle.

Use one Smooth Classes JAR in a development Fabric profile with its existing required dependencies. Keep the public version unchanged (`1.0.5-1.20.1`). Do not treat the reference `.bbmodel`/Bedrock exports as installable replacement mob models.

Suggested commit message:

```text
feat: add Forbidden Apple birth and Avenger jukebox dance party
```

## Known limits / release status

Development acceptance is distinct from a public release. Old summons require recall/resummon; four explicit vanilla exclusions remain; 55 of the allowlisted variants were not individually runtime-tested; arbitrary third-party replacement renderers/resource-pack combinations and resource-reload stress are not certified. Server-side allocations/GC attribution and full end-to-end network profiling were unavailable. One disposable peer profile stalled before login and later produced a native GLFW access violation; the raw log is preserved locally. A clean temporary peer profile with the same identity completed the tests, with raw mouse input disabled in that test profile only. The native root cause is not conclusively diagnosed and no system/driver/MCP settings were changed. No in-game test result is invented for those gaps. User visual approval remains required before public release; nothing has been pushed or uploaded.
