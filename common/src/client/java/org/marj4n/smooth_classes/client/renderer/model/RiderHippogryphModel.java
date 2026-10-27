package org.marj4n.smooth_classes.client.renderer.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.marj4n.smooth_classes.entity.RiderHippogryphEntity;

/**
 * Rider Hippogryph model.
 * Geometry/UV layout is ported from Ice and Fire Community Edition (LGPL-3.0).
 * Runtime animation is reimplemented for Smooth Classes and has no Ice and Fire/Uranus dependency.
 */
public final class RiderHippogryphModel extends EntityModel<RiderHippogryphEntity> {
    private final ModelPart body;
    private final ModelPart wingL;
    private final ModelPart wingL2;
    private final ModelPart wingL3;
    private final ModelPart wingR;
    private final ModelPart wingR2;
    private final ModelPart wingR3;
    private final ModelPart fingerL3;
    private final ModelPart fingerR3;
    private final ModelPart frontLegR;
    private final ModelPart frontLegL;
    private final ModelPart hindThighR;
    private final ModelPart hindThighL;
    private final ModelPart neck;
    private final ModelPart neck2;
    private final ModelPart headPivot;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;

    public RiderHippogryphModel(ModelPart root) {
        this.body = root.getChild("Body");
        this.wingL = body.getChild("WingL");
        this.wingL2 = wingL.getChild("WingL2");
        this.wingL3 = wingL2.getChild("WingL3");
        this.wingR = body.getChild("WingR");
        this.wingR2 = wingR.getChild("WingR2");
        this.wingR3 = wingR2.getChild("WingR3");
        this.fingerL3 = wingL3.getChild("FingerL3");
        this.fingerR3 = wingR3.getChild("FingerR3");
        this.frontLegR = body.getChild("BackLegR1");
        this.frontLegL = body.getChild("BackLegR1_1");
        this.hindThighR = body.getChild("HindThighR");
        this.hindThighL = body.getChild("HindThighL");
        this.neck = body.getChild("Neck");
        this.neck2 = neck.getChild("Neck2");
        this.headPivot = neck2.getChild("HeadPivot");
        this.tail1 = body.getChild("Tail1");
        this.tail2 = tail1.getChild("Tail2");
        this.tail3 = tail2.getChild("Tail3");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData Body = root.addChild("Body", ModelPartBuilder.create().uv(0, 34).cuboid(-5.0F, -8.0F, -19.0F, 10.0F, 10.0F, 24.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 11.0F, 9.0F));
        ModelPartData HindThighL = Body.addChild("HindThighL", ModelPartBuilder.create().uv(96, 29).mirrored().cuboid(-2.5F, -2.0F, -2.5F, 4.0F, 9.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(4.0F, -1.0F, 2.0F));
        ModelPartData HindLegL = HindThighL.addChild("HindLegL", ModelPartBuilder.create().uv(96, 43).mirrored().cuboid(-2.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 7.0F, 0.0F));
        ModelPartData HindFootL = HindLegL.addChild("HindFootL", ModelPartBuilder.create().uv(96, 51).mirrored().cuboid(-2.5F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 5.0F, 0.0F));
        ModelPartData BackLegR1_1 = Body.addChild("BackLegR1_1", ModelPartBuilder.create().uv(66, 40).mirrored().cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(4.2F, -3.9F, -17.0F));
        ModelPartData BackLegR2_1 = BackLegR1_1.addChild("BackLegR2_1", ModelPartBuilder.create().uv(81, 42).mirrored().cuboid(-1.0F, 0.0F, -0.7F, 2.0F, 10.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 6.9F, 0.8F, -0.31869712F, 0.0F, 0.0F));
        ModelPartData ToeL4_1 = BackLegR2_1.addChild("ToeL4_1", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-0.6F, 9.8F, 0.2F, -1.2292354F, 0.61086524F, 0.0F));
        ModelPartData ToeR3_1 = BackLegR2_1.addChild("ToeR3_1", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 9.8F, -0.7F, -1.0927507F, 0.0F, 0.0F));
        ModelPartData ToeR1_1 = BackLegR2_1.addChild("ToeR1_1", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 9.8F, 0.9F, -1.821251F, 3.1415927F, 0.0F));
        ModelPartData ToeR2_1 = BackLegR2_1.addChild("ToeR2_1", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.6F, 9.8F, 0.2F, -1.1838568F, -0.95609134F, 0.0F));
        ModelPartData WingL = Body.addChild("WingL", ModelPartBuilder.create().uv(100, 107).mirrored().cuboid(-0.1F, 0.0F, -5.0F, 1.0F, 8.0F, 12.0F, new Dilation(0.0F)), ModelTransform.of(4.2F, -6.6F, -13.2F, 0.12217305F, 0.38397244F, -0.6981317F));
        ModelPartData WingL2 = WingL.addChild("WingL2", ModelPartBuilder.create().uv(80, 90).mirrored().cuboid(-0.6F, -2.5F, -2.1F, 1.0F, 11.0F, 11.0F, new Dilation(0.0F)), ModelTransform.of(0.4F, 7.6F, -2.8F, 1.548107F, 0.0F, -0.17453292F));
        ModelPartData WingL21 = WingL2.addChild("WingL21", ModelPartBuilder.create().uv(80, 90).mirrored().cuboid(-0.4F, -2.5F, -2.1F, 1.0F, 11.0F, 11.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.5F, 0.0F, 0.0F));
        ModelPartData WingL3 = WingL2.addChild("WingL3", ModelPartBuilder.create().uv(124, 86).mirrored().cuboid(-0.7F, -0.1F, -2.0F, 1.0F, 18.0F, 10.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 7.6F, 0.0F));
        ModelPartData FingerL2 = WingL3.addChild("FingerL2", ModelPartBuilder.create().uv(50, 80).cuboid(-0.8F, -0.1F, -2.0F, 1.0F, 14.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-0.1F, 15.0F, 2.0F, 0.10471976F, 0.0F, 0.0F));
        ModelPartData FingerL3 = WingL3.addChild("FingerL3", ModelPartBuilder.create().uv(40, 80).cuboid(-0.8F, -0.1F, -2.0F, 1.0F, 16.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 15.0F, 4.4F, 0.08726646F, 0.0F, 0.0F));
        ModelPartData FingerL1 = WingL3.addChild("FingerL1", ModelPartBuilder.create().uv(60, 80).cuboid(-0.8F, -0.1F, -2.0F, 1.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 15.0F, 0.1F, 0.12217305F, 0.0F, 0.0F));
        ModelPartData FingerL4 = WingL3.addChild("FingerL4", ModelPartBuilder.create().uv(30, 80).cuboid(-0.9F, -0.1F, -2.0F, 1.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 15.0F, 6.6F));
        ModelPartData Saddle = Body.addChild("Saddle", ModelPartBuilder.create().uv(80, 0).cuboid(-5.0F, 0.0F, -3.0F, 10.0F, 1.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -8.9F, -7.0F));
        ModelPartData StirrupL = Saddle.addChild("StirrupL", ModelPartBuilder.create().uv(70, 0).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(5.0F, 1.0F, 0.0F));
        ModelPartData StirrupIronL = StirrupL.addChild("StirrupIronL", ModelPartBuilder.create().uv(74, 0).cuboid(-0.5F, 6.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData Saddleback = Saddle.addChild("Saddleback", ModelPartBuilder.create().uv(80, 9).cuboid(-4.0F, -1.0F, 3.0F, 8.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData SaddleFront = Saddle.addChild("SaddleFront", ModelPartBuilder.create().uv(106, 9).cuboid(-1.5F, -1.0F, -3.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData StirrupR = Saddle.addChild("StirrupR", ModelPartBuilder.create().uv(80, 0).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-5.0F, 1.0F, 0.0F));
        ModelPartData StirrupIronR = StirrupR.addChild("StirrupIronR", ModelPartBuilder.create().uv(74, 4).cuboid(-0.5F, 6.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData ChestR = Saddle.addChild("ChestR", ModelPartBuilder.create().uv(0, 34).cuboid(-3.0F, 0.0F, -3.0F, 8.0F, 8.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-4.5F, 1.0F, 8.0F, 0.0F, 1.5707964F, 0.0F));
        ModelPartData ChestL = Saddle.addChild("ChestL", ModelPartBuilder.create().uv(0, 47).cuboid(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(4.5F, 1.0F, 8.0F, 0.0F, 1.5707964F, 0.0F));
        ModelPartData Neck = Body.addChild("Neck", ModelPartBuilder.create().uv(1, 109).cuboid(-3.0F, -6.6F, -2.2F, 6.0F, 9.0F, 9.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.1F, -18.2F, 0.7740535F, 0.0F, 0.0F));
        ModelPartData Crest1 = Neck.addChild("Crest1", ModelPartBuilder.create().uv(30, 100).cuboid(0.0F, -8.0F, 0.1F, 1.0F, 8.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -10.4F, 6.1F, -2.4586453F, 0.0F, 0.0F));
        ModelPartData Neck2 = Neck.addChild("Neck2", ModelPartBuilder.create().uv(36, 108).cuboid(-2.02F, -8.5F, -1.6F, 4.0F, 10.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -6.8F, 0.2F, -0.68294734F, 0.0F, 0.0F));
        ModelPartData HeadPivot = Neck2.addChild("HeadPivot", ModelPartBuilder.create(), ModelTransform.of(0.0F, -7.8F, 1.2F, 0.3642502F, 0.0F, 0.0F));
        ModelPartData Head = HeadPivot.addChild("Head", ModelPartBuilder.create().uv(0, 68).cuboid(-2.5F, -4.7F, -3.9F, 5.0F, 6.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData NoseBand = Head.addChild("NoseBand", ModelPartBuilder.create().uv(85, 60).cuboid(-3.0F, -11.1F, -7.0F, 6.0F, 6.0F, 12.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 6.5F, -2.2F, 0.09110618F, 0.0F, 0.0F));
        ModelPartData ReinR = NoseBand.addChild("ReinR", ModelPartBuilder.create().uv(46, 55).mirrored().cuboid(-3.1F, -6.0F, -3.4F, 0.0F, 3.0F, 19.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.04363323F, 0.0F, 0.0F));
        ModelPartData ReinL = NoseBand.addChild("ReinL", ModelPartBuilder.create().uv(46, 55).cuboid(3.1F, -6.3F, -3.4F, 0.0F, 3.0F, 19.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.04363323F, 0.0F, 0.0F));
        ModelPartData Jaw = Head.addChild("Jaw", ModelPartBuilder.create().uv(24, 68).cuboid(-2.0F, -0.3F, -5.4F, 4.0F, 1.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.4F, -3.0F, -0.04555309F, 0.0F, 0.0F));
        ModelPartData Quill_L = Head.addChild("Quill_L", ModelPartBuilder.create().uv(22, 99).mirrored().cuboid(-0.5F, -4.5F, -0.6F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(2.0F, -4.3F, 1.9F, -1.1838568F, 0.17453292F, 0.0F));
        ModelPartData Crest1_1 = Head.addChild("Crest1_1", ModelPartBuilder.create().uv(30, 100).cuboid(0.0F, -8.0F, 0.1F, 1.0F, 8.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -5.4F, 3.1F, -2.2310543F, 0.0F, 0.0F));
        ModelPartData Beak = Head.addChild("Beak", ModelPartBuilder.create().uv(0, 84).cuboid(-2.01F, -3.2F, -4.0F, 4.0F, 4.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -0.6F, -4.8F));
        ModelPartData BeakTip = Beak.addChild("BeakTip", ModelPartBuilder.create().uv(14, 99).cuboid(-1.0F, -2.8F, -1.7F, 2.0F, 5.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.6F, -2.7F));
        ModelPartData Beak2 = Beak.addChild("Beak2", ModelPartBuilder.create().uv(0, 84).cuboid(-1.99F, -3.2F, -4.0F, 4.0F, 4.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.1F, 0.0F));
        ModelPartData Quill_R = Head.addChild("Quill_R", ModelPartBuilder.create().uv(22, 99).cuboid(-0.5F, -4.5F, -0.6F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, -4.2F, 1.9F, -1.1838568F, -0.17453292F, 0.0F));
        ModelPartData Tail1 = Body.addChild("Tail1", ModelPartBuilder.create().uv(44, 0).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -8.1F, 5.0F, -1.134464F, 0.0F, 0.0F));
        ModelPartData Tail2 = Tail1.addChild("Tail2", ModelPartBuilder.create().uv(38, 7).cuboid(-1.5F, -2.0F, 0.0F, 3.0F, 4.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 2.6F));
        ModelPartData Tail3 = Tail2.addChild("Tail3", ModelPartBuilder.create().uv(24, 3).cuboid(-1.5F, -2.0F, 0.0F, 3.0F, 4.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -0.2F, 6.3F, -0.34906584F, 0.0F, 0.0F));
        ModelPartData HindThighR = Body.addChild("HindThighR", ModelPartBuilder.create().uv(96, 29).cuboid(-1.5F, -2.0F, -2.5F, 4.0F, 9.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(-4.0F, -1.0F, 2.0F));
        ModelPartData HindLegR = HindThighR.addChild("HindLegR", ModelPartBuilder.create().uv(96, 43).cuboid(-1.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 7.0F, 0.0F));
        ModelPartData HindFootR = HindLegR.addChild("HindFootR", ModelPartBuilder.create().uv(96, 51).cuboid(-1.5F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 5.0F, 0.0F));
        ModelPartData BackLegR1 = Body.addChild("BackLegR1", ModelPartBuilder.create().uv(66, 40).cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(-4.2F, -3.9F, -17.0F));
        ModelPartData BackLegR2 = BackLegR1.addChild("BackLegR2", ModelPartBuilder.create().uv(81, 42).cuboid(-1.0F, 0.0F, -0.7F, 2.0F, 10.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 6.9F, 0.8F, -0.31869712F, 0.0F, 0.0F));
        ModelPartData ToeR1 = BackLegR2.addChild("ToeR1", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 9.8F, 0.9F, -1.821251F, 3.1415927F, 0.0F));
        ModelPartData ToeL4 = BackLegR2.addChild("ToeL4", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-0.6F, 9.8F, 0.2F, -1.2292354F, 0.95609134F, 0.0F));
        ModelPartData ToeR3 = BackLegR2.addChild("ToeR3", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 9.8F, -0.7F, -1.0927507F, 0.0F, 0.0F));
        ModelPartData ToeR2 = BackLegR2.addChild("ToeR2", ModelPartBuilder.create().uv(51, 43).mirrored().cuboid(-0.5F, -0.5F, -0.7F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.6F, 9.8F, 0.2F, -1.1838568F, -0.61086524F, 0.0F));
        ModelPartData WingR = Body.addChild("WingR", ModelPartBuilder.create().uv(100, 107).cuboid(-0.9F, 0.0F, -5.0F, 1.0F, 8.0F, 12.0F, new Dilation(0.0F)), ModelTransform.of(-4.2F, -6.6F, -13.2F, 0.12217305F, -0.38397244F, 0.6981317F));
        ModelPartData WingR2 = WingR.addChild("WingR2", ModelPartBuilder.create().uv(80, 90).cuboid(-0.4F, -2.5F, -2.1F, 1.0F, 11.0F, 11.0F, new Dilation(0.0F)), ModelTransform.of(-0.4F, 7.6F, -2.8F, 1.548107F, 0.0F, 0.17453292F));
        ModelPartData WingR21 = WingR2.addChild("WingR21", ModelPartBuilder.create().uv(80, 90).cuboid(-0.6F, -2.5F, -2.1F, 1.0F, 11.0F, 11.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 0.0F, 0.0F));
        ModelPartData WingR3 = WingR2.addChild("WingR3", ModelPartBuilder.create().uv(124, 86).cuboid(-0.3F, -0.1F, -2.0F, 1.0F, 18.0F, 10.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 7.6F, 0.0F));
        ModelPartData FingerR3 = WingR3.addChild("FingerR3", ModelPartBuilder.create().uv(40, 80).mirrored().cuboid(-0.2F, -0.1F, -2.0F, 1.0F, 16.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 15.0F, 4.5F, 0.08726646F, 0.0F, 0.0F));
        ModelPartData FingerR2 = WingR3.addChild("FingerR2", ModelPartBuilder.create().uv(50, 80).mirrored().cuboid(-0.2F, -0.1F, -2.0F, 1.0F, 14.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.1F, 15.0F, 2.0F, 0.10471976F, 0.0F, 0.0F));
        ModelPartData FingerR4 = WingR3.addChild("FingerR4", ModelPartBuilder.create().uv(30, 80).mirrored().cuboid(-0.1F, -0.1F, -2.0F, 1.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 15.6F, 6.6F));
        ModelPartData FingerR1 = WingR3.addChild("FingerR1", ModelPartBuilder.create().uv(60, 80).mirrored().cuboid(-0.2F, -0.1F, -2.0F, 1.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 15.0F, 0.1F, 0.12217305F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 256, 128);
    }

    @Override
    public void setAngles(RiderHippogryphEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0F, 1.0F);
        float flightBlend = entity.getWingFlightBlend(tickDelta);
        float boostBlend = entity.getWingBoostBlend(tickDelta);
        // Smoothstep keeps both ends of every pose transition from visibly snapping.
        flightBlend = flightBlend * flightBlend * (3.0F - 2.0F * flightBlend);
        boostBlend = boostBlend * boostBlend * (3.0F - 2.0F * boostBlend);
        float flapPhase = entity.getWingFlapPhase(tickDelta);

        var velocity = entity.getVelocity();
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float speed01 = MathHelper.clamp(horizontalSpeed / 0.95F, 0.0F, 1.0F);
        float climb01 = MathHelper.clamp((float) velocity.y * 2.2F, -1.0F, 1.0F);

        // Reset core pose. Wing angles below are not hard-switched by FLYING;
        // every joint is blended between ground/folded and air/boost poses.
        body.pitch = 0.0F;
        body.yaw = 0.0F;
        body.roll = 0.0F;
        neck.pitch = 0.7740535F;
        neck.yaw = 0.0F;
        neck.roll = 0.0F;
        neck2.pitch = -0.68294734F;
        neck2.yaw = 0.0F;
        neck2.roll = 0.0F;
        headPivot.pitch = 0.3642502F;
        headPivot.yaw = 0.0F;
        headPivot.roll = 0.0F;
        tail1.pitch = -1.134464F;
        tail1.yaw = 0.0F;
        tail1.roll = 0.0F;
        tail2.pitch = 0.0F;
        tail2.yaw = 0.0F;
        tail2.roll = 0.0F;
        tail3.pitch = -0.34906585F;
        tail3.yaw = 0.0F;
        tail3.roll = 0.0F;

        // Ground/water/lava gait. Surface walking uses actual horizontal speed so
        // fluid contact cannot collapse the vanilla limbDistance animation.
        float stride = limbDistance;
        if (entity.isRiderSurfaceWalking()) {
            stride = Math.max(stride, MathHelper.clamp(horizontalSpeed * 3.25F, 0.0F, 1.0F));
        }
        float walk = MathHelper.cos(limbAngle * 0.6662F) * 1.05F * stride;
        float groundFrontR = walk;
        float groundFrontL = -walk;
        float groundHindR = -walk;
        float groundHindL = walk;

        // Folded wings are still alive on the ground: footfalls flex the shoulder
        // and idle breathing moves the feathers.
        float idleWing = MathHelper.sin(animationProgress * 0.11F) * 0.016F;
        float stepWing = MathHelper.sin(limbAngle * 0.6662F + 0.65F) * 0.055F * stride;
        float groundFlex = idleWing + stepWing;

        float groundWingLPitch = 0.10F + groundFlex * 0.28F;
        float groundWingRPitch = 0.10F + groundFlex * 0.28F;
        float groundWingLYaw = 0.52F;
        float groundWingRYaw = -0.52F;
        float groundWingLRoll = -0.34F - groundFlex;
        float groundWingRRoll = 0.34F + groundFlex;

        float groundWing2Pitch = 1.46F + groundFlex * 0.40F;
        float groundWingL2Roll = -0.13F - groundFlex * 0.35F;
        float groundWingR2Roll = 0.13F + groundFlex * 0.35F;
        float groundWing3Pitch = 0.08F - groundFlex * 0.25F;
        float groundWingL3Roll = -0.03F - groundFlex * 0.18F;
        float groundWingR3Roll = 0.03F + groundFlex * 0.18F;

        // Normal flight keeps the broad IAF-like silhouette and rhythmic flap.
        // As Ctrl/boost blends in, flap cadence/amplitude dies away while the
        // entire wing sweeps rearward into a compact jet/glide pose.
        float normalFlapAmplitude = 0.39F + Math.max(0.0F, climb01) * 0.13F;
        normalFlapAmplitude *= 1.0F - speed01 * 0.15F;
        float flapAmplitude = MathHelper.lerp(boostBlend, normalFlapAmplitude, 0.035F);
        float flap = MathHelper.sin(flapPhase) * flapAmplitude;
        float secondary = MathHelper.sin(flapPhase - 0.72F) * flapAmplitude * 0.34F;
        float outer = MathHelper.sin(flapPhase - 1.08F) * flapAmplitude * 0.20F;

        // Boost keeps the full span visible.  The wing sweeps back like a fast
        // aircraft but does NOT fold inward; the previous compact pose made the
        // silhouette look smaller exactly when it should feel fastest.
        float spread = MathHelper.lerp(boostBlend, 1.30F, 1.48F);
        float sweptYaw = MathHelper.lerp(boostBlend, 0.02F, 0.42F);
        float flightRootPitch = MathHelper.lerp(boostBlend, 0.087F + climb01 * 0.035F, -0.075F);
        float flightWingLPitch = flightRootPitch;
        float flightWingRPitch = flightRootPitch;
        float flightWingLYaw = sweptYaw;
        float flightWingRYaw = -sweptYaw;
        float flightWingLRoll = -spread - flap;
        float flightWingRRoll = spread + flap;

        float flightWing2Pitch = MathHelper.lerp(boostBlend, -0.349F + secondary * 0.12F, -0.24F);
        float flightWingL2Roll = MathHelper.lerp(boostBlend, -0.175F - secondary, -0.14F);
        float flightWingR2Roll = MathHelper.lerp(boostBlend, 0.175F + secondary, 0.14F);
        float flightWing3Pitch = MathHelper.lerp(boostBlend, 0.524F + outer * 0.22F, 0.34F);
        float flightWingL3Roll = MathHelper.lerp(boostBlend, -outer, -0.085F);
        float flightWingR3Roll = MathHelper.lerp(boostBlend, outer, 0.085F);

        // Smooth takeoff / landing / boost transitions. There is never an instant
        // folded->open or flap->static branch anymore.
        wingL.pitch = MathHelper.lerp(flightBlend, groundWingLPitch, flightWingLPitch);
        wingR.pitch = MathHelper.lerp(flightBlend, groundWingRPitch, flightWingRPitch);
        wingL.yaw = MathHelper.lerp(flightBlend, groundWingLYaw, flightWingLYaw);
        wingR.yaw = MathHelper.lerp(flightBlend, groundWingRYaw, flightWingRYaw);
        wingL.roll = MathHelper.lerp(flightBlend, groundWingLRoll, flightWingLRoll);
        wingR.roll = MathHelper.lerp(flightBlend, groundWingRRoll, flightWingRRoll);

        wingL2.pitch = MathHelper.lerp(flightBlend, groundWing2Pitch, flightWing2Pitch);
        wingR2.pitch = MathHelper.lerp(flightBlend, groundWing2Pitch, flightWing2Pitch);
        wingL2.yaw = 0.0F;
        wingR2.yaw = 0.0F;
        wingL2.roll = MathHelper.lerp(flightBlend, groundWingL2Roll, flightWingL2Roll);
        wingR2.roll = MathHelper.lerp(flightBlend, groundWingR2Roll, flightWingR2Roll);

        wingL3.pitch = MathHelper.lerp(flightBlend, groundWing3Pitch, flightWing3Pitch);
        wingR3.pitch = MathHelper.lerp(flightBlend, groundWing3Pitch, flightWing3Pitch);
        wingL3.yaw = 0.0F;
        wingR3.yaw = 0.0F;
        wingL3.roll = MathHelper.lerp(flightBlend, groundWingL3Roll, flightWingL3Roll);
        wingR3.roll = MathHelper.lerp(flightBlend, groundWingR3Roll, flightWingR3Roll);

        // No banking/carving pose: flight keeps the body level. Only pitch follows
        // climb/dive so the mount still reads naturally in vertical flight.
        body.roll = 0.0F;
        body.yaw = 0.0F;
        body.pitch = -entity.getVisualFlightPitchRadians() * 0.38F * flightBlend;
        neck.yaw = 0.0F;
        neck2.yaw = 0.0F;
        headPivot.yaw = 0.0F;
        tail1.yaw = 0.0F;
        tail2.yaw = 0.0F;
        tail3.yaw = 0.0F;
        tail1.roll = 0.0F;
        tail2.roll = 0.0F;
        tail3.roll = 0.0F;

        // Legs also transition instead of popping from run into a fully tucked pose.
        float flightFront = -0.48F - climb01 * 0.06F;
        float flightHind = 0.46F + climb01 * 0.05F;
        frontLegR.pitch = MathHelper.lerp(flightBlend, groundFrontR, flightFront);
        frontLegL.pitch = MathHelper.lerp(flightBlend, groundFrontL, flightFront);
        hindThighR.pitch = MathHelper.lerp(flightBlend, groundHindR, flightHind);
        hindThighL.pitch = MathHelper.lerp(flightBlend, groundHindL, flightHind);
        tail1.pitch = MathHelper.lerp(flightBlend, -1.134464F, -1.02F + entity.getVisualFlightPitchRadians() * 0.16F);

        // Never feed raw renderer headYaw/headPitch directly into a mount that is
        // inches from the first-person camera.  Entity-side damping removes the
        // tiny network corrections that otherwise look like head stutter.
        headPivot.yaw += entity.getVisualHeadYawRadians(tickDelta) * 0.58F;
        headPivot.pitch += entity.getVisualHeadPitchRadians(tickDelta) * 0.50F;
        tail2.yaw += MathHelper.sin(animationProgress * 0.18F) * 0.08F * (1.0F - flightBlend * 0.75F);
    }


    public void applyLeftWingTipTransform(MatrixStack matrices) {
        body.rotate(matrices);
        wingL.rotate(matrices);
        wingL2.rotate(matrices);
        wingL3.rotate(matrices);
        fingerL3.rotate(matrices);
        // FingerL3 is 16 model pixels long along local +Y.
        matrices.translate(0.0D, 1.0D, 0.0D);
    }

    public void applyRightWingTipTransform(MatrixStack matrices) {
        body.rotate(matrices);
        wingR.rotate(matrices);
        wingR2.rotate(matrices);
        wingR3.rotate(matrices);
        fingerR3.rotate(matrices);
        matrices.translate(0.0D, 1.0D, 0.0D);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
