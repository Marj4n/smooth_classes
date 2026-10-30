package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ModelPart.Cuboid.class)
public interface AvengerDanceCuboidAccessor {
    @Accessor("sides") ModelPart.Quad[] smoothClasses$danceSides();
}
