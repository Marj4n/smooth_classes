package org.marj4n.smooth_classes.entity;

import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.marj4n.smooth_classes.runtime.BloodRainRuntime;

/** A fixed local storm; never changes the world's global weather. */
public final class BloodRainEntity extends Entity {
    public UUID owner;
    private long expires;
    private boolean empowered;
    public BloodRainEntity(EntityType<?> type,World world){super(type,world);setNoGravity(true);}
    public void configure(UUID id,boolean plus){owner=id;empowered=plus;expires=getWorld().getTime()+3000;}
    public static boolean inside(double dx,double dz){return dx*dx+dz*dz<=4096;}
    public static boolean exposed(LivingEntity e){
        var head=BlockPos.ofFloored(e.getX(),e.getEyeY(),e.getZ());
        return !e.getWorld().getDimension().hasCeiling() && e.getWorld().isSkyVisible(head)
            && e.getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING,head.getX(),head.getZ())<=head.getY();
    }
    public boolean valid(){
        var p=owner==null?null:getServer().getPlayerManager().getPlayer(owner);
        return getWorld().getTime()<expires&&p!=null&&p.isAlive()&&p.getWorld()==getWorld()&&inside(p.getX()-getX(),p.getZ()-getZ());
    }
    @Override public void tick(){
        super.tick();if(!(getWorld() instanceof ServerWorld world))return;
        BloodRainRuntime.track(this);if(isRemoved())return;
        if(!valid()){discard();return;}
        if(age%10!=0)return;
        var box=new Box(getX()-64,world.getBottomY(),getZ()-64,getX()+64,world.getTopY(),getZ()+64);
        for(var e:world.getEntitiesByClass(LivingEntity.class,box,t->t.isAlive()&&!t.isSpectator())){
            if(!inside(e.getX()-getX(),e.getZ()-getZ())||!exposed(e))continue;
            if(e.getUuid().equals(owner)){
                effect(e,StatusEffects.REGENERATION,empowered?2:1);
                // Saturation is instant: pulse once per second instead of filling every game tick.
                if(age%20==0)e.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION,1,2,false,false,true));
                if(empowered)effect(e,StatusEffects.RESISTANCE,2);
            }else{
                effect(e,StatusEffects.WITHER,empowered?2:1);
                effect(e,StatusEffects.HUNGER,empowered?4:2);
            }
        }
    }
    private static void effect(LivingEntity e,StatusEffect fx,int amplifier){
        // Preserve vanilla pulse cadence when refreshing an aura, rather than restarting its timer.
        int period=fx==StatusEffects.REGENERATION?Math.max(1,50>>amplifier):fx==StatusEffects.WITHER?Math.max(1,40>>amplifier):20;
        int duration=period*2-(int)(e.getWorld().getTime()%period);
        e.addStatusEffect(new StatusEffectInstance(fx,duration,amplifier,false,true,true));
    }
    @Override public void remove(RemovalReason reason){super.remove(reason);if(!getWorld().isClient&&reason.shouldDestroy())BloodRainRuntime.ended(this);}
    @Override protected void initDataTracker(){}
    @Override protected void writeCustomDataToNbt(NbtCompound n){if(owner!=null)n.putUuid("Owner",owner);n.putLong("Expires",expires);n.putBoolean("Empowered",empowered);}
    @Override protected void readCustomDataFromNbt(NbtCompound n){owner=n.containsUuid("Owner")?n.getUuid("Owner"):null;expires=n.getLong("Expires");empowered=n.getBoolean("Empowered");}
}
