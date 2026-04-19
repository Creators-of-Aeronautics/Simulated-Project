package dev.simulated_team.simulated.compat.computercraft.wired;

public interface IDockingConnectorWiredElement {
	void connect(IDockingConnectorWiredElement other);
	void disconnect(IDockingConnectorWiredElement other);
	void remove();

	static IDockingConnectorWiredElement noop() {
		return new IDockingConnectorWiredElement() {
			@Override
			public void connect(IDockingConnectorWiredElement other) {}

			@Override
			public void disconnect(IDockingConnectorWiredElement other) {}

			@Override
			public void remove() {}
		};
	}
}
