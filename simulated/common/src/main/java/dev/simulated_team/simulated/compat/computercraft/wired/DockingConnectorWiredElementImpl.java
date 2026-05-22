package dev.simulated_team.simulated.compat.computercraft.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class DockingConnectorWiredElementImpl implements DockingConnectorWiredElement, WiredElement {

    private final DockingConnectorBlockEntity entity;
    private final WiredNode node;

    public DockingConnectorWiredElementImpl(final DockingConnectorBlockEntity entity) {
        this.entity = entity;
        this.node = ComputerCraftAPI.createWiredNodeForElement(this);
    }

    @Override
    public WiredNode getNode() {
        return this.node;
    }

    @Override
    public String getSenderID() {
        return "docking_connector";
    }

    @Override
    public Level getLevel() {
        return this.entity.getLevel();
    }

    @Override
    public Vec3 getPosition() {
        return Vec3.atCenterOf(this.entity.getBlockPos());
    }

    @Override
    public void connect(final DockingConnectorWiredElement other) {
        if (other instanceof DockingConnectorWiredElementImpl we) {
            getNode().connectTo(we.getNode());
        }
    }

    @Override
    public void disconnect(final DockingConnectorWiredElement other) {
        if (other instanceof DockingConnectorWiredElementImpl we) {
            getNode().disconnectFrom(we.getNode());
        }
    }

    @Override
    public void remove() {
        this.getNode().remove();
    }
}
