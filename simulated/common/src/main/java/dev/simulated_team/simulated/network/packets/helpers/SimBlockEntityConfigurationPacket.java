package dev.simulated_team.simulated.network.packets.helpers;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class SimBlockEntityConfigurationPacket<T extends SmartBlockEntity> implements CustomPacketPayload {
    private final BlockPos pos;

    public SimBlockEntityConfigurationPacket(final BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();

        final Level world = player.level();
        if (world.isLoaded(this.pos)) {
            if (player.distanceToSqr(Vec3.atBottomCenterOf(this.pos)) <= this.maxRangeSqr(player)) {
                final BlockEntity blockEntity = world.getBlockEntity(this.pos);
                if (blockEntity instanceof SyncedBlockEntity) {
                    this.applySettings(player, (T) blockEntity);
                    if (!this.causeUpdate()) {
                        return;
                    }

                    ((SyncedBlockEntity)blockEntity).sendData();
                    blockEntity.setChanged();
                }
            }
        }
    }

    protected double maxRangeSqr(Player player) {
        double range = player.blockInteractionRange() + 4.0d;
        return range*range;
    }

    protected boolean causeUpdate() {
        return true;
    }

    protected abstract void applySettings(ServerPlayer player, T blockEntity);
}
