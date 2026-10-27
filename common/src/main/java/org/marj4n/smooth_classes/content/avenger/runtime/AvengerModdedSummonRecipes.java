package org.marj4n.smooth_classes.content.avenger.runtime;

/**
 * Built-in optional mob integrations generated from the actual 1.20.1 Fabric JAR
 * entity IDs and loot tables supplied for Smooth Odyssey.
 * No direct class reference or hard dependency on any external mob mod.
 * The two ingredients are ONE item from each hand; they are refunded on recall.
 */
final class AvengerModdedSummonRecipes {
    private AvengerModdedSummonRecipes() {}

    static void register() {
        // alexscaves: entity registry + loot table based recipes.
        AvengerSummonRecipes.registerExternal("alexscaves:atlatitan", "alexscaves:dinosaur_chop", "alexscaves:heavy_bone");
        AvengerSummonRecipes.registerExternal("alexscaves:boundroid", "alexscaves:heavyweight", "minecraft:chain");
        AvengerSummonRecipes.registerExternal("alexscaves:brainiac", "alexscaves:green_soylent", "alexscaves:charred_remnant");
        AvengerSummonRecipes.registerExternal("alexscaves:candicorn", "alexscaves:sweet_puff", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:caniac", "alexscaves:candy_cane", "alexscaves:sharpened_candy_cane");
        AvengerSummonRecipes.registerExternal("alexscaves:caramel_cube", "alexscaves:caramel", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:corrodent", "alexscaves:corrodent_teeth", "minecraft:coarse_dirt");
        AvengerSummonRecipes.registerExternal("alexscaves:deep_one", "alexscaves:sea_glass_shards", "minecraft:prismarine_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexscaves:deep_one_knight", "alexscaves:sea_glass_shards", "minecraft:iron_sword"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexscaves:deep_one_mage", "alexscaves:sea_glass_shards", "minecraft:lapis_lazuli"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexscaves:ferrouslime", "alexscaves:ferrouslime_ball", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:forsaken", "alexscaves:pure_darkness", "minecraft:bone");
        AvengerSummonRecipes.registerExternal("alexscaves:gammaroach", "alexscaves:toxic_paste", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:gingerbread_man", "alexscaves:gingerbread_crumbs", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:gloomoth", "alexscaves:moth_dust", "minecraft:string");
        AvengerSummonRecipes.registerExternal("alexscaves:gossamer_worm", "alexscaves:bioluminesscence", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:grottoceratops", "alexscaves:dinosaur_chop", "alexscaves:tough_hide");
        AvengerSummonRecipes.registerExternal("alexscaves:gum_worm", "alexscaves:sweet_tooth", "minecraft:sugar");
        AvengerSummonRecipes.registerExternal("alexscaves:gumbeeper", "alexscaves:gumball_pile", "minecraft:gunpowder");
        AvengerSummonRecipes.registerExternal("alexscaves:gummy_bear", "alexscaves:gelatin_red", "alexscaves:sweetish_fish_red");
        AvengerSummonRecipes.registerExternal("alexscaves:lanternfish", "alexscaves:lanternfish", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexscaves:licowitch", "alexscaves:radiant_essence", "alexscaves:sugar_staff");
        AvengerSummonRecipes.registerExternal("alexscaves:magnetron", "minecraft:iron_ingot", "minecraft:redstone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexscaves:mine_guardian", "alexscaves:depth_charge", "minecraft:prismarine_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:notor", "alexscaves:notor_gizmo", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:nucleeper", "alexscaves:fissile_core", "minecraft:gunpowder");
        AvengerSummonRecipes.registerExternal("alexscaves:radgill", "alexscaves:radgill", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:raycat", "minecraft:bone", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:relicheirus", "alexscaves:dinosaur_chop", "minecraft:feather");
        AvengerSummonRecipes.registerExternal("alexscaves:sea_pig", "alexscaves:sea_pig", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexscaves:subterranodon", "minecraft:bone", "minecraft:raw_beef"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexscaves:sweetish_fish", "alexscaves:sweetish_fish_red", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexscaves:teletor", "alexscaves:raw_azure_neodymium", "alexscaves:raw_scarlet_neodymium");
        AvengerSummonRecipes.registerExternal("alexscaves:tremorsaurus", "alexscaves:dinosaur_chop", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:trilocaris", "alexscaves:trilocaris_tail", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexscaves:tripodfish", "alexscaves:tripodfish", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexscaves:underzealot", "alexscaves:dark_tatters", "alexscaves:desolate_dagger");
        AvengerSummonRecipes.registerExternal("alexscaves:vallumraptor", "minecraft:bone", "minecraft:iron_ingot"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexscaves:vesper", "alexscaves:guano", "alexscaves:vesper_wing");
        AvengerSummonRecipes.registerExternal("alexscaves:watcher", "alexscaves:occult_gem", "alexscaves:dark_tatters");
        // alexsmobs: entity registry + loot table based recipes.
        AvengerSummonRecipes.registerExternal("alexsmobs:alligator_snapping_turtle", "minecraft:scute", "minecraft:kelp"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:anaconda", "minecraft:leather", "minecraft:bone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:anteater", "minecraft:leather", "minecraft:amethyst_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:bald_eagle", "minecraft:feather", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:banana_slug", "alexsmobs:banana_slug_slime", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:bison", "alexsmobs:bison_fur", "minecraft:beef");
        AvengerSummonRecipes.registerExternal("alexsmobs:blobfish", "alexsmobs:blobfish", "minecraft:bone_meal");
        AvengerSummonRecipes.registerExternal("alexsmobs:blue_jay", "minecraft:feather", "minecraft:bone");
        AvengerSummonRecipes.registerExternal("alexsmobs:bone_serpent", "alexsmobs:bone_serpent_tooth", "minecraft:bone");
        AvengerSummonRecipes.registerExternal("alexsmobs:bunfungus", "minecraft:red_mushroom", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:cachalot_whale", "minecraft:cod", "minecraft:amethyst_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:caiman", "minecraft:cod", "minecraft:kelp"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:capuchin_monkey", "minecraft:leather", "minecraft:iron_ingot"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:catfish", "alexsmobs:raw_catfish", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexsmobs:centipede_head", "alexsmobs:centipede_leg", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:cockroach", "alexsmobs:cockroach_wing_fragment", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:comb_jelly", "alexsmobs:rainbow_jelly", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:cosmaw", "minecraft:chorus_fruit", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:cosmic_cod", "alexsmobs:cosmic_cod", "minecraft:bone_meal");
        AvengerSummonRecipes.registerExternal("alexsmobs:crimson_mosquito", "alexsmobs:mosquito_proboscis", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:crocodile", "alexsmobs:crocodile_scute", "alexsmobs:crocodile_egg");
        AvengerSummonRecipes.registerExternal("alexsmobs:crow", "minecraft:feather", "minecraft:iron_ingot");
        AvengerSummonRecipes.registerExternal("alexsmobs:devils_hole_pupfish", "minecraft:cod", "minecraft:bone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:dropbear", "alexsmobs:dropbear_claw", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:elephant", "minecraft:leather", "minecraft:redstone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:emu", "alexsmobs:emu_feather", "minecraft:feather");
        AvengerSummonRecipes.registerExternal("alexsmobs:endergrade", "minecraft:ender_pearl", "minecraft:amethyst_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:enderiophage", "alexsmobs:capsid", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:farseer", "alexsmobs:farseer_arm", "minecraft:ender_pearl");
        AvengerSummonRecipes.registerExternal("alexsmobs:flutter", "minecraft:spore_blossom", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:fly", "alexsmobs:maggot", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:flying_fish", "alexsmobs:flying_fish", "minecraft:bone_meal");
        AvengerSummonRecipes.registerExternal("alexsmobs:frilled_shark", "minecraft:cod", "minecraft:iron_ingot"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:froststalker", "alexsmobs:froststalker_horn", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:gazelle", "alexsmobs:gazelle_horn", "minecraft:mutton");
        AvengerSummonRecipes.registerExternal("alexsmobs:gelada_monkey", "minecraft:leather", "minecraft:coal"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:giant_squid", "minecraft:ink_sac", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexsmobs:gorilla", "minecraft:leather", "minecraft:glowstone_dust"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:grizzly_bear", "alexsmobs:bear_fur", "alexsmobs:bear_dust");
        AvengerSummonRecipes.registerExternal("alexsmobs:guster", "alexsmobs:guster_eye", "minecraft:sand");
        AvengerSummonRecipes.registerExternal("alexsmobs:hammerhead_shark", "minecraft:cod", "minecraft:redstone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:hummingbird", "minecraft:feather", "minecraft:redstone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:jerboa", "minecraft:leather", "minecraft:lapis_lazuli"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:kangaroo", "alexsmobs:kangaroo_hide", "alexsmobs:kangaroo_meat");
        AvengerSummonRecipes.registerExternal("alexsmobs:komodo_dragon", "minecraft:leather", "minecraft:copper_ingot"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:laviathan", "minecraft:magma_block", "minecraft:blackstone");
        AvengerSummonRecipes.registerExternal("alexsmobs:leafcutter_ant", "minecraft:slime_ball", "minecraft:spider_eye"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:lobster", "alexsmobs:lobster_tail", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:maned_wolf", "minecraft:leather", "minecraft:prismarine_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:mantis_shrimp", "minecraft:cod", "minecraft:prismarine_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:mimic_octopus", "minecraft:ink_sac", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:mimicube", "alexsmobs:mimicream", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:moose", "alexsmobs:moose_ribs", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:mudskipper", "minecraft:tropical_fish", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:mungus", "minecraft:leather", "minecraft:flint"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:murmur", "alexsmobs:elastic_tendon", "minecraft:red_wool");
        AvengerSummonRecipes.registerExternal("alexsmobs:orca", "minecraft:cod", "minecraft:coal"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:platypus", "minecraft:leather", "minecraft:slime_ball"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:potoo", "minecraft:feather", "minecraft:coal");
        AvengerSummonRecipes.registerExternal("alexsmobs:raccoon", "alexsmobs:raccoon_tail", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:rain_frog", "minecraft:slime_ball", "minecraft:amethyst_shard"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:rattlesnake", "alexsmobs:rattlesnake_rattle", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:rhinoceros", "minecraft:leather", "minecraft:ender_pearl"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:roadrunner", "alexsmobs:roadrunner_feather", "minecraft:feather");
        AvengerSummonRecipes.registerExternal("alexsmobs:rocky_roller", "alexsmobs:rocky_shell", "minecraft:tuff");
        AvengerSummonRecipes.registerExternal("alexsmobs:sea_bear", "minecraft:cod", "minecraft:glowstone_dust"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:seagull", "minecraft:feather", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("alexsmobs:seal", "minecraft:cod", "minecraft:lapis_lazuli"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:shoebill", "minecraft:feather", "minecraft:glowstone_dust");
        AvengerSummonRecipes.registerExternal("alexsmobs:skelewag", "alexsmobs:skelewag_sword", "alexsmobs:novelty_hat");
        AvengerSummonRecipes.registerExternal("alexsmobs:skreecher", "alexsmobs:skreecher_soul", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:skunk", "minecraft:leather", "minecraft:gunpowder"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:snow_leopard", "minecraft:leather", "minecraft:feather"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:soul_vulture", "minecraft:bone", "minecraft:redstone");
        AvengerSummonRecipes.registerExternal("alexsmobs:spectre", "minecraft:ender_pearl", "minecraft:bone"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:straddler", "alexsmobs:straddlite", "minecraft:basalt");
        AvengerSummonRecipes.registerExternal("alexsmobs:stradpole", "minecraft:cod", "minecraft:copper_ingot"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:sugar_glider", "minecraft:feather", "minecraft:lapis_lazuli"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:sunbird", "minecraft:feather", "minecraft:blaze_powder"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:tarantula_hawk", "alexsmobs:tarantula_hawk_wing_fragment", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("alexsmobs:tasmanian_devil", "minecraft:leather", "minecraft:sugar"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:terrapin", "minecraft:cod", "minecraft:flint"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:tiger", "minecraft:leather", "minecraft:string"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:toucan", "minecraft:feather", "minecraft:copper_ingot");
        AvengerSummonRecipes.registerExternal("alexsmobs:triops", "minecraft:cod", "minecraft:slime_ball"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:tusklin", "minecraft:porkchop", "minecraft:snowball");
        AvengerSummonRecipes.registerExternal("alexsmobs:underminer", "minecraft:iron_pickaxe", "minecraft:coal"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("alexsmobs:warped_toad", "minecraft:shroomlight", "minecraft:nether_wart");
        // illagerinvasion: entity registry + loot table based recipes.
        AvengerSummonRecipes.registerExternal("illagerinvasion:alchemist", "minecraft:glass_bottle", "minecraft:gunpowder");
        AvengerSummonRecipes.registerExternal("illagerinvasion:archivist", "minecraft:book", "minecraft:paper");
        AvengerSummonRecipes.registerExternal("illagerinvasion:basher", "minecraft:iron_nugget", "minecraft:emerald");
        AvengerSummonRecipes.registerExternal("illagerinvasion:firecaller", "minecraft:fire_charge", "minecraft:emerald");
        AvengerSummonRecipes.registerExternal("illagerinvasion:inquisitor", "illagerinvasion:platinum_chunk", "minecraft:emerald");
        AvengerSummonRecipes.registerExternal("illagerinvasion:marauder", "minecraft:emerald", "minecraft:iron_axe");
        AvengerSummonRecipes.registerExternal("illagerinvasion:necromancer", "minecraft:skeleton_skull", "minecraft:emerald");
        AvengerSummonRecipes.registerExternal("illagerinvasion:provoker", "minecraft:arrow", "minecraft:emerald");
        AvengerSummonRecipes.registerExternal("illagerinvasion:sorcerer", "illagerinvasion:unusual_dust", "minecraft:book");
        AvengerSummonRecipes.registerExternal("illagerinvasion:surrendered", "minecraft:emerald", "minecraft:white_banner"); // thematic fallback: no item loot
        // takesapillage: entity registry + loot table based recipes.
        AvengerSummonRecipes.registerExternal("takesapillage:archer", "minecraft:arrow", "minecraft:amethyst_shard");
        AvengerSummonRecipes.registerExternal("takesapillage:clay_golem", "minecraft:clay_ball", "minecraft:allium");
        AvengerSummonRecipes.registerExternal("takesapillage:legioner", "minecraft:emerald", "minecraft:iron_sword");
        AvengerSummonRecipes.registerExternal("takesapillage:skirmisher", "minecraft:emerald", "minecraft:crossbow");
        // friendsandfoes: entity registry + loot table based recipes.
        AvengerSummonRecipes.registerExternal("friendsandfoes:copper_golem", "minecraft:copper_ingot", "minecraft:redstone");
        AvengerSummonRecipes.registerExternal("friendsandfoes:crab", "friendsandfoes:crab_claw", "minecraft:kelp");
        AvengerSummonRecipes.registerExternal("friendsandfoes:glare", "minecraft:glow_berries", "minecraft:moss_block");
        AvengerSummonRecipes.registerExternal("friendsandfoes:iceologer", "minecraft:blue_ice", "minecraft:emerald");
        AvengerSummonRecipes.registerExternal("friendsandfoes:mauler", "minecraft:bone", "minecraft:leather"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("friendsandfoes:moobloom", "minecraft:leather", "minecraft:dandelion");
        AvengerSummonRecipes.registerExternal("friendsandfoes:rascal", "minecraft:amethyst_shard", "minecraft:leather"); // thematic fallback: no item loot
        AvengerSummonRecipes.registerExternal("friendsandfoes:tuff_golem", "minecraft:tuff", "minecraft:white_wool");
        AvengerSummonRecipes.registerExternal("friendsandfoes:wildfire", "friendsandfoes:wildfire_crown_fragment", "minecraft:blaze_rod");
    }
}
