package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AnimalModel.class)
public interface AvengerDanceAnimalAccessor {
    @Invoker("getHeadParts") Iterable<ModelPart> smoothClasses$danceHeads();
    @Invoker("getBodyParts") Iterable<ModelPart> smoothClasses$danceBodies();
}
