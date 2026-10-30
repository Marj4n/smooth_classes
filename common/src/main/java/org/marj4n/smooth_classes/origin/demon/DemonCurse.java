package org.marj4n.smooth_classes.origin.demon;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.*;
import net.minecraft.util.math.*;
import org.marj4n.smooth_classes.origin.*;
import org.marj4n.smooth_classes.origin.OriginAbilityDispatcher.Result;
import org.joml.Vector3f;

public final class DemonCurse {
    private static final UUID ARMOR=UUID.fromString("993dd1a2-8e48-44b9-a084-4a44213b9e11"), SLOW=UUID.fromString("993dd1a2-8e48-44b9-a084-4a44213b9e12");
    private DemonCurse(){}
    public static Result activate(ServerPlayerEntity p){
        var state=OriginRuntime.state(p);var ledger=DemonWorldState.get(p.getServer());
        UUID token=ledger.mainCasts.get(p.getUuid());
        if(token!=null){
            var cast=ledger.casts.get(token);if(cast!=null){cast.cancelled=true;ledger.markDirty();}
            ledger.mainCasts.remove(p.getUuid());state.progress("demon.brand_cd",0);state.unflag("demon.brand_recall");
            for(var world:p.getServer().getWorlds())for(var entity:world.iterateEntities())if(entity instanceof LivingEntity living&&token.equals(DemonEntityData.of(living).curseOwner))cleanse(living);
            return Result.ok("Brand of Ruin recalled.");
        }
        if(state.progress("demon.brand_cd")>0)return Result.fail("Brand of Ruin is recovering.");
        LivingEntity target=target(p,32);
        if(target==null)return Result.fail("Aim at a living target within 32 blocks.");
        if(!validTarget(p,target))return Result.fail("This target cannot receive your Brand of Ruin.");
        if(DemonEntityData.of(target).curseOwner!=null)return Result.fail("This target is already branded.");
        UUID id=UUID.randomUUID();var cast=new DemonWorldState.Cast(id,p.getUuid(),target.getUuid(),DemonConfig.INSTANCE.propagationBudget);
        ledger.casts.put(id,cast);ledger.mainCasts.put(p.getUuid(),id);ledger.markDirty();
        apply(target,id,curseTier(p),false);state.progress("demon.brand_cd",DemonRules.BRAND);state.flag("demon.brand_recall");
        return Result.ok("Brand of Ruin cast. Activate again to recall.");
    }
    private static int curseTier(ServerPlayerEntity p){return DemonProgression.has(p,"lord")?3:DemonProgression.has(p,"sovereign")?2:DemonProgression.has(p,"netherforged")?1:0;}
    public static boolean validTarget(ServerPlayerEntity caster,LivingEntity target){
        if(target==caster||!target.isAlive()||target.isSpectator()||!target.isAttackable())return false;
        if(target instanceof net.minecraft.entity.player.PlayerEntity player&&DemonProgression.demon(player))return false;
        if(target instanceof ServerPlayerEntity q&&(!caster.getServer().isPvpEnabled()||!caster.shouldDamagePlayer(q)||caster.isTeammate(q)))return false;
        if(caster.getUuid().equals(DemonEntityData.of(target).commander))return false;
        return DemonProtection.allowCombat(caster,caster.getServerWorld(),target.getBlockPos());
    }
    public static LivingEntity target(ServerPlayerEntity p,double range){
        Vec3d start=p.getEyePos(),end=start.add(p.getRotationVec(1F).multiply(range));
        var block=p.getWorld().raycast(new net.minecraft.world.RaycastContext(start,end,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,p));
        double max=start.squaredDistanceTo(block.getPos());LivingEntity selected=null;
        for(LivingEntity e:p.getWorld().getEntitiesByClass(LivingEntity.class,p.getBoundingBox().stretch(end.subtract(start)).expand(1),e->e!=p&&e.isAlive()&&!e.isSpectator()&&e.isAttackable())){
            if(e instanceof ServerPlayerEntity q&&(!p.getServer().isPvpEnabled()||!p.shouldDamagePlayer(q)||p.isTeammate(q)))continue;
            var hit=e.getBoundingBox().expand(.15).raycast(start,end);
            if(hit.isPresent()&&start.squaredDistanceTo(hit.get())<max){max=start.squaredDistanceTo(hit.get());selected=e;}
        }
        return selected;
    }
    private static void apply(LivingEntity e,UUID token,int tier,boolean propagated){var d=DemonEntityData.of(e);d.curseOwner=token;d.curseTier=tier;d.propagated=propagated;modifiers(e);}
    private static void modifiers(LivingEntity e){
        add(e,EntityAttributes.GENERIC_ARMOR,ARMOR,-DemonConfig.INSTANCE.curseArmorReduction);add(e,EntityAttributes.GENERIC_MOVEMENT_SPEED,SLOW,-DemonConfig.INSTANCE.curseSpeedReduction);
    }
    private static void add(LivingEntity e,EntityAttribute attr,UUID id,double amount){var a=e.getAttributeInstance(attr);if(a!=null&&a.getModifier(id)==null)a.addTemporaryModifier(new EntityAttributeModifier(id,"Brand of Ruin",amount,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));}
    /** Angel/other approved Origin integrations call this hook; milk never touches ledger ownership. */
    public static boolean cleanse(LivingEntity e){
        var d=DemonEntityData.of(e);if(d.curseOwner==null)return false;
        if(e.getWorld() instanceof ServerWorld w){var ledger=DemonWorldState.get(w.getServer());var c=ledger.casts.get(d.curseOwner);if(c!=null){c.count=Math.max(0,c.count-1);if(e.getUuid().equals(c.root)){ledger.mainCasts.remove(c.owner,c.id);c.root=null;}if(c.count==0&&c.pending==0)ledger.casts.remove(c.id);ledger.markDirty();}}
        d.curseOwner=null;d.curseTier=0;d.dotTicks=0;
        for(var pair:Map.of(EntityAttributes.GENERIC_ARMOR,ARMOR,EntityAttributes.GENERIC_MOVEMENT_SPEED,SLOW).entrySet()){var a=e.getAttributeInstance(pair.getKey());if(a!=null)a.removeModifier(pair.getValue());}
        return true;
    }
    public static float outgoing(net.minecraft.entity.damage.DamageSource source,float amount){return source.getAttacker() instanceof LivingEntity e&&DemonEntityData.of(e).curseOwner!=null?(float)(amount*(1-DemonConfig.INSTANCE.curseOutgoingReduction)):amount;}
    public static void tick(LivingEntity e){
        var d=DemonEntityData.of(e);if(d.curseOwner==null||!(e.getWorld() instanceof ServerWorld world)||!e.isAlive())return;
        var ledger=DemonWorldState.get(world.getServer());var c=ledger.casts.get(d.curseOwner);
        if(c==null||c.cancelled||(e instanceof net.minecraft.entity.player.PlayerEntity p&&DemonProgression.demon(p))){cleanse(e);return;}
        var owner=world.getServer().getPlayerManager().getPlayer(c.owner);
        if(owner!=null)d.curseTier=Math.max(d.curseTier,curseTier(owner));
        modifiers(e);
        if(e.age%20==0){
            // A small crimson ring reads as a glyph, capped at eight particles per entity/second.
            var dust=new DustParticleEffect(new Vector3f(.6F,.015F,.05F),.8F);
            for(int i=0;i<8;i++){double a=i*Math.PI/4;world.spawnParticles(dust,e.getX()+Math.cos(a)*.4,e.getBodyY(.6),e.getZ()+Math.sin(a)*.4,1,0,0,0,0);}
        }
        if(d.curseTier>=1&&++d.dotTicks>=DemonConfig.INSTANCE.curseDotInterval){d.dotTicks=0;
            if(!DemonProtection.allowPersistentDamage(c.owner,world,e.getBlockPos()))return;
            if(e instanceof ServerPlayerEntity&&(owner==null||!validTarget(owner,e)))return;
            d.damageCredit=c.owner;
            try{e.damage(owner==null?world.getDamageSources().magic():world.getDamageSources().indirectMagic(owner,owner),d.curseTier>=3?DemonConfig.INSTANCE.lordDotDamage:DemonConfig.INSTANCE.curseDotDamage);}
            finally{d.damageCredit=null;}
        }
    }
    public static void onDeath(LivingEntity e){
        var d=DemonEntityData.of(e);if(d.curseOwner==null||!(e.getWorld() instanceof ServerWorld w))return;
        var ledger=DemonWorldState.get(w.getServer());var c=ledger.casts.get(d.curseOwner);
        if(c!=null&&!c.cancelled&&d.curseTier>=2){
            int tier=d.curseTier;int radius=DemonConfig.INSTANCE.propagationRadius;
            var owner=w.getServer().getPlayerManager().getPlayer(c.owner);
            w.spawnParticles(ParticleTypes.FLAME,e.getX(),e.getBodyY(.5),e.getZ(),24,.7,.7,.7,.03);
            w.playSound(null,e.getBlockPos(),net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE,net.minecraft.sound.SoundCategory.HOSTILE,.7F,.8F);
            // Entity-only burst: no terrain griefing, no unbounded vanilla explosion recursion.
            for(var target:w.getEntitiesByClass(LivingEntity.class,e.getBoundingBox().expand(radius),v->v!=e&&v.isAlive())){
                if(target.squaredDistanceTo(e)>radius*radius)continue;
                ledger.pendingBursts.add(new DemonWorldState.Burst(c.id,target.getUuid(),w.getRegistryKey().getValue().toString(),tier,e.getX(),e.getY(),e.getZ()));c.pending++;
            }
            ledger.markDirty();
        }
        cleanse(e);
    }
    public static void tickBursts(net.minecraft.server.MinecraftServer server){
        var ledger=DemonWorldState.get(server);int budget=DemonConfig.INSTANCE.curseBurstWorkBudget;
        while(budget-->0&&!ledger.pendingBursts.isEmpty()){
            var hit=ledger.pendingBursts.remove();var cast=ledger.casts.get(hit.token());if(cast==null)continue;
            cast.pending=Math.max(0,cast.pending-1);
            var key=net.minecraft.util.Identifier.tryParse(hit.dimension());
            var world=key==null?null:server.getWorld(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,key));
            var entity=world==null?null:world.getEntity(hit.target());var owner=server.getPlayerManager().getPlayer(cast.owner);
            if(!cast.cancelled&&entity instanceof LivingEntity target&&target.isAlive()&&target.squaredDistanceTo(hit.x(),hit.y(),hit.z())<=DemonConfig.INSTANCE.propagationRadius*DemonConfig.INSTANCE.propagationRadius
                    &&!(target instanceof net.minecraft.entity.player.PlayerEntity player&&DemonProgression.demon(player))
                    &&!cast.owner.equals(DemonEntityData.of(target).commander)
                    &&DemonProtection.allowPersistentDamage(cast.owner,world,target.getBlockPos())){
                if(!(target instanceof net.minecraft.entity.player.PlayerEntity)||owner!=null&&server.isPvpEnabled()&&validTarget(owner,target)){
                    var data=DemonEntityData.of(target);
                    if(target instanceof net.minecraft.entity.mob.MobEntity&&data.curseOwner==null&&cast.budget>0&&!DemonMobIdentity.boss(target)){
                        cast.budget--;cast.count++;apply(target,cast.id,hit.tier(),true);world.spawnParticles(ParticleTypes.CRIMSON_SPORE,target.getX(),target.getBodyY(.5),target.getZ(),4,.2,.3,.2,0);
                    }
                    data.damageCredit=cast.owner;
                    try{target.damage(world.getDamageSources().explosion(owner,owner),hit.tier()>=3?DemonConfig.INSTANCE.lordExplosionDamage:DemonConfig.INSTANCE.explosionDamage);}
                    finally{data.damageCredit=null;}
                }
            }
            if(cast.count==0&&cast.pending==0)ledger.casts.remove(cast.id);ledger.markDirty();
        }
    }
    public static void tickPlayer(ServerPlayerEntity p){
        var s=OriginRuntime.state(p);if(s.progress("demon.brand_cd")>0)s.progress("demon.brand_cd",s.progress("demon.brand_cd")-1);
        boolean main=DemonWorldState.get(p.getServer()).mainCasts.containsKey(p.getUuid());
        if(main)s.flag("demon.brand_recall");else s.unflag("demon.brand_recall");
    }
}
