package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.IHaveBigOutline;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.api.IDirectionalAnalogOutput;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.util.QuietUse;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class SteeringWheelBlock extends HorizontalDirectionalBlock
        implements IBE<SteeringWheelBlockEntity>, ProperWaterloggedBlock, IRotate, IHaveBigOutline, QuietUse, IDirectionalAnalogOutput {

    public static final BooleanProperty ON_FLOOR = BooleanProperty.create("on_floor");
    public static final MapCodec<SteeringWheelBlock> CODEC = simpleCodec(SteeringWheelBlock::new);

    public SteeringWheelBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(ON_FLOOR, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
        final boolean onFloor = state.getValue(ON_FLOOR);
        final Direction facing = state.getValue(FACING);

        if (context instanceof final EntityCollisionContext entityContext &&
                entityContext.getEntity() instanceof final Player player &&
                player.isLocalPlayer()) {
            final VoxelShape wheel = (onFloor ? SimBlockShapes.STEERING_WHEEL_FLOOR : SimBlockShapes.STEERING_WHEEL_CEILING).get(facing);
            final VoxelShape mount = SimBlockShapes.STEERING_WHEEL_MOUNT.get(facing);
            return lookingAtWheel(player, pos, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), wheel, mount) ? wheel : mount;
        }

        if (state.getValue(ON_FLOOR)) {
            return SimBlockShapes.STEERING_WHEEL_FULL_FLOOR.get(facing);
        } else {
            return SimBlockShapes.STEERING_WHEEL_FULL_CEILING.get(facing);
        }
    }

    // todo this whole approach is actually very bad because its very easy for the client and server to disagree
    public static boolean lookingAtWheel(final Player player, final BlockPos pos, final float pt, final BlockState state) {
        final boolean onFloor = state.getValue(ON_FLOOR);
        final Direction facing = state.getValue(FACING);

        final VoxelShape wheel = (onFloor ? SimBlockShapes.STEERING_WHEEL_FLOOR : SimBlockShapes.STEERING_WHEEL_CEILING).get(facing);
        final VoxelShape mount = SimBlockShapes.STEERING_WHEEL_MOUNT.get(facing);

        return lookingAtWheel(player, pos, pt, wheel, mount);
    }

    public static boolean lookingAtWheel(final Player player, final BlockPos pos, final float pt, final VoxelShape wheel, final VoxelShape mount) {
        Vec3 from = player.getEyePosition(pt);
        Vec3 to = from.add(player.getViewVector(pt).scale(player.blockInteractionRange()));
        final SubLevel subLevel = Sable.HELPER.getContaining(player.level(), pos);
        if (subLevel != null) {
            final Pose3dc pose;
            if (subLevel instanceof final ClientSubLevel clientSubLevel) {
                pose = clientSubLevel.renderPose(pt);
            } else {
                pose = subLevel.logicalPose();
            }
            from = pose.transformPositionInverse(from);
            to = pose.transformPositionInverse(to);
        }

        final BlockHitResult wheelResult = wheel.clip(from, to, pos);
        final BlockHitResult mountResult = mount.clip(from, to, pos);

        if (wheelResult == null || wheelResult.getType() == HitResult.Type.MISS) {
            return false;
        }

        if (mountResult == null || mountResult.getType() == HitResult.Type.MISS) {
            return true;
        }

        return wheelResult.getLocation().distanceTo(from) < mountResult.getLocation().distanceTo(from);
    }

    @Override
    protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        if (state.getValue(ON_FLOOR)) {
            return SimBlockShapes.STEERING_WHEEL_FULL_FLOOR.get(state.getValue(FACING));
        } else {
            return SimBlockShapes.STEERING_WHEEL_FULL_CEILING.get(state.getValue(FACING));
        }
    }

    @Deprecated
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext pContext) {
        return SimBlockShapes.STEERING_WHEEL_MOUNT.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(WATERLOGGED).add(FACING).add(ON_FLOOR));
    }

    @Override
    public @Nullable InteractionResult quietUse(final Player player, final InteractionHand hand, final BlockPos pos, final BlockState state) {
        if (lookingAtWheel(player, pos, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), state)) {
            return this.getBlockEntityOptional(player.level(), pos).map( be -> {
                if (!be.held &&
                        !be.isMaterialValid(player.getItemInHand(hand)) &&
                        !be.angleInput.testHit(this.getPlayerHitLocation())
                ) {
                    SimClickInteractions.STEERING_WHEEL_MANAGER.startHold(player.level(), player, pos);
                    return InteractionResult.SUCCESS;
                }
                return null;
            }).orElse(null);
        }
        return null;
    }

    public Vec3 getPlayerHitLocation() {
        return Minecraft.getInstance().hitResult.getLocation();
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return this.onBlockEntityUseItemOn(level, pos, be -> be.applyMaterialIfValid(stack));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState defaultState = this.withWater(this.defaultBlockState(), context);
        final boolean floor = switch (context.getClickedFace()) {
            case UP -> true;
            case DOWN -> false;
            default -> {
                final Direction verticalLookDir = Arrays.stream(context.getNearestLookingDirections())
                        .filter(d -> d.getAxis().isVertical()).findFirst().get();
                yield verticalLookDir == Direction.DOWN;
            }
        };

        final Direction horizontalLookDir = Arrays.stream(context.getNearestLookingDirections())
                .filter(d -> d.getAxis().isHorizontal()).findFirst().get();
        return defaultState.setValue(FACING, horizontalLookDir.getOpposite()).setValue(ON_FLOOR, floor);
    }

    @Override
    public BlockState updateShape(final BlockState pState, final Direction pDirection, final BlockState pNeighborState,
                                  final LevelAccessor pLevel, final BlockPos pCurrentPos, final BlockPos pNeighborPos) {
        this.updateWater(pLevel, pState, pCurrentPos);
        return pState;
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignalFrom(final BlockState blockState, final Level level, final BlockPos blockPos, final Direction dir) {
        final Direction facing = blockState.getValue(FACING);
        final SteeringWheelBlockEntity be = this.getBlockEntity(level, blockPos);

        final float frac = Mth.clamp(be.targetAngleToUpdate / be.angleInput.getValue(), -1, 1);

        if (facing == dir) {
            return be.held ? 15 : 0;
        }

        if (Math.abs(be.getAngle()) < 0.99) {
            return 0;
        }
        final int value = (int) (((frac < 0 ? Math.floor(frac * 15) : Math.ceil(frac * 15)) *
                ((facing.getStepX() == 1 || facing.getStepZ() == 1) ? -1 : 1)));

        if (facing.getClockWise() == dir && value > 0) {
            return value;
        } else if (facing.getCounterClockWise() == dir && value < 0) {
            return -value;
        }
        return 0;
    }

    @Override
    public FluidState getFluidState(final BlockState pState) {
        return this.fluidState(pState);
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return face == (state.getValue(ON_FLOOR) ? Direction.DOWN : Direction.UP);
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public Class<SteeringWheelBlockEntity> getBlockEntityClass() {
        return SteeringWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteeringWheelBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.STEERING_WHEEL.get();
    }
}
