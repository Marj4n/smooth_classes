package org.marj4n.smooth_classes.api.specialization;

import net.minecraft.util.Identifier;

/** Foundation for future class evolutions/ascendancies without coupling them to the base class implementation. */
public record ClassSpecialization(
        Identifier id,
        Identifier baseClassId,
        String name,
        String description,
        int requiredLevel
) {}
