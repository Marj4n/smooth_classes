package org.marj4n.smooth_classes.mixin.client;

import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.puffish.skillsmod.util.Bounds2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SkillsScreen.class)
public interface SkillsScreenAccessor {

    @Accessor("bounds")
    Bounds2i getBounds();

    // Add other accessors as needed
}
