package dev.eriksonn.aeronautics.mixin.balloon;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TODO: event hook please (or some system to register things)
 */
@Mixin(SimAssemblyHelper.class)
public class SimAssemblyHelperMixin {

    @Inject(method = "disassembleSubLevel", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/SubLevel;getPlot()Ldev/ryanhcode/sable/sublevel/plot/LevelPlot;", ordinal = 1))
    private static void aeronautics$needsBitSet(final Level level,
                                                final SubLevel toDisassemble,
                                                final BlockPos subLevelAnchor,
                                                final BlockPos disassemblyGoal,
                                                final Rotation rotation,
                                                final boolean playSound,
                                                final CallbackInfo ci,
                                                @Local final SubLevelAssemblyHelper.AssemblyTransform transform) {
        final BalloonMap balloonMap = BalloonMap.MAP.get(level);
        if (balloonMap != null) {
            for (final Balloon balloon : balloonMap.getBalloons()) {
                final BlockPos controllerPos = balloon.getControllerPos();
                if (toDisassemble.getPlot().contains(controllerPos.getX(), controllerPos.getZ())) {
                    balloon.setAssembling(transform);
                    return;
                }
            }
        }
    }
}
