package dev.eriksonn.aeronautics.util;

import dev.eriksonn.aeronautics.content.blocks.hot_air.sound.BalloonBurnerSoundInstance;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound.PropellerBearingSoundHolder;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound.PropellerBearingSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class AeroSoundDistUtil {
    public static @Nullable Object tickPropellerSounds(final PropellerBearingBlockEntity be, @Nullable Object soundInstance) {
        final SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        boolean needsNewSounds = false;

        if (soundInstance instanceof PropellerBearingSoundHolder(
                final PropellerBearingSoundInstance small, final PropellerBearingSoundInstance large
        ) && (small.isStopped() || large.isStopped())) {
            soundManager.stop(small);
            soundManager.stop(large);
            needsNewSounds = true;
        }

        if (soundInstance == null) {
            needsNewSounds = true;
        }

        if (needsNewSounds) {
            final PropellerBearingSoundInstance smallSound = new PropellerBearingSoundInstance(be, false);
            final PropellerBearingSoundInstance largeSound = new PropellerBearingSoundInstance(be, true);
            soundManager.queueTickingSound(smallSound);
            soundManager.queueTickingSound(largeSound);
            soundInstance = new PropellerBearingSoundHolder(smallSound, largeSound);
        }

        return soundInstance;
    }

    public static void tickGlobalBurnerSound() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final SoundManager soundManager = minecraft.getSoundManager();

        if (level == null) {
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        if (!soundManager.isActive(BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND)) {
            soundManager.queueTickingSound(BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND);
        }

        if (!soundManager.isActive(BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND)) {
            soundManager.queueTickingSound(BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND);
        }
    }

    public static void addPosHotAirBurnerSound(final BlockPos pos) {
        BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND.addPos(pos);
    }

    public static void removePosHotAirBurnerSound(final BlockPos pos) {
        BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND.removePos(pos);
    }

    public static void addPosSteamVentSound(final BlockPos pos) {
        BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND.addPos(pos);
    }

    public static void removePosSteamVentSound(final BlockPos pos) {
        BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND.removePos(pos);
    }
}
