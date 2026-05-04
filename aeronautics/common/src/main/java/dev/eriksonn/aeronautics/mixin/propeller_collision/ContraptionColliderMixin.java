package dev.eriksonn.aeronautics.mixin.propeller_collision;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContraptionCollider.class)
public abstract class ContraptionColliderMixin {

    @Shadow
    static Vec3 collide(final Vec3 p_20273_, final Entity e) {
        return null;
    }

    @Inject(method = "collideEntities", at = @At("HEAD"), cancellable = true)
    private static void aeronautics$nullAxisGuard(
            final AbstractContraptionEntity contraptionEntity,
            final CallbackInfo ci) {
        if (contraptionEntity instanceof final PropellerBearingContraptionEntity propeller
                && propeller.getRotationAxis() == null) {
            ci.cancel();
        }
    }

    @Inject(method = "collideEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isAlive()Z"))
    private static void sable$removeInitialDeltaMovement(final CallbackInfo ci, @Local(argsOnly = true) final AbstractContraptionEntity contraptionEntity, @Local(ordinal = 0) final Entity entity, @Share("previousDeltaMovement") final LocalRef<Vec3> previousDeltaMovement) {
        previousDeltaMovement.set(null);
    }

    @Inject(method = "collideEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
    private static void sable$saveInitialDeltaMovement(final CallbackInfo ci, @Local(argsOnly = true) final AbstractContraptionEntity contraptionEntity, @Local(ordinal = 0) final Entity entity, @Share("previousDeltaMovement") final LocalRef<Vec3> previousDeltaMovement) {
        if (contraptionEntity instanceof final PropellerBearingContraptionEntity propeller) {
            final PropellerBearingBlockEntity bearing = propeller.getBearingEntity();

            if (bearing != null && Math.abs(bearing.getDirectionIndependentSpeed()) > 32.0) {
                previousDeltaMovement.set(entity.getDeltaMovement());
            }
        }
    }

    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void sable$setDeltaMovement(final Entity instance, final Vec3 deltaMovement, @Share("previousDeltaMovement") final LocalRef<Vec3> previousDeltaMovement) {
        if (previousDeltaMovement.get() != null) {
            instance.setDeltaMovement(deltaMovement.lerp(previousDeltaMovement.get(), 0.75));
            return;
        }

        instance.setDeltaMovement(deltaMovement);
    }
    @Redirect(method = "collideEntities", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/ContraptionCollider;collide(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 sable$overrideCollisionStrength(final Vec3 vec, final Entity entity, @Local(argsOnly = true) final AbstractContraptionEntity contraptionEntity, @Share("previousDeltaMovement") final LocalRef<Vec3> previousDeltaMovement) {
        if (previousDeltaMovement.get() != null) {
            return collide(vec, entity).scale(0.2);
        }
        return collide(vec, entity);
    }

}
