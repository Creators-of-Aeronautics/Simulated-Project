package dev.simulated_team.simulated.multiloader.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class SingleBatteryWrapper implements IEnergyStorage {
    private final SingleBattery battery;
    public SingleBatteryWrapper(final SingleBattery battery) {
        this.battery = battery;
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        return this.battery.receiveEnergy(toReceive, simulate);
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        return this.battery.extractEnergy(toExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return this.battery.getEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return this.battery.maxEnergy;
    }

    @Override
    public boolean canExtract() {
        return this.battery.canExtract();
    }

    @Override
    public boolean canReceive() {
        return this.battery.canReceive();
    }
}
