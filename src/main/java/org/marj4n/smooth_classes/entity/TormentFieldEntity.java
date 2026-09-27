package org.marj4n.smooth_classes.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.registry.SmoothParticles;
import org.marj4n.smooth_classes.runtime.TormentRuntime;
import org.marj4n.smooth_classes.runtime.TormentBurnAccess;
import java.util.UUID;

/** Stationary magical fire, independent of block/fire/fluid updates. Never destroys or spreads blocks. */
public final class TormentFieldEntity extends Entity {
    private UUID owner;
    private long castAt,expires;
    private int blasts;
    private float burnDamage,blastDamage;
    public TormentFieldEntity(EntityType<?> type,World world){super(type,world);setNoGravity(true);}
    public void configure(UUID owner,long now,float burn,float blast){this.owner=owner;castAt=now;expires=now+TormentRuntime.DURATION;burnDamage=burn;blastDamage=blast;}
    @Override public void tick(){
        super.tick();
        if(!(getWorld() instanceof ServerWorld world))return;
        long now=TormentRuntime.now(world);
        if(now>=expires){discard();return;}
        boolean explode=blasts<3 && now-castAt>=blasts*12L;
        if(explode){
            blasts++;
            world.playSound(null,getX(),getY(),getZ(),SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.PLAYERS,2F,.65F+blasts*.12F);
            world.spawnParticles(SmoothParticles.BLACK_FLAME,getX(),getY()+.8,getZ(),180,1.5,.8,1.5,.18);
        }
        if(age%4==0){
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x*x+z*z<=6)
                world.spawnParticles(SmoothParticles.BLACK_FLAME,getX()+x,getY()+.15,getZ()+z,3,.28,.12,.28,.02);
        }
        // No owner/team exemption: caster and allies can also catch the black flame.
        for(var target:world.getEntitiesByClass(LivingEntity.class,new Box(getX()-2.5,getY()-.3,getZ()-2.5,getX()+2.5,getY()+2.5,getZ()+2.5),e->e.isAlive()&&!e.isSpectator())) {
            double dx=Math.max(0,Math.abs(target.getX()-getX())-target.getWidth()/2),dz=Math.max(0,Math.abs(target.getZ()-getZ())-target.getWidth()/2);
            if(dx*dx+dz*dz>6.25)continue;
            ((TormentBurnAccess)target).smooth$ignite(getUuid(),owner,expires,burnDamage);
            if(explode)TormentRuntime.hurt(target,owner,blastDamage,TormentRuntime.BLAST_COEFFICIENT);
        }
    }
    @Override protected void initDataTracker(){}
    @Override protected void writeCustomDataToNbt(NbtCompound n){if(owner!=null)n.putUuid("Owner",owner);n.putLong("CastAt",castAt);n.putLong("Expires",expires);n.putInt("Blasts",blasts);n.putFloat("Burn",burnDamage);n.putFloat("Blast",blastDamage);}
    @Override protected void readCustomDataFromNbt(NbtCompound n){owner=n.containsUuid("Owner")?n.getUuid("Owner"):null;castAt=n.getLong("CastAt");expires=n.getLong("Expires");blasts=n.getInt("Blasts");burnDamage=n.getFloat("Burn");blastDamage=n.getFloat("Blast");}
}
