package dev.simulated_team.simulated.network.packets.physics_staff;

import dev.ryanhcode.sable.util.SableBufferUtils;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffAction;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

public record PhysicsStaffDragPacket(UUID subLevel, Vector3dc playerRelativeGoal,
                                     Vector3dc localAnchor, Quaterniondc orientation) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicsStaffDragPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), PhysicsStaffDragPacket::read);
    public static Type<PhysicsStaffDragPacket> TYPE = new Type<>(Simulated.path("physics_staff_drag"));

    private static PhysicsStaffDragPacket read(final RegistryFriendlyByteBuf buf) {
        return new PhysicsStaffDragPacket(buf.readUUID(), SableBufferUtils.read(buf, new Vector3d()), SableBufferUtils.read(buf, new Vector3d()), SableBufferUtils.read(buf, new Quaterniond()));
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.subLevel);
        SableBufferUtils.write(buf, this.playerRelativeGoal);
        SableBufferUtils.write(buf, this.localAnchor);
        SableBufferUtils.write(buf, this.orientation);
    }

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();

        if (!PhysicsStaffItem.isHolding(context.player())) {
            return;
        }
        final Level level = player.level();

        PhysicsStaffServerHandler.get((ServerLevel) level)
                .drag(player.getGameProfile().getId(), this.subLevel, this.playerRelativeGoal, this.localAnchor, this.orientation);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
