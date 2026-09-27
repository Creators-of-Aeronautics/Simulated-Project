package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.CenterOfLiftCalculator;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.util.SimCodecUtil;
import foundry.veil.api.network.handler.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.joml.Vector3d;

import java.util.UUID;

public record LiftMarkerDataPacket(UUID subLevel, CenterOfLiftCalculator.Status status, Vector3d position) implements CustomPacketPayload {
    public static final Type<LiftMarkerDataPacket> TYPE = new Type<>(Simulated.path("lift_marker_data"));

    private static final StreamCodec<ByteBuf, CenterOfLiftCalculator.Status> STATUS_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> CenterOfLiftCalculator.Status.values()[i], CenterOfLiftCalculator.Status::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, LiftMarkerDataPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LiftMarkerDataPacket::subLevel,
            STATUS_CODEC, LiftMarkerDataPacket::status,
            SimCodecUtil.STREAM_VECTOR3D, LiftMarkerDataPacket::position,
            LiftMarkerDataPacket::new);

    public void handle(final ClientPacketContext context) {
        handle(this);
    }

    private static void handle(final LiftMarkerDataPacket packet) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen screen = minecraft.screen;

        if (screen instanceof final DiagramScreen diagramScreen) {
            diagramScreen.updateLiftMarker(packet);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
