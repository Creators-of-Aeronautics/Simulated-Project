package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;

public class VelocitySensorPeripheral extends SimPeripheral<VelocitySensorBlockEntity> {

    public VelocitySensorPeripheral(final VelocitySensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "velocity_sensor";
    }

    @LuaFunction
    public float getVelocity() {
        return this.blockEntity.getAdjustedVelocity();
    }
}
