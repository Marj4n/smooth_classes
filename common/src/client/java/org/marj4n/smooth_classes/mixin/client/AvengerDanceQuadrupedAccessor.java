package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(QuadrupedEntityModel.class)
public interface AvengerDanceQuadrupedAccessor {
    @Accessor("head") ModelPart smoothClasses$danceHead();
    @Accessor("body") ModelPart smoothClasses$danceBody();
    @Accessor("rightHindLeg") ModelPart smoothClasses$danceRH();
    @Accessor("leftHindLeg") ModelPart smoothClasses$danceLH();
    @Accessor("rightFrontLeg") ModelPart smoothClasses$danceRF();
    @Accessor("leftFrontLeg") ModelPart smoothClasses$danceLF();
}
