package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ModulatingLinkedReceiverBlock extends WrenchableDirectionalBlock implements IBE<ModulatingLinkedReceiverBlockEntity>, IWrenchable, CommonRedstoneBlock {
    public static final MapCodec<ModulatingLinkedReceiverBlock> CODEC = simpleCodec(ModulatingLinkedReceiverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    static {
        BlockMovementChecksImpl.registerAttachedCheck((state, world, pos, direction) -> {
            final BlockState relativeState = world.getBlockState(pos.relative(direction));
            if (state.getBlock() instanceof ModulatingLinkedReceiverBlock && state.getValue(FACING) == direction.getOpposite()) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            if (relativeState.getBlock() instanceof ModulatingLinkedReceiverBlock && relativeState.getValue(FACING) == direction) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }

            return BlockMovementChecks.CheckResult.PASS;
        });
    }

    public ModulatingLinkedReceiverBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<ModulatingLinkedReceiverBlockEntity> getBlockEntityClass() {
        return ModulatingLinkedReceiverBlockEntity.class;
    }


    @Override
    public BlockEntityType<? extends ModulatingLinkedReceiverBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.MODULATING_LINKED_RECEIVER.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        state = state.setValue(FACING, context.getClickedFace());
        return state;
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    public int getDirectSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
        if (side != blockState.getValue(FACING))
            return 0;
        return this.getSignal(blockState, blockAccess, pos, side);
    }

    @Override
    public int getSignal(final BlockState state, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {

        return this.getBlockEntityOptional(blockAccess, pos).map(ModulatingLinkedReceiverBlockEntity::getReceivedSignal)
                .orElse(0);
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext pContext) {
        return SimBlockShapes.MODULATING_DIRECTIONAL_LINK.get(pState.getValue(FACING));
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block blockIn, final BlockPos fromPos,
                                final boolean isMoving) {
        if (level.isClientSide)
            return;

        final Direction blockFacing = state.getValue(FACING);
        if (fromPos.equals(pos.relative(blockFacing.getOpposite()))) {
            if (!this.canSurvive(state, level, pos)) {
                level.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockPos neighbourPos = pos.relative(state.getValue(FACING)
                .getOpposite());
        final BlockState neighbour = level.getBlockState(neighbourPos);
        return !neighbour.canBeReplaced();
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
        return side != null;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        if (!player.getMainHandItem().is(SimBlocks.LINKED_TYPEWRITER.asItem())) { //TODO: make this more generalized
	        if (level.isClientSide()) {
                this.withBlockEntityDo(level, blockPos, ModulatingLinkedReceiverScreen::open);
	        }

	        return ItemInteractionResult.SUCCESS;
        }

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
