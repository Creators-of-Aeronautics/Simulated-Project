package dev.simulated_team.simulated.network.packets;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public record RopeBreakPacket(UUID uuid) implements CustomPacketPayload {
    public static Type<RopeBreakPacket> TYPE = new Type<>(Simulated.path("break_rope"));

    public static StreamCodec<RegistryFriendlyByteBuf, RopeBreakPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RopeBreakPacket::uuid,
            RopeBreakPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext ctx) {
        final ServerPlayer player = ctx.player();

        final Level level = player.level();

        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        final ServerRopeStrand strand = manager.getStrand(this.uuid);

        if (strand != null) {
            final RopeAttachment startAttachment = strand.getAttachment(RopeAttachmentPoint.START);
            if (startAttachment == null) {
                return;
            }

            final BlockPos blockAttachment = startAttachment.blockAttachment();

            final BlockEntity blockEntity = level.getBlockEntity(blockAttachment);

            if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity)) {
                return;
            }

            final RopeStrandHolderBehavior holder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

            if (holder == null) {
                return;
            }

            holder.destroyRope(player, null, !player.hasInfiniteMaterials());
        }
    }
}
