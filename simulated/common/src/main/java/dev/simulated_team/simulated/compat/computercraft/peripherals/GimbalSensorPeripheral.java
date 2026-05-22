package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;

import java.util.List;

public class GimbalSensorPeripheral extends SimPeripheral<GimbalSensorBlockEntity> {

    public GimbalSensorPeripheral(final GimbalSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "gimbal_sensor";
    }

    @LuaFunction
    public List<Double> getAngles() {
        return List.of(Math.toDegrees(this.blockEntity.getXAngle()), Math.toDegrees(this.blockEntity.getZAngle()));
    }

    @LuaFunction
    public List<Double> getAnglesRad() {
        return List.of(this.blockEntity.getXAngle(), this.blockEntity.getZAngle());
    }
}
