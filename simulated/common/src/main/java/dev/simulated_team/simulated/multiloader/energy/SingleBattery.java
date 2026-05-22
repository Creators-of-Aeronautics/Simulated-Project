package dev.simulated_team.simulated.multiloader.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class SingleBattery {
    public final int maxEnergy;
    protected int throughput;
    protected int energy = 0;

    public SingleBattery(final int maxEnergy, final int throughput) {
        this.maxEnergy = maxEnergy;
        this.throughput = throughput;
    }

    public int getEnergy() {
        return this.energy;
    }

    public void setEnergy(final int v) {
        this.energy = v;
    }

    public int getThroughput() {
        return this.throughput;
    }

    public void setThroughput(final int v) {
        this.throughput = v;
    }

    public int receiveEnergy(final int toReceive, final boolean simulate) {
        final int diff = Math.min(Math.min(toReceive, this.maxEnergy - this.energy), this.throughput);
        if (!simulate) {
            this.energy += diff;
        }
        return diff;
    }

    public int extractEnergy(final int toExtract, final boolean simulate) {
        final int diff = Math.min(Math.min(toExtract, this.energy), this.throughput);
        if (!simulate) {
            this.energy -= diff;
        }
        return diff;
    }

    public boolean canExtract() {
        return true;
    }

    public boolean canReceive() {
        return true;
    }

    public void read(final CompoundTag tag) {
        this.energy = Mth.clamp(tag.getInt("Energy"), 0, this.maxEnergy);
    }

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", this.energy);
        return tag;
    }
}
