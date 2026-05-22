package dev.simulated_team.simulated.compat.computercraft.wired;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum NoopDockingConnectorWiredElement implements DockingConnectorWiredElement {
    INSTANCE;

    @Override
    public void connect(DockingConnectorWiredElement other) {
    }

    @Override
    public void disconnect(DockingConnectorWiredElement other) {
    }

    @Override
    public void remove() {
    }
}
