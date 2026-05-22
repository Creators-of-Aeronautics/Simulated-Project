package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.multiloader.inventory.SingleSlotContainer;

public class DockingConnectorSoloInventory extends SingleSlotContainer {
    private boolean allowsInsertion = false;
    public DockingConnectorSoloInventory() {
        super(64);
    }

    public void dock() {
        this.allowsInsertion = true;
    }

    public void unDock() {
        this.allowsInsertion = false;
    }

    @Override
    public boolean canInsertItem(final ItemInfoWrapper info) {
        return this.allowsInsertion;
    }
}
