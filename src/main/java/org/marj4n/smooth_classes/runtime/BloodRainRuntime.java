package org.marj4n.smooth_classes.runtime;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.SmoothClasses;
import org.marj4n.smooth_classes.entity.BloodRainEntity;
import org.marj4n.smooth_classes.registry.SmoothEntities;
import org.marj4n.smooth_classes.network.SmoothClassesNetworking;

public final class BloodRainRuntime {
    private static final Map<UUID,BloodRainEntity> ACTIVE=new HashMap<>();
    private static final Map<UUID,Integer> HIT_COUNTS = new HashMap<>();
    private static boolean striking;
    private static final Identifier BESMIRCHMENT_SUNSCREEN = new Identifier("besmirchment", "sunscreen");
    private static java.lang.reflect.Method bewitchmentIsVampire;
    private static boolean bewitchmentVampireLookupDone;
    private BloodRainRuntime(){}
    public static boolean shelters(net.minecraft.entity.LivingEntity entity) {
        for (BloodRainEntity e : ACTIVE.values()) {
            if (!e.isRemoved() && e.getWorld() == entity.getWorld()
                    && e.inside(entity.getX()-e.getX(), entity.getZ()-e.getZ())) return true;
        }
        return false;
    }
    public static void confirmedHit(net.minecraft.entity.LivingEntity target, net.minecraft.entity.damage.DamageSource source) {
        if (striking || !(source.getAttacker() instanceof ServerPlayerEntity p)) return;
        BloodRainEntity storm = ACTIVE.get(p.getUuid());
        if (storm == null || !storm.transcendent() || !storm.valid() || target == p
                || target.getWorld() != storm.getWorld() || storm.isOwnedByCaster(target) || target.isTeammate(p)
                || !storm.inside(target.getX()-storm.getX(), target.getZ()-storm.getZ()) || !BloodRainEntity.exposed(target)) return;
        if (target instanceof net.minecraft.entity.player.PlayerEntity other && !p.shouldDamagePlayer(other)) return;
        int hits = HIT_COUNTS.merge(p.getUuid(), 1, Integer::sum);
        if (hits < 5) return;
        HIT_COUNTS.put(p.getUuid(), 0);
        var bolt = EntityType.LIGHTNING_BOLT.create(p.getServerWorld());
        if (bolt != null) { bolt.setPosition(target.getPos()); bolt.setCosmetic(true); p.getServerWorld().spawnEntity(bolt); }
        striking = true;
        try {
            target.timeUntilRegen = 0;
            target.damage(net.spell_power.api.SpellDamageSource.create(net.spell_power.api.SpellSchools.LIGHTNING, p),
                    SpellPowerRuntime.lightning(p, 1.5D));
        } finally { striking = false; }
    }
    public static boolean active(ServerPlayerEntity p){return ACTIVE.containsKey(p.getUuid());}
    public static void track(BloodRainEntity e){if(e.owner!=null){var old=ACTIVE.putIfAbsent(e.owner,e);if(old!=null&&old!=e)e.discard();}}
    public static void ended(BloodRainEntity e) {
        ended(e, null);
    }

    private static void ended(BloodRainEntity e, ServerPlayerEntity fallbackPlayer) {
        if (e.owner == null || !ACTIVE.remove(e.owner, e)) return;
        HIT_COUNTS.remove(e.owner);
        ServerPlayerEntity player = e.getServer().getPlayerManager().getPlayer(e.owner);
        if (player == null) player = fallbackPlayer;
        if (player != null) {
            AbilityCooldowns.start(player, SmoothClasses.id("ascendancy_magic_circle"),
                    AscendancyBalance.cooldownTicks("magic_circle"));
            SmoothClassesNetworking.sendAbilityState(player);
        }
    }
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(ACTIVE.isEmpty())return;
            for(var e:new ArrayList<>(ACTIVE.values()))if(e.isRemoved()||!e.valid()){ended(e);e.discard();}
            // Generic storm shade. Keep the hard extinguish fallback for any vampire
            // implementation that already ignited the player this tick.
            for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                if (shelters(player) && (player.isOnFire() || player.getFireTicks() > 0)) {
                    player.extinguish();
                    player.setFireTicks(0);
                }
            }

            // Besmirchment ships the canonical Vampire Sunscreen effect and already
            // hooks it into Bewitchment's AllowVampireBurn event. Give that effect to
            // a vampire caster while their own Raining Blood is active. This prevents
            // sunlight ignition before damage is produced instead of merely clearing
            // fire after the fact.
            for (BloodRainEntity storm : ACTIVE.values()) {
                if (storm == null || storm.isRemoved() || storm.owner == null || !storm.valid()) continue;
                ServerPlayerEntity caster = s.getPlayerManager().getPlayer(storm.owner);
                if (caster != null && caster.getWorld() == storm.getWorld()
                        && storm.inside(caster.getX() - storm.getX(), caster.getZ() - storm.getZ())) {
                    refreshVampireSunscreen(caster);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{var e=ACTIVE.get(h.player.getUuid());if(e!=null){ended(e,h.player);e.discard();}});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var e:new ArrayList<>(ACTIVE.values())){ended(e);e.discard();}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{ACTIVE.clear(); HIT_COUNTS.clear();});
    }
    public static ExecutionResult cast(ServerPlayerEntity p,int points){
        if(active(p))return ExecutionResult.failure("Raining Blood is still active.");
        var world=p.getServerWorld();
        if (world.getDimension().hasCeiling())
            return ExecutionResult.failure("Raining Blood requires a dimension with an open sky.");
        if(!BloodRainEntity.exposed(p))return ExecutionResult.failure("You must stand under the open sky.");
        var e=SmoothEntities.BLOOD_RAIN.create(world);
        if(e==null)return ExecutionResult.failure("Cannot summon blood rain here.");
        e.setPosition(p.getPos());e.configure(p.getUuid(),points);
        if(!world.spawnEntity(e))return ExecutionResult.failure("Cannot summon blood rain here.");
        track(e);
        refreshVampireSunscreen(p);
        // A vampire may already have a few fire ticks from the sun when the storm
        // is cast. Clear them immediately instead of waiting for END_SERVER_TICK.
        p.extinguish();
        p.setFireTicks(0);
        // Cosmetic lightning: no incidental vanilla damage, fire, or block changes.
        var hit=p.raycast(64,1,false);
        var bolt=EntityType.LIGHTNING_BOLT.create(world);
        if(bolt!=null){bolt.setPosition(hit.getPos());bolt.setCosmetic(true);world.spawnEntity(bolt);}
        return ExecutionResult.success(1,"magic_circle");
    }
    /**
     * Applies Besmirchment's real sunscreen effect without taking a hard dependency
     * on either Bewitchment or Besmirchment. Their own AllowVampireBurn listener is
     * then responsible for denying sunlight burn exactly as if the player consumed
     * Vampire Sunscreen normally.
     */
    private static void refreshVampireSunscreen(ServerPlayerEntity player) {
        var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
        if (!loader.isModLoaded("bewitchment") || !loader.isModLoaded("besmirchment")) return;
        if (!Registries.STATUS_EFFECT.containsId(BESMIRCHMENT_SUNSCREEN)) return;

        // Prefer Bewitchment's own API to avoid showing Sunscreen on non-vampires.
        // If a particular Bewitchment build moves that API, fail safe: the
        // Besmirchment effect itself is harmless to a non-vampire, while omitting
        // it would leave a vampire burning.
        Boolean vampire = isBewitchmentVampire(player);
        if (Boolean.FALSE.equals(vampire)) return;

        StatusEffect sunscreen = Registries.STATUS_EFFECT.get(BESMIRCHMENT_SUNSCREEN);
        StatusEffectInstance current = player.getStatusEffect(sunscreen);
        // Do not overwrite a real, long-duration sunscreen the player consumed.
        // Our temporary copy is refreshed only as it gets close to expiring.
        if (current == null || current.getDuration() < 15) {
            player.addStatusEffect(new StatusEffectInstance(sunscreen, 40, 0, true, true, true));
        }
    }

    private static Boolean isBewitchmentVampire(ServerPlayerEntity player) {
        if (!bewitchmentVampireLookupDone) {
            bewitchmentVampireLookupDone = true;
            try {
                Class<?> api = Class.forName("moriyashiine.bewitchment.api.BewitchmentAPI");
                bewitchmentIsVampire = api.getMethod("isVampire", Entity.class, boolean.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                SmoothClasses.LOGGER.warn("Could not resolve optional Bewitchment vampire API for Raining Blood sunscreen.", ex);
            }
        }
        if (bewitchmentIsVampire == null) return null;
        try {
            return Boolean.TRUE.equals(bewitchmentIsVampire.invoke(null, player, true));
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

}
