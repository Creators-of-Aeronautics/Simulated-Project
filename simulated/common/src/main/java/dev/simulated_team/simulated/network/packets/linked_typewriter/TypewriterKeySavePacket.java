package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import foundry.veil.api.network.handler.ServerPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public record TypewriterKeySavePacket(Map<Integer, LinkedTypewriterEntries.KeyboardEntry> changedKeys, BlockPos pos,
                                      boolean clearAll) implements CustomPacketPayload {

    public static final Type<TypewriterKeySavePacket> TYPE = new Type<>(Simulated.path("linked_typewriter_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TypewriterKeySavePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC), TypewriterKeySavePacket::changedKeys,
            BlockPos.STREAM_CODEC, TypewriterKeySavePacket::pos,
            ByteBufCodecs.BOOL, TypewriterKeySavePacket::clearAll,
            TypewriterKeySavePacket::new);

    public TypewriterKeySavePacket(final LinkedTypewriterEntries keys, final BlockPos pos, final boolean clearAll) {
        this(keys.getKeyMap(), pos, clearAll);
    }


    public void handle(final ServerPacketContext context) {
        double range = context.player().blockInteractionRange() + 4;
        if (context.player().distanceToSqr(pos.getCenter()) > range*range) return;
        final Level level = context.level();

        final BlockEntity be = level.getBlockEntity(this.pos);
        if (be instanceof final LinkedTypewriterBlockEntity lbe) {
            // make sure all entries have a valid pos
            for (final LinkedTypewriterEntries.KeyboardEntry entry : this.changedKeys.values()) {
                entry.setLocation(this.pos);
            }

            lbe.getTypewriterEntries().clearAll();
            if (!this.clearAll) {
                lbe.getTypewriterEntries().addAll(this.changedKeys);
            }

            if (lbe.getTypewriterEntries().getSize() >= 26) {
                SimAdvancements.I_PAID_FOR_THE_WHOLE_TYPEWRITER.awardTo(context.player());
            }

            lbe.setChanged();
            lbe.sendData();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
