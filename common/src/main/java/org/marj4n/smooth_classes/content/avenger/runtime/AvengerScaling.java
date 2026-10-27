package org.marj4n.smooth_classes.content.avenger.runtime;

/** Numeric rules kept separate from entity code so balancing remains easy to audit. */
public final class AvengerScaling {
    private AvengerScaling() {}

    public static double attackDamage(double soulPower, AvengerMinionType type) {
        double multiplier = switch (type) {
            case GREATER_DREADGLARE -> 3.0;
            case DREADGLARE, WRAITH -> 1.2;
        };
        return 3.0 + multiplier * soulPower;
    }

    public static double maxHealth(double ownerMaxHealth, AvengerMinionType type) {
        double multiplier = switch (type) {
            case DREADGLARE -> 1.4;
            case WRAITH -> 0.8;
            case GREATER_DREADGLARE -> 4.8;
        };
        return 1.0 + multiplier * ownerMaxHealth;
    }

    public static double inheritedArmor(double ownerArmor, AvengerMinionType type) {
        double multiplier = type == AvengerMinionType.GREATER_DREADGLARE ? 1.0 : 0.5;
        return 1.0 + multiplier * ownerArmor;
    }

    public static double inheritedArmorToughness(double ownerArmorToughness, AvengerMinionType type) {
        double multiplier = type == AvengerMinionType.GREATER_DREADGLARE ? 1.0 : 0.5;
        return 1.0 + multiplier * ownerArmorToughness;
    }

    public static int shadowCombustRadius(AvengerMinionType type) {
        return type == AvengerMinionType.GREATER_DREADGLARE ? 7 : 4;
    }

    public static double shadowCombustSoulMultiplier(AvengerMinionType type) {
        return type == AvengerMinionType.GREATER_DREADGLARE ? 6.4 : 3.2;
    }

    public static int endlessServitudeChance(int harmfulEffectCount) {
        return Math.min(21 + Math.max(0, harmfulEffectCount) * 5, 60);
    }
}
