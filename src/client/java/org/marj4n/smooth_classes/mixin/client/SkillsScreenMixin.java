package org.marj4n.smooth_classes.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.puffish.skillsmod.util.Bounds2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Mixin(SkillsScreen.class)
public abstract class SkillsScreenMixin {

    @Unique
    private static final boolean PROMINENT_LOADED = FabricLoader.getInstance().isModLoaded("prominent");
    @Unique
    private static final Identifier OMINOUS_EYE = new Identifier("smooth_classes", "textures/backgrounds/decor/ominous_eye.png");
    @Unique
    private static final int EYE_FRAME_COUNT = 8;
    @Unique
    private static final int EYE_FRAME_SIZE = 64;
    @Unique
    private static final int EYE_SHEET_WIDTH = EYE_FRAME_COUNT * EYE_FRAME_SIZE;
    @Unique
    private static final int EYE_MIN_GAP = 36;
    @Unique
    private final Identifier cloudsTexture1 = new Identifier("smooth_classes", "textures/backgrounds/decor/clouds.png");
    @Unique
    private final Identifier cloudsTexture2 = new Identifier("smooth_classes", "textures/backgrounds/decor/clouds_2.png");
    @Unique
    private final Identifier cloudsTexture3 = new Identifier("smooth_classes", "textures/backgrounds/decor/clouds_3.png");
    @Unique
    private float cloudsX = 0;

    @Unique
    private Identifier selectedCloudsTexture = null;
    @Unique
    private final List<EyeManifestation> activeEyes = new ArrayList<>();
    @Unique
    private long nextEyeSpawnAt = 0L;

    @Unique
    private void selectRandomCloudsTexture() {
        int textureIndex = ThreadLocalRandom.current().nextInt(3);
        switch (textureIndex) {
            case 0 -> selectedCloudsTexture = cloudsTexture1;
            case 1 -> selectedCloudsTexture = cloudsTexture2;
            default -> selectedCloudsTexture = cloudsTexture3;
        }
    }

    /* Scaling Variant - not aligned
    @Redirect(method = "drawContentWithCategory(Lnet/minecraft/client/gui/DrawContext;DDLnet/puffish/skillsmod/client/data/ClientSkillCategoryData;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V"))
    private void redirectDrawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        int originalTextureWidth = 5120;
        int originalTextureHeight = 2880;

        float aspectRatio = (float) originalTextureWidth / (float) originalTextureHeight;

        int newTextureWidth = bounds.width();
        int newTextureHeight = (int) (newTextureWidth / aspectRatio);

        int centerX = bounds.min().x + bounds.width() / 2;
        int centerY = bounds.min().y + bounds.height() / 2;

        int newX = centerX - newTextureWidth / 2;
        int newY = centerY - newTextureHeight / 2;

        context.drawTexture(texture, newX, newY, u, v, newTextureWidth, newTextureHeight, newTextureWidth, newTextureHeight);
    }
     */
    // Maintains image position in 16:9
    /*
    @Redirect(method = "drawContentWithCategory(Lnet/minecraft/client/gui/DrawContext;DDLnet/puffish/skillsmod/client/data/ClientSkillCategoryData;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V"))
    private void redirectDrawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        SkillsScreen screen = (SkillsScreen) (Object) this;
        SkillsScreenAccessor accessor = (SkillsScreenAccessor) screen;
        Bounds2i bounds = accessor.getBounds();
        int originalTextureWidth = 1920;
        int originalTextureHeight = 1080;

        float aspectRatio = (float) originalTextureWidth / (float) originalTextureHeight;

        int newTextureWidth = bounds.width(); // new width
        int newTextureHeight = (int) (newTextureWidth / aspectRatio);

        int centerX = bounds.min().x + bounds.width() / 2;
        int centerY = bounds.min().y + bounds.height() / 2;

        int newX = centerX - newTextureWidth / 2;
        int newY = centerY - newTextureHeight / 2;

        context.drawTexture(texture, newX, newY, u, v, newTextureWidth, newTextureHeight, newTextureWidth, newTextureHeight);
    }*/

    // MAIN
    @Inject(method = "drawContentWithCategory(Lnet/minecraft/client/gui/DrawContext;DDLnet/puffish/skillsmod/client/data/ClientCategoryData;)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/puffish/skillsmod/client/gui/SkillsScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;Lnet/puffish/skillsmod/client/config/ClientBackgroundConfig;)V"))
    private void injectDrawBackground(DrawContext context, double mouseX, double mouseY, ClientCategoryData activeCategoryData, CallbackInfo ci) {
        SkillsScreenAccessor accessor = (SkillsScreenAccessor) this;
        Bounds2i bounds = accessor.getBounds();

        // Don't draw star systems when prominent is loaded
        if (!PROMINENT_LOADED)
            drawParallaxTextures(context, bounds);
    }

    @Unique
    private void drawParallaxTextures(DrawContext context, Bounds2i bounds) {
        long currentTime = System.currentTimeMillis();
        updateEyes(bounds, currentTime);
        drawOminousEyes(context, bounds, currentTime);
        updateAndDrawCloudsTexture(context, bounds);
    }

    @Unique
    private void updateEyes(Bounds2i bounds, long currentTime) {
        Iterator<EyeManifestation> iterator = activeEyes.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired(currentTime)) {
                iterator.remove();
            }
        }

        if (nextEyeSpawnAt == 0L) {
            nextEyeSpawnAt = currentTime + ThreadLocalRandom.current().nextLong(700L, 1800L);
        }

        if (currentTime < nextEyeSpawnAt || activeEyes.size() >= 3) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int capacity = 3 - activeEyes.size();
        int burstMax = Math.min(capacity, 2);
        int burst = random.nextInt(1, burstMax + 1);

        for (int i = 0; i < burst; i++) {
            // Previous eyes were 1.2x-2.15x a 64px frame, making them dominate the
            // entire skill screen. Keep them ominous but background-sized instead.
            float scale = random.nextFloat(0.55F, 0.96F);
            int renderSize = Math.round(EYE_FRAME_SIZE * scale);
            int minX = bounds.min().x + 20;
            int maxX = Math.max(minX, bounds.max().x - renderSize - 20);
            int minY = bounds.min().y + 14;
            int maxY = Math.max(minY, bounds.max().y - renderSize - 14);

            int x = minX;
            int y = minY;
            boolean placed = false;
            for (int attempt = 0; attempt < 28; attempt++) {
                int candidateX = random.nextInt(minX, maxX + 1);
                int candidateY = random.nextInt(minY, maxY + 1);
                if (eyeHasRoom(candidateX, candidateY, renderSize)) {
                    x = candidateX;
                    y = candidateY;
                    placed = true;
                    break;
                }
            }
            if (!placed) continue;

            long delay = i == 0 ? 0L : random.nextLong(140L, 320L);
            long spawnAt = currentTime + delay;
            long life = random.nextLong(1800L, 3600L);
            activeEyes.add(new EyeManifestation(x, y, scale, spawnAt, life));
        }

        nextEyeSpawnAt = currentTime + random.nextLong(activeEyes.isEmpty() ? 900L : 1500L, 3600L);
    }

    @Unique
    private boolean eyeHasRoom(int x, int y, int size) {
        for (EyeManifestation eye : activeEyes) {
            int otherSize = Math.round(EYE_FRAME_SIZE * eye.scale());
            boolean separated = x + size + EYE_MIN_GAP <= eye.x()
                    || eye.x() + otherSize + EYE_MIN_GAP <= x
                    || y + size + EYE_MIN_GAP <= eye.y()
                    || eye.y() + otherSize + EYE_MIN_GAP <= y;
            if (!separated) return false;
        }
        return true;
    }

    @Unique
    private void drawOminousEyes(DrawContext context, Bounds2i bounds, long currentTime) {
        if (activeEyes.isEmpty()) {
            return;
        }

        for (EyeManifestation eye : activeEyes) {
            if (currentTime < eye.spawnAt()) {
                continue;
            }

            float alpha = eye.alpha(currentTime);
            if (alpha <= 0.01F) {
                continue;
            }

            int frame = eye.frame(currentTime);
            int u = frame * EYE_FRAME_SIZE;
            float pulse = eye.pulse(currentTime);

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(eye.x(), eye.y(), 0.0F);
            matrices.scale(eye.scale(), eye.scale(), 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(pulse, pulse * 0.80F, pulse * 0.70F, alpha);
            context.drawTexture(OMINOUS_EYE, 0, 0, u, 0, EYE_FRAME_SIZE, EYE_FRAME_SIZE, EYE_SHEET_WIDTH, EYE_FRAME_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }

    @Unique
    private record EyeManifestation(int x, int y, float scale, long spawnAt, long lifeMs) {

        private static final long FADE_IN_MS = 180L;
        private static final long FADE_OUT_MS = 280L;

        boolean isExpired(long currentTime) {
            return currentTime > spawnAt + lifeMs;
        }

        float alpha(long currentTime) {
            long age = currentTime - spawnAt;
            if (age <= 0L) {
                return 0.0F;
            }
            if (age < FADE_IN_MS) {
                return Math.min(1.0F, age / (float) FADE_IN_MS);
            }

            long remaining = (spawnAt + lifeMs) - currentTime;
            if (remaining < FADE_OUT_MS) {
                return Math.max(0.0F, remaining / (float) FADE_OUT_MS);
            }
            return 0.94F;
        }

        float pulse(long currentTime) {
            return 0.88F + 0.12F * (float) Math.sin((currentTime + x * 17L + y * 31L) / 210.0D);
        }

        int frame(long currentTime) {
            long age = Math.max(0L, currentTime - spawnAt);
            long local = age % 2600L;

            if (local < 1500L) return 4;
            if (local < 1560L) return 3;
            if (local < 1620L) return 2;
            if (local < 1680L) return 1;
            if (local < 1760L) return 0;
            if (local < 1820L) return 1;
            if (local < 1880L) return 2;
            if (local < 1940L) return 3;
            if (local < 2160L) return 4;
            if (local < 2220L) return 3;
            if (local < 2280L) return 2;
            if (local < 2340L) return 1;
            if (local < 2400L) return 0;
            if (local < 2460L) return 1;
            if (local < 2520L) return 2;
            if (local < 2580L) return 3;
            return 4;
        }
    }

    @Unique
    private void updateAndDrawCloudsTexture(DrawContext context, Bounds2i bounds) {
        if (cloudsX == 0)
            cloudsX = -bounds.width();
        if (selectedCloudsTexture == null) {
            selectRandomCloudsTexture();
        }
        float cloudsSpeed = 0.05f;
        float newX = cloudsX + cloudsSpeed;
        if (newX <= -bounds.width()) {
            newX = -bounds.width();
        } else if (newX > 0) {
            newX = 0;
        }
        cloudsX = newX;
        MatrixStack matrixStack = context.getMatrices();
        matrixStack.push();

        // Set partial transparency
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        int cloudsHeight = 1440;
        context.drawTexture(selectedCloudsTexture, (int) cloudsX, bounds.min().y, 0, 0, bounds.width() + 10240, cloudsHeight, bounds.width() + 10240, cloudsHeight);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.pop();
    }
}
