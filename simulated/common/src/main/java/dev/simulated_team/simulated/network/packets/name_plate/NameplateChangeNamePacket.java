package dev.simulated_team.simulated.network.packets.name_plate;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record NameplateChangeNamePacket(BlockPos controllerPos, @Nullable String name) implements CustomPacketPayload {

    public static Type<NameplateChangeNamePacket> TYPE = new Type<>(Simulated.path("nameplate_change_name"));

    public static StreamCodec<RegistryFriendlyByteBuf, NameplateChangeNamePacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NameplateChangeNamePacket::controllerPos,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), (packet) -> Optional.ofNullable(packet.name()),
            NameplateChangeNamePacket::fromCodec);

    public static NameplateChangeNamePacket fromCodec(final BlockPos controllerPos, final Optional<String> name) {
        return new NameplateChangeNamePacket(controllerPos, name.orElse(null));
    }

    public void handle(final ServerPacketContext context) {
        final Level level = context.level();
        double range = context.player().blockInteractionRange() + 4;
        if (context.player().distanceToSqr(controllerPos.getCenter()) > range*range) return;
        if (level.getBlockEntity(this.controllerPos()) instanceof final NameplateBlockEntity nbe && nbe.allowsEditing()) {
            nbe.setName(this.name, true, context.player());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
