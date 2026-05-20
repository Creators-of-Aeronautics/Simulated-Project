package dev.simulated_team.simulated.content.navigation_targets;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneInformation;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CompassNavigationTarget implements NavigationTarget {

	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final Level level = navBE.getLevel();
		if (self.has(SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER)) {
			final LodestoneTrackingMap map = LodestoneTrackingMap.getOrLoad(level);
			if (map != null) {
				final LodestoneInformation information = map.getInformation(self.get(SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER));
				if (information != null) {
					return JOMLConversion.toMojang(information.projectedPos());
				}
			}
		}

		return level.getSharedSpawnPos().getCenter();
	}

	@Override
	public float getMaxRange() {
		return 0;
	}

	@Override
	public java.util.Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
		if (self.has(SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER)) {
			return java.util.Map.of("source", "lodestone");
		}
		return java.util.Map.of("source", "spawn");
	}
}
