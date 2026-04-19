package dev.simulated_team.simulated.compat.computercraft.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DockingConnectorWiredElement implements IDockingConnectorWiredElement, WiredElement {
	private final WiredNode node = ComputerCraftAPI.createWiredNodeForElement(this);
	private final DockingConnectorBlockEntity entity;

	public DockingConnectorWiredElement(DockingConnectorBlockEntity entity) {
		this.entity = entity;
	}

	@Override
	public WiredNode getNode() {
		return node;
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
	public void connect(IDockingConnectorWiredElement other) {
		if (other instanceof DockingConnectorWiredElement we) {
			getNode().connectTo(we.getNode());
		}
	}

	@Override
	public void disconnect(IDockingConnectorWiredElement other) {
		if (other instanceof DockingConnectorWiredElement we) {
			getNode().disconnectFrom(we.getNode());
		}
	}

	@Override
	public void remove() {
		this.getNode().remove();
	}
}
