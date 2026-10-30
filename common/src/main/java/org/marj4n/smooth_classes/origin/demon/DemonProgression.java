package org.marj4n.smooth_classes.origin.demon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.puffish.skillsmod.api.Skill;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;
import net.minecraft.text.Text;

public final class DemonProgression {
    private DemonProgression(){}
    public static boolean demon(net.minecraft.entity.player.PlayerEntity p){return OriginRuntime.state(p).origin()==OriginType.DEMON;}
    public static boolean has(ServerPlayerEntity p,String key){return OriginRuntime.state(p).hasFlag("demon."+key);}
    public static int tier(ServerPlayerEntity p){return has(p,"lord")?3:has(p,"sovereign")?2:has(p,"recognition")?1:0;}
    public static boolean choose(ServerPlayerEntity p,String path){
        OriginState s=OriginRuntime.state(p);
        if(ForbiddenBirth.active(p)||!demon(p)||!s.hasFlag("demon.cinder_unlocked")||s.hasFlag("demon.world_ender")||!"world_ender".equals(path))return false;
        s.flag("demon.world_ender");sync(p,s);SmoothClassesNetworking.sendOriginState(p);return true;
    }
    public static void onKill(ServerPlayerEntity p,LivingEntity victim){
        recordKill(p.getUuid(),victim);
        if(demon(p))sync(p,OriginRuntime.state(p));
    }
    public static void recordKill(java.util.UUID owner,LivingEntity victim){
        if(victim.getServer()==null)return;
        String id=Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        DemonConfig.INSTANCE.bossEntities.forEach((boss,entity)->{
            if(id.equals(entity)&&(!boss.equals("azazel")||victim.getWorld().getRegistryKey()==World.NETHER)){
                var ledger=DemonWorldState.get(victim.getServer());ledger.historicalKills.computeIfAbsent(owner,ignored->new java.util.HashSet<>()).add(boss);ledger.markDirty();
                var p=victim.getServer().getPlayerManager().getPlayer(owner);if(p!=null&&demon(p))OriginRuntime.state(p).flag("demon.boss."+boss);
            }
        });
    }
    public static void sync(ServerPlayerEntity p,OriginState s){
        if(!demon(p))return;
        DemonBodyGeometry.refresh(p);
        for(String boss:DemonWorldState.get(p.getServer()).historicalKills.getOrDefault(p.getUuid(),java.util.Set.of()))s.flag("demon.boss."+boss);
        // Historical credit uses exact registered entity types, including only
        // Azazel's Nether encounter type, never the separate azazel_human boss.
        DemonConfig.INSTANCE.bossEntities.forEach((boss,id)->{
            Identifier key=Identifier.tryParse(id);
            if(key!=null&&Registries.ENTITY_TYPE.containsId(key)&&p.getStatHandler().getStat(net.minecraft.stat.Stats.KILLED,Registries.ENTITY_TYPE.get(key))>0)s.flag("demon.boss."+boss);
        });
        unlock(p,"origin_base",true);
        unlock(p,"cinder_reflex",s.hasFlag("demon.cinder_unlocked"));
        unlock(p,"choose_path",s.hasFlag("demon.cinder_unlocked"));
        unlock(p,"world_ender",s.hasFlag("demon.world_ender"));
        if(!s.hasFlag("demon.world_ender"))return;
        boolean trial=s.hasFlag("demon.trial.fortress")&&s.hasFlag("demon.trial.bastion");
        unlock(p,"trial",trial);grant(p,s,"brand",trial,"brand_of_ruin");
        var advancement=p.getServer().getAdvancementLoader().get(new Identifier("minecraft:nether/explore_nether"));
        boolean tourist=trial&&advancement!=null&&p.getAdvancementTracker().getProgress(advancement).isDone();
        grant(p,s,"netherforged",tourist,"netherforged");grant(p,s,"pandemonium",tourist,"pandemonium");
        boolean two=tourist&&s.hasFlag("demon.boss.nether_gauntlet")&&s.hasFlag("demon.boss.decaying_king");
        grant(p,s,"recognition",two,"recognition");
        boolean five=two;
        for(String boss:DemonRules.BOSSES)five&=s.hasFlag("demon.boss."+boss);
        grant(p,s,"sovereign",five,"sovereign");grant(p,s,"army",five,"all_hell_shall_kneel");
        unlock(p,"demon_lord",s.hasFlag("demon.lord"));
        for(String boss:DemonRules.BOSSES){
            Identifier id=Identifier.tryParse(DemonConfig.INSTANCE.bossEntities.getOrDefault(boss,""));
            s.progress("demon.requirement."+boss,id!=null&&Registries.ENTITY_TYPE.containsId(id)?1:0);
        }
    }
    private static void grant(ServerPlayerEntity p,OriginState s,String flag,boolean ready,String node){
        if(!ready&&!s.hasFlag("demon."+flag))return;
        if(s.flag("demon."+flag))p.sendMessage(Text.literal("Demon evolution: "+node.replace('_',' ')),false);
        unlock(p,node,true);
    }
    public static void unlock(ServerPlayerEntity p,String id,boolean ready){
        if(!ready)return;
        PuffishSkillsIntegration.category(OriginType.DEMON.categoryId()).ifPresent(c->{
            if(!c.isUnlocked(p))c.unlock(p);
            c.getSkill(id).ifPresent(skill->{if(skill.getState(p)!=Skill.State.UNLOCKED){skill.unlock(p);PuffishSkillsIntegration.invalidateRuntimeCache(p);}});
        });
    }
}
