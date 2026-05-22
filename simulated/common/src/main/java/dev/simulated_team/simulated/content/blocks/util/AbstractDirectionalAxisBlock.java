package dev.simulated_team.simulated.content.blocks.util;

import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDirectionalAxisBlock extends DirectionalBlock implements TransformableBlock, IWrenchable {
    public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = BooleanProperty.create("axis_along_first");

    public AbstractDirectionalAxisBlock(final Properties properties) {
        super(properties);
    }

    //Placement
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AXIS_ALONG_FIRST_COORDINATE, FACING);
        super.createBlockStateDefinition(pBuilder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
        final boolean shift = context.isSecondaryUseActive();
        final Direction facing = this.getFacingForPlacement(context);
        boolean alongFirst = false;
        final Direction.Axis faceAxis = facing.getAxis();

        if (faceAxis.isHorizontal())
            alongFirst = faceAxis == Direction.Axis.Z;

        if (faceAxis.isVertical())
            alongFirst = this.getAxisAlignmentForPlacement(context);

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS_ALONG_FIRST_COORDINATE, shift != alongFirst);
    }

    protected boolean getAxisAlignmentForPlacement(final BlockPlaceContext context) {
        return context.getHorizontalDirection()
                .getAxis() == Direction.Axis.Z;
    }

    public static Direction.Axis getAxis(final BlockState state) {
        if (state.getBlock() instanceof AbstractDirectionalAxisBlock) {
            final Direction facing = state.getValue(FACING);
            final Direction.Axis gatheredAxis;

            if (facing.getAxis().isVertical()) {
                gatheredAxis = state.getValue(AXIS_ALONG_FIRST_COORDINATE) ? Direction.Axis.X : Direction.Axis.Z;
            } else {
                final boolean facingUp = state.getValue(AXIS_ALONG_FIRST_COORDINATE) != (facing.getStepX() == 0);
                gatheredAxis = facingUp ? Direction.Axis.Y : facing.getClockWise().getAxis();
            }

            return gatheredAxis;
        }

        return Direction.Axis.Y;
    }

    @Override
    public BlockState getRotatedBlockState(final BlockState originalState, final Direction targetedFace) {
        if (targetedFace == originalState.getValue(FACING)) {
            return IWrenchable.super.getRotatedBlockState(originalState, targetedFace)
                    .setValue(AXIS_ALONG_FIRST_COORDINATE, !originalState.getValue(AXIS_ALONG_FIRST_COORDINATE));
        }
        return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);
    }

    /**
     * Always returns the positive direction of the given AbstractDirectionalAxisBlock
     *
     * @return The normal of the current placed axis
     */
    @Nullable
    public static Direction getDirectionOfAxis(final BlockState state) {
        if (state.getBlock() instanceof AbstractDirectionalAxisBlock) {
            final Direction.Axis axis = getAxis(state);
            return Direction.get(Direction.AxisDirection.POSITIVE, axis);
        }

        return null;
    }

    protected Direction getFacingForPlacement(final BlockPlaceContext context) {
//        Direction facing = context.getNearestLookingDirection()
//                .getOpposite();
        return context.getClickedFace();
    }

    //Misc Overrides
    @Override
    public BlockState rotate(BlockState state, final Rotation rot) {
        if (rot.ordinal() % 2 == 1)
            state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState transform(BlockState state, final StructureTransform transform) {
        if (transform.mirror != null) {
            state = this.mirror(state, transform.mirror);
        }

        if (transform.rotationAxis == Direction.Axis.Y) {
            return this.rotate(state, transform.rotation);
        }

        final Direction newFacing = transform.rotateFacing(state.getValue(FACING));
        if (transform.rotationAxis == newFacing.getAxis() && transform.rotation.ordinal() % 2 == 1) {
            state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
        }
        return state.setValue(FACING, newFacing);
    }
}
