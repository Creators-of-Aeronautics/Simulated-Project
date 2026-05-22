package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.SimItemService;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

public class PortableEngineBlock extends HorizontalKineticBlock implements IBE<PortableEngineBlockEntity> {
    private final static int BURN_TIME_THRESHOLD = 10 * 20 /* conversion from seconds to ticks */;
    protected final DyeColor color;

    public PortableEngineBlock(final Properties properties, final DyeColor color) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
        this.color = color;
    }

    public static boolean isLitState(final BlockState blockState) {
        return blockState.getValue(LIT);
    }

    public static Couple<Integer> getSpeedRange() {
        return Couple.create(32, 32);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
        if (state.hasBlockEntity() && !SimBlocks.PORTABLE_ENGINES.contains(newState.getBlock())) {
            final PortableEngineBlockEntity be = (PortableEngineBlockEntity) level.getBlockEntity(pos);

            if (be != null && !be.inventory.isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.inventory.getItem(0));

            level.removeBlockEntity(pos);
        }
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction preferred = this.getPreferredHorizontalFacing(context);
        if (preferred == null || (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())) {
            final Direction horizontalDirection = context.getHorizontalDirection();
            return this.defaultBlockState().setValue(HORIZONTAL_FACING, (context.getPlayer() != null && context.getPlayer()
                    .isShiftKeyDown()) ? horizontalDirection.getOpposite() : horizontalDirection);
        }
        return this.defaultBlockState().setValue(HORIZONTAL_FACING, preferred);

    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldItem, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        final PortableEngineBlockEntity be = (PortableEngineBlockEntity) level.getBlockEntity(blockPos);

        final PortableEngineInventory inventory = be.inventory;
        final ContainerSlot slot = inventory.slot;
        final ItemStack currentItemStack = slot.getStack().copy();
        if (currentItemStack.isEmpty() && heldItem.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        final DyeColor color = SimItemService.getDyeColor(heldItem);
        if (color != null) {
            if (!level.isClientSide)
                level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.1f - level.random.nextFloat() * .2f);

            final BlockState newState = BlockHelper.copyProperties(blockState, SimBlocks.PORTABLE_ENGINES.get(color).getDefaultState());
            level.setBlockAndUpdate(blockPos, newState);

            return ItemInteractionResult.SUCCESS;
        }

        if (AllItems.CREATIVE_BLAZE_CAKE.isIn(heldItem)) {
            if (!level.isClientSide) {
                if (be.isCurrentFuelInfinite()) {
                    if (be.isSuperHeated()) {
                        be.setCurrentBurnTime(0);
                        be.setSuperHeated(false);
                    } else {
                        be.setSuperHeated(true);
                    }
                } else {
                    be.setCurrentBurnTime(PortableEngineBlockEntity.INFINITE_THRESHOLD);
                }
            }
            if (!player.hasInfiniteMaterials()) {
                heldItem.shrink(1);
                player.setItemInHand(interactionHand, heldItem);
            }
        } else {
            if ((!heldItem.isEmpty() && !inventory.canInsertItem(ItemInfoWrapper.generateFromStack(heldItem)))) {
                return ItemInteractionResult.FAIL;
            }

            if (currentItemStack.isEmpty()) {
                slot.setStack(heldItem);
                player.setItemInHand(interactionHand, ItemStack.EMPTY);
                if (be.getCurrentBurnTime() <= 0) {
                    SimStats.PORTABLE_ENGINES_FED.awardTo(player);
                }
            } else if (ItemStack.isSameItem(heldItem, currentItemStack) && ItemStack.isSameItemSameComponents(heldItem, currentItemStack)) {
                int targetAmount = currentItemStack.getCount() + heldItem.getCount();
                targetAmount = Math.min(targetAmount, currentItemStack.getMaxStackSize());
                final int transferAmount = Math.min(targetAmount - currentItemStack.getCount(), heldItem.getCount());

                if (transferAmount <= 0)
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);

                slot.shrink(-transferAmount);
                heldItem.shrink(transferAmount);
                player.setItemInHand(interactionHand, heldItem);
            } else {
                slot.setStack(ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(currentItemStack);
                level.playSound(null, blockPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f,
                        1f + level.getRandom().nextFloat());
            }

            if (be.getTotalBurnTime() >= 20 * 60 * 60 * 10 && !be.isTotalFuelInfinite()) {
                SimAdvancements.THAT_SHOULD_DO_FOR_NOW.awardTo(player);
            }
        }

        if (!slot.isEmpty()) {
            SimAdvancements.STEAMLESS_ENGINE.awardTo(player);
        }

        be.notifyUpdate();

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(final BlockState pState, final Level pLevel, final BlockPos pPos) {
        final PortableEngineBlockEntity be = this.getBlockEntity(pLevel, pPos);
        int power = 0;

        if (be != null) {
            if (be.isTotalFuelInfinite())
                return 15;

            final int ticks = be.getTotalBurnTime();
            if (ticks > 0) {
                power = Math.min(ticks / BURN_TIME_THRESHOLD, 14) + 1;
            }
        }

        return power;
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext ctx) {
        return SimBlockShapes.PORTABLE_ENGINE.get(pState.getValue(HORIZONTAL_FACING));
    }

    @Override
    public Class<PortableEngineBlockEntity> getBlockEntityClass() {
        return PortableEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PortableEngineBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.PORTABLE_ENGINE.get();
    }
}