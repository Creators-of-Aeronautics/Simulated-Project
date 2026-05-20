package dev.simulated_team.simulated.compat.naturescompass;

import com.chaosthedude.naturescompass.NaturesCompass;
import com.chaosthedude.naturescompass.util.CompassState;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class NaturesCompassNavigationTarget implements NavigationTarget {
	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final Integer x = self.getComponents().get(NaturesCompass.FOUND_X);
		final Integer z = self.getComponents().get(NaturesCompass.FOUND_Z);
		if(x != null && z != null) {
			final Vec3 pos = navBE.getProjectedSelfPos();
			return new Vec3(x, pos.y(), z);
		}

		return null;
	}

	@Override
	public float getMaxRange() {
		return 0;
	}

	@Override
	public Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
		final Map<String, Object> out = new LinkedHashMap<>();

		final String biome = self.getComponents().get(NaturesCompass.BIOME_ID);
		if (biome != null) {
			out.put("biome", biome);
		}

		final Integer stateId = self.getComponents().get(NaturesCompass.COMPASS_STATE);
		if (stateId != null) {
			final CompassState state = CompassState.fromID(stateId);
			if (state != null) {
				out.put("state", state.name().toLowerCase(Locale.ROOT));
			}
		}

		final Integer radius = self.getComponents().get(NaturesCompass.SEARCH_RADIUS);
		if (radius != null) {
			out.put("search_radius", radius);
		}

		return out;
	}
}
