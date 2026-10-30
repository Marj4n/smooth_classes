# Book-bound Avenger librarians

Developer documentation only. This is a hidden Easter egg: do not add recipe hints to the player-facing Death List, tutorials or unlock notices.

Hold **one emerald in the main hand** and **an enchanted book in the off hand**, then use the existing Death List summon action (including Stay summon). This uses one existing villager soul, one summon charge, one emerald and one enchanted book, only after spawning succeeds. An emerald without an enchanted book still summons the normal unemployed villager. Existing emerald fusion recipes are unchanged.

The summoned villager is a novice librarian with one guaranteed trade: **16 emeralds + one ordinary book → one exact copy of the supplied enchanted book**. It has 12 uses and normal vanilla price/reputation/lectern restocking rules. One initial villager XP keeps its profession from being dropped before the first trade. Further vanilla leveling may add normal trades.

All stored enchantment IDs and levels are copied as ItemStack NBT, including modded enchantments, multiple enchantments, names and custom book data. No vanilla enchantment whitelist or level clamping is applied. This supports vanilla enchanted books carrying modded enchantments and registered items extending EnchantedBookItem; unrelated custom containers using a different item type are not treated as books. Empty enchanted books are rejected before any cost is spent.

Vanilla villager data persists the profession/trade offers. A separate small villager NBT field persists the original ritual book for Recall, independently of trade usage, discounts, restocking or other mods changing the offers. Successful Recall returns the normal emerald and the original book including its NBT exactly once through the existing recall lifecycle. Unbind keeps the real villager/trades and does not refund ingredients. Wild villagers are not assigned new trades.

The implementation is shared by Fabric and Forge 1.20.1. Tests cover recipe precedence and normal fallback, exact book copies, mod-namespaced enchantment payloads, trade save/load, and original-ingredient persistence. A synthetic mod enchantment ID verifies raw-data preservation; it is not a claim of testing every enchantment mod in-game.

Verification: all 93 Fabric-loader-backed JUnit tests passed, including 9 librarian tests. The combined `build verifyLoaderJars` task passed for Fabric and Forge, verifying loader metadata, registered mixin classes/refmaps and shared resources. Existing deprecation and annotation-processor warnings remain. Actual player trading, workstation restocking, Recall/save-load gameplay and two-client multiplayer were not manually exercised in this change; unit-level NBT/trade serialization is separate from those runtime checks.
