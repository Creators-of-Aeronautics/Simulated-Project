package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * A structure filled with hot air, and enclosed with #aeronautics:airtight blocks
 */
public abstract class Balloon {
    protected final Level level;
    /**
     * All heaters currently heating this balloon
     */
    protected final Set<BlockEntityLiftingGasProvider> heaters;
    /**
     * The rough bounds (larger than the actual bounds of purely the hot air blocks)
     */
    protected final BoundingBox3i bounds = new BoundingBox3i();
    /**
     * The level (accelerator) this balloon is in
     */
    protected final LevelAccelerator accelerator;
    /**
     * The graph representing the volume of the balloon
     */
    protected BalloonLayerGraph graph;
    /**
     * The "controller" position that originated the floodfill
     */
    protected BlockPos controllerPos;
    protected int capacity;
    private boolean assembling;

    protected Balloon(final Level level,
                      final LevelAccelerator accelerator,
                      final BlockPos controllerPos,
                      final BalloonLayerGraph graph,
                      final ObjectArrayList<BlockEntityLiftingGasProvider> heaters) {
        this.level = level;
        this.accelerator = accelerator;
        this.controllerPos = controllerPos;
        this.graph = graph;
        this.heaters = new ObjectOpenHashSet<>(heaters);
        this.recomputeBalloonData();
    }

    public boolean tick() {
        boolean changed = false;

        if (this.assembling) {
            this.rebuildBalloonFromController();
            this.assembling = false;
            changed = true;
        }

        return this.checkHeaters() || changed;
    }

    protected boolean checkHeaters() {
        final Iterator<BlockEntityLiftingGasProvider> iterator = this.heaters.iterator();

        BlockPos highestPos = null;
        boolean changed = false;

        final SubLevel balloonSubLevel = Sable.HELPER.getContaining(this.level, this.controllerPos);
        while (iterator.hasNext()) {
            final BlockEntityLiftingGasProvider heater = iterator.next();

            final BlockPos heaterPos = heater.getCastPosition();

            if (!heater.canOutputGas() || heaterPos == null || Sable.HELPER.getContaining(this.level, heaterPos) != balloonSubLevel) {
                heater.setBalloon(null);
                iterator.remove();
                changed = true;
                continue;
            }

            if (highestPos == null) highestPos = heaterPos;

            if (heaterPos.getY() > highestPos.getY()) {
                highestPos = heaterPos;
            }
        }

        if (highestPos != null) {
            changed |= this.moveController(highestPos);
        }

        return this.splitHeaters() || changed;
    }

    /**
     * Splits heaters which are controlling blocks outside of this balloon off of this balloon
     * (in-case the region was split, or something of the nature)
     */
    private boolean splitHeaters() {
        final Iterator<BlockEntityLiftingGasProvider> iterator = this.heaters.iterator();
        boolean changed = false;

        while (iterator.hasNext()) {
            final BlockEntityLiftingGasProvider heater = iterator.next();

            final BlockPos heaterPos = heater.getCastPosition();
            assert heaterPos != null;

            // split heaters out
            if (!this.graph.hasBlockAt(heaterPos)) {
                heater.setBalloon(null);
                iterator.remove();
                changed = true;
            }
        }

        return changed;
    }

    public void onSolidBlockAdded(final BlockPos pos) {
        if (this.assembling) {
            return;
        }
        final BalloonLayerData layer = this.graph.getLayerAt(pos);

        if (layer != null) {
            final int x = pos.getX();
            final int z = pos.getZ();

            if (!layer.getSolidBlock(x, z)) {
                layer.addSolidBlock(x, z);
                layer.solidCount++;
                this.capacity--;

                this.onHotAirRemoved(pos);
            }
        }
    }

    public void onSolidBlockRemoved(final BlockPos pos) {
        final BalloonLayerData layer = this.graph.getLayerAt(pos);

        if (layer != null) {
            final int x = pos.getX();
            final int z = pos.getZ();

            if (layer.getSolidBlock(x, z)) {
                layer.removeSolidBlock(x, z);
                layer.solidCount--;
                this.capacity++;

                this.onHotAirAdded(pos);
            }
        }
    }

    public void onAirtightBlockRemoved(final BlockPos pos) {
        if (this.assembling)
            return;

        final BlockPos.MutableBlockPos adjacentPos = new BlockPos.MutableBlockPos();
        final boolean shouldSpawnGust = this.shouldSpawnGust(pos);
        boolean gusted = false;

        for (final Direction dir : SimDirectionUtil.VALUES) {
            adjacentPos.setWithOffset(pos, dir);

            final BalloonLayerData layer = this.graph.getLayerAt(adjacentPos);
            if (layer == null) continue;

            layer.removeHotAirBlock(adjacentPos.getX(), adjacentPos.getZ());
            final BalloonLayerGraph result = BalloonBuilder.buildBalloon(this.level, adjacentPos, this.graph);
            layer.addHotAirBlock(adjacentPos.getX(), adjacentPos.getZ());

            if (result == null) {
                // oh no... leak...
                final Iterable<Direction> gustDirs = shouldSpawnGust ? this.findGustDirections(pos) : null;

                if (layer.inwardConnections.isEmpty()) {
                    if (shouldSpawnGust && !gusted) {
                        for (final Direction gustDir : gustDirs) {
                            this.spawnGust(this.level, pos.relative(gustDir), gustDir);
                        }
                    }

                    this.setLeaking();
                    return;
                }

                final int beforeCapacity = this.capacity;
                final Iterable<BalloonLayerData> removedLayers = this.graph.propagateRemoval(layer);

                for (final BalloonLayerData removedLayer : removedLayers) {
                    this.onHotAirRemoved(removedLayer::nonSolidBlockIterator);
                }

                final int lostHotAir = beforeCapacity - this.capacity;

                if (lostHotAir > 0 && shouldSpawnGust && !gusted) {
                    for (final Direction gustDir : gustDirs) {
                        this.spawnGust(this.level, pos.relative(gustDir), gustDir);
                    }
                    gusted = true;
                }

                this.graph.rebuildConnections(this.controllerPos);
                this.recomputeBalloonData();
            } else {
                /*this.graph.propagateRemoval(layer);
                this.graph.rebuildConnections(this.controllerPos);
                needsRebuilding = true;*/
                this.rebuildBalloonFromController();
                return;
            }
        }

        /*if (needsRebuilding) {
            for (final List<BalloonLayerData> layers : this.graph.getAllLayers()) {
                for (final BalloonLayerData layerLeft : layers) {
                    layerLeft.setState(BalloonLayerData.State.NEEDS_DOWN_PASS);
                }
            }

            CyanBalloonBuilder.completeBalloon(this.accelerator, this.graph, null);

            this.graph.rebuildConnections(this.controllerPos);
            this.recomputeBalloonData();
            this.onRebuilt();
        }*/
    }

    private Iterable<Direction> findGustDirections(final BlockPos pos) {
        final List<Direction> directions = new ObjectArrayList<>();

        for (final Direction dir : SimDirectionUtil.VALUES) {
            final Direction oppositeDir = dir.getOpposite();

            final BlockState state = this.accelerator.getBlockState(pos.relative(oppositeDir));
            if ((state.isAir() || !state.is(AeroTags.BlockTags.AIRTIGHT)) && this.graph.hasBlockAt(pos.relative(dir))) {
                directions.add(oppositeDir);
            }
        }

        return directions;
    }

    public void onAirtightBlockAdded(final BlockPos pos) {
        if (this.assembling)
            return;

        this.rebuildBalloonFromController();
    }

    public abstract boolean shouldSpawnGust(final BlockPos pos);

    public void spawnGust(final Level level, final BlockPos pos, final Direction dir) {
        GustEntity.addGust(level, pos, dir);
    }

    public void setLeaking() {

    }

    protected void onRebuilt() {

    }

    protected void onHotAirAdded(final BlockPos blockPos) {

    }

    protected void onHotAirRemoved(final BlockPos blockPos) {

    }

    protected void onHotAirRemoved(final Iterable<BlockPos> blockPos) {

    }

    protected void onHotAirAdded(final Iterable<BlockPos> hotAir) {

    }

    /**
     * Moves the controller to a new position, rebuilding the region
     * @param newControllerPos the position to move the controller to
     */
    public boolean moveController(final BlockPos newControllerPos) {
        if (this.controllerPos.equals(newControllerPos))
            return false;

        final boolean needsRebuild = !Objects.equals(this.graph.getLayerAt(newControllerPos), this.graph.getLayerAt(this.controllerPos));
        this.controllerPos = newControllerPos;

        if (needsRebuild)
            this.rebuildBalloonFromController();

        return true;
    }

    protected void rebuildBalloonFromController() {
        final BalloonLayerGraph result = BalloonBuilder.buildBalloon(this.level, this.controllerPos, null);

        if (result == null) {
            this.setLeaking();
            return;
        }

        this.graph = result;
        this.recomputeBalloonData();
        this.onRebuilt();
    }

    private void recomputeBalloonData() {
        assert this.graph != null;

        this.bounds.minY = this.graph.getMinY();
        this.bounds.maxY = this.graph.getMaxY();

        this.bounds.minX = this.controllerPos.getX();
        this.bounds.maxX = this.controllerPos.getX();

        this.bounds.minZ = this.controllerPos.getZ();
        this.bounds.maxZ = this.controllerPos.getZ();

        this.capacity = 0;

        for (int y = this.graph.getMinY(); y <= this.graph.getMaxY(); y++) {
            final List<BalloonLayerData> layers = this.graph.getLayersAtY(y);

            for (final BalloonLayerData layer : layers) {
                this.capacity += layer.hotAirCount;
                this.capacity -= layer.solidCount;

                for (final long chunkLong : layer.getChunks().keySet()) {
                    final int chunkOriginX = BalloonLayerData.getChunkX(chunkLong) << BalloonLayerData.LAYER_CHUNK_SHIFT;
                    final int chunkOriginZ = BalloonLayerData.getChunkZ(chunkLong) << BalloonLayerData.LAYER_CHUNK_SHIFT;

                    this.bounds.expandTo(chunkOriginX, y, chunkOriginZ);
                    this.bounds.expandTo(chunkOriginX + 7, y, chunkOriginZ + 7);
                }
            }
        }
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void addHeater(final BlockEntityLiftingGasProvider heater) {
        this.heaters.add(heater);
    }

    public void removeHeater(final BlockEntityLiftingGasProvider heater) {
        this.heaters.remove(heater);
    }

    public Collection<BlockEntityLiftingGasProvider> getHeaters() {
        return this.heaters;
    }

    /**
     * @return the height of the balloon
     */
    public float getHeight() {
        return this.bounds.maxY - this.bounds.minY + 1;
    }

    public abstract boolean isValid();

    public void onRemoved() {
        for (final BlockEntityLiftingGasProvider heater : this.heaters) {
            heater.setBalloon(null);
        }
    }

    /**
     * @param other the other balloon merging into us
     */
    public void merge(final Balloon other) {
        this.heaters.addAll(other.heaters);

        for (final BlockEntityLiftingGasProvider heater : other.heaters) {
            heater.setBalloon(this);
        }
    }

    public BalloonLayerGraph getGraph() {
        return this.graph;
    }

    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    public BoundingBox3ic getBounds() {
        return this.bounds;
    }

    public void setAssembling(final SubLevelAssemblyHelper.AssemblyTransform transform) {
        this.assembling = true;
        this.controllerPos = transform.apply(this.controllerPos);

        for (final BlockEntityLiftingGasProvider heater : this.heaters) {
            heater.setBalloon(null);
        }

        this.heaters.clear(); // let the heaters re-join

        final BlockPos minPos = transform.apply(new BlockPos(this.bounds.minX(), this.bounds.minY(), this.bounds.minZ()));
        final BlockPos maxPos = transform.apply(new BlockPos(this.bounds.maxX(), this.bounds.maxY(), this.bounds.maxZ()));

        this.bounds.set(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
    }

    public boolean isAssembling() {
        return this.assembling;
    }
}
