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
    public List<Double> getAnglesRad() {
        return List.of(this.blockEntity.getXAngle(), this.blockEntity.getZAngle());
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

    @LuaFunction
    public List<Double> getGravity() {
        final Vector3dc g = this.blockEntity.getGravityBody();
        return List.of(g.x(), g.y(), g.z());
    }

    // Body-frame linear acceleration (m/s^2). Gravity is not included: this is
    // the "true" acceleration of the ship, i.e. d/dt of linear velocity, what a
    // real accelerometer would output after subtracting the gravity vector.
    // Derived by finite-difference at 20 Hz; expect measurement noise on hard
    // maneuvers. Returns {0, 0, 0} on the first tick and whenever the block is
    // not attached to a ship.
    @LuaFunction
    public List<Double> getLinearAcceleration() {
        final Vector3dc a = this.blockEntity.getLinearAccelerationBody();
        return List.of(a.x(), a.y(), a.z());
    }
}
