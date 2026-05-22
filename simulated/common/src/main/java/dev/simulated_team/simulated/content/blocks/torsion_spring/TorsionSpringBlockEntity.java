package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TorsionSpringBlockEntity extends KineticBlockEntity implements ExtraKinetics {
    private final Output springOutput;
    public ScrollValueBehaviour angleInput;
    protected double sequencedAngleLimit;

    public TorsionSpringBlockEntity(final BlockEntityType<?> blockEntityType, final BlockPos blockPos, final BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.springOutput = new Output(blockEntityType, new ExtraBlockPos(blockPos), blockState, this);
        this.sequencedAngleLimit = -1;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    public boolean isSpringStatic() {
        return this.springOutput.angle == this.springOutput.oldAngle;
    }

    public float interpolatedSpring(final float pt) {
        return (float) (this.springOutput.oldAngle + (this.springOutput.angle - this.springOutput.oldAngle) * pt);
    }

    public float getAngle() {
        return (float) this.springOutput.angle;
    }

    public void setAngle(final float angle) {
        this.springOutput.angle = angle;
    }

    public void onSignalChanged() {

    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(this.angleInput = new TorsionSpringScrollValueBehaviour(this).between(1, 360));
        this.angleInput.onlyActiveWhen(this::showValue);
        this.angleInput.setValue(90);
    }

    public boolean showValue() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.springOutput.tick();
    }

    @Override
    public void onSpeedChanged(final float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        this.sequencedAngleLimit = -1;

        if (this.sequenceContext != null && this.sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE)
            this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue(this.getTheoreticalSpeed());

        this.springOutput.updateParentSpeed(previousSpeed, this.getSpeed());
    }

    @Override
    protected void copySequenceContextFrom(final KineticBlockEntity sourceBE) {
        super.copySequenceContextFrom(sourceBE);
    }

    @Override
    public float calculateAddedStressCapacity() {
        return 0;
    }

    @Override
    public String getExtraKineticsSaveName() {
        return "TorsionSpringOutput";
    }

    @Override
    public KineticBlockEntity getExtraKinetics() {
        return this.springOutput;
    }

    @Override
    public boolean shouldConnectExtraKinetics() {
        return false;
    }

    @Override
    protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        if (this.sequencedAngleLimit >= 0)
            compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1;
    }

    public static class Output extends GeneratingKineticBlockEntity implements ExtraKineticsBlockEntity {
        public static final IRotate CONFIG = new IRotate() {
            @Override
            public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
                return face == state.getValue(TorsionSpringBlock.FACING);
            }

            @Override
            public Direction.Axis getRotationAxis(final BlockState state) {
                return state.getValue(TorsionSpringBlock.FACING).getAxis();
            }
        };

        private final TorsionSpringBlockEntity parent;

        protected double oldAngle = 0.0f;
        protected double angle = 0.0f;

        private int rotationDurationTicks = 0; // duration of the currently active context.
        private int rotationProgressTicks = 0; // progress of our movement, always less than context duration
        private double sequencedAngleLimit = -1;

        /**
         * The previous, non-zero spring input speed
         */
        private float lastSpringSpeed = 0;

        private float generatedSpeed;
        private double targetAngle = 0;
        private State currentState = State.STOPPED;
        private float queuedSpeed;
        private int customValidationCountdown;

        public Output(final BlockEntityType<?> type, final ExtraBlockPos pos, final BlockState state, final TorsionSpringBlockEntity parentBlockEntity) {
            super(type, pos, state);
            this.parent = parentBlockEntity;
        }

        @Override
        public Component getKey() {
            return SimLang.translate("extra_kinetics.torsion_output").component();
        }

        @Override
        public void initialize() {
            super.initialize();
            this.reActivateSource = true;
            this.updateSpeed = true;
        }

        @Override
        public void tick() {
            // The built in kinetic BE validation will cook us (when our speed is not zero, but our generated speed is 0
            // on the tick we are stopping rotation)
            // Let's run a custom one instead
            ((KineticBlockEntityExtension) this).simulated$setValidationCountdown(Integer.MAX_VALUE);

            if (this.customValidationCountdown-- <= 0) {
                this.customValidationCountdown = AllConfigs.server().kinetics.kineticValidationFrequency.get();
                this.customValidateKinetics();
            }

            this.generatedSpeed = this.queuedSpeed;

            super.tick();

            this.oldAngle = this.angle;
            if (this.rotationDurationTicks >= 0 && this.rotationProgressTicks <= this.rotationDurationTicks) {
                this.rotationProgressTicks++;

                float angularSpeed = KineticBlockEntity.convertToAngular(this.speed);

                if (this.sequencedAngleLimit >= 0)
                    angularSpeed = (float) Mth.clamp(angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);

                if (this.sequencedAngleLimit >= 0)
                    this.sequencedAngleLimit = Math.max(0, this.sequencedAngleLimit - Math.abs(angularSpeed));

                this.angle += angularSpeed;

                this.level.updateNeighborsAt(this.getBlockPos(), this.getParentBlockEntity().getBlockState().getBlock());

                if (this.rotationProgressTicks == this.rotationDurationTicks) {
                    this.sequenceContext = null;
                    this.rotationProgressTicks = -1;
                    this.rotationDurationTicks = -1;
                    this.queuedSpeed = 0;

                    this.reActivateSource = true;
                    this.updateSpeed = true;
                    this.currentState = State.STOPPED;
                }
            }

            final boolean powered = this.getBlockState().getValue(TorsionSpringBlock.POWERED);
            final boolean parentStopped = this.parent.getSpeed() == 0;

            if (this.currentState == State.TURNING && parentStopped) {
                if (this.targetAngle != 0 || powered)
                    this.stopTurning();
            } else if (this.currentState == State.STOPPED && parentStopped && !powered) {
                if (this.targetAngle != 0.0) { // return back to 0 angle when not powered
                    this.beginTurnTo(0.0);
                    SimAdvancements.REWIND_TIME.awardToNearby(this.parent.getBlockPos(), this.parent.getLevel());
                }
            } else if (this.currentState == State.TURNING) {
                // if the parent speed changes direction, stop turning so next tick
                final double targetAngle = this.parent.angleInput.getValue() * Math.signum(this.parent.getSpeed());

                if (this.targetAngle != targetAngle || this.lastSpringSpeed != this.generatedSpeed) {
                    this.stopTurning();
                }
            } else if (!parentStopped && this.currentState == State.STOPPED) {
                // start rotating if the parent is, and we're stopped
                final double targetAngle = this.parent.angleInput.getValue() * Math.signum(this.parent.getSpeed());
                this.beginTurnTo(targetAngle);
            }
        }

        private void customValidateKinetics() {
            if (this.hasSource()) {
                if (!this.hasNetwork()) {
                    this.removeSource();
                    return;
                }

                if (!this.level.isLoaded(this.source))
                    return;

                BlockEntity blockEntity = this.level.getBlockEntity(this.source);

                if (blockEntity instanceof final ExtraKinetics ek && ((KineticBlockEntityExtension) this).simulated$getConnectedToExtraKinetics()) {
                    blockEntity = ek.getExtraKinetics();
                }

                final KineticBlockEntity sourceBE =
                        blockEntity instanceof KineticBlockEntity ? (KineticBlockEntity) blockEntity : null;
                if (sourceBE == null || sourceBE.getTheoreticalSpeed() == 0) {
                    this.removeSource();
                    this.detachKinetics();
                }
            }
        }

        private void updateParentSpeed(final float previousSpeed, final float newParentSpeed) {
            if (newParentSpeed != 0) {
                this.lastSpringSpeed = newParentSpeed;
            } else if (previousSpeed != 0) {
                this.lastSpringSpeed = previousSpeed;
            }
        }

        private void stopTurning() {
            this.sequenceContext = null;
            this.rotationProgressTicks = -1;
            this.rotationDurationTicks = -1;
            this.sequencedAngleLimit = -1;
            this.targetAngle = Double.MAX_VALUE;

            this.reActivateSource = true;
            this.updateSpeed = true;
            this.queuedSpeed = 0;

            this.currentState = State.STOPPED;
        }

        private void beginTurnTo(final double targetAngle) {
            double relativeAngle = targetAngle - this.angle;

            if (relativeAngle == 0) return;
            if (this.currentState == State.TURNING && this.targetAngle == targetAngle) return;

            this.lastSpringSpeed = (float) (Math.abs(this.lastSpringSpeed) * Math.signum(relativeAngle));

            if (this.parent.sequencedAngleLimit >= 0) {
                relativeAngle = (float) Mth.clamp(relativeAngle, -this.parent.sequencedAngleLimit, this.parent.sequencedAngleLimit);
            }

            this.detachKinetics();
            this.targetAngle = targetAngle;
            this.sequenceContext = new SequencedGearshiftBlockEntity.SequenceContext(SequencerInstructions.TURN_ANGLE, relativeAngle / this.lastSpringSpeed);

            final double degreesPerTick = KineticBlockEntity.convertToAngular(Math.abs(this.lastSpringSpeed));
            this.rotationDurationTicks = (int) Math.ceil(Math.abs(relativeAngle) / degreesPerTick) + 2;
            this.rotationProgressTicks = 0;
            this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue(this.lastSpringSpeed);
            this.currentState = State.TURNING;
            this.queuedSpeed = this.lastSpringSpeed;
            this.generatedSpeed = this.queuedSpeed;

            this.reActivateSource = true;
            this.updateSpeed = true;
        }

        @Override
        public float getGeneratedSpeed() {
            return this.generatedSpeed;
        }

        @Override
        public float calculateStressApplied() {
            return 0;
        }

        @Override
        protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
            super.write(compound, registries, clientPacket);
            compound.putDouble("OldAngle", this.oldAngle);
            compound.putDouble("Angle", this.angle);
            compound.putDouble("TargetAngle", this.targetAngle);
            compound.putFloat("LastSpringSpeed", this.lastSpringSpeed);
            compound.putInt("CurrentState", this.currentState.ordinal());
            compound.putInt("RotationProgressTicks", this.rotationProgressTicks);
            compound.putInt("RotationDurationTicks", this.rotationDurationTicks);
            compound.putFloat("GeneratedSpeed", this.generatedSpeed);
            compound.putFloat("QueuedSpeed", this.queuedSpeed);

            if (this.sequencedAngleLimit >= 0)
                compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
        }

        @Override
        protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
            super.read(compound, registries, clientPacket);
            this.oldAngle = compound.getDouble("OldAngle");
            this.angle = compound.getDouble("Angle");
            this.targetAngle = compound.getDouble("TargetAngle");
            this.lastSpringSpeed = compound.getFloat("LastSpringSpeed");
            this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1;
            this.rotationProgressTicks = compound.getInt("RotationProgressTicks");
            this.rotationDurationTicks = compound.getInt("RotationDurationTicks");
            this.generatedSpeed = compound.getFloat("GeneratedSpeed");
            this.queuedSpeed = compound.getFloat("QueuedSpeed");

            if (compound.contains("CurrentState"))
                this.currentState = State.values()[compound.getInt("CurrentState")];
        }

        @Override
        public KineticBlockEntity getParentBlockEntity() {
            return this.parent;
        }

        private enum State {
            STOPPED,
            TURNING
        }
    }

    public static class TorsionSpringScrollValueBehaviour extends ScrollValueBehaviour {

        public TorsionSpringScrollValueBehaviour(final SmartBlockEntity be) {
            super(SimLang.translate("torsion_spring.angle_limit").component(), be, new TorsionSpringValueBox());
            this.withFormatter(v -> Math.max(1, v) + CreateLang.translateDirect("generic.unit.degrees")
                    .getString());
        }

        @Override
        public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
            return new ValueSettingsBoard(this.label, 360, 45, ImmutableList.of(Component.literal("\u27f3").withStyle(ChatFormatting.BOLD)), new ValueSettingsFormatter(this::formatValue));
        }

        public MutableComponent formatValue(final ValueSettings settings) {
            return SimLang.number(Math.max(1, settings.value()))
                    .add(CreateLang.translateDirect("generic.unit.degrees"))
                    .component();
        }
    }

    public static class TorsionSpringValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            return super.getLocalOffset(level, pos, state)
                    .add(Vec3.atLowerCornerOf(state.getValue(TorsionSpringBlock.FACING).getNormal()).scale(-5 / 16f));
        }

        @Override
        public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
            if (!this.getSide().getAxis().isHorizontal()) {
                TransformStack.of(ms)
                        .rotateY((AngleHelper.horizontalAngle(state.getValue(TorsionSpringBlock.FACING)) + 180) * (float) Math.PI / 180);
            }
            super.rotate(level, pos, state, ms);
        }

        @Override
        public boolean testHit(final LevelAccessor level, final BlockPos pos, final BlockState state, final Vec3 localHit) {
            final Vec3 offset = this.getLocalOffset(level, pos, state);
            if (offset == null) {
                return false;
            }
            return localHit.distanceTo(offset) < this.scale / 1.5f;
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return direction.getAxis() != state.getValue(TorsionSpringBlock.FACING).getAxis();
        }
    }
}
