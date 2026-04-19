package dev.eriksonn.aeronautics.mixin.balloon;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * TODO: event hook please (or some system to register things)
 */
@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblyHelperMixin {

    @Inject(method = "needsBitSet", at = @At("HEAD"), cancellable = true)
    private static void aeronautics$needsBitSet(final ServerLevel level, final BoundingBox3ic bounds, final List<Entity> entities, final CallbackInfoReturnable<Boolean> cir) {
        final BalloonMap balloonMap = BalloonMap.MAP.get(level);
        if (balloonMap != null) {
            for (final Balloon balloon : balloonMap.getBalloons()) {
                if (balloon.getBounds().intersects(bounds)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "moveOtherStuff", at = @At("TAIL"))
    private static void aeronautics$assemble(final ServerLevel level,
                                             final SubLevelAssemblyHelper.AssemblyTransform transform,
                                             final Iterable<BlockPos> blocks,
                                             final BoundingBox3ic bounds,
                                             final CallbackInfo ci,
                                             @Local final BoundedBitVolume3i volume) {
        final BalloonMap balloonMap = BalloonMap.MAP.get(level);
        if (balloonMap != null) {
            for (final Balloon balloon : balloonMap.getBalloons()) {
                if (!balloon.getBounds().intersects(bounds)) {
                    continue;
                }

                boolean shouldMoveBalloon = false;

                for (final Direction direction : SimDirectionUtil.VALUES) {
                    final BlockPos relativePos = balloon.getControllerPos().relative(direction);

                    if (volume.getOccupied(relativePos.getX(), relativePos.getY(), relativePos.getZ())) {
                        shouldMoveBalloon = true;
                        break;
                    }
                }

                if (!shouldMoveBalloon) {
                    continue;
                }

                balloon.setAssembling(transform);
            }
        }
    }

}
