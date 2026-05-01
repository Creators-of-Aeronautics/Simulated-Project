package dev.eriksonn.aeronautics.content.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eriksonn.aeronautics.content.particle.LevititeSparkleParticleData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Optional;

public record Levitating(Float dragFraction, Optional<ParticleOptions> particle) {
	public static final Codec<Levitating> CODEC = RecordCodecBuilder.create(
			i -> i.group(
							Codec.FLOAT.optionalFieldOf("drag_fraction", 0.93f).forGetter(Levitating::dragFraction),
							ParticleTypes.CODEC.lenientOptionalFieldOf("particle").forGetter(Levitating::particle))
					.apply(i, Levitating::new));

	public static final Levitating DEFAULT = new Levitating(0.93f, Optional.empty());
	public static final Levitating END_STONE = new Levitating(0.85f, Optional.empty());
	public static final Levitating LEVITITE = new Levitating(0.93f,
			Optional.of(new LevititeSparkleParticleData(LevititeSparkleParticleData.LEVITITE_GREEN)));
	public static final Levitating PEARLESCENT_LEVITITE = new Levitating(0.93f,
			Optional.of(new LevititeSparkleParticleData(LevititeSparkleParticleData.LEVITITE_PINK)));
}
