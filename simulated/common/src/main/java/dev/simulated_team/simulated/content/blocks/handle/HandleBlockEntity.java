package dev.simulated_team.simulated.content.blocks.handle;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.service.SimConfigService;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HandleBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {
    static final double MAX_HANDLE_RANGE = 5.0;
    // call this.setChanged(); after modifying
    private final Map<UUID, HandleConstraint> players = new Object2ObjectOpenHashMap<>();

    public HandleBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();

        this.checkPlayers();
    }

    public boolean hasPlayer() {
        return !this.players.isEmpty();
    }

    private void checkPlayers() {
        assert this.level != null;

        for (final Iterator<Map.Entry<UUID, HandleConstraint>> it = this.players.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<UUID, HandleConstraint> entry = it.next();
            final Player player = this.level.getPlayerByUUID(entry.getKey());
            final HandleConstraint constraint = entry.getValue();

            if (player == null || player.isDeadOrDying()) {
                if (constraint != null) {
                    constraint.removeJoint();
                }

                it.remove();
                this.setChanged();
            } else {
                if (constraint == null || !constraint.hasJoint()) {
                    player.resetFallDistance();
                }
            }
        }
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        this.checkPlayers();
        for (final HandleConstraint constraint : this.players.values()) {
            constraint.physicsTick(subLevel, handle);
        }
    }

    public void startGrabbingServer(final UUID player, final float desiredRange) {
        if (this.players.containsKey(player)) {
            // Player is already grabbing, update the range
            this.players.get(player).setScrollDistance(desiredRange);
            return;
        }
        final HandleConstraint handle = new HandleConstraint(player, desiredRange, null);

        this.players.put(player, handle);
        this.setChanged();
    }

    public void stopGrabbingServer(final UUID player) {
        final HandleConstraint constraint = this.players.remove(player);
        this.setChanged();

        if (constraint != null) {
            constraint.removeJoint();
        }
    }

    @Override
    public void remove() {
        super.remove();

        this.players.values().forEach(HandleConstraint::removeJoint);
        this.players.clear();
        this.setChanged();
    }

    @Contract(value = "->new", pure = true)
    public Vector3d getGrabCenter() {
        final Direction facing = this.getBlockState().getValue(HandleBlock.FACING);
        return JOMLConversion.atCenterOf(HandleBlockEntity.this.getBlockPos())
                .fma(-(0.5 - 5.0 / 16.0), JOMLConversion.atLowerCornerOf(facing.getNormal()));
    }

    private class HandleConstraint {
        private static final double CONSTRAINT_DAMPING = 30.0;
        private static final double CONSTRAINT_STIFFNESS = 240.0;
        private final UUID playerId;
        private float scrollDistance;
        private @Nullable PhysicsConstraintHandle constraintHandle;

        public HandleConstraint(final UUID playerId, final float scrollDistance, final PhysicsConstraintHandle constraintHandle) {
            this.playerId = playerId;
            this.scrollDistance = scrollDistance;
            this.constraintHandle = constraintHandle;
        }

        public void physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle) {
            this.removeJoint();

            final Player player = HandleBlockEntity.this.level.getPlayerByUUID(this.playerId);

            if (player == null) {
                return;
            }

            if (!player.onGround() && !player.isInWater() && !player.getAbilities().flying &&!player.onClimbable()) {
                return;
            } else {
                final SubLevel standingSubLevel = Sable.HELPER.getTrackingSubLevel(player);
                if (standingSubLevel == subLevel) {
                    return;
                }
            }

            final Vector3d constraintGoal = JOMLConversion.toJOML(player.getEyePosition().add(player.getLookAngle().scale(Math.max(2.0, this.scrollDistance))));
            final Vector3d constraintPosition = HandleBlockEntity.this.getGrabCenter();

            final double validRange = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue() + 2.0;
            final double currentDistance = Sable.HELPER.distanceSquaredWithSubLevels(HandleBlockEntity.this.level, constraintGoal, constraintPosition);

            if (Mth.equal(-1, this.scrollDistance) || currentDistance > validRange * validRange) {
                return;
            }

            final ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
            assert container != null;

            final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

            this.constraintHandle = physicsSystem.getPipeline().addConstraint(
                    null, subLevel,
                    new FreeConstraintConfiguration(constraintGoal, constraintPosition, new Quaterniond())
            );

            final double maxForce = SimConfigService.INSTANCE.server().physics.handleMaxForce.getF();

            // linear axes
            for (final ConstraintJointAxis axis : ConstraintJointAxis.LINEAR) {
                this.constraintHandle.setMotor(axis, 0.0, CONSTRAINT_STIFFNESS, CONSTRAINT_DAMPING, true, maxForce);
            }

            // angular axes
            for (final ConstraintJointAxis axis : ConstraintJointAxis.ANGULAR) {
                this.constraintHandle.setMotor(axis, 0.0, 0.0, 4.5, true, maxForce);
            }
        }

        public boolean hasJoint() {
            return this.constraintHandle != null;
        }

        public void removeJoint() {
            if (this.constraintHandle != null) {
                this.constraintHandle.remove();
                this.constraintHandle = null;
            }
        }

        public void setScrollDistance(final float desiredRange) {
            this.scrollDistance = (float) Math.min(desiredRange, 2.5);
        }
    }
}