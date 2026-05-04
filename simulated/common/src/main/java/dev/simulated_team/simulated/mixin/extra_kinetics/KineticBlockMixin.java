package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticBlock.class)
public class KineticBlockMixin {

    @Inject(method = "updateIndirectNeighbourShapes", at = @At("TAIL"))
    public void simulated$extraKineticsReset(final BlockState stateIn, final LevelAccessor worldIn, final BlockPos pos, final int flags, final int count, final CallbackInfo ci, @Local final BlockEntity be) {
        if (be instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                extraKinetics.warnOfMovement();
                extraKinetics.clearKineticInformation();
                extraKinetics.updateSpeed = true;
            }
        }
    }

}
