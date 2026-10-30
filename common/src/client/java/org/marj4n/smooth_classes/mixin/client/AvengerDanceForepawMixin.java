package org.marj4n.smooth_classes.mixin.client;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.marj4n.smooth_classes.client.avenger.dance.CatForepawJoint;
@Mixin(ModelPart.class)
public abstract class AvengerDanceForepawMixin {
    @Inject(method="renderCuboids",at=@At("HEAD"),cancellable=true)
    private void danceElbow(MatrixStack.Entry entry,VertexConsumer vertices,int light,int overlay,float r,float g,float b,float a,CallbackInfo ci){
        if(CatForepawJoint.draw((ModelPart)(Object)this,entry,vertices,light,overlay,r,g,b,a))ci.cancel();
    }
}
