package org.marj4n.smooth_classes.mixin.client;
import java.util.*;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ModelPart.class)
public interface AvengerDancePartAccessor {
    @Accessor("children") Map<String,ModelPart> smoothClasses$danceChildren();
    @Accessor("cuboids") List<ModelPart.Cuboid> smoothClasses$danceCuboids();
}
