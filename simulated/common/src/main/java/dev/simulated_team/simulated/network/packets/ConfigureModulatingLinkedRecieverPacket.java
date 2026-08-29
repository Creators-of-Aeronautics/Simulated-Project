package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.network.packets.helpers.SimBlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ConfigureModulatingLinkedRecieverPacket extends SimBlockEntityConfigurationPacket<ModulatingLinkedReceiverBlockEntity> {
    public static final Type<ConfigureModulatingLinkedRecieverPacket> TYPE = new Type<>(Simulated.path("configure_modulating_linked_reciever"));
    public static final StreamCodec<ByteBuf, ConfigureModulatingLinkedRecieverPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SimBlockEntityConfigurationPacket::getPos,
            ByteBufCodecs.INT, ConfigureModulatingLinkedRecieverPacket::getMinRange,
            ByteBufCodecs.INT, ConfigureModulatingLinkedRecieverPacket::getMaxRange,
            ConfigureModulatingLinkedRecieverPacket::new);

    private final int minRange;
    private final int maxRange;

    public ConfigureModulatingLinkedRecieverPacket(final BlockPos pos, final int minRange, final int maxRange) {
        super(pos);
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public int getMinRange() {
        return this.minRange;
    }

    public int getMaxRange() {
        return this.maxRange;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void applySettings(final ServerPlayer serverPlayer, final ModulatingLinkedReceiverBlockEntity be) {
        if (be instanceof final ModulatingLinkedReceiverBlockEntity abe) {
            abe.minRange = Mth.clamp(this.minRange, 1, ModulatingLinkedReceiverBlockEntity.RANGE_LIMIT);
            abe.maxRange = Mth.clamp(this.maxRange, abe.minRange, ModulatingLinkedReceiverBlockEntity.RANGE_LIMIT);

            abe.notifyUpdate();
        }
    }
}
