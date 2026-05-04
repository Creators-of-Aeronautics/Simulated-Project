package dev.simulated_team.simulated.content.blocks.rope;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.config.server.blocks.SimBlockConfigs;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopePoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeStoppedPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.*;

public class RopeStrandHolderBehavior extends BlockEntityBehaviour {
    public static final BehaviourType<RopeStrandHolderBehavior> TYPE = new BehaviourType<>("rope_strand_holder");

    @Nullable
    private UUID attachedRopeID = null;
    private boolean strandOwner = false;

    @Nullable
    private ServerRopeStrand ownedServerStrand;
    private boolean queuedLevelAddition = false;

    @Nullable
    private ClientRopeStrand ownedClientStrand;

    public boolean renderAttached;

    public RopeStrandHolderBehavior(final SmartBlockEntity be) {
        super(be);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.ownedServerStrand != null) {
            if (this.queuedLevelAddition) {
                this.addServerStrand(this.ownedServerStrand);
                this.queuedLevelAddition = false;
            }

            final SubLevelPhysicsSystem system = this.getPhysicsSystem();
            final ServerLevel level = system.getLevel();

            final boolean attachmentsLoaded = this.ownedServerStrand.areAttachmentsLoaded(level);

            if (!this.ownedServerStrand.isActive() && attachmentsLoaded && system.getTicketManager().wouldBeLoaded(level, this.ownedServerStrand)) {
                system.addObject(this.ownedServerStrand);
            }

            if (this.ownedServerStrand.isActive()) {
                this.ownedServerStrand.updatePose();
            }

            this.tickStrandTrackingPlayers();

            this.destroyRopeIfAttachmentBroken();
            this.blockEntity.setChanged();
        }

        if (this.ownedClientStrand != null) {
            ClientLevelRopeManager.getOrCreate(this.getLevel())
                    .addStrand(this.ownedClientStrand);
        }
    }

    private void tickStrandTrackingPlayers() {
        assert this.ownedServerStrand != null;
        if (!this.ownedServerStrand.isActive()) return;

        final Set<UUID> alreadyTrackingPlayers = this.ownedServerStrand.getTrackingPlayers();
        
        final Iterator<UUID> iter = alreadyTrackingPlayers.iterator();

        final List<ServerPlayer> players = this.getStrandTrackingPlayers();

        while (iter.hasNext()) {
            final UUID uuid = iter.next();
            final ServerPlayer player = (ServerPlayer) this.getLevel().getPlayerByUUID(uuid);

            if (player == null || !players.contains(player)) {
                iter.remove();
            }
        }

        for (final ServerPlayer player : players) {
            final UUID uuid = player.getUUID();

            if (alreadyTrackingPlayers.add(uuid)) {
                this.ownedServerStrand.updatePose();
                VeilPacketManager.player(player).sendPacket(this.makeUpdatePacket(this.ownedServerStrand));
            }
        }
    }

    public VeilPacketManager.PacketSink getStrandPacketSink() {
        final List<ServerPlayer> players = this.getStrandTrackingPlayers();
        return packet -> {
            for (final ServerPlayer player : players) {
                player.connection.send(packet);
            }
        };
    }

    public List<ServerPlayer> getStrandTrackingPlayers() {
        final ServerLevel level = (ServerLevel) this.getLevel();
        if (level == null) return List.of();

        final ChunkPos chunk = new ChunkPos(this.getPos());

        return level.getChunkSource().chunkMap.getPlayers(chunk, false);
    }

    @NotNull
    public ClientboundRopeDataPacket makeUpdatePacket(ServerRopeStrand strand) {
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.getLevel());
        assert container != null;

        final SubLevelTrackingSystem trackingSystem = container.trackingSystem();

        //todo can be null from schematics
        final RopeAttachment startAttachment = strand.getAttachment(RopeAttachmentPoint.START);
        final RopeAttachment endAttachment = strand.getAttachment(RopeAttachmentPoint.END);

        return new ClientboundRopeDataPacket(
                trackingSystem.getInterpolationTick(),
                this.blockEntity.getBlockPos(),
                strand.getUUID(),
                new ObjectArrayList<>(strand.getPoints()),
                startAttachment != null ? startAttachment.blockAttachment() : null,
                endAttachment != null ? endAttachment.blockAttachment() : null
        );
    }

    public ClientboundRopeStoppedPacket makeStopPacket() {
        return new ClientboundRopeStoppedPacket(
                this.blockEntity.getBlockPos()
        );
    }

    private void destroyRopeIfAttachmentBroken() {
        final ServerRopeStrand strand = this.ownedServerStrand;
        assert strand != null;

        final ServerLevel level = (ServerLevel) this.getLevel();

        if (!strand.areAttachmentsLoaded(level)) {
            return;
        }

        final RopeAttachment endAttachment = strand.getAttachment(RopeAttachmentPoint.END);

        if (endAttachment == null) {
            this.destroyRope(null, this.getAttachmentPoint());
            return;
        }

        final BlockPos blockAttachment = endAttachment.blockAttachment();
        final BlockEntity blockEntity = level.getBlockEntity(blockAttachment);

        if (blockEntity == null) {
            this.destroyRope(null, blockAttachment.getCenter());
            return;
        }

        if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity)) {
            this.destroyRope(null, blockAttachment.getCenter());
            return;
        }

        final RopeStrandHolderBehavior holderBehavior = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

        if (holderBehavior == null) {
            this.destroyRope(null, blockAttachment.getCenter());
        }
    }


    /**
     * Attempts to attach a rope strand to the target holder.
     *
     * @param target the target holder to attach the rope to
     * @return true if the rope was successfully created, false if it already exists or the target is invalid
     */
    public boolean createRope(final RopeStrandHolderBehavior target) {
        if (target == this)
            return false;

        if (target.attachedRopeID != null) {
            return false;
        }

        final ServerLevel level = (ServerLevel) this.getLevel();

        final Vec3 localRopeStart = this.getAttachmentPoint();
        final Vec3 localRopeTarget = target.getAttachmentPoint();

        final SubLevel subLevelStart = Sable.HELPER.getContaining(level, localRopeStart);
        final SubLevel subLevelTarget = Sable.HELPER.getContaining(level, localRopeTarget);

        final Vec3 ropeStart = Sable.HELPER.projectOutOfSubLevel(level, localRopeStart);
        final Vec3 ropeTarget = Sable.HELPER.projectOutOfSubLevel(level, localRopeTarget);

        final SimBlockConfigs blockConfig = SimConfigService.INSTANCE.server().blocks;
        final double maxRopeRange = blockConfig.maxRopeRange.get();

        if (!ropeTarget.closerThan(ropeStart, maxRopeRange)) {
            return false;
        }

        this.destroyRope(null, null);

        final double distance = ropeTarget.distanceTo(ropeStart);
        final int oneLongSegments = Mth.floor(distance);
        final int points = Math.max(1, oneLongSegments + 1);

        final Vec3 diff = ropeTarget.subtract(ropeStart).normalize();

        final List<Vector3d> pointList = new ObjectArrayList<>();
        pointList.add(JOMLConversion.toJOML(ropeStart));

        final double shortSegmentLength = distance - oneLongSegments;

        for (int i = 0; i < points; i++) {
            pointList.add(JOMLConversion.toJOML(ropeStart.add(diff.scale(i + shortSegmentLength))));
        }

        final ServerRopeStrand strand = new ServerRopeStrand(UUID.randomUUID(), pointList);

        strand.updateFirstSegmentExtension(shortSegmentLength);
        strand.addAttachment(level, RopeAttachmentPoint.START, new RopeAttachment(RopeAttachmentPoint.START, Optional.ofNullable(subLevelStart).map(SubLevel::getUniqueId).orElse(null), this.blockEntity.getBlockPos()));
        strand.addAttachment(level, RopeAttachmentPoint.END, new RopeAttachment(RopeAttachmentPoint.END, Optional.ofNullable(subLevelTarget).map(SubLevel::getUniqueId).orElse(null), target.blockEntity.getBlockPos()));

        this.strandOwner = true;
        this.attachedRopeID = strand.getUUID();
        target.strandOwner = false;
        target.attachedRopeID = strand.getUUID();

        this.addServerStrand(strand);

        this.blockEntity.notifyUpdate();
        target.blockEntity.notifyUpdate();

        return true;
    }

    private @Nullable Level getLevel() {
        return this.blockEntity.getLevel();
    }

    public SubLevelPhysicsSystem getPhysicsSystem() {
        return SubLevelContainer.getContainer((ServerLevel) this.getLevel()).physicsSystem();
    }

    /**
     * Block destroyed or Chunk unloaded. Usually invalidates capabilities
     */
    @Override
    public void unload() {
        final Level level = this.getLevel();

        if (this.ownedServerStrand != null) {
            this.removeServerStrand(level);
        }

        if (level != null && level.isClientSide) {
            this.removeClientStrand();
        }
    }

    private void removeServerStrand(final Level level) {
        if (this.ownedServerStrand.isActive()) {
            this.ownedServerStrand.updatePose();
        }
        this.getPhysicsSystem().removeObject(this.ownedServerStrand);
        ServerLevelRopeManager.getOrCreate(level)
                .removeStrand(this.ownedServerStrand.getUUID());
        this.ownedServerStrand = null;
    }

    private void addServerStrand(final ServerRopeStrand strand) {
        this.ownedServerStrand = strand;
        ServerLevelRopeManager.getOrCreate(this.getLevel())
                .addStrand(this.ownedServerStrand);
    }

    /**
     * Destroys the current rope.
     */
    public void destroyRope(@Nullable final ServerPlayer player, @Nullable final Vec3 ropeDropPos) {
        if (!this.strandOwner || this.ownedServerStrand == null) {
            return;
        }

        final Level level = this.getLevel();
        if (level == null) {
            return;
        }

        final ServerRopeStrand strand = this.getOwnedStrand();

        if (strand != null) {
            final RopeAttachment target = strand.getAttachment(RopeAttachmentPoint.END);
            if (target != null) {
                final BlockPos targetBlockPos = target.blockAttachment();

                if (level.getBlockEntity(targetBlockPos) instanceof final SmartBlockEntity be) {
                    final RopeStrandHolderBehavior behaviour = be.getBehaviour(TYPE);

                    if (behaviour != null) {
                        behaviour.detachRope();
                        behaviour.blockEntity.notifyUpdate();
                    }
                }
            }

            final List<Vector3d> points = strand.getPoints();
            final Vector3d middlePointPos = new Vector3d(points.get(points.size() / 2));

            final ItemStack stack = new ItemStack(SimItems.ROPE_COUPLING.get());

            if (player != null) {
                player.getInventory().placeItemBackInInventory(stack);
            } else {
                if (ropeDropPos != null)
                    middlePointPos.set(ropeDropPos.x, ropeDropPos.y, ropeDropPos.z);

                level.addFreshEntity(new ItemEntity(level, middlePointPos.x, middlePointPos.y, middlePointPos.z, stack));
            }

            for (final Vector3d position : points) {
                level.playSound(null, position.x, position.y, position.z, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.5F, 1F);

                if (level instanceof final ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, AllBlocks.ROPE.getDefaultState()),
                            position.x, position.y, position.z,
                            10, 0.1, 0.1, 0.1, 0.1
                    );
                }
            }
        }

        if (this.ownedServerStrand != null) {
            this.removeServerStrand(level);
        }

        this.attachedRopeID = null;
        this.strandOwner = false;
        this.blockEntity.notifyUpdate();
    }

    public Vec3 getAttachmentPoint() {
        final SmartBlockEntity be = this.blockEntity;
        return ((RopeStrandHolderBlockEntity) be).getAttachmentPoint(be.getBlockPos(), be.getBlockState());
    }

    public Vec3 getVisualAttachmentPoint() {
        final SmartBlockEntity be = this.blockEntity;
        return ((RopeStrandHolderBlockEntity) be).getVisualAttachmentPoint(be.getBlockPos(), be.getBlockState());
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Nullable
    public ServerRopeStrand getOwnedStrand() {
        return this.ownedServerStrand;
    }

    @Nullable
    public ServerRopeStrand getAttachedStrand() {
        final ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(this.getLevel());
        return (manager != null && this.attachedRopeID != null) ? manager.getStrand(this.attachedRopeID) : null;
    }

    @Override
    public void write(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putBoolean("OwnStrand", this.strandOwner);

        if (this.attachedRopeID != null) {
            nbt.putUUID("HasRopeAttached", this.attachedRopeID);
        }

        final ServerRopeStrand strand = this.getOwnedStrand();
        if (strand != null && this.strandOwner && !clientPacket) {
            nbt.put("Strand", ServerRopeStrand.CODEC.encodeStart(NbtOps.INSTANCE, strand).getOrThrow());
        }
    }

    @Override
    public void read(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        this.strandOwner = nbt.getBoolean("OwnStrand");

        if (nbt.contains("HasRopeAttached")) {
            this.attachedRopeID = nbt.getUUID("HasRopeAttached");
        } else {
            this.attachedRopeID = null;
        }

        if (clientPacket) {
            if (!this.strandOwner)
                this.removeClientStrand();
        } else {
            if (nbt.contains("Strand")) {
                final CompoundTag strandNBT = nbt.getCompound("Strand");

                if (this.ownedServerStrand == null) {
                    this.loadServerStrand(strandNBT);
                }
            }
        }
    }

    private void removeClientStrand() {
        if (this.ownedClientStrand != null) {
            ClientLevelRopeManager.getOrCreate(this.getLevel()).removeStrand(this.ownedClientStrand.getUuid());
            this.ownedClientStrand = null;
        }
    }

    public void receiveClientStrand(final int interpolationTick, final List<Vector3d> incomingPoints, final UUID uuid, @Nullable final BlockPos startAttachmentPos, @Nullable final BlockPos endAttachmentPos) {
        if (this.ownedClientStrand == null) {
            this.ownedClientStrand = new ClientRopeStrand(uuid);
        }

        ClientLevelRopeManager.getOrCreate(this.getLevel())
                .addStrand(this.ownedClientStrand);

        final Vec3 startAttachmentPoint = this.getAttachmentPoint(startAttachmentPos);
        final Vec3 endAttachmentPoint = this.getAttachmentPoint(endAttachmentPos);

        if (startAttachmentPoint != null)
            this.ownedClientStrand.startAttachment = startAttachmentPoint;

        if (endAttachmentPoint != null)
            this.ownedClientStrand.endAttachment = endAttachmentPoint;

        final ObjectArrayList<ClientRopePoint> points = this.ownedClientStrand.getPoints();
        this.ownedClientStrand.setStopped(false);

        while (points.size() < incomingPoints.size()) {
            final Vector3dc position = incomingPoints.get(incomingPoints.size() - points.size() - 1);
            points.addFirst(new ClientRopePoint(new Vector3d(position), new Vector3d(position), new ObjectArrayList<>()));
        }

        while (points.size() > incomingPoints.size()) {
            points.removeFirst();
        }

        for (int i = 0; i < incomingPoints.size(); i++) {
            final Vector3d incomingPoint = incomingPoints.get(i);
            points.get(i)
                    .snapshots()
                    .add(new ClientRopePoint.Snapshot(interpolationTick, incomingPoint));
        }
    }

    private Vec3 getAttachmentPoint(@Nullable final BlockPos attachment) {
        if (attachment == null) {
            return null;
        }

        final BlockEntity blockEntity = this.getLevel().getBlockEntity(attachment);

        if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity))
            return null;

        final RopeStrandHolderBehavior holder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

        if (holder == null)
            return null;

        return holder.getAttachmentPoint();
    }

    private Vec3 getVisualAttachmentPoint(@Nullable final BlockPos attachment) {
        if (attachment == null) {
            return null;
        }

        final BlockEntity blockEntity = this.getLevel().getBlockEntity(attachment);

        if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity))
            return null;

        final RopeStrandHolderBehavior holder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);

        if (holder == null)
            return null;

        return holder.getVisualAttachmentPoint();
    }

    /**
     * Loads a strand into the level.
     */
    private void loadServerStrand(final CompoundTag tag) {
        final DataResult<Pair<ServerRopeStrand, Tag>> result = ServerRopeStrand.CODEC.decode(NbtOps.INSTANCE, tag);
        final ServerRopeStrand strand = result.getOrThrow().getFirst();

        this.ownedServerStrand = strand;
        this.queuedLevelAddition = true;
    }

    /**
     * Block destroyed or removed. Requires block to call ITE::onRemove
     */
    @Override
    public void destroy() {
        final ServerRopeStrand attachedStrand = this.getAttachedStrand();

        final Level level = this.getLevel();
        if (level != null && level.isClientSide) {
            this.removeClientStrand();
        }

        if (!this.strandOwner && attachedStrand != null) {
            final RopeAttachment startAttachment = attachedStrand.getAttachment(RopeAttachmentPoint.START);
            final BlockPos blockAttachment = startAttachment.blockAttachment();
            final BlockEntity blockEntity = Objects.requireNonNull(level).getBlockEntity(blockAttachment);

            if (blockEntity instanceof final SmartBlockEntity smartBlockEntity) {
                final RopeStrandHolderBehavior holder = smartBlockEntity.getBehaviour(TYPE);

                if (holder != null) {
                    holder.destroyRope(null, this.getAttachmentPoint());
                    return;
                }
            }
        }

        this.destroyRope(null, this.getAttachmentPoint());
    }

    public boolean isAttached() {
        return this.attachedRopeID != null;
    }

    public boolean ownsRope() {
        return this.strandOwner;
    }

    /**
     * Detaches the holder from the rope. Be wary of this, as references must not be entirely lost to the manager.
     */
    @ApiStatus.Internal
    public void detachRope() {
        this.strandOwner = false;
        this.attachedRopeID = null;
        this.ownedServerStrand = null;
    }

    public ClientRopeStrand getClientStrand() {
        return this.ownedClientStrand;
    }

    public void giveFakeClientStrand(final ClientRopeStrand strand) {
        this.strandOwner = true;
        this.ownedClientStrand = strand;
        this.attachedRopeID = strand.getUuid();
    }

    public void giveFakeClientStrand(final UUID ropeUUID) {
        this.attachedRopeID = ropeUUID;
    }

    public void takeOwnedStrand(final ServerRopeStrand ownedStrand) {
        this.ownedServerStrand = ownedStrand;
    }

    public void receiveClientStrandStopped() {
        if (this.ownedClientStrand != null) {
            this.ownedClientStrand.setStopped(true);
        }
    }
}
