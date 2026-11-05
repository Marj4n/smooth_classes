# Smooth Classes: ownership scope and third-party provenance

The permission-required notice in LICENSE.txt applies only to material whose rights are held by Marj4n. It does not claim ownership of Minecraft, loaders, libraries, other mods, or their names and trademarks. External components retain their own terms.

## Current original visual replacements

The Storm Herald mount geometry/material and Siren tail geometry/material were independently authored for Smooth Classes through the retained native Renas workflow. Dread Steed base/emissive materials were independently painted on the unchanged external vanilla skeleton-horse UV/model contract. The vanilla geometry is not claimed as an original Smooth Classes model. The three revised equipped-animation clips retain their source and export mapping; other clips and gameplay callers remain unchanged.

Exact source/export hashes and the separately recorded visual adapters are included in assets/smooth_classes/provenance. These records describe the selected artwork and technical coverage; they are not a transfer of another party's rights or a guarantee of exclusive rights in AI-assisted material.

## Preserved historical and code provenance

Earlier internal mount geometry/UV and Dread Steed textures were attributed to Ice and Fire Community Edition. The former Siren tail model/material referenced Mermod. Four exact historical texture paths are excluded from distribution JARs, while original files and their historical credits are retained privately. Those historical assets are not relabeled as original work. Historical entity comments remain provenance records for earlier versions, not statements that the current replacement textures are upstream artwork.

The tail's existing functional preparation/wave/visibility contract was preserved separately from the replacement artwork. Rider fluid-surface behavior previously referenced Eternal Nether / Bygone Nether. The current world binding and isolated surface-motion policy were reimplemented for Smooth Classes against its frozen behavior contract; 937,280 observable test-double steps matched the archived implementation. Historical source exposure and attribution remain recorded; this work is not a clean-room provenance or exclusive-rights determination. Replacing visual geometry/materials does not itself settle revision-specific code obligations or establish exclusive ownership of these retained functional portions. Relevant upstream projects include https://github.com/ThatPreston/Mermod, https://github.com/IAFEnvoy/IceAndFire-CE, https://github.com/Fuzss/eternal-nether and https://github.com/izofar/bygone-nether.

Existing third-party texts under META-INF/licenses are retained, including GPL-3.0, LGPL-3.0 and the Saints and Dragons MIT notice. Retaining a notice alone is not a certification that all distribution obligations have been satisfied. Historical raw-receipt gaps and code/dependency permissions remain separate review items. Nothing here licenses external dependencies under Marj4n's notice or grants additional third-party distribution rights. Publishing this repository does not expand those rights.

## Flight and tail code review, 6 November 2026

The Rider flight solver was reimplemented as an explicit-input state machine for Smooth Classes. Its previous Saints & Dragons-derived controller is retained privately as historical evidence. The frozen previous and replacement calculations match bit-for-bit across 71,752 test cases/sequence steps using Minecraft's actual vector/math API. This establishes scoped calculation equivalence, not exclusive ownership or strict clean-room provenance. The Leon Saint MIT notice remains included; no prior valid rights are revoked.

The current Siren tail uses the independently authored replacement native geometry and its own dry/wet/swimming pose paths. Source review against Mermod 1.20.1 commit cf987b684a54bf67fc42e3dab13351e91b9add29 records different branching, amplitudes, phases, rendering and no TailStyle or BiConsumer callback implementation. Shared hierarchical part names and a sine-wave formula remain functional references. This comparison is not a legal determination or a retroactive removal of historical attribution.
