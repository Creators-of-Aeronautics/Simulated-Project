package dev.eriksonn.aeronautics.index;


import com.simibubi.create.foundation.particle.ICustomParticleData;
import dev.eriksonn.aeronautics.content.particle.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum AeroParticleTypes {
    PROPELLER_AIR_FLOW(PropellerAirParticleData::new),
    HOT_AIR_EMBER(HotAirEmberParticleData::new),
    LEVITITE_SPARKLE(LevititeSparkleParticleData::new),
    GUST(GustParticleData::new),
    AIR_POOF(AirPoofParticleData::new);

    public final ParticleEntry<?> entry;

    <D extends ParticleOptions> AeroParticleTypes(final Supplier<? extends ICustomParticleData<D>> typeFactory) {
        this.entry = new ParticleEntry<>(typeFactory);
    }

    public static void init() {
        // no-op
    }

    public static void registerClientParticles(final Consumer<ParticleEntry<?>> consume) {
        for (final AeroParticleTypes value : values()) {
            consume.accept(value.entry);
        }
    }

    public ParticleType<?> get() {
        return this.entry.object;
    }

    public static class ParticleEntry<D extends ParticleOptions> {
        private final Supplier<? extends ICustomParticleData<D>> typeFactory;
        private final ParticleType<D> object;

        public ParticleEntry(final Supplier<? extends ICustomParticleData<D>> typeFactory) {
            this.typeFactory = typeFactory;

            this.object = this.typeFactory.get().createType();
        }

        public Supplier<? extends ICustomParticleData<D>> getTypeFactory() {
            return this.typeFactory;
        }

        public ParticleType<D> getObject() {
            return this.object;
        }

    }
}
