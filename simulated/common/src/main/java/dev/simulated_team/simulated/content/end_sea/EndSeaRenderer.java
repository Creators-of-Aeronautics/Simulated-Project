package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.simulated_team.simulated.Simulated;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.ext.VeilMultiBind;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.lwjgl.opengl.GL30;

// todo good rendering for negative physics depthGradient?
/**
 * Renders the end-sea effect
 */
public class EndSeaRenderer {

    private static final ResourceLocation SHADER = Simulated.path("end_sea");

    private static final int LAYER_COUNT = 48;
    private static final VertexFormat FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .build();

    private static final Vec3[] LAYER_COLORS = new Vec3[]{
            new Vec3(0.022087, 0.098399, 0.110818),
            new Vec3(0.011892, 0.095924, 0.089485),
            new Vec3(0.027636, 0.101689, 0.100326),
            new Vec3(0.046564, 0.109883, 0.114838),
            new Vec3(0.064901, 0.117696, 0.097189),
            new Vec3(0.063761, 0.086895, 0.123646),
            new Vec3(0.084817, 0.111994, 0.166380),
            new Vec3(0.097489, 0.154120, 0.091064),
            new Vec3(0.106152, 0.131144, 0.195191),
            new Vec3(0.097721, 0.110188, 0.187229),
            new Vec3(0.133516, 0.138278, 0.148582),
            new Vec3(0.070006, 0.243332, 0.235792),
            new Vec3(0.196766, 0.142899, 0.214696),
            new Vec3(0.047281, 0.315338, 0.321970),
            new Vec3(0.204675, 0.390010, 0.302066),
            new Vec3(0.080955, 0.314821, 0.661491)
    };

    public static void tick() {

    }

    public static void render(final Camera camera, final GameRenderer gameRenderer) {
        final Minecraft minecraft = Minecraft.getInstance();
        final EndSeaPhysics physics = EndSeaPhysicsData.of(minecraft.level);
        if (physics == null) {
            return;
        }

        if (EndSeaShadowRenderer.renderingShadowMap()) {
            return;
        }

//        final VeilDebug debug = VeilDebug.get();
//        debug.pushDebugGroup("end_sea");
        renderLayers(physics, camera);
//        debug.popDebugGroup();
    }

    private static void renderLayers(final EndSeaPhysics physics, final Camera camera) {
        final Minecraft minecraft = Minecraft.getInstance();

        final ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        shader.bind();
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS);
        final AdvancedFbo shadowBuffer = EndSeaShadowRenderer.getShadowsFramebuffer();

        shader.setTexture("ShadowDepthSampler", GL30.GL_TEXTURE_2D, shadowBuffer.getDepthTextureAttachment().getId());
        shader.setTexture("ShadowStrengthSampler", GL30.GL_TEXTURE_2D, shadowBuffer.getColorTextureAttachment(0).getId());

        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, FORMAT);

        for (int i = 0; i < LAYER_COLORS.length; i++) {
            LAYER_COLORS[i] = LAYER_COLORS[i].lerp(LAYER_COLORS[i].normalize(), 1.0);
        }

        final Vector3dc renderOrigin = EndSeaShadowRenderer.getLastRenderOrigin();

        final PoseStack poseStack = new PoseStack();
        poseStack.translate(renderOrigin.x() - camera.getPosition().x, 0.0, renderOrigin.z() - camera.getPosition().z);
        poseStack.scale(EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS, 1.0f, EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS);
        poseStack.translate(0.0, physics.startY() - camera.getPosition().y, 0.0);

        final ShaderUniform volumeSize = shader.getUniform("ShadowVolumeSize");
        if (volumeSize != null) {
            volumeSize.setFloat(EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS);
        }

        final ShaderUniform startY = shader.getUniform("StartY");

        if (startY != null) {
            startY.setFloat((float) physics.startY());
        }

        final LocalPlayer player = minecraft.player;
        final float renderTime = player.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        for (int i = 0; i < LAYER_COUNT; i++) {
            final Vec3 layer = LAYER_COLORS[i % LAYER_COLORS.length];
            final float yCoord = -i / 2f;
            final float uvScale = EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * 2.0f / 25.0f;
            final float uvShift = (float) Mth.frac(renderTime / 2000.0) + (float) (renderOrigin.z() / EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * uvScale / 2f);
            final float parallelUVShift = (float) ((float) (layer.x + layer.y) + (renderOrigin.x() / EndSeaShadowRenderer.SHADOW_VOLUME_RADIUS * uvScale / 2f));
            final float alpha = 1.0f;

            final Matrix4f pose = poseStack.last().pose();

            builder.addVertex(pose, -1.0f, yCoord, -1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(0.0f + parallelUVShift, 0.0f + uvShift)
                    .setUv2(0, 0);

            builder.addVertex(pose, 1.0f, yCoord, -1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(uvScale + parallelUVShift, 0.0f + uvShift)
                    .setUv2(1, 0);

            builder.addVertex(pose, 1.0f, yCoord, 1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(uvScale + parallelUVShift, uvScale + uvShift)
                    .setUv2(1, 1);

            builder.addVertex(pose, -1.0f, yCoord, 1.0f)
                    .setColor((float) layer.x, (float) layer.y, (float) layer.z, alpha)
                    .setUv(0.0f + parallelUVShift, uvScale + uvShift)
                    .setUv2(0, 1);
        }

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        // additive
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        BufferUploader.drawWithShader(builder.buildOrThrow());
        ShaderProgram.unbind();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }
}
