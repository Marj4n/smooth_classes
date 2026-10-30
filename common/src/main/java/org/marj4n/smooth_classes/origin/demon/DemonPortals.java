package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.OriginAbilityDispatcher.Result;

/** Transfers existing eligible mobs using vanilla cross-dimension entity transfer, never copies via summon. */
public final class DemonPortals {
    public record Portal(BlockPos bottom,int width,int height,Direction.Axis axis){
        public Vec3d middle(){return Vec3d.ofBottomCenter(bottom).add(axis==Direction.Axis.X?(width-1)*.5:0,0,axis==Direction.Axis.Z?(width-1)*.5:0);}
    }
    private record Job(UUID owner,ServerWorld source,ServerWorld target,Portal from,Portal to,ArrayDeque<UUID> mobs){}
    private static final ArrayDeque<Job> JOBS=new ArrayDeque<>();
    private DemonPortals(){}
    public static Portal rectangle(ServerWorld w,BlockPos seed){
        if(!w.isChunkLoaded(seed)||!w.getBlockState(seed).isOf(Blocks.NETHER_PORTAL))return null;
        Direction.Axis axis=w.getBlockState(seed).get(NetherPortalBlock.AXIS);BlockPos step=axis==Direction.Axis.X?new BlockPos(1,0,0):new BlockPos(0,0,1);
        BlockPos bottom=seed;
        for(int i=0;i<21&&portal(w,bottom.down(),axis);i++)bottom=bottom.down();
        for(int i=0;i<21&&portal(w,bottom.subtract(step),axis);i++)bottom=bottom.subtract(step);
        int width=0,height=0;
        while(width<22&&portal(w,bottom.add(step.multiply(width)),axis))width++;
        while(height<22&&portal(w,bottom.up(height),axis))height++;
        if(width<2||width>21||height<3||height>21)return null;
        for(int x=0;x<width;x++)for(int y=0;y<height;y++)if(!portal(w,bottom.add(step.multiply(x)).up(y),axis))return null;
        for(int x=0;x<width;x++)if(!obsidian(w,bottom.add(step.multiply(x)).down())||!obsidian(w,bottom.add(step.multiply(x)).up(height)))return null;
        for(int y=0;y<height;y++)if(!obsidian(w,bottom.subtract(step).up(y))||!obsidian(w,bottom.add(step.multiply(width)).up(y)))return null;
        return new Portal(bottom,width,height,axis);
    }
    private static boolean portal(ServerWorld w,BlockPos p,Direction.Axis a){return w.isChunkLoaded(p)&&w.getBlockState(p).isOf(Blocks.NETHER_PORTAL)&&w.getBlockState(p).get(NetherPortalBlock.AXIS)==a;}
    private static boolean obsidian(ServerWorld w,BlockPos p){return w.isChunkLoaded(p)&&w.getBlockState(p).isOf(Blocks.OBSIDIAN);}
    private static Portal near(ServerWorld w,BlockPos around,int radius){
        // Vanilla portal POIs cover the entire vertical range, without scanning
        // millions of blocks or loading terrain chunks merely to search for troops.
        return w.getPointOfInterestStorage().getInSquare(
                entry->entry.matchesKey(net.minecraft.world.poi.PointOfInterestTypes.NETHER_PORTAL),around,radius,
                net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus.ANY)
            .map(net.minecraft.world.poi.PointOfInterest::getPos).filter(w::isChunkLoaded)
            .sorted(Comparator.comparingDouble(pos->pos.getSquaredDistance(around)))
            .map(pos->rectangle(w,pos)).filter(Objects::nonNull).findFirst().orElse(null);
    }
    public static Result backup(ServerPlayerEntity p){
        if(JOBS.stream().anyMatch(j->j.owner().equals(p.getUuid())))return Result.fail("Your Hellgate reinforcement is already transferring.");
        if(p.getWorld().getRegistryKey()!=World.OVERWORLD)return Result.fail("Hellgate: this dimension has no supported Nether Portal connection.");
        ServerWorld nether=p.getServer().getWorld(World.NETHER);if(nether==null)return Result.fail("The Nether is unavailable.");
        Portal to=near(p.getServerWorld(),p.getBlockPos(),12);if(to==null)return Result.fail("Stand within 12 blocks of an intact Nether Portal.");
        BlockPos expected=new BlockPos(MathHelper.floor(to.middle().x/8),to.bottom().getY(),MathHelper.floor(to.middle().z/8));
        Portal from=near(nether,expected,16);if(from==null)return Result.fail("The matching Nether-side portal must already be loaded within its normal search range.");
        // Check the reverse coordinate link instead of arbitrarily choosing any loaded portal.
        BlockPos reverse=new BlockPos(MathHelper.floor(from.middle().x*8),to.bottom().getY(),MathHelper.floor(from.middle().z*8));
        if(Math.abs(reverse.getX()-to.bottom().getX())>128||Math.abs(reverse.getZ()-to.bottom().getZ())>128)return Result.fail("These portals do not form a compatible coordinate link.");
        Portal reciprocal=near(p.getServerWorld(),reverse,128);
        if(reciprocal==null||!reciprocal.bottom().equals(to.bottom()))return Result.fail("The Nether-side portal links to another Overworld exit.");
        ArrayDeque<UUID> entities=new ArrayDeque<>();
        for(MobEntity m:nether.getEntitiesByClass(MobEntity.class,new Box(from.bottom()).expand(128),m->m.squaredDistanceTo(from.middle())<=16384&&DemonArmy.available(m,p)&&!m.hasPassengers()&&!m.hasVehicle())){
            if(fits(m,from)&&fits(m,to))entities.add(m.getUuid());
        }
        if(entities.isEmpty())return Result.fail("No eligible troops fit both portals.");
        JOBS.add(new Job(p.getUuid(),nether,p.getServerWorld(),from,to,entities));return Result.ok("Hellgate: transferring "+entities.size()+" existing Nether allies.");
    }
    public static boolean fits(Entity e,Portal p){return e.getWidth()+.1<=p.width()&&e.getHeight()+.1<=p.height();}
    public static void tick(net.minecraft.server.MinecraftServer server){
        int budget=Math.max(1,DemonConfig.INSTANCE.armyUpdateBudget/12);
        while(budget-->0&&!JOBS.isEmpty()){
            Job j=JOBS.remove();var owner=server.getPlayerManager().getPlayer(j.owner());
            if(owner==null||owner.getServerWorld()!=j.target()||!DemonProgression.has(owner,"lord")||rectangle(j.source(),j.from().bottom())==null||rectangle(j.target(),j.to().bottom())==null)continue;
            UUID id=j.mobs().poll();var entity=id==null?null:j.source().getEntity(id);
            if(entity instanceof MobEntity m&&DemonArmy.available(m,owner)&&fits(m,j.from())&&fits(m,j.to())&&j.target().getEntity(id)==null){
                Vec3d exit=exit(j.target(),j.to(),m);
                if(exit==null)j.mobs().add(id);
                else if(DemonProtection.allowCombat(owner,j.target(),BlockPos.ofFloored(exit))){
                    var d=DemonEntityData.of(m);d.commander=owner.getUuid();d.command=1;d.attackTarget=null;
                    m.resetPortalCooldown(); // Command-style transfer must not immediately bounce back through the frame.
                    boolean moved=m.teleport(j.target(),exit.x,exit.y,exit.z,Set.of(),owner.getYaw(),0);
                    if(moved){var arrived=j.target().getEntity(id);if(arrived!=null)arrived.resetPortalCooldown();j.target().spawnParticles(net.minecraft.particle.ParticleTypes.PORTAL,exit.x,exit.y+.8,exit.z,18,.4,.6,.4,.1);}
                }
            }
            if(!j.mobs().isEmpty())JOBS.add(j);
        }
    }
    private static Vec3d exit(ServerWorld w,Portal portal,MobEntity mob){
        Vec3d middle=portal.middle();
        double offset=.5+mob.getWidth()*.5+.15;
        for(int depth=0;depth<6;depth++)for(int side:new int[]{-1,1})for(int spread=0;spread<portal.width();spread++){
            double along=spread-(portal.width()-1)*.5;
            double outward=side*(offset+depth*(mob.getWidth()+.25));
            Vec3d p=middle.add(portal.axis()==Direction.Axis.X?along:outward,0,portal.axis()==Direction.Axis.Z?along:outward);
            if(!w.isChunkLoaded(BlockPos.ofFloored(p)))continue;
            Box box=mob.getDimensions(mob.getPose()).getBoxAt(p);
            if(!w.isSpaceEmpty(mob,box)||!w.getOtherEntities(null,box).isEmpty()||!w.getFluidState(BlockPos.ofFloored(p)).isEmpty())continue;
            if(!(mob instanceof FlyingEntity)&&!w.getBlockState(BlockPos.ofFloored(p).down()).isSolidBlock(w,BlockPos.ofFloored(p).down()))continue;
            return p;
        }
        return null;
    }
    public static void clear(){JOBS.clear();}
    public static void cancel(UUID owner){JOBS.removeIf(job->job.owner().equals(owner));}
}
