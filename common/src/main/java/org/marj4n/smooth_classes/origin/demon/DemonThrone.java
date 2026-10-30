package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.entity.DemonSeatEntity;

public final class DemonThrone {
    private static final Map<UUID,DemonSeatEntity> SEATS=new HashMap<>();
    private DemonThrone(){}
    public static boolean validate(ServerWorld world,BlockPos origin,int rotation){
        if(world.getRegistryKey()!=World.NETHER)return false;
        for(int y=0;y<4;y++)for(int z=0;z<3;z++)for(int x=0;x<3;x++){
            int[] rz=DemonRules.rotate(x,z,rotation);BlockPos p=origin.add(rz[0],y,rz[1]);
            if(!world.isChunkLoaded(p)||!world.getWorldBorder().contains(p))return false;
            var block=world.getBlockState(p);char c=DemonRules.throne(x,y,z);
            if(c=='N'?!block.isOf(Blocks.NETHERITE_BLOCK):c=='B'?!block.isOf(Blocks.BEACON):!block.isAir())return false;
        }
        return true;
    }
    public static Vec3d seat(DemonWorldState.Throne t){
        double x=1.5,z=1.4;
        for(int i=0;i<t.rotation();i++){double old=x;x=3-z;z=old;}
        return new Vec3d(t.origin().getX()+x,t.origin().getY()+1,t.origin().getZ()+z);
    }
    public static float yaw(DemonWorldState.Throne t){return 180+t.rotation()*90;}
    public static boolean bind(ServerPlayerEntity p,BlockPos core){
        if(!DemonProgression.demon(p)||p.interactionManager.getGameMode()!=net.minecraft.world.GameMode.SURVIVAL||!DemonProgression.has(p,"army")||!DemonProgression.has(p,"sovereign"))return false;
        ServerWorld world=p.getServerWorld();if(world.getRegistryKey()!=World.NETHER)return false;
        DemonWorldState ledger=DemonWorldState.get(p.getServer());
        for(int r=0;r<4;r++){
            int[] beacon=DemonRules.rotate(1,2,r);BlockPos origin=core.add(-beacon[0],-2,-beacon[1]);
            if(!validate(world,origin,r))continue;
            for(var existing:ledger.thrones.values())if(Math.abs(existing.origin().getX()-origin.getX())<3&&Math.abs(existing.origin().getZ()-origin.getZ())<3&&Math.abs(existing.origin().getY()-origin.getY())<4&&!existing.owner().equals(p.getUuid())){
                p.sendMessage(Text.literal("This Black Throne is bound to another soul."),true);return true;
            }
            var old=ledger.thrones.get(p.getUuid());
            if(old!=null&&!old.origin().equals(origin)&&validate(world,old.origin(),old.rotation())){
                p.sendMessage(Text.literal("You already have an intact active Black Throne."),true);return true;
            }
            if(!DemonProtection.allowCombat(p,world,core)){p.sendMessage(Text.literal("You cannot bind a throne in this protected region."),true);return true;}
            DemonWorldState.Throne throne=new DemonWorldState.Throne(p.getUuid(),origin,r);
            // Commit binding only after the real seat entity and passenger relation succeed.
            if(!sit(p,throne)){p.sendMessage(Text.literal("The throne seat is obstructed."),true);return true;}
            ledger.thrones.put(p.getUuid(),throne);ledger.markDirty();
            OriginState state=OriginRuntime.state(p);state.flag("demon.lord");state.progress("demon.decay",0);state.unflag("demon.zombified");
            DemonProgression.sync(p,state);
            state.progress("demon.binding_cinematic",40);
            var advancement=p.getServer().getAdvancementLoader().get(new net.minecraft.util.Identifier("smooth_classes:demon/demon_lord"));
            if(advancement!=null)for(String criterion:advancement.getCriteria().keySet())p.getAdvancementTracker().grantCriterion(advancement,criterion);
            p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal("DEMON LORD").formatted(net.minecraft.util.Formatting.DARK_RED)));
            org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendOriginState(p);return true;
        }
        return false;
    }
    public static boolean sit(ServerPlayerEntity p,DemonWorldState.Throne throne){
        ServerWorld nether=p.getServer().getWorld(World.NETHER);
        if(nether==null||!validate(nether,throne.origin(),throne.rotation()))return false;
        Vec3d pos=seat(throne);double radius=DemonBodyGeometry.WIDTH/2;Box standing=new Box(pos.x-radius,pos.y,pos.z-radius,pos.x+radius,pos.y+DemonBodyGeometry.THRONE_HEIGHT,pos.z+radius);
        if(!nether.isSpaceEmpty(p,standing))return false;
        removeSeat(p);
        p.stopRiding();p.teleport(nether,pos.x,pos.y,pos.z,yaw(throne),0);
        DemonSeatEntity entity=new DemonSeatEntity(org.marj4n.smooth_classes.registry.SmoothEntities.DEMON_SEAT,nether);
        entity.configure(p.getUuid(),throne.origin(),throne.rotation(),pos);
        if(!nether.spawnEntity(entity))return false;
        if(!p.startRiding(entity,true)){entity.discard();return false;}
        p.calculateDimensions();
        SEATS.put(p.getUuid(),entity);return true;
    }
    public static void removeSeat(ServerPlayerEntity p){var seat=SEATS.remove(p.getUuid());if(seat!=null){p.stopRiding();seat.discard();}}
    public static void clear(){for(var seat:SEATS.values())seat.discard();SEATS.clear();}
    public static boolean returnReady(ServerPlayerEntity p){
        var t=DemonWorldState.get(p.getServer()).thrones.get(p.getUuid());var w=p.getServer().getWorld(World.NETHER);
        // Only the bound throne's at most four chunks may be loaded for an actual fatal return.
        // Periodic validation and ordinary domain/army ticks never load chunks.
        if(t==null||w==null)return false;
        for(int x=t.origin().getX()>>4;x<=(t.origin().getX()+2)>>4;x++)for(int z=t.origin().getZ()>>4;z<=(t.origin().getZ()+2)>>4;z++)w.getChunk(x,z);
        return validate(w,t.origin(),t.rotation())&&DemonProtection.allowCombat(p,w,t.origin());
    }
    public static boolean returnTo(ServerPlayerEntity p){var t=DemonWorldState.get(p.getServer()).thrones.get(p.getUuid());return t!=null&&sit(p,t);}
    public static void tick(ServerPlayerEntity p){
        var s=OriginRuntime.state(p);int ritual=s.progress("demon.binding_cinematic");
        if(ritual>0){s.progress("demon.binding_cinematic",ritual-1);if(p.age%4==0)p.getServerWorld().spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getBodyY(.5),p.getZ(),6,.4,.6,.4,.01);}
        if(p.age%40==0){var t=DemonWorldState.get(p.getServer()).thrones.get(p.getUuid());var w=p.getServer().getWorld(World.NETHER);s.progress("demon.throne_intact",t!=null&&w!=null&&validate(w,t.origin(),t.rotation())?1:0);}
    }
}
