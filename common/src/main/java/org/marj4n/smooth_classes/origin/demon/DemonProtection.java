package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import java.lang.reflect.Method;
import java.util.*;
import org.marj4n.smooth_classes.SmoothClasses;

/** Public extension point: claim/region adapters may veto all destructive domain writes. */
public final class DemonProtection {
    public interface Rule { boolean allow(ServerPlayerEntity player,ServerWorld world,BlockPos pos); }
    private static final List<Rule> RULES=new ArrayList<>();
    private static Method opacGet,opacProtection,opacAccess;
    private static boolean opacFailed;
    private DemonProtection(){}
    public static void register(Rule rule){RULES.add(Objects.requireNonNull(rule));}
    public static boolean allowPersistentDamage(UUID owner,ServerWorld world,BlockPos pos){
        var player=world.getServer().getPlayerManager().getPlayer(owner);if(player!=null)return allowCombat(player,world,pos);
        if(opacFailed||!RULES.isEmpty()||!world.getWorldBorder().contains(pos))return false;
        if(world.getRegistryKey()==net.minecraft.world.World.OVERWORLD&&Math.max(Math.abs(pos.getX()-world.getSpawnPos().getX()),Math.abs(pos.getZ()-world.getSpawnPos().getZ()))<=world.getServer().getSpawnProtectionRadius())return false;
        if(opacGet!=null)try{Object api=opacGet.invoke(null,world.getServer());return (boolean)opacAccess.invoke(opacProtection.invoke(api),owner,world.getRegistryKey().getValue(),pos.getX()>>4,pos.getZ()>>4);}
        catch(ReflectiveOperationException ex){opacFailed=true;SmoothClasses.LOGGER.error("Demon persistent damage disabled after OpenPAC failure",ex);return false;}
        return true;
    }
    public static void initialize(){
        // Match the actual loaded API signatures; no Fabric-side import of Forge classes.
        if(!org.marj4n.smooth_classes.platform.PlatformLoader.getInstance().isModLoaded("openpartiesandclaims"))return;
        try{
            Class<?> api=Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
            opacGet=api.getMethod("get",net.minecraft.server.MinecraftServer.class);
            opacProtection=api.getMethod("getChunkProtection");
            opacAccess=opacProtection.getReturnType().getMethod("hasChunkAccess",UUID.class,Identifier.class,int.class,int.class);
        }catch(ReflectiveOperationException ex){opacFailed=true;SmoothClasses.LOGGER.error("Demon domain writes disabled: OpenPAC API unavailable",ex);}
    }
    public static boolean allowCombat(ServerPlayerEntity p,ServerWorld w,BlockPos pos){
        if(p==null)return false;
        if(!w.getWorldBorder().contains(pos)||!w.canPlayerModifyAt(p,pos)||w.getServer().isSpawnProtected(w,pos,p)||opacFailed)return false;

        if(opacGet!=null){
            try{
                Object api=opacGet.invoke(null,w.getServer());Object protection=opacProtection.invoke(api);
                if(!(boolean)opacAccess.invoke(protection,p.getUuid(),w.getRegistryKey().getValue(),pos.getX()>>4,pos.getZ()>>4))return false;
            }catch(ReflectiveOperationException ex){opacFailed=true;SmoothClasses.LOGGER.error("Demon domain writes disabled after OpenPAC failure",ex);return false;}
        }
        for(Rule rule:RULES)if(!rule.allow(p,w,pos))return false;
        return true;
    }
    public static boolean allow(ServerPlayerEntity p,ServerWorld w,BlockPos pos){
        if(!allowCombat(p,w,pos))return false;
        if(w.getServer().isDedicated()&&!DemonConfig.INSTANCE.multiplayerTerrainChanges)return false;
        var state=w.getBlockState(pos);
        return net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(w,p,pos,state,w.getBlockEntity(pos));
    }
}
