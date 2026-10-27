package org.marj4n.smooth_classes.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.world.World;
public final class GreaterDreadglareEntity extends AvengerMinionEntity {
    public GreaterDreadglareEntity(EntityType<? extends TameableEntity> type, World world) { super(type, world); }
    public static DefaultAttributeContainer.Builder createAttributes() { return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,15).add(EntityAttributes.GENERIC_FLYING_SPEED,1.6).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.6).add(EntityAttributes.GENERIC_ATTACK_DAMAGE,10).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,.6).add(EntityAttributes.GENERIC_FOLLOW_RANGE,48); }
    @Override public double healthMultiplier(){return 4.8;} @Override public double attackMultiplier(){return 3.0;} @Override public boolean isGreater(){return true;}
}
