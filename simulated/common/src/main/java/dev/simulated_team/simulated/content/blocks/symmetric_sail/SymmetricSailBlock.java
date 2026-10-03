package dev.simulated_team.simulated.content.blocks.symmetric_sail;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.service.SimItemService;
import dev.simulated_team.simulated.util.placement_helpers.SymmetricSailPlacementHelper;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import dev.simulated_team.simulated.service.SimConfigService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SymmetricSailBlock extends RotatedPillarBlock implements IWrenchable, BlockSubLevelLiftProvider, SpecialBlockItemRequirement {
    private static final int placementHelperId = PlacementHelpers.register(new SymmetricSailPlacementHelper(SymmetricSailBlock::checkItem, SymmetricSailBlock::checkState));

    protected final DyeColor color;

    public SymmetricSailBlock(final Properties properties, final DyeColor color) {
        super(properties);
        this.color = color;
    }

    private static boolean checkItem(final ItemStack i) {
        return i.getItem() instanceof final BlockItem bi && bi.getBlock() instanceof SymmetricSailBlock;
    }

    private static boolean checkState(final BlockState state) {
        return state.getBlock() instanceof SymmetricSailBlock;
    }

    public static SymmetricSailBlock withCanvas(final Properties properties, final DyeColor color) {
        return new SymmetricSailBlock(properties, color);
    }

    public void applyDye(final BlockState state, final Level world, final BlockPos pos, final Vec3 hit, @Nullable final DyeColor color) {
        BlockState newState = (SimBlocks.DYED_SYMMETRIC_SAILS.get(color)).getDefaultState();
        newState = BlockHelper.copyProperties(state, newState);

        // Dye the block itself
        if (state != newState) {
            world.setBlockAndUpdate(pos, newState);
            return;
        }

        // Dye all adjacent
        final List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, hit, state.getValue(SymmetricSailBlock.AXIS));
        for (final Direction d : directions) {
            final BlockPos offset = pos.relative(d);
            final BlockState adjacentState = world.getBlockState(offset);
            final Block block = adjacentState.getBlock();
            if (!(block instanceof SymmetricSailBlock))
                continue;
            if (state.getValue(SymmetricSailBlock.AXIS) != adjacentState.getValue(SymmetricSailBlock.AXIS))
                continue;
            if (state == adjacentState)
                continue;
            world.setBlockAndUpdate(offset, newState);
            return;
        }

        // Dye all the things
        final List<BlockPos> frontier = new ArrayList<>();
        frontier.add(pos);
        final Set<BlockPos> visited = new HashSet<>();
        int timeout = 100;
        while (!frontier.isEmpty()) {
            if (timeout-- < 0)
                break;

            final BlockPos currentPos = frontier.removeFirst();
            visited.add(currentPos);

            for (final Direction d : Iterate.directions) {
                if (d.getAxis() == state.getValue(SymmetricSailBlock.AXIS))
                    continue;
                final BlockPos offset = currentPos.relative(d);
                if (visited.contains(offset))
                    continue;
                final BlockState adjacentState = world.getBlockState(offset);
                final Block block = adjacentState.getBlock();
                if (!(block instanceof SymmetricSailBlock))
                    continue;
                if (adjacentState.getValue(SymmetricSailBlock.AXIS) != state.getValue(SymmetricSailBlock.AXIS))
                    continue;
                if (state != adjacentState)
                    world.setBlockAndUpdate(offset, newState);
                frontier.add(offset);
                visited.add(offset);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        final ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        final DyeColor color = SimItemService.getDyeColor(heldItem);
        if (color != null) {
            if (!level.isClientSide)
                level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.1f - level.random.nextFloat() * .2f);
            this.applyDye(blockState, level, blockPos, blockHitResult.getLocation(), color);
            return ItemInteractionResult.SUCCESS;
        }

        final IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (placementHelper.matchesItem(heldItem)) {
            placementHelper.getOffset(player, level, blockState, blockPos, blockHitResult).placeInWorld(level, (BlockItem) heldItem.getItem(), player, interactionHand, blockHitResult);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(AXIS, pContext.getNearestLookingDirection().getAxis());
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext ctx) {
        return SimBlockShapes.SYMMETRIC_SAIL.get(pState.getValue(AXIS));
    }

    @Override
    public void fallOn(final Level level, final BlockState state, final BlockPos pos, final Entity entity, final float fallDistance) {
        super.fallOn(level, state, pos, entity, 0);
    }

    @Override
    public void updateEntityAfterFallOn(final BlockGetter level, final Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            this.bounce(entity);
        }
    }

    private void bounce(final Entity pEntity) {
        final Vec3 Vec3 = pEntity.getDeltaMovement();
        if (Vec3.y < 0.0D) {
            final double d0 = pEntity instanceof LivingEntity ? 1.0D : 0.8D;
            pEntity.setDeltaMovement(Vec3.x, -Vec3.y * (double) 0.26F * d0, Vec3.z);
        }
    }

    @Override
    public float sable$getLiftScalar() {
        return 0;
    }

    @Override
    public float sable$getParallelDragScalar() {
        return SimConfigService.INSTANCE.server().physics.symmetricSailParallelDragScalar.getF();
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return SimBlocks.WHITE_SYMMETRIC_SAIL.asStack();
    }

    @Override
    public @NotNull Direction sable$getNormal(final BlockState blockState) {
        return Direction.get(Direction.AxisDirection.POSITIVE, blockState.getValue(SymmetricSailBlock.AXIS));
    }

    @Override
    public ItemRequirement getRequiredItems(final BlockState state, @org.jetbrains.annotations.Nullable final BlockEntity blockEntity) {
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, SimBlocks.WHITE_SYMMETRIC_SAIL.asStack());
    }
}

