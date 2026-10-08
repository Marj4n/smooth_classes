package org.marj4n.smooth_classes.client.origin.appearance;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Smooth Classes' 1.20.1 backport of the Nycto Dark Form silhouette.
 * Geometry follows the user's Nycto reference: tall beast torso, long arms,
 * digitigrade legs, bat head and the large two-stage membrane wings.
 */
public final class ManBatFormModel<T extends PlayerEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart leftWing1;
    private final ModelPart leftWing2;
    private final ModelPart rightWing1;
    private final ModelPart rightWing2;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftHand;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart leftLeg;
    private final ModelPart leftForeleg;
    private final ModelPart leftFoot;
    private final ModelPart rightLeg;
    private final ModelPart rightForeleg;
    private final ModelPart rightFoot;

    public ManBatFormModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = body.getChild("chest");
        this.head = body.getChild("head");
        this.leftWing1 = chest.getChild("left_wing1");
        this.leftWing2 = leftWing1.getChild("left_wing2");
        this.rightWing1 = chest.getChild("right_wing1");
        this.rightWing2 = rightWing1.getChild("right_wing2");
        this.leftArm = root.getChild("left_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.leftHand = leftForearm.getChild("left_hand");
        this.rightArm = root.getChild("right_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.rightHand = rightForearm.getChild("right_hand");
        this.leftLeg = root.getChild("left_leg").getChild("left_leg_upper");
        this.leftForeleg = leftLeg.getChild("left_foreleg");
        this.leftFoot = leftForeleg.getChild("left_foot");
        this.rightLeg = root.getChild("right_leg").getChild("right_leg_upper");
        this.rightForeleg = rightLeg.getChild("right_foreleg");
        this.rightFoot = rightForeleg.getChild("right_foot");
    }

    public static TexturedModelData createData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(102, 20)
                        .cuboid(-3.5F, -6.0F, -3.0F, 7.0F, 9.0F, 6.0F),
                ModelTransform.pivot(0.0F, 3.7382F, 1.5058F));

        ModelPartData chest = body.addChild("chest", ModelPartBuilder.create(),
                ModelTransform.pivot(0.0F, -4.75F, -1.0F));
        chest.addChild("chest_cube", ModelPartBuilder.create().uv(92, 0)
                        .cuboid(-5.0F, -4.0F, -4.5F, 10.0F, 10.0F, 8.0F),
                ModelTransform.of(0.0F, -6.0F, 0.0F, 0.2182F, 0.0F, 0.0F));

        ModelPartData leftWing1 = chest.addChild("left_wing1", ModelPartBuilder.create().uv(108, 67)
                        .cuboid(-2.0F, -5.0F, -1.0F, 3.0F, 6.0F, 4.0F),
                ModelTransform.of(3.75F, -7.75F, 2.0F, -1.0036F, 0.2182F, -0.0873F));
        ModelPartData leftWing2 = leftWing1.addChild("left_wing2", ModelPartBuilder.create().uv(84, 58)
                        .cuboid(0.0F, -14.0F, -2.0F, 1.0F, 21.0F, 21.0F),
                ModelTransform.of(-0.5F, -5.25F, 0.75F, 0.6109F, 0.0F, 0.3054F));
        leftWing2.addChild("left_membrane", ModelPartBuilder.create().uv(68, 73)
                        .cuboid(0.25F, -14.0F, -2.0F, 0.0F, 25.0F, 30.0F),
                ModelTransform.of(0.25F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));
        leftWing1.addChild("left_inner_membrane", ModelPartBuilder.create().uv(36, 101)
                        .cuboid(0.0F, -13.0F, -1.0F, 0.0F, 13.0F, 14.0F),
                ModelTransform.of(-0.75F, -1.0F, 3.0F, -1.0472F, 0.0F, 0.0F));

        ModelPartData rightWing1 = chest.addChild("right_wing1", ModelPartBuilder.create().uv(108, 67).mirrored()
                        .cuboid(-1.0F, -5.0F, -1.0F, 3.0F, 6.0F, 4.0F),
                ModelTransform.of(-3.75F, -7.75F, 2.0F, -1.0036F, -0.2182F, 0.0873F));
        ModelPartData rightWing2 = rightWing1.addChild("right_wing2", ModelPartBuilder.create().uv(84, 58).mirrored()
                        .cuboid(-1.0F, -14.0F, -2.0F, 1.0F, 21.0F, 21.0F),
                ModelTransform.of(0.5F, -5.25F, 0.75F, 0.6109F, 0.0F, -0.3054F));
        rightWing2.addChild("right_membrane", ModelPartBuilder.create().uv(68, 73).mirrored()
                        .cuboid(-0.25F, -14.0F, -2.0F, 0.0F, 25.0F, 30.0F),
                ModelTransform.of(-0.25F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));
        rightWing1.addChild("right_inner_membrane", ModelPartBuilder.create().uv(36, 101).mirrored()
                        .cuboid(0.0F, -13.0F, -1.0F, 0.0F, 13.0F, 14.0F),
                ModelTransform.of(0.75F, -1.0F, 3.0F, -1.0472F, 0.0F, 0.0F));

        // Exact Dark Form shag/fur plane hierarchy.
        ModelPartData hair = chest.addChild("hair", ModelPartBuilder.create(),
                ModelTransform.of(0.0F, -7.5F, 4.75F, 0.7854F, 0.0F, 0.0F));
        hair.addChild("lTruff01_r1", ModelPartBuilder.create().uv(57, 9)
                        .cuboid(1.0F, 0.0F, -0.5F, 8.0F, 0.0F, 9.0F),
                ModelTransform.of(0.0F, -9.0F, 0.0F, -1.2217F, 0.0F, 0.4363F));
        hair.addChild("rTruff01_r1", ModelPartBuilder.create().uv(59, 27)
                        .cuboid(-9.0F, 0.0F, -0.5F, 8.0F, 0.0F, 9.0F),
                ModelTransform.of(0.0F, -9.0F, 0.0F, -1.2217F, 0.0F, -0.5236F));
        ModelPartData innerTruff = hair.addChild("innerTruff", ModelPartBuilder.create(),
                ModelTransform.of(0.0F, 1.0F, -4.75F, -0.2618F, 0.0F, 0.0F));
        ModelPartData lowerTruff = innerTruff.addChild("lowerTruff", ModelPartBuilder.create(),
                ModelTransform.of(-0.5F, -7.0F, 5.25F, -0.3927F, 0.0F, 0.0F));
        lowerTruff.addChild("lTruff03_r1", ModelPartBuilder.create().uv(60, 54)
                        .cuboid(1.9096F, 0.7376F, 0.0368F, 8.0F, 0.0F, 9.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, -1.309F, 0.0F, 0.5236F));
        lowerTruff.addChild("rTruff03_r1", ModelPartBuilder.create().uv(60, 45)
                        .cuboid(-8.9096F, 0.7376F, 0.0368F, 8.0F, 0.0F, 9.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, -1.309F, 0.0F, -0.5236F));
        ModelPartData upperTruff = innerTruff.addChild("upperTruff", ModelPartBuilder.create(),
                ModelTransform.of(0.0F, 0.0F, 3.5F, -0.2182F, 0.0F, 0.0F));
        upperTruff.addChild("lTruff02_r1", ModelPartBuilder.create().uv(57, 9)
                        .cuboid(1.4096F, 0.7816F, -0.0296F, 8.0F, 0.0F, 9.0F),
                ModelTransform.of(0.0F, -9.0F, 0.0F, -1.2217F, 0.0F, 0.5236F));
        upperTruff.addChild("rTruff02_r1", ModelPartBuilder.create().uv(59, 27)
                        .cuboid(-9.4096F, 0.7816F, -0.0296F, 8.0F, 0.0F, 9.0F),
                ModelTransform.of(0.0F, -9.0F, 0.0F, -1.2217F, 0.0F, -0.5236F));
        body.addChild("cloth", ModelPartBuilder.create().uv(74, 65)
                        .cuboid(-3.0F, -1.0F, 0.6F, 6.0F, 11.0F, 0.0F),
                ModelTransform.of(0.0F, 1.75F, -3.75F, -0.2182F, 0.0F, 0.0F));

        ModelPartData head = body.addChild("head", ModelPartBuilder.create().uv(86, 37)
                        .cuboid(-3.5F, -8.0F, -6.0F, 7.0F, 8.0F, 8.0F, new Dilation(-0.01F)),
                ModelTransform.pivot(0.0F, -13.75F, -3.0F));
        ModelPartData upperJaw = head.addChild("upperjaw", ModelPartBuilder.create().uv(24, 105)
                        .cuboid(-1.5F, -1.0F, -2.0F, 3.0F, 1.0F, 2.0F)
                        .uv(0, 0).cuboid(-1.5F, -3.5F, -1.75F, 3.0F, 3.0F, 0.0F),
                ModelTransform.pivot(0.0F, -1.5F, -6.0F));
        upperJaw.addChild("lips", ModelPartBuilder.create().uv(36, 105)
                        .cuboid(-1.0F, -0.5F, -2.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.01F)),
                ModelTransform.pivot(-0.5F, 0.5F, 0.0F));
        upperJaw.addChild("fangs", ModelPartBuilder.create().uv(24, 109)
                        .cuboid(-1.5F, 0.0F, -2.0F, 3.0F, 2.0F, 2.0F, new Dilation(-0.01F)),
                ModelTransform.pivot(0.0F, -1.0F, 0.0F));
        head.addChild("lower_jaw", ModelPartBuilder.create().uv(24, 115)
                        .cuboid(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                ModelTransform.pivot(0.0F, -1.0F, -6.0F));
        head.addChild("left_ear", ModelPartBuilder.create().uv(114, 46).mirrored()
                        .cuboid(0.25F, -10.0F, -1.5F, 0.0F, 11.0F, 7.0F),
                ModelTransform.of(3.0F, -3.75F, -3.0F, -0.48F, 0.4363F, 0.0873F));
        head.addChild("right_ear", ModelPartBuilder.create().uv(114, 46)
                        .cuboid(-0.25F, -10.0F, -1.5F, 0.0F, 11.0F, 7.0F),
                ModelTransform.of(-3.0F, -3.75F, -3.0F, -0.48F, -0.4363F, -0.0873F));

        ModelPartData leftArm = root.addChild("left_arm", ModelPartBuilder.create().uv(23, 71)
                        .cuboid(-1.0F, -2.0F, -1.0F, 3.0F, 10.0F, 4.0F)
                        .uv(0, 113).cuboid(-0.5F, -2.25F, -1.5F, 3.0F, 5.0F, 5.0F),
                ModelTransform.of(5.0F, -7.0118F, -0.4942F, 0.0436F, 0.0F, -0.1309F));
        ModelPartData leftForearm = leftArm.addChild("left_forearm", ModelPartBuilder.create().uv(24, 89)
                        .cuboid(-0.5F, 0.0F, -3.0F, 2.0F, 8.0F, 3.0F),
                ModelTransform.of(0.0F, 8.0F, 3.0F, -0.2618F, 0.0F, 0.0F));
        ModelPartData leftHand = leftForearm.addChild("left_hand", ModelPartBuilder.create(), ModelTransform.pivot(0.5F, 9.0F, 0.0F));
        leftHand.addChild("hand_cube", ModelPartBuilder.create().uv(42, 92)
                        .cuboid(-1.5F, -1.0F, -3.0F, 2.0F, 5.0F, 5.0F),
                ModelTransform.of(0.5F, -0.7382F, -1.0057F, 0.0F, 0.0F, 0.4363F));

        ModelPartData rightArm = root.addChild("right_arm", ModelPartBuilder.create().uv(23, 71).mirrored()
                        .cuboid(-2.0F, -2.0F, -1.0F, 3.0F, 10.0F, 4.0F)
                        .uv(0, 113).cuboid(-2.5F, -2.25F, -1.5F, 3.0F, 5.0F, 5.0F),
                ModelTransform.of(-5.0F, -7.0118F, -0.4942F, 0.0436F, 0.0F, 0.1309F));
        ModelPartData rightForearm = rightArm.addChild("right_forearm", ModelPartBuilder.create().uv(24, 89).mirrored()
                        .cuboid(-1.5F, 0.0F, -3.0F, 2.0F, 8.0F, 3.0F),
                ModelTransform.of(0.0F, 8.0F, 3.0F, -0.2618F, 0.0F, 0.0F));
        ModelPartData rightHand = rightForearm.addChild("right_hand", ModelPartBuilder.create(), ModelTransform.pivot(-0.5F, 9.0F, 1.0F));
        rightHand.addChild("hand_cube", ModelPartBuilder.create().uv(42, 92).mirrored()
                        .cuboid(-0.5F, -1.0F, -3.0F, 2.0F, 5.0F, 5.0F),
                ModelTransform.of(-0.5F, -0.7382F, -2.0058F, 0.0F, 0.0F, -0.4363F));

        ModelPartData leftLegRoot = root.addChild("left_leg", ModelPartBuilder.create(),
                ModelTransform.pivot(3.0F, 23.9647F, 1.8651F));
        ModelPartData leftLeg = leftLegRoot.addChild("left_leg_upper", ModelPartBuilder.create().uv(0, 69)
                        .cuboid(-2.0F, -1.0F, -3.0F, 4.0F, 13.0F, 5.0F, new Dilation(-0.1F)),
                ModelTransform.of(0.0F, -19.2266F, 0.3906F, -0.2488F, -0.194F, -0.0552F));
        ModelPartData leftForeleg = leftLeg.addChild("left_foreleg", ModelPartBuilder.create().uv(0, 88)
                        .cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F),
                ModelTransform.of(0.0F, 8.0F, 2.0F, 0.1745F, 0.0F, 0.0F));
        leftForeleg.addChild("left_foot", ModelPartBuilder.create().uv(0, 104)
                        .cuboid(-2.0F, -1.75F, -3.75F, 4.0F, 2.0F, 5.0F),
                ModelTransform.of(0.0F, 11.0F, -1.0F, 0.0873F, 0.0F, 0.0873F));

        ModelPartData rightLegRoot = root.addChild("right_leg", ModelPartBuilder.create(),
                ModelTransform.pivot(-3.0F, 23.9413F, 1.8651F));
        ModelPartData rightLeg = rightLegRoot.addChild("right_leg_upper", ModelPartBuilder.create().uv(0, 69).mirrored()
                        .cuboid(-2.0F, -1.0F, -3.0F, 4.0F, 13.0F, 5.0F, new Dilation(-0.1F)),
                ModelTransform.of(0.0F, -19.2031F, 0.3906F, -0.2488F, 0.194F, 0.0552F));
        ModelPartData rightForeleg = rightLeg.addChild("right_foreleg", ModelPartBuilder.create().uv(0, 88).mirrored()
                        .cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F),
                ModelTransform.of(0.0F, 8.0F, 2.0F, 0.1745F, 0.0F, 0.0F));
        rightForeleg.addChild("right_foot", ModelPartBuilder.create().uv(0, 104).mirrored()
                        .cuboid(-2.0F, -1.75F, -3.75F, 4.0F, 2.0F, 5.0F),
                ModelTransform.of(0.0F, 11.0F, -1.0F, 0.0873F, 0.0F, -0.0873F));

        return TexturedModelData.of(data, 128, 128);
    }

    public void prepare(T player, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float yaw = MathHelper.clamp(headYaw * ((float)Math.PI / 180.0F), -0.75F, 0.75F);
        float pitch = MathHelper.clamp(headPitch * ((float)Math.PI / 180.0F), -0.65F, 0.65F);
        resetNyctoBasePose();
        head.yaw += yaw;
        head.pitch += pitch;

        boolean airborne = !player.isOnGround();
        if (airborne) {
            if (player.getVelocity().y > 0.08D) {
                applyNyctoJump(animationProgress);
            } else {
                applyNyctoFly(animationProgress);
            }
        } else if (limbDistance > 0.02F) {
            applyNyctoGroundWalk(limbAngle, limbDistance, player.isSprinting());
        } else {
            applyNyctoIdle(animationProgress, player.isSneaking());
        }

        if (player.handSwingProgress > 0.0F) {
            // Nycto maps main-hand swing to RIGHT_ATTACK and off-hand swing to LEFT_ATTACK.
            applyNyctoRightAttack(player.handSwingProgress);
        }
    }

    private void resetNyctoBasePose() {
        body.pitch = 0.0F; body.yaw = 0.0F; body.roll = 0.0F;
        chest.pitch = 0.0F; chest.yaw = 0.0F; chest.roll = 0.0F;
        head.pitch = 0.0F; head.yaw = 0.0F; head.roll = 0.0F;

        leftWing1.pitch = -1.0036F; leftWing1.yaw = 0.2182F; leftWing1.roll = -0.0873F;
        rightWing1.pitch = -1.0036F; rightWing1.yaw = -0.2182F; rightWing1.roll = 0.0873F;
        leftWing2.pitch = 0.6109F; leftWing2.yaw = 0.0F; leftWing2.roll = 0.3054F;
        rightWing2.pitch = 0.6109F; rightWing2.yaw = 0.0F; rightWing2.roll = -0.3054F;

        leftArm.pitch = 0.0436F; leftArm.yaw = 0.0F; leftArm.roll = -0.1309F;
        rightArm.pitch = 0.0436F; rightArm.yaw = 0.0F; rightArm.roll = 0.1309F;
        leftForearm.pitch = -0.2618F; leftForearm.yaw = 0.0F; leftForearm.roll = 0.0F;
        rightForearm.pitch = -0.2618F; rightForearm.yaw = 0.0F; rightForearm.roll = 0.0F;
        leftHand.pitch = leftHand.yaw = leftHand.roll = 0.0F;
        rightHand.pitch = rightHand.yaw = rightHand.roll = 0.0F;

        leftLeg.pitch = -0.2488F; leftLeg.yaw = -0.194F; leftLeg.roll = -0.0552F;
        rightLeg.pitch = -0.2488F; rightLeg.yaw = 0.194F; rightLeg.roll = 0.0552F;
        leftForeleg.pitch = 0.1745F; leftForeleg.yaw = leftForeleg.roll = 0.0F;
        rightForeleg.pitch = 0.1745F; rightForeleg.yaw = rightForeleg.roll = 0.0F;
        leftFoot.pitch = 0.0873F; leftFoot.yaw = 0.0F; leftFoot.roll = 0.0873F;
        rightFoot.pitch = 0.0873F; rightFoot.yaw = 0.0F; rightFoot.roll = -0.0873F;
    }

    /** Nycto Dark Form FLY: 2s looping, three strong wing beats per loop. */
    private void applyNyctoFly(float age) {
        float t = (age / 20.0F) % 2.0F;
        float wave = MathHelper.cos(t * (float)Math.PI); // slow body/limb breathing
        float beatPhase = (t * 3.0F) % 1.0F;
        float beat = beatPhase < 0.5F ? beatPhase * 2.0F : (1.0F - beatPhase) * 2.0F;
        float fold = MathHelper.lerp(beat, 8.84F, 53.84F);
        float tip = beatPhase < 0.3333F
                ? MathHelper.lerp(beatPhase / 0.3333F, -5.86F, -39.32F)
                : MathHelper.lerp((beatPhase - 0.3333F) / 0.6667F, -39.32F, -5.86F);

        body.pitch += rad(-10.05F - 2.0F * (0.5F + 0.5F * wave));
        body.yaw += rad(1.29F);
        chest.pitch += rad(20.0F + 2.0F * wave);
        head.pitch += rad(10.0F + 3.0F * wave);

        // These are the characteristic Nycto fly wing channels.
        leftWing1.pitch += rad(-7.32F);
        leftWing1.yaw += rad(fold);
        leftWing1.roll += rad(13.14F);
        rightWing1.pitch += rad(-7.32F);
        rightWing1.yaw -= rad(fold);
        rightWing1.roll -= rad(13.14F);
        leftWing2.roll += rad(tip);
        rightWing2.roll -= rad(tip);

        leftArm.pitch += rad(-8.47F + 4.0F * wave);
        leftArm.yaw += rad(11.95F);
        leftArm.roll += rad(-25.72F - 4.3F * wave);
        leftForearm.pitch += rad(-27.5F);
        rightArm.pitch += rad(19.6F - 3.0F * wave);
        rightArm.yaw += rad(8.43F);
        rightArm.roll += rad(27.52F + 4.3F * wave);
        rightForearm.pitch += rad(-32.5F);

        leftLeg.pitch += rad(5.08F - 4.33F * wave);
        leftLeg.yaw += rad(-14.77F);
        leftLeg.roll += rad(0.48F);
        leftForeleg.pitch += rad(12.5F + 3.83F * wave);
        leftFoot.pitch += rad(22.5F);
        rightLeg.pitch += rad(-19.4F - 4.33F * wave);
        rightLeg.yaw += rad(13.44F);
        rightLeg.roll += rad(0.48F);
        rightForeleg.pitch += rad(42.5F + 3.83F * wave);
        rightFoot.pitch += rad(2.5F);
    }

    /** First second of Nycto's JUMP state, used while the Man-Bat is gaining altitude. */
    private void applyNyctoJump(float age) {
        float p = MathHelper.clamp((age % 20.0F) / 20.0F, 0.0F, 1.0F);
        float s = MathHelper.sin(p * (float)Math.PI);
        body.pitch += rad(17.0F * s - 10.05F * p);
        chest.pitch += rad(20.0F * p + 7.5F * s);
        leftArm.pitch += rad(-59.0F * s - 8.47F * p);
        leftArm.yaw += rad(11.0F * s + 11.95F * p);
        leftArm.roll += rad(-31.0F * s - 25.72F * p);
        rightArm.pitch += rad(44.0F * s + 19.61F * p);
        rightArm.yaw += rad(18.0F * s + 8.43F * p);
        rightArm.roll += rad(30.0F * s + 27.52F * p);
        leftForearm.pitch += rad(-27.5F * p);
        rightForearm.pitch += rad(-32.5F * p);
        leftWing1.pitch += rad(19.4F * s - 7.32F * p);
        leftWing1.yaw += rad(39.4F * s + 11.34F * p);
        leftWing1.roll += rad(48.2F * s + 13.14F * p);
        rightWing1.pitch += rad(19.9F * s - 7.32F * p);
        rightWing1.yaw += rad(-48.1F * s - 11.34F * p);
        rightWing1.roll += rad(-50.9F * s - 13.14F * p);
        leftLeg.pitch += rad(47.5F * s + 5.08F * p);
        rightLeg.pitch += rad(-45.0F * s - 19.4F * p);
        leftForeleg.pitch += rad(10.0F * s + 12.5F * p);
        rightForeleg.pitch += rad(35.0F * s + 42.5F * p);
    }

    private void applyNyctoIdle(float age, boolean sneak) {
        float t = age * 0.05236F; // 2 second loop
        float s = MathHelper.sin(t);
        body.pitch += rad(-2.0F * s);
        chest.pitch += rad(2.0F * s);
        leftArm.pitch += rad(-1.3F * s);
        rightArm.pitch += rad(0.65F * s);
        leftForearm.pitch += rad(0.7F * s);
        rightForearm.pitch += rad(0.7F * s);
        leftLeg.pitch += rad(1.3F * s);
        rightLeg.pitch += rad(1.3F * s);
        if (sneak) {
            body.pitch += rad(20.0F);
            chest.pitch += rad(8.0F);
            leftLeg.pitch += rad(20.0F);
            rightLeg.pitch += rad(20.0F);
        }
    }

    private void applyNyctoGroundWalk(float limbAngle, float limbDistance, boolean sprinting) {
        float speed = Math.min(1.0F, limbDistance * (sprinting ? 2.8F : 2.1F));
        float a = limbAngle * 0.6662F * (sprinting ? 1.25F : 1.0F);
        float l = MathHelper.cos(a) * speed;
        float r = MathHelper.cos(a + (float)Math.PI) * speed;
        float amp = sprinting ? 1.0F : 0.72F;
        leftArm.pitch += l * amp;
        rightArm.pitch += r * amp;
        leftForearm.pitch += Math.max(0.0F, -l) * 0.55F;
        rightForearm.pitch += Math.max(0.0F, -r) * 0.55F;
        leftLeg.pitch += r * amp;
        rightLeg.pitch += l * amp;
        leftForeleg.pitch -= r * 0.55F;
        rightForeleg.pitch -= l * 0.55F;
        body.pitch += sprinting ? rad(8.0F) : 0.0F;
    }

    private void applyNyctoRightAttack(float swing) {
        float p = MathHelper.clamp(swing, 0.0F, 1.0F);
        float s = MathHelper.sin(p * (float)Math.PI);
        // Dominant values from Nycto RIGHT_ATTACK, including torso twist and claw curl.
        body.pitch += rad(-13.83F * s);
        body.yaw += rad(18.49F * s);
        body.roll += rad(-4.95F * s);
        rightArm.pitch += rad(-60.5F * s);
        rightArm.yaw += rad(-28.87F * s);
        rightArm.roll += rad(-15.67F * s);
        rightForearm.pitch += rad(-18.0F * s);
        rightForearm.yaw += rad(-20.0F * s);
        rightForearm.roll += rad(-38.0F * s);
        rightHand.roll += rad(-32.5F * s);
        leftArm.yaw += rad(-36.18F * s);
        leftWing1.yaw += rad(-17.37F * s);
        rightWing1.yaw += rad(27.70F * s);
    }

    /** Nycto-style first-person LEFT claw; no fake two-arm centre-screen pose. */
    public void renderFirstPersonLeftClaw(float swingProgress, float equipProgress,
                                          MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        resetNyctoBasePose();
        float p = MathHelper.clamp(swingProgress, 0.0F, 1.0F);
        float s = MathHelper.sin(p * (float)Math.PI);

        leftArm.pitch += rad(-60.5F * s);
        leftArm.yaw += rad(28.87F * s);
        leftArm.roll += rad(15.67F * s);
        leftForearm.pitch += rad(-18.0F * s);
        leftForearm.yaw += rad(20.0F * s);
        leftForearm.roll += rad(38.0F * s);
        leftHand.roll += rad(-32.5F * s);

        matrices.push();
        // Nycto's claw is shown from the left side; keep the same chained arm hierarchy.
        matrices.translate(-0.62D, -0.58D + equipProgress * 0.18D, -0.72D - s * 0.20D);
        matrices.scale(0.82F, 0.82F, 0.82F);
        leftArm.render(matrices, vertices, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
    }

    private static float rad(float degrees) {
        return degrees * ((float)Math.PI / 180.0F);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        prepare(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
