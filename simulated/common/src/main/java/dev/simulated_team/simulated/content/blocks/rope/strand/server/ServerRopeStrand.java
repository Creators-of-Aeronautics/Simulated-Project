package dev.simulated_team.simulated.content.blocks.rope.strand.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import foundry.veil.api.util.CodecUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.*;

/**
 * A strand of rope on the server with physics, made up of a list of points
 */
public class ServerRopeStrand extends RopePhysicsObject {

    public static final Codec<ServerRopeStrand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(ServerRopeStrand::getUUID),
            CodecUtil.VECTOR3D_CODEC.listOf().fieldOf("points").forGetter(strand -> List.copyOf(strand.getPoints())),
            RopeAttachment.CODEC.listOf().fieldOf("attachments").forGetter(strand -> List.copyOf(strand.attachments.values())),
            Codec.DOUBLE.fieldOf("extension_goal").forGetter(strand -> strand.extensionGoal)
    ).apply(instance, (uuid, points, attachments, extensionGoal) -> {
        final ObjectArrayList<Vector3d> newPoints = new ObjectArrayList<>();
        for (Vector3dc point : points) {
            newPoints.add(new Vector3d(point));
        }
        final ServerRopeStrand strand = new ServerRopeStrand(uuid, newPoints);

        for (RopeAttachment attachment : attachments) {
            strand.attachments.put(attachment.point(), attachment);
        }

        strand.extensionGoal = extensionGoal;
        strand.lastExtensionGoal = extensionGoal;

        return strand;
    }));

    public static final double SEGMENT_LENGTH = 1.0;

    /**
     * The unique identifier for this strand
     */
    private final UUID uuid;
    /**
     * All attachments this rope strand has
     */
    private final Map<RopeAttachmentPoint, RopeAttachment> attachments = new Object2ObjectOpenHashMap<>();
    /**
     * The list of points we last sent clients, to know if they have moved enough to re-sync
     */
    private final List<Vector3d> lastNetworkedPoints = new ObjectArrayList<>();
    /**
     * The point count at the last substep
     */
    private int lastPointCount = 0;
    @Nullable
    private PhysicsConstraintHandle constraint = null;
    /**
     * The extension goal from the last game tick / previous call of {@link ServerRopeStrand#updateFirstSegmentExtension(double)}
     * (0 - {@link ServerRopeStrand#SEGMENT_LENGTH})
     */
    private double lastExtensionGoal = SEGMENT_LENGTH;
    /**
     * The current extension goal of the last segment (0 - {@link ServerRopeStrand#SEGMENT_LENGTH})
     */
    private double extensionGoal = SEGMENT_LENGTH;
    private double lastFirstSegmentExtension = SEGMENT_LENGTH;
    private boolean attachmentsDirty;
    public boolean networkingStopped;

    /**
     * Players that are currently tracking the rope
     */
    private final Set<UUID> trackingPlayers = new ObjectOpenHashSet<>();

    public ServerRopeStrand(final UUID uuid, final Collection<Vector3d> points) {
        super(points, 0.125);
        this.uuid = uuid;
    }

    public boolean needsSync() {
        if (this.lastNetworkedPoints.size() != this.points.size()) {
            return true;
        }

        final double threshold = Mth.square(1.0 / 16.0 * 0.1);
        for (int i = 0; i < this.points.size(); i++) {
            if (this.points.get(i).distanceSquared(this.lastNetworkedPoints.get(i)) > threshold) {
                return true;
            }
        }

        return false;
    }

    public void justSynced() {
        this.lastNetworkedPoints.clear();

        for (final Vector3d point : this.points) {
            this.lastNetworkedPoints.add(new Vector3d(point));
        }
    }

    /**
     * Sets the goal for the extension of the last segment of the rope
     *
     * @param extensionGoal the goal for the extension of the last segment of the rope (0-1)
     */
    public void updateFirstSegmentExtension(final double extensionGoal) {
        this.lastExtensionGoal = this.extensionGoal;
        this.extensionGoal = extensionGoal;
    }

    public Set<UUID> getTrackingPlayers() {
        return this.trackingPlayers;
    }

    /**
     * @return the unique identifier for this rope
     */
    public UUID getUUID() {
        return this.uuid;
    }

    public boolean isOwnerLoaded(final ServerLevel level) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        final RopeAttachment attachment = this.attachments.get(RopeAttachmentPoint.START);
        if (attachment == null) {
            return false;
        }
        final UUID subLevelID = attachment.subLevelID();

        if (subLevelID != null) {
            if (container.getSubLevel(subLevelID) == null) {
                return false;
            }
        }

        final BlockPos blockPos = attachment.blockAttachment();

        return PhysicsChunkTicketManager.isChunkLoadedEnough(level, blockPos.getX() >> SectionPos.SECTION_BITS, blockPos.getZ() >> SectionPos.SECTION_BITS);
    }

    /**
     * @return true if all attachments are loaded by this rope, and it's okay to substep
     */
    public boolean areAttachmentsLoaded(final ServerLevel level) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        final Collection<RopeAttachment> attachments = this.attachments.values();
        for (final RopeAttachment attachment : attachments) {
            final UUID subLevelID = attachment.subLevelID();

            if (subLevelID != null) {
                return container.getSubLevel(subLevelID) != null;
            }

            final BlockPos blockPos = attachment.blockAttachment();

            if (!PhysicsChunkTicketManager.isChunkLoadedEnough(level, blockPos.getX() >> SectionPos.SECTION_BITS, blockPos.getZ() >> SectionPos.SECTION_BITS)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Totals up the current extension of the rope.
     *
     * @return the current extension of the rope, which is the sum of the segment lengths
     */
    public double getCurrentExtension() {
        double totalExtension = 0.0;
        for (int i = 0; i < this.points.size() - 1; i++) {
            final Vector3d a = this.points.get(i);
            final Vector3d b = this.points.get(i + 1);
            totalExtension += a.distance(b);
        }
        return totalExtension;
    }

    public void addAttachment(final ServerLevel level, final RopeAttachmentPoint point, final RopeAttachment ropeAttachment) {
        this.attachments.put(point, ropeAttachment);
        this.removeConstraints();

        if (this.isActive()) {
            this.applyAttachment(ropeAttachment, level);
        }
    }

    public void removeConstraints() {
        if (this.constraint != null) {
            this.constraint.remove();
        }
        this.constraint = null;
    }

    public void reattachConstraints(final ServerLevel level) {
        this.removeConstraints();

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        final RopeAttachment start = this.attachments.get(RopeAttachmentPoint.START);
        final RopeAttachment end = this.attachments.get(RopeAttachmentPoint.END);

        if (start == null || end == null) {
            return;
        }

        final UUID idA = start.subLevelID();
        final UUID idB = end.subLevelID();

        final ServerSubLevel subLevelA = idA != null ? (ServerSubLevel) container.getSubLevel(idA) : null;
        final ServerSubLevel subLevelB = idB != null ? (ServerSubLevel) container.getSubLevel(idB) : null;

        if (subLevelA == subLevelB) return;

        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        final PhysicsPipeline pipeline = physicsSystem.getPipeline();

        final FreeConstraintConfiguration config = new FreeConstraintConfiguration(
                JOMLConversion.toJOML(start.blockAttachment().getCenter()),
                JOMLConversion.toJOML(end.blockAttachment().getCenter()),
                new Quaterniond()
        );

        this.constraint = pipeline.addConstraint(subLevelA, subLevelB, config);

        for (final ConstraintJointAxis angularAxis : ConstraintJointAxis.ANGULAR) {
            this.constraint.setMotor(angularAxis, 0.0, 0.0, 1.3, false, 0.0);
        }

        for (final ConstraintJointAxis linearAxis : ConstraintJointAxis.LINEAR) {
            this.constraint.setMotor(linearAxis, 0.0, 0.0, 0.25, false, 0.0);
        }
    }

    public Iterable<RopeAttachment> getAttachments() {
        return this.attachments.values();
    }

    public RopeAttachment getAttachment(final RopeAttachmentPoint point) {
        return this.attachments.get(point);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        this.removeConstraints();
    }

    @Override
    public void onUnloaded(final SubLevelHoldingChunkMap holdingChunkMap, final ChunkPos chunkPos) {
        super.onUnloaded(holdingChunkMap, chunkPos);
        this.removeConstraints();
    }

    /**
     * Called upon the physics object being added to the world
     */
    @Override
    public void onAddition(final SubLevelPhysicsSystem physicsSystem) {
        super.onAddition(physicsSystem);
        this.setFirstSegmentLength(this.extensionGoal);

        final ServerLevel level = physicsSystem.getLevel();

        for (final RopeAttachment attachment : this.attachments.values()) {
            this.applyAttachment(attachment, level);
        }
    }

    private void applyAttachment(final RopeAttachment attachment, final ServerLevel level) {
        final RopeAttachmentPoint point = attachment.point();

        final RopeHandle.AttachmentPoint sableAttachmentPoint = point == RopeAttachmentPoint.END ?
                RopeHandle.AttachmentPoint.END : RopeHandle.AttachmentPoint.START;

        final BlockPos blockAttachment = attachment.blockAttachment();
        final BlockEntity blockEntity = level.getBlockEntity(blockAttachment);

        if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity)) {
            return;
        }

        final RopeStrandHolderBehavior ropeHolder = smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
        if (ropeHolder == null) {
            return;
        }

        final Vector3d attachmentPoint = JOMLConversion.toJOML(ropeHolder.getAttachmentPoint());

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        final SubLevel subLevel = attachment.subLevelID() != null ? Objects.requireNonNull(container.getSubLevel(attachment.subLevelID())) : null;
        this.setAttachment(sableAttachmentPoint, attachmentPoint, (ServerSubLevel) subLevel);
    }

    public double getExtension() {
        return this.extensionGoal;
    }


    public void prePhysicsTick(final SubLevelPhysicsSystem physicsSystem, final ServerLevel level, final double timeStep) {
        if (this.constraint == null || !this.constraint.isValid()) {
            this.reattachConstraints(physicsSystem.getLevel());
        }

        if (this.points.size() != this.lastPointCount) {
            this.lastExtensionGoal = this.extensionGoal;
            this.lastPointCount = this.points.size();
        }

        final double extension = Mth.lerp(physicsSystem.getPartialPhysicsTick(), this.lastExtensionGoal, this.extensionGoal);

        if (!Mth.equal(extension, this.lastFirstSegmentExtension)) {
            this.setFirstSegmentLength(extension);
            this.lastFirstSegmentExtension = extension;
        }

        if (this.attachmentsDirty) {
            for (final RopeAttachment attachment : this.attachments.values()) {
                this.applyAttachment(attachment, level);
            }
            this.attachmentsDirty = false;
        }
    }

    @Override
    public void removeFirstPoint() {
        super.removeFirstPoint();
        this.attachmentsDirty = true;
    }

    @Override
    public void addPoint(final Vector3dc position) {
        super.addPoint(position);
        this.attachmentsDirty = true;
    }
}