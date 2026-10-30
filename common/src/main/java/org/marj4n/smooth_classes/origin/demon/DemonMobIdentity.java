package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import net.minecraft.entity.boss.ServerBossBar;

public final class DemonMobIdentity {
    public static final TagKey<EntityType<?>> NATIVE=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("smooth_classes:demon_nether_native"));
    public static final TagKey<EntityType<?>> BOSSES=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("smooth_classes:demon_bosses"));
    private static final TagKey<EntityType<?>> COMMON_BOSSES=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("c:bosses")),FORGE_BOSSES=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("forge:bosses"));
    public static final TagKey<EntityType<?>> EXCLUDED=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("smooth_classes:demon_command_excluded"));
    public static final TagKey<EntityType<?>> STRUCTURE_NATIVE=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("smooth_classes:demon_nether_structure_native"));
    private static final Set<EntityType<?>> NATIVES=new HashSet<>();
    private static final Set<net.minecraft.world.gen.structure.Structure> NETHER_STRUCTURES=new HashSet<>();
    private static final Map<Class<?>,Boolean> BOSS_CLASSES=new HashMap<>();
    private DemonMobIdentity(){}
    public static void rebuild(MinecraftServer server){
        NATIVES.clear();BOSS_CLASSES.clear();NETHER_STRUCTURES.clear();Set<EntityType<?>> other=new HashSet<>();
        for(var world:server.getWorlds()){
            var pool=world.getRegistryKey()==World.NETHER?NATIVES:other;
            for(var biome:world.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes())
                for(SpawnGroup group:SpawnGroup.values())for(var spawn:biome.value().getSpawnSettings().getSpawnEntries(group).getEntries())pool.add(spawn.type);
        }
        // Being a Nether visitor (e.g. an Overworld enderman) does not establish Nether identity.
        NATIVES.removeAll(other);
        for(String id:DemonConfig.INSTANCE.nativeEntities){Identifier key=Identifier.tryParse(id);if(key!=null&&Registries.ENTITY_TYPE.containsId(key))NATIVES.add(Registries.ENTITY_TYPE.get(key));}
        var nether=server.getWorld(World.NETHER);
        if(nether!=null){
            var netherBiomes=nether.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes();
            for(var structure:server.getRegistryManager().get(RegistryKeys.STRUCTURE))if(structure.getValidBiomes().stream().findAny().isPresent()&&structure.getValidBiomes().stream().allMatch(netherBiomes::contains))NETHER_STRUCTURES.add(structure);
        }
        for(var world:server.getWorlds())for(var entity:world.iterateEntities())if(entity!=null)onLoad(entity,world);
        org.marj4n.smooth_classes.SmoothClasses.LOGGER.info("Demon identity: {} Nether-exclusive spawn types, plus explicit native tags",NATIVES.size());
    }
    public static void onLoad(Entity entity,net.minecraft.server.world.ServerWorld world){
        if(!DemonRuntime.running||!(entity instanceof MobEntity mob)||!mob.getType().isIn(STRUCTURE_NATIVE))return;
        var data=DemonEntityData.of(mob);if(data.birthRecorded)return;data.birthRecorded=true;
        if(world.getRegistryKey()!=World.NETHER)return;
        for(var structure:NETHER_STRUCTURES){var start=world.getStructureAccessor().getStructureContaining(mob.getBlockPos(),structure);if(start!=null&&start.hasChildren()){data.nativeBirth=true;return;}}
    }
    public static boolean nativeMob(LivingEntity e){return e instanceof MobEntity&&(e.getType().isIn(NATIVE)||NATIVES.contains(e.getType())||DemonEntityData.of(e).nativeBirth);}
    public static boolean boss(LivingEntity e){
        if(e.getType().isIn(BOSSES)||e.getType().isIn(COMMON_BOSSES)||e.getType().isIn(FORGE_BOSSES))return true;
        if(DemonConfig.INSTANCE.bossEntities.containsValue(Registries.ENTITY_TYPE.getId(e.getType()).toString()))return true;
        return BOSS_CLASSES.computeIfAbsent(e.getClass(),type->{
            try{for(Class<?> c=type;c!=null&&c!=Object.class;c=c.getSuperclass()){
                // Cataclysm also has minibosses without a visible ServerBossBar.
                if(c.getName().contains(".BossMonsters."))return true;
                for(var f:c.getDeclaredFields())if(ServerBossBar.class.isAssignableFrom(f.getType()))return true;
            }}
            catch(LinkageError ex){org.marj4n.smooth_classes.SmoothClasses.LOGGER.warn("Demon commands exclude {} because its boss/ownership metadata cannot be linked",type.getName(),ex);return true;}
            return false;
        });
    }
    public static boolean eligible(LivingEntity e){
        if(!nativeMob(e)||!e.isAlive()||boss(e)||e.getType().isIn(EXCLUDED)||DemonConfig.INSTANCE.excludedEntities.contains(Registries.ENTITY_TYPE.getId(e.getType()).toString()))return false;
        if(e instanceof net.minecraft.entity.Tameable t&&t.getOwnerUuid()!=null)return false;
        if(e instanceof AbstractHorseEntity h&&h.getOwnerUuid()!=null)return false;
        if(e instanceof MobEntity mob&&mob.isLeashed())return false;
        for(String tag:e.getCommandTags())if(tag.startsWith("smooth_classes_avenger_")||tag.startsWith("owner:")||tag.startsWith("tamed"))return false;
        return true;
    }
    public static boolean ignore(MobEntity mob,LivingEntity target){
        if(!(target instanceof ServerPlayerEntity p)||!DemonProgression.demon(p)||!eligible(mob))return false;
        var d=DemonEntityData.of(mob);
        if(d.commander!=null&&!d.commander.equals(p.getUuid()))return false;
        return DemonProgression.has(p,"sovereign")||(DemonProgression.has(p,"recognition")&&!p.getUuid().equals(d.provokedBy)&&!d.provoked.contains(p.getUuid()));
    }
}
