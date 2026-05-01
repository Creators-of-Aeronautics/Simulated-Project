package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorInteractorBehaviour;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimLevelUtil;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LaserPointerBlockEntity extends AbstractLaserBlockEntity implements ClipboardCloneable {
    private ScrollValueBehaviour range;
    public LaserSensorInteractorBehaviour sensorInteraction;
    private boolean rainbow;
    public int laserColor;
    protected int bestPower;
    public Vec3 currentHitPos;

    public LaserPointerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.laserColor = SimColors.MEDIA_OURPLE;
        this.currentHitPos = Vec3.ZERO;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        final int rangeMax = SimConfigService.INSTANCE.server().blocks.laserPointerRange.get();
        this.range = new ScrollValueBehaviour(
                SimLang.translate("laser_pointer.max_length").component(), this, new RangeValueBoxTransform()
        ).between(1, rangeMax);
        this.range.value = rangeMax;

        this.sensorInteraction = new LaserSensorInteractorBehaviour(this, this::gatherStartAndEnd, this::getRaycastLength, this::getPower, this::matchesSensor);
        this.sensorInteraction.setShouldCast(this::shouldCast);

        behaviours.add(this.sensorInteraction);
        behaviours.add(this.range);
    }

    @Override
    public void tick() {
        if (this.level == null || !SimLevelUtil.isAreaActuallyLoaded(this.level, this.worldPosition, 2)) {
            return;
        }

        if (!this.level.isClientSide || this.isVirtual()) {
            final int currentPower = this.level.getBestNeighborSignal(this.worldPosition);
            if (currentPower != this.bestPower) {
                this.bestPower = currentPower;
                this.sendData();
            }
        }

        super.tick();
        if (!this.shouldCast()) {
            this.currentHitPos = Vec3.ZERO;
            return;
        }

        if (!this.isVirtual()) {
            final BlockHitResult context = this.sensorInteraction.getBlockHitResult();
            if (context.getType() != HitResult.Type.MISS) {
                this.currentHitPos = context.getLocation();
            } else {
                this.currentHitPos = Vec3.ZERO;
            }
        }
    }

    public boolean isAmethyst() {
        return this.laserColor == SimColors.MEDIA_OURPLE && !this.isRainbow();
    }

    public int getPower() {
        return this.getBlockState().getValue(LaserPointerBlock.INVERTED) ? 15 - this.bestPower : this.bestPower;
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.laserColor = tag.contains("LaserColor", Tag.TAG_ANY_NUMERIC) ? tag.getInt("LaserColor") : SimColors.MEDIA_OURPLE;
        this.bestPower = tag.getInt("BestPower");
        this.rainbow = tag.getBoolean("Rainbow");

        this.currentHitPos = this.readHitPos(tag);
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        tag.putInt("LaserColor", this.laserColor);
        tag.putInt("BestPower", this.bestPower);
        tag.putBoolean("Rainbow", this.isRainbow());

        this.writeHitPos(tag);
        super.write(tag, registries, clientPacket);
    }

    private void writeHitPos(final CompoundTag tag) {
        tag.put("HitPos", VecHelper.writeNBT(this.currentHitPos));
    }

    private Vec3 readHitPos(final CompoundTag tag) {
        Vec3 currentHit = Vec3.ZERO;

        if (tag.contains("HitPos")) {
            currentHit = VecHelper.readNBT(tag.getList("HitPos", Tag.TAG_COMPOUND));
        }

        return currentHit;
    }

    @Override
    public Direction getDirection() {
        return this.getBlockState().getValue(LaserPointerBlock.FACING);
    }

    @Override
    public float getRaycastLength() {
        return this.range.value;
    }

    @Override
    public boolean shouldCast() {
        return this.getPower() != 0;
    }

    public void setLaserColor(final int color) {
        this.laserColor = color;
        this.setChanged();
        this.sendData();
    }

    public int getLaserColor() {
        return this.laserColor;
    }

    public boolean matchesSensor(final LaserSensorBlockEntity sensor) {
        return sensor.filterColor(this.laserColor, this.rainbow);
    }

    @Override
    public String getClipboardKey() {
        return "LaserPointer";
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider registries, final CompoundTag tag, final Direction side) {
        tag.putInt("Color", this.laserColor);
        tag.putBoolean("Rainbow", this.isRainbow());
        return true;
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.@NotNull Provider registries, final CompoundTag tag, final Player player, final Direction side, final boolean simulate) {
        if(simulate) {
            return true;
        }
        this.setLaserColor(tag.getInt("Color"));
        this.setRainbow(tag.getBoolean("Rainbow"));
        return true;
    }

    public boolean isRainbow() {
        return this.rainbow;
    }

    public void setRainbow(final boolean rainbow) {
        this.rainbow = rainbow;

        if (!this.getLevel().isClientSide) {
            this.notifyUpdate();
        }
    }

    private static class RangeValueBoxTransform extends ValueBoxTransform.Sided {

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            return state.getValue(LaserPointerBlock.FACING).getOpposite() == direction;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
            super.rotate(level, pos, state, ms);
            final Direction facing = state.getValue(LaserPointerBlock.FACING);

            if (facing.getAxis() == Direction.Axis.Y)
                return;

            if (this.getSide() != Direction.UP)
                return;

            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }
}
