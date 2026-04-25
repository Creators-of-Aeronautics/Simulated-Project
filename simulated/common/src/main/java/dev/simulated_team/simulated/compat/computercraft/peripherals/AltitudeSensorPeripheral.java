package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;

public class AltitudeSensorPeripheral extends SimPeripheral<AltitudeSensorBlockEntity> {

    public AltitudeSensorPeripheral(final AltitudeSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "altitude_sensor";
    }

    @LuaFunction
    public float getHeight() {
        return this.blockEntity.getWorldHeight();
    }

    @LuaFunction
    public double getAirPressure() {
        return this.blockEntity.getAirPressure();
    }

    @LuaFunction
    public double getVerticalSpeed() {
        return this.blockEntity.getVerticalSpeed();
    }
}
