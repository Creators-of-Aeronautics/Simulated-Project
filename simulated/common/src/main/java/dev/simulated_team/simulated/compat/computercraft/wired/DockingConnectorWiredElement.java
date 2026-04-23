package dev.simulated_team.simulated.compat.computercraft.wired;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.service.SimPlatformService;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface DockingConnectorWiredElement {

    boolean CC_LOADED = SimPlatformService.INSTANCE.isLoaded("computercraft");

    void connect(DockingConnectorWiredElement other);

    void disconnect(DockingConnectorWiredElement other);

    void remove();

    static DockingConnectorWiredElement create(final DockingConnectorBlockEntity blockEntity) {
        return CC_LOADED ? new DockingConnectorWiredElementImpl(blockEntity) : NoopDockingConnectorWiredElement.INSTANCE;
    }
}
