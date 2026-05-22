package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;

public class TorsionSpringPeripheral extends SimPeripheral<TorsionSpringBlockEntity>{
    public TorsionSpringPeripheral(final TorsionSpringBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "torsion_spring";
    }

    @LuaFunction
    public void setLimit(final int limit) {
        if(this.blockEntity.isSpringStatic()) {
            this.blockEntity.angleInput.setValue(limit);
        }
    }

    @LuaFunction
    public float getAngle() {
        return this.blockEntity.getAngle();
    }

    @LuaFunction
    public double getAngleRad() {
        return Math.toRadians(this.blockEntity.getAngle());
    }

    @LuaFunction
    public int getLimit() {
        return this.blockEntity.angleInput.getValue();
    }

    @LuaFunction
    public boolean isRunning() {
        return !this.blockEntity.isSpringStatic();
    }

}
