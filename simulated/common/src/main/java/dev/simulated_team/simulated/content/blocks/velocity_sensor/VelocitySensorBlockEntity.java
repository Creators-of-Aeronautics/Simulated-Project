package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.data.SimLang;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.lang.ref.WeakReference;
import java.util.List;

public class VelocitySensorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private float adjustedVelocity;

    private int signedRedstoneStrength;

    private Vector3dc currentNormal;

    private WeakReference<SubLevel> subLevelReference;

    private VelocitySensorScrollValueBehaviour maxSpeed;

    // fan rendering, clientside
    private final LerpedFloat fanSpeed = LerpedFloat.linear().chase(0, 0.5, LerpedFloat.Chaser.EXP);
    private float fanAngle = 0;
    private float oldFanAngle = 0;

    public VelocitySensorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.subLevelReference = new WeakReference<>(null);

        this.adjustedVelocity = 0;
        this.signedRedstoneStrength = 0;

        this.currentNormal = new Vector3d();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> list) {
        this.maxSpeed = new VelocitySensorScrollValueBehaviour(SimLang.translate("velocity_sensor.description").component(), this, new VelocitySensorValueBoxTransform());
        this.maxSpeed.between(1, 50);
        this.maxSpeed.value = 10;
        this.maxSpeed.withFormatter((value) -> value + " m/s");
        list.add(this.maxSpeed);
    }

    @Override
    public void initialize() {
        super.initialize();

        this.subLevelReference = new WeakReference<>(Sable.HELPER.getContaining(this.getLevel(), this.worldPosition));
    }

    @Override
    public void tick() {
        this.currentNormal = JOMLConversion.toJOML(Vec3.atLowerCornerOf(AbstractDirectionalAxisBlock.getDirectionOfAxis(this.getBlockState()).getNormal()));
        super.tick();

        final SubLevel subLevel = this.subLevelReference.get();
        final int redstoneStrengthBefore = this.signedRedstoneStrength;
        if (!this.level.isClientSide) {
            if (subLevel != null) {
                final float dot = (float) this.getGlobalVelocity().dot(subLevel.logicalPose().transformNormal(this.currentNormal, new Vector3d()));
                if (Math.abs(dot) > 0.05) {
                    this.adjustedVelocity = dot;
                } else {
                    this.adjustedVelocity = 0;
                }

                // int cast rounds towards 0
                this.signedRedstoneStrength = (int) Math.clamp((this.getAdjustedVelocity() / this.maxSpeed.getValue()) * 15f, -15, 15);
            } else { // sublevel is null, aka on stationary ground
                this.adjustedVelocity = 0;
                this.signedRedstoneStrength = 0;
            }

            if (redstoneStrengthBefore != this.signedRedstoneStrength) {
                final int power;
                if (this.signedRedstoneStrength == 0) {
                    power = 0;
                } else if (this.signedRedstoneStrength < 0) {
                    power = 1;
                } else {
                    power = 2;
                }

                this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(VelocitySensorBlock.POWERED, power));

                final Direction axisDir = AbstractDirectionalAxisBlock.getDirectionOfAxis(this.getBlockState());

                this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
                this.level.updateNeighborsAt(this.worldPosition.relative(axisDir), this.getBlockState().getBlock());
                this.level.updateNeighborsAt(this.worldPosition.relative(axisDir.getOpposite()), this.getBlockState().getBlock());
            }

            this.sendData();
        } else { // is clientside
            this.fanSpeed.updateChaseTarget(Mth.clamp(this.getAdjustedVelocity() / this.maxSpeed.getValue(), -1, 1));
            this.fanSpeed.tickChaser();
            this.oldFanAngle = this.fanAngle;
            this.fanAngle += this.fanSpeed.getValue();
        }
    }

    public ScrollValueBehaviour getMaxSpeed() {
        return this.maxSpeed;
    }

    public float getFanAngle(final float pt) {
        return Mth.lerp(pt, this.oldFanAngle, this.fanAngle);
    }

    private Vector3d getGlobalVelocity() {
        final SubLevel subLevel = this.subLevelReference.get();
        if (subLevel == null) {
            return new Vector3d();
        }

        final Vector3d jomlPos = JOMLConversion.toJOML(this.worldPosition.getCenter());
        return subLevel.logicalPose().transformPosition(jomlPos, new Vector3d()).sub(subLevel.lastPose().transformPosition(jomlPos, new Vector3d()), jomlPos).mul(20.0F);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putFloat("AdjustedVelocity", this.getAdjustedVelocity());
        tag.putInt("SignedRedstoneStrength", this.signedRedstoneStrength);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.adjustedVelocity = tag.getFloat("AdjustedVelocity");
        this.signedRedstoneStrength = Mth.clamp(tag.getInt("SignedRedstoneStrength"), -15, 15);
    }

    public Vector3dc getCurrentNormal() {
        return this.currentNormal;
    }

    public int getRedstoneStrength() {
        return Mth.abs(this.signedRedstoneStrength);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (this.subLevelReference.get() != null) {
            final float velocity = this.getAdjustedVelocity();
            final float displayVelocity = this.maxSpeed.isTowards() ? velocity : -velocity;
            SimLang.number(displayVelocity)
                    .text(" m/s").forGoggles(tooltip);
        }

        return IHaveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    /**
     * Positive is towards axis normal, negative is away
     */
    public float getAdjustedVelocity() {
        return this.adjustedVelocity;
    }

    private static class VelocitySensorValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.8);
        }

        @Override
        public float getScale() {
            return 0.35f;
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return AbstractDirectionalAxisBlock.getAxis(state) == direction.getAxis();
        }
    }

    public static class VelocitySensorScrollValueBehaviour extends ScrollValueBehaviour {
        private boolean towards;
        public VelocitySensorScrollValueBehaviour(final Component label, final SmartBlockEntity be, final ValueBoxTransform slot) {
            super(label, be, slot);
        }

        @Override
        public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
            final ImmutableList<Component> rows = ImmutableList.of(
                    SimLang.translate("velocity_sensor.selection.away").component(),
                    SimLang.translate("velocity_sensor.selection.towards").component()
            );
            return new ValueSettingsBoard(this.label, this.max, 10, rows,
                    new ValueSettingsFormatter(this::formatValue));
        }

        public MutableComponent formatValue(final ValueSettings settings) {
            return SimLang.number(settings.value()).component().append(" m/s");
        }

        @Override
        public void setValueSettings(final Player player, final ValueSettings valueSetting, final boolean ctrlDown) {
            super.setValueSettings(player, valueSetting, ctrlDown);
            this.towards = valueSetting.row() == 1;
        }

        @Override
        public int getValue() {
            return super.getValue() * (this.towards ? 1 : -1);
        }

        @Override
        public ValueSettings getValueSettings() {
            return new ValueSettings(this.towards ? 1 : 0, this.value);
        }

        public boolean isTowards() {
            return this.towards;
        }

        @Override
        public void read(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
            this.towards = nbt.getBoolean("ScrollValueTowards");
            super.read(nbt, registries, clientPacket);
        }

        @Override
        public void write(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
            nbt.putBoolean("ScrollValueTowards", this.towards);
            super.write(nbt, registries, clientPacket);
        }
    }
}
