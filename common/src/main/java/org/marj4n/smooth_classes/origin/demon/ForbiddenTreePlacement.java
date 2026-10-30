package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState;
import org.marj4n.smooth_classes.SmoothClasses;

/** Single server-thread job; reservation is persisted before the first block write. */
public final class ForbiddenTreePlacement {
    private static Job job;
    public static long lastTickNanos,maxTickNanos;public static int writtenThisTick,maxWrittenThisTick;
    private static final class Job {BlockPos anchor;int chunk,check;final boolean resume;final List<ForbiddenChunkLoad> leases=new ArrayList<>();ForbiddenChunkLoad loading;Job(BlockPos a,boolean resume){anchor=a;this.resume=resume;}}
    private static void releaseJob(){if(job!=null)for(var lease:job.leases)lease.close();job=null;}
    private ForbiddenTreePlacement(){}
    public static ForbiddenTreeRecord record(MinecraftServer s){return DemonWorldState.get(s).forbiddenTree;}
    public static void request(ServerPlayerEntity p){var r=record(p.getServer());if(r.request(p.getUuid())){DemonWorldState.get(p.getServer()).markDirty();SmoothClasses.LOGGER.info("FORBIDDEN_TREE requested once by {}",p.getUuid());}}
    public static void clear(){releaseJob();lastTickNanos=maxTickNanos=0;writtenThisTick=maxWrittenThisTick=0;}
    public static void tick(MinecraftServer server){
        long start=System.nanoTime();writtenThisTick=0;
        try{run(server);}catch(Exception ex){var r=record(server);r.fail("placement exception: "+ex.getMessage());DemonWorldState.get(server).markDirty();releaseJob();SmoothClasses.LOGGER.error("Forbidden Tree failed safely at its reserved anchor",ex);}
        finally{lastTickNanos=System.nanoTime()-start;maxTickNanos=Math.max(maxTickNanos,lastTickNanos);maxWrittenThisTick=Math.max(maxWrittenThisTick,writtenThisTick);}
    }
    private static void run(MinecraftServer server){
        var ledger=DemonWorldState.get(server);var r=ledger.forbiddenTree;var cfg=ForbiddenBirthConfig.INSTANCE;
        if(!cfg.enabled||r.phase==ForbiddenTreeRecord.Phase.UNREQUESTED||r.phase==ForbiddenTreeRecord.Phase.READY||r.phase==ForbiddenTreeRecord.Phase.FAILED)return;
        var actor=r.requestedBy==null?null:server.getPlayerManager().getPlayer(r.requestedBy);
        if(actor==null){for(var p:server.getPlayerManager().getPlayerList())if(org.marj4n.smooth_classes.origin.OriginRuntime.state(p).hasFlag("demon.birth_pending")){actor=p;r.requestedBy=p.getUuid();ledger.markDirty();break;}}
        if(actor==null){releaseJob();return;} // Resume when an authorized online player exists; never fake a protection identity.
        ServerWorld world=server.getWorld(World.NETHER);if(world==null){fail(server,"Nether unavailable");return;}
        var cells=ForbiddenTreeAsset.cells();
        if(r.anchored&&!r.hash.equals(ForbiddenTreeAsset.HASH)){fail(server,"Saved tree source hash differs; explicit migration/repair required, no second tree generated");return;}
        if(job==null){
            if(r.anchored)job=new Job(ForbiddenTreeAsset.anchor(r),true);
            else{
                if(r.attempt>=cfg.searchAttempts){fail(server,"No safe85-block-tall site within "+cfg.searchAttempts+" attempts");return;}
                double angle=r.attempt*2.399963229728653,rad=128+(cfg.searchRadius-128)*(r.attempt+1d)/cfg.searchAttempts;
                int x=((int)(Math.cos(angle)*rad)-32)>>4<<4,z=((int)(Math.sin(angle)*rad)-18)>>4<<4;
                job=new Job(new BlockPos(x,0,z),false);r.phase=ForbiddenTreeRecord.Phase.SEARCHING;
            }
        }
        // Exactly12 aligned chunks for the64x37 source footprint. At most configured1..2 new requests/tick.
        for(int loaded=0;job.chunk<12&&loaded<cfg.chunkLoadsPerTick;loaded++){
            if(job.loading==null){job.loading=new ForbiddenChunkLoad(world,(job.anchor.getX()>>4)+job.chunk%4,(job.anchor.getZ()>>4)+job.chunk/4);job.leases.add(job.loading);return;}
            if(!job.loading.ready())return;job.loading=null;job.chunk++;
        }
        if(job.chunk<12)return;
        if(!job.resume&&r.phase==ForbiddenTreeRecord.Phase.SEARCHING){
            int floor=-1;BlockPos center=job.anchor.add(33,0,28);
            for(int y=39;y>=6;y--){BlockPos feet=new BlockPos(center.getX(),y,center.getZ());if(safeFloor(world,feet.down())&&world.isAir(feet)&&world.isAir(feet.up())&&world.isAir(feet.up(2))){floor=y-1;break;}}
            if(floor<0){reject(server,"No supported ground below Nether roof");return;}
            job.anchor=new BlockPos(job.anchor.getX(),floor,job.anchor.getZ());r.phase=ForbiddenTreeRecord.Phase.VALIDATING;
            Box bounds=ForbiddenTreeAsset.bounds(job.anchor);
            if(job.anchor.getY()<5||job.anchor.getY()+ForbiddenTreeAsset.HEIGHT>124||!world.getWorldBorder().contains(job.anchor)||!world.getWorldBorder().contains(job.anchor.add(63,84,36))){reject(server,"Border/ceiling");return;}
            for(int i=0;i<12;i++)for(var structure:world.getChunk((job.anchor.getX()>>4)+i%4,(job.anchor.getZ()>>4)+i/4).getStructureStarts().values())if(structure.hasChildren()){
                var b=structure.getBoundingBox();if(bounds.intersects(new Box(b.getMinX(),b.getMinY(),b.getMinZ(),b.getMaxX()+1,b.getMaxY()+1,b.getMaxZ()+1))){reject(server,"Existing generated structure");return;}}
            if(!world.getEntitiesByClass(net.minecraft.entity.player.PlayerEntity.class,bounds.expand(2),p->true).isEmpty()){reject(server,"Player already occupying site");return;}
        }
        if(r.phase==ForbiddenTreeRecord.Phase.VALIDATING){
            int budget=cfg.validationBudget;
            while(budget-->0&&job.check<cells.size()){
                var cell=cells.get(job.check++);BlockPos p=job.anchor.add(cell.offset());
                if(!allowed(actor,world,p)||!replaceable(world,p,cell,false)){reject(server,"Protected or occupied structure cell "+p.toShortString());return;}
                if(cell.offset().getY()==0&&!safeFloor(world,p.down())){reject(server,"Unsupported source foundation");return;}
            }
            if(job.check<cells.size())return;
            if(!fallColumnClear(world,job.anchor)){reject(server,"Fruit fall/player emergence blocked");return;}
            if(!r.reserve(job.anchor.getX(),job.anchor.getY(),job.anchor.getZ(),ForbiddenTreeAsset.HASH)){fail(server,"Duplicate reservation refused");return;}
            ledger.markDirty();checkpoint(server);verifyReservation(server,r);
            SmoothClasses.LOGGER.info("FORBIDDEN_TREE durable reservation anchor={} source={} count={}",job.anchor,ForbiddenTreeAsset.HASH,cells.size());
        }
        if(r.phase==ForbiddenTreeRecord.Phase.PLACING){
            long deadline=System.nanoTime()+6_000_000L;int budget=cfg.placementBudget;
            while(budget-->0&&r.cursor<cells.size()&&System.nanoTime()<deadline){
                var cell=cells.get(r.cursor);BlockPos p=job.anchor.add(cell.offset());
                if(!allowed(actor,world,p)||!replaceable(world,p,cell,r.repair)){fail(server,"Reserved site modified/denied at "+p.toShortString()+"; explicit same-anchor repair required");return;}
                if(!world.getBlockState(p).equals(cell.state())){world.setBlockState(p,cell.state(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);writtenThisTick++;}
                r.cursor++;ledger.markDirty();
            }
            if(r.cursor==cells.size()){
                world.save(null,true,false);r.complete();ledger.markDirty();checkpoint(server);releaseJob();
                SmoothClasses.LOGGER.info("FORBIDDEN_TREE READY anchor=[{},{},{}] placements={} physical85 selection96 palette53 blocks8271",r.x,r.y,r.z,r.placements);
            }else if(server.getTicks()%20==0)checkpoint(server);
        }
    }
    private static boolean allowed(ServerPlayerEntity p,ServerWorld w,BlockPos at){return DemonProtection.allowCombat(p,w,at)&&net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(w,p,at,w.getBlockState(at),w.getBlockEntity(at));}
    private static boolean replaceable(ServerWorld world,BlockPos p,ForbiddenTreeAsset.Cell cell,boolean repair){
        var current=world.getBlockState(p);if(world.getBlockEntity(p)!=null||current.isOf(Blocks.BEDROCK)||!current.getFluidState().isEmpty())return false;
        if(current.equals(cell.state())||current.isAir())return true;
        if(cell.offset().getY()==0&&cell.state().isOf(Blocks.GRASS_BLOCK)&&current.isOf(Blocks.DIRT)&&record(world.getServer()).anchored)return true;
        if(repair)return true; // Explicit operator command only; no inventories/bedrock/fluids are overwritten.
        return cell.offset().getY()==0&&(current.isOf(Blocks.NETHERRACK)||current.isOf(Blocks.BASALT)||current.isOf(Blocks.BLACKSTONE)||current.isOf(Blocks.SOUL_SOIL)||current.isOf(Blocks.SOUL_SAND));
    }
    public static boolean safeFloor(ServerWorld w,BlockPos p){var s=w.getBlockState(p);return s.isSolidBlock(w,p)&&s.getFluidState().isEmpty()&&!s.isOf(Blocks.MAGMA_BLOCK)&&!s.isOf(Blocks.CAMPFIRE)&&!s.isOf(Blocks.SOUL_CAMPFIRE);}
    private static boolean fallColumnClear(ServerWorld w,BlockPos a){for(int y=1;y<31;y++)if(!w.isAir(a.add(33,y,28)))return false;return w.isSpaceEmpty(new Box(a.getX()+33.15,a.getY()+1,a.getZ()+28.15,a.getX()+33.85,a.getY()+3.1,a.getZ()+28.85));}
    public static boolean recognizable(ServerWorld w,ForbiddenTreeRecord r){BlockPos a=ForbiddenTreeAsset.anchor(r);return r.generated&&r.phase==ForbiddenTreeRecord.Phase.READY&&w.getBlockState(a.add(33,31,28)).isOf(Blocks.ACACIA_WOOD)&&w.getBlockState(a.add(28,9,24)).isOf(Blocks.DIORITE_STAIRS);}
    private static void reject(MinecraftServer s,String reason){var r=record(s);SmoothClasses.LOGGER.info("FORBIDDEN_TREE rejected attempt={} reason={}",r.attempt,reason);r.attempt++;r.phase=ForbiddenTreeRecord.Phase.SEARCHING;DemonWorldState.get(s).markDirty();releaseJob();}
    public static void fail(MinecraftServer s,String reason){var r=record(s);r.fail(reason);DemonWorldState.get(s).markDirty();releaseJob();SmoothClasses.LOGGER.warn("FORBIDDEN_TREE fallback: {}. Anchor reserved={}; never relocate an anchored tree.",reason,r.anchored);}
    public static void checkpoint(MinecraftServer s){s.getOverworld().getPersistentStateManager().save();}
    private static void verifyReservation(MinecraftServer server,ForbiddenTreeRecord r){
        var file=server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("data/smooth_classes_demon.dat");
        try(var in=java.nio.file.Files.newInputStream(file)){var saved=net.minecraft.nbt.NbtIo.readCompressed(in).getCompound("data").getCompound("ForbiddenTree");
            if(!saved.getBoolean("Anchored")||!saved.getString("Phase").equals("PLACING")||saved.getInt("X")!=r.x||saved.getInt("Y")!=r.y||saved.getInt("Z")!=r.z||!saved.getString("Hash").equals(r.hash))throw new IllegalStateException("Reservation read-back differs; refusing first block write");
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot verify durable tree reservation; no first block write",ex);}
    }
    public static boolean repair(ServerPlayerEntity p){var r=record(p.getServer());if(!r.repair())return false;r.requestedBy=p.getUuid();DemonWorldState.get(p.getServer()).markDirty();checkpoint(p.getServer());releaseJob();return true;}
}
