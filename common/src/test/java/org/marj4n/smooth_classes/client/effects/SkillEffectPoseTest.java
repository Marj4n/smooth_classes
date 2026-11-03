package org.marj4n.smooth_classes.client.effects;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.util.math.MatrixStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SkillEffectPoseTest {
    record Draw(String model, String material, float[] matrix) {}
    record Case(String profile, float worldTicks, float spinTicks, float width, float height,
                float entityScale, float bodyYaw, float pitch, boolean prone, int amplifier,
                List<Draw> draws) {
        SkillEffectPose.Frame frame() {
            return new SkillEffectPose.Frame(worldTicks, spinTicks, width, height, entityScale,
                    bodyYaw, pitch, prone, amplifier);
        }

        @Override
        public String toString() {
            return profile + "/amp=" + amplifier + "/prone=" + prone + "/height=" + height;
        }
    }

    static Stream<Case> legacyPoses() {
        var input = Objects.requireNonNull(SkillEffectPoseTest.class.getResourceAsStream(
                "/effects/legacy-render-poses.json"));
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return Stream.of(new Gson().fromJson(reader, Case[].class));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @ParameterizedTest
    @MethodSource("legacyPoses")
    void preservesRecordedPlacementLayerOrderAndCopyCount(Case fixture) {
        MatrixStack stack = new MatrixStack();
        List<Draw> actual = new ArrayList<>();
        SkillEffectPose.draw(SkillEffectVisual.valueOf(fixture.profile), fixture.frame(), stack,
                (pass, matrices) -> actual.add(new Draw("smooth_classes:" + pass.modelPath(),
                        pass.material().name(), matrices.peek().getPositionMatrix().get(new float[16]))));
        assertEquals(fixture.draws.size(), actual.size(), "draw count");
        for (int index = 0; index < actual.size(); index++) {
            Draw expected = fixture.draws.get(index);
            Draw observed = actual.get(index);
            assertEquals(expected.model, observed.model, "model " + index);
            assertEquals(expected.material, observed.material, "layer " + index);
            assertArrayEquals(expected.matrix, observed.matrix, .00001F, "transform " + index);
        }
        assertArrayEquals(new org.joml.Matrix4f().get(new float[16]),
                stack.peek().getPositionMatrix().get(new float[16]), "pose leaked after render");
    }

    @Test
    void restoresCallerPoseWhenDrawingFails() {
        for (SkillEffectVisual visual : SkillEffectVisual.values()) {
            MatrixStack stack = new MatrixStack();
            stack.translate(3, 5, -7);
            float[] before = stack.peek().getPositionMatrix().get(new float[16]);
            var frame = new SkillEffectPose.Frame(800, 200, .6F, 1.8F, 1, 90, 20, false, 2);
            assertThrows(IllegalStateException.class, () -> SkillEffectPose.draw(visual, frame,
                    stack, (pass, matrices) -> { throw new IllegalStateException("draw failed"); }));
            assertArrayEquals(before, stack.peek().getPositionMatrix().get(new float[16]));
        }
    }
}
