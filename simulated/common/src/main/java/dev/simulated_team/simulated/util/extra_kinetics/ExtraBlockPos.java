package dev.simulated_team.simulated.util.extra_kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * Just a helper block position class for ExtraKinetics block entities. Acting as a flag for systems to check said ExtraKinetics block entities.
 */
//TODO: really should remove this and replace it with something more concise.
public class ExtraBlockPos extends BlockPos {

    public ExtraBlockPos(final Vec3i blockPos) {
        super(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

}
