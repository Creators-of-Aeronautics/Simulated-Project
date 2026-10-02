package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import foundry.veil.api.network.handler.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TypewriterDisconnectUser(BlockPos pos) implements CustomPacketPayload {

    public static Type<TypewriterDisconnectUser> TYPE = new Type<>(Simulated.path("typewriter_disconnect_user"));

    public static StreamCodec<ByteBuf, TypewriterDisconnectUser> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TypewriterDisconnectUser::pos, TypewriterDisconnectUser::new
    );

    public void handle(final ServerPacketContext context) {
        if (context.level().isLoaded(this.pos) &&
                context.level().getBlockEntity(this.pos) instanceof final LinkedTypewriterBlockEntity lbe) {
            if (lbe.checkUser(context.player().getUUID())) {
                lbe.disconnectUser();
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
