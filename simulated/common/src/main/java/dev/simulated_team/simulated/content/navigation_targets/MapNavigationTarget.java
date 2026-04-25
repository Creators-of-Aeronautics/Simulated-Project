package dev.simulated_team.simulated.content.navigation_targets;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class MapNavigationTarget implements NavigationTarget {
	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final Level level = navBE.getLevel();
		final Vec3 pos = navBE.getProjectedSelfPos();
		return getNearestDecorationPos(level, pos, self);
	}

	@Override
	public float getMaxRange() {
		return 0;
	}

	private static Vec3 getNearestDecorationPos(final Level level, final Vec3 pos, final ItemStack stack) {
		final MapDecorations decorations = stack.getComponents().get(DataComponents.MAP_DECORATIONS);
		final MapId mapId = stack.getComponents().get(DataComponents.MAP_ID);
		if(decorations != null && mapId != null) {

			double closestDist = Double.POSITIVE_INFINITY;
			Vec3 closestPos = null;
			for (final MapDecorations.Entry decoration : decorations.decorations().values()) {
				if(!decoration.type().is(SimTags.Misc.NAV_TABLE_FINDABLE))
					continue;

				final double dist = pos.distanceToSqr(decoration.x(), pos.y(), decoration.z());
				if(dist < closestDist) {
					closestPos = new Vec3(decoration.x(), pos.y(), decoration.z());
					closestDist = dist;
				}
			}

			final MapItemSavedData mapData = level.getMapData(mapId);
			if (mapData != null) {
				final Collection<MapBanner> banners = mapData.getBanners();
				for (final MapBanner banner : banners) {
					final Vec3 bannerPos = banner.pos().getCenter();
					final double dist = pos.distanceToSqr(bannerPos.x(), pos.y(), bannerPos.z());
					if(dist < closestDist) {
						closestPos = bannerPos;
						closestDist = dist;
					}
				}
			}

			return closestPos;
		}

		return null;
	}
}
