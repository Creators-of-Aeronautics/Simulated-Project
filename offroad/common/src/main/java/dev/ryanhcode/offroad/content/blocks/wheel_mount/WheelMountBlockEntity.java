package dev.ryanhcode.offroad.content.blocks.wheel_mount;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.data.OffroadLang;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.multiloader.inventory.SingleSlotContainer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.List;

public class WheelMountBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor, Clearable, ClipboardCloneable, SpecialBlockEntityItemRequirement {
    private static final MutableComponent SCROLL_OPTION_TITLE = OffroadLang.translate("scroll_option.suspension_strength").component();
    private static final double MAX_ALLOWED_EXTENSION = 0.65;
    private static final double NO_WHEEL_EXTENSION = 0.5;

    private static final Collection<WheelMountBlockEntity> queuedWheelMounts = new ObjectOpenHashSet<>();
    private final WheelMountInventory inventory;
    private SuspensionStrengthValueBehaviour strength;

    private int clientSteeringSignal;
    protected int clientSteeringSignalLeft;
    protected int clientSteeringSignalRight;
    private double extension = NO_WHEEL_EXTENSION, lastExtension = this.extension;
    private double chasingYaw, lastChasingYaw;
    private double lastAngle, angle;
    private double angularVelocity = 0.0;
    private double touchingFriction = 1.0;

    private int lastServerSteeringSignal;
    private int lastServerSteeringSignalLeft;
    private int lastServerSteeringSignalRight;

    private boolean liftedUp = false;
    private final Vector3d queuedForcePos = new Vector3d();
    private final Vector3d queuedForce = new Vector3d();

    private final ForceTotal forceTotal = new ForceTotal();

    public WheelMountBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.inventory = new WheelMountInventory(this);
    }

    public static void applyAllBatchedForces(final ServerLevel level, final double timeStep) {
        for (final WheelMountBlockEntity blockEntity : queuedWheelMounts) {
            if (blockEntity.isRemoved())
                continue;

            blockEntity.applyBatchedForces();
        }
        queuedWheelMounts.clear();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        behaviours.add(this.strength = new SuspensionStrengthValueBehaviour(SCROLL_OPTION_TITLE, this, new SuspensionStrengthValueBox(0)));
        this.strength.value = 10;
    }

    @Override
    public ItemRequirement getRequiredItems(final BlockState state) {
        final ItemStack stack = this.inventory.slot.getStack();

        if (stack.isEmpty()) {
            return super.getRequiredItems(state);
        }

        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, stack);
    }

    // give a slight amount of friction even when at 0
    public static double fudgeFriction(final double realValue) {
        if (realValue < 1) {
            return 0.1 + 0.9 * realValue;
        }
        return realValue;
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        final ItemStack item = this.getHeldItem();
        final TireLike tire = item.get(OffroadDataComponents.TIRE);
        final BlockPos blockPos = this.getBlockPos();

        if (tire == null) {
            return;
        }

        final float radius = tire.radius();

        final double suspensionRestDistance = MAX_ALLOWED_EXTENSION;

        final MassData massData = subLevel.getMassTracker();

        final Direction facing = this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
        final Vec3 localPos = blockPos.relative(facing).getCenter();
        this.queuedForcePos.set(localPos.x, localPos.y, localPos.z);
        final double normalMass = 1.0 / massData.getInverseNormalMass(this.queuedForcePos, OrientedBoundingBox3d.UP);

        final double effectiveStrength = this.strength.getValue();
        final double normalMassScaling = Math.min(normalMass / effectiveStrength, 1.0) * 10.0;

        final double strengthMul = effectiveStrength * normalMassScaling * 2;
        final double springStrength = effectiveStrength * normalMassScaling * 40;
        final double dampingStrength = effectiveStrength * normalMassScaling;

        final Pose3d pose = subLevel.logicalPose();

        final Direction.Axis axis = facing.getAxis();
        Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis).getNormal();
        final Vector3dc sideD = this.getRotatedWheelAxis(normal);
        normal = new Vec3i(normal.getZ(), 0, normal.getX());
        final Vector3dc normalD = this.getRotatedWheelAxis(normal);

        final TerrainCastResult extensionToTerrain = this.computeMaxExtensionToTerrain(normalD, pose);
        final double maxExtension = extensionToTerrain.maxExtension();

        this.extension = Mth.lerp(1.0, this.extension, maxExtension);

        if (maxExtension > suspensionRestDistance + radius + 0.25) {
            this.extension = suspensionRestDistance;
            return;
        }

        final double distance = (suspensionRestDistance / 6.0) + this.extension;
        final double springLength = Mth.clamp(distance - radius, 0.0, suspensionRestDistance);

        final Vector3d velocity = Sable.HELPER.getVelocity(this.level, JOMLConversion.toJOML(localPos));
        final Vector3d localVelocity = pose.transformNormalInverse(velocity);

        final double dampingForce = -localVelocity.y * dampingStrength;

        final double springForce = ((suspensionRestDistance - springLength) * springStrength + dampingForce) * timeStep;

        final Vec3i rayHitNormal = extensionToTerrain.normal().getNormal();

        Vec3 localForce = new Vec3(springForce * rayHitNormal.getX(), springForce * rayHitNormal.getY(), springForce * rayHitNormal.getZ());
        if (extensionToTerrain.subLevel() != null) {
            localForce = extensionToTerrain.subLevel().logicalPose().transformNormal(localForce);
        }
        localForce = pose.transformNormalInverse(localForce);

        this.queuedForce.set(localForce.x, localForce.y, localForce.z);

        // damping
        {
            if (extensionToTerrain.minInteractingBlock() != null) {
                this.touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(this.level.getBlockState(extensionToTerrain.minInteractingBlock())));
            } else {
                this.touchingFriction = 1.0;
            }

            final double brakeStrength = this.level.getSignal(blockPos.above(), Direction.UP) / 15.0;
            final double surfaceBraking = Math.min(this.touchingFriction, 1.0);
            final double brakingFrictionStrength = (0.075 + brakeStrength * 0.3) * surfaceBraking;

            final float kineticSpeed = facing.getAxis() == Direction.Axis.X ? this.getSpeed() : -this.getSpeed();
            this.queuedForce.fma(
                    (localVelocity.dot(normalD) * -brakingFrictionStrength * strengthMul * timeStep) +
                    (kineticSpeed * (1.0 - brakeStrength) * surfaceBraking * 1.75 * timeStep)
                    , normalD);
            this.queuedForce.fma(localVelocity.dot(sideD) * -0.6 * this.touchingFriction * strengthMul * timeStep, sideD);
        }

        this.forceTotal.applyImpulseAtPoint(subLevel, this.queuedForcePos, this.queuedForce);
        queuedWheelMounts.add(this);
    }


    private void applyBatchedForces() {
        final SubLevel subLevel = Sable.HELPER.getContaining(this);

        if (subLevel == null) {
            return;
        }

        final RigidBodyHandle handle = RigidBodyHandle.of((ServerSubLevel) subLevel);

        handle.applyForcesAndReset(this.forceTotal);
    }

    @Override
    public void tick() {
        super.tick();

        final ItemStack item = this.getHeldItem();
        final TireLike tire = item.get(OffroadDataComponents.TIRE);

        this.lastChasingYaw = this.chasingYaw;
        this.chasingYaw = Mth.lerp(0.4, this.chasingYaw, this.computeYaw());

        if (!this.level.isClientSide) return;

        if (tire == null) {
            this.angle = 0.0;
            this.lastAngle = 0.0;

            this.lastExtension = this.extension;
            this.extension = Mth.lerp(0.6, this.extension, NO_WHEEL_EXTENSION);
            return;
        }

        final float radius = tire.radius();

        final SubLevel subLevel = Sable.HELPER.getContaining(this);

        this.lastExtension = this.extension;
        this.extension = Mth.lerp(0.7, this.extension, this.computeMaxExtension(radius));

        final Direction facing = this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
        final float speed = facing.getAxis() == Direction.Axis.X ? -this.getSpeed() : this.getSpeed();
        final double rpt = speed * Math.PI * 2.0 / 60.0 / 20.0 * (15 - this.level.getSignal(this.getBlockPos().above(), Direction.UP)) / 15.0;
        final double attemptedAngularVelocity = Mth.lerp(0.2, this.angularVelocity, rpt);

        if (subLevel == null || this.liftedUp) {
            this.angularVelocity = attemptedAngularVelocity;
            this.lastAngle = this.angle;
            this.angle += this.angularVelocity;
            return;
        }

        final Vector3d velocity = Sable.HELPER.getVelocity(this.level, JOMLConversion.atCenterOf(this.getBlockPos().relative(facing)));
        final Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(velocity).div(20.0);
        final Direction.Axis axis = facing.getAxis();

        Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis).getNormal();
        normal = new Vec3i(normal.getZ(), 0, normal.getX());
        final Vector3dc normalD = this.getRotatedWheelAxis(normal);

        final double translation = localVelocity.dot(normalD);

        // compute the angle we must've changed for the current tick
        final double circumference = Math.PI * radius * 2.0;
        double angularDelta = -translation / circumference * Math.PI * 2.0;

        // visually slip if trying to drive on slippery surface
        if (this.touchingFriction < 1.0) {
            angularDelta = Mth.lerp(this.touchingFriction, attemptedAngularVelocity, angularDelta);
        }

        this.lastAngle = this.angle;
        this.angle += angularDelta;
        this.angularVelocity = angularDelta;
    }

    private double computeMaxExtension(final float radius) {
        final SubLevel subLevel = Sable.HELPER.getContaining(this);

        if (subLevel == null) {
            return MAX_ALLOWED_EXTENSION;
        }

        final Direction facing = this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
        final Pose3dc pose = subLevel.logicalPose();

        final Direction.Axis axis = facing.getAxis();
        Vec3i normal = Direction.get(Direction.AxisDirection.POSITIVE, axis).getNormal();
        normal = new Vec3i(normal.getZ(), 0, normal.getX());
        final Vector3dc rotatedAxis = this.getRotatedWheelAxis(normal);

        final TerrainCastResult extensionToTerrain = this.computeMaxExtensionToTerrain(rotatedAxis, pose);
        final double unclampedExtension = extensionToTerrain.maxExtension - radius;

        this.liftedUp = unclampedExtension > MAX_ALLOWED_EXTENSION;
        if (extensionToTerrain.minInteractingBlock() == null) {
            this.touchingFriction = 1.0;
        } else {
            this.touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(this.level.getBlockState(extensionToTerrain.minInteractingBlock())));
        }

        return Mth.clamp(unclampedExtension, -0.45, MAX_ALLOWED_EXTENSION);
    }

    @Override
    public String getClipboardKey() {
        return "Wheel Mount";
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider registries, final CompoundTag tag, final Direction side) {
        return false;
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.@NotNull Provider registries, final CompoundTag tag, final Player player, final Direction side, final boolean simulate) {
        return false;
    }

    private record TerrainCastResult(double maxExtension, @NotNull Direction normal, @Nullable SubLevel subLevel, @Nullable BlockPos minInteractingBlock) {}

    private TerrainCastResult computeMaxExtensionToTerrain(final Vector3dc normalD, final Pose3dc pose) {
        final Direction facing = this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
        final Vec3 wheelPosCenter = this.getBlockPos().relative(facing).getCenter();
        double minExtension = 5.0;
        Direction minNormal = Direction.UP;
        SubLevel minHitSubLevel = null;
        BlockPos minInteractingBlock = null;

        for (int i = -1; i <= 1; i++) {
            final Vec3 localPosO = wheelPosCenter.add(JOMLConversion.toMojang(normalD).scale(i));

            final ClipContext clipContext = new ClipContext(localPosO, localPosO.subtract(0.0, 5.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) clipContext).sable$setIgnoredSubLevel(Sable.HELPER.getContaining(this));
            final BlockHitResult clipResult = this.level.clip(clipContext);

            if (clipResult.getType() == HitResult.Type.MISS) {
                continue;
            }

            final SubLevel hitSubLevel = Sable.HELPER.getContaining(this.level, clipResult.getLocation());
            final Vec3 localHitPos = pose.transformPositionInverse(hitSubLevel == null ? clipResult.getLocation() : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation()));

            if (localHitPos.y > wheelPosCenter.y) {
                continue;
            }

            if (localPosO.distanceTo(localHitPos) < 0.05) {
                continue;
            }

            final double dist = wheelPosCenter.y - localHitPos.y;

            if (dist <= 1e-5) {
                continue;
            }

            final Direction dir = clipResult.getDirection();
            final Vector3d hitNormal = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());

            if (hitSubLevel != null) {
               hitSubLevel.logicalPose().transformNormal(hitNormal);
            }
            pose.transformNormalInverse(hitNormal);

            if (hitNormal.dot(0.0, 1.0, 0.0) < 0.5) {
                continue;
            }

            minExtension = Math.min(minExtension, dist);
            minNormal = clipResult.getDirection();
            minHitSubLevel = hitSubLevel;
            minInteractingBlock = clipResult.getBlockPos();
        }

        return new TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
    }

    private @NotNull Vector3dc getRotatedWheelAxis(final Vec3i normal) {
        final Vector3d normalD = new Vector3d(normal.getX(), normal.getY(), normal.getZ());
        normalD.rotateY(this.getChasingYaw());
        return normalD;
    }

    protected double getChasingYaw() {
        return this.chasingYaw;
    }

    protected double getLerpedYaw(final double partialTick) {
        return Mth.lerp(partialTick, this.lastChasingYaw, this.chasingYaw);
    }

    public float getLerpedAngle(final float partialTicks) {
        return (float) Mth.lerp(partialTicks, this.lastAngle, this.angle);
    }

    public double getLerpedExtension(final float partialTick) {
        return Mth.lerp(partialTick, this.lastExtension, this.extension);
    }

    protected double computeYaw() {
        final int signal = this.getSteeringSignal();
        if (signal == 0) return 0.0;
        return (-signal / 15.0 * Math.PI / 4.0 * (30.0 / 45.0));
    }

    protected int getSteeringSignal() {
        if (this.level.isClientSide) {
            return this.clientSteeringSignal;
        }

        final BlockState state = this.getBlockState();
        final Direction facing = state.getValue(WheelMountBlock.HORIZONTAL_FACING);

        final Direction d1 = facing.getClockWise();
        final Direction d2 = facing.getCounterClockWise();
        final BlockPos pos = this.getBlockPos();

        final int signalLeft = this.level.getSignal(pos.relative(d1), d2);
        final int signalRight = this.level.getSignal(pos.relative(d2), d1);
        final int signal = signalLeft - signalRight;

        final boolean sendData = signal != this.lastServerSteeringSignal || signalLeft != this.lastServerSteeringSignalLeft || signalRight != this.lastServerSteeringSignalRight;

        this.lastServerSteeringSignal = signal;
        this.lastServerSteeringSignalLeft = signalLeft;
        this.lastServerSteeringSignalRight = signalRight;

        if (sendData) {
            this.sendData();
        }

        return signal;
    }

    public ItemStack getHeldItem() {
        return this.inventory.getItem(0);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        tag.put("CurrentStack", this.getHeldItem().saveOptional(registries));

        if (clientPacket) {
            tag.putInt("SteeringSignalStrength", this.lastServerSteeringSignal);
            tag.putInt("SteeringSignalStrengthLeft", this.lastServerSteeringSignalLeft);
            tag.putInt("SteeringSignalStrengthRight", this.lastServerSteeringSignalRight);
        }

        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        final ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("CurrentStack"));

        this.inventory.suppressUpdate = true;
        this.inventory.slot.setStack(stack);
        this.inventory.suppressUpdate = false;

        if (clientPacket) {
            if (tag.contains("SteeringSignalStrength")) {
                this.clientSteeringSignal = tag.getInt("SteeringSignalStrength");
                this.clientSteeringSignalLeft = tag.getInt("SteeringSignalStrengthLeft");
                this.clientSteeringSignalRight = tag.getInt("SteeringSignalStrengthRight");
            }
            this.onStackChanged();
        }

        super.read(tag, registries, clientPacket);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    public SingleSlotContainer getInventory() {
        return this.inventory;
    }

    public void onStackChanged() {
        this.invalidateRenderBoundingBox();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB aabb = new AABB(this.getBlockPos());
        if(this.getHeldItem() != null && this.getHeldItem().has(OffroadDataComponents.TIRE)) {
            final TireLike tire = this.getHeldItem().getComponents().get(OffroadDataComponents.TIRE);
            aabb = aabb.inflate(tire.radius() + 1);
        }
        return aabb;
    }

    private static class SuspensionStrengthValueBehaviour extends ScrollValueBehaviour {
        private static final int MAX_SUSPENSION_STRENGTH = 180;

        public SuspensionStrengthValueBehaviour(final Component label, final SmartBlockEntity be, final ValueBoxTransform slot) {
            super(label, be, slot);
            this.between(5, MAX_SUSPENSION_STRENGTH);
        }

        @Override
        public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
            return new ValueSettingsBoard(this.label, MAX_SUSPENSION_STRENGTH, 20, ImmutableList.of(OffroadLang.translate("scroll_option.suspension_strength_label").component()),
                    new ValueSettingsFormatter(ValueSettings::format));
        }
    }
    private static final class SuspensionStrengthValueBox extends ValueBoxTransform {
        private final int hOffset;

        public SuspensionStrengthValueBox(final int hOffset) {
            this.hOffset = hOffset;
        }

        @Override
        public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
            final Direction facing = state.getValue(RollerBlock.FACING);
            final float yRot = AngleHelper.horizontalAngle(facing) + 180;
            TransformStack.of(ms)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(level, pos, state);
            if (offset == null)
                return false;
            return localHit.distanceTo(offset) < this.scale / 3;
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            final Direction facing = state.getValue(RollerBlock.FACING);
            final float stateAngle = AngleHelper.horizontalAngle(facing) + 180;
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8 + this.hOffset, 15.5f, 11), stateAngle, Direction.Axis.Y);
        }
    }
}
