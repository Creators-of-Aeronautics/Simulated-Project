package dev.simulated_team.simulated.mixin.sounds;

import dev.simulated_team.simulated.mixin_interface.sounds.SoundExtension;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundManager.class)
public class SoundManagerMixin implements SoundExtension {

    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Override
    public boolean simulated$isSoundPlaying(SoundInstance sound) {
        return ((SoundExtension) this.soundEngine).simulated$isSoundPlaying(sound);
    }
}
