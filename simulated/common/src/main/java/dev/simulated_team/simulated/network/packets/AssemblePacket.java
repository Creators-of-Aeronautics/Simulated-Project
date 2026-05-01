package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimStats;
import foundry.veil.api.network.handler.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record AssemblePacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<AssemblePacket> TYPE = new Type<>(Simulated.path("assemble"));

    public static final StreamCodec<ByteBuf, AssemblePacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, packet -> packet.pos,
            AssemblePacket::new);

    @Override
    public Type<AssemblePacket> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext context) {
        final ServerLevel level = (ServerLevel) context.player().level();

        final BlockEntity blockEntity = level.getBlockEntity(this.pos);

        if (blockEntity instanceof final PhysicsAssemblerBlockEntity assembler) {
            assembler.assembleOrDisassemble();
            SimStats.INTERACT_WITH_ASSEMBLER.awardTo(context.player());
        }
    }
}
