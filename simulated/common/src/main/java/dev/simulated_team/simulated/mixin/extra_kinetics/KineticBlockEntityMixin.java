package dev.simulated_team.simulated.mixin.extra_kinetics;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticBlockEntity.class)
//TODO: fix contraptions moving ExtraKinetic BlockEntities and creating a timestamp-space split, allowing wireless networks to exist
public abstract class KineticBlockEntityMixin extends SmartBlockEntity implements KineticBlockEntityExtension {

    public KineticBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract boolean hasSource();

    @Shadow
    protected float speed;

    @Shadow
    public abstract void initialize();

    @Shadow private int validationCountdown;
    @Unique
    private boolean simulated$extraKineticsConnected = false;

    @Override
    public void simulated$setConnectedToExtraKinetics(final boolean connectedToExtraKinetics) {
        this.simulated$extraKineticsConnected = connectedToExtraKinetics;
    }

    @Override
    public boolean simulated$getConnectedToExtraKinetics() {
        return this.simulated$extraKineticsConnected;
    }

    @Inject(method = "switchToBlockState", at = @At("TAIL"))
    private static void simulated$switchExtraKinetics(final Level world, final BlockPos pos, final BlockState state, final CallbackInfo ci, @Local final BlockEntity be) {
        if (be instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                if (extraKinetics.hasNetwork()) {
                    extraKinetics.getOrCreateNetwork().remove(extraKinetics);

                    extraKinetics.detachKinetics();
                    extraKinetics.removeSource();

                    if (extraKinetics instanceof final GeneratingKineticBlockEntity gbe) {
                        gbe.reActivateSource = true;
                    }
                }
            }
        }
    }

    @Redirect(method = "validateKinetics", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    public BlockEntity simulated$useProperSource(final Level instance, final BlockPos blockPos) {
        BlockEntity be = instance.getBlockEntity(blockPos);
        if (be instanceof final ExtraKinetics ek && this.simulated$extraKineticsConnected) {
            be = ek.getExtraKinetics();
        }

        return be;
    }

    @Redirect(method = "setSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    public BlockEntity simulated$useProperSource2(final Level instance, final BlockPos blockPos) {
        BlockEntity be = instance.getBlockEntity(blockPos);
        if (be instanceof final ExtraKinetics ek && blockPos instanceof final ExtraBlockPos exp) {
            this.simulated$extraKineticsConnected = true;
            be = ek.getExtraKinetics();
        }

        return be;
    }

    @Override
    public void setLevel(final Level level) {
        super.setLevel(level);
        if (this instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                extraKinetics.setLevel(level);
            }
        }
    }

    @Override
    public void setBlockState(final BlockState blockState) {
        super.setBlockState(blockState);
        if (this instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                extraKinetics.setBlockState(blockState);
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (this instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                extraKinetics.invalidate();
            }
        }
    }

    @Inject(method = "remove", at = @At("TAIL"), remap = false)
    public void simulated$injectRemove(final CallbackInfo ci) {
        if (this instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                extraKinetics.remove();
            }
        }
    }

    @Inject(method = "removeSource", at = @At("TAIL"), remap = false)
    public void simulated$removeConnected(final CallbackInfo ci) {
        this.simulated$extraKineticsConnected = false;
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    public void simulated$saveConnected(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (this instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                final CompoundTag internalTag = new CompoundTag();
                if (clientPacket) {
                    extraKinetics.writeClient(internalTag, registries);
                } else {
                    extraKinetics.saveAdditional(internalTag, registries);
                }

                compound.put(ek.getExtraKineticsSaveName(), internalTag);
            }
        }

        if (this.hasSource()) {
            compound.putBoolean("ConnectedToExtraKinetics", this.simulated$extraKineticsConnected);
        }
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    public void simulated$readConnected(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (this instanceof final ExtraKinetics ek) {
            final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
            if (extraKinetics != null) {
                final CompoundTag extraKineticsTag = compound.getCompound(ek.getExtraKineticsSaveName());
                if (clientPacket) {
                    extraKinetics.readClient(extraKineticsTag, registries);
                } else {
                    extraKinetics.loadCustomOnly(extraKineticsTag, registries);
                }
            }
        }

        if (compound.contains("ConnectedToExtraKinetics")) {
            this.simulated$extraKineticsConnected = compound.getBoolean("ConnectedToExtraKinetics");
        }
    }

    @Override
    public void simulated$setValidationCountdown(final int validationCountdown) {
        this.validationCountdown = validationCountdown;
    }
}
