package dev.simulated_team.simulated.content.blocks.gimbal_sensor;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.data.SimLang;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.*;

public class GimbalSensorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private static final Pose3dc IDENTITY_POSE = new Pose3d(new Vector3d(), new Quaterniond(), new Vector3d(), new Vector3d(1.0));
    private static final double MAX_ANGLE_X = Math.toRadians(90);
    private static final double MAX_ANGLE_Z = Math.toRadians(90);
    private final EnumMap<Direction, Integer> redstoneMap;

    // Client Animations
    private final Vector3d previousAngles = new Vector3d(0, 0, 0);
    private final Vector3d angleInertia = new Vector3d(110, 110, 34);
    private final Vector3d angleDamping = new Vector3d(0.2, 0.2, 0.2);

    /**
     * should only be false in the ponder for easier rendering
     */
    public boolean updateVisualRotation = true;
    CompassTarget compassTarget = new CompassTarget();
    private GimbalSensorScrollValueBehaviour axisBehaviour;
    private Vector3d eulerAngles = new Vector3d(0, 0, 0);
    private Vector3d angleVelocities = new Vector3d(0, 0, 0);
    private Quaterniond lastShellOrientation = null;

    //stored separately for goggle information and display sources
    private double ZAngle;
    private double XAngle;

    private final Vector3d angularVelocityBody = new Vector3d();
    private final Vector3d gravityBody = new Vector3d();

    public GimbalSensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);

        //Unknown if this is needed, but better to be safe than sorry - cyvack
        this.redstoneMap = new EnumMap<>(Direction.class);
        for (final Direction dir : Iterate.directions) {
            this.redstoneMap.put(dir, 0);
        }
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        behaviours.add((this.axisBehaviour = new GimbalSensorScrollValueBehaviour(this)).between(-90, 90));
        this.axisBehaviour.primaryValue = 45;
        this.axisBehaviour.secondaryValue = 45;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.randomNudge();

    }

    @Override
    public void tick() {
        super.tick();
        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        final Pose3dc pose = subLevel != null ? subLevel.logicalPose() : IDENTITY_POSE;

        if (this.level.isClientSide)
            this.animateClientRotation(subLevel, pose);
        if (subLevel == null)
            return;

        final Vector3d ld = JOMLConversion.toJOML(Vec3.atLowerCornerOf(Direction.DOWN.getNormal()));
        subLevel.logicalPose().orientation().transformInverse(ld);

        this.XAngle = ld.y() < 0 || ld.z() * ld.z() > 0.001 ? atan2(ld.z(), -ld.y()) : 0;
        this.ZAngle = ld.y() < 0 || ld.x() * ld.x() > 0.001 ? atan2(ld.x(), -ld.y()) : 0;

        this.setPower(this.ZAngle, Direction.EAST);
        this.setPower(-this.ZAngle, Direction.WEST);

        this.setPower(this.XAngle, Direction.SOUTH);
        this.setPower(-this.XAngle, Direction.NORTH);

        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            RigidBodyHandle.of(serverSubLevel).getAngularVelocity(this.angularVelocityBody);
            serverSubLevel.logicalPose().orientation().transformInverse(this.angularVelocityBody);

            final Vector3dc worldPos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos()));
            this.gravityBody.set(DimensionPhysicsData.getGravity(this.getLevel(), worldPos));
            serverSubLevel.logicalPose().orientation().transformInverse(this.gravityBody);
        }
    }

    public void randomNudge() {
        final Vec3 v = (VecHelper.offsetRandomly(new Vec3(0, 0, 0), this.level.random, 0.2f));
        this.angleVelocities.set(v.x, v.y, v.z);
        this.eulerAngles.set(0, 0, this.level.random.nextFloat() * Math.PI * 2);
    }

    void animateClientRotation(final SubLevel subLevel, final Pose3dc pose) {

        this.previousAngles.set(this.eulerAngles);

        final Vector3d shellVelocity = this.getShellVelocity(subLevel);

        final Vector3d acceleration = new Vector3d();
        if (this.updateVisualRotation) this.addGravityTorque(pose, acceleration);

        final Vec3 globalPosition = pose.transformPosition(Vec3.atCenterOf(this.getBlockPos()));
        this.compassTarget.update(globalPosition, this.level);
        final Vector3d target = new Vector3d();
        this.compassTarget.getTarget(target);

        this.addCompassTorque(pose, acceleration, target);
        if (this.compassTarget.isRandom())
            acceleration.z += (2 * this.level.random.nextFloat() - 1) * 2.1;

        acceleration.div(this.angleInertia);
        final Vector3d relativeVelocity = this.angleVelocities.add(shellVelocity, new Vector3d());
        final Vector3d currentDamping = relativeVelocity.mul(this.angleDamping);
        this.angleVelocities.add(acceleration).sub(currentDamping);

        final Vector3d totalVelocity = this.angleVelocities.add(shellVelocity, new Vector3d());
        this.eulerAngles.add(totalVelocity);


        this.collide(this.eulerAngles, totalVelocity, 1, Math.abs(Math.toRadians(this.axisBehaviour.primaryValue)));
        this.collide(this.eulerAngles, totalVelocity, 0, Math.abs(Math.toRadians(this.axisBehaviour.secondaryValue)));
        totalVelocity.sub(shellVelocity, this.angleVelocities);
    }

    void addGravityTorque(final Pose3dc pose, final Vector3d torque) {
        final Vector3dc globalPosition = Sable.HELPER.projectOutOfSubLevel(this.level, JOMLConversion.atCenterOf(this.getBlockPos()));
        final Vector3d localGravity = new Vector3d(DimensionPhysicsData.getGravity(this.level, globalPosition));
        this.transformBaseInverse(localGravity, pose);

        // transforms to be relative to the gimbal ring
        this.transformPrimaryInverse(localGravity);

        // the down direction of the compass, relative to the gimbal ring
        final Vector3d localDown = new Vector3d(0, -1, 0).rotateX(this.eulerAngles.y);
        final Vector3d localTorque = localDown.cross(localGravity);

        torque.x += localTorque.z;
        torque.y += localTorque.x;
    }

    void addCompassTorque(final Pose3dc pose, final Vector3d torque, final Vector3d target) {
        this.transformBaseInverse(target, pose);
        this.transformPrimaryInverse(target);//from base to gimbal ring
        this.transformSecondaryInverse(target);//from gimbal ring to compass
        this.transformCompassInverse(target);//from compass to needle

        final Vector3d localTorque = new Vector3d(0, 0, -1).cross(target);
        torque.z += localTorque.y;
    }

    private Vector3d getShellVelocity(final SubLevel subLevel) {
        final Vector3d shellVelocity = new Vector3d();
        if (subLevel != null) {
            final Pose3d pose = subLevel.logicalPose();

            if (this.lastShellOrientation == null) {
                this.lastShellOrientation = new Quaterniond(pose.orientation());
            } else {
                final Quaterniond rotationDiff = this.lastShellOrientation.div(pose.orientation(), new Quaterniond());
                final Vector3d angularVelocity = new Vector3d(rotationDiff.x, rotationDiff.y, rotationDiff.z).mul(2);
                this.transformBaseInverse(angularVelocity, pose);
                shellVelocity.x = angularVelocity.z;
                this.transformPrimaryInverse(angularVelocity);
                shellVelocity.y = angularVelocity.x;
                this.transformSecondaryInverse(angularVelocity);
                shellVelocity.z = angularVelocity.y;
                this.lastShellOrientation.set(pose.orientation());
            }
        } else
            this.lastShellOrientation = null;
        return shellVelocity;
    }

    private void collide(final Vector3d position, final Vector3d velocity, final int index, final double limit) {
        double p = position.get(index);
        double v = velocity.get(index);
        final double m = p > 0 ? 1 : -1;
        p *= m;
        v *= m;
        if (p >= limit) {
            p = limit;
            if (v > 0)
                v *= -0.9;
        }
        position.setComponent(index, p * m);
        velocity.setComponent(index, v * m);
    }

    private void setPower(final double angle, final Direction dir) {
        final boolean alongPrimary = (dir.getAxis() == this.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS));
        final float angleLimit = alongPrimary ? this.axisBehaviour.primaryValue : this.axisBehaviour.secondaryValue;

        final int newPower = angleLimit == 0 ? 0 : max(min((int) (14.5 * angle / Math.toRadians(angleLimit) + 0.5), 15), 0);
        if (this.redstoneMap.get(dir) != newPower) {
            this.redstoneMap.put(dir, newPower);
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            this.level.updateNeighborsAt(this.worldPosition.relative(dir), this.getBlockState().getBlock());
        }
    }

    public Quaternionf getBaseQuaternion() {
        final Quaternionf Q = new Quaternionf();
        final float angle = Direction.fromAxisAndDirection(this.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE).toYRot();
        Q.rotateY((float) Math.toRadians(angle));
        return Q;
    }

    public Quaternionf applyPrimaryQuaternion(final Quaternionf Q, final float partialTick) {
        Q.rotateZ(this.lerp((float) this.previousAngles.x, (float) this.eulerAngles.x, partialTick));
        return Q;
    }

    public Quaternionf applySecondaryQuaternion(final Quaternionf Q, final float partialTick) {
        Q.rotateX(this.lerp((float) this.previousAngles.y, (float) this.eulerAngles.y, partialTick));
        return Q;
    }

    public Quaternionf applyCompassQuaternion(final Quaternionf Q, final float partialTick) {
        Q.rotateY(this.lerp((float) this.previousAngles.z, (float) this.eulerAngles.z, partialTick));
        return Q;
    }

    private Vector3d transformBaseInverse(final Vector3d v, final Pose3dc ctx) {
        final float angle = Direction.fromAxisAndDirection(this.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE).toYRot();
        ctx.orientation().transformInverse(v);
        v.rotateY(-Math.toRadians(angle));
        return v;
    }

    private Vector3d transformPrimaryInverse(final Vector3d v) {
        v.rotateZ(-this.eulerAngles.x);
        return v;
    }

    private Vector3d transformSecondaryInverse(final Vector3d v) {
        v.rotateX(-this.eulerAngles.y);
        return v;
    }

    private Vector3d transformCompassInverse(final Vector3d v) {
        v.rotateY(-this.eulerAngles.z);
        return v;
    }

    float lerp(final float a, final float b, final float t) {
        return a * (1 - t) + b * t;
    }

    public int getPower(final Direction dir) {
        return this.redstoneMap.get(dir);
    }

    public double getZAngle() {
        return this.ZAngle;
    }

    public double getXAngle() {
        return this.XAngle;
    }

    public Vector3dc getAngularVelocityBody() {
        return this.angularVelocityBody;
    }

    public Vector3dc getGravityBody() {
        return this.gravityBody;
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        final CompoundTag powers = new CompoundTag();
        for (final Map.Entry<Direction, Integer> entry : this.redstoneMap.entrySet()) {
            powers.putInt(entry.getKey().getName(), entry.getValue());
        }
        if (!clientPacket) {
            tag.putFloat("Angle1", (float) this.eulerAngles.x);
            tag.putFloat("Angle2", (float) this.eulerAngles.y);
            tag.putFloat("Angle3", (float) this.eulerAngles.z);
            tag.putFloat("Vel1", (float) this.angleVelocities.x);
            tag.putFloat("Vel2", (float) this.angleVelocities.y);
            tag.putFloat("Vel3", (float) this.angleVelocities.z);
        }

        tag.putDouble("x_angle", this.XAngle);
        tag.putDouble("zz_angle", this.ZAngle);

        tag.put("Powers", powers);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        if (tag.contains("Powers")) {
            final CompoundTag powers = (CompoundTag) tag.get("Powers");
            for (final Map.Entry<Direction, Integer> entry : this.redstoneMap.entrySet()) {
                entry.setValue(powers.getInt(entry.getKey().getName()));
            }
        }
        if (!clientPacket) {
            float x = tag.getFloat("Angle1");
            float y = tag.getFloat("Angle2");
            float z = tag.getFloat("Angle3");
            this.eulerAngles = new Vector3d(x, y, z);
            x = tag.getFloat("Vel1");
            y = tag.getFloat("Vel2");
            z = tag.getFloat("Vel3");
            this.angleVelocities = new Vector3d(x, y, z);
        }

        this.XAngle = tag.getDouble("x_angle");
        this.ZAngle = tag.getDouble("z_angle");
    }

    public static class GimbalSensorScrollValueBehaviour extends ScrollValueBehaviour {
        protected Direction lastSide = Direction.NORTH;
        protected int primaryValue;
        protected int secondaryValue;
        protected Function<Integer, String> formatter;
        protected int min;
        protected int max;

        public GimbalSensorScrollValueBehaviour(final GimbalSensorBlockEntity be) {
            super(Component.translatable("create.kinetics.valve_handle.rotated_angle"), be, new GimbalSensorValueBox(be));
            this.withFormatter(this.formatter = v -> Math.abs(v) + Component.translatable("create.generic.unit.degrees")
                    .getString());
            this.primaryValue = 0;
            this.secondaryValue = 0;
        }

        public ScrollValueBehaviour between(final int min, final int max) {
            this.min = min;
            this.max = max;
            return super.between(min, max);
        }

        public boolean isPrimaryAxis() {
            final Direction.Axis blockAxis = this.blockEntity.getBlockState().getValue(GimbalSensorBlock.HORIZONTAL_AXIS);
            return this.lastSide.getAxis().isHorizontal() && this.lastSide.getAxis() != blockAxis;
        }

        @Override
        public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
            final ImmutableList<Component> rows = ImmutableList.of(Component.literal("\u27f3") // ⟳
                            .withStyle(ChatFormatting.BOLD),
                    Component.literal("\u27f2") // ⟲
                            .withStyle(ChatFormatting.BOLD));
            return new ValueSettingsBoard(this.label, 90, 15, rows, new ValueSettingsFormatter(this::formatValue));
        }

        @Override
        public void setValueSettings(final Player player, final ValueSettings valueSetting, final boolean ctrlHeld) {
            final int value = Math.max(0, valueSetting.value());
            if (!valueSetting.equals(this.getValueSettings()))
                this.playFeedbackSound(this);
            this.setValue(valueSetting.row() == 0 ? -value : value);
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(this.getValue() < 0 ? 0 : 1, Math.abs(this.getValue()));
        }

        public MutableComponent formatValue(final ValueSettings settings) {
            return SimLang.number(Math.max(0, Math.abs(settings.value())))
                    .add(Component.translatable(Create.ID + "." + "generic.unit.degrees"))
                    .component();
        }

        @Override
        public void write(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
            nbt.putInt("ScrollValue1", this.primaryValue);
            nbt.putInt("ScrollValue2", this.secondaryValue);
            super.write(nbt, registries, clientPacket);
        }

        @Override
        public void read(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
            this.primaryValue = nbt.getInt("ScrollValue1");
            this.secondaryValue = nbt.getInt("ScrollValue2");
            super.read(nbt, registries, clientPacket);
        }

        @Override
        public boolean writeToClipboard(HolderLookup.@NotNull Provider registries, CompoundTag tag, Direction side) {
            if(!acceptsValueSettings())
                return false;
            tag.putInt("ScrollValue1", this.primaryValue);
            tag.putInt("ScrollValue2", this.secondaryValue);
            return true;
        }

        @Override
        public boolean readFromClipboard(HolderLookup.@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
            if(!acceptsValueSettings()) return false;
            if(!tag.contains("ScrollValue1") || !tag.contains("ScrollValue2")) return true;
            if(simulate) return true;
            this.primaryValue = tag.getInt("ScrollValue1");
            this.secondaryValue = tag.getInt("ScrollValue2");
            blockEntity.setChanged();
            blockEntity.sendData();
            return true;
        }

        @Override
        public int getValue() {
            return this.isPrimaryAxis() ? this.primaryValue : this.secondaryValue;
        }

        public void setValue(int value) {
            value = Mth.clamp(value, this.min, this.max);
            if (value == this.getValue())
                return;
            if (this.isPrimaryAxis())
                this.primaryValue = value;
            else
                this.secondaryValue = value;
            //callback.accept(value);
            this.blockEntity.setChanged();
            this.blockEntity.sendData();
        }

        @Override
        public String formatValue() {
            return this.formatter.apply(this.getValue());
        }
    }

    public static class GimbalSensorValueBox extends ValueBoxTransform.Sided {
        GimbalSensorBlockEntity be;

        public GimbalSensorValueBox(final GimbalSensorBlockEntity be) {
            this.be = be;
        }

        public Sided fromSide(final Direction direction) {
            this.direction = direction;
            this.be.axisBehaviour.lastSide = direction;
            return this;
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            final boolean a = (direction.getAxis() == state.getValue(GimbalSensorBlock.HORIZONTAL_AXIS));
            return direction.getAxis().isHorizontal();// && (a == primaryAxis);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16);
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(level, pos, state);

            if (offset == null)
                return false;

            return localHit.distanceTo(offset) < this.scale / 1.5f;
        }

    }

    public static class DualGimbalSensorValueBox extends ValueBoxTransform.Dual {

        protected Direction direction = Direction.UP;

        public DualGimbalSensorValueBox(final boolean first) {
            super(first);
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState state) {
            Vec3 location = this.getSouthLocation();
            location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(this.getSide()), Direction.Axis.Y);
            location = VecHelper.rotateCentered(location, AngleHelper.verticalAngle(this.getSide()), Direction.Axis.X);
            return location;
        }


        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16);
        }

        @Override
        public void rotate(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(this.getSide()) + 180;
            final float xRot = this.getSide() == Direction.UP ? 90 : this.getSide() == Direction.DOWN ? 270 : 0;
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(xRot);
        }

        @Override
        public boolean shouldRender(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            return super.shouldRender(level, pos, state) && this.isSideActive(state, this.getSide());
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(level, pos, state);
            if (offset == null)
                return false;
            return localHit.distanceTo(offset) < this.scale / 1.5f;
        }

        protected boolean isSideActive(final BlockState state, final Direction direction) {
            final boolean a = (direction.getAxis() == state.getValue(GimbalSensorBlock.HORIZONTAL_AXIS));
            return direction.getAxis().isHorizontal() && (a == this.first);
        }

        public Direction getSide() {
            return Direction.NORTH;
        }
    }

    static class CompassTarget {
        private final Vector3d target = new Vector3d(0, 0, 0);
        private final Vector3d randomTarget = new Vector3d(0, 0, 0);
        private int randomTargetTimer = 0;
        private double randomTargetLength = 3;
        private boolean isRandom = false;

        public void update(final Vec3 pos, final Level level) {
            this.isRandom = !level.dimensionType().natural();
            if (!this.isRandom) {
                this.target.set(0, 0, -1);
            } else {
                final RandomSource r = level.random;
                if (this.randomTargetTimer-- < 0) {

                    final float radius = 1.0f;
                    this.randomTarget.set(
                            (r.nextFloat() - .5f) * 2 * radius,
                            (r.nextFloat() - .5f) * 2 * radius,
                            (r.nextFloat() - .5f) * 2 * radius);
                    this.randomTargetTimer = level.random.nextInt(5, 15);
                }
                final float nudge = 0.3f;
                this.randomTarget.add(
                        (r.nextFloat() - .5f) * 2 * nudge,
                        (r.nextFloat() - .5f) * 2 * nudge,
                        (r.nextFloat() - .5f) * 2 * nudge);
                this.randomTarget.normalize();
                final double step = 0.5;
                this.target.mul(1 - step).fma(step, this.randomTarget);
                this.target.normalize();
            }

        }

        public boolean isRandom() {
            return this.isRandom;
        }

        public void setRandomTargetLength(final double s) {
            this.randomTargetLength = s;
        }

        public Vector3d getTarget(final Vector3d v) {
            return this.target.mul(this.isRandom() ? this.randomTargetLength : 1, v);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
//        tooltip.add(Component.literal("hello"));
        Component x = SimLang.text("%.2f".formatted(Math.toDegrees(this.getXAngle())))
                .style(ChatFormatting.RED)
                .component();
        Component z = SimLang.text("%.2f".formatted(Math.toDegrees(this.getZAngle())))
                .style(ChatFormatting.BLUE)
                .component();

        SimLang.blockName(getBlockState())
                .forGoggles(tooltip, 1);
        SimLang.translate("gimbal_sensor.x_angle", x)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 2);
        SimLang.translate("gimbal_sensor.z_angle", z)
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 2);


        return true;
    }
}
