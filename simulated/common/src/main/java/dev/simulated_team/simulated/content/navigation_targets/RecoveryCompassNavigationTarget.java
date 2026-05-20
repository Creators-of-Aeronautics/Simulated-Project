package dev.simulated_team.simulated.content.navigation_targets;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RecoveryCompassNavigationTarget implements NavigationTarget {
	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final String lastPlayer = self.getComponents().get(SimDataComponents.COMPASS_PLACER_UUID);
		if(lastPlayer != null) {
			final UUID uuid = UUID.fromString(lastPlayer);
			final Player player = navBE.getLevel().getPlayerByUUID(uuid);
			if(player == null) {
                return null;
            }

			if(player.getLastDeathLocation().isEmpty()) {
                return null;
            }

			final ResourceKey<Level> dimension = navBE.getLevel().dimension();
			final GlobalPos globalPos = player.getLastDeathLocation().get();
			if(!globalPos.dimension().equals(dimension)) {
                return null;
            }

			return globalPos.pos().getCenter();
		}

		return null;
	}

	@Override
	public void onInsert(final ItemStack itemStack, final NavTableBlockEntity be, final Player player) {
		itemStack.applyComponents(DataComponentMap.builder()
				.set(SimDataComponents.COMPASS_PLACER_UUID, player.getUUID().toString())
				.build());
	}

	@Override
	public java.util.Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
		final String placer = self.getComponents().get(SimDataComponents.COMPASS_PLACER_UUID);
		if (placer == null) {
			return java.util.Map.of();
		}
		return java.util.Map.of("placer", placer);
	}
}
