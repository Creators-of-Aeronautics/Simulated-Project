package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class ClientBalloonEffectRenderer {

    private static final ResourceLocation FBO_ID = Aeronautics.path("soft_light");
    private static final ResourceLocation POST_SHADER_ID = Aeronautics.path("soft_light");

    private static final ResourceLocation SIDE_TEXTURE = Aeronautics.path("textures/special/heat_overlay.png");
    private static final ResourceLocation TOP_TEXTURE = Aeronautics.path("textures/special/lava_still.png");

    private static final ResourceLocation SHADER_ID = Aeronautics.path("hot_air_overlay");

    @Nullable
    private static AdvancedFbo overlayFbo;

    public static void onRenderLevelStage(final VeilRenderLevelStageEvent.Stage stage,
                                          final Matrix4fc frustumMatrix,
                                          final Matrix4fc projectionMatrix,
                                          final int renderTick) {
        if (stage != VeilRenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            freeFbo();
            return;
        }

        final BalloonMap ballonMap = BalloonMap.MAP.get(level);
        if (ballonMap == null || ballonMap.isEmpty()) {
            freeFbo();
            return;
        }

        final Window window = minecraft.getWindow();
        if (overlayFbo == null || overlayFbo.getWidth() != window.getWidth() || overlayFbo.getHeight() != window.getHeight()) {
            freeFbo();
            overlayFbo = AdvancedFbo.withSize(window.getWidth(), window.getHeight())
                    .addColorTextureBuffer()
                    .setDepthTextureBuffer()
                    .build(true);
        }

        renderBalloonEffects(ballonMap, frustumMatrix, projectionMatrix, renderTick);
    }

    /**
     * Renders the balloon effects for a given balloon map
     * @param balloonMap the balloon map to render effects for
     * @param frustumMatrix the model view matrix
     * @param projectionMatrix the projection matrix
     * @param renderTick the render-tick
     */
    private static void renderBalloonEffects(final BalloonMap balloonMap, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick) {
        final Minecraft minecraft = Minecraft.getInstance();
        final float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        final ShaderProgram shader = VeilRenderSystem.setShader(SHADER_ID);
        if (shader == null) return;

        overlayFbo.bind(false);
        overlayFbo.clear(0.0f, 0.0f, 0.0f, 0.0f, GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);

        RenderSystem.setShaderTexture(0, SIDE_TEXTURE);
        RenderSystem.setShaderTexture(1, TOP_TEXTURE);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        GL30.glCullFace(GL11.GL_FRONT);

        // Polygon offset to be before blocks
        RenderSystem.polygonOffset(-0.5F, -30.0F);
        RenderSystem.enablePolygonOffset();

        final float scrollAmount = (renderTick + partialTicks) / -20.0f;

        final ShaderUniformAccess scrollUniform = shader.getUniformSafe("Scroll");
        final ShaderUniformAccess yCutoffUniform = shader.getUniformSafe("CutoffY");

        scrollUniform.setFloat((float) (Math.floor(scrollAmount * 16.0f) / 16.0f));

        final float brightness = 0.85f;
        final float alpha = 1.0f;
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);

        final Matrix4f modelViewMat = new Matrix4f(frustumMatrix);
        final Matrix4f projMat = new Matrix4f(projectionMatrix);

        for (final Balloon balloon : balloonMap.getBalloons()) {
            final ClientBalloon clientBalloon = (ClientBalloon) balloon;

            final HeatedCulledRenderRegion renderRegion = clientBalloon.getRenderRegion();

            if (renderRegion == null) {
                continue;
            }

            float filledPercent = 0.0f;

            // These should all have the same client predicted volume, but we pick the max
            // to be safe
            for (final BlockEntityLiftingGasProvider heater : balloon.getHeaters()) {
                filledPercent = Math.max(filledPercent, (float) heater.getClientPredictedVolume() / balloon.getCapacity());
            }

            filledPercent = Mth.clamp(filledPercent, 0.0f, 1.0f);
            yCutoffUniform.setFloat((1.0f - filledPercent) * (balloon.getHeight() + 1.0f));

            renderRegion.render(modelViewMat, projMat);
        }

        // Cleanup render state
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        GL30.glCullFace(GL11.GL_BACK);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        AdvancedFbo.unbind();

        applyHeatingToScreen();
    }

    private static void applyHeatingToScreen() {
        final PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        final PostPipeline pipeline = manager.getPipeline(POST_SHADER_ID);
        final PostPipeline.Context context = manager.getPostPipelineContext();

        context.setFramebuffer(FBO_ID, overlayFbo);
        manager.runPipeline(pipeline);
    }

    private static void freeFbo() {
        if (overlayFbo != null) {
            overlayFbo.free();
        }

        overlayFbo = null;
    }
}
