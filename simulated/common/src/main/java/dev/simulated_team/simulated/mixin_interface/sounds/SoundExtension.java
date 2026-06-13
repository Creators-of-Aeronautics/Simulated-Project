package dev.simulated_team.simulated.mixin_interface.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

public interface SoundExtension {

    boolean simulated$isSoundPlaying(SoundInstance sound);

    static boolean isSoundPlaying(SoundInstance sound) {
        return ((SoundExtension) Minecraft.getInstance().getSoundManager()).simulated$isSoundPlaying(sound);
    }
}
