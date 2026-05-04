package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EncasedCogwheelBlock.class)
public class EncasedCogwheelBlockMixin {
    @WrapOperation(method = "handleEncasing", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;hasShaftTowards(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    private boolean simulated$handleEncasingExtraKinetics(final IRotate instance, final LevelReader level, final BlockPos pos, final BlockState state, final Direction direction, final Operation<Boolean> original) {
        final boolean bl = original.call(instance, level, pos, state, direction);
        if (!bl && level.getBlockEntity(pos) instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                if (extraKinetics.getBlockState().getBlock() instanceof final ExtraKinetics.ExtraKineticsBlock ekb) {
                    return ekb.getExtraKineticsRotationConfiguration().hasShaftTowards(level, pos, state, direction);
                }
            }
        }
        return bl;
    }
}
