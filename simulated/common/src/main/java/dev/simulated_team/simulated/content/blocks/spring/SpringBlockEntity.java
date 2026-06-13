package dev.simulated_team.simulated.content.blocks.spring;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.content.items.spring.SpringItemHandler;
import dev.simulated_team.simulated.util.SimLevelUtil;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.UUID;

public class SpringBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {
    private static final Vector3d frictionForce = new Vector3d();
    private static final Vector3d frictionTorque = new Vector3d();
    private static final Vector3d localLinearVelocity = new Vector3d();
    private static final Vector3d localAngularVelocity = new Vector3d();
    private static final Vector3d expectedVelocity = new Vector3d();
    private static final Vector3d localDampingPointForce = new Vector3d();

    /**
     * Time the spring must be over-extended past the snap extension to snap
     */
    private static final double TIME_TO_SNAP = 0.75;

    protected LerpedFloat renderLength = LerpedFloat.linear();
    protected double desiredLength;
    protected boolean isController;
    protected boolean assembling;

    /**
     * The block position of the attached sub-level
     */
    protected BlockPos partnerPos;

    /**
     * The ID of the attached sub-level
     */
    @Nullable
    private UUID partnerSubLevel;

    private float ticksWithoutPartner = 0;

    private ForceTotal forceTotal;
    private ForceTotal partnerForceTotal;

    /**
     * The amount of time we're ready to snap
     */
    private double snappingTime;

    public SpringBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.renderLength.chase(0, 0.2, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
    }

    public Vector3d getCenter() {
        final BlockState state = this.getBlockState();

        final Direction facing = state.getValue(SpringBlock.FACING);
        final Vec3i facingVec = facing.getNormal();

        double scale = 0.5 - 4.0 / 16.0;
        return JOMLConversion.atCenterOf(this.worldPosition)
                .sub(facingVec.getX() * scale,
                        facingVec.getY() * scale,
                        facingVec.getZ() * scale);
    }

    public @Nullable String tryChangeLengthOrError(final Level level, final double delta) {
        if (delta > 0 && this.desiredLength >= SpringItemHandler.MAX_LENGTH) {
            return "max_length";
        }
        if (delta < 0 && this.desiredLength <= 1) {
            return "min_length";
        }
        double newDesiredLength = Math.clamp(this.desiredLength + delta, 1, SpringItemHandler.MAX_LENGTH);
        newDesiredLength = Math.round(newDesiredLength / 0.25) * 0.25;

        final double currentLength = Sable.HELPER.distanceSquaredWithSubLevels(level, this.worldPosition.getCenter(), this.partnerPos.getCenter()) + 1;
        if (delta < 0 && currentLength > newDesiredLength * newDesiredLength * 4) {
            return "too_stretched";
        }
        if (delta > 0 && currentLength < newDesiredLength * newDesiredLength / 4) {
            return "too_compressed";
        }

        this.desiredLength = newDesiredLength;
        this.setChanged();
        this.sendData();
        if (level.getBlockEntity(this.partnerPos) instanceof final SpringBlockEntity partnerBE) {
            partnerBE.desiredLength = this.desiredLength;
            partnerBE.setChanged();
            partnerBE.sendData();
        }
        return null;
    }

    public double getRenderLength(final float pt) {
        return this.renderLength.getValue(pt);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide) {
            this.renderLength.updateChaseTarget((float) this.desiredLength);
            this.renderLength.tickChaser();
            return;
        }

        if (this.snappingTime > TIME_TO_SNAP) {
            this.level.destroyBlock(this.getBlockPos(), true);
        }

        if (this.partnerPos != null) {
            if (SimLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.partnerPos, 1)) {
                if (this.getPairedSpring() == null && this.ticksWithoutPartner++ > 20) {
                    this.level.destroyBlock(this.getBlockPos(), true);
                } else {
                    this.ticksWithoutPartner = 0;
                }
            }
        } else {
            this.level.destroyBlock(this.getBlockPos(), true);
        }
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        final SpringBlockEntity partner = this.getPairedSpring();
        if (this.partnerPos == null || !SimLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.partnerPos, 1) || partner == null || this.ticksWithoutPartner != 0) {
            return;
        }

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
        final SubLevelPhysicsSystem system = container.physicsSystem();

        system.updatePose(subLevel);

        final ServerSubLevel partnerSubLevel = this.partnerSubLevel != null ? (ServerSubLevel) container.getSubLevel(this.partnerSubLevel) : null;

        if (this.partnerSubLevel != null && partnerSubLevel == null) {
            return;
        }

        if (partnerSubLevel != null && !this.isController) {
            return;
        }
        if (partnerSubLevel == subLevel) {
            return;
        }

        if (partnerSubLevel != null)
            system.updatePose(partnerSubLevel);

        final BlockState state = this.getBlockState();
        final SpringBlock.Size size = state.getValue(SpringBlock.SIZE);

        final Vector3dc center = this.getCenter();
        final Vector3dc partnerCenter = partner.getCenter();
        final Vector3d velo1 = Sable.HELPER.getVelocity(this.level, center, new Vector3d());
        final Vector3d velo2 = Sable.HELPER.getVelocity(this.level, partnerCenter, new Vector3d());

        final Vector3d positionA = subLevel.logicalPose().transformPosition(center, new Vector3d());
        final Vector3d positionB = partnerSubLevel != null ? partnerSubLevel.logicalPose().transformPosition(JOMLConversion.atCenterOf(this.partnerPos)) : JOMLConversion.atCenterOf(this.partnerPos);

        //#region damping
        final Vector3d relativeVelo = velo1.sub(velo2);
        final Vector3d dampingPointForce = new Vector3d(relativeVelo);
        dampingPointForce.mul(-4.5);
        //#endregion

        // math shenanigans means that a desiredLength value of 1.25 would lead to an actual length of 2 blocks
        final double desiredLength = (this.isController ? this.desiredLength : partner.desiredLength) - 0.75;

        // Snap the spring if it's overextended
        if (positionA.distanceSquared(positionB) > Mth.square(this.getSnappingDistance())) {
            this.snappingTime += timeStep;
        } else {
            this.snappingTime = 0.0;
        }

        //#region alignment torque
        final Vector3d globalNormalA = JOMLConversion.atLowerCornerOf(state.getValue(SpringBlock.FACING).getNormal());
        final Vector3d globalNormalB = JOMLConversion.atLowerCornerOf(partner.getBlockState().getValue(SpringBlock.FACING).getNormal());

        subLevel.logicalPose().transformNormal(globalNormalA);
        if (partnerSubLevel != null) {
            partnerSubLevel.logicalPose().transformNormal(globalNormalB);
        }

        final Vector3d torque = globalNormalA.cross(globalNormalB.negate(), new Vector3d())
                .mul(20.0)
                .mul(timeStep);
        //#endregion

        //#region alignment force
        // both springs desire their target to be desired length
        final Vector3d mediumNormal = globalNormalA.lerp(globalNormalB, 0.5);

        final Vector3dc middle = new Vector3d(positionA.x, positionA.y, positionA.z).lerp(positionB, 0.5);
        final Vector3d desireA = middle.fma(-desiredLength / 2.0, mediumNormal, new Vector3d());

        final Vector3d alignmentForce = desireA.sub(positionA.x, positionA.y, positionA.z);

        final Vector3d hookesPointForce = alignmentForce.mul(145.0);
        //#endregion

        //#region angular damping along spring axes (to stop spinning !)
        final Vector3d angVelo1 = new Vector3d();
        final Vector3d angVelo2 = new Vector3d();
        handle.getAngularVelocity(angVelo1);
        if (partnerSubLevel != null) {
            final RigidBodyHandle otherHandle = RigidBodyHandle.of(partnerSubLevel);
            otherHandle.getAngularVelocity(angVelo2);
        }

        final Vector3d relativeAngVelo = angVelo1.sub(angVelo2);
        final Vector3d dampingTorque = new Vector3d();
        if (mediumNormal.lengthSquared() > 0.0) {
            mediumNormal.normalize();
            final double dot = mediumNormal.dot(relativeAngVelo);
            relativeAngVelo.set(mediumNormal).mul(dot);
            dampingTorque.fma(-2.0, relativeAngVelo);
        }

        final double sizeScale = switch (size) {
            case LARGE -> 8.0;
            case MEDIUM -> 1.0;
            case SMALL -> 0.5;
        };

        hookesPointForce.mul(sizeScale);
        torque.mul(sizeScale);
        dampingTorque.mul(sizeScale);
        dampingPointForce.mul(sizeScale);

        if (this.forceTotal == null || this.partnerForceTotal == null) {
            this.forceTotal = new ForceTotal();
            this.partnerForceTotal = new ForceTotal();
        }

        this.applyLocalDamping(subLevel, handle, this.forceTotal, center, dampingPointForce, dampingTorque, timeStep);
        this.forceTotal.applyImpulseAtPoint(
                subLevel,
                center,
                subLevel.logicalPose().transformNormalInverse(new Vector3d(hookesPointForce)).mul(timeStep)
        );
        this.forceTotal.applyLinearAndAngularImpulse(JOMLConversion.ZERO, subLevel.logicalPose().transformNormalInverse(torque, new Vector3d()));
        handle.applyForcesAndReset(this.forceTotal);

        if (partnerSubLevel != null) {
            final RigidBodyHandle partnerHandle = RigidBodyHandle.of(partnerSubLevel);
            this.applyLocalDamping(partnerSubLevel, partnerHandle, this.partnerForceTotal, partnerCenter, dampingPointForce.negate(), dampingTorque.negate(), timeStep);
            this.partnerForceTotal.applyImpulseAtPoint(
                    partnerSubLevel,
                    partnerCenter,
                    partnerSubLevel.logicalPose().transformNormalInverse(hookesPointForce).mul(-timeStep)
            );
            this.partnerForceTotal.applyLinearAndAngularImpulse(JOMLConversion.ZERO, partnerSubLevel.logicalPose().transformNormalInverse(torque.negate()));
            partnerHandle.applyForcesAndReset(this.partnerForceTotal);
        }
    }

    private void applyLocalDamping(final ServerSubLevel subLevel,
                                   final RigidBodyHandle handle,
                                   final ForceTotal forceTotal,
                                   final Vector3dc worldSpringPos,
                                   final Vector3dc dampingPointForce,
                                   final Vector3dc dampingTorque,
                                   final double timeStep) {
        final Pose3d pose = subLevel.logicalPose();

        handle.getAngularVelocity(localAngularVelocity);
        handle.getLinearVelocity(localLinearVelocity);

        pose.orientation().transformInverse(localAngularVelocity);
        pose.orientation().transformInverse(localLinearVelocity);

        final Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
        pose.orientation().transformInverse(dampingPointForce, localDampingPointForce);

        final Vector3d angularDamping = new Vector3d();
        angularDamping.add(dampingTorque);
        pose.orientation().transformInverse(angularDamping);
        angularDamping.add(worldSpringPos.sub(centerOfMass, new Vector3d()).cross(localDampingPointForce));

        final Vector3d linearDamping = new Vector3d();
        linearDamping.add(localDampingPointForce);

        frictionForce.set(linearDamping);
        frictionTorque.set(angularDamping);

        expectedVelocity.set(frictionForce);
        expectedVelocity.mul(subLevel.getMassTracker().getInverseMass());
        expectedVelocity.mul(timeStep);
        final double forceScale = this.getClampingFactor(localLinearVelocity, expectedVelocity);

        expectedVelocity.set(frictionTorque);
        subLevel.getMassTracker().getInverseInertiaTensor().transform(expectedVelocity);
        expectedVelocity.mul(timeStep);
        final double torqueScale = this.getClampingFactor(localAngularVelocity, expectedVelocity);

        frictionForce.mul(forceScale * timeStep);
        frictionTorque.mul(torqueScale * timeStep);

        forceTotal.applyLinearAndAngularImpulse(frictionForce, frictionTorque);
    }

    /**
     * TODO: in physics utils?
     */
    private double getClampingFactor(final Vector3dc currentVelocity, final Vector3dc expectedVelocityChange) {
        final double k = -currentVelocity.dot(expectedVelocityChange);
        final double v = currentVelocity.lengthSquared();
        if (k < 0) // don't apply friction that increases velocity
            return 0;
        if (10 * k < v) // if the expected velocity is 10 times smaller than the actual velocity, dont bother with clamping it
            return 1;
        if (v < 1E-10) // simpler clamping for tiny values to avoid inaccuracies and numerical explosion
            return v / (k + 1E-10);
        return v * (1 - Math.exp(-k / v)) / k;
    }


    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        if (this.partnerSubLevel != null) {
            final SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.partnerSubLevel);

            if (subLevel != null) {
                return List.of(subLevel);
            }
        }

        return List.of();
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && this.partnerPos != null && !this.assembling) {
            this.level.destroyBlock(this.partnerPos, false);
        }

        this.partnerPos = null;
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putBoolean("Controller", this.isController);
        tag.putDouble("DesiredLength", this.desiredLength);

        if (this.partnerPos == null) return;

        final SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();

        // we can avoid all that logic if there is no schematic context, or we're placing into a schematic level (just carry over the nbt into it)
        if (schematicContext == null || schematicContext.getType() == SubLevelSchematicSerializationContext.Type.PLACE) {
            if (this.partnerSubLevel != null) {
                tag.putUUID("GoalSubLevel", this.partnerSubLevel);
            }

            BlockPos partnerPos = this.partnerPos;
            if (partnerPos != null) {
                if (schematicContext != null && this.partnerSubLevel == null) {
                    partnerPos = schematicContext.getSetupTransform().apply(partnerPos);
                }

                tag.putLong("Goal", partnerPos.asLong());
            }

            return;
        }

        BlockPos partnerPos = this.partnerPos;
        UUID id = this.partnerSubLevel;

        if (id != null) {
            final SubLevelSchematicSerializationContext.SchematicMapping mapping = schematicContext.getMapping(id);

            if (mapping != null) {
                id = mapping.newUUID();
                partnerPos = mapping.transform().apply(partnerPos);
            } else {
                // the other end of the spring isn't being schematic-ed.
                // self destruct!!
                id = null;
                partnerPos = null;
            }
        } else {
            if (schematicContext.getBoundingBox().contains(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ())) {
                partnerPos = schematicContext.getPlaceTransform().apply(partnerPos);
            } else {
                // the other end of the spring isn't being schematic-ed.
                // self destruct!!
                partnerPos = null;
            }
        }

        if (partnerPos != null) {
            tag.putLong("Goal", partnerPos.asLong());
        }

        if (id != null) {
            tag.putUUID("GoalSubLevel", id);
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.isController = tag.getBoolean("Controller");
        this.desiredLength = tag.getDouble("DesiredLength");

        if (this.renderLength.getValue() == 0) {
            this.renderLength.setValue(this.desiredLength);
        }

        final SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();
        final boolean isPlacingFromSchematic = schematicContext != null && schematicContext.getType() == SubLevelSchematicSerializationContext.Type.PLACE;
        SubLevelSchematicSerializationContext.SchematicMapping mapping = null;

        if (tag.hasUUID("GoalSubLevel")) {
            UUID subLevelID = tag.getUUID("GoalSubLevel");

            if (isPlacingFromSchematic) {
                mapping = schematicContext.getMapping(subLevelID);

                if (mapping == null) {
                    // we're missing the mapping!!
                    this.partnerSubLevel = null;
                    this.partnerPos = null;
                    return;
                }

                subLevelID = mapping.newUUID();
            }

            this.partnerSubLevel = subLevelID;
        }

        if (tag.contains("Goal")) {
            BlockPos blockPos = BlockPos.of(tag.getLong("Goal"));

            if (isPlacingFromSchematic) {
                if (mapping != null) {
                    blockPos = mapping.transform().apply(blockPos);
                } else {
                    blockPos = schematicContext.getPlaceTransform().apply(blockPos);
                }
            }

            this.partnerPos = blockPos;
        }
    }

    public void setPartnerPos(final BlockPos pos, final UUID subLevel) {
        this.partnerPos = pos;
        this.partnerSubLevel = subLevel;
        this.sendData();
    }

    public boolean isController() {
        return this.isController;
    }

    public void setController(final boolean b) {
        this.isController = b;
    }

    public double getSnappingDistance() {
        return this.desiredLength * 4.0 + 2.0;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        this.invalidateRenderBoundingBox();
    }

    public @Nullable SpringBlockEntity getPairedSpring() {
        if (this.partnerPos == null) {
            return null;
        }

        final BlockEntity be = this.level.getBlockEntity(this.partnerPos);
        if (be instanceof SpringBlockEntity) {
            return (SpringBlockEntity) be;
        }
        return null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        final SpringBlockEntity goal = this.getPairedSpring();
        if (goal == null) {
            return new AABB(this.getBlockPos());
        }

        final Vec3 center = this.getBlockPos().getCenter();
        Vec3 partnerPos = this.partnerPos.getCenter();

        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        final SubLevel partnerSubLevel = Sable.HELPER.getContaining(this.level, this.partnerPos);
        if (partnerSubLevel != null) {
            partnerPos = partnerSubLevel.logicalPose().transformPosition(partnerPos);
        }
        if (subLevel != null) {
            partnerPos = subLevel.logicalPose().transformPositionInverse(partnerPos);
        }

        return new AABB(center, partnerPos).inflate(3.0);
    }

    public void setDesiredLength(final double desiredLength) {
        this.desiredLength = desiredLength;
    }

    public UUID getPartnerSubLevelID() {
        return this.partnerSubLevel;
    }
}
