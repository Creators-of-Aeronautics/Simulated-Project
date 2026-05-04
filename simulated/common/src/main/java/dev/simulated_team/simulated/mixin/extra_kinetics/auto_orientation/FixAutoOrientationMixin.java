package dev.simulated_team.simulated.mixin.extra_kinetics.auto_orientation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.*;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {HorizontalKineticBlock.class, DirectionalAxisKineticBlock.class, DirectionalKineticBlock.class})
public class FixAutoOrientationMixin {

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;hasShaftTowards(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"), remap = false)
    public boolean simulated$handleAutoOrientationWithExtraKinetics(final IRotate instance, final LevelReader world, final BlockPos pos, final BlockState state, final Direction incomingDir, final Operation<Boolean> originalWrap) {
        Boolean original = originalWrap.call(instance, world, pos, state, incomingDir);

        if (!original)  { //if the original call does not succeed, check to see if we have internal block entities, and check those
            final BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof final ExtraKinetics ek) {
                final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
                if (extraKinetics != null) {
                    if (extraKinetics.getBlockState().getBlock() instanceof final ExtraKinetics.ExtraKineticsBlock ekb) {
                        original = ekb.getExtraKineticsRotationConfiguration().hasShaftTowards(world, pos, state, incomingDir);
                    }
                }
            }
        }

        return original;
    }

}
