package dev.simulated_team.simulated.content.physics_staff;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffDragSessionsPacket;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffLocksPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class PhysicsStaffServerHandler extends SavedData {
    public static final String ID = "simulated_physics_staff_lock_data";
    private final Map<UUID, Lock> locks = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, DragSession> draggingSessions = new Object2ObjectOpenHashMap<>();
    private ServerLevel level;
    private boolean draggingSessionsDirty = false;

    public PhysicsStaffServerHandler() {
        this(null);
    }

    public PhysicsStaffServerHandler(final LevelAccessor level) {
        this.level = (ServerLevel) level;
    }

    public static void sendAllData(final Player player) {
        final MinecraftServer server = player.getServer();
        assert server != null;

        for (final ServerLevel level : server.getAllLevels()) {
            final PhysicsStaffServerHandler handler = PhysicsStaffServerHandler.get(level);
            VeilPacketManager.player((ServerPlayer) player).sendPacket(new PhysicsStaffLocksPacket(level.dimension(), handler.locks.keySet()), makeSessionsPacket(level, handler));
        }
    }

    private static @NotNull PhysicsStaffDragSessionsPacket makeSessionsPacket(final ServerLevel level, final PhysicsStaffServerHandler handler) {
        List<Pair<UUID, Vector3d>> sessions = new ObjectArrayList<>(handler.draggingSessions.size());
        
        for (Entry<UUID, DragSession> entry : handler.draggingSessions.entrySet())
            sessions.add(Pair.of(entry.getKey(), entry.getValue().plotAnchor));
        
        return new PhysicsStaffDragSessionsPacket(level.dimension(), sessions);
    }

    private static FixedConstraintHandle addConstraint(final ServerSubLevelContainer container, final ServerSubLevel subLevel) {
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        final PhysicsPipeline pipeline = physicsSystem.getPipeline();

        final FixedConstraintHandle handle = pipeline.addConstraint(null, subLevel, new FixedConstraintConfiguration(
                subLevel.logicalPose().position(),
                subLevel.logicalPose().rotationPoint(),
                subLevel.logicalPose().orientation()
        ));
        return handle;
    }

    private static PhysicsStaffServerHandler create(final ServerLevel level, final CompoundTag nbt, final HolderLookup.Provider registries) {
        final PhysicsStaffServerHandler sd = new PhysicsStaffServerHandler(level);
        sd.loadLocks(nbt.getList(ID, Tag.TAG_INT_ARRAY));
        return sd;
    }

    public static PhysicsStaffServerHandler get(final ServerLevel level) {
        final PhysicsStaffServerHandler data = level.getChunkSource().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PhysicsStaffServerHandler::new, (nbt, lookup) -> create(level, nbt, lookup), null),
                PhysicsStaffServerHandler.ID);
        data.level = level;

        return data;
    }

    public void tick() {
        final Iterator<Map.Entry<UUID, DragSession>> iter = this.draggingSessions.entrySet().iterator();

        while (iter.hasNext()) {
            final Map.Entry<UUID, DragSession> entry = iter.next();

            final DragSession session = entry.getValue();
            final Player player = this.level.getPlayerByUUID(entry.getKey());

            if (player == null || !PhysicsStaffItem.isHolding(player)) {
                session.onRemoved();
                iter.remove();
                this.markDraggingSessionsDirty();
                continue;
            }

            session.tick();

            if (session.isMarkedForRemoval()) {
                session.onRemoved();
                iter.remove();
                this.markDraggingSessionsDirty();
            }
        }

        if (this.draggingSessionsDirty) {
            this.sendDragSessionsToClients();
            this.draggingSessionsDirty = false;
        }
    }

    private void markDraggingSessionsDirty() {
        this.draggingSessionsDirty = true;
    }

    public void physicsTick(final SubLevelPhysicsSystem physicsSystem) {
        for (final DragSession session : this.draggingSessions.values()) {
            session.physicsTick(physicsSystem);
        }
    }

    public void toggleLock(final UUID uuid) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        final ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(uuid);

        if (subLevel == null) return;
        final Lock existingLock = this.locks.get(uuid);

        if (existingLock != null) {
            this.locks.remove(uuid);
            existingLock.remove();
            this.setLocksDirty();
            return;
        }

        final FixedConstraintHandle handle = addConstraint(container, subLevel);

        this.locks.put(uuid, new Lock(uuid, handle));
        this.setLocksDirty();
    }

    private void setLocksDirty() {
        this.setDirty(true);
        this.sendLocksToClients();
    }

    private void sendLocksToClients() {
        VeilPacketManager.all(this.level.getServer()).sendPacket(
                new PhysicsStaffLocksPacket(this.level.dimension(), this.locks.keySet())
        );
    }

    private void sendDragSessionsToClients() {
        VeilPacketManager.all(this.level.getServer()).sendPacket(
                makeSessionsPacket(this.level, this)
        );
    }

    @Override
    public @NotNull CompoundTag save(final CompoundTag tag, final HolderLookup.@NotNull Provider provider) {
        final ListTag tags = new ListTag();
        this.saveLocks(tags);
        tag.put(ID, tags);

        return tag;
    }

    private void loadLocks(final ListTag list) {
        for (final Tag tag : list) {
            final UUID uuid = NbtUtils.loadUUID(tag);
            this.locks.put(uuid, new Lock(uuid, null));
        }
    }

    private void saveLocks(final ListTag list) {
        list.addAll(this.locks.keySet().stream().map(NbtUtils::createUUID).toList());
    }

    public boolean isLocked(final SubLevel subLevel) {
        final Lock lock = this.locks.get(subLevel.getUniqueId());
        return lock != null && lock.handle != null && lock.handle.isValid();
    }

    @ApiStatus.Internal
    public void applyLockIfNeeded(final SubLevel subLevel) {
        final Lock lock = this.locks.get(subLevel.getUniqueId());
        if (lock != null && (lock.handle == null || !lock.handle.isValid())) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
            assert container != null;

            final FixedConstraintHandle handle = addConstraint(container, (ServerSubLevel) subLevel);

            this.locks.put(lock.subLevel(), new Lock(lock.subLevel(), handle));
        }
    }

    public void removeLock(final SubLevel subLevel) {
        final Lock removedLock = this.locks.remove(subLevel.getUniqueId());

        if (removedLock != null) {
            removedLock.remove();
            this.setLocksDirty();
        }
    }

    public void drag(final UUID playerUUID, final UUID subLevelUUID, final Vector3dc globalAnchor, final Vector3dc localAnchor, final Quaterniondc orientation) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);

        final SubLevel subLevel = container.getSubLevel(subLevelUUID);

        if (subLevel == null) {
            return;
        }

        this.removeLock(subLevel);

        DragSession session = this.draggingSessions.get(playerUUID);

        if (session == null) {
            this.draggingSessions.put(playerUUID, session = new DragSession(playerUUID, (ServerSubLevel) subLevel));
            this.markDraggingSessionsDirty();
        }

        session.playerRelativeGoal.set(globalAnchor);
        session.plotAnchor.set(localAnchor);
        session.orientation.set(orientation);
    }


    public void stopDragging(final UUID playerUUID) {
        final DragSession session = this.draggingSessions.remove(playerUUID);

        if (session != null) {
            this.markDraggingSessionsDirty();
            session.onRemoved();
        }
    }

    private record Lock(@NotNull UUID subLevel, @Nullable PhysicsConstraintHandle handle) {
        private void remove() {
            if (this.handle != null) {
                this.handle.remove();
            }
        }
    }

    private static class DragSession {
        private final UUID playerUUID;
        private final Vector3d plotAnchor = new Vector3d();
        private final Vector3d playerRelativeGoal = new Vector3d();
        private final Vector3d localGoal = new Vector3d();
        private final Quaterniond orientation = new Quaterniond();
        private final ServerSubLevel subLevel;
        private boolean markedForRemoval = false;

        private PhysicsConstraintHandle constraint = null;

        private DragSession(final UUID playerUUID, final ServerSubLevel subLevel) {
            this.playerUUID = playerUUID;
            this.subLevel = subLevel;
        }

        private void tick() {
            if (this.subLevel.isRemoved()) {
                this.markForRemoval();
            }
        }

        private void physicsTick(final SubLevelPhysicsSystem physicsSystem) {
            if (this.subLevel.isRemoved()) {
                return;
            }

            if (this.constraint != null) {
                this.constraint.remove();
                this.constraint = null;
            }

            this.attachConstraint(physicsSystem);

            final Player player = this.subLevel.getLevel().getPlayerByUUID(this.playerUUID);
            final SimPhysics config = SimConfigService.INSTANCE.server().physics;

            if (player != null && this.constraint != null) {
                final float angularStiffness = config.physicsStaffAngularStiffness.getF();
                final float angularDamping = config.physicsStaffAngularDamping.getF();

                final float linearStiffness = config.physicsStaffLinearStiffness.getF();
                final float linearDamping = config.physicsStaffLinearDamping.getF();

                for (final ConstraintJointAxis angularAxis : ConstraintJointAxis.ANGULAR) {
                    this.constraint.setMotor(angularAxis, 0.0, angularStiffness, angularDamping, false, 0.0);
                }

                final double partialTick = physicsSystem.getPartialPhysicsTick();

                final double eyePosX = Mth.lerp(partialTick, player.xOld, player.getX());
                final double eyePosY = Mth.lerp(partialTick, player.yOld, player.getY()) + player.getEyeHeight();
                final double eyePosZ = Mth.lerp(partialTick, player.zOld, player.getZ());

                this.localGoal.set(this.playerRelativeGoal).add(eyePosX, eyePosY, eyePosZ);
                this.orientation.transformInverse(this.localGoal);

                this.constraint.setMotor(ConstraintJointAxis.LINEAR_X, this.localGoal.x(), linearStiffness, linearDamping, false, 0.0);
                this.constraint.setMotor(ConstraintJointAxis.LINEAR_Y, this.localGoal.y(), linearStiffness, linearDamping, false, 0.0);
                this.constraint.setMotor(ConstraintJointAxis.LINEAR_Z, this.localGoal.z(), linearStiffness, linearDamping, false, 0.0);
            }
        }

        private void attachConstraint(final SubLevelPhysicsSystem physicsSystem) {
            final PhysicsPipeline pipeline = physicsSystem.getPipeline();
            final FreeConstraintConfiguration config = new FreeConstraintConfiguration(JOMLConversion.ZERO, this.plotAnchor, this.orientation);

            this.constraint = pipeline.addConstraint(null, this.subLevel, config);
        }

        public boolean isMarkedForRemoval() {
            return this.markedForRemoval;
        }

        public void markForRemoval() {
            this.markedForRemoval = true;
        }

        public void onRemoved() {
            if (this.constraint != null) {
                this.constraint.remove();
            }

            this.constraint = null;
        }
    }
}
