package dev.simulated_team.simulated.content.items.rope.RopeItem;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RopeItem extends Item {

    public RopeItem(final Properties properties) {
        super(properties);
    }

    /**
     * Checks if a location is valid for rope to attach to
     *
     * @param level    the level to check in
     * @param blockPos the block to check
     * @return if rope can attach to this block
     */
    public static boolean isValidRopeAttachment(final Level level, final BlockPos blockPos) {
        boolean validLocation = false;
        if (level.getBlockEntity(blockPos) instanceof final SmartBlockEntity smartBlockEntity) {
            final RopeStrandHolderBehavior behavior = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

            if (behavior != null && !behavior.isAttached()) {
                validLocation = true;
            }
        }
        return validLocation;
    }

    public static RopeStrandHolderBehavior getRopeHolder(final Level level, final BlockPos blockPos) {
        RopeStrandHolderBehavior holder = null;
        if (level.getBlockEntity(blockPos) instanceof final SmartBlockEntity smartBlockEntity) {
            final RopeStrandHolderBehavior behavior = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

            if (behavior != null) {
                holder = behavior;
            }
        }
        return holder;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final BlockPos clickedPos = context.getClickedPos();
        final Level level = context.getLevel();
        final ItemStack heldStack = context.getItemInHand();
        final Player player = context.getPlayer();

        final boolean validLocation = isValidRopeAttachment(level, clickedPos);

        if (player != null && player.isShiftKeyDown()) {
            heldStack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);
            return InteractionResult.SUCCESS;
        }

        if (validLocation) {
            if (heldStack.has(SimDataComponents.ROPE_FIRST_CONNECTION)) {

                if (!level.isClientSide) {
                    if (!this.attachRope(level, heldStack.get(SimDataComponents.ROPE_FIRST_CONNECTION), clickedPos)) {
                        // failure to connect
                        heldStack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);
                        return InteractionResult.SUCCESS;
                    } else {
                        // we attached the rope!
                        SimAdvancements.LEARNING_THE_ROPES.awardTo(player);
                    }
                }

                heldStack.remove(SimDataComponents.ROPE_FIRST_CONNECTION);

                if (!player.isCreative())
                    context.getItemInHand()
                            .shrink(1);

                return InteractionResult.SUCCESS;
            }

            heldStack.set(SimDataComponents.ROPE_FIRST_CONNECTION, clickedPos);
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    /**
     * Attaches a rope between two positions
     *
     * @param posA the position of the first block entity, assumed to have a {@link RopeStrandHolderBehavior}
     * @param posB the position of the second block entity, assumed to have a {@link RopeStrandHolderBehavior}
     */
    private boolean attachRope(final Level level, final BlockPos posA, final BlockPos posB) {
        RopeStrandHolderBehavior ropeHolderA = getRopeHolder(level, posA);
        if (ropeHolderA == null) return false;

        RopeStrandHolderBehavior ropeHolderB = getRopeHolder(level, posB);
        if (ropeHolderB == null) return false;

        if (ropeHolderB.blockEntity instanceof RopeWinchBlockEntity && !(ropeHolderA.blockEntity instanceof RopeWinchBlockEntity)) {
            final RopeStrandHolderBehavior temp = ropeHolderA;
            ropeHolderA = ropeHolderB;
            ropeHolderB = temp;
        }
        if (ropeHolderA.blockEntity instanceof RopeWinchBlockEntity && ropeHolderB.blockEntity instanceof RopeWinchBlockEntity) {
            return false;
        }

        if (ropeHolderA.createRope(ropeHolderB)) {
            level.playSound(null, posA, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1F);
            level.playSound(null, posB, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5F, 1F);
            return true;
        }
        return false;
    }
}
