package dev.eriksonn.aeronautics.mixin.balloon;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {

    @Shadow @Final private Level level;

    @Unique
    private BlockPos simulated$blockSet = null;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void simulated$preSetBlockState(final BlockPos pPos, final BlockState pState, final boolean pIsMoving,
                                            final CallbackInfoReturnable<BlockState> cir) {
        this.simulated$blockSet = pPos;
    }

    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState simulated$setBlockState(final LevelChunkSection instance, final int pX, final int pY, final int pZ, final BlockState newState, final Operation<BlockState> original) {
        final BlockState oldState = original.call(instance, pX, pY, pZ, newState);

        if (this.level.isClientSide() && oldState != newState) {
            final BalloonMap balloonMap = BalloonMap.MAP.get(this.level);
            if (balloonMap != null) {
                balloonMap.updateNearbyBalloons(this.simulated$blockSet, oldState, newState);
            }
        }

        return oldState;
    }

}
