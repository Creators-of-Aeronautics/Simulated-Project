package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.*;

public class LevititeCrystallizerManager {
	private static final Map<LevelAccessor, List<LevititeBlendTicker>> tickers = new HashMap<>();
	private static final Map<LevelAccessor, Map<BlockPos, Integer>> tickedPositionCounts = new HashMap<>();
	private static final List<LevititeBlendTicker> queuedTickers = new ArrayList<>();

	public static void tick(final Level level) {
		final List<LevititeBlendTicker> levelTickers = tickers.get(level);
		if (levelTickers != null) {
			final List<BlockPos> removedPositions = new ArrayList<>();
			levelTickers.removeIf(ticker -> {
				final boolean remove = ticker.tick();
				if (remove) {
					removedPositions.add(ticker.getPos());
				}
				return remove;
			});
			removedPositions.forEach(pos -> removeTickedPosition(level, pos));
		}

		addQueued(level);
	}

	private static void addQueued(final Level level) {
		final CrystallizationWorldSaveData data = CrystallizationWorldSaveData.get((ServerLevel) level);

		final Set<BlockPos> tickedPositions = getTickedPositions(level);
		final List<LevititeBlendTicker> levelTickers = getOrCreateTickers(level);

		for (final LevititeBlendTicker queuedTicker : queuedTickers) {
			if (tickedPositions.contains(queuedTicker.getPos())) {
				continue;
			}

			levelTickers.add(queuedTicker);
			addTickedPosition(level, queuedTicker.getPos());
			queuedTicker.getContext().onCrystallizationInitialize(level, queuedTicker.getPos(), queuedTicker.isDormant);
			data.setDirty();
		}

		queuedTickers.clear();
	}

	/**
	 * @usage Should only be called when adding *new* entries to the ticker group
	 */
	public static void addTicker(final Level level, final BlockPos pos, final int delay, final boolean requiresCatalyst, final boolean skipDormant, final CrystalPropagationContext context) {
		queuedTickers.add(new LevititeBlendTicker(delay, pos, level, requiresCatalyst, skipDormant, context));
	}

	public static boolean isTickedPosition(final Level level, final BlockPos pos) {
		getOrCreateTickers(level);
		return getOrCreateTickedPositionCounts(level).containsKey(pos);
	}

	public static Set<BlockPos> getTickedPositions(final Level level) {
		getOrCreateTickers(level);
		return new HashSet<>(getOrCreateTickedPositionCounts(level).keySet());
	}

	public static void saveData(final ListTag list, final Level level) {
		if (tickers.containsKey(level)) {
			for (final LevititeBlendTicker ticker : tickers.get(level)) {
				list.add(ticker.serialize());
			}
		}
	}

	public static void loadData(final CompoundTag tag, final Level level) {
		final ListTag data = tag.getList("Levitite Manager Data", Tag.TAG_COMPOUND);
		final List<LevititeBlendTicker> newTickers = new ArrayList<>();
		for (int i = 0; i < data.size(); i++) {
			newTickers.add(new LevititeBlendTicker(data.getCompound(i), level));
		}

		tickers.put(level, newTickers);
		rebuildTickedPositions(level, newTickers);
	}

	public static void clearLevel(final LevelAccessor level) {
		tickers.remove(level);
		tickedPositionCounts.remove(level);
	}

	private static List<LevititeBlendTicker> getOrCreateTickers(final LevelAccessor level) {
		return tickers.computeIfAbsent(level, key -> new ArrayList<>());
	}

	private static Map<BlockPos, Integer> getOrCreateTickedPositionCounts(final LevelAccessor level) {
		return tickedPositionCounts.computeIfAbsent(level, key -> new HashMap<>());
	}

	private static void addTickedPosition(final LevelAccessor level, final BlockPos pos) {
		getOrCreateTickedPositionCounts(level).merge(pos, 1, Integer::sum);
	}

	private static void removeTickedPosition(final LevelAccessor level, final BlockPos pos) {
		getOrCreateTickedPositionCounts(level).computeIfPresent(pos, (key, count) -> count > 1 ? count - 1 : null);
	}

	private static void rebuildTickedPositions(final LevelAccessor level, final List<LevititeBlendTicker> levelTickers) {
		final Map<BlockPos, Integer> counts = new HashMap<>();
		for (final LevititeBlendTicker ticker : levelTickers) {
			counts.merge(ticker.getPos(), 1, Integer::sum);
		}
		tickedPositionCounts.put(level, counts);
	}
}
