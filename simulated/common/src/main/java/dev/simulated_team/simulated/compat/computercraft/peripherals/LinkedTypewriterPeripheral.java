package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;

import java.util.List;

public class LinkedTypewriterPeripheral extends SimPeripheral<LinkedTypewriterBlockEntity> {

    public LinkedTypewriterPeripheral(final LinkedTypewriterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "linked_typewriter";
    }

    @Override
    public void attach(IComputerAccess computer) {
        super.attach(computer);
        this.blockEntity.computerHandler.attach(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        super.detach(computer);
        this.blockEntity.computerHandler.detach(computer);
    }

    @LuaFunction
    public final List<Integer> getPressedKeyCodes() {
        return this.blockEntity.getPressedKeys();
    }
}
