package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BalloonMap {
    public static WorldAttached<BalloonMap> MAP = new WorldAttached<>(BalloonMap::new);

    private final Level level;
    private final ObjectSet<Balloon> balloons = new ObjectOpenHashSet<>();
    private final ObjectSet<SavedBalloon> unloadedBalloons = new ObjectOpenHashSet<>();
    private boolean initialized;

    public BalloonMap(final LevelAccessor level) {
        this.level = (Level) level;
    }

    public static void tick(final Level level) {
        MAP.get(level).tick();
    }

    public static void physicsTick(final ServerLevel level, final double timeStep) {
        final BalloonMap handler = MAP.get(level);

        for (final Balloon balloon : handler.balloons) {
            if (balloon instanceof final ServerBalloon serverBalloon)
                serverBalloon.applyForces(timeStep);
        }
    }

    public static SavedBalloon saveBalloon(final ServerBalloon balloon) {
        return new SavedBalloon(new BoundingBox3i(balloon.getBounds()), balloon.getControllerPos(), balloon.getLiftingGasHolders());
    }

    /**
     * Adds a balloon to the map
     *
     * @param balloon the balloon to add
     */
    public void addBalloon(final Balloon balloon) {
        this.balloons.add(balloon);
        this.markDirty();
    }

    public void markDirty() {
        if (this.level instanceof final ServerLevel serverLevel)
            BalloonLevelSavedData.get(serverLevel).setDirty();
    }

    /**
     * Updates nearby balloons with a block-state change
     */
    public void updateNearbyBalloons(final BlockPos blockPos, final BlockState oldState, final BlockState newState) {
        final boolean oldAirtight = oldState.is(AeroTags.BlockTags.AIRTIGHT);
        final boolean newAirtight = newState.is(AeroTags.BlockTags.AIRTIGHT);

        if (oldAirtight != newAirtight) {
            boolean changed = false;
            for (final Balloon balloon : this.getBalloonsNear(blockPos)) {
                if (newAirtight)
                    balloon.onAirtightBlockAdded(blockPos);
                else
                    balloon.onAirtightBlockRemoved(blockPos);
                changed = true;
            }

            if (changed) {
                this.markDirty();
            }
            return;
        }

        final boolean oldSolid = BalloonBuilder.isSolid(oldState);
        final boolean newSolid = BalloonBuilder.isSolid(newState);

        if (oldSolid != newSolid) {
            boolean changed = false;
            for (final Balloon balloon : this.getBalloonsNear(blockPos)) {
                if (newSolid)
                    balloon.onSolidBlockAdded(blockPos);
                else
                    balloon.onSolidBlockRemoved(blockPos);
                changed = true;
            }

            if (changed) {
                this.markDirty();
            }
        }
    }

    public void tick() {
        if (!this.initialized) {
            if (this.level instanceof final ServerLevel serverLevel)
                BalloonLevelSavedData.get(serverLevel);

            this.initialized = true;
        }

        boolean changed = false;
        for (final Balloon balloon : this.balloons) {
            changed |= balloon.tick();
        }

        changed |= this.removeBalloons();
        changed |= this.mergeBalloons();

        if (changed) {
            this.markDirty();
        }
    }

    /**
     * Merges balloons that hold heaters that hold eachothers controller positions
     */
    private boolean mergeBalloons() {
        if (this.balloons.size() < 2) {
            return false;
        }

        final Long2ObjectOpenHashMap<ObjectList<Balloon>> controllersByLayerChunk = new Long2ObjectOpenHashMap<>();
        for (final Balloon balloon : this.balloons) {
            controllersByLayerChunk.computeIfAbsent(getLayerChunkKey(balloon.getControllerPos()), key -> new ObjectArrayList<>())
                    .add(balloon);
        }

        final ObjectIterator<Balloon> iter = this.balloons.iterator();
        boolean changed = false;

        while (iter.hasNext()) {
            final Balloon balloon = iter.next();
            final Balloon otherBalloon = this.findMergeTarget(balloon, controllersByLayerChunk);

            if (otherBalloon != null) {
                // the balloons intersect. delightful. we shall nuke the first
                balloon.onRemoved();
                otherBalloon.merge(balloon);
                iter.remove();
                removeControllerFromIndex(balloon, controllersByLayerChunk);
                changed = true;
            }
        }

        return changed;
    }

    @Nullable
    private Balloon findMergeTarget(final Balloon balloon,
                                    final Long2ObjectOpenHashMap<ObjectList<Balloon>> controllersByLayerChunk) {
        for (final List<BalloonLayerData> layersAtY : balloon.getGraph().getAllLayers()) {
            for (final BalloonLayerData layer : layersAtY) {
                for (final long chunkLong : layer.getChunks().keySet()) {
                    final int chunkX = BalloonLayerData.getChunkX(chunkLong);
                    final int chunkZ = BalloonLayerData.getChunkZ(chunkLong);
                    final ObjectList<Balloon> candidates = controllersByLayerChunk.get(getLayerChunkKey(chunkX, layer.getYLevel(), chunkZ));

                    if (candidates == null) {
                        continue;
                    }

                    for (final Balloon otherBalloon : candidates) {
                        if (balloon == otherBalloon) {
                            continue;
                        }

                        if (balloon.getBounds().intersects(otherBalloon.getBounds()) &&
                                balloon.getGraph().getLayerAt(otherBalloon.getControllerPos()) != null) {
                            return otherBalloon;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static void removeControllerFromIndex(final Balloon balloon,
                                                  final Long2ObjectOpenHashMap<ObjectList<Balloon>> controllersByLayerChunk) {
        final long key = getLayerChunkKey(balloon.getControllerPos());
        final ObjectList<Balloon> balloons = controllersByLayerChunk.get(key);

        if (balloons == null) {
            return;
        }

        balloons.remove(balloon);

        if (balloons.isEmpty()) {
            controllersByLayerChunk.remove(key);
        }
    }

    private static long getLayerChunkKey(final BlockPos pos) {
        return getLayerChunkKey(pos.getX() >> BalloonLayerData.LAYER_CHUNK_SHIFT, pos.getY(), pos.getZ() >> BalloonLayerData.LAYER_CHUNK_SHIFT);
    }

    private static long getLayerChunkKey(final int chunkX, final int y, final int chunkZ) {
        return BlockPos.asLong(chunkX, y, chunkZ);
    }

    /**
     * Remove non-valid balloons
     */
    private boolean removeBalloons() {
        final ObjectIterator<Balloon> iter = this.balloons.iterator();
        boolean changed = false;

        while (iter.hasNext()) {
            final Balloon balloon = iter.next();

            if (!balloon.isValid()) {
                iter.remove();
                balloon.onRemoved();
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Removes a specific balloon
     * @param balloon the balloon to remove
     */
    public void removeBalloon(final Balloon balloon) {
        if (this.balloons.remove(balloon)) {
            balloon.onRemoved();
            this.markDirty();
        }
    }

    @Nullable
    public Balloon getBalloon(final BlockPos blockPos) {
        for (final Balloon balloon : this.balloons) {
            final BoundingBox3ic bounds = balloon.getBounds();

            if (bounds.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()) &&
                    balloon.getGraph().hasBlockAt(blockPos)) {
                return balloon;
            }
        }

        return null;
    }

    private Iterable<Balloon> getBalloonsNear(final BlockPos blockPos) {
        ObjectList<Balloon> balloons = null;

        final int padding = 24;
        for (final Balloon balloon : this.balloons) {
            final BoundingBox3ic bounds = balloon.getBounds();

            if (blockPos.getX() > bounds.minX() - padding &&
                    blockPos.getY() > bounds.minY() - padding &&
                    blockPos.getZ() > bounds.minZ() - padding &&
                    blockPos.getX() < bounds.maxX() + padding &&
                    blockPos.getY() < bounds.maxY() + padding &&
                    blockPos.getZ() < bounds.maxZ() + padding) {
                if (balloons == null) balloons = new ObjectArrayList<>();
                balloons.add(balloon);
            }
        }

        return balloons != null ? balloons : List.of();
    }

    /**
     * @return all balloons in this map
     */
    public Iterable<Balloon> getBalloons() {
        return this.balloons;
    }

    /**
     * @return if this map is empty, and contains no balloons
     */
    public boolean isEmpty() {
        return this.balloons.isEmpty();
    }

    public void unloadBalloon(final ServerBalloon balloon) {
        final SavedBalloon unloadedBalloon = saveBalloon(balloon);

        this.balloons.remove(balloon);
        this.unloadedBalloons.add(unloadedBalloon);
        this.markDirty();
    }

    public Collection<SavedBalloon> getUnloadedBalloons() {
        return this.unloadedBalloons;
    }

    public static class BalloonSubLevelObserver implements SubLevelObserver {

        private final Level level;

        public BalloonSubLevelObserver(final Level level) {
            this.level = level;
        }

        @Override
        public void onSubLevelRemoved(final SubLevel subLevel, final SubLevelRemovalReason reason) {
            if (reason == SubLevelRemovalReason.REMOVED) {
                final LevelPlot plot = subLevel.getPlot();

                final BalloonMap map = BalloonMap.MAP.get(this.level);
                final Iterator<Balloon> iter = map.getBalloons().iterator();
                boolean changed = false;

                while (iter.hasNext()) {
                    final Balloon balloon = iter.next();

                    if (balloon.isAssembling())
                        continue;

                    final BlockPos controllerPos = balloon.getControllerPos();
                    if (plot.contains(controllerPos.getX(), controllerPos.getZ())) {
                        balloon.onRemoved();
                        iter.remove();
                        changed = true;
                    }
                }

                if (changed) {
                    map.markDirty();
                }
            }
        }
    }
}
