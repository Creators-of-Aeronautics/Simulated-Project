package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;


import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


public class ModulatingLinkedReceiverBlockEntity extends AbstractLinkedReceiverBlockEntity implements ClipboardCloneable {
    public static int RANGE_LIMIT = 256;

    public int minRange;
    public int maxRange;

    private double distanceToClosest = 0;
    private double oldDistanceToClosest = 0;
    private double clientDistance = 0;
    private double clientOldDistance = 0;

    public ModulatingLinkedReceiverBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.minRange = 8;
        this.maxRange = 64;
        this.setLazyTickRate(20);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (this.distanceToClosest != this.oldDistanceToClosest) {
            this.sendData();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide()) {
            if (this.clientDistance == 0 && this.distanceToClosest != 0) {
                this.clientDistance = this.distanceToClosest;
                this.clientOldDistance = this.distanceToClosest;
            } else {
                final double targetDelta = (this.distanceToClosest - this.clientDistance) / this.lazyTickRate;
                final double delta = Math.min(
                        Math.abs(this.distanceToClosest - this.clientDistance),
                        Math.abs(targetDelta));
                final double sign = Mth.sign(targetDelta);
                this.clientOldDistance = this.clientDistance;
                this.clientDistance += delta * sign;
            }
        }
    }

    public double getClientDistance(final float pt) {
        return Mth.lerp(pt, this.clientOldDistance, this.clientDistance);
    }

    @Override
    public void updateSignal() {
        this.oldDistanceToClosest = this.distanceToClosest;
        this.distanceToClosest = RANGE_LIMIT;
        super.updateSignal();
    }

    @Override
    public Tuple<Integer, Double> getSignalFromLink(final Vec3 relativePosition, final int transmittedStrength) {
        final double distance = relativePosition.length();

        if (this.distanceToClosest > distance) {
            this.distanceToClosest = distance;
        }

        if (distance > this.maxRange) {
            return new Tuple<>(0, 0.0);
        }

        if (this.minRange == this.maxRange) {
            return new Tuple<>(transmittedStrength, distance);
        } else {
            final double strengthScalar = Math.clamp((distance - this.maxRange) / (this.minRange - this.maxRange), 0, 1);
            return new Tuple<>((int) Math.ceil(strengthScalar * transmittedStrength), distance);
        }
    }

    @Override
    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        compound.putInt("MinRange", this.minRange);
        compound.putInt("MaxRange", this.maxRange);
        compound.putDouble("DistanceToClosest", this.distanceToClosest);

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.distanceToClosest = compound.getDouble("DistanceToClosest");

        if (!clientPacket ||
                !(Minecraft.getInstance().screen instanceof final ModulatingLinkedReceiverScreen screen) ||
                !screen.isThisBlock(this.getBlockPos())) {
            this.minRange = compound.getInt("MinRange");
            this.maxRange = compound.getInt("MaxRange");
        }

        super.read(compound, registries, clientPacket);
    }

    @Override
    public String getClipboardKey() {
        return "LinkRange";
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider provider, final CompoundTag tag, final Direction direction) {
        tag.putInt("minRange", this.minRange);
        tag.putInt("maxRange", this.maxRange);
        return true;
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.@NotNull Provider provider, final CompoundTag tag, final Player player, final Direction direction, final boolean simulate) {
        if (!tag.contains("minRange"))
            return false;
        if (simulate)
            return true;

        this.minRange = tag.getInt("minRange");
        this.maxRange = tag.getInt("maxRange");
        this.sendData();
        return true;
    }

    public double getDistanceToClosest() {
        return this.distanceToClosest;
    }
}
