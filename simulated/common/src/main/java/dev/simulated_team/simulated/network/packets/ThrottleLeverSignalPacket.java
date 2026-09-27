package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.handler.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record ThrottleLeverSignalPacket(BlockPos pos, int signal) implements CustomPacketPayload {
    public static final Type<ThrottleLeverSignalPacket> TYPE = new Type<>(Simulated.path("throttle_lever_signal"));
    public static final StreamCodec<ByteBuf, ThrottleLeverSignalPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ThrottleLeverSignalPacket::pos,
            ByteBufCodecs.INT, ThrottleLeverSignalPacket::signal,
            ThrottleLeverSignalPacket::new
    );

    @Override
    public Type<ThrottleLeverSignalPacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();
        final ServerLevel level = (ServerLevel) player.level();

        if (BlockHoldInteraction.inInteractionRange(player, this.pos.getCenter(), 4) &&
                level.getBlockEntity(this.pos) instanceof final ThrottleLeverBlockEntity throttleLever) {
            throttleLever.setSignal(this.signal);
        }
    }
}
