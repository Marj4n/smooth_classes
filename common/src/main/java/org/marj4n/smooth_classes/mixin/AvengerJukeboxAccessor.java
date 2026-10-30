package org.marj4n.smooth_classes.mixin;
import net.minecraft.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(JukeboxBlockEntity.class)
public interface AvengerJukeboxAccessor {
    @Accessor("tickCount") long smoothClasses$danceTick();
    @Accessor("recordStartTick") long smoothClasses$danceStart();
}
