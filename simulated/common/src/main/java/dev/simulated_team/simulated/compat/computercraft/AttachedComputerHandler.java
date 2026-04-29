package dev.simulated_team.simulated.compat.computercraft;

import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import org.jspecify.annotations.Nullable;

public class AttachedComputerHandler {
    private final AttachedComputerSet attachedComputers = new AttachedComputerSet();

    public void attach(IComputerAccess computer) {
        this.attachedComputers.add(computer);
    }

    public void detach(IComputerAccess computer) {
        this.attachedComputers.remove(computer);
    }

    public void queueEvent(String event, @Nullable Object... args) {
        this.attachedComputers.queueEvent(event, args);
    }
}
