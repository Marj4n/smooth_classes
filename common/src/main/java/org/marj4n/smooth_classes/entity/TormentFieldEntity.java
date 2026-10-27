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
    private int blasts,maxBlasts=3;
    private float burnDamage,blastDamage,burnCoefficient=TormentRuntime.BURN_COEFFICIENT,blastCoefficient=TormentRuntime.BLAST_COEFFICIENT;
    private double radius=2.5D;
    private boolean transcendent;
    public TormentFieldEntity(EntityType<?> type,World world){super(type,world);setNoGravity(true);}
    public void configure(UUID owner,long now,float burn,float blast,float burnCoefficient,float blastCoefficient,
                          int maxBlasts,double radius,int duration,boolean transcendent){
        this.owner=owner;castAt=now;expires=now+duration;burnDamage=burn;blastDamage=blast;
        this.burnCoefficient=burnCoefficient;this.blastCoefficient=blastCoefficient;this.maxBlasts=maxBlasts;
        this.radius=radius;this.transcendent=transcendent;
    }
    @Override public void tick(){
        super.tick();
        if(!(getWorld() instanceof ServerWorld world))return;
        long now=TormentRuntime.now(world);
        if(now>=expires){discard();return;}
        boolean explode=blasts<maxBlasts && now-castAt>=blasts*12L;
        if(explode){
            blasts++;
            world.playSound(null,getX(),getY(),getZ(),SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.PLAYERS,2F,.65F+blasts*.12F);
            world.spawnParticles(SmoothParticles.BLACK_FLAME,getX(),getY()+.8,getZ(),180,1.5,.8,1.5,.18);
        }
        if(age%4==0){
            int r=(int)Math.ceil(radius);
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=radius*radius)
                world.spawnParticles(SmoothParticles.BLACK_FLAME,getX()+x,getY()+.15,getZ()+z,transcendent?5:3,.28,.12,.28,.02);
        }
        // Explosion stays exact; persistent contact checks run at 10 Hz.
        if(explode || (age&1)==0) {
        // No owner/team exemption: caster and allies can also catch the black flame.
        for(var target:world.getEntitiesByClass(LivingEntity.class,new Box(getX()-radius,getY()-.3,getZ()-radius,getX()+radius,getY()+2.5,getZ()+radius),e->e.isAlive()&&!e.isSpectator())) {
            double dx=Math.max(0,Math.abs(target.getX()-getX())-target.getWidth()/2),dz=Math.max(0,Math.abs(target.getZ()-getZ())-target.getWidth()/2);
            if(dx*dx+dz*dz>radius*radius)continue;
            ((TormentBurnAccess)target).smooth$ignite(getUuid(),owner,expires,burnDamage,burnCoefficient);
            if(transcendent){
                target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WEAKNESS,30,2,false,false,true));
                target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,30,1,false,false,true));
            }
            if(explode)TormentRuntime.hurt(target,owner,blastDamage,blastCoefficient);
        }
        }
    }
    @Override protected void initDataTracker(){}
    @Override protected void writeCustomDataToNbt(NbtCompound n){if(owner!=null)n.putUuid("Owner",owner);n.putLong("CastAt",castAt);n.putLong("Expires",expires);n.putInt("Blasts",blasts);n.putInt("MaxBlasts",maxBlasts);n.putFloat("Burn",burnDamage);n.putFloat("Blast",blastDamage);n.putFloat("BurnCoefficient",burnCoefficient);n.putFloat("BlastCoefficient",blastCoefficient);n.putDouble("Radius",radius);n.putBoolean("Transcendent",transcendent);}
    @Override protected void readCustomDataFromNbt(NbtCompound n){owner=n.containsUuid("Owner")?n.getUuid("Owner"):null;castAt=n.getLong("CastAt");expires=n.getLong("Expires");blasts=n.getInt("Blasts");maxBlasts=n.contains("MaxBlasts")?n.getInt("MaxBlasts"):3;burnDamage=n.getFloat("Burn");blastDamage=n.getFloat("Blast");burnCoefficient=n.contains("BurnCoefficient")?n.getFloat("BurnCoefficient"):TormentRuntime.BURN_COEFFICIENT;blastCoefficient=n.contains("BlastCoefficient")?n.getFloat("BlastCoefficient"):TormentRuntime.BLAST_COEFFICIENT;radius=n.contains("Radius")?n.getDouble("Radius"):2.5D;transcendent=n.getBoolean("Transcendent");}
}
