package dev.simulated_team.simulated.content.blocks.rope;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public interface RopeHolderBlock <T extends SmartBlockEntity> extends BlockSubLevelAssemblyListener, IBE<T> {
    static <T extends SmartBlockEntity> ItemInteractionResult shearRope(final RopeHolderBlock<T> block, final Level level, final BlockPos pos, final ServerPlayer player) {
        return block.onBlockEntityUseItemOn(level, pos, be -> {
            final RopeStrandHolderBehavior ropeHolder = block.getHolder(be);

            final ServerRopeStrand strand = ropeHolder.getAttachedStrand();
            if (strand == null) {
                return ItemInteractionResult.FAIL;
            }

            final RopeAttachment ropeAttachment = strand.getAttachment(RopeAttachmentPoint.START);
            if (ropeAttachment == null) {
                return ItemInteractionResult.FAIL;
            }
            final BlockPos attachment = ropeAttachment.blockAttachment();

            final BlockEntity blockEntity = level.getBlockEntity(attachment);

            if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity)) return ItemInteractionResult.FAIL;

            final RopeStrandHolderBehavior otherHolder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
            if (otherHolder == null) return ItemInteractionResult.FAIL;

            otherHolder.destroyRope(player, pos.getCenter(), !player.hasInfiniteMaterials());
            return ItemInteractionResult.SUCCESS;
        });
    }

    default RopeStrandHolderBehavior getHolder(final T blockEntity) {
        return blockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
    }

    @Override
    default void afterMove(final ServerLevel originLevel, final ServerLevel serverLevel, final BlockState blockState, final BlockPos oldPos, final BlockPos newPos) {
        final AtomicReference<ServerRopeStrand> ownedStrand = new AtomicReference<>();
        this.withBlockEntityDo(originLevel, oldPos, be -> {
            final RopeStrandHolderBehavior holder = this.getHolder(be);
            ownedStrand.set(holder.getOwnedStrand());
            holder.detachRope();
        });
        this.withBlockEntityDo(serverLevel, newPos, be -> {
            final RopeStrandHolderBehavior holder = this.getHolder(be);

            if (ownedStrand.get() != null && holder.ownsRope()) {
                holder.takeOwnedStrand(ownedStrand.get());
            }

            final ServerRopeStrand strand = holder.getAttachedStrand();

            if (strand != null) {
                strand.getTrackingPlayers().clear();
                final SubLevel newSubLevel = Sable.HELPER.getContaining(serverLevel, newPos);
                final UUID newSubLevelId = newSubLevel != null ? newSubLevel.getUniqueId() : null;

                final RopeAttachmentPoint point = holder.ownsRope() ? RopeAttachmentPoint.START : RopeAttachmentPoint.END;
                final RopeAttachment attachment = new RopeAttachment(point, newSubLevelId, newPos);

                strand.addAttachment(serverLevel, point, attachment);
            }
        });
    }

}
