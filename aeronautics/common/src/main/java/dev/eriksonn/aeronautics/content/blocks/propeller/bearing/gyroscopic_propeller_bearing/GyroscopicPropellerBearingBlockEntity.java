package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class GyroscopicPropellerBearingBlockEntity extends PropellerBearingBlockEntity {
    private static final Vector3d PHYSICS_THRUST = new Vector3d();
    protected Quaternionf previousTiltQuat; //for client smoothing
    protected Quaternionf tiltQuat;
    protected final Vector3d blockNormal = new Vector3d();
    protected final Vector3d tiltVector = new Vector3d(0, 1, 0);
    boolean powered = false;
    boolean initialized = false;
    double currentGravity;
    double physicsTimer = 0;

    public GyroscopicPropellerBearingBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.tiltQuat = new Quaternionf();
        this.previousTiltQuat = new Quaternionf();
        this.tiltQuat.normalize();
    }

    @Override
    public PropellerActorBehaviour createProp() {
        return new GyroActorBehaviour<>(this);
    }

    @Override
    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        compound.putBoolean("IsPowered", this.powered);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.powered = compound.getBoolean("IsPowered");

        super.read(compound, registries, clientPacket);
    }

    public void tick() {
        super.tick();
        this.physicsTimer = 0;
        final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
        this.blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        if (!this.initialized) {
            this.tiltVector.set(this.blockNormal);
            this.initialized = true;
            this.applyTilt();
        }

        this.previousTiltQuat.set(this.tiltQuat);
        //todo: redstone signal stuff in blockstate instead of here
        this.updateSignal();


        if (this.isVirtual()) {
            return;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        this.updateTilt(this.tiltVector, subLevel, 1);
        this.applyTilt();
        PropellerBearingContraptionEntity propellerContraption = this.getMovedContraption();
        if (propellerContraption != null) {
            propellerContraption.tiltQuat = new Quaternionf(this.tiltQuat);
            propellerContraption.previousTiltQuat = new Quaternionf(this.previousTiltQuat);
            propellerContraption.direction = this.getBlockState().getValue(PropellerBearingBlock.FACING);
        }
    }

    public void updateSignal() {
        this.powered = this.level.hasNeighborSignal(this.worldPosition);
    }

    /**
     * Forces the tilt to fully update whenever the bearing is wrenched / rotate
     */
    public void forceTilt(final BlockState state) {
        final Direction facing = state.getValue(GyroscopicPropellerBearingBlock.FACING);
        this.blockNormal.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        this.tiltVector.set(this.blockNormal);

        this.applyTilt();
        this.previousTiltQuat.set(this.tiltQuat);
    }

    private void updateTilt(final Vector3d tilt, final SubLevel subLevel, final double stepScale) {
        final Level level = this.getLevel();
        final Vector3dc gravityVector = DimensionPhysicsData.getGravity(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.atCenterOf(this.getBlockPos())));
        this.currentGravity = gravityVector.length();
        final Vector3d target = this.currentGravity > 0 && subLevel != null ? subLevel.logicalPose().orientation().transformInverse(new Vector3d(gravityVector).mul(-1 / this.currentGravity)) : this.blockNormal;

        this.setTilt(tilt, target, 0.05 * stepScale);
    }

    public void setTilt(final Vector3d tilt, Vector3d target, final double maxStep) {
        if (this.powered) {
            target = this.blockNormal;
        }

        final Direction direction = this.getBlockState().getValue(PropellerBearingBlock.FACING);
        this.blockNormal.set(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        SimMathUtils.clampIntoCone(target, this.blockNormal, Math.toRadians(12));

        target.lerp(this.blockNormal, 1 - this.getLerpDistance());

        final Vector3d difference = new Vector3d(target).sub(tilt);
        if (difference.lengthSquared() > maxStep * maxStep) {
            tilt.add(difference.normalize().mul(maxStep));
        } else {
            tilt.set(target);
        }
    }

    /**
     * Directly sets the tilt by the provided lerp amount. Used by ponders.
     */
    public void setStrictTilt(Vector3d target, final double lerpAmount, final double maxStep) {
        if (this.powered) {
            target = this.blockNormal;
        }

        final Direction direction = this.getBlockState().getValue(PropellerBearingBlock.FACING);
        this.blockNormal.set(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        SimMathUtils.clampIntoCone(target, this.blockNormal, Math.toRadians(12));

        target.lerp(this.blockNormal, 1 - lerpAmount);

        final Vector3d difference = new Vector3d(target).sub(this.tiltVector);
        if (difference.lengthSquared() > maxStep * maxStep) {
            this.tiltVector.add(difference.normalize(maxStep));
        } else {
            this.tiltVector.set(target);
        }

        this.applyTilt();
    }

    private double getLerpDistance() {
        double lerpDistance = 1f;
        if (this.getMovedContraption() == null) {
            lerpDistance = 0;
        }

        final double currentSpeed = Math.abs(this.getSpeed());
        if (currentSpeed < 1) {
            lerpDistance *= currentSpeed;
        }

        if (this.disassemblySlowdown) {
            lerpDistance *= this.slowdownController.getCountdown() / this.slowdownController.getMaxTime();
        }

        return lerpDistance;
    }

    public void applyTilt() {
        this.tiltVector.normalize();
        this.tiltQuat = SimMathUtils.getQuaternionfFromVectorRotation(this.blockNormal, this.tiltVector);
        this.thrustDirection.set(this.tiltVector);
        PropellerBearingContraptionEntity propellerContraption = this.getMovedContraption();
        if (propellerContraption == null) {
            return;
        }

        propellerContraption.tiltQuat = new Quaternionf(this.tiltQuat);
        propellerContraption.direction = this.getBlockState().getValue(PropellerBearingBlock.FACING);
    }


    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        this.physicsTimer += timeStep * 20;
        PHYSICS_THRUST.set(this.tiltVector);
        this.updateTilt(PHYSICS_THRUST, subLevel, this.physicsTimer);
        this.thrustDirection.set(PHYSICS_THRUST);

        if (this.isActive()) {
            super.applyForces(subLevel, JOMLConversion.toMojang(this.thrustDirection), timeStep);
        }
    }

    @Override
    public ValueBoxTransform getMovementModeSlot() {
        return new GyroscopicPropellerValueBoxTransform();
    }

    private static class GyroscopicPropellerValueBoxTransform extends DirectionalExtenderScrollOptionSlot {

        public GyroscopicPropellerValueBoxTransform() {
            super((state, d) -> {
                final Direction.Axis axis = d.getAxis();
                final Direction.Axis bearingAxis = state.getValue(BearingBlock.FACING).getAxis();
                return bearingAxis != axis;
            });
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf((state.getValue(BlockStateProperties.FACING)).getNormal()).scale(-0.125));
        }
    }
}
