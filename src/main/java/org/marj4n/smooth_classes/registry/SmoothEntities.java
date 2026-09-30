package org.marj4n.smooth_classes.registry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.entity.*;
public final class SmoothEntities {
    private SmoothEntities() {}
    public static final EntityType<DreadglareEntity> DREADGLARE = Registry.register(Registries.ENTITY_TYPE, SmoothClasses.id("dreadglare"), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, DreadglareEntity::new).dimensions(EntityDimensions.fixed(.75f,.75f)).build());
    public static final EntityType<GreaterDreadglareEntity> GREATER_DREADGLARE = Registry.register(Registries.ENTITY_TYPE, SmoothClasses.id("greater_dreadglare"), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GreaterDreadglareEntity::new).dimensions(EntityDimensions.fixed(.85f,.85f)).build());
    public static final EntityType<WraithEntity> WRAITH = Registry.register(Registries.ENTITY_TYPE, SmoothClasses.id("wraith"), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, WraithEntity::new).dimensions(EntityDimensions.fixed(1f,1f)).build());
    public static final EntityType<SpellTargetEntity> SPELL_TARGET = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("spell_target"), FabricEntityTypeBuilder.create(SpawnGroup.MISC, SpellTargetEntity::new)
                    .dimensions(EntityDimensions.fixed(.1f,.1f)).build());
    public static final EntityType<SacredBannerEntity> SACRED_BANNER = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("sacred_banner"), FabricEntityTypeBuilder.create(SpawnGroup.MISC, SacredBannerEntity::new)
                    .dimensions(EntityDimensions.fixed(1.3F,4F)).trackRangeBlocks(64).build());
    public static final EntityType<TormentFieldEntity> TORMENT_FIELD = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("torment_field"), FabricEntityTypeBuilder.create(SpawnGroup.MISC, TormentFieldEntity::new)
                    .dimensions(EntityDimensions.fixed(5F,2.5F)).trackRangeBlocks(64).build());
    public static final EntityType<BloodRainEntity> BLOOD_RAIN = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("blood_rain"), FabricEntityTypeBuilder.create(SpawnGroup.MISC, BloodRainEntity::new)
                    .dimensions(EntityDimensions.fixed(1F,1F)).trackRangeBlocks(160).build());
    public static final EntityType<HighBeamEntity> HIGH_BEAM = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("high_beam"), FabricEntityTypeBuilder.create(SpawnGroup.MISC, HighBeamEntity::new)
                    .dimensions(EntityDimensions.fixed(1F,4F)).trackRangeBlocks(128).trackedUpdateRate(1).build());

    public static final EntityType<LancerImpaleEntity> LANCER_IMPALE = Registry.register(
            Registries.ENTITY_TYPE,
            SmoothClasses.id("lancer_impale"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, LancerImpaleEntity::new)
                    .dimensions(EntityDimensions.fixed(0.1F, 0.1F))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(1)
                    .build());

    public static final EntityType<RiderHorseEntity> RIDER_HORSE = Registry.register(
            Registries.ENTITY_TYPE,
            SmoothClasses.id("rider_horse"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, RiderHorseEntity::new)
                    .dimensions(EntityDimensions.fixed(1.39648F, 1.6F))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(1)
                    .build());

    public static final EntityType<RiderDreadSteedEntity> RIDER_DREAD_STEED = Registry.register(
            Registries.ENTITY_TYPE,
            SmoothClasses.id("rider_dread_steed"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, RiderDreadSteedEntity::new)
                    .fireImmune()
                    .dimensions(EntityDimensions.fixed(1.39648F, 1.6F))
                    .trackRangeBlocks(96)
                    .build());

    public static final EntityType<RiderHippogryphEntity> RIDER_HIPPOGRYPH = Registry.register(
            Registries.ENTITY_TYPE,
            SmoothClasses.id("rider_hippogryph"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, RiderHippogryphEntity::new)
                    .fireImmune()
                    .dimensions(EntityDimensions.fixed(1.7F, 1.6F))
                    .trackRangeBlocks(128)
                    .trackedUpdateRate(1)
                    .build());
    public static final EntityType<BladePortalEntity> BLADE_PORTAL = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("blade_portal"), FabricEntityTypeBuilder.create(SpawnGroup.MISC, BladePortalEntity::new)
                    .dimensions(EntityDimensions.fixed(2.8F, 2.8F)).trackRangeBlocks(64).trackedUpdateRate(1).build());
    public static final EntityType<ProjectedDaggerEntity> PROJECTED_DAGGER = Registry.register(Registries.ENTITY_TYPE,
            SmoothClasses.id("projected_dagger"), FabricEntityTypeBuilder.<ProjectedDaggerEntity>create(SpawnGroup.MISC, ProjectedDaggerEntity::new)
                    .dimensions(EntityDimensions.fixed(.2F, .2F)).trackRangeBlocks(80).trackedUpdateRate(2).build());
    public static void register() {
        FabricDefaultAttributeRegistry.register(DREADGLARE, DreadglareEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GREATER_DREADGLARE, GreaterDreadglareEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WRAITH, WraithEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RIDER_HORSE, RiderHorseEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RIDER_DREAD_STEED, RiderDreadSteedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RIDER_HIPPOGRYPH, RiderHippogryphEntity.createAttributes());
        SmoothClasses.LOGGER.info("Registered Smooth Classes entities, including standalone Rider mounts.");
    }
}
