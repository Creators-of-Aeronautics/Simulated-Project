package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.mixin_interface.PlayerTypewriterExtension;
import dev.simulated_team.simulated.service.SimMenuService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LinkedTypewriterBlock extends HorizontalDirectionalBlock implements IBE<LinkedTypewriterBlockEntity>, IWrenchable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<LinkedTypewriterBlock> CODEC = simpleCodec(LinkedTypewriterBlock::new);
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    public LinkedTypewriterBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED).add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext pContext) {
        final Direction dir = pContext.getHorizontalDirection().getOpposite();

        assert pContext.getPlayer() != null;
        return this.defaultBlockState().setValue(HORIZONTAL_FACING, pContext.getPlayer().isShiftKeyDown() ? dir.getOpposite() : dir);
    }

    @Override
    public Class<LinkedTypewriterBlockEntity> getBlockEntityClass() {
        return LinkedTypewriterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LinkedTypewriterBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.LINKED_TYPEWRITER.get();
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter blockGetter, final BlockPos pos, final CollisionContext context) {
        return SimBlockShapes.LINKED_TYPEWRITER.get(state.getValue(HORIZONTAL_FACING));
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        final ItemStack heldItem = player.getItemInHand(interactionHand);

        final Item linkedControllerItem = AllItems.LINKED_CONTROLLER.asItem();
        if (player.getMainHandItem().is(linkedControllerItem) || player.getOffhandItem().is(linkedControllerItem)) {
            if (level.isClientSide) {
                final ItemStack item = player.getMainHandItem().is(linkedControllerItem) ?
                        player.getMainHandItem() : player.getOffhandItem();
                player.displayClientMessage(SimLang.translate("linked_typewriter.linked_controller_copy").component(), true);
                LinkedTypewriterInteractionHandler.sendLinkedControllerData(level, blockPos, item);
            }

            LinkedControllerClientHandler.MODE = LinkedControllerClientHandler.Mode.IDLE;
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (heldItem.isEmpty() && interactionHand == InteractionHand.MAIN_HAND) {
            final MutableBoolean success = new MutableBoolean(false);

            this.withBlockEntityDo(level, blockPos, (be) -> {
                final UUID uuid = player.getUUID();

                if (player.isShiftKeyDown() && be.checkAndStartUsing(uuid)) {
                    if (!level.isClientSide) {
                        this.displayScreen(be, player);
                    } else {
                        LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.SCREEN_BINDING);
                    }

                    success.setTrue();
                    return;
                }

                // check the user with this block entity, and disconnect previous one
                if (be.checkAndStartUsing(uuid)) {
                    success.setTrue();
                    return;
                }

                // disconnect if the user is interacting with this typewriter again
                if (be.checkUser(uuid)) {
                    ((PlayerTypewriterExtension) player).simulated$setCurrentTypewriter(null);
                    be.disconnectUser();
                    success.setTrue();
                }
            });

            if (success.getValue()) {
                return ItemInteractionResult.SUCCESS;
            }
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult);
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    public InteractionResult onSneakWrenched(final BlockState state, final UseOnContext context) {
        final Level world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Player player = context.getPlayer();
        if (world instanceof ServerLevel) {
            if (player != null && player.isCreative()) {
                Block.getDrops(state, (ServerLevel) world, pos, world.getBlockEntity(pos), player, context.getItemInHand()).forEach((itemStack) -> {
                    player.getInventory().placeItemBackInInventory(itemStack);
                });
            }
        }
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final ItemStack itemStack = super.getCloneItemStack(level, pos, state);
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), blockEntity.saveWithoutMetadata(level.registryAccess()));
            if (blockEntity.components().has(DataComponents.CUSTOM_NAME)) {
                itemStack.set(DataComponents.CUSTOM_NAME, blockEntity.components().get(DataComponents.CUSTOM_NAME));
            }
        }

        return itemStack;
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        assert level != null;

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && !level.isClientSide && player.isCreative() &&
                blockEntity instanceof final LinkedTypewriterBlockEntity linkedTypewriterBlockEntity && (!linkedTypewriterBlockEntity.getTypewriterEntries().getKeyMap().isEmpty() || linkedTypewriterBlockEntity.components().has(DataComponents.CUSTOM_NAME))) {

            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), this.getCloneItemStack(level, pos, state));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull final BlockState state, final LootParams.Builder params) {
        final BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof final LinkedTypewriterBlockEntity typewriter) {
            final ItemStack itemStack = new ItemStack(this);
            typewriter.saveToItem(itemStack, params.getLevel().registryAccess());

            params.withDynamicDrop(ShulkerBoxBlock.CONTENTS, consumer -> itemStack.copy());
            return ImmutableList.of(itemStack);
        }
        return super.getDrops(state, params);
    }

    protected void displayScreen(final LinkedTypewriterBlockEntity be, final Player player) {
        SimMenuService.INSTANCE.openScreen((ServerPlayer) player, be, be::sendToMenu);
    }
}
