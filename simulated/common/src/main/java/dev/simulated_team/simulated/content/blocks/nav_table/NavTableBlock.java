package dev.simulated_team.simulated.content.blocks.nav_table;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NavTableBlock extends DirectionalBlock implements IBE<NavTableBlockEntity>, IWrenchable, CommonRedstoneBlock, SpecialBlockItemRequirement {
    public static final MapCodec<NavTableBlock> CODEC = simpleCodec(NavTableBlock::new);

    public NavTableBlock(final Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(FACING));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction clickedFace = context.getClickedFace();
        return super.getStateForPlacement(context).setValue(FACING, clickedFace);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        if (level.isClientSide() && this.canSwitchStacks(itemStack, level, blockPos)) {
            return ItemInteractionResult.SUCCESS;
        }

        if (this.switchStacks(level, blockPos, player, interactionHand)) {
            return ItemInteractionResult.CONSUME;
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult);
    }

    private boolean canSwitchStacks(ItemStack heldStack, Level level, BlockPos pos) {
        NavTableBlockEntity blockEntity = (NavTableBlockEntity) level.getBlockEntity(pos);
        if(blockEntity != null) {
            return heldStack.has(SimDataComponents.TARGET) || !blockEntity.getHeldItem().isEmpty() && heldStack.isEmpty();
        }
        return false;
    }

    private boolean switchStacks(final Level level, final BlockPos pos, final Player player, final InteractionHand hand) {
        boolean passed = false;

        final ItemStack heldItem = player.getItemInHand(hand);
        final NavigationTarget newTarget = NavigationTarget.ofStack(heldItem);
        if (heldItem.isEmpty() || newTarget != null) {
            this.withBlockEntityDo(level, pos, nav -> {
                final ContainerSlot slot = nav.inventory.slot;
                final ItemStack extract = slot.getStack().copy();
                final ItemStack insert = heldItem.copyWithCount(1);

                if (!extract.isEmpty()) {
                    NavigationTarget oldTarget = nav.getNavTableItem();
                    if (oldTarget != null) {
                        oldTarget.onExtract(extract, nav, player);
                    }
                }

                if (newTarget != null) {
                    newTarget.onInsert(insert, nav, player);
                }

                slot.setStack(insert);
                if (!player.hasInfiniteMaterials()) {
                    heldItem.shrink(1);
                }
                if (!extract.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(extract.copy());
                }

                final ItemStack newSlotItem = slot.getStack();
                nav.setChanged();
                nav.sendData();

                final float pitch = 0.8f + level.random.nextFloat() * 0.4f;
                final float volume = .75f;

                if (extract.isEmpty() && !newSlotItem.isEmpty()) {
                    // item inserted
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, volume, pitch);
                } else if (!extract.isEmpty() && newSlotItem.isEmpty()) {
                    // item picked up
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, volume, pitch);
                } else if (!extract.isEmpty()) {
                    // item changed
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, volume, pitch);
                }
            });

            passed = true;
        }

        return passed;
    }

    /* REDSTONE */
    @Override
    public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        final NavTableBlockEntity be = this.getBlockEntity(level, pos);
        if (be == null || direction.getAxis() == state.getValue(FACING).getAxis())
            return 0;

        return be.getRedstoneStrength(direction);
    }

    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        if (direction.getAxis() == state.getValue(FACING).getAxis()) {
            return 0;
        }

        if (state.getValue(FACING).getAxis().isHorizontal()) {
            if (direction == Direction.DOWN) {
                return this.getSignal(state, level, pos, direction);
            }
        }

        return 0;
    }

    @Override
    public boolean commonCheckWeakPower(final BlockState state, final SignalGetter level, final BlockPos pos, final Direction side) {
        return false;
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        if (direction == null)
            return false;

        return direction.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        this.withBlockEntityDo(level, pos, NavTableBlockEntity::dropHeldItem);
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SimBlockShapes.NAV_TABLE.get(state.getValue(FACING));
    }

    @Override
    public Class<NavTableBlockEntity> getBlockEntityClass() {
        return NavTableBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NavTableBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.NAVIGATION_TABLE.get();
    }

    @Override
    public ItemRequirement getRequiredItems(final BlockState state, @Nullable final BlockEntity blockEntity) {
        final ItemStack tableStack = SimBlocks.NAVIGATION_TABLE.asStack();
        if (blockEntity instanceof final NavTableBlockEntity ntbe) {
            final ItemStack heldItem = ntbe.getHeldItem();
            if (!heldItem.isEmpty()) {
                return new ItemRequirement(List.of(
                        new ItemRequirement.StackRequirement(tableStack, ItemRequirement.ItemUseType.CONSUME),
                        new ItemRequirement.StrictNbtStackRequirement(heldItem, ItemRequirement.ItemUseType.CONSUME)
                ));
            }
        }
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, tableStack);
    }
}
