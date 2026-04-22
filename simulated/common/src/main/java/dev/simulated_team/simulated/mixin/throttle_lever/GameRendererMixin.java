package dev.simulated_team.simulated.mixin.throttle_lever;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverClientGripHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void simulated$pickThrottleLever(final float partialTicks, final CallbackInfo ci) {
        if (this.minecraft == null) return;

        final LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        final Vec3 eyePos = Sable.HELPER.getEyePositionInterpolated(player, partialTicks);

        final HitResult mcHitResult = this.minecraft.hitResult;
        double minDistance = mcHitResult != null && mcHitResult.getType() != HitResult.Type.MISS ? Sable.HELPER.distanceSquaredWithSubLevels(player.level(), eyePos, mcHitResult.getLocation()) : Double.MAX_VALUE;

        for (final ThrottleLeverBlockEntity lever : ThrottleLeverClientGripHandler.getNearbyThrottleLevers()) {
            if (lever.isRemoved()) continue;

            final Double hitResultDistance = ThrottleLeverClientGripHandler.raycastLever(eyePos, player.getViewVector(partialTicks), lever, partialTicks);

            if (hitResultDistance != null) {
                if (hitResultDistance < minDistance) {
                    minDistance = hitResultDistance;
                    this.minecraft.hitResult = new BlockHitResult(lever.getBlockPos().getCenter(), Direction.UP, lever.getBlockPos(), false);
                }
            }

        }
    }

}
