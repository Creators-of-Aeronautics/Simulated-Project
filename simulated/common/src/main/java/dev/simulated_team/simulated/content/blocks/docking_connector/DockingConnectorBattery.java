package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.energy.SingleBattery;

public class DockingConnectorBattery extends SingleBattery {
    private DockingConnectorBattery other = null;

    public DockingConnectorBattery(final int maxEnergy, final int throughput) {
        super(maxEnergy, throughput);
    }

    public void connect(final DockingConnectorBattery other) {
        this.other = other;
    }

    public void disconnect() {
        this.other = null;
    }

    protected int superReceiveEnergy(final int toReceive, final boolean simulate) {
        return super.receiveEnergy(toReceive, simulate);
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        if (this.other != null) {
            return this.other.superReceiveEnergy(toReceive, simulate);
        }
        return 0;
    }

    @Override
    public boolean canReceive() {
        return this.other != null;
    }
}
