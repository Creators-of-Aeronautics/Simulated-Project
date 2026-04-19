package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import org.joml.Vector3dc;

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
    public List<Double> getAngularRates() {
        final Vector3dc w = this.blockEntity.getAngularVelocityBody();
        return List.of(Math.toDegrees(w.x()), Math.toDegrees(w.y()), Math.toDegrees(w.z()));
    }

    @LuaFunction
    public List<Double> getAngularRatesRad() {
        final Vector3dc w = this.blockEntity.getAngularVelocityBody();
        return List.of(w.x(), w.y(), w.z());
    }
}
