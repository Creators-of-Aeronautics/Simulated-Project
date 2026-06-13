package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(RotationPropagator.class)
public abstract class RotationPropagatorMixin {

    @WrapOperation(method = {"handleRemoved", "propagateMissingSource", "findConnectedNeighbour"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity simulated$accountForExtraKinetics(final Level instance, final BlockPos pos, final Operation<BlockEntity> original) {
        final BlockEntity be = original.call(instance, pos);
        if (be instanceof final ExtraKinetics ek && pos instanceof ExtraBlockPos) {
            return ek.getExtraKinetics();
        }

        return be;
    }

    @WrapOperation(method = "getRotationSpeedModifier", at = {
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isSmallCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0),
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isSmallCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 3)
    })
    private static boolean testSmallCogFrom1(final BlockState state, final Operation<Boolean> original, @Local(argsOnly = true, ordinal = 0) final KineticBlockEntity fromBE) {
        return simulated$checkCogStateSmall(original.call(state), fromBE);
    }

    @WrapOperation(method = "getRotationSpeedModifier", at = {
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isSmallCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 1),
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isSmallCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 2)
    })
    private static boolean testSmallCogTo(final BlockState state, final Operation<Boolean> original, @Local(argsOnly = true, ordinal = 1) final KineticBlockEntity toBE) {
        return simulated$checkCogStateSmall(original.call(state), toBE);
    }

    @WrapOperation(method = "getRotationSpeedModifier", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isLargeCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private static boolean testLargeCogFrom(final BlockState state, final Operation<Boolean> original, @Local(argsOnly = true, ordinal = 0) final KineticBlockEntity fromBE) {
        return simulated$checkCogStateLarge(original.call(state), fromBE);
    }

    @WrapOperation(method = "getRotationSpeedModifier", at = {
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isLargeCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 1),
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ICogWheel;isLargeCog(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 2)
    })
    private static boolean testLargeCogTo(final BlockState state, final Operation<Boolean> original, @Local(argsOnly = true, ordinal = 1) final KineticBlockEntity toBE) {
        return simulated$checkCogStateLarge(original.call(state), toBE);
    }

    @ModifyReceiver(method = "getRotationSpeedModifier", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;hasShaftTowards(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", ordinal = 0))
    private static IRotate changeIRotateShaftFrom(final IRotate instance, final LevelReader levelReader, final BlockPos blockPos, final BlockState state, final Direction direction, @Local(argsOnly = true, ordinal = 0) final KineticBlockEntity fromBE) {
        return simulated$getNewIRotate(instance, fromBE);
    }

    @ModifyReceiver(method = "getRotationSpeedModifier", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;hasShaftTowards(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", ordinal = 1))
    private static IRotate changeIRotateShaftTo(final IRotate instance, final LevelReader levelReader, final BlockPos blockPos, final BlockState state, final Direction direction, @Local(argsOnly = true, ordinal = 1) final KineticBlockEntity toBe) {
        return simulated$getNewIRotate(instance, toBe);
    }

    @ModifyReceiver(method = "getRotationSpeedModifier", at = {
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;getRotationAxis(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;", ordinal = 0),
            @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;getRotationAxis(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;", ordinal = 1)
    })
    private static IRotate changeIRotateAxisFrom(final IRotate instance, final BlockState state, @Local(argsOnly = true, ordinal = 0) final KineticBlockEntity fromBE) {
        return simulated$getNewIRotate(instance, fromBE);
    }

    @ModifyReceiver(method = "getRotationSpeedModifier", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/IRotate;getRotationAxis(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;", ordinal = 2))
    private static IRotate changeIRotateAxisTo(final IRotate instance, final BlockState state, @Local(argsOnly = true, ordinal = 1) final KineticBlockEntity toBe) {
        return simulated$getNewIRotate(instance, toBe);
    }

    @Inject(method = "getConnectedNeighbours", at = @At("TAIL"), remap = false)
    private static void simulated$addExtraKineticsBlockEntities(final KineticBlockEntity be, final CallbackInfoReturnable<List<KineticBlockEntity>> cir) {
        final List<KineticBlockEntity> list = cir.getReturnValue();
        if (be instanceof final ExtraKinetics.ExtraKineticsBlockEntity ekbe) {
            final KineticBlockEntity parent = ekbe.getParentBlockEntity();
            if (parent != null && ((ExtraKinetics) parent).shouldConnectExtraKinetics()) {
                list.add(parent);
            }
        } else if (be instanceof final ExtraKinetics ek && ek.shouldConnectExtraKinetics()) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                list.add(extraKinetics);
            }
        }
    }

    @Inject(method = "getPotentialNeighbourLocations", at = @At("TAIL"), remap = false)
    private static void simulated$getExtraKineticsBlockPositions(final KineticBlockEntity be, final CallbackInfoReturnable<List<BlockPos>> cir) {

        //This list should NOT be getting replaced ever ever, OR made immutable... whoever is replacing the list with their own IMMUTABLE one needs to fix it on THEIR side. This method provides its own list that should be modified by mixins, OR the block, NOT replaced.
        final List<BlockPos> list = cir.getReturnValue();
        final Level level = be.getLevel();

        final ListIterator<BlockPos> lIter = list.listIterator();
        while (lIter.hasNext()) {
            final BlockPos current = lIter.next();
            //make sure that we're not actually modifying the list unless there are extra kinetics
            if (level.getBlockState(current).getBlock() instanceof ExtraKinetics.ExtraKineticsBlock) {
                lIter.add(new ExtraBlockPos(current));
            }
        }
    }

    @Unique
    private static boolean simulated$checkCogStateSmall(final boolean original, final KineticBlockEntity be) {
        if (original) {
            return true;
        }

        if (be.getBlockPos() instanceof ExtraBlockPos && be.getBlockState().getBlock() instanceof final ExtraKinetics.ExtraKineticsBlock ekb) {
            return ekb.getExtraKineticsRotationConfiguration() instanceof final ICogWheel ic && ic.isSmallCog();
        }

        return false;
    }

    @Unique
    private static boolean simulated$checkCogStateLarge(final boolean original, final KineticBlockEntity be) {
        if (original) {
            return true;
        }

        if (be.getBlockPos() instanceof ExtraBlockPos && be.getBlockState().getBlock() instanceof final ExtraKinetics.ExtraKineticsBlock ekb) {
            return ekb.getExtraKineticsRotationConfiguration() instanceof final ICogWheel ic && ic.isLargeCog();
        }

        return false;
    }

    @Unique
    private static IRotate simulated$getNewIRotate(final IRotate currentRotate, final KineticBlockEntity be) {
        if (be.getBlockPos() instanceof ExtraBlockPos && be.getBlockState().getBlock() instanceof final ExtraKinetics.ExtraKineticsBlock ekb) {
            return ekb.getExtraKineticsRotationConfiguration();
        }

        return currentRotate;
    }
}
