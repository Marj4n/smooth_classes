package org.marj4n.smooth_classes.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_classes.effects.SmoothEffects;
import org.marj4n.smooth_classes.SmoothClasses;
import java.lang.reflect.Method;

/**
 * Optional-mod boundary. This class deliberately has no compile-time imports from
 * optional mods, so Smooth Classes remains loadable when every optional dependency
 * is absent.
 */
public final class OptionalCompatRuntime {
    private static final FabricLoader LOADER = FabricLoader.getInstance();
    private static final boolean SIMPLY_SWORDS = LOADER.isModLoaded("simplyswords")
            && LOADER.getModContainer("simplyswords").map(c -> {
                String version = c.getMetadata().getVersion().toString();
                return !version.contains("1.50") && !version.contains("1.48");
            }).orElse(false);
    private static final boolean PALADINS = LOADER.isModLoaded("paladins");
    private static final boolean ARCHERS = LOADER.isModLoaded("archers");
    private static final boolean IMMERSIVE_MELODIES = LOADER.isModLoaded("immersive_melodies");
    private static final boolean OPAC = LOADER.isModLoaded("openpartiesandclaims");

    private OptionalCompatRuntime(){}

    public static boolean loaded(String id){ return LOADER.isModLoaded(id); }
    public static boolean simplySwords(){ return SIMPLY_SWORDS; }
    public static boolean paladins(){ return PALADINS; }
    public static boolean archers(){ return ARCHERS; }
    public static boolean immersiveMelodies(){ return IMMERSIVE_MELODIES; }
    public static boolean opac(){ return OPAC; }

    /**
     * Simply Swords socket read without linking its API.
     * Simply Swords stores its socket power in the item's nether_power NBT.
     */
    public static boolean hasNetherPower(PlayerEntity player,String power){
        if(!simplySwords())return false;
        return containsPower(player.getMainHandStack(),power)
                || (!power.equals("spellforged") && containsPower(player.getOffHandStack(),power));
    }
    private static boolean containsPower(ItemStack stack,String power){
        return stack.getItem() instanceof SwordItem && stack.hasNbt() && stack.getNbt()!=null
                && stack.getNbt().getString("nether_power").contains(power);
    }

    /** Generic signature-gem procs that don't require Simply Swords API classes. */
    public static void onSignatureAbility(ServerPlayerEntity p){
        if(!simplySwords())return;
        if(hasNetherPower(p,"precise") && p.getRandom().nextInt(100)<15)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.PRECISION,200,5,false,false,true));
        if(hasNetherPower(p,"mighty") && p.getRandom().nextInt(100)<15)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.MIGHT,200,3,false,false,true));
        if(hasNetherPower(p,"stealthy") && p.getRandom().nextInt(100)<15)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.STEALTH,600,0,false,false,true));
    }

    public static void onSpellCast(ServerPlayerEntity p){
        if(hasNetherPower(p,"spellshield") && p.getRandom().nextInt(100)<15)
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.BARRIER,100,0,false,false,true));
        if (hasNetherPower(p, "spell_Standard") && p.getRandom().nextInt(100)<10
                && !hasOwnSpellStandard(p)) {
            if (spawnStandard(p, -2, "smooth_classes:precision", "smooth_classes:spellforged",
                    0, null, null)) playCompatibilitySound(p);
        }
    }

    /** Simply Swords API is resolved only while the optional mod is installed. */
    private static boolean spawnStandard(PlayerEntity p, int duration,
                                         String positive, String secondary, int negativeRadius,
                                         String negative, String negativeSecondary) {
        if (!simplySwords()) return false;
        try {
            Class<?> api=Class.forName("net.sweenus.simplyswords.api.SimplySwordsAPI");
            for (Method method:api.getMethods()) {
                if (!method.getName().equals("spawnBattleStandard") || method.getParameterCount()!=13) continue;
                method.invoke(null,p,3,"api",3,duration,positive,secondary,negativeRadius,
                        negative,negativeSecondary,0,false,false);
                return true;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            SmoothClasses.LOGGER.warn("Simply Swords Battle Standard API unavailable", ex);
        }
        return false;
    }

    private static boolean hasOwnSpellStandard(PlayerEntity p) {
        for (net.minecraft.entity.Entity entity:p.getWorld().getOtherEntities(p,p.getBoundingBox().expand(20),
                e->e.getClass().getName().equals("net.sweenus.simplyswords.entity.BattleStandardEntity"))) {
            try {
                Class<?> type=entity.getClass();
                if (type.getField("ownerEntity").get(entity)==p
                        && String.valueOf(type.getField("positiveEffect").get(entity)).contains("smooth_classes:precision")
                        && String.valueOf(type.getField("positiveEffectSecondary").get(entity)).contains("smooth_classes:spellforged")) return true;
            } catch (ReflectiveOperationException ignored) { }
        }
        return false;
    }

    public static void onChannelEnd(ServerPlayerEntity p) {
        if (hasNetherPower(p,"war_standard") && spawnStandard(p,3,"smooth_classes:might",null,
                4,"smooth_classes:revealed",null)) playCompatibilitySound(p);
    }

    public static void tick(ServerPlayerEntity p){
        if(p.age%20!=0)return;
        if(hasNetherPower(p,"spellforged"))
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.SPELLFORGED,25,0,false,false,true));
        if(hasNetherPower(p,"soulshock"))
            p.addStatusEffect(new StatusEffectInstance(SmoothEffects.SOULSHOCK,25,0,false,false,true));
        // Paladins/Archers/Immersive Melodies are detected here but intentionally
        // not linked by class name; their APIs remain genuinely optional.
    }

    /** Socket effects applied to signature cooldown before haste. */
    public static int signatureCooldown(ServerPlayerEntity player, int ticks) {
        if (hasNetherPower(player, "renewed") && player.getRandom().nextInt(100) < 15) {
            playCompatibilitySound(player);
            ticks = Math.max(20, org.marj4n.smooth_classes.config.SmoothBalance.General.minimumAchievableCooldown * 20);
        }
        if (hasNetherPower(player, "accelerant")) {
            playCompatibilitySound(player);
            ticks = Math.max(1, ticks - 240);
        }
        return ticks;
    }

    public static void onEvasion(ServerPlayerEntity player) {
        if (player.hasStatusEffect(SmoothEffects.REVEALED) && hasNetherPower(player, "deception")
                && player.getRandom().nextBoolean()) {
            player.removeStatusEffect(SmoothEffects.REVEALED);
            playCompatibilitySound(player);
        }
    }

    private static void playCompatibilitySound(PlayerEntity player) {
        org.marj4n.smooth_classes.runtime.SkillFx.sound(player, "fx_ui_unlock3", 1F, 1.6F);
    }

    /**
     * Safe baseline friendly-fire contract. OPAC-specific party/alliance checks are
     * isolated behind the optional boundary; vanilla teams remain authoritative
     * when OPAC is absent or its API isn't linked.
     */
    public static boolean canHarm(ServerPlayerEntity source,LivingEntity target){
        if(target==source)return false;
        if(source.isTeammate(target))return false;
        if(target instanceof PlayerEntity other)return other.shouldDamagePlayer(source);
        if(target instanceof net.minecraft.entity.passive.TameableEntity pet
                && pet.getOwner() instanceof PlayerEntity owner)
            return owner!=source && source.shouldDamagePlayer(owner);
        return true;
    }
}
