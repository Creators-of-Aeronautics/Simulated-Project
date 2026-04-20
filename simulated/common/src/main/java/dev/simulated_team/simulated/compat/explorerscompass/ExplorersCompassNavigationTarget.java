package dev.simulated_team.simulated.compat.explorerscompass;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ExplorersCompassNavigationTarget implements NavigationTarget {
	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final Integer x = self.getComponents().get(ExplorersCompass.FOUND_X_COMPONENT);
		final Integer z = self.getComponents().get(ExplorersCompass.FOUND_Z_COMPONENT);
		if (x != null && z != null) {
			final Vec3 pos = navBE.getProjectedSelfPos();
			return new Vec3(x, pos.y(), z);
		}

		return null;
	}

	@Override
	public float getMaxRange() {
		return 0;
	}
}
