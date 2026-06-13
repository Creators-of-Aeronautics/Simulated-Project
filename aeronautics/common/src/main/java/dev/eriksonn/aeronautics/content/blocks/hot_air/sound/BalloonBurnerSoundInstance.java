package dev.eriksonn.aeronautics.content.blocks.hot_air.sound;

import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BalloonBurnerSoundInstance extends AbstractTickableSoundInstance {
    public static final BalloonBurnerSoundInstance GLOBAL_HOT_AIR_BURNER_SOUND = new BalloonBurnerSoundInstance(AeroSoundEvents.HOT_AIR_BURNER_HEAT.event());
    public static final BalloonBurnerSoundInstance GLOBAL_STEAM_VENT_AIR_BURNER_SOUND = new BalloonBurnerSoundInstance(AeroSoundEvents.STEAM_VENT_HEAT.event());

    private static final int MAX_DISTANCE = 10;
    private static final float VOLUME_SCALE = 0.325f;

    /**
     * All nearby hot air burner blocks
     */
    private final Set<BlockPos> NEARBY_BLOCKS = new HashSet<>();
    private final Vector3d meanPos = new Vector3d();

    private float meanPitch = 0;
    private float meanVolume = 0;

    public BalloonBurnerSoundInstance(SoundEvent sound) {
        super(sound, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 0.0F;
    }

    public void addPos(final BlockPos pos) {
        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        if (distSquared(camera, pos) < MAX_DISTANCE * MAX_DISTANCE) {
            if (NEARBY_BLOCKS.add(pos.immutable())) {
                updateMeanPos();
            }
        }
    }

    public void removePos(final BlockPos pos) {
        NEARBY_BLOCKS.remove(pos);
        updateMeanPos();
    }

    private void updateMeanPos() {
        this.meanPos.zero();

        final Vector3d v = new Vector3d();

        if (!NEARBY_BLOCKS.isEmpty()) {
            for (final BlockPos nearby : NEARBY_BLOCKS) {
                v.set(nearby.getX() + 0.5, nearby.getY() + 0.5, nearby.getZ() + 0.5);

                final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(v);

                if (subLevel != null)
                    subLevel.logicalPose().transformPosition(v);

                this.meanPos.add(v);
            }

            this.meanPos.div(NEARBY_BLOCKS.size());
        }
    }

    private void updateInformation() {
        if (NEARBY_BLOCKS.isEmpty()) {
            return;
        }

        final ClientLevel level = Minecraft.getInstance().level;
        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        this.meanVolume = 0.001f;
        int volumeChangers = 0;
        final Iterator<BlockPos> iter = NEARBY_BLOCKS.iterator();
        while (iter.hasNext()) {
            final BlockPos next = iter.next();
            if (next != null) {
                if (distSquared(camera, next) > MAX_DISTANCE * MAX_DISTANCE) {
                    iter.remove();
                    updateMeanPos();
                    continue;
                }

                final BlockEntity be = level.getBlockEntity(next);
                float intensityScaling;
                if (be instanceof HotAirBurnerBlockEntity hbe) {
                    intensityScaling = Mth.clamp(hbe.getClientIntensity().getValue(), 0.0f, 1.0f);
                } else if (be instanceof final SteamVentBlockEntity sbe) {
                    intensityScaling = Mth.clamp(sbe.getClientIntensity().getValue(), 0.0f, 1.0f);
                } else {
                    iter.remove();
                    updateMeanPos();
                    continue;
                }


                this.meanVolume += Math.clamp(intensityScaling * 4.0f, 0.0f, 2.0f);
                volumeChangers++;
            }
        }

        if (!NEARBY_BLOCKS.isEmpty()) {
            this.meanPitch = 1.0f;
            this.meanVolume /= volumeChangers;
            this.meanVolume *= (float) (1 - (Math.sqrt(distSquared(camera, this.meanPos)) / MAX_DISTANCE));
        }
    }

    private static double distSquared(final Camera camera, final Vector3dc pos) {
        final ClientLevel level = Minecraft.getInstance().level;
        return Sable.HELPER.distanceSquaredWithSubLevels(level, camera.getPosition(), pos.x(), pos.y(), pos.z());
    }

    private static double distSquared(final Camera camera, final Vec3i pos) {
        final ClientLevel level = Minecraft.getInstance().level;
        return Sable.HELPER.distanceSquaredWithSubLevels(level, camera.getPosition(), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
    }

    @Override
    public void tick() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        updateInformation();

        if (NEARBY_BLOCKS.isEmpty()) {
            return;
        }

        this.x = this.meanPos.x;
        this.y = this.meanPos.y;
        this.z = this.meanPos.z;

        this.volume = this.meanVolume * VOLUME_SCALE;
        this.pitch = this.meanPitch;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        final ClientLevel level = Minecraft.getInstance().level;
        return level != null && !NEARBY_BLOCKS.isEmpty();
    }

    @Override
    public boolean isStopped() {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null || NEARBY_BLOCKS.isEmpty();
    }
}