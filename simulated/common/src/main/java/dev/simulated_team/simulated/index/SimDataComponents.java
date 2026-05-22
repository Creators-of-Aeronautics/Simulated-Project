package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class SimDataComponents {
    private static final RegistrationProvider<DataComponentType<?>> REGISTRY = RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, Simulated.MOD_ID);

    public static final DataComponentType<BlockPos> ROPE_FIRST_CONNECTION = register(
            "rope_first_connection",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC)
    );

	public static final DataComponentType<UUID> LODESTONE_COMPASS_SUBLEVEL_TRACKER = register("lodestone_compass_tracker",
			uuidBuilder -> uuidBuilder.persistent(UUIDUtil.CODEC));

    public static final DataComponentType<UUID> COMPASS_PLACER_UUID = register("compass_placer",
            builder -> builder.persistent(UUIDUtil.STRING_CODEC));
    public static final DataComponentType<GlobalPos> LAST_PLAYER_DEATH_LOCATION = register("last_player_death_location",
            builder -> builder.persistent(GlobalPos.CODEC));

    public static final DataComponentType<NavigationTarget> TARGET = register("target", builder -> builder
            .persistent(SimRegistries.NAVIGATION_TARGET.byNameCodec())
            .networkSynchronized(ResourceLocation.STREAM_CODEC
                    .map(SimRegistries.NAVIGATION_TARGET::get, SimRegistries.NAVIGATION_TARGET::getKey))
    );

    private static <T> DataComponentType<T> register(final String name, final UnaryOperator<DataComponentType.Builder<T>> builder) {
        final DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        REGISTRY.register(name, () -> type);
        return type;
    }

    public static void register() {}
}
