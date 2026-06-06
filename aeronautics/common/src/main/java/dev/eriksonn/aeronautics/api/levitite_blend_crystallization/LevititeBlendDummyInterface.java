package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public interface LevititeBlendDummyInterface {
    default void levititeBlendTick(final Level level, final BlockPos pos, final FluidState state) {
        if (LevititeCrystallizerManager.isTickedPosition(level, pos))
            return;

        //temp
        LevititeBlendHelper.checkSurroundingSources(level, pos, state);
    }
}
