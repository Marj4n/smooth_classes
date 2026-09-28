package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.spell_engine.entity.SpellProjectile;
import org.marj4n.smooth_classes.effects.SmoothEffects;

/** A hammer is ammunition: only the owner's successful direct hit launches one. */
public final class RighteousHammerChargeRuntime {
    private RighteousHammerChargeRuntime() {}
    private static final ThreadLocal<Boolean> PASSIVE_HIT=ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> LAUNCHING=ThreadLocal.withInitial(() -> false);
    private record HitKey(java.util.UUID owner, java.util.UUID target) {}
    private static final java.util.Set<HitKey> HITS_THIS_TICK=new java.util.HashSet<>();
    private static long hitTick=Long.MIN_VALUE;

    public static void passiveHit(ServerPlayerEntity owner, LivingEntity target, float damage) {
        PASSIVE_HIT.set(true);
        try { target.damage(owner.getDamageSources().playerAttack(owner), damage); }
        finally { PASSIVE_HIT.set(false); }
    }

    public static void onLandedHit(ServerPlayerEntity owner, LivingEntity target, DamageSource source) {
        if (PASSIVE_HIT.get() || LAUNCHING.get()) return;
        if (source.getSource() instanceof SpellProjectile projectile && projectile.getSpellEntry()!=null
                && projectile.getSpellEntry().getKey().map(key -> key.getValue().getPath()
                        .contains("righteous_hammer_projectile")).orElse(false)) return;
        onTriggeredHit(owner,target);
    }

    public static void onTriggeredHit(ServerPlayerEntity owner, LivingEntity target) {
        if (PASSIVE_HIT.get() || LAUNCHING.get() || !owner.isAlive() || !target.isAlive()
                || !owner.hasStatusEffect(SmoothEffects.RIGHTEOUS_HAMMERS)) return;
        if (target.isTeammate(owner)
                || target instanceof net.minecraft.entity.passive.TameableEntity tame && tame.isOwner(owner)) return;
        long tick=owner.getWorld().getTime();
        if (tick!=hitTick) { HITS_THIS_TICK.clear(); hitTick=tick; }
        if (!HITS_THIS_TICK.add(new HitKey(owner.getUuid(),target.getUuid()))) return;
        StatusEffectInstance active=owner.getStatusEffect(SmoothEffects.RIGHTEOUS_HAMMERS);
        if (active==null) return;
        int total=active.getAmplifier()+1;
        int pts=AscendancyRuntime.points(owner);
        int wanted=pts>=60?2:1;
        float multiplier=pts>=60?1.50F:pts>=30?1.25F:1.0F;
        int launchedCount=0;
        LAUNCHING.set(true);
        try {
            for(int j=0;j<Math.min(wanted,total);j++){
                var before=owner.getWorld().getEntitiesByClass(SpellProjectile.class,
                        owner.getBoundingBox().expand(6), p->p.getOwner()==owner).stream()
                        .map(net.minecraft.entity.Entity::getUuid).collect(java.util.stream.Collectors.toSet());
                boolean launched=InternalSpellRuntime.target(owner,"smooth_classes:righteous_hammer_projectile",target,multiplier);
                if(!launched)break;
                launchedCount++;
                double angle=Math.toRadians(owner.getWorld().getTime()*9.0-45.0+j*(360.0/Math.max(1,total)));
                double x=owner.getX()-Math.sin(angle)*3.0;
                double y=owner.getY()+owner.getHeight()*0.5;
                double z=owner.getZ()-Math.cos(angle)*3.0;
                for (SpellProjectile projectile:owner.getWorld().getEntitiesByClass(SpellProjectile.class,
                        owner.getBoundingBox().expand(6), p->p.getOwner()==owner && p.age<=1
                                && !before.contains(p.getUuid()) && p.getSpellEntry()!=null
                                && p.getSpellEntry().getKey().map(k -> k.getValue().getPath()
                                        .equals("righteous_hammer_projectile")).orElse(false))) {
                    projectile.teleport(x,y,z);
                    projectile.setFollowedTarget(target);
                    var direction=target.getEyePos().subtract(x,y,z).normalize();
                    projectile.setVelocity(direction.multiply(pts>=60?1.8:pts>=30?1.5:1.2));
                    projectile.velocityModified=true;
                }
            }
        } finally { LAUNCHING.set(false); }
        if (launchedCount<=0) return;
        int left=total-launchedCount;
        owner.removeStatusEffect(SmoothEffects.RIGHTEOUS_HAMMERS);
        if(left>0)owner.addStatusEffect(new StatusEffectInstance(SmoothEffects.RIGHTEOUS_HAMMERS,
                active.getDuration(),left-1,false,false,true));
    }
}
