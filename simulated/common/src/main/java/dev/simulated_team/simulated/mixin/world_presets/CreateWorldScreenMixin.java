package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Optional;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow
    @Final
    WorldCreationUiState uiState;

    @Inject(method = "createNewWorld", at = @At("HEAD"))
    private void simulated$createNewWorld(final CallbackInfo ci) {
        final Holder<WorldPreset> holder = this.uiState.getWorldType().preset();
        if (holder == null) {
            return;
        }

        final Optional<ResourceKey<WorldPreset>> key = holder.unwrapKey();
        if (key.isEmpty()) {
            return;
        }

        final ResourceLocation location = key.get().location();
        final SimulatedWorldPreset simPreset = SimWorldPresets.PRESETS.get(location);

        if (simPreset != null) {
            final GameRules gameRules = this.uiState.getGameRules();
            simPreset.modifyGameRules(gameRules);
        }
    }

    @Inject(method = "createNewWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createWorldOpenFlows()Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;", shift = At.Shift.BEFORE))
    private void simulated$createNewWorld2(final CallbackInfo ci, @Local final WorldData worldData) {
        final Holder<WorldPreset> holder = this.uiState.getWorldType().preset();
        if (holder == null) {
            return;
        }

        final Optional<ResourceKey<WorldPreset>> key = holder.unwrapKey();
        if (key.isEmpty()) {
            return;
        }

        ((PrimaryLevelDataExtension) worldData).setPreset(key.get().location());
        if (holder.is(SimWorldPresets.END_SEA.id())) {
            ((PrimaryLevelDataExtension) worldData).setEndDragonFight(new EndDragonFight.Data(false, true, true, false, Optional.empty(), Optional.empty(), Optional.empty()));
        }
    }
}
