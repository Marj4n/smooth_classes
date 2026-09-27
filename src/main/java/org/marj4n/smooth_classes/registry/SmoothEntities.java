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
    public static void register() {
        FabricDefaultAttributeRegistry.register(DREADGLARE, DreadglareEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GREATER_DREADGLARE, GreaterDreadglareEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WRAITH, WraithEntity.createAttributes());
        SmoothClasses.LOGGER.info("Registered Avenger summon entities.");
    }
}
