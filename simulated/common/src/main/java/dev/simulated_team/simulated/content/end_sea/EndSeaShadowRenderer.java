package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.void_anchor.VoidAnchorBlockEntity;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.compat.IrisCompat;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.List;

public class EndSeaShadowRenderer {
    public static final float SHADOW_VOLUME_RADIUS = 256f / 2f;
    private static final Matrix4f PROJECTION_MAT = new Matrix4f();
    private static final Vector3d SHADOW_CAMERA_POSITION = new Vector3d();
    private static boolean isRenderingShadowMap = false;
    private static final ObjectArrayList<Vector3dc> voidAnchors = new ObjectArrayList<>();

    public static boolean isEnabled() {
        return true;
    }

    public static void renderShadowMap(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {
        if (!EndSeaShadowRenderer.isEnabled() ||
                stage != VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        if (IrisCompat.isLoaded() && IrisApi.getInstance().isRenderingShadowPass()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final EndSeaPhysics physics = EndSeaPhysicsData.of(level);

        if (physics == null) {
            return;
        }

        final AdvancedFbo fbo = getShadowsFramebuffer();
        if (fbo == null) {
            return;
        }

        final float zNear = 0.5f;

        final Matrix4f modelView = new Matrix4f();
        PROJECTION_MAT.identity().ortho(-SHADOW_VOLUME_RADIUS, SHADOW_VOLUME_RADIUS, -SHADOW_VOLUME_RADIUS, SHADOW_VOLUME_RADIUS, zNear, SHADOW_VOLUME_RADIUS);

        // account for the smaller screen size
        final Vec3 cameraPosition = camera.getPosition();
        final Vec3 shadowCameraPosition = new Vec3(cameraPosition.x, physics.startY() - SHADOW_VOLUME_RADIUS, cameraPosition.z);

        SHADOW_CAMERA_POSITION.set(JOMLConversion.toJOML(shadowCameraPosition));
        SHADOW_CAMERA_POSITION.set(Math.floor(SHADOW_CAMERA_POSITION.x), SHADOW_CAMERA_POSITION.y, Math.floor(SHADOW_CAMERA_POSITION.z));
        isRenderingShadowMap = true;

        final Quaternionf orientation = new Quaternionf().rotateX(Mth.DEG_TO_RAD * -90);
        final BoundingBox3dc bounds = new BoundingBox3d(-30_000_000, -10_000, -30_000_000, 30_000_000, 10_000, 30_000_000);

        final List<ClientSubLevel> clientSubLevelGroup = new ObjectArrayList<>();
        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(level, bounds);

        for (final SubLevel subLevel : intersecting) {
            clientSubLevelGroup.add((ClientSubLevel) subLevel);
        }

        fbo.bind(true);
        fbo.clear();
        SimpleSubLevelGroupRenderer.renderGroup(level, clientSubLevelGroup, fbo, modelView, PROJECTION_MAT, SHADOW_CAMERA_POSITION, orientation, SHADOW_VOLUME_RADIUS / 16f, false);
        isRenderingShadowMap = false;

        final PostProcessingManager post = VeilRenderSystem.renderer().getPostProcessingManager();
        final PostPipeline pipeline = post.getPipeline(Simulated.path("spread_end_sea"));
        if (pipeline != null) {
            for (int i = 0; i < 5; i++) {
                post.runPipeline(pipeline, false);
            }
        }
    }

    public static void renderVoidAnchors(final Camera camera) {
        if (voidAnchors.isEmpty()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();

        RenderSystem.setShaderTexture(0, Simulated.path("textures/effects/cracks.png"));
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        final ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            return;
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), minecraft.getWindow());
        shader.apply();

        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        final Vector3d pos = new Vector3d();
        final Vec3 cameraPos = camera.getPosition();

        for (final Vector3dc voidAnchor : voidAnchors) {
            // render quad
            final float size = 60;

            voidAnchor.sub(cameraPos.x, cameraPos.y, cameraPos.z, pos);
            final Matrix4f pose = new Matrix4f().translate((float) pos.x, (float) pos.y, (float) pos.z);
            builder.addVertex(pose, -size, 0, -size).setUv(0.0f, 0.0f).setColor(0.5f, 0, 0, 1);
            builder.addVertex(pose, size, 0, -size).setUv(1.0f, 0.0f).setColor(0.5f, 0, 0, 1);
            builder.addVertex(pose, size, 0, size).setUv(1.0f, 1.0f).setColor(0.5f, 0, 0, 1);
            builder.addVertex(pose, -size, 0, size).setUv(0.0f, 1.0f).setColor(0.5f, 0, 0, 1);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableDepthTest();
        shader.clear();

        voidAnchors.clear();
    }

    public static @Nullable AdvancedFbo getShadowsFramebuffer() {
        return VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Simulated.path("end_sea_shadows"));
    }

    public static boolean renderingShadowMap() {
        return isRenderingShadowMap;
    }

    public static Vector3dc getLastRenderOrigin() {
        return SHADOW_CAMERA_POSITION;
    }

    public static void addVoidAnchor(final VoidAnchorBlockEntity voidAnchor) {
        voidAnchors.add(Sable.HELPER.projectOutOfSubLevel(voidAnchor.getLevel(), JOMLConversion.atCenterOf(voidAnchor.getBlockPos())));
    }
}
