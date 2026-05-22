package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class PropellerBearingSoundInstance extends AbstractTickableSoundInstance {
    public float largeCutoff = 14;
    public float largeCutoffFallOff = 3;
    public PropellerBearingBlockEntity be;

    public PropellerBearingSoundInstance(final PropellerBearingBlockEntity be, final boolean large) {
        super(large ? AeroSoundEvents.PROPELLER_LARGE_LOOP.event() : AeroSoundEvents.PROPELLER_SMALL_LOOP.event(), SoundSource.AMBIENT, RandomSource.create());
        this.be = be;

        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;

        final BlockPos bpos = be.getBlockPos();
        this.x = bpos.getX();
        this.y = bpos.getY();
        this.z = bpos.getZ();

        //weighted average of layers, weighted by squared radius difference (approximate annulus area)
        double weight = 0;
        double total = 0;

        final PropellerActorBehaviour behavior = this.be.getBehaviour(PropellerActorBehaviour.TYPE);
        for (final PropellerActorBehaviour.PropellerLayer layer : behavior.getLayers()) {
            final double w = layer.outerRadiusSquared() - layer.innerRadiusSquared();
            total += layer.offset() * w;
            weight += w;
        }

        if (weight > 0) {
            total /= weight;

            final Vec3i normal = be.getBlockDirection().getNormal();

            this.x += total * normal.getX();
            this.y += total * normal.getY();
            this.z += total * normal.getZ();
        }
    }

    public float getLayerVolume() {
        final double rad = this.be.getBehaviour(PropellerActorBehaviour.TYPE).radius;
        double res = 0.0;

        if (rad > this.largeCutoff + this.largeCutoffFallOff) {
            res = 0.0f;
        }

        if (rad > this.largeCutoff - this.largeCutoffFallOff) {
            res = (float) ((rad - this.largeCutoff + this.largeCutoffFallOff) / (2 * this.largeCutoffFallOff));
        }

        if (rad < this.largeCutoff - this.largeCutoffFallOff) {
            res = 1.0f;
        }

        return (float) (res);
    }

    public void tick() {
        final double rad = this.be.getBehaviour(PropellerActorBehaviour.TYPE).radius;
        if (!this.be.isRemoved() && this.be.getMovedContraption() != null) {
            final float rpmFac = Mth.clamp(Math.abs(this.be.getAngularSpeed() / 64f), 0.0f, 1.0f);
            this.volume = (float) (this.getLayerVolume() * (float) Math.pow(rpmFac, 2) * rad);//** volumeFactor*//*;

            this.attenuation = Attenuation.LINEAR;
            this.pitch = 0.0f + 1.6f * rpmFac / Mth.clamp((float) rad / 5.0f, 1.0f, 5.0f);
        } else {
            this.stop();
        }
    }

    public boolean canStartSilent() {
        return true;
    }

    public boolean canPlaySound() {
        return true;
    }

    public void setVolume(final float volume) {
        this.volume = volume;
    }
}