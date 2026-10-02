package dev.simulated_team.simulated.content.blocks.rope.strand.server;

import dev.ryanhcode.sable.api.sublevel.SubLevelTrackingPlugin;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerRopeTrackingSystem implements SubLevelTrackingPlugin {
    private final ServerLevel level;

    public ServerRopeTrackingSystem(final ServerLevel level) {
        this.level = level;
    }

    private ServerLevelRopeManager getRopeManager() {
        return ServerLevelRopeManager.getOrCreate(this.level);
    }

    @Override
    public Iterable<UUID> neededPlayers() {
        final ServerLevelRopeManager ropeManager = this.getRopeManager();
        final Collection<ServerRopeStrand> strands = ropeManager.getAllStrands();

        if (strands.isEmpty()) return List.of();

        final Set<UUID> players = new ObjectOpenHashSet<>();

        for (final ServerRopeStrand strand : strands) {
            if (!strand.isActive()) continue;

            strand.updatePose();
            if (!strand.needsSync() && strand.networkingStopped) {
                continue;
            }

            final RopeStrandHolderBehavior holder = this.getStartHolder(strand);

            if (holder == null) {
                continue;
            }

            for (final ServerPlayer player : holder.getStrandTrackingPlayers()) {
                players.add(player.getUUID());
            }
        }

        return players;
    }

    @Override
    public void sendTrackingData(final int interpolationTick) {
        final ServerLevelRopeManager ropeManager = this.getRopeManager();
        final Iterable<ServerRopeStrand> strands = ropeManager.getAllStrands();

        for (final ServerRopeStrand strand : strands) {
            if (!strand.isActive()) continue;

            if (strand.needsSync()) {
                strand.networkingStopped = false;

                final RopeStrandHolderBehavior holder = this.getStartHolder(strand);

                if (holder == null) {
                    continue;
                }

                holder.getStrandPacketSink().sendPacket(holder.makeUpdatePacket(strand));
                strand.justSynced();
            } else if (!strand.networkingStopped) {
                strand.networkingStopped = true;

                final RopeStrandHolderBehavior holder = this.getStartHolder(strand);

                if (holder == null) {
                    continue;
                }

                holder.getStrandPacketSink().sendPacket(holder.makeStopPacket());
            }
        }
    }

    @Nullable
    private RopeStrandHolderBehavior getStartHolder(final ServerRopeStrand strand) {
        final RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);

        if (attachment == null) {
            return null;
        }

        final BlockPos block = attachment.blockAttachment();
        final RopeStrandHolderBehavior holder = RopeStrandHolderBehavior.get(this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE);

        if (holder == null) {
            return null;
        }

        final ServerRopeStrand ownedStrand = holder.getOwnedStrand();
        if (ownedStrand != null) {
            return ownedStrand.getUUID().equals(strand.getUUID()) ? holder : null;
        }

        final ServerRopeStrand attachedStrand = holder.getAttachedStrand();
        if (attachedStrand != null && !attachedStrand.getUUID().equals(strand.getUUID())) {
            return null;
        }

        holder.takeOwnedStrand(strand);

        return holder;
    }
}
