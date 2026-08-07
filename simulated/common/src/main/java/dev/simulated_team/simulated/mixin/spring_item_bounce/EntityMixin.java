package dev.simulated_team.simulated.mixin.spring_item_bounce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract void playSound(SoundEvent soundEvent, float f, float g);

    @Shadow public abstract Level level();

    @Shadow public abstract Vec3 getPosition(float partialTicks);

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
    private void simulated$bouncyItemVelocity(final Block instance, final BlockGetter level, final Entity entity,
                                              final Operation<Void> original) {
        if (entity instanceof final ItemEntity item) {
            final Float bounce = item.getItem().get(SimDataComponents.BOUNCINESS);
            if (bounce != null && bounce > 0) {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, -bounce, 1));
                return;
            }
        }

        original.call(instance, level, entity);
    }

    @Inject(method = "checkFallDamage", at = @At(value = "HEAD"))
    private void simulated$bouncyItemAdvancement(final double d, final boolean onGround, final BlockState blockState, final BlockPos blockPos,
                                                 final CallbackInfo ci) {
        if (onGround && ((Entity)(Object)this) instanceof final ItemEntity item) {
            final Float bounce = item.getItem().get(SimDataComponents.BOUNCINESS);
            if (bounce != null && bounce > 0) {
                if (item.fallDistance >= 128 && item.getOwner() instanceof final Player player) {
                    SimAdvancements.MUST_COME_UP.awardTo(player);
                }
            }
        }
    }
}
