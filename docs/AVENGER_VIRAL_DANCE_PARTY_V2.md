# Smooth Classes — Avenger Viral Dance Party V2

Development review build, Minecraft Java 1.20.1 / Fabric. Public metadata remains **1.0.5-1.20.1**. **USER VISUAL APPROVAL PENDING.** No GitHub push or CurseForge release performed.

## IMPLEMENTED

27 original hand-authored anatomy-specific adaptations, exported through the real Renas Blockbench MCP and extracted into Java ModelPart tracks. Warden, Wither and Shulker now have dedicated mappings. Vanilla meshes, textures, renderers and held items remain in use; editable Blockbench files are logical authoring references, not replacement models. UV/texturing stage is N/A because no vanilla texture is replaced.

Only authenticated vanilla Avenger Death List bound summons dance: existing server-issued binding, legitimate summon/cost path, entity type, owner UUID and ownership ledger checks remain. Wild mobs and forged tags fail closed. Jukebox playback/radius16 and shared record timing are preserved. No per-frame animation packets or world scan per minion. Steady-state benchmark packet counts exclude activation/stop bursts during setup and warmup. Server activation packets use an internal V2 protocol identifier; both server and observing clients need this build.

Vanilla poses run first. Each dance applies sampled transforms then restores pivots, rotations, scales and visibility after rendering, including shared renderer instances and original secondary armor/energy/clothing layers. Root-coupled body leans preserve sibling limb attachment; no changing entity collision/position for cosmetic motion. Normal music stop blends out; combat/special-state stops are immediate. Declared 120 BPM is a stable fallback, **not measured disc BPM**.

Existing Guard/Stay/Follow/Attack and summon costs/ownership are preserved. Special priority reads Warden tracked poses/anger (persistent vanilla AnimationState flags deliberately excluded), Wither invulnerability/live head targets/actual projectile attacks, potion drinking and spellcasting. No stat bonuses, AI replacement, new items, external music or added GeckoLib dependency.

| Vanilla mob | Creative movement reference | Loop |
|---|---|---|
| warden | Kicau Mania | 8s |
| wither_skeleton | Orange Justice | 4s |
| enderman | The Robot | 4s |
| spider | Crab Rave | 4s |
| wither | Harlem Shake | 8s |
| shulker | Coffin Dance | 8s |
| villager | Macarena | 8s |
| skeleton | Floss | 4s |
| zombie | Default Dance | 4s |
| creeper | Hype Bounce | 4s |
| drowned | Scuba Dance | 4s |
| zombified_piglin | Get Griddy | 4s |
| witch | Wednesday Dance | 8s |
| piglin | Gangnam Style | 4s |
| piglin_brute | Take the L | 4s |
| pillager | Electro Shuffle | 4s |
| vindicator | Hype | 4s |
| evoker | Rizz Dance | 8s |
| iron_golem | Boogie Down | 4s |
| blaze | Phonk Spin | 4s |
| ghast | Flying Groove | 4s |
| allay | Best Mates | 4s |
| slime | Jelly Bounce | 4s |
| horse | Ride the Pony | 4s |
| chicken | Chicken Dance | 4s |
| cow | Get Sturdy | 4s |
| ravager | Heavy Stomp | 4s |

Boss testing caught a real Warden resume defect: vanilla AnimationState.isRunning can remain true indefinitely (and server states are not advanced by a renderer). Candidate10 uses the current tracked pose plus existing actual combat/anger gates; no vanilla animation state is stopped or modified by this fix. Evidence: `vanilla-audit/WardenEntity.bytecode.txt` and boss interrupt/resume rerun.

A separate existing Warden bug was exposed by actual combat testing: Attack/Guard/Follow repeatedly called vanilla updateAttackTarget, which resets Sonic Boom cooldown to200ticks. Final build now calls it only when the actual target changes, while preserving ownership, anger and command selection. Actual fresh-Warden ranged combat and return-to-dance evidence: `warden-sonic-regression/PASS.json`. The original no-damage32s observation is retained rather than relabelled as a pass. Runtime diagnostics identified a NoAI STAY freeze during SNIFFING. Final13 waits for the verified finite vanilla sniff4.16s/roar4.2s clip plus one game tick, using a read-only animation clock; it never rewrites pose or AI. Normal navigation/attack checks remain unchanged. Dig/emerge and unknown clock states fail closed. Live targets, brain targets, owner orders, anger and damage still take priority. Final13 core regression rechecks genuine NoAI target interruption and wild/forged exclusion.

There are **27 signatures**, not just ten. Anatomy-compatible older generic flying, aquatic, quadruped and ground profiles remain; recipe routing covers 77 of 78 vanilla entries. Parrot is explicitly excluded from the custom overlay to preserve its native jukebox behavior. Unknown/modded entities fail closed. A fallback root-only dance is deliberately less elaborate than a signature.

## TESTED

- Build succeeded: `compile-13-tests.log`; **80 tests, 0 failures/errors/skips** (`unit-summary-final.json`).
- Actual dedicated Minecraft server and two actual clients on this Windows PC; no simulated client claim. All27 full-loop captures verify render eligibility/application, exact shared record start, phase against each client's game tick, per-capture clip hashes and JAR hash. The27-portrait suite used candidate09; final candidate13 fixes Warden's persistent-state gate and three repeated target-refresh calls. Warden gets a final native recapture and actual Sonic Boom test; all authored tracks/renderers remain byte-identical. Final13 has its own full V2 benchmark and combined Demon regression. Each evidence file retains its actual binary hash. Native evidence: `NATIVE_ALL27_MANIFEST.json` plus per-frame diagnostics. Wither all three heads have distinct time-varying rotations.
- Final13 core runtime report present: **True**. Earlier candidate09 extended runtime report present: **True** (ownership/persistence/command code is unchanged except the documented Warden target guards). Ownership isolation, ordinary observer, wild/tag-forged rejection, shared-model pose isolation, radius, target interruption/resume, record ejection/destruction, multiple boxes, two owners, owner class/offline checks, legacy proof fail-closed, real Recall/Follow/Guard/Stay/Attack, submerged Drowned, save/restart/reconnect and actual chunk unload/reload were exercised. No force-loaded chunks.
- Additional boss/late-join/group suite present: **True**. See individual results; synthetic tracked-pose gates are explicitly labelled, separate from real AI observations.
- Natural Warden Sonic Boom observed: **True**. Natural Shulker projectile observed: **True**; damage-driven teleport observed: **True**. A pose setter alone is not evidence of natural AI behavior.
- Demon Birth fresh-player combined-JAR regression report present: **True**. See `demon-regression-evidence/PASS.json` for actual cutscene/anchor/leases/protection checks. All 1,505 baseline common/src files were hash-audited; changed/added files are scoped to dance implementation/tests/mixin registration and three Warden target-identity guards in the existing Avenger runtime. Demon, Vampire and unrelated source/assets remain unchanged (`source-preservation-final.json`).

## PERFORMANCE COMPARISON

Same PC, Java17, two clients concurrently, 1280×900, FPS cap120, VSync off, same flat-world settings/composition/positions. Each count is an equal mix of the ten original signatures. Each idle/dance window uses3s warmup +15s observation. V1 is the exact frozen original JAR, not V2 with reduced motion. No image encoding/Blockbench jobs should run during measurement. Values are short observational samples; shared GPU/CPU, JVM warmup, window scheduling and background desktop activity limit generalization.

| Minions | State | V1 FPS A/B | V1 1% low A/B | V1 tick ms | V1 packets | V2 FPS A/B | V2 1% low A/B | V2 tick ms | V2 packets |
|---:|---|---|---|---:|---:|---|---|---:|---:|
| 10 | idle | 117.8 / 117.9 | 84.2 / 82.2 | 5.14 | 0 | 117.8 / 118.1 | 72.4 / 71.7 | 3.16 | 0 |
| 10 | dance | 117.5 / 117.5 | 85.8 / 85.7 | 3.92 | 0 | 118.0 / 118.1 | 76.3 / 73.7 | 2.73 | 0 |
| 50 | idle | 117.5 / 117.6 | 81.6 / 76.4 | 4.52 | 0 | 117.9 / 117.8 | 74.0 / 68.0 | 3.49 | 0 |
| 50 | dance | 117.9 / 118.1 | 85.6 / 85.5 | 4.45 | 0 | 117.6 / 117.5 | 69.8 / 70.0 | 3.64 | 0 |
| 100 | idle | 118.1 / 118.3 | 82.2 / 78.7 | 4.92 | 0 | 117.3 / 117.4 | 67.6 / 66.4 | 3.97 | 0 |
| 100 | dance | 118.6 / 118.4 | 85.4 / 82.0 | 4.80 | 0 | 117.2 / 117.1 | 65.3 / 59.6 | 3.51 | 0 |
| 300 | idle | 83.0 / 87.9 | 53.7 / 56.0 | 8.51 | 0 | 76.7 / 79.0 | 40.7 / 38.4 | 9.01 | 0 |
| 300 | dance | 83.3 / 87.9 | 56.9 / 55.5 | 8.78 | 0 | 75.3 / 78.1 | 39.3 / 41.3 | 8.66 | 0 |

The V2 table uses the final13 rerun; earlier trials remain under `benchmark-v2/performance-candidate10/` and `performance-candidate11/`.

Full frame-time, render callback CPU, GC collection time and feature packet bytes are in each `benchmark-v*/performance/*.json`. Callback CPU is instrumented dance code cost, not whole GPU rendering cost. Allocation bytes are unavailable (`null`). The final V2 sample has lower300-mob FPS and lower1% lows than V1; even its idle baseline is lower, so this run cannot isolate animation cost from shared-PC variability. No isolated GPU certification or guarantee of300-mob performance is implied. Inspect raw percentile field names when consuming JSON; unavailable metrics must not be inferred as zero.

## ART / QA / BENCHMARK

Real Renas authoring: 472 recorded local tool calls /489 protocol requests across revisions; accumulated local script wall time15,639ms. These figures exclude exploratory tool discovery, earlier native calls, controller reasoning/image review and Minecraft integration time. Last full Jason batch:81 tools /83 protocol requests /6,002ms. Exact per-stage events and timing scope: `PIPELINE_BENCHMARK.json`, `mcp-events.jsonl`, `qa-events.jsonl`.

Jason ran its real CLI MCP route with a per-asset root after the native session root rejected this workspace. Geometry validation: **0 errors,193 warnings**; animation validation: **0 errors,3 warnings**. All warnings are retained verbatim per asset and in `qa-final-summary.json`. Logical untextured rigs include vanilla layered clothes/hat meshes, shell overlap and original joint intersections; some overlap self-tests cannot distinguish a new intersection from existing vanilla overlap. Blaze/Ghast/Wither have floating-rest-baseline ground warnings. This is not a warning-free certificate or a UV audit of Minecraft's copyrighted original atlas.

Geometry, texture N/A and animation checkpoints: `visual-checkpoints.json`. Native image-sequence review is distinct from colored Blockbench references. Rejected side-facing/obstructed takes are preserved. STAY intentionally watches its owner, so final portrait owner placement was moved behind the observer; AI was not changed for framing. Ghast's floating portrait fixture keeps all original tentacles visible above the studio floor.

Opt-in test commands require the explicit Java system property, operator permission and exact disposable world names; they are not enabled during ordinary play.

AI token usage: **unavailable / null**. Tool counts and elapsed times are not token usage. No percentage/token savings claim is made because the original V1 authoring task was not measured under equivalent conditions.

## NOT VERIFIED / KNOWN LIMITATIONS

- User artistic approval remains pending. Familiar dance names describe creative references; original Minecraft adaptations are not frame-exact reproductions of commercial motion assets.
- Full Vampire ability suite and every unrelated class ability were not manually replayed. Their source/assets were preserved and existing automated tests passed.
- Natural Warden sniff/roar/dig/emerge state coverage is limited to what the actual observation report records; deterministic tracked-state interruption tests are separate.
- Shulker natural bullets and damage-driven teleport were observed; every attached-face orientation and third-party renderer combination was not exhaustively tested. NoAI digging/emerging Warden poses remain blocked until normal AI is resumed, because they are not idle ambient poses.
- Baby/variant/third-party resource packs and every one of77 routed mob variants were not all visually reviewed. Unknown custom rigs fail closed. Native Parrot remains excluded.
- V1 comparison capture uses its original 4fps diagnostic recorder; V2 captures nominal20 game-tick fps. V1 frames may be held/repeated for video playback, never AI-interpolated. Comparison cameras are similar, not pixel-registered.
- Native MP4s/GIFs are silent. GIF palette is256 colors; high-quality silent H264 full-viewport videos are also included. Per-frame game ticks document actual capture cadence; no synthetic animation frames are used.

## DELIVERABLE PATHS

- Repository: `D:/Development/minecraft/Smooth Classes` (existing branch/work preserved).
- Development JAR: `delivery/smooth_classes-1.0.5-1.20.1-fabric-dance-v2-dev.jar`.
- JAR SHA-256: `80b2d6f8323d436f474e7fe5d19a4c194d95586e7b1f88de8c60fd9c45a4dba9`.
- Full source snapshot: `delivery/Smooth_Classes-Avenger_Dance_V2-Source.zip` (source/build scripts only; fresh-machine build not tested).
- Source delta: `delivery/source-delta/`; patch: `delivery/avenger-viral-dance-v2.patch`.
- Patch applied to a preserved baseline copy and every changed text verified; `delivery/SOURCE_MANIFEST.json`. Baseline is the in-progress pre-V2 source, not clean Git HEAD. Preserve its existing Demon work when applying. Git reports CRLF/whitespace warnings; byte-for-byte application verification succeeds (`package-source-final.log`).
- 27 editable models/animation exports/runtime tracks/QA/rig specs: `assets/<mob>/`.
- Actual vanilla baked rig exports: `original-rigs/`.
- 27 native GIFs +MP4s: `gallery-final/`; gallery index: `gallery-final/GALLERY.md` and `index.html`.
- Group/five-hero/boss/comparison outputs: `gallery-final/` (only claim outputs actually present).
- Tests: `runtime-evidence-final13/`, `extended-evidence/`, `boss-runtime/`, `warden-sonic-regression/`, `demon-regression-evidence/`; performance: `benchmark-v1/performance/`, `benchmark-v2/performance/`.

Suggested commit: `feat(avenger): add 27 dance signatures and preserve Warden attack cooldowns`
