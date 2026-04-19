package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;

public class SwivelBearingPeripheral extends SimPeripheral<SwivelBearingBlockEntity> {

    public SwivelBearingPeripheral(final SwivelBearingBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "swivel_bearing";
    }

    @LuaFunction
    public double getTargetAngle() {
        return this.blockEntity.getTargetAngleDegrees();
    }

    @LuaFunction
    public double getTargetAngleRad() {
        return Math.toRadians(this.blockEntity.getTargetAngleDegrees());
    }
}
