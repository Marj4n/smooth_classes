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
    public static void register() {
        FabricDefaultAttributeRegistry.register(DREADGLARE, DreadglareEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GREATER_DREADGLARE, GreaterDreadglareEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WRAITH, WraithEntity.createAttributes());
        SmoothClasses.LOGGER.info("Registered Avenger summon entities.");
    }
}
