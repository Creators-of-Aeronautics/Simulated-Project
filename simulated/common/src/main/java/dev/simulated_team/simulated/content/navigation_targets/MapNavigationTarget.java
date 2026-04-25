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
import java.util.Locale;
import java.util.Map;

public class MapNavigationTarget implements NavigationTarget {
	@Override
	public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
		final Resolved resolved = resolveNearest(navBE.getLevel(), navBE.getProjectedSelfPos(), self);
		return resolved == null ? null : resolved.pos();
	}

	@Override
	public float getMaxRange() {
		return 0;
	}

	@Override
	public Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
		final Resolved resolved = resolveNearest(be.getLevel(), be.getProjectedSelfPos(), self);
		if (resolved == null) {
			return Map.of();
		}
		return Map.of("kind", resolved.kind().name().toLowerCase(Locale.ROOT));
	}

	private static @Nullable Resolved resolveNearest(final Level level, final Vec3 pos, final ItemStack stack) {
		final MapDecorations decorations = stack.getComponents().get(DataComponents.MAP_DECORATIONS);
		final MapId mapId = stack.getComponents().get(DataComponents.MAP_ID);
		if(decorations == null || mapId == null) {
			return null;
		}

		double closestDist = Double.POSITIVE_INFINITY;
		Vec3 closestPos = null;
		Kind closestKind = null;

		for (final MapDecorations.Entry decoration : decorations.decorations().values()) {
			if(!decoration.type().is(SimTags.Misc.NAV_TABLE_FINDABLE))
				continue;

			final double dist = pos.distanceToSqr(decoration.x(), pos.y(), decoration.z());
			if(dist < closestDist) {
				closestPos = new Vec3(decoration.x(), pos.y(), decoration.z());
				closestDist = dist;
				closestKind = Kind.DECORATION;
			}
		}

		final MapItemSavedData mapData = level.getMapData(mapId);
		final Collection<MapBanner> banners = mapData.getBanners();
		for (final MapBanner banner : banners) {
			final Vec3 bannerPos = banner.pos().getCenter();
			final double dist = pos.distanceToSqr(bannerPos.x(), pos.y(), bannerPos.z());
			if(dist < closestDist) {
				closestPos = bannerPos;
				closestDist = dist;
				closestKind = Kind.BANNER;
			}
		}

		return closestPos == null ? null : new Resolved(closestPos, closestKind);
	}

	private enum Kind { DECORATION, BANNER }

	private record Resolved(Vec3 pos, Kind kind) { }
}
