package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;

public class PortableEnginePeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return Simulated.MOD_ID + ":portable_engine";
    }

    @LuaFunction
    public final float getCurrentBurnTime(PortableEngineBlockEntity blockEntity) {
        return (float) blockEntity.getCurrentBurnTime() / 20;
    }

    @LuaFunction(mainThread = true)
    public final float getTotalBurnTime(PortableEngineBlockEntity blockEntity) {
        return (float) blockEntity.getTotalBurnTime() / 20;
    }

    @LuaFunction
    public final boolean isSuperHeated(PortableEngineBlockEntity blockEntity) {
        return blockEntity.isSuperHeated();
    }
}
