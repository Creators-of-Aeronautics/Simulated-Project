package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;

public class NavTablePeripheral extends SimPeripheral<NavTableBlockEntity> {

    public NavTablePeripheral(final NavTableBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "navigation_table";
    }

    @LuaFunction
    public Float getRelativeAngle() {
        return this.blockEntity.getRelativeAngle();
    }

    @LuaFunction
    public double getRelativeAngleRad() {
        return Math.toRadians(this.blockEntity.getRelativeAngle());
    }

    @LuaFunction
    public double getDistanceToTarget() {
        return this.blockEntity.distanceToTarget();
    }

    // Nav table samples distance every 11 ticks (0.55s); closure rate reflects that cadence.
    @LuaFunction
    public double getClosureRate() {
        return (this.blockEntity.lastDistanceToTarget() - this.blockEntity.distanceToTarget()) / (11.0 / 20.0);
    }
}
