package org.marj4n.smooth_classes.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** Temporary vanilla light block: illuminates nearby blocks, then fades and removes itself. */
public final class DivineRayLightRuntime {
    private DivineRayLightRuntime() {}

    private record Spot(ServerWorld world, BlockPos position) {}
    private static final Map<Spot,Integer> LIGHTS = new HashMap<>();
    private record Recipient(LivingEntity entity, int ticks) {}
    private static final Map<UUID,Recipient> RECIPIENTS = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID,Recipient>> recipients=RECIPIENTS.entrySet().iterator();
            while(recipients.hasNext()) {
                var entry=recipients.next();
                Recipient pending=entry.getValue();
                if(!pending.entity().isAlive()) { recipients.remove(); continue; }
                if(pending.ticks()%2==0 && pending.entity().getWorld() instanceof ServerWorld world) {
                    LivingEntity target=pending.entity();
                    for(int height=0;height<5;height++) world.spawnParticles(ParticleTypes.END_ROD,
                            target.getX(),target.getY()+target.getHeight()+height*.75,
                            target.getZ(),3,.35,.15,.35,.012);
                    world.spawnParticles(ParticleTypes.INSTANT_EFFECT,target.getX(),
                            target.getY()+target.getHeight()*.5,target.getZ(),5,.45,.4,.45,.015);
                }
                if(pending.ticks()<=1) {
                    pending.entity().addStatusEffect(new StatusEffectInstance(SmoothEffects.UNDYING,280,0,false,false,true));
                    recipients.remove();
                } else entry.setValue(new Recipient(pending.entity(),pending.ticks()-1));
            }
            Iterator<Map.Entry<Spot,Integer>> iterator=LIGHTS.entrySet().iterator();
            while(iterator.hasNext()) {
                var entry=iterator.next();
                Spot spot=entry.getKey();
                int remaining=entry.getValue()-1;
                if(spot.world().getBlockState(spot.position()).isOf(Blocks.LIGHT)) {
                    if(remaining<=0) spot.world().setBlockState(spot.position(),Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);
                    else if(remaining%5==0) spot.world().setBlockState(spot.position(),
                            Blocks.LIGHT.getDefaultState().with(Properties.LEVEL_15,Math.max(1,remaining/2)),Block.NOTIFY_ALL);
                }
                if(remaining<=0 || !spot.world().getBlockState(spot.position()).isOf(Blocks.LIGHT)) iterator.remove();
                else entry.setValue(remaining);
            }
        });
    }

    public static void illuminate(LivingEntity entity) {
        if(!(entity.getWorld() instanceof ServerWorld world)) return;
        RECIPIENTS.put(entity.getUuid(),new Recipient(entity,12));
        BlockPos pos=entity.getBlockPos().up(Math.max(2,(int)Math.ceil(entity.getHeight())+1));
        Spot spot=new Spot(world,pos);
        if(!world.getBlockState(pos).isAir() && !LIGHTS.containsKey(spot)) return;
        world.setBlockState(pos,Blocks.LIGHT.getDefaultState().with(Properties.LEVEL_15,15),Block.NOTIFY_ALL);
        LIGHTS.put(spot,30);
    }
}
