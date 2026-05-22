package dev.simulated_team.simulated.mixin.sounds;

import dev.simulated_team.simulated.mixin_interface.sounds.SoundExtension;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Map;

@Mixin(SoundEngine.class)
public class SoundEngineMixin implements SoundExtension {

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Override
    public boolean simulated$isSoundPlaying(SoundInstance sound) {
        return this.instanceToChannel.containsKey(sound);
    }
}
