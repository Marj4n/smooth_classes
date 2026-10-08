# Smooth Classes — R22 (Based on R21 Full Source)

## Test commands (operator / cheats only)

`/smoothclasses origin level` — show the current Origin milestone level.

`/smoothclasses origin level up` — unlock the next milestone of the selected Origin.

`/smoothclasses origin level up 3` — unlock up to three additional milestones.

`/smoothclasses origin level set 4` — unlock the first four milestones (increases only).

`/smoothclasses origin level max` — unlock all seven milestones, including evolution.

The levels here are **Origin skill-tree milestones (0–7)**, not Smooth Progression character XP.
These test commands use Puffish Skills' real saved unlock state. They intentionally bypass
normal objectives (including Vampire Lord's cave, giant bats, and Forsaken requirements)
for **operator testing only**; players without cheats must still complete actual objectives.
Existing milestone unlocks are never revoked by `set`, and level 0 is the starting Origin base.
The four supported V1 Origins are Human, Vampire, Mermaid, and Slime.

## Bat Form lockdown

- Applies only to the small Vampire Bat Form (`vampire.form.bat`), **not** Man-Bat.
- The vanilla hotbar's nine item slots render a dark X overlay.
- All visible Spell Engine spell slots render an X overlay at their configured HUD positions.
- Smooth Classes class ability slots remain X-marked; Origin slots 1 and 2 are locked.
- **Origin slot 0 is deliberately left enabled to return to humanoid form** (default V).
- Entering Bat Form switches the ability page to Origin automatically, and blocks switching
  to a useless Class page until humanoid form is restored.
- Client prevents left click, right click, and continuous mining; server denies use-item,
  use-block, interact-entity, dig/drop/swap, swing, and inventory click packets.
- Spell Engine active casts are denied client-side and server-side. Already running casts
  are cleared, including their charged and channelled release paths.
- Smooth Classes special casts and ability packets are server-guarded as a backstop.
- Bat movement / flight, controls to navigate, chat/commands, and returning to humanoid
  form are preserved; no Bat or Man-Bat model, movement, or Lord color assets were changed.

## Manual verification checklist

1. In a cheats-enabled test world choose Vampire, then run `/smoothclasses origin level up`
   to unlock Bat Form. Verify the new node is unlocked in the Puffish Origin tree.
2. Use the Origin page default V key to become a small bat. All nine vanilla hotbar item
   icons, all Spell Engine slots, and blocked Smooth Classes slots should show red X overlays.
3. Attempt attacking an entity, left-clicking a block, holding left click, right-clicking
   a chest, placing blocks, eating, using a spell, casting class skills, dropping/swap items,
   and changing inventory slots. No world/inventory action should execute.
4. Verify Bat flight and movement still work, and the Origin page remains reachable.
5. Press default V to return to humanoid; verify interaction, combat, spells, and hotbar
   work normally with no persistent lock.
6. Test `/smoothclasses origin level set 7` to test Vampire Lord and confirm regular
   (non-operator) progression still requires trial objectives.
7. Repeat on a multiplayer Fabric server, including a modified client sending packets.

## Build status

This archive is a source patch, not a compiled or gameplay-verified release. The local
Gradle wrapper couldn't run in the patch environment because it needed to download
Gradle 9.6.1 and the host was unreachable. Compile and test in IntelliJ with JDK 21
(or an already cached Gradle 9.6.1 distribution), then run `gradlew.bat clean build`.
