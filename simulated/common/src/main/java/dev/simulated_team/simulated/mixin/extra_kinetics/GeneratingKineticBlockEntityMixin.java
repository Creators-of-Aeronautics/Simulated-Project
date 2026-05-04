package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GeneratingKineticBlockEntity.class)
public abstract class GeneratingKineticBlockEntityMixin extends KineticBlockEntity {

    public GeneratingKineticBlockEntityMixin(final BlockEntityType<?> typeIn, final BlockPos pos, final BlockState state) {
        super(typeIn, pos, state);
    }

    @WrapOperation(method = "setSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    //TODO: figure out how to do this without needing to mixin into overrides of setSource
    public BlockEntity simulated$getExtraKinetics(final Level instance, final BlockPos blockPos, final Operation<BlockEntity> original) {
        BlockEntity be = instance.getBlockEntity(blockPos);
        if (be instanceof final ExtraKinetics ek && blockPos instanceof ExtraBlockPos) {
            be = ek.getExtraKinetics();
        }

        return be;
    }
}
