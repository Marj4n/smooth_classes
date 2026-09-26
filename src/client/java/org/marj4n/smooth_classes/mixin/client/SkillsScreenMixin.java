package org.marj4n.smooth_classes.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.puffish.skillsmod.util.Bounds2i;
import org.marj4n.smooth_classes.client.gui.TextureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@Mixin(SkillsScreen.class)
public abstract class SkillsScreenMixin {

    @Unique
    Map<Identifier, TextureState> textureStates = new HashMap<>();
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
    private void selectRandomCloudsTexture() {
        Random random = new Random();
        int textureIndex = random.nextInt(3);
        switch (textureIndex) {
            case 0:
                selectedCloudsTexture = cloudsTexture1;
                break;
            case 1:
                selectedCloudsTexture = cloudsTexture2;
                break;
            case 2:
                selectedCloudsTexture = cloudsTexture3;
                break;
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
        if (!FabricLoader.getInstance().isModLoaded("prominent"))
            drawParallaxTextures(context, bounds);
    }

    @Unique
    private void drawParallaxTextures(DrawContext context, Bounds2i bounds) {

        long currentTime = System.currentTimeMillis();

        for(int i = 1; i <= 30; i++){
            Identifier parallaxTexture = new Identifier("smooth_classes", String.format("textures/backgrounds/decor/planet_%02d.png", i));
            updateAndDrawAnimatedTexture(context, parallaxTexture, bounds, currentTime);
        }

        updateAndDrawCloudsTexture(context, bounds);
    }

    @Unique
    private void updateAndDrawAnimatedTexture(DrawContext context, Identifier texture, Bounds2i bounds, long currentTime) {
        int frameCount = 120;
        int spriteSheetWidth = 7680;
        int frameWidth = spriteSheetWidth / frameCount;
        int frameHeight = 64;

        // Initialize texture state if not present
        textureStates.computeIfAbsent(texture, k -> {
            float initialX = bounds.min().x + (float) Math.random() * (bounds.width() - frameWidth);
            float initialY = bounds.min().y + (float) Math.random() * (bounds.height() - frameHeight);
            float scale = (Math.max(0.4f, (float) (Math.random() * 5f)));
            float speed = Math.min(0.04f, (float) ((Math.random() * 0.08f) / scale));
            long animationSpeed = Math.max(240, (long) (Math.random() * 300 + (10 * scale)));
            float brightness = Math.min(0.5f, 0.1f + (float) Math.random());
            TextureState newState = new TextureState(initialX, bounds.min().y, speed, scale, animationSpeed, brightness);
            float[] newPosition = updatePlanetPosition(initialX, initialY, bounds, speed, scale);
            newState.x = newPosition[0];
            newState.y = newPosition[1];
            return newState;
        });

        TextureState state = textureStates.get(texture);
        int currentFrame = (int) ((currentTime / state.animationSpeed) % frameCount);
        int u = currentFrame * frameWidth;
        float[] newPosition = updatePlanetPosition(state.x, state.y, bounds, state.speed, state.scale);
        state.x = newPosition[0];
        state.y = newPosition[1];
        MatrixStack matrixStack = context.getMatrices();
        matrixStack.push();
        matrixStack.scale(state.scale, state.scale, 1.0f);

        int boundsWidth = bounds.width();
        int scaledWidth = (int) (frameWidth * state.scale);
        float effectiveXCenter = state.x + scaledWidth / 2.0f;
        float distanceToLeftEdge = effectiveXCenter - bounds.min().x;
        float distanceToRightEdge = bounds.max().x - effectiveXCenter;
        float effectiveEdgeDistance = Math.min(distanceToLeftEdge, distanceToRightEdge);
        float edgeFactor = effectiveEdgeDistance / (boundsWidth / 2.0f);
        float dynamicBrightness = state.brightness * edgeFactor;
        dynamicBrightness = Math.max(dynamicBrightness, 0.0f);

        RenderSystem.setShaderColor(dynamicBrightness, dynamicBrightness, dynamicBrightness, 1.0F);
        context.drawTexture(texture, (int) state.x, (int) state.y, u, 0, frameWidth, frameHeight, spriteSheetWidth, frameHeight);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.pop();
    }

    @Unique
    private float[] updatePlanetPosition(float x, float y, Bounds2i bounds, float moveSpeed, float scale) {
        int frameCount = 120;
        int spriteSheetWidth = 7680;
        int frameWidth = spriteSheetWidth / frameCount;
        int frameHeight = 64;
        int scaledWidth = (int) (frameWidth * scale);
        int scaledHeight = (int) (frameHeight * scale);
        float newX = x - moveSpeed;
        float newY = y + moveSpeed;
        int outOfBoundsMargin = 800;

        boolean outOfBoundsHorizontally = newX + scaledWidth + outOfBoundsMargin < bounds.min().x || newX - outOfBoundsMargin > bounds.max().x;
        boolean outOfBoundsVertically = newY - outOfBoundsMargin > bounds.max().y;

        if (outOfBoundsHorizontally || outOfBoundsVertically) {
            newX = bounds.min().x + (float) Math.random() * (bounds.width() - scaledWidth);
            newY = bounds.min().y - scaledHeight - (float) Math.random() * (scaledHeight + outOfBoundsMargin);
        }

        return new float[]{newX, newY};
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
