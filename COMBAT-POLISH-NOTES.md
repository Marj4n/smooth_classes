# Smooth Classes — combat polish

Basis: Smooth Classes(20260930-075933).zip, uploaded 30 September 2026. This package reconstructs the requested batch from that source; it does not contain the inaccessible /mnt/data/sc_latest folder from the interrupted session.

## Implemented

- Arcane Slash: smaller, lower, offset charge particles to keep the aiming area clear.
- Assassin Preparation now displays Shadow Technique. Aim at a creature/block within 10 blocks; blink behind the creature or to safe space beside the block. Unsafe destinations fail without spending cooldown. Target creatures are blinded for 3 seconds, including server-side target suppression for mobs.
- A translucent black humanoid anchor remains for 10 seconds. Press the signature again to return. The next successful melee damage event gets +30% once. Cooldown starts at 30 seconds on return, timeout, death, or disconnect. Kills reset the skill and remove the shadow. Three existing branch upgrades each add 4 blocks of range, up to 22 blocks.
- Blood Rain: local rain/thunder gradients darken the camera's sky without changing global weather. Its footprint protects mobs from sunlight ignition. At 60+ Ascendancy, every fifth successful caster hit on an exposed, non-allied target in the storm triggers cosmetic lightning plus 1.5x lightning spell-power damage. Recursive lightning procs and caster-owned summons are excluded. Existing storm radius/duration/scaling from the input ZIP are preserved.
- Avenger: stop rewriting the Death List every maintenance tick; protect cursor/container transactions, slot position, and book drops. Existing written books migrate in place when Patchouli is installed. Soul records remain in PersistentState.
- Patchouli: optional normal-use guide with controls and all supported summon recipes. Sneak-use opens the personal ledger in Minecraft's book screen. The personal ledger is not a custom live Patchouli page. Patchouli itself is not bundled in this source ZIP.
- Shift+H while aiming at an owned summon unbinds that individual entity, clears ownership/taming/target state, and restores normal AI. It stays alive; no soul or ingredients are refunded. Other souls of the same species and lifetime kill records remain intact.
- Portal of Sovereignty: session cleanup now includes remaining projected daggers; portals also validate caster liveness and have a hard lifetime limit. Removed snowflake bursts at dagger emergence.
- Internal Java runtime, method, constant, and cooldown field names now use Portal of Sovereignty. Serialized unlimited_blade_works ability strings, asset paths, and existing Puffish unlock IDs deliberately remain compatible.

## Verification

Java 17 syntax parser passes for all source files. JSON parsing, mixin source references, and ZIP integrity checked. Full Gradle compile and Minecraft testing NOT completed: Gradle distribution is absent locally, and download failed with Network is unreachable. Syntax validation does not prove dependency/API compatibility or correct mixin application.

## In-game checks required

1. Shadow blink/return/obstructed return, one-hit bonus, kill reset, 10-second expiry, logout/death, and 30-second HUD timer.
2. Blood Rain local sky transition, zombie sunlight protection, fifth-hit lightning, roofs, PvP rules, and caster-owned summons.
3. Move the Death List across hotbar slots and cursor; try dropping, chest transfer, death/respawn and reconnect. Check guide and personal ledger, H recall versus Shift+H unbind.
4. Finish/cancel Portal of Sovereignty, change dimension, die and disconnect. Confirm no portal/projectile entities remain.

The missing continuation about Assassin passive clones was not recoverable from the interrupted conversation, so no speculative clone passive was added.
