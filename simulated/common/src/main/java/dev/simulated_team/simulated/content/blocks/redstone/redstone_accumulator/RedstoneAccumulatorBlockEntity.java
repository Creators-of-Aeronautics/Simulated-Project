package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.diodes.BrassDiodeScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class RedstoneAccumulatorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ClipboardCloneable {
    protected ScrollValueBehaviour inputDelay;
    protected int delayTicks;
    protected int outputSignal;
    protected LerpedFloat lerpedState;

    public RedstoneAccumulatorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.lerpedState = LerpedFloat.linear();
        this.delayTicks = 0;
        this.outputSignal = 0;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.updateSignal();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        this.inputDelay = new BrassDiodeScrollValueBehaviour(
                Component.translatable("block.simulated.redstone_accumulator.input_delay"),
                this, new RedstoneAccumulatorValueBoxTransform());
        this.inputDelay.between(2, 60 * 20 * 60);
        this.inputDelay.value = 10;
        this.inputDelay.withFormatter(this::format);
        this.inputDelay.withCallback(this::inputDelayChanged);
        behaviours.add(this.inputDelay);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null) return;

        //check behind power and update block accordingly
        final Direction facing = this.getBlockState().getValue(RedstoneAccumulatorBlock.FACING);
        final boolean backSignal = this.getBlockState().getValue(RedstoneAccumulatorBlock.POWERED);
        final boolean sideSignal = this.getBlockState().getValue(RedstoneAccumulatorBlock.SIDE_POWERED);

        if(backSignal && sideSignal) return;
        if(!backSignal && !sideSignal) this.delayTicks = 0;

        int tempSignal = this.outputSignal;
        if (this.delayTicks == this.inputDelay.value) {
            if (backSignal) {
                tempSignal++;
                this.delayTicks = 0;
            } else if (sideSignal) {
                tempSignal--;
                this.delayTicks = 0;
            }

            if (tempSignal != this.outputSignal) this.setOutputSignal(tempSignal);
        } else {
            this.delayTicks = Math.min(this.delayTicks + 1 , this.inputDelay.value);
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            this.level.updateNeighborsAt(this.worldPosition.relative(facing), this.getBlockState().getBlock());
        }
        if(this.level.isClientSide) this.lerpedState.tickChaser();
        this.lerpedState.chase(this.outputSignal, 0.4, LerpedFloat.Chaser.EXP);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.outputSignal = tag.getInt("OutputSignal");
        this.delayTicks = tag.getInt("DelayTicks");
        this.lerpedState.chase(this.outputSignal, 0.4, LerpedFloat.Chaser.EXP);
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        tag.putInt("OutputSignal", this.outputSignal);
        tag.putInt("DelayTicks", this.delayTicks);
        super.write(tag, registries, clientPacket);
    }

    private void inputDelayChanged(final Integer integer) {
        this.sendData();
    }

    public void updateSignal() {
        this.sendData();
    }

    public void setOutputSignal(final int output){
        boolean update = output != this.outputSignal;
        this.outputSignal = Mth.clamp(output, 0, 15);
        if (update) {
            this.updateFacingBlock(this.getBlockState().getBlock(), this.level);
        }
    }

    private void updateFacingBlock(final Block block, final Level levelIn) {
        levelIn.updateNeighborsAt(this.worldPosition, block);
        levelIn.updateNeighborsAt(this.worldPosition.relative(this.getBlockState().getValue(RedstoneAccumulatorBlock.FACING).getOpposite()), block);
    }

    private int step(final ScrollValueBehaviour.StepContext context) {
        int value = context.currentValue;
        if (!context.forward)
            value--;

        if (value < 20)
            return 1;
        if (value < 20 * 60)
            return 20;
        return 20 * 60;
    }

    private String format(final int value) {
        if (value < 20)
            return value + "t";
        if (value < 20 * 60)
            return (value / 20) + "s";
        return (value / 20 / 60) + "m";
    }

    @Override
    public String getClipboardKey() {
        return "Block";
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.@NotNull Provider provider, final CompoundTag tag, final Player player, final Direction direction, final boolean simulate) {
        if (!tag.contains("Inverted")) {
            return false;
        } else if (simulate) {
            return true;
        } else {
            final BlockState blockState = this.getBlockState();
            if (blockState.getValue(RedstoneAccumulatorBlock.INVERTED) != tag.getBoolean("Inverted")) {
                this.level.setBlockAndUpdate(this.worldPosition, blockState.cycle(RedstoneAccumulatorBlock.INVERTED));
            }

            return true;
        }
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider provider, final CompoundTag tag, final Direction direction) {
        tag.putBoolean("Inverted", this.getBlockState().getOptionalValue(RedstoneAccumulatorBlock.INVERTED).orElse(false));
        return true;
    }

    private static class RedstoneAccumulatorValueBoxTransform extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState) {
            return new Vec3(0.5, 6.6f / 16.0f, 0.5);
        }

        @Override
        public void rotate(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(blockState.getValue(RedstoneAccumulatorBlock.FACING)) + 180;
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }
    }
}
