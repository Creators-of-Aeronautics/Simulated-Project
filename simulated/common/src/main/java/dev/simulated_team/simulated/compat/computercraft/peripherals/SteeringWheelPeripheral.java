package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;

public class SteeringWheelPeripheral extends SimPeripheral<SteeringWheelBlockEntity> {

    public SteeringWheelPeripheral(SteeringWheelBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "steering_wheel";
    }

    @LuaFunction
    public final float getAngle() {
        return blockEntity.getAngle();
    }

    @LuaFunction
    public final float getTargetAngle() {
        return blockEntity.targetAngleToUpdate;
    }

    @LuaFunction
    public final void setTargetAngle(double angle) {
        blockEntity.targetAngleToUpdate = (float)angle;
    }

    @LuaFunction
    public final int getLimit() {
        return blockEntity.angleInput.getValue();
    }

    @LuaFunction
    public final void setLimit(int limit) {
        blockEntity.angleInput.setValue(limit);
    }

    @LuaFunction
    public final boolean isHeld() {
        return blockEntity.held;
    }
}
