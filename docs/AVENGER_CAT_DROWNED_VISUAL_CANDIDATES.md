# Avenger Cat / Drowned — visual candidates

Status: tested development candidates, **USER VISUAL APPROVAL PENDING**. No CurseForge release, GitHub push or public version bump.

## Current behavior

Cat stands on the two original hind legs. Right forepaw stays at the mouth; only left forepaw waves. The user-requested smoother cyclic curves replace abrupt beat holds; accepted leg key poses remain within 0.0051 degrees. Original Cat model data/textures, AI, owner commands and collision are preserved. A single temporary cosmetic forepaw elbow deforms original faces/UVs; there is no replacement entity/model/renderer/texture. Live owner head-looking is respected by the mouth solve.

Drowned has a new full-body land phrase and a separate smoothly blended underwater variant. The other26 original signature clips and all Demon resources/sources remain byte-identical to the captured pre-task source baseline.

## Review the actual native animation

- [Cat front](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/cat-front-r08.mp4>) / [GIF](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/cat-front-r08.gif>)
- [Cat side](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/cat-side-r09.mp4>) / [GIF](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/cat-side-r09.gif>)
- [Cat before/after smoothing](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/cat-before-after-smoothing.mp4>)
- [Original Drowned V2 —20fps](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/drowned-original-v2-20fps.mp4>)
- [Revised Drowned front](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/drowned-front-r08.mp4>) / [GIF](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/drowned-front-r08.gif>)
- [Revised Drowned side](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/drowned-side-r09.mp4>)
- [Drowned underwater](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/drowned-underwater-r08.mp4>)
- [Drowned phase-aligned before/after](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/drowned-before-after.mp4>)
- [Observed reference frames beside native candidates](<C:/Users/kevin/OneDrive/Documents/ChatGPT/Minecraft Project/avenger-dance-v3-cat-drowned/gallery/reference-motion-comparison.png>)

Standalone MP4s preserve actual GameRenderer frame timestamps at full speed, approximately 51.7–59.6 measured source frames/second across these takes. They are near60FPS recordings with real cadence variations, not fabricated stable60FPS. GIFs reduce cadence to20FPS for size. Before/after comparison files have a60FPS carrier and hold original lower-cadence source frames; they do not interpolate movement. Old Drowned remains its original20FPS source. Side-view media was retaken as r09 using the unchanged final JAR because a wild Slime obstructed the r08 camera. Silent recordings. Raw frame/timing evidence and exact crops remain under `runtime/client-b/` and `gallery/*-capture.json`.

## Actual validation

- 84 unit tests; 0 failures, 0 errors, 0 skipped. Build: `compile-r07-final-contact.log`.
- Real Renas Blockbench authoring; actual Jason stdio QA using this workspace root. Geometry:0 errors/21 warnings; animation:0 errors/0 warnings. No QA suppression.
- Saved-key Cat mouth error 0.000035 model units; independent live owner-looking check passed. Full reports retain tests and limits.
- Two actual Fabric1.20.1 clients: actual Death List bindings/UUIDs, shared record start/phase, rendering on both clients. Wild Cats/Drowned and forged scoreboard tags rejected. Same vanilla model instance used without pose leakage; saved original transforms restored with0 errors.
- Actual damage/combat interruption, idle resume,16-block radius, eject/destroy, Follow/Guard/STAY command path, two separate Avenger owners, unchanged owner UUIDs and stationary entity positions. Drowned land→water→land variant tested; navigation/collision are not rewritten. Saved world was reopened for matched benchmark runs; raw ownership state is recorded before cleanup.

## Matched local runtime benchmark

Equal Cat/Drowned mixes, original V2 versus final candidate, same disposable world/PC/settings, two actual clients,1280×900, cap120,3s warm-up and10s observation per idle/dancing case. No recording, Blockbench or media encoding during measurement. This is a short shared-PC comparison, not isolated hardware certification.

| Minions | V2 FPS A/B | Candidate FPS A/B | V2/candidate server tick ms | Candidate steady dance packets |
|---|---|---|---|---|
| 10 | 118.0 / 117.8 | 118.0 / 117.9 | 3.251 / 3.372 | 0 |
| 50 | 118.0 / 118.0 | 117.5 / 117.6 | 3.654 / 3.903 | 0 |
| 100 | 117.8 / 117.8 | 117.9 / 117.9 | 4.159 / 4.298 | 0 |

Raw results include idle baselines,1% low, overlay CPU time/render count, GC time and packet bytes. Allocation-byte telemetry was unavailable. Do not infer perfect performance or token savings. Pipeline recorded 119 artist script tool calls plus1 native status check and 36 Jason tool calls; operation timings/revisions are in `reports/PIPELINE_BENCHMARK.json`. AI token usage is unavailable/null. Different artistic workloads prevent a fair token-use comparison against all27 V2 signatures.

## Warnings, failed iterations and limits

- **cat**: body_0_cube_0 and body_1_cube_0 (different groups) interpenetrate by 1.9 units. body_0_cube_0 and body_2_cube_0 (different groups) interpenetrate by 1.9 units. body_0_cube_0 and body_3_cube_0 (different groups) interpenetrate by 1.8 units. body_0_cube_0 and body_4_cube_0 (different groups) interpenetrate by 1.8 units. body_0_cube_0 and body_5_cube_0 (different groups) interpenetrate by 1 units. body_0_cube_0 and right_forepaw_lower (different groups) interpenetrate by 0.9 units. body_5_cube_0 and body_6_cube_0 (different groups) interpenetrate by 1 units.
- **drowned**: head_cube_0 and hat_cube_0 (different groups) interpenetrate by 8 units. head_cube_0 and right_arm_cube_0 (different groups) interpenetrate by 0.62 units. head_cube_0 and left_arm_cube_0 (different groups) interpenetrate by 0.62 units. hat_cube_0 and right_arm_cube_0 (different groups) interpenetrate by 0.62 units. hat_cube_0 and left_arm_cube_0 (different groups) interpenetrate by 0.62 units. body_cube_0 and right_arm_cube_0 (different groups) interpenetrate by 1.03 units. body_cube_0 and left_arm_cube_0 (different groups) interpenetrate by 1.03 units.
- **drowned_water**: head_cube_0 and hat_cube_0 (different groups) interpenetrate by 8 units. head_cube_0 and right_arm_cube_0 (different groups) interpenetrate by 0.62 units. head_cube_0 and left_arm_cube_0 (different groups) interpenetrate by 0.62 units. hat_cube_0 and right_arm_cube_0 (different groups) interpenetrate by 0.62 units. hat_cube_0 and left_arm_cube_0 (different groups) interpenetrate by 0.62 units. body_cube_0 and right_arm_cube_0 (different groups) interpenetrate by 1.03 units. body_cube_0 and left_arm_cube_0 (different groups) interpenetrate by 1.03 units.

These are retained rest-pose mesh/junction intersections, including the original Drowned hat shell and the authoring forepaw subdivision. Jason does not directly validate the Java render deformation; native captures verify its appearance. Untextured authoring replicas intentionally use existing vanilla UVs only in-game; no new PNG atlas is supplied.

Earlier quadruped Cat and alternating-hand candidates are rejected/preserved. Earlier pre-login timeouts, camera/probe setup misses, short damage-window polling and water-scenario spawn-placement failures remain in their original logs. The final r08 run passes after correcting fixture observation/cleanup; no production combat/spawn policy was weakened. Complete Demon/Vampire gameplay suites were not rerun: their protected source/resources are unchanged, and prior V2 test evidence remains separate. No claim is made about every possible modded renderer or all sleeping/sitting variants.

## Exact deliverables

- Actual repository: `D:\Development\minecraft\Smooth Classes`.
- Development JAR: `C:\Users\kevin\OneDrive\Documents\ChatGPT\Minecraft Project\avenger-dance-v3-cat-drowned\delivery\smooth_classes-1.0.5-1.20.1-fabric-cat-drowned-dev.jar`; SHA256 `bb73a6287c759b4d4768bbba4deec2844dffc526584dd69768295aa728291029`. Public internal version remains1.0.5-1.20.1.
- Editable sources: `assets/cat/cat.bbmodel`, `assets/drowned/drowned.bbmodel`, `assets/drowned_water/drowned_water.bbmodel`.
- Animation exports and Java tracks: same directories, `.animation.json` and `.runtime.json`.
- Full native acceptance: `runtime-evidence-r08/PASS.json`; performance: `performance/SUMMARY.json`; QA: each asset's `bbmodel_validate*.json`; source preservation: `reports/source-preservation.json`.
- Original references and interpretation corrections: `REFERENCE_MOTION_STUDY.md`, `REFERENCE_UPRIGHT_CORRECTION.md`, `CURRENT_DESIGN.md`, `SMOOTHING_REVISION.md`.
- Suggested commit: `Refine Avenger Cat and Drowned dances with synchronized vanilla-model animation`.

Stop here for user visual review. These are not automatically approved or released.
