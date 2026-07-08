package dev.eriksonn.aeronautics.content.particle;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class AirPoofParticleData implements ParticleOptions, ICustomParticleDataWithSprite<AirPoofParticleData> {
    public static final AirPoofParticleData INSTANCE = new AirPoofParticleData();
    private static final MapCodec<AirPoofParticleData> CODEC = MapCodec.unit(INSTANCE);
    private static final StreamCodec<FriendlyByteBuf, AirPoofParticleData> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private AirPoofParticleData() {}

    @Override
    public ParticleEngine.SpriteParticleRegistration<AirPoofParticleData> getMetaFactory() {
        return AirPoofParticle.Factory::new;
    }

    @Override
    public MapCodec<AirPoofParticleData> getCodec(final ParticleType<AirPoofParticleData> type) {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, AirPoofParticleData> getStreamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public ParticleType<?> getType() {
        return AeroParticleTypes.AIR_POOF.get();
    }
}
