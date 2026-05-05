package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class TorsionSpringRenderer extends KineticBlockEntityRenderer<TorsionSpringBlockEntity> {
    public TorsionSpringRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final TorsionSpringBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        final Direction facing = be.getBlockState().getValue(TorsionSpringBlock.FACING);

        final SuperByteBuffer spring = CachedBuffers.partial(be.getBlockState().getValue(TorsionSpringBlock.VARIANT) == TorsionSpringBlock.Variant.IRON ? SimPartialModels.TORSION_SPRING : SimPartialModels.TORSION_SPRING_BRASS, be.getBlockState());
        final float angle = be.interpolatedSpring(partialTicks);
        kineticRotationTransform(spring, be, facing.getAxis(), Mth.DEG_TO_RAD * angle, light);
        if (facing.getAxis().isHorizontal()) {
            spring.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
        }
        spring.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)), Direction.EAST);
        spring.renderInto(ms, buffer.getBuffer(RenderType.solid()));

        final SuperByteBuffer shaftOut = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), facing);
        kineticRotationTransform(shaftOut, be, facing.getAxis(), getAngleForBe(be.getExtraKinetics(), be.getBlockPos(), facing.getAxis()), light);
        shaftOut.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(final TorsionSpringBlockEntity be, final BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, state
                .getValue(BearingBlock.FACING)
                .getOpposite());
    }
}
