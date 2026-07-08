package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ThrottleLeverBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<ThrottleLeverBlockEntity>, IWrenchable, CommonRedstoneBlock {
    public static MapCodec<ThrottleLeverBlock> CODEC = simpleCodec(ThrottleLeverBlock::new);
    public static BooleanProperty INVERTED = BooleanProperty.create("inverted");

    public ThrottleLeverBlock(final Properties builder) {
        super(builder);
        this.registerDefaultState(this.defaultBlockState().setValue(INVERTED, false));
    }

    private static void addParticles(final BlockState state, final LevelAccessor level, final BlockPos pos, final float alpha) {
        final Direction direction = state.getValue(FACING)
                .getOpposite();
        final Direction direction1 = getConnectedDirection(state).getOpposite();
        final double d0 =
                (double) pos.getX() + 0.5D + 0.1D * (double) direction.getStepX() + 0.2D * (double) direction1.getStepX();
        final double d1 =
                (double) pos.getY() + 0.5D + 0.1D * (double) direction.getStepY() + 0.2D * (double) direction1.getStepY();
        final double d2 =
                (double) pos.getZ() + 0.5D + 0.1D * (double) direction.getStepZ() + 0.2D * (double) direction1.getStepZ();
        level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), alpha), d0, d1, d2, 0.0D, 0.0D,
                0.0D);
    }

    static void updateNeighbors(final BlockState state, final Level world, final BlockPos pos) {
        world.updateNeighborsAt(pos, state.getBlock());
        world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (AllItems.WRENCH.isIn(player.getMainHandItem())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide && player.isLocalPlayer()) {
            addParticles(state, level, pos, 1.0F);
            return this.onBlockEntityUse(level, pos, be -> {
                if (!SimClickInteractions.THROTTLE_LEVER_MANAGER.isActive()) {
                    SimClickInteractions.THROTTLE_LEVER_MANAGER.startHold(level, player, pos);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            });
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        return direction != null;
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        final BlockPos pos = context.getClickedPos();
        final int signal = this.getSignal(state, level, pos, context.getClickedFace());

        addParticles(state, level, pos, 1.0F);
        level.setBlock(pos, state.cycle(INVERTED), 2);
        this.withBlockEntityDo(level, pos, (be) -> {
            be.setSignal(state.getValue(INVERTED) ? 15 - signal : signal);
        });
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
        return this.getBlockEntityOptional(blockAccess, pos).map(al -> al.state)
                .orElse(0);
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    public int getDirectSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
        return getConnectedDirection(blockState) == side ? this.getSignal(blockState, blockAccess, pos, side) : 0;
    }

    @Override
    public void animateTick(final BlockState pState, final Level pLevel, final BlockPos pPos, final RandomSource pRandom) {
        this.withBlockEntityDo(pLevel, pPos, be -> {
            if (be.state != 0 && pRandom.nextFloat() < 0.25F)
                addParticles(pState, pLevel, pPos, 0.5F);
        });
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
        if (isMoving || state.getBlock() == newState.getBlock())
            return;
        this.withBlockEntityDo(level, pos, be -> {
            if (be.state != 0)
                updateNeighbors(state, level, pos);
            level.removeBlockEntity(pos);
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (state.getValue(FACE) != AttachFace.WALL && state.getValue(FACING).getAxis() == Direction.Axis.X)
            return SimBlockShapes.THROTTLE_LEVER_SWAP.get(getConnectedDirection(state));
        return SimBlockShapes.THROTTLE_LEVER.get(getConnectedDirection(state));
    }

    public VoxelShape getHandleShape(final BlockState state) {
        if (state.getValue(FACE) != AttachFace.WALL && state.getValue(FACING).getAxis() == Direction.Axis.X)
            return SimBlockShapes.THROTTLE_LEVER_HANDLE_SWAP.get(getConnectedDirection(state));
        return SimBlockShapes.THROTTLE_LEVER_HANDLE.get(getConnectedDirection(state));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, FACE, INVERTED));
    }

    @Override
    public Class<ThrottleLeverBlockEntity> getBlockEntityClass() {
        return ThrottleLeverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThrottleLeverBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.THROTTLE_LEVER.get();
    }

    @Override
    protected boolean isPathfindable(final BlockState blockState, final PathComputationType pathComputationType) {
        return false;
    }
}
