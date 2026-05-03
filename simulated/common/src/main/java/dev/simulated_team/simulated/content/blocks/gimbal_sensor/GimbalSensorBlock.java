package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GimbalSensorBlock extends Block implements IBE<GimbalSensorBlockEntity>, IWrenchable, CommonRedstoneBlock {

    public static final Property<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public GimbalSensorBlock(final Properties pProperties) {
        super(pProperties);
    }
    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_AXIS);
        super.createBlockStateDefinition(builder);
    }
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }


    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    public int getSignal(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final Direction side) {
        final GimbalSensorBlockEntity be = this.getBlockEntity(pLevel, pPos);
        if (be == null)
            return 0;

        return be.getPower(side.getOpposite());
    }

    @Override
    public boolean commonCheckWeakPower(final BlockState state, final SignalGetter level, final BlockPos pos, final Direction side) {
        return false;
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        return direction != null;
    }

    @Override
    public Class<GimbalSensorBlockEntity> getBlockEntityClass() {
        return GimbalSensorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GimbalSensorBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.GIMBAL_SENSOR.get();
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rot) {
        final Direction.Axis axis = state.getValue(HORIZONTAL_AXIS);
        return state.setValue(HORIZONTAL_AXIS,
                rot.rotate(Direction.get(Direction.AxisDirection.POSITIVE, axis)).getAxis());
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext ctx) {
        return SimBlockShapes.GIMBAL_SENSOR;
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirrorIn) {
        return state;
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {

        final InteractionResult result = IWrenchable.super.onWrenched(state, context);

        if(result == InteractionResult.SUCCESS)
        {
            if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof final GimbalSensorBlockEntity be)
            {
                be.randomNudge();
            }
        }
        return result;
    }
}
