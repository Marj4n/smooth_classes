package org.marj4n.smooth_classes.content.ruler.runtime;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Box;
import org.joml.Vector3f;
import org.marj4n.smooth_classes.entity.SacredBannerEntity;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.SkillNodeIds;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import java.util.*;

public final class SacredBannerRuntime {
    public static final double RADIUS = 20;
    private static final Map<LivingEntity,Integer> WANTED = new HashMap<>();
    private static final Map<LivingEntity,Map<StatusEffect,Lease>> ACTIVE = new HashMap<>();
    private static final DustParticleEffect BORDER = new DustParticleEffect(new Vector3f(1F,.8F,.22F),1.2F);
    private static final Map<UUID,SacredBannerEntity> BANNERS = new HashMap<>();
    private static boolean applying;

    public static boolean isActive(ServerPlayerEntity player) { return BANNERS.containsKey(player.getUuid()); }
    public static void track(SacredBannerEntity banner) {
        if (banner.owner != null) {
            var old=BANNERS.putIfAbsent(banner.owner,banner);
            if(old!=null && old!=banner) banner.discard();
        }
    }
    public static void ended(SacredBannerEntity banner) {
        if (banner.owner==null || !BANNERS.remove(banner.owner,banner)) return;
        var p=banner.getServer().getPlayerManager().getPlayer(banner.owner);
        if(p!=null) {
            org.marj4n.smooth_classes.runtime.AbilityCooldowns.start(p,
                    org.marj4n.smooth_classes.SmoothClasses.id("sacred_orb"),2400);
            org.marj4n.smooth_classes.network.SmoothClassesNetworking.sendAbilityState(p);
        }
    }
    private static void validateBanners() {
        for(var banner:new ArrayList<>(BANNERS.values())) {
            var p=banner.getServer().getPlayerManager().getPlayer(banner.owner);
            if(banner.isRemoved() || banner.expired() || p==null || !p.isAlive()
                    || p.getWorld()!=banner.getWorld() || p.squaredDistanceTo(banner)>400) {
                ended(banner);
                banner.discard();
            }
        }
    }
    private SacredBannerRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            validateBanners();
            for (var banner : BANNERS.values()) collect(banner);
            reconcile();
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server) -> {
            var banner=BANNERS.get(handler.player.getUuid());
            if(banner!=null) {
                ended(banner);
                org.marj4n.smooth_classes.runtime.AbilityCooldowns.start(handler.player,
                        org.marj4n.smooth_classes.SmoothClasses.id("sacred_orb"),2400);
                banner.discard();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for(var banner:new ArrayList<>(BANNERS.values())) { ended(banner); banner.discard(); }
            WANTED.clear(); reconcile();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ACTIVE.clear(); WANTED.clear(); BANNERS.clear(); });
    }

    public static ExecutionResult cast(ServerPlayerEntity player) {
        if(isActive(player)) return ExecutionResult.failure("Sacred Banner is still active.");
        var world = player.getServerWorld();
        int buffs = 0;
        if (has(player, SkillNodeIds.clericSpecialisationSacredOrbSpeed)) buffs |= 1;
        if (has(player, SkillNodeIds.clericSpecialisationSacredOrbDebuffs)) buffs |= 2;
        if (has(player, SkillNodeIds.clericSpecialisationSacredOrbBuffs)) buffs |= 4;
        var from = player.getPos().add(0, 0.5, 0);
        var ground = world.raycast(new net.minecraft.world.RaycastContext(from, from.add(0, -64, 0),
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
        if (ground.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK
                || ground.getSide() != net.minecraft.util.math.Direction.UP)
            return ExecutionResult.failure("Sacred Banner needs solid ground below you.");
        var anchor = ground.getPos();
        var banner = new SacredBannerEntity(SmoothEntities.SACRED_BANNER, world);
        banner.refreshPositionAndAngles(anchor.x, anchor.y - 0.025, anchor.z, player.getYaw(), 0);
        banner.configure(player.getUuid(), buffs);
        if (!world.spawnEntity(banner)) return ExecutionResult.failure("Cannot summon Sacred Banner here.");
        track(banner);
        return ExecutionResult.success(1, "sacred_orb");
    }
    private static boolean has(ServerPlayerEntity p,String id) {
        return PuffishSkillsIntegration.isSkillUnlocked(PuffishSkillsIntegration.RULER,id,p);
    }
    public static void collect(SacredBannerEntity banner) {
        var world = (ServerWorld) banner.getWorld();
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class,
                banner.getBoundingBox().expand(RADIUS),
                e -> e.isAlive() && !e.isSpectator() && e.squaredDistanceTo(banner) <= RADIUS*RADIUS)) {
            if (eligible(e,banner.owner)) WANTED.merge(e,banner.buffs,(a,b)->a|b);
        }
        if (banner.age % 10 == 0) {
            for (int i=0;i<128;i++) {
                double a=i*Math.PI*2/128;
                world.spawnParticles(BORDER,banner.getX()+Math.cos(a)*RADIUS,
                        banner.getY()+.12,banner.getZ()+Math.sin(a)*RADIUS,1,0,0,0,0);
            }
        }
    }
    private static boolean eligible(LivingEntity e,UUID owner) {
        if (e instanceof net.minecraft.entity.player.PlayerEntity) return true;
        if (e instanceof TameableEntity tame && owner.equals(tame.getOwnerUuid())) return true;
        if (e instanceof AbstractHorseEntity horse && owner.equals(horse.getOwnerUuid())) return true;
        if (e instanceof Ownable owned) {
            Entity master = owned.getOwner();
            if (master != null && owner.equals(master.getUuid())) return true;
            if (master instanceof TameableEntity tame && owner.equals(tame.getOwnerUuid())) return true;
        }
        return false;
    }
    private static void reconcile() {
        Set<LivingEntity> entities = new HashSet<>(ACTIVE.keySet());
        entities.addAll(WANTED.keySet());
        for (LivingEntity e : entities) {
            int mask = e.isRemoved() || !e.isAlive() ? -1 : WANTED.getOrDefault(e,-1);
            var leases = ACTIVE.computeIfAbsent(e,k->new HashMap<>());
            update(e,leases,StatusEffects.REGENERATION,mask>=0,false);
            update(e,leases,StatusEffects.STRENGTH,mask>=0&&(mask&1)!=0,true);
            update(e,leases,StatusEffects.RESISTANCE,mask>=0&&(mask&2)!=0,true);
            update(e,leases,StatusEffects.HASTE,mask>=0&&(mask&4)!=0,true);
            // Vanilla regeneration excludes undead; summoned undead still receive healing.
            if (mask>=0 && e.isUndead() && e.age%12==0) e.heal(1F);
            if (leases.isEmpty()) ACTIVE.remove(e);
        }
        WANTED.clear();
    }
    private static void update(LivingEntity e,Map<StatusEffect,Lease> leases,
                               StatusEffect effect,boolean wanted,boolean additive) {
        Lease lease=leases.get(effect);
        if (!wanted) {
            if (lease!=null) { lease.restore(e,effect); leases.remove(effect); }
            return;
        }
        if (lease==null) { lease=new Lease(e,effect,additive); leases.put(effect,lease); }
        lease.apply(e,effect);
    }

    /** External potions update the baseline, never the already boosted level. */
    public static Boolean externalEffect(LivingEntity e,StatusEffectInstance incoming) {
        if (applying || e.getWorld().isClient) return null;
        var leases=ACTIVE.get(e);
        Lease lease=leases==null?null:leases.get(incoming.getEffectType());
        if (lease==null) return null;
        StatusEffectInstance base=lease.remaining(e);
        if (base==null) base=new StatusEffectInstance(incoming);
        else base.upgrade(incoming);
        lease.base=base; lease.baseTime=e.getWorld().getTime();
        lease.owned=null;
        lease.apply(e,incoming.getEffectType());
        return true;
    }
    private static final class Lease {
        StatusEffectInstance base,owned;
        long baseTime;
        final boolean additive;
        Lease(LivingEntity e,StatusEffect effect,boolean additive) {
            var old=e.getStatusEffect(effect);
            base=old==null?null:new StatusEffectInstance(old);
            baseTime=e.getWorld().getTime(); this.additive=additive;
        }
        StatusEffectInstance remaining(LivingEntity e) {
            if (base==null) return null;
            int duration=base.getDuration()==-1?-1:base.getDuration()-(int)(e.getWorld().getTime()-baseTime);
            if (base.getDuration()!=-1 && duration<=0) return null;
            return new StatusEffectInstance(base.getEffectType(),duration,base.getAmplifier(),
                    base.isAmbient(),base.shouldShowParticles(),base.shouldShowIcon());
        }
        void apply(LivingEntity e,StatusEffect effect) {
            var current=e.getStatusEffect(effect);
            // Milk/cleansing clears the original effect as well.
            if (owned!=null && current==null) { base=null; baseTime=e.getWorld().getTime(); }
            var baseline=remaining(e);
            int amp=additive?(baseline==null?2:baseline.getAmplifier()+3)
                    :Math.max(2,baseline==null?0:baseline.getAmplifier());
            if (current==owned && current!=null && current.getAmplifier()==amp && current.getDuration()>10) return;
            var value=new StatusEffectInstance(effect,30,amp,false,false,true);
            // Hidden original survives logout/unload until normal ticking resumes.
            if (baseline!=null) {
                var nbt=value.writeNbt(new net.minecraft.nbt.NbtCompound());
                nbt.put("HiddenEffect",baseline.writeNbt(new net.minecraft.nbt.NbtCompound()));
                value=StatusEffectInstance.fromNbt(nbt);
            }
            applying=true;
            try { e.removeStatusEffect(effect); e.addStatusEffect(value); owned=e.getStatusEffect(effect); }
            finally { applying=false; }
        }
        void restore(LivingEntity e,StatusEffect effect) {
            if (e.getStatusEffect(effect)!=owned) return;
            var baseline=remaining(e);
            applying=true;
            try { e.removeStatusEffect(effect); if(baseline!=null)e.addStatusEffect(baseline); }
            finally { applying=false; }
        }
    }
}
