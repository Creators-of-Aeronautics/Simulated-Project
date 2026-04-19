package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlockEntity;

public class DirectionalLinkPeripheral extends SimPeripheral<DirectionalLinkedReceiverBlockEntity>{
    public DirectionalLinkPeripheral(final DirectionalLinkedReceiverBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "directional_link";
    }

    @LuaFunction
    public double getClosestAngle() {
        return Math.toDegrees(this.blockEntity.getAngleToClosestLink());
    }

    @LuaFunction
    public double getClosestAngleRad() {
        return this.blockEntity.getAngleToClosestLink();
    }
}
