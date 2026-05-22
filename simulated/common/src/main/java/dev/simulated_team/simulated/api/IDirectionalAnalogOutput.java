package dev.simulated_team.simulated.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IDirectionalAnalogOutput {
    int getAnalogOutputSignalFrom(final BlockState blockState, final Level level, final BlockPos blockPos, final Direction dir);
}
