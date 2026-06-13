package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MountedPotatoCannonBlock extends DirectionalAxisKineticBlock implements IBE<MountedPotatoCannonBlockEntity> {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty BLOCKED = BooleanProperty.create("blocked");

	public MountedPotatoCannonBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(BLOCKED, false));
	}

	@Override
	public Class<MountedPotatoCannonBlockEntity> getBlockEntityClass() {
		return MountedPotatoCannonBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MountedPotatoCannonBlockEntity> getBlockEntityType() {
		return AeroBlockEntityTypes.MOUNTED_POTATO_CANNON.get();
	}

	@Override
	protected ItemInteractionResult useItemOn(final ItemStack heldItem, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
		if (level.getBlockEntity(blockPos) instanceof final MountedPotatoCannonBlockEntity be) {
			final ContainerSlot slot = be.getInventory().slot;
			if (heldItem.isEmpty() && slot.isEmpty()) {
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}

			//either we were able to add more to the current stack, or we added a new stack
			ItemInfoWrapper info = ItemInfoWrapper.generateFromStack(heldItem);
			if (slot.isEmpty() || slot.getType() == heldItem.getItem()) {
				final long inserted = slot.insertStack(info, Math.min(heldItem.getCount(), 16), true);
				if (inserted > 0) {
					if (!level.isClientSide) {
						slot.insertStack(info, Math.min(heldItem.getCount(), 16), false);
					}

					if (!player.hasInfiniteMaterials()) {
						heldItem.shrink((int) inserted);
					}
					return ItemInteractionResult.sidedSuccess(level.isClientSide());
				}
			}

			//We're attempting to swap
			if (slot.getType() != heldItem.getItem() && slot.canInsert(info)) {
				final ItemStack extracted = slot.getStack().copy();
				player.getInventory().placeItemBackInInventory(extracted);
				slot.setStack(ItemStack.EMPTY);

				if (!level.isClientSide) {
					final long inserted = slot.insertStack(info, Math.min(heldItem.getCount(), 16), false);
					heldItem.shrink((int) inserted);
				}

				return ItemInteractionResult.SUCCESS;
			}
		}

		return super.useItemOn(heldItem, blockState, level, blockPos, player, interactionHand, blockHitResult);
	}

	@Override
	public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
		if (level.isClientSide) {
			return;
		}

		final boolean previouslyPowered = state.getValue(POWERED);
		if (previouslyPowered != level.hasNeighborSignal(pos)) {
			level.setBlock(pos, state.cycle(POWERED), 2);
		}

		this.withBlockEntityDo(level, pos, MountedPotatoCannonBlockEntity::sendData);
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED, BLOCKED);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(POWERED,
				context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	DirectionalAxisShaper MOUNTED_POTATO_CANNON = DirectionalAxisShaper.make(AeroBlockShapes.MOUNTED_POTATO_CANNON);
	DirectionalAxisShaper MOUNTED_POTATO_CANNON_BLOCKED = DirectionalAxisShaper.make(AeroBlockShapes.MOUNTED_POTATO_CANNON_BLOCKED);

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter getter, final BlockPos pos, final CollisionContext context) {
		return state.getValue(BLOCKED) ? this.MOUNTED_POTATO_CANNON_BLOCKED.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE))
				: this.MOUNTED_POTATO_CANNON.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
	}
}

