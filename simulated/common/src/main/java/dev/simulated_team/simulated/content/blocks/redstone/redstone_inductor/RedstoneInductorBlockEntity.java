package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RedstoneInductorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ClipboardCloneable {

    protected ScrollValueBehaviour inputDelay;
    int delayTicks;
    int outputSignal;
    LerpedFloat lerpedState;

    public RedstoneInductorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
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
        this.inputDelay = new RedstoneInductorValueBehaviour(CreateLang.translateDirect("logistics.redstone_interval"), this, new RedstoneInductorValueBoxTransform());
        this.inputDelay.between(0, 60 * 20 * 60);
        this.inputDelay.value = 10;
        this.inputDelay.withFormatter(this::format);
        this.inputDelay.withCallback(this::inputDelayChanged);
        behaviours.add(this.inputDelay);
    }

    private void inputDelayChanged(final Integer integer) {
        this.updateSignal();
    }

    public void updateSignal() {
        this.updateFacingBlock((RedstoneInductorBlock) this.getBlockState().getBlock(), this.getLevel());
        this.notifyUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null) {
            return;
        }

        final RedstoneInductorBlock block = (RedstoneInductorBlock) this.getBlockState().getBlock();

        final int backSignal = block.getBackSignal(this.level, this.worldPosition, this.getBlockState());
        int tempPower = this.outputSignal;

        final boolean powered = this.getBlockState().getValue(RedstoneInductorBlock.POWERED);

        if (!this.level.isClientSide) {
            if (tempPower == 0 && !powered) {
                this.delayTicks = 0;
                // setDischarge(false, level);
            }

            if (this.inputDelay.getValue() != 0 && this.delayTicks >= this.inputDelay.getValue()) {
                this.delayTicks = 0;

                if (tempPower > backSignal) tempPower--;
                if (tempPower < backSignal) tempPower++;

                // setDischarge(outputSignal > tempPower, level);

            } else if (this.inputDelay.getValue() == 0 && this.delayTicks > 2) {
                this.delayTicks = 0;
                tempPower = backSignal;
            }

            this.delayTicks++;
            if(this.outputSignal != tempPower) {
                this.outputSignal = tempPower;
                this.updateFacingBlock(block, this.level);
                this.sendData();
            }
        }

        if(this.level.isClientSide) this.lerpedState.tickChaser();
    }

    private void updateFacingBlock(final RedstoneInductorBlock block, final Level levelIn) {
        levelIn.updateNeighborsAt(this.worldPosition, block);
        levelIn.updateNeighborsAt(this.worldPosition.relative(this.getBlockState().getValue(RedstoneInductorBlock.FACING).getOpposite()), block);
    }

    private String format(final int value) {
        if (value == 0) {
            return Component.translatable("block.simulated.redstone_inductor." +
                    (this.getBlockState().getValue(RedstoneInductorBlock.INVERTED) ? "invert" : "copy")).getString();
        }
        if (value <= 60) {
            return value + "t";
        }
        if (value < 20 * 60) {
            return (value / 20) + "s";
        }
        return (value / 20 / 60) + "m";
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
            if (blockState.getValue(RedstoneInductorBlock.INVERTED) != tag.getBoolean("Inverted")) {
                this.level.setBlockAndUpdate(this.worldPosition, blockState.cycle(RedstoneInductorBlock.INVERTED));
            }

            return true;
        }
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider provider, final CompoundTag tag, final Direction direction) {
        tag.putBoolean("Inverted", this.getBlockState().getOptionalValue(RedstoneInductorBlock.INVERTED).orElse(false));
        return true;
    }

    private static class RedstoneInductorValueBoxTransform extends ValueBoxTransform {

        @Override
        public Vec3 getLocalOffset(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState) {
            return new Vec3(0.5, 5.5f / 16.0f, 0.5);
        }

        @Override
        public void rotate(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)) + 180;
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90);
        }

        @Override
        public float getScale() {
            return 0.5f;
        }
    }
}
