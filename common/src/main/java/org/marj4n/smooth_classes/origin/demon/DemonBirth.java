package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.marj4n.smooth_classes.origin.*;

/** One first birth per existing ledger UUID. Legacy born players never replay. */
public final class DemonBirth {
    private static final java.util.Map<java.util.UUID,ForbiddenChunkLoad> LOADS=new java.util.HashMap<>();
    public static void release(java.util.UUID id){var lease=LOADS.remove(id);if(lease!=null)lease.close();}
    public static void clear(){for(var id:java.util.List.copyOf(LOADS.keySet()))release(id);}
    private DemonBirth(){}
    public static void begin(ServerPlayerEntity p){if(!DemonWorldState.get(p.getServer()).born.contains(p.getUuid())){var s=OriginRuntime.state(p);s.flag("demon.birth_pending");s.progress("demon.birth_wait",0);s.progress("demon.birth_candidate",0);ForbiddenTreePlacement.request(p);}}
    public static void tick(ServerPlayerEntity p){
        ForbiddenBirth.tick(p);var s=OriginRuntime.state(p);if(!s.hasFlag("demon.birth_pending")||p.age%10!=0)return;
        var ledger=DemonWorldState.get(p.getServer());if(ledger.born.contains(p.getUuid())){s.unflag("demon.birth_pending");return;}
        ForbiddenTreePlacement.request(p);s.progress("demon.birth_wait",s.progress("demon.birth_wait")+10);
        var t=ledger.forbiddenTree;
        if(t.phase==ForbiddenTreeRecord.Phase.READY){if(ForbiddenBirth.start(p))return;fallback(p,"Designated tree modified or safe cinematic landing unavailable");return;}
        if(!ForbiddenBirthConfig.INSTANCE.enabled||t.phase==ForbiddenTreeRecord.Phase.FAILED||s.progress("demon.birth_wait")>=ForbiddenBirthConfig.INSTANCE.waitTicks)fallback(p,t.failure.isEmpty()?"Bounded tree wait expired":t.failure);
        else if(s.progress("demon.birth_wait")%100==0)p.sendMessage(net.minecraft.text.Text.literal("The Forbidden Tree is awakening... "+t.phase),true);
    }
    private static void fallback(ServerPlayerEntity p,String why){
        var s=OriginRuntime.state(p);var world=p.getServer().getWorld(World.NETHER);int candidate=s.progress("demon.birth_candidate");
        if(world!=null&&candidate<ForbiddenBirthConfig.INSTANCE.fallbackAttempts){
            for(int attempt=0;attempt<2&&candidate<ForbiddenBirthConfig.INSTANCE.fallbackAttempts;attempt++){
                int i=candidate;double a=i*2.399963229728653,r=8+Math.sqrt(i)*5;var tree=DemonWorldState.get(p.getServer()).forbiddenTree;
                int x=(tree.anchored?tree.x+33:0)+(int)Math.round(Math.cos(a)*r),z=(tree.anchored?tree.z+28:0)+(int)Math.round(Math.sin(a)*r);var lease=LOADS.get(p.getUuid());if(lease==null){lease=new ForbiddenChunkLoad(world,x>>4,z>>4);LOADS.put(p.getUuid(),lease);}
                try{if(!lease.ready()){s.progress("demon.birth_candidate",candidate);return;}}catch(RuntimeException ex){release(p.getUuid());candidate++;continue;}
                candidate++;
                for(int y=110;y>=6;y--){BlockPos feet=new BlockPos(x,y,z);if(!world.getWorldBorder().contains(feet)||!ForbiddenTreePlacement.safeFloor(world,feet.down())||!world.isAir(feet)||!world.isAir(feet.up())||!world.isAir(feet.up(2))||!DemonProtection.allowCombat(p,world,feet))continue;
                    if(!world.getEntitiesByClass(HostileEntity.class,new Box(feet).expand(10),HostileEntity::isAlive).isEmpty()||!world.isSpaceEmpty(p,new Box(x+.2,y,z+.2,x+.8,y+2.1,z+.8)))continue;
                    release(p.getUuid());p.stopRiding();p.teleport(world,x+.5,y,z+.5,180,0);p.setVelocity(Vec3d.ZERO);p.fallDistance=0;completeFallback(p,why);return;
                }
                release(p.getUuid());
            }s.progress("demon.birth_candidate",candidate);return;
        }
        // Never softlock or build a replacement structure. Retain the current login-safe location.
        completeFallback(p,why+"; bounded Nether fallback exhausted, retained current location");
    }
    private static void completeFallback(ServerPlayerEntity p,String why){release(p.getUuid());var s=OriginRuntime.state(p);s.unflag("demon.birth_pending");s.progress("demon.protection",200);var ledger=DemonWorldState.get(p.getServer());ledger.born.add(p.getUuid());ledger.markDirty();ForbiddenTreePlacement.checkpoint(p.getServer());p.sendMessage(net.minecraft.text.Text.literal("Appleborn: safe birth fallback. "+why),false);org.marj4n.smooth_classes.SmoothClasses.LOGGER.warn("FORBIDDEN_BIRTH FALLBACK uuid={} reason={}; no replacement tree",p.getUuid(),why);}
}
