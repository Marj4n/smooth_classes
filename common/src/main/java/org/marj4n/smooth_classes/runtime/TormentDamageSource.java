package org.marj4n.smooth_classes.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.spell_power.api.SpellSchools;

/** Real Fire spell damage identity with only vanilla fire protection/immunity disabled. */
public final class TormentDamageSource extends DamageSource {
    public TormentDamageSource(ServerWorld world, Entity caster) {
        super(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(SpellSchools.FIRE.damageType),caster);
    }
    @Override public boolean isIn(TagKey<DamageType> tag) {
        return !DamageTypeTags.IS_FIRE.equals(tag) && super.isIn(tag);
    }
}
