package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * An info wrapper that holds an item type, and its associated component data. Primarly used for Simulated's multiloader inventory structure. <p>
 * In order to generate a wrapper from a given item easily, <b>{@link ItemInfoWrapper#generateFromStack(ItemStack) generateFromStack()}</b> can be used. <p>
 * In order to generate a new item from a given wrapper easily, <b>{@link ItemInfoWrapper#generateFromInfo(ItemInfoWrapper) generateFromInfo()}</b> can be used.
 *
 * @param type     The item type of this wrapper
 * @param patchMap The data components of this wrapper
 */
public record ItemInfoWrapper(Item type, DataComponentPatch patchMap) {

    /**
     * Generates a new wrapper from the given item
     *
     * @param stack The item stack to gather information from.
     * @return A <b>new</b> {@link ItemInfoWrapper} containing the type and data components from the given item.
     */
    public static ItemInfoWrapper generateFromStack(final ItemStack stack) {
        return new ItemInfoWrapper(stack.getItem(), stack.getComponentsPatch());
    }

    /**
     * Generates a new {@link ItemStack} from the given wrapper.
     *
     * @param info The {@link ItemInfoWrapper} to use information from.
     * @return A <b>new</b> {@link ItemStack} containing data from the given wrapper.
     */
    public static @NotNull ItemStack generateFromInfo(final ItemInfoWrapper info) {
        final ItemStack newStack = info.type().getDefaultInstance();
        final DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (final Map.Entry<DataComponentType<?>, Optional<?>> set : info.patchMap().entrySet()) {
            setDataComponent(set.getKey(), set.getValue(), builder);
        }
        newStack.applyComponents(builder.build());
        return newStack;
    }

    private static <T> void setDataComponent(final DataComponentType<?> type, final Optional<?> set, final DataComponentPatch.Builder builder) {
        if (set.isEmpty()) {
            builder.remove(type);
        } else {
            builder.set((DataComponentType<T>) type, (T) set.get());
        }
    }
}
