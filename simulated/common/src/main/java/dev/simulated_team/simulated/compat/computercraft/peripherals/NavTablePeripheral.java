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
}
