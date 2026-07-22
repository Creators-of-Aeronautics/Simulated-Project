package dev.eriksonn.aeronautics.content.blocks.hot_air.envelope;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class EnvelopeBlock extends CasingBlock implements Envelope, SpecialBlockItemRequirement {
    private static final BlockPos[] DIRECTION_OFFSETS = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(1, 1, 0), new BlockPos(-1, -1, 0), new BlockPos(1, -1, 0), new BlockPos(-1, 1, 0), new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(0, 1, 1), new BlockPos(0, -1, -1), new BlockPos(0, -1, 1), new BlockPos(0, 1, -1)};
    protected final DyeColor color;

    public EnvelopeBlock(final Properties properties, final DyeColor color) {
        super(properties);
        this.color = color;
    }

    protected static void applyDye(final BlockState state, final Level level, final BlockPos pos, final DyeColor color) {
        final BlockState newEnvelopeState = BlockHelper.copyProperties(state, AeroBlocks.DYED_ENVELOPE_BLOCKS.get(color).getDefaultState());
        final BlockState newEncasedEnvelopeState = BlockHelper.copyProperties(state, AeroBlocks.ENVELOPE_ENCASED_SHAFTS.get(color).getDefaultState());

        // Dye the block itself
        if (selfDye(level, pos, state, color)) {
            return;
        }

        // Dye all adjacent
        boolean hasDyed = false;
        for (final Direction d : Iterate.directions) {
            final BlockPos offset = pos.relative(d);
            final BlockState adjacentState = level.getBlockState(offset);
            if (!selfDye(level, offset, adjacentState, color)) {
                continue;
            }

            hasDyed = true;
        }
        if (hasDyed)
            return;

        // Dye all the things
        final List<BlockPos> frontier = new ObjectArrayList<>();
        frontier.add(pos);
        final Set<BlockPos> visited = new ObjectOpenHashSet<>();
        float timeout = 125F;
        while (!frontier.isEmpty()) {
            if (timeout-- < 0.0F)
                break;

            final BlockPos currentPos = frontier.removeFirst();
            visited.add(currentPos);

            for (final BlockPos d : DIRECTION_OFFSETS) {
                final BlockPos offsetPos = currentPos.offset(d);
                if (visited.contains(offsetPos))
                    continue;

                final BlockState adjacentState = level.getBlockState(offsetPos);
                if (!(multiDye(level, offsetPos, adjacentState, newEnvelopeState) || multiDye(level, offsetPos, adjacentState, newEncasedEnvelopeState))) {
                    continue;
                }

                frontier.add(offsetPos);
                visited.add(offsetPos);
            }
        }
    }

    static boolean selfDye(final Level level, final BlockPos pos, final BlockState state, final DyeColor color) {
        if (state.getBlock() instanceof final EnvelopeBlock eb && eb.getColor() != color) {
            level.setBlockAndUpdate(pos, AeroBlocks.DYED_ENVELOPE_BLOCKS.get(color).getDefaultState());
            return true;
        }

        if (state.getBlock() instanceof final EnvelopeEncasedShaftBlock eb && eb.getColor() != color) {
            final Direction.Axis axis = eb.getRotationAxis(state);
            level.setBlockAndUpdate(pos, AeroBlocks.ENVELOPE_ENCASED_SHAFTS.get(color).getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, axis));
            return true;
        }

        return false;
    }

    static boolean multiDye(final Level Level, final BlockPos pos, final BlockState state, final BlockState newState) {
        if (state.getBlock() instanceof EnvelopeBlock && newState.getBlock() instanceof EnvelopeBlock) {
            if (state != newState) {
                Level.setBlockAndUpdate(pos, newState);
            }
            return true;
        }

        if (state.getBlock() instanceof EnvelopeEncasedShaftBlock && newState.getBlock() instanceof EnvelopeEncasedShaftBlock) {
            if (state != newState) {
                final Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
                Level.setBlockAndUpdate(pos, newState.setValue(RotatedPillarKineticBlock.AXIS, axis));

            }
            return true;
        }

        return false;
    }

    @Override
    protected int getLightBlock(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return 1;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        return EnvelopeBlockHelper.useItemOnHelper(itemStack, blockState, level, blockPos);
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader levelReader, final BlockPos blockPos, final BlockState blockState) {
        return AeroBlocks.DYED_ENVELOPE_BLOCKS.get(this.color).asStack();
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public void fallOn(final Level pLevel, final BlockState pState, final BlockPos pPos, final Entity pEntity, final float pFallDistance) {
        if (pEntity.isSuppressingBounce()) {
            super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance);
        } else {
            pEntity.causeFallDamage(pFallDistance, 0.0F, pLevel.damageSources().fall());
        }

    }

    @Override
    public void updateEntityAfterFallOn(final BlockGetter pLevel, final Entity pEntity) {
        if (pEntity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(pLevel, pEntity);
        } else {
            this.bounceUp(pEntity);
        }
    }

    private void bounceUp(final Entity entity) {
        final Vec3 vec3 = entity.getDeltaMovement();
        if (vec3.y < 0.0) {
            final double scale = 0.65 * (entity instanceof LivingEntity ? 1.0 : 0.8);
            entity.setDeltaMovement(vec3.x, -vec3.y * scale, vec3.z);
        }
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack stack = AeroBlocks.WHITE_ENVELOPE_BLOCK.asStack();
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, stack);
    }
}