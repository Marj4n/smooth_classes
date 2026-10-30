# Shared Vampire graveyard

Fabric and Forge use the same world-owned cemetery ledger. The first new Vampire
creates one church in untouched Overworld terrain, at least 512 blocks from world
spawn. Later Vampires receive their own underground crypts and named headstones
around that church. Human players do not receive graves.

UUID ownership, church coordinates and plot reservations survive world reloads,
death, reconnects and Origin resets. The intro and starting chest are allocated
once per UUID. Normal respawn rules remain unchanged. Breaking the church does
not cause automatic reconstruction.

New plots expand in rings with ten-block spacing. Construction checks a saved
snapshot before using a reserved plot; changed plots are skipped. New church and
plot searches reject previously inhabited chunks, fluids, block entities and
non-natural blocks. If no safe location is available, nothing is forcibly
overwritten. Plot search is batched and a waiting player is retried automatically.
There are 8,192 candidate positions, rather than unlimited generation.

## Commands

- `/smoothclasses origin grave status`: show the registered church and plot counts.
- `/smoothclasses origin grave claim`: retry an uncompleted first awakening.
- `/smoothclasses origin grave adopt`: operator level 2; stand near an intact old
  crypt in the Overworld to register its existing church before new Vampires join.
  Searches within two chunks and does not require its journal to remain in the
  chest. An existing shared church cannot be replaced with this command.
- `/smoothclasses origin grave test`: operator level 2; replay an assigned intact
  coffin without regenerating structures or loot.

On upgrade, an intact old crypt with its journal may be adopted automatically in
loaded chunks near world spawn. Otherwise use `adopt` before the first new Vampire
joins. Old churches are never deleted automatically. The adopted old crypt is
reserved against reuse because legacy ownership is unknown.

World data is stored in `data/smooth_classes_vampire_graveyard.dat`. Keep it with
the world backup; deleting it discards the shared-church and ownership records.

Automated coverage checks one-church ownership, unique plot spacing, independent
UUID allocations, blocked plots, and NBT reloads with 256 owners. These tests do
not substitute for an in-game multiplayer placement and awakening check.
