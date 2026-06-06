package dev.eriksonn.aeronautics.content.blocks.hot_air.envelope;

import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.simulated_team.simulated.service.SimItemService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EnvelopeBlockHelper {
    static ItemInteractionResult useItemOnHelper(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos) {
        final DyeColor color = SimItemService.getDyeColor(itemStack);

        if (color != null) {
            if (!level.isClientSide())
                level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.1f - level.random.nextFloat() * .2f);

            EnvelopeBlock.applyDye(blockState, level, blockPos, color);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    static InteractionResult onSneakWrenchedHelper(UseOnContext context) {
        final Level world = context.getLevel();
        if (world instanceof ServerLevel) {
            final Player player = context.getPlayer();
            if (player != null && !player.hasInfiniteMaterials())
                player.getInventory().placeItemBackInInventory(AeroBlocks.WHITE_ENVELOPE_BLOCK.asStack());
        }
        return InteractionResult.SUCCESS;
    }

    static void encasedBounceUp(final Entity pEntity) {
        final Vec3 vec3 = pEntity.getDeltaMovement();
        if (vec3.y < 0.0D) {
            final double d0 = pEntity instanceof LivingEntity ? 0.5D : 0.25D;
            pEntity.setDeltaMovement(vec3.x, -vec3.y * d0, vec3.z);
        }
    }
}
