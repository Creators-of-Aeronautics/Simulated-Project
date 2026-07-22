package dev.ryanhcode.offroad.content.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.Create;
import dev.ryanhcode.offroad.Offroad;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public record TireLike(float radius, Vec3 rotation, Vec3 offset, Optional<ResourceLocation> model, float minimumFriction) {
    public static final Codec<TireLike> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.FLOAT.optionalFieldOf("radius", 1.0f).forGetter(TireLike::radius),
                    Vec3.CODEC.optionalFieldOf("rotation", new Vec3(90, 0, 0)).forGetter(TireLike::rotation),
                    Vec3.CODEC.optionalFieldOf("offset", new Vec3(0, 0, 0)).forGetter(TireLike::offset),
                    ResourceLocation.CODEC.optionalFieldOf("model").forGetter(TireLike::model),
                    Codec.FLOAT.optionalFieldOf("minimumFriction", 0.0f).forGetter(TireLike::minimumFriction)
            ).apply(i, TireLike::new));

    public TireLike(float radius, Vec3 rotation, Vec3 offset, @Nullable ResourceLocation model, float minimumFriction) {
        this(radius, rotation, offset, Optional.ofNullable(model), minimumFriction);
    }

    public TireLike(float radius, Vec3 rotation, Vec3 offset, @Nullable ResourceLocation model) {
        this(radius, rotation, offset, Optional.ofNullable(model), 0.0f);
    }

    public TireLike(final float radius) {
        this(radius, new Vec3(90, 0, 0), new Vec3(0, 0, 0), Optional.empty(), 0.0f);
    }

    public TireLike(final float radius, final ResourceLocation model) {
        this(radius, new Vec3(90, 0, 0), new Vec3(0, 0, 0), Optional.of(model), 0.0f);
    }

    /*
    public static final TireLike SMALL_TIRE = new TireLike(9.0f / 16.0f, Offroad.path("item/small_tire/block"));
    public static final TireLike TIRE = new TireLike(15.5f / 16.0f, Offroad.path("item/tire/block"));
    public static final TireLike LARGE_TIRE = new TireLike(1.0f + 4.0f / 16.0f, Offroad.path("item/large_tire/block"));
    public static final TireLike MONSTROUS_TIRE = new TireLike(1.0f + 14.0f / 16.0f, Offroad.path("item/monstrous_tire/block"));
    */
    public static final TireLike SMALL_TIRE = new TireLike(12.0f / 16.0f);
    public static final TireLike TIRE = new TireLike(15.5f / 16.0f);
    public static final TireLike LARGE_TIRE = new TireLike(1.0f + 4.0f / 16.0f);
    public static final TireLike MONSTROUS_TIRE = new TireLike(2.0f);
    public static final TireLike CRUSHING_WHEEL = new TireLike(1.0f);
    public static final TireLike WATER_WHEEL = new TireLike(1.0f);
    public static final TireLike FLYWHEEL = new TireLike(1.0f + 6.0f / 16.0f);
    public static final TireLike LARGE_WATER_WHEEL = new TireLike(2.0f + 7.0f / 16.0f);
    public static final TireLike ROCKCUTTING_WHEEL = new TireLike(0.8f, new Vec3(90, 0, 0), Vec3.ZERO, Offroad.path("block/rockcutting_wheel/wheel"), 0.0f);
    public static final TireLike MECHANICAL_ROLLER = new TireLike(0.7f, Vec3.ZERO, new Vec3(0, -0.5f, 0), Create.asResource("block/mechanical_roller/wheel"), 0.0f);
}
