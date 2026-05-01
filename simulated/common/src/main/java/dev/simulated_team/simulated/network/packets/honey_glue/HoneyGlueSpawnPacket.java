package dev.simulated_team.simulated.network.packets.honey_glue;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueMaxSizing;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimSoundEvents;
import foundry.veil.api.network.handler.PacketContext;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public record HoneyGlueSpawnPacket(BlockPos from, BlockPos to) implements CustomPacketPayload {
    public static Type<HoneyGlueSpawnPacket> TYPE = new Type<>(Simulated.path("honey_glue_spawn"));
    public static StreamCodec<RegistryFriendlyByteBuf, HoneyGlueSpawnPacket> CODEC = StreamCodec.of(HoneyGlueSpawnPacket::writeToBuf, HoneyGlueSpawnPacket::readFromBuf);

    public static void writeToBuf(final RegistryFriendlyByteBuf buf, final HoneyGlueSpawnPacket packet) {
        buf.writeBlockPos(packet.from);
        buf.writeBlockPos(packet.to);
    }

    public static HoneyGlueSpawnPacket readFromBuf(final RegistryFriendlyByteBuf buf) {
        return new HoneyGlueSpawnPacket(buf.readBlockPos(), buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private InteractionHand getHoneyGlueHand(final Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(SimItems.HONEY_GLUE) ? InteractionHand.MAIN_HAND :
                player.getItemInHand(InteractionHand.OFF_HAND).is(SimItems.HONEY_GLUE) ? InteractionHand.OFF_HAND :
                        null;
    }

    public void handle(final PacketContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();
        assert player != null;

        final InteractionHand hand = this.getHoneyGlueHand(player);

        if (hand == null)
            return;

        final AABB newBounds = AABB.encapsulatingFullBlocks(this.from, this.to);
        final Pair<Boolean, String> pair = HoneyGlueMaxSizing.checkBounds(newBounds);

        if (pair.getFirst()) {
            final ServerLevel level = (ServerLevel) context.level();
            assert level != null;

            level.playSound(player, this.to, SimSoundEvents.HONEY_ADDED.event(), SoundSource.BLOCKS, 0.5f, 0.95f);
            level.playSound(player, this.to, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1.0f);

            final ItemStack honeyGlueItem = player.getItemInHand(hand);
            honeyGlueItem.hurtAndBreak(1, level, player, (item) -> {});

            final HoneyGlueEntity entity = SimEntityTypes.HONEY_GLUE.create(level);
            assert entity != null;

            entity.setBounds(newBounds);
            level.addFreshEntity(entity);
            entity.spawnParticles();

            player.awardStat(Stats.ITEM_USED.get(honeyGlueItem.getItem()));
            SimAdvancements.NOT_GONNA_SUGARCOAT_IT.awardTo(player);
        }
    }
}
