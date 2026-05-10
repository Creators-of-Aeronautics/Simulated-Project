package dev.eriksonn.aeronautics.util;

import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.world.item.ItemStack;

public class CatalyzerHelper {
    public static boolean isCatalyzer(final ItemStack item) {
        return item.is(AeroTags.ItemTags.LEVITITE_CATALYZER) || item.is(AeroTags.ItemTags.LEVITITE_SOUL_CATALYZER);
    }
}
