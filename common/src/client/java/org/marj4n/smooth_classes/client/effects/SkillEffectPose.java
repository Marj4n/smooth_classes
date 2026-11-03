package org.marj4n.smooth_classes.client.effects;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class SkillEffectPose {
    public record Frame(float worldTicks, float spinTicks, float width, float height,
                        float entityScale, float bodyYaw, float pitch, boolean prone,
                        int amplifier) {}

    @FunctionalInterface
    public interface Draw {
        void model(SkillEffectVisual.Pass pass, MatrixStack matrices);
    }

    private SkillEffectPose() {}

    public static void draw(SkillEffectVisual visual, Frame frame, MatrixStack matrices,
                            Draw draw) {
        int copies = visual.layout == SkillEffectVisual.Layout.ORBIT ? frame.amplifier + 1 : 1;
        for (int copy = 0; copy < copies; copy++) {
            for (int layer = 0; layer < visual.passes().size(); layer++) {
                matrices.push();
                try {
                    place(visual, frame, copy, copies, layer, matrices);
                    draw.model(visual.passes().get(layer), matrices);
                } finally {
                    matrices.pop();
                }
            }
        }
    }

    private static void place(SkillEffectVisual visual, Frame f, int copy, int copies,
                              int layer, MatrixStack matrices) {
        switch (visual.layout) {
            case ORBIT -> {
                yaw(matrices, f.worldTicks * visual.angularSpeed - 45F + 360F / copies * copy);
                matrices.translate(0, f.height / 2F, -(visual.radius * f.entityScale));
                uniformScale(matrices, visual.scale);
                if (visual.spin) yaw(matrices, f.spinTicks * 10F);
            }
            case BODY -> attached(f, matrices, 1F);
            case FEET -> {
                matrices.translate(0, .5F, 0);
                if (f.prone) pitch(matrices, f.pitch + 90F);
                uniformScale(matrices, f.prone ? 0F : 1.2F);
            }
            case CIRCLE -> {
                float halfWidth = f.width / 2F;
                matrices.translate(0, .9F + halfWidth, 0);
                uniformScale(matrices, f.prone ? 0F : 1.9F + halfWidth);
                yaw(matrices, f.worldTicks * 4F - 45F);
            }
            case HEAD -> {
                matrices.translate(0, f.height - .4F, 0);
                uniformScale(matrices, 1.2F);
            }
            case UNDYING -> {
                if (layer < 2) {
                    attached(f, matrices, layer == 0 ? 1F : 1.05F);
                } else {
                    float width = Math.max(1.1F, f.width * 1.5F);
                    matrices.translate(0, f.height * .37F, 0);
                    matrices.scale(width, (f.height + .15F) / 1.94F, width);
                }
            }
        }
    }

    private static void attached(Frame f, MatrixStack matrices, float scale) {
        matrices.translate(0, .68F, 0);
        if (f.prone) matrices.translate(0, -.30F, 0);
        if (scale != 1F) uniformScale(matrices, scale);
        yaw(matrices, -f.bodyYaw);
        if (f.prone) pitch(matrices, f.pitch + 90F);
    }

    private static void yaw(MatrixStack matrices, float degrees) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(degrees));
    }

    private static void pitch(MatrixStack matrices, float degrees) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(degrees));
    }

    private static void uniformScale(MatrixStack matrices, float scale) {
        matrices.scale(scale, scale, scale);
    }
}
