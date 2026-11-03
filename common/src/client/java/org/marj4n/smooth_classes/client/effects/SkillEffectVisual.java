package org.marj4n.smooth_classes.client.effects;

import java.util.List;

/** Model references and draw parameters; artwork remains a separate resource concern. */
public enum SkillEffectVisual {
    ARCANE_VOLLEY(Layout.ORBIT, .4F, 1.55F, 16.25F, false,
            new Pass("spell_projectile/arcane_projectile", Material.GLOW)),
    FROST_VOLLEY(Layout.ORBIT, .4F, 1.55F, 16.25F, false,
            new Pass("spell_projectile/ice_projectile", Material.GLOW_CULL)),
    BLADESTORM(Layout.ORBIT, .4F, 1.55F, 16.25F, false,
            new Pass("spell_projectile/swordfall", Material.RADIATE),
            new Pass("spell_projectile/swordfall", Material.TRANSLUCENT)),
    VITALITY_BOND(Layout.ORBIT, .6F, 1.1F, 16.25F, false,
            new Pass("effect/vitality_bond", Material.RADIATE),
            new Pass("effect/vitality_bond", Material.TRANSLUCENT)),
    MARKSMANSHIP(Layout.ORBIT, 1.2F, 1.1F, 16.25F, false,
            new Pass("spell_projectile/arrow", Material.TRANSLUCENT)),
    CURSE(Layout.ORBIT, .9F, 1.2F, 6F, false,
            new Pass("effect/curse", Material.GLOW)),
    BONE_ARMOR(Layout.ORBIT, .9F, 1.2F, 6F, true,
            new Pass("spell_projectile/bones", Material.TRANSLUCENT)),
    HAMMERS(Layout.ORBIT, .9F, 3F, 9F, true,
            new Pass("spell_projectile/righteous_hammers", Material.GLOW_CULL)),
    BARRIER(Layout.BODY, new Pass("effect/barrier", Material.TRANSLUCENT)),
    IMMOBILIZE(Layout.FEET, new Pass("effect/immobilize", Material.TRANSLUCENT)),
    DEATH_MARK(Layout.CIRCLE, new Pass("effect/death_mark", Material.GLOW)),
    MAGIC_CIRCLE(Layout.CIRCLE, new Pass("effect/magic_circle", Material.GLOW_CULL)),
    TAUNTED(Layout.HEAD, new Pass("effect/taunted", Material.GLOW)),
    UNDYING(Layout.UNDYING,
            new Pass("effect/undying", Material.TRANSLUCENT),
            new Pass("effect/undying_glow", Material.GLOW_CULL),
            new Pass("effect/undying_barrier", Material.TRANSLUCENT));

    public enum Material { TRANSLUCENT, GLOW, GLOW_CULL, RADIATE }
    enum Layout { ORBIT, BODY, FEET, CIRCLE, HEAD, UNDYING }
    public record Pass(String modelPath, Material material) {}

    final Layout layout;
    final float scale;
    final float radius;
    final float angularSpeed;
    final boolean spin;
    private final List<Pass> passes;

    SkillEffectVisual(Layout layout, Pass... passes) {
        this(layout, 1F, 0F, 0F, false, passes);
    }

    SkillEffectVisual(Layout layout, float scale, float radius, float angularSpeed,
                      boolean spin, Pass... passes) {
        this.layout = layout;
        this.scale = scale;
        this.radius = radius;
        this.angularSpeed = angularSpeed;
        this.spin = spin;
        this.passes = List.of(passes);
    }

    public List<Pass> passes() {
        return passes;
    }
}
