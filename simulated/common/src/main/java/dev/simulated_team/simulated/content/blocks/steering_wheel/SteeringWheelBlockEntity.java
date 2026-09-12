package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
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
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SteeringWheelBlockEntity extends GeneratingKineticBlockEntity {
    public static final int RPM = 16;

    public boolean held = false;
    private int inUse = 0;

    public ScrollValueBehaviour angleInput;

    public float targetAngle = 0;
    public float targetAngleToUpdate = 0;
    private float angle = 0;

    private float clientAngle = 0;
    private float oldClientAngle = 0;

    private double sequencedAngleLimit = 0;

    // output shaft rotation
    float generatedSpeed = 0;
    // internal angle rotation
    float logicalSpeed = 0;

    public BlockState material = Blocks.SPRUCE_PLANKS.defaultBlockState();

    private static final MutableComponent ROTATE_TIP = SimLang.translate("gui.hold_tip.hold_to_rotate").component();

    public SteeringWheelBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.effects = new NoParticleKineticEffectHandler(this);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new HoldTipBehaviour(this, SteeringWheelBlockEntity::holdTipGetter));
        behaviours.add(this.angleInput = new SteeringWheelScrollValueBehaviour(this).between(1, 360));
        this.angleInput.value = 180;
    }

    public static MutableComponent holdTipGetter(final Player player, final BlockPos pos, final BlockState state) {
        if (SteeringWheelBlock.lookingAtWheel(player, pos, 1, state)) {
            return ROTATE_TIP;
        }
        return null;
    }

    public void startHolding() {
        this.held = true;
        this.notifyUpdate();
    }

    public void stopHolding() {
        this.held = false;
        this.notifyUpdate();
    }

    public float directionConvert(final float val) {
        return -KineticBlockEntity.convertToDirection(val, this.getBlockState().getValue(SteeringWheelBlock.FACING));
    }

    // a lot of this is black magic stolen from the torsion spring
    public void updateTargetAngle(float absoluteTarget) {
        absoluteTarget = Mth.clamp(absoluteTarget, -this.angleInput.getValue(), this.angleInput.getValue());

        if (this.targetAngle == absoluteTarget)
            return;

        this.targetAngle = absoluteTarget;
        final float relativeAngle = absoluteTarget - this.angle;

        if (Math.abs(relativeAngle) < 0.001 && this.inUse <= 0) {
            this.generatedSpeed = 0;
            this.updateGeneratedRotation();
            return;
        }

        final float rotationSpeed = RPM * Math.signum(relativeAngle);
        if (rotationSpeed == 0)
            return;

        final float relativeValue = relativeAngle / rotationSpeed;
        if (relativeValue <= 0 && this.inUse <= 0) {
            this.generatedSpeed = 0;
            this.updateGeneratedRotation();
            return;
        }

        final double degreesPerTick = KineticBlockEntity.convertToAngular(rotationSpeed);
        this.inUse = (int) Math.ceil(relativeAngle / degreesPerTick) + 2;

        this.sequenceContext = new SequencedGearshiftBlockEntity.SequenceContext(SequencerInstructions.TURN_ANGLE, relativeValue);
        this.sequencedAngleLimit = Math.abs(relativeAngle);

        this.logicalSpeed = rotationSpeed;
        final Direction facing = this.getBlockState().getValue(SteeringWheelBlock.FACING);
        final boolean floor = this.getBlockState().getValue(SteeringWheelBlock.ON_FLOOR);
        if ((facing == Direction.NORTH || facing == Direction.WEST) == floor) {
            this.generatedSpeed = -this.logicalSpeed;
        } else {
            this.generatedSpeed = this.logicalSpeed;
        }

        this.updateGeneratedRotation();
    }

    @Override
    protected void copySequenceContextFrom(final KineticBlockEntity sourceBE) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide) {
            this.oldClientAngle = this.clientAngle;
            if (SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(this.getBlockPos())) {
                this.clientAngle = this.targetAngleToUpdate;
            } else {
                this.clientAngle = this.clientAngle + (this.targetAngleToUpdate - this.clientAngle) * 0.25f;
            }
        }

        if (this.getGeneratedSpeed() != 0) {
            this.integrateAngle();
        }

        if (this.inUse > 0) {
            this.inUse--;

            if (this.inUse == 0 && !this.level.isClientSide) {
                this.sequenceContext = null;
                this.generatedSpeed = 0;
                this.updateGeneratedRotation();
            }
        } else if(!this.level.isClientSide) {
            this.updateTargetAngle(this.targetAngleToUpdate);
        }
    }

    /**
     * linearly updates angle, limited in change by sequencedAngleLimit
     */
    private void integrateAngle() {
        float angularSpeed = this.getAngularSpeed();

        if (this.sequencedAngleLimit >= 0) {
            angularSpeed = (float) Mth.clamp(angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
            this.sequencedAngleLimit = Math.max(0, this.sequencedAngleLimit - Math.abs(angularSpeed));
        }

        this.angle = this.angle + angularSpeed;
    }

    public float getAngle() {
        return this.angle;
    }

    public float getAngularSpeed() {
        float speed = convertToAngular(this.getLogicalSpeed());
        if (this.getSpeed() == 0 || this.getLogicalSpeed() == 0) {
            speed = 0;
        }

        return speed;
    }

    public float getLogicalSpeed() {
        return this.inUse == 0 ? 0 : this.logicalSpeed;
    }

    @Override
    public float getGeneratedSpeed() {
        return this.inUse == 0 ? 0 : this.generatedSpeed;
    }

    @Override
    protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        compound.putFloat("Angle", this.angle);
        compound.putFloat("TargetAngle", this.targetAngle);
        if (this.targetAngleToUpdate != this.targetAngle) {
            compound.putFloat("TargetAngleToUpdate", this.targetAngleToUpdate);
        }

        if (clientPacket) {
            compound.putBoolean("Held", this.held);
        }

        compound.put("Material", NbtUtils.writeBlockState(this.material));
    }

    @Override
    public void writeSafe(final CompoundTag compound, final HolderLookup.Provider registries) {
        super.writeSafe(compound, registries);
        compound.put("Material", NbtUtils.writeBlockState(this.material));
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        // todo pr create to validate this for all scroll value behaviours
        this.angleInput.value = Mth.clamp(this.angleInput.value, 1, 360);

        this.angle = Math.clamp(compound.getFloat("Angle"), -360, 360);
        if (clientPacket) {
            this.held = compound.getBoolean("Held");
        }

        if (!clientPacket || !SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(this.getBlockPos())) {
            this.updateTargetAngle(Math.clamp(compound.getFloat("TargetAngle"), -360, 360));
            if (compound.contains("TargetAngleToUpdate")) {
                this.targetAngleToUpdate = Math.clamp(compound.getFloat("TargetAngleToUpdate"), -360, 360);
            } else {
                this.targetAngleToUpdate = this.targetAngle;
            }
        }

        final BlockState prevMaterial = this.material;
        if (!compound.contains("Material"))
            return;

        this.material = NbtUtils.readBlockState(this.blockHolderGetter(), compound.getCompound("Material"));
        if (this.material.isAir())
            this.material = Blocks.SPRUCE_PLANKS.defaultBlockState();

        if (clientPacket && prevMaterial != this.material)
            this.redraw();
    }

    public boolean shouldRenderShaft() {
        return true;
    }

    @Override
    protected Block getStressConfigKey() {
        return SimBlocks.STEERING_WHEEL.get();
    }

    public float getRenderAngle(final float partialTicks) {
        // todo add ponder angle support please :3
        final float renderAngle = Mth.lerp(partialTicks, this.oldClientAngle, this.clientAngle);
        final Direction facing = this.getBlockState().getValue(SteeringWheelBlock.FACING);
        if (facing == Direction.NORTH || facing == Direction.WEST) {
            return (float) Math.toRadians(-renderAngle);
        } else {
            return (float) Math.toRadians(renderAngle);
        }
    }

    public float getInteractionAngle(final float partialTicks) {
        return Mth.lerp(partialTicks, this.oldClientAngle, this.clientAngle);
    }

    @Override
    public AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(0.4);
    }

    // 0 or -1 at center
    // 4 or -5 at edges
    private int lastPlayedIncrement = 0;

    @Override
    public void tickAudio() {
        super.tickAudio();
        float renderAngle = this.getRenderAngle(0);
        // angle rarely caps at exactly 180

        if (Math.abs(Math.abs(this.angle) - this.angleInput.getValue()) < 0.01) {
            renderAngle += (float) (Math.signum(this.angle) * 0.01);
        }

        final int playingIncrement = (int) Math.floor(Math.toDegrees(renderAngle) / 45);

        if (this.lastPlayedIncrement != playingIncrement) {
            int spokeCrossed = playingIncrement;
            if (this.lastPlayedIncrement - playingIncrement > 0) { // make - travel over a spoke match + travel
                spokeCrossed++;
            }
            // if not travelling away from edge spoke
            if (spokeCrossed != Math.signum(this.lastPlayedIncrement - playingIncrement) * 4) {
                switch (spokeCrossed) {
                    case -4, 4 -> {
                        AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, 1.25f, 0.85f, true);
                    }

                    case 0 -> {
                        AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, 1.25f, 0.5f, true);
                    }

                    default -> {
                        AllSoundEvents.CRANKING.playAt(this.level, this.worldPosition, 0.5f, 1.25f, true);
                    }
                }
            }
            this.lastPlayedIncrement = playingIncrement;
        }
    }

    private void redraw() {
        if (!this.isVirtual())
            this.requestModelDataUpdate();
        if (this.hasLevel()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
            this.level.getChunkSource()
                    .getLightEngine()
                    .checkBlock(this.worldPosition);
        }
    }

    public boolean isMaterialValid(final ItemStack stack) {
        if (!(stack.getItem()instanceof final BlockItem blockItem))
            return false;
        final BlockState material = blockItem.getBlock()
                .defaultBlockState();
        if (material == this.material)
            return false;
        return material.is(BlockTags.PLANKS);
    }

    public ItemInteractionResult applyMaterialIfValid(final ItemStack stack) {
        if (this.isMaterialValid(stack) && (stack.getItem()instanceof final BlockItem blockItem)) {
            if (this.level.isClientSide() && !this.isVirtual())
                return ItemInteractionResult.SUCCESS;
            this.material = blockItem.getBlock().defaultBlockState();;
            this.notifyUpdate();
            this.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, this.worldPosition, Block.getId(material));
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static class SteeringWheelValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return direction == (state.getValue(SteeringWheelBlock.ON_FLOOR) ? Direction.UP : Direction.DOWN);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
            super.rotate(level, pos, state, ms);
            final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }

    private static class SteeringWheelScrollValueBehaviour extends ScrollValueBehaviour {
        public SteeringWheelScrollValueBehaviour(final SmartBlockEntity be) {
            super(SimLang.translate("torsion_spring.angle_limit").component(), be, new SteeringWheelValueBoxTransform());
            this.withFormatter(v -> Math.abs(v) + CreateLang.translateDirect("generic.unit.degrees")
                    .getString());
        }

        @Override
        public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hitResult) {
            return new ValueSettingsBoard(this.label, 360, 45, ImmutableList.of(Component.literal("\u27f3").withStyle(ChatFormatting.BOLD)), new ValueSettingsFormatter(this::formatValue));
        }

        public MutableComponent formatValue(final ValueSettings settings) {
            return SimLang.number(Math.max(1, Math.abs(settings.value())))
                    .add(CreateLang.translateDirect("generic.unit.degrees"))
                    .component();
        }
    }
}
