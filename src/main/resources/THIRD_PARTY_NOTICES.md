# Third-party notices

## Ice and Fire Community Edition

Portions of the Rider mount visuals are adapted from **Ice and Fire Community Edition** by IAFEnvoy and contributors.

Upstream source: https://github.com/IAFEnvoy/IceAndFire-CE
Upstream license: GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later).

Smooth Classes includes/adapts the following Rider-only material:

- White Hippogryph texture and model geometry/UV layout.
- Dread Knight Horse texture used by the Rider Dread Steed.

Smooth Classes does **not** bundle or require the Ice and Fire mod at runtime. The Rider entity behavior, registration, networking, progression integration, mount evolution, and simplified flight controls are implemented inside Smooth Classes. The Hippogryph model was ported to Minecraft's vanilla model API and its runtime animation was reimplemented for Smooth Classes.

The LGPL-3.0 and GPL-3.0 license texts are included under `META-INF/licenses/`.

## Saints & Dragons — rider-flight controller reference

Smooth Classes Rider Hippogryph flight behavior adapts source-code concepts from **Saints & Dragons** by Leon Saint, including smooth rider-yaw following, throttle/boost state, pitch-directed steering, dive momentum, banking, and speed-sensitive FOV feedback. The implementation was rewritten for Smooth Classes / Yarn / Fabric 1.20.1; no Saints & Dragons art, models, animations, textures, audio, or other non-code assets are included.

- Upstream project: `LilRicefield/saints-dragons`
- Upstream source code license: MIT License
- Copyright (c) 2025 Leon Saint
- Reference revision inspected for this adaptation: `312846ff5aedfd6270778682dacf680c6df45d35`
- Relevant upstream source areas: `DragonRiderFlightController`, `DragonRiderFlightSettings`, `DragonRiderControllerHelper`, `DragonFlightVisuals`, `DragonRideInputHandler`, and `DragonFovHelper`.

The MIT license notice for the adapted code reference is included at `META-INF/licenses/saints-dragons-MIT.txt`.

