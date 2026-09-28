package org.marj4n.smooth_classes.gameplay;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_classes.content.archer.runtime.ArcherRuntime;
import org.marj4n.smooth_classes.content.assassin.runtime.AssassinRuntime;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSummonPlan;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerSummoningRuntime;
import org.marj4n.smooth_classes.content.avenger.runtime.AvengerMinionGameplay;
import org.marj4n.smooth_classes.content.berserker.runtime.BerserkerRuntime;
import org.marj4n.smooth_classes.content.caster.runtime.CasterRuntime;
import org.marj4n.smooth_classes.content.foreigner.runtime.ForeignerRuntime;
import org.marj4n.smooth_classes.content.lancer.runtime.LancerRuntime;
import org.marj4n.smooth_classes.content.rider.runtime.RiderRuntime;
import org.marj4n.smooth_classes.content.ruler.runtime.RulerRuntime;
import org.marj4n.smooth_classes.content.saber.runtime.SaberRuntime;
import org.marj4n.smooth_classes.integration.PuffishSkillsIntegration;
import org.marj4n.smooth_classes.integration.OptionalCompatRuntime;
import org.marj4n.smooth_classes.runtime.ExecutionResult;
import org.marj4n.smooth_classes.runtime.AbilityCooldowns;

/** Resolves the signature node selected in Puffish Skills and dispatches it to our clean class runtime. */
public final class SignatureAbilityDispatcher {
    private SignatureAbilityDispatcher() {}

    public record DispatchResult(boolean success, String ability, String message) {
        public static DispatchResult fail(String message) { return new DispatchResult(false, "none", message); }
    }

    public static DispatchResult cast(ServerPlayerEntity player) {
        if(org.marj4n.smooth_classes.runtime.WhenOnHighRuntime.active(player))return DispatchResult.fail("When On High is still channeling.");
        try {
            if (clazz(player, "caster")) {
                if (skill(player,"caster","is053f9imz801s57")) return result(player, "meteor_shower", () -> CasterRuntime.executeMeteorShower(player));
                if (skill(player,"caster","kp8uei8ppni71b5x")) return result(player, "ice_comet", () -> CasterRuntime.executeIceComet(player));
                if (skill(player,"caster","a3ns9xl58ixdg2lo")) return result(player, "static_discharge", () -> CasterRuntime.executeStaticDischarge(player));
                if (skill(player,"caster","7peocg44cggilh9z")) return result(player, "arcane_bolt", () -> CasterRuntime.executeArcaneBolt(player));
            }
            if (clazz(player, "berserker")) {
                if (skill(player,"berserker","kp8uei8ppni71b5x")) return result(player, "rampage", () -> BerserkerRuntime.executeRampage(player));
                if (skill(player,"berserker","a3ns9xl58ixdg2lo")) return result(player, "bloodthirsty", () -> BerserkerRuntime.executeBloodthirsty(player));
                if (skill(player,"berserker","is053f9imz801s57")) return result(player, "berserking", () -> BerserkerRuntime.executeBerserking(player));
            }
            if (clazz(player, "assassin")) {
                if (skill(player,"assassin","is053f9imz801s57")) return result(player, "evasion", () -> AssassinRuntime.executeEvasion(player));
                if (skill(player,"assassin","kp8uei8ppni71b5x")) return result(player, "preparation", () -> AssassinRuntime.executePreparation(player));
                if (skill(player,"assassin","a3ns9xl58ixdg2lo")) return result(player, "siphoning_strikes", () -> AssassinRuntime.executeSiphoningStrikes(player));
            }
            if (clazz(player, "archer")) {
                if (skill(player,"archer","is053f9imz801s57")) return result(player, "disengage", () -> ArcherRuntime.executeDisengage(player));
                if (skill(player,"archer","a3ns9xl58ixdg2lo")) return result(player, "arrow_rain", () -> ArcherRuntime.executeArrowRain(player));
                if (skill(player,"archer","kp8uei8ppni71b5x")) return result(player, "elemental_arrows", () -> ArcherRuntime.executeElementalArrows(player));
            }
            if (clazz(player, "foreigner")) {
                if (skill(player,"foreigner","a3ns9xl58ixdg2lo")) return result(player, "elemental_surge", () -> ForeignerRuntime.executeElementalSurge(player));
                if (skill(player,"foreigner","kp8uei8ppni71b5x")) return result(player, "elemental_impact", () -> ForeignerRuntime.executeElementalImpact(player));
                if (skill(player,"foreigner","is053f9imz801s57")) return result(player, "spellweaver", () -> ForeignerRuntime.executeSpellweaver(player));
            }
            if (clazz(player, "saber")) {
                if (skill(player,"saber","is053f9imz801s57")) return result(player, "consecration", () -> SaberRuntime.executeConsecration(player));
                if (skill(player,"saber","kp8uei8ppni71b5x")) return result(player, "sacred_onslaught", () -> SaberRuntime.executeSacredOnslaught(player));
                if (skill(player,"saber","a3ns9xl58ixdg2lo")) return result(player, "heavensmiths_call", () -> SaberRuntime.executeHeavensmithsCall(player));
            }
            if (clazz(player, "ruler")) {
                if (skill(player,"ruler","is053f9imz801s57")) return result(player, "sacred_orb", () -> RulerRuntime.executeSacredOrb(player));
                if (skill(player,"ruler","a3ns9xl58ixdg2lo")) return result(player, "divine_intervention", () -> RulerRuntime.executeDivineIntervention(player));
                if (skill(player,"ruler","kp8uei8ppni71b5x")) return result(player, "anoint_weapon", () -> RulerRuntime.executeAnointWeapon(player));
            }
            if (clazz(player, "rider")) {
                if (skill(player,"rider","rider_charge")) return result(player, "rider_charge", () -> RiderRuntime.executeCharge(player));
                if (skill(player,"rider","rider_war_aura")) return result(player, "rider_war_aura", () -> RiderRuntime.executeWarAura(player));
                if (skill(player,"rider","rider_blazing_hooves")) return result(player, "rider_blazing_hooves", () -> RiderRuntime.executeBlazingHooves(player));
            }
            if (clazz(player, "lancer")) {
                if (skill(player,"lancer","lancer_impaling_volley")) return result(player, "impaling_volley", () -> LancerRuntime.executeImpalingVolley(player));
                if (skill(player,"lancer","lancer_dragon_thrust")) return result(player, "dragon_thrust", () -> LancerRuntime.executeDragonThrust(player));
                if (skill(player,"lancer","lancer_spearstorm")) return result(player, "spearstorm", () -> LancerRuntime.executeSpearstorm(player));
            }
            if (clazz(player, "avenger") && skill(player,"avenger","yl0wtsb5m85wmvfa")) {
                Identifier abilityId = new Identifier("smooth_classes", "summoning_ritual");
                long remaining = AbilityCooldowns.remainingTicks(player, abilityId);
                if (remaining > 0) return DispatchResult.fail("summoning_ritual cooldown " + formatSeconds(remaining) + "s");
                AvengerSummonPlan plan = AvengerSummoningRuntime.resolve(player);
                int spawned = AvengerMinionGameplay.summon(player, plan);
                if (spawned <= 0) return DispatchResult.fail("summoning_ritual could not spawn minions");
                AbilityCooldowns.start(player, abilityId, AbilityCooldowns.adjustedTicks(player,
                        OptionalCompatRuntime.signatureCooldown(player, SignatureCooldowns.ticks("summoning_ritual"))));
                return new DispatchResult(true, "summoning_ritual", "summoned " + spawned + " minion(s)");
            }
            return DispatchResult.fail("No unlocked signature ability found in your active Puffish class tree.");
        } catch (IllegalStateException e) {
            return DispatchResult.fail(e.getMessage());
        }
    }


    /** Playtest entrypoint: cast one exact ability while still enforcing Puffish unlock + cooldown. */
    public static DispatchResult castNamed(ServerPlayerEntity player, String ability) {
        if(org.marj4n.smooth_classes.runtime.WhenOnHighRuntime.active(player))return DispatchResult.fail("When On High is still channeling.");
        String a = ability.toLowerCase(java.util.Locale.ROOT);

        try {
            return switch (a) {
                case "meteor_shower" -> unlocked(player,"caster","is053f9imz801s57",a,()->CasterRuntime.executeMeteorShower(player));
                case "ice_comet" -> unlocked(player,"caster","kp8uei8ppni71b5x",a,()->CasterRuntime.executeIceComet(player));
                case "static_discharge" -> unlocked(player,"caster","a3ns9xl58ixdg2lo",a,()->CasterRuntime.executeStaticDischarge(player));
                case "arcane_bolt" -> unlocked(player,"caster","7peocg44cggilh9z",a,()->CasterRuntime.executeArcaneBolt(player));

                case "rampage" -> unlocked(player,"berserker","kp8uei8ppni71b5x",a,()->BerserkerRuntime.executeRampage(player));
                case "bloodthirsty" -> unlocked(player,"berserker","a3ns9xl58ixdg2lo",a,()->BerserkerRuntime.executeBloodthirsty(player));
                case "berserking" -> unlocked(player,"berserker","is053f9imz801s57",a,()->BerserkerRuntime.executeBerserking(player));

                case "evasion" -> unlocked(player,"assassin","is053f9imz801s57",a,()->AssassinRuntime.executeEvasion(player));
                case "preparation" -> unlocked(player,"assassin","kp8uei8ppni71b5x",a,()->AssassinRuntime.executePreparation(player));
                case "siphoning_strikes" -> unlocked(player,"assassin","a3ns9xl58ixdg2lo",a,()->AssassinRuntime.executeSiphoningStrikes(player));

                case "disengage" -> unlocked(player,"archer","is053f9imz801s57",a,()->ArcherRuntime.executeDisengage(player));
                case "arrow_rain" -> unlocked(player,"archer","a3ns9xl58ixdg2lo",a,()->ArcherRuntime.executeArrowRain(player));
                case "elemental_arrows" -> unlocked(player,"archer","kp8uei8ppni71b5x",a,()->ArcherRuntime.executeElementalArrows(player));

                case "elemental_surge" -> unlocked(player,"foreigner","a3ns9xl58ixdg2lo",a,()->ForeignerRuntime.executeElementalSurge(player));
                case "elemental_impact" -> unlocked(player,"foreigner","kp8uei8ppni71b5x",a,()->ForeignerRuntime.executeElementalImpact(player));
                case "spellweaver" -> unlocked(player,"foreigner","is053f9imz801s57",a,()->ForeignerRuntime.executeSpellweaver(player));

                case "consecration" -> unlocked(player,"saber","is053f9imz801s57",a,()->SaberRuntime.executeConsecration(player));
                case "sacred_onslaught" -> unlocked(player,"saber","kp8uei8ppni71b5x",a,()->SaberRuntime.executeSacredOnslaught(player));
                case "heavensmiths_call" -> unlocked(player,"saber","a3ns9xl58ixdg2lo",a,()->SaberRuntime.executeHeavensmithsCall(player));

                case "sacred_orb" -> unlocked(player,"ruler","is053f9imz801s57",a,()->RulerRuntime.executeSacredOrb(player));
                case "divine_intervention" -> unlocked(player,"ruler","a3ns9xl58ixdg2lo",a,()->RulerRuntime.executeDivineIntervention(player));
                case "anoint_weapon" -> unlocked(player,"ruler","kp8uei8ppni71b5x",a,()->RulerRuntime.executeAnointWeapon(player));

                case "rider_charge" -> unlocked(player,"rider","rider_charge",a,()->RiderRuntime.executeCharge(player));
                case "rider_war_aura" -> unlocked(player,"rider","rider_war_aura",a,()->RiderRuntime.executeWarAura(player));
                case "rider_blazing_hooves" -> unlocked(player,"rider","rider_blazing_hooves",a,()->RiderRuntime.executeBlazingHooves(player));

                case "impaling_volley" -> unlocked(player,"lancer","lancer_impaling_volley",a,()->LancerRuntime.executeImpalingVolley(player));
                case "dragon_thrust" -> unlocked(player,"lancer","lancer_dragon_thrust",a,()->LancerRuntime.executeDragonThrust(player));
                case "spearstorm" -> unlocked(player,"lancer","lancer_spearstorm",a,()->LancerRuntime.executeSpearstorm(player));

                case "summoning_ritual" -> castSummoningRitual(player);
                default -> DispatchResult.fail("Unknown ability: " + ability);
            };
        } catch (IllegalStateException e) {
            return DispatchResult.fail(e.getMessage());
        }
    }

    private static DispatchResult unlocked(ServerPlayerEntity player, String category, String skillId,
                                           String ability, CastAction action) {
        if (!clazz(player, category)) return DispatchResult.fail("Active Puffish class is not " + category + ".");
        if (!skill(player, category, skillId)) return DispatchResult.fail(ability + " is not unlocked in Puffish Skills.");
        return result(player, ability, action);
    }

    private static DispatchResult castSummoningRitual(ServerPlayerEntity player) {
        if (!clazz(player, "avenger")) return DispatchResult.fail("Active Puffish class is not avenger.");
        if (!skill(player,"avenger","yl0wtsb5m85wmvfa")) return DispatchResult.fail("summoning_ritual is not unlocked in Puffish Skills.");
        Identifier abilityId = new Identifier("smooth_classes", "summoning_ritual");
        long remaining = AbilityCooldowns.remainingTicks(player, abilityId);
        if (remaining > 0) return DispatchResult.fail("summoning_ritual cooldown " + formatSeconds(remaining) + "s");
        AvengerSummonPlan plan = AvengerSummoningRuntime.resolve(player);
        int spawned = AvengerMinionGameplay.summon(player, plan);
        if (spawned <= 0) return DispatchResult.fail("summoning_ritual could not spawn minions");
        AbilityCooldowns.start(player, abilityId, AbilityCooldowns.adjustedTicks(player,
                OptionalCompatRuntime.signatureCooldown(player, SignatureCooldowns.ticks("summoning_ritual"))));
        return new DispatchResult(true, "summoning_ritual", "summoned " + spawned + " minion(s)");
    }

    /** HUD slot selection: exactly one active signature is displayed/cast. */
    public static String selectedAbility(ServerPlayerEntity player) {
        java.util.List<String> unlocked = unlockedAbilities(player);
        return unlocked.isEmpty() ? "" : unlocked.get(0);
    }

    /** Human-readable playtest inventory for /smoothclasses status. */
    public static java.util.List<String> unlockedAbilities(ServerPlayerEntity player) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String[][] rows = {
            {"caster","is053f9imz801s57","meteor_shower"},{"caster","kp8uei8ppni71b5x","ice_comet"},{"caster","a3ns9xl58ixdg2lo","static_discharge"},{"caster","7peocg44cggilh9z","arcane_bolt"},
            {"berserker","kp8uei8ppni71b5x","rampage"},{"berserker","a3ns9xl58ixdg2lo","bloodthirsty"},{"berserker","is053f9imz801s57","berserking"},
            {"assassin","is053f9imz801s57","evasion"},{"assassin","kp8uei8ppni71b5x","preparation"},{"assassin","a3ns9xl58ixdg2lo","siphoning_strikes"},
            {"archer","is053f9imz801s57","disengage"},{"archer","a3ns9xl58ixdg2lo","arrow_rain"},{"archer","kp8uei8ppni71b5x","elemental_arrows"},
            {"foreigner","a3ns9xl58ixdg2lo","elemental_surge"},{"foreigner","kp8uei8ppni71b5x","elemental_impact"},{"foreigner","is053f9imz801s57","spellweaver"},
            {"saber","is053f9imz801s57","consecration"},{"saber","kp8uei8ppni71b5x","sacred_onslaught"},{"saber","a3ns9xl58ixdg2lo","heavensmiths_call"},
            {"ruler","is053f9imz801s57","sacred_orb"},{"ruler","a3ns9xl58ixdg2lo","divine_intervention"},{"ruler","kp8uei8ppni71b5x","anoint_weapon"},
            {"avenger","yl0wtsb5m85wmvfa","summoning_ritual"},
            {"rider","rider_charge","rider_charge"},{"rider","rider_war_aura","rider_war_aura"},{"rider","rider_blazing_hooves","rider_blazing_hooves"},
            {"lancer","lancer_impaling_volley","impaling_volley"},{"lancer","lancer_dragon_thrust","dragon_thrust"},{"lancer","lancer_spearstorm","spearstorm"}
        };
        for (String[] row : rows) if (clazz(player,row[0]) && skill(player,row[0],row[1])) out.add(row[2]);
        return out;
    }

    private static boolean clazz(ServerPlayerEntity p, String path) {
        return PuffishSkillsIntegration.isCategoryUnlocked(new Identifier("smooth_classes", path), p);
    }
    private static boolean skill(ServerPlayerEntity p, String category, String skill) {
        return PuffishSkillsIntegration.isSkillUnlocked(new Identifier("smooth_classes", category), skill, p);
    }
    @FunctionalInterface
    private interface CastAction {
        ExecutionResult run();
    }

    private static DispatchResult result(ServerPlayerEntity player, String ability, CastAction action) {
        Identifier abilityId = new Identifier("smooth_classes", ability);
        long remaining = AbilityCooldowns.remainingTicks(player, abilityId);
        if (remaining > 0) return DispatchResult.fail(ability + " cooldown " + formatSeconds(remaining) + "s");
        ExecutionResult r = action.run();
        if (!r.success()) return DispatchResult.fail(r.detail());
        OptionalCompatRuntime.onSignatureAbility(player);
        if (!"sacred_orb".equals(ability))
            AbilityCooldowns.start(player, abilityId, AbilityCooldowns.adjustedTicks(player,
                    OptionalCompatRuntime.signatureCooldown(player, SignatureCooldowns.ticks(ability))));
        return new DispatchResult(true, ability, r.detail() + " affected=" + r.affectedTargets());
    }

    private static String formatSeconds(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }
}
