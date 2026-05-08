package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.util.SimCodecUtil;
import org.joml.Matrix3dc;
import foundry.veil.api.network.handler.ClientPacketContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Map;

public record DiagramDataPacket(Map<ForceGroup, List<QueuedForceGroup.PointForce>> forces, double mass, Matrix3dc inertiaTensor) implements CustomPacketPayload {
    public static final Type<DiagramDataPacket> TYPE = new Type<>(Simulated.path("diagram_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramDataPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(Object2ObjectOpenHashMap::new, SimCodecUtil.STREAM_FORCE_GROUP, SimCodecUtil.STREAM_POINT_FORCE.apply(ByteBufCodecs.list())), DiagramDataPacket::forces,
            ByteBufCodecs.DOUBLE, DiagramDataPacket::mass,
            SimCodecUtil.STREAM_MATRIX3D, DiagramDataPacket::inertiaTensor,
            DiagramDataPacket::new
    );

    public void handle(final ClientPacketContext context) {
        handle(this);
    }

    private static void handle(final DiagramDataPacket packet) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen screen = minecraft.screen;

        if (screen instanceof final DiagramScreen diagramScreen) {
            diagramScreen.updateData(packet);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
