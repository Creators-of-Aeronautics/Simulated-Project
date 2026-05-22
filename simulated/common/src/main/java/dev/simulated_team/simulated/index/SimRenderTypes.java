package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.simulated_team.simulated.Simulated;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Function;

public final class SimRenderTypes extends RenderType {

    private static final RenderType STAFF_OVERLAY = create(
            Simulated.MOD_ID + ":staff_overlay/staff_overlay",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_STRIP,
            TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(Simulated.path("staff_overlay/staff_overlay")))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(CULL)
                    .createCompositeState(false)
    );
    private static final RenderType LASER = create(
            Simulated.MOD_ID + ":laser",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(Simulated.path("laser/laser")))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );
    private static final RenderType LENS = create(
            Simulated.MOD_ID + ":laser_pointer_lens",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            TRANSIENT_BUFFER_SIZE,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setLightmapState(LIGHTMAP)
                    .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                    .setTextureState(BLOCK_SHEET_MIPPED)
                    .setShaderState(VeilRenderBridge.shaderState(Simulated.path("laser_pointer/lens")))
                    .createCompositeState(true));

    private static final VertexFormat SPRING_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Stress", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build();

    private static final RenderType LOCK = create(
            Simulated.MOD_ID + ":lock",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            TRANSIENT_BUFFER_SIZE,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setTextureState(new RenderStateShard.TextureStateShard(Simulated.path("textures/gui/lock.png"), false, false))
                    .createCompositeState(true));

    private static final RenderType ROPE = create(
            Simulated.MOD_ID + ":rope",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            TRANSIENT_BUFFER_SIZE,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(Simulated.path("rope/rope")))
                    .setTextureState(new RenderStateShard.TextureStateShard(Simulated.path("textures/block/rope_particle.png"), false, false))
                    .setLightmapState(LIGHTMAP)
                    .setCullState(CULL)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> SPRING = Util.memoize((ResourceLocation texture) -> {
        CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(VeilRenderBridge.shaderState(Simulated.path("spring/spring")))
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create("spring", SPRING_FORMAT, VertexFormat.Mode.QUADS, TRANSIENT_BUFFER_SIZE, true, false, state);
    });

    private SimRenderTypes(final String name, final VertexFormat format, final VertexFormat.Mode mode, final int bufferSize, final boolean affectsCrumbling, final boolean sortOnUpload,
                           final Runnable setupState, final Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType staffOverlay() {
        return STAFF_OVERLAY;
    }

    public static RenderType laser() {
        return LASER;
    }

    public static RenderType lens() {
        return LENS;
    }

    public static RenderType lock() {
        return LOCK;
    }

    public static RenderType rope() {
        return ROPE;
    }

    public static RenderType itemGlowingSolid(boolean shadersActive) {
        return shadersActive ? Sheets.solidBlockSheet() : RenderTypes.itemGlowingSolid();
    }

    public static RenderType itemGlowingTranslucent(boolean shadersActive) {
        return shadersActive ? Sheets.translucentCullBlockSheet() : RenderTypes.itemGlowingTranslucent();
    }

    public static RenderType spring(final ResourceLocation texture) {
        return SPRING.apply(texture);
    }
}
