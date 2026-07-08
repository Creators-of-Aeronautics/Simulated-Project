package dev.simulated_team.simulated.multiloader.tanks;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A loader-independent representation of a fluid
 */
public class CFluidType {

    public final Fluid fluid;
    DataComponentMap data;

    public CFluidType(final ResourceLocation type, @Nullable final DataComponentMap data) {
        this.fluid = BuiltInRegistries.FLUID.get(type);
        this.data = normalize(data);
    }

    public CFluidType(final Fluid type, @Nullable final DataComponentMap data) {
        this.fluid = type;
        this.data = normalize(data);
    }

    @Nullable
    private static DataComponentMap normalize(@Nullable final DataComponentMap data) {
        return data == null || data.isEmpty() ? null : data;
    }

    public boolean isBlank() {
        return this.equals(BLANK);
    }

    public static final CFluidType BLANK = new CFluidType(Fluids.EMPTY, null);

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());
        if (this.data != null) {
            final Codec<DataComponentMap> codec = DataComponentMap.CODEC;
            final DataResult<Tag> result = codec.encodeStart(NbtOps.INSTANCE, this.data);

            if (result.isError()) {
                Simulated.LOGGER.warn(result.error().get().message());
            } else {
                tag.put("data", result.result().get());
            }
        }
        return tag;
    }

    public static CFluidType read(final CompoundTag tag) {
        DataComponentMap map = null;
        if (tag.contains("data")) {
            final DataResult<Pair<DataComponentMap, Tag>> result = DataComponentMap.CODEC.decode(NbtOps.INSTANCE, tag.getCompound("data"));
            if (result.isError()) {
                Simulated.LOGGER.warn(result.error().get().message());
            } else {
                map = result.result().get().getFirst();
            }
        }

        return new CFluidType(ResourceLocation.parse(tag.getString("Fluid")), map);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final CFluidType other) {
            return this.fluid.isSame(other.fluid) && componentsMatch(this.data, other.data);
        }
        return false;
    }

    private static boolean componentsMatch(@Nullable final DataComponentMap first, @Nullable final DataComponentMap second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || first.size() != second.size()) {
            return false;
        }
        for (final TypedDataComponent<?> component : first) {
            if (!componentMatches(component, second)) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean componentMatches(final TypedDataComponent<T> component, final DataComponentMap other) {
        return Objects.equals(component.value(), other.get(component.type()));
    }

    @Override
    public int hashCode() {
        int componentHash = 0;
        if (this.data != null) {
            for (final TypedDataComponent<?> component : this.data) {
                componentHash += Objects.hash(component.type(), component.value());
            }
        }
        return 31 * this.fluid.hashCode() + componentHash;
    }
}
