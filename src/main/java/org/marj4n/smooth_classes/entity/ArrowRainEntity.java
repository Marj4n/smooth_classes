package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.content.archer.ArcherContent;
import org.marj4n.smooth_classes.runtime.AbilityRuntime;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellSchools;

/** transient Arrow Rain arrow using vanilla ARROW entity type. */
public final class ArrowRainEntity extends ArrowEntity {
    private int groundLife;
    public ArrowRainEntity(World world,double x,double y,double z){super(EntityType.ARROW,world);setPosition(x,y,z);}

    @Override public void tick(){
        super.tick();
        if(getWorld().isClient)return;
        if(inGround){
            if(++groundLife>=600){discard();return;}
            if((groundLife&1)!=0)return; // proximity scan at 10 Hz; max 50 ms detonation delay
            if(getOwner() instanceof ServerPlayerEntity owner && AbilityRuntime.hasTalent(owner,ArcherContent.ARROW_RAIN_EXPLOSIVE.id())){
                for(LivingEntity target:getWorld().getEntitiesByClass(LivingEntity.class,getBoundingBox().expand(1),
                        x->x!=owner&&x.isAlive()&&!x.isTeammate(owner))){
                    getWorld().createExplosion(this,getX(),getY(),getZ(),1F,false,World.ExplosionSourceType.NONE);
                    if(AbilityRuntime.hasTalent(owner,ArcherContent.ARROW_RAIN_ELEMENTAL.id())){
                        if(random.nextInt(100)<25)target.damage(SpellDamageSource.player(SpellSchools.FIRE,owner),5F);
                        else if(random.nextInt(100)<45)target.damage(SpellDamageSource.player(SpellSchools.FROST,owner),5F);
                        else if(random.nextInt(100)<65)target.damage(SpellDamageSource.player(SpellSchools.LIGHTNING,owner),5F);
                    }
                    discard();break;
                }
            }
        }
    }

    @Override protected void onBlockHit(net.minecraft.util.hit.BlockHitResult hit){
        if(random.nextInt(100)<80){discard();return;}
        super.onBlockHit(hit);
    }
}
