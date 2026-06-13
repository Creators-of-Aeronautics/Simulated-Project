package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class HotAirBurnerRenderer extends SmartBlockEntityRenderer<HotAirBurnerBlockEntity> {
    private static final ResourceLocation BURNER_FLAME_SHADER = Aeronautics.path("burner_flame");

    public HotAirBurnerRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final HotAirBurnerBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
        final float signalStrength = Math.max(0, be.getSignalStrength() / 15F);
        final SuperByteBuffer indicator = CachedBuffers.partial(AeroPartialModels.HOT_AIR_BURNER_INDICATOR, be.getBlockState());
        final VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        indicator.light(light)
                .color(SimColors.redstone(signalStrength))
                .renderInto(ms, vb);

        if (signalStrength <= 0.0) {
            return;
        }

        ms.pushPose();
        ms.translate(-0.5, 0.35, 0.5);

        final BlockPos pos = be.getBlockPos();
        final Vec3 center = pos.getCenter();

        final Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        if (be.getLevel() instanceof PonderLevel) {
            camera = minecraft.getCameraEntity().getPosition(partialTicks);
        }

        final SubLevel sublevel = Sable.HELPER.getContaining(be);
        if (sublevel != null) {
            camera = sublevel.logicalPose().transformPositionInverse(camera);
        }

        final float angle = (float) Math.atan2(camera.z() - center.z(), camera.x() - center.x());

        final HotAirBurnerBlock.Variant variant = be.getBlockState().getValue(HotAirBurnerBlock.VARIANT);
        final float palette = variant == HotAirBurnerBlock.Variant.FIRE ? 0.25f : 0.75f;

        final ShaderProgram shader = VeilRenderSystem.setShader(BURNER_FLAME_SHADER);
        if (shader != null) {
            final float flameRenderTime = (float) Mth.lerp(partialTicks, be.lastRenderTime, be.renderTime) + be.getTimeOffset();
            shader.getUniformSafe("FlameRenderTime").setFloat(flameRenderTime);
            shader.getUniformSafe("Intensity").setFloat(be.getFlameIntensity(partialTicks));
            shader.getUniformSafe("Palette").setFloat(palette);

            ms.rotateAround(Axis.YP.rotation((float) (-angle + Math.PI * 0.5f)), 1.0f, 0.0f, 0.0f);
            renderFlame(ms);
            ms.popPose();
        }

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
    }

    private static void renderFlame(final PoseStack poseStack) {
        final float size = 2.0f;

        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        final Matrix4f pose = poseStack.last().pose();
        builder.addVertex(pose, 0.0f, 0.0f, 0.0f).setUv(0.0f, 1.0f);
        builder.addVertex(pose, size, 0.0f, 0.0f).setUv(1.0f, 1.0f);
        builder.addVertex(pose, size, size, 0.0f).setUv(1.0f, 0.0f);
        builder.addVertex(pose, 0.0f, size, 0.0f).setUv(0.0f, 0.0f);

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();

    }
}
