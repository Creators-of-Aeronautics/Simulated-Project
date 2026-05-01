package dev.simulated_team.simulated.network.packets;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.items.spring.SpringItem;
import dev.simulated_team.simulated.content.items.spring.SpringItemHandler;
import dev.simulated_team.simulated.index.SimBlocks;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record PlaceSpringPacket(BlockPos parentPos, BlockPos childPos, Direction parentFacing, Direction childFacing,
                                InteractionHand hand) implements CustomPacketPayload {

    public static Type<PlaceSpringPacket> TYPE = new Type<>(Simulated.path("place_spring"));

    public static StreamCodec<RegistryFriendlyByteBuf, PlaceSpringPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, (packet) -> packet.hand().ordinal(),
            BlockPos.STREAM_CODEC, PlaceSpringPacket::parentPos,
            BlockPos.STREAM_CODEC, PlaceSpringPacket::childPos,
            Direction.STREAM_CODEC, PlaceSpringPacket::parentFacing,
            Direction.STREAM_CODEC, PlaceSpringPacket::childFacing,
            (hand, parentPos, childPos, parentFacing, childFacing) -> new PlaceSpringPacket(parentPos, childPos, parentFacing, childFacing, hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext ctx) {
        final ServerPlayer player = ctx.player();
        final Level level = ctx.level();

        final BlockPos parentRelative = this.parentPos().relative(this.parentFacing);
        final BlockPos childRelative = this.childPos().relative(this.childFacing);

        final ItemStack spring = player.getItemInHand(this.hand);
        final double distanceSquared = Sable.HELPER.distanceSquaredWithSubLevels(level, parentRelative.getCenter(), childRelative.getCenter());
        if (!(spring.getItem() instanceof SpringItem) || distanceSquared > (SpringItemHandler.MAX_LENGTH + 1) * (SpringItemHandler.MAX_LENGTH + 1)) {
            return;
        }

        final SpringBlockEntity controllerSpring = this.addSpring(level, parentRelative, childRelative, this.parentFacing(), true, (float) distanceSquared);
        final SpringBlockEntity partnerSpring = this.addSpring(level, childRelative, parentRelative, this.childFacing(), false, (float) distanceSquared);

        if (controllerSpring == null || partnerSpring == null) {
            level.setBlockAndUpdate(parentRelative, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(childRelative, Blocks.AIR.defaultBlockState());
            return;
        }

        final double distance = Math.clamp(Math.sqrt(distanceSquared) + 1, 1, SpringItemHandler.MAX_LENGTH);
        controllerSpring.setDesiredLength(distance);
        partnerSpring.setDesiredLength(distance);

        player.awardStat(Stats.ITEM_USED.get(spring.getItem()));
        if (!player.hasInfiniteMaterials()) {
            spring.shrink(1);
        }
    }

    private SpringBlockEntity addSpring(final Level level, final BlockPos placedPos, final BlockPos childPos, final Direction facing, final boolean controller, final float distance) {
        final BlockState newState = SimBlocks.SPRING.getDefaultState();

        if (level.setBlockAndUpdate(placedPos, newState.setValue(SpringBlock.FACING, facing))) {
            final SpringBlockEntity parentSpring = (SpringBlockEntity) level.getBlockEntity(placedPos);

            parentSpring.setController(controller);

            final SubLevel subLevel = Sable.HELPER.getContaining(level, childPos);
            parentSpring.setPartnerPos(childPos, subLevel != null ? subLevel.getUniqueId() : null);

            parentSpring.notifyUpdate();
            return parentSpring;
        }
        return null;
    }
}
