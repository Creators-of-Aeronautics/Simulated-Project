package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.SavedBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasHolder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServerBalloon extends Balloon {
    private final Map<LiftingGasType, LiftingGasData> gasAmounts = new Object2ObjectOpenHashMap<>();

    // Physics
    private final Matrix3d outerProduct = new Matrix3d();

    private final Matrix3d translatedOuterProduct = new Matrix3d();

    private final Vector3d averagePosition = new Vector3d();
    private final Vector3d translatedAveragePosition = new Vector3d();

    /**
     * The origin vector used for the average position & matrices
     */
    private final Vector3d physicsOrigin;

    // Fill
    private double totalLift;
    private double totalFilledVolume;
    private double totalTargetVolume;
    private double totalVolumeChange;
    private boolean leaking = false;

    @ApiStatus.Internal
    public ServerBalloon(final Level level, final LevelAccelerator accelerator, final BlockPos controllerPos, final BalloonLayerGraph graph, final ObjectArrayList<BlockEntityLiftingGasProvider> heaters) {
        super(level, accelerator, controllerPos, graph, heaters);
        this.physicsOrigin = new Vector3d(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ());
        this.onRebuilt();
    }

    public void translateMatrices() {
        this.translatedOuterProduct.set(this.outerProduct);
        SableMathUtils.fmaOuterProduct(this.averagePosition, this.averagePosition, -this.getCapacity(), this.translatedOuterProduct);
    }

    protected void checkHeaters() {
        super.checkHeaters();

        for (final LiftingGasData data : this.gasAmounts.values()) {
            data.target = 0;
        }

        if (this.leaking) {
            return;
        }

        for (final BlockEntityLiftingGasProvider heater : this.heaters) {
            this.gasAmounts.compute(heater.getLiftingGasType(), (k, v) -> {
                if (v == null) {
                    v = new LiftingGasData();
                }

                v.target += heater.getGasOutput();
                return v;
            });
        }
    }
    static final Vector3d force = new Vector3d();
    static final Vector3d torque = new Vector3d();
    static final Vector3d localAveragePosition = new Vector3d();
    static final Vector3d worldCenter = new Vector3d();
    static final Vector3d gradient = new Vector3d();
    static final Vector3d gravity = new Vector3d();
    public void applyForces(final double timeStep) {
        final int capacity = this.getCapacity();
        if (capacity <= 0) return;

        final ServerSubLevel subLevel = (ServerSubLevel) Sable.HELPER.getContaining(this.level, this.controllerPos);

        if (subLevel == null || this.totalFilledVolume == 0) {
            return;
        }

        this.translateMatrices();

        final Level level = subLevel.getLevel();
        final Pose3d pose = subLevel.logicalPose();

        this.translatedAveragePosition.set(this.averagePosition)
                .add(this.physicsOrigin);

        // calculate and impart appropriate forces onto associated sub-level

        localAveragePosition.set(this.translatedAveragePosition).sub(pose.rotationPoint());
        worldCenter.set(localAveragePosition);
        pose.orientation().transform(worldCenter).add(pose.position());

        DimensionPhysicsData.getGravity(level, worldCenter,gravity);
        pose.orientation().transformInverse(gravity);
        final double pressure = DimensionPhysicsData.getAirPressure(level, worldCenter);
        if(pressure < 1E-5 || gravity.lengthSquared() == 0)
            return;

        //get gradient in local space
        final double diff = 0.1;

        final double pressureX = DimensionPhysicsData.getAirPressure(level, gradient.set(diff, 0, 0).add(worldCenter)) - pressure;
        final double pressureY = DimensionPhysicsData.getAirPressure(level, gradient.set(0, diff, 0).add(worldCenter)) - pressure;
        final double pressureZ = DimensionPhysicsData.getAirPressure(level, gradient.set(0, 0, diff).add(worldCenter)) - pressure;
        gradient.set(pressureX, pressureY, pressureZ).div(diff);
        pose.orientation().transformInverse(gradient);

        //compute force and torque
        force.set(gravity).mul(-this.totalLift);
        torque.set(localAveragePosition).mul(pressure);
        torque.fma(1.0/capacity,this.translatedOuterProduct.transform(gradient));
        torque.cross(force);
        force.mul(pressure);

        // torque and force are both in momentum units, let's get them into acceleration
        force.mul(timeStep);
        torque.mul(timeStep);

        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.BALLOON_LIFT.get());
        forceGroup.getForceTotal().applyLinearAndAngularImpulse(force, torque);

        if (subLevel.isTrackingIndividualQueuedForces()) {
            forceGroup.recordPointForce(new Vector3d(this.translatedAveragePosition).fma(1/(pressure*capacity),gradient), new Vector3d(force));
        }
    }
    static final Vector3d POSITION_TEMP = new Vector3d();
    @Override
    protected void onRebuilt() {
        this.outerProduct.zero();
        this.averagePosition.zero();

        for (final List<BalloonLayerData> layers : this.graph.getAllLayers()) {
            for (final BalloonLayerData layer : layers) {
                final Iterator<BlockPos> iter = layer.nonSolidBlockIterator();

                while (iter.hasNext()) {
                    final BlockPos pos = iter.next();

                    POSITION_TEMP.set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).sub(this.physicsOrigin);
                    this.averagePosition.add(POSITION_TEMP);
                }
            }
        }

        this.averagePosition.div(this.getCapacity());

        for (final List<BalloonLayerData> layers : this.graph.getAllLayers()) {
            for (final BalloonLayerData layer : layers) {
                final Iterator<BlockPos> iter = layer.nonSolidBlockIterator();

                while (iter.hasNext()) {
                    final BlockPos pos = iter.next();
                    final int x = pos.getX();
                    final int y = pos.getY();
                    final int z = pos.getZ();

                    POSITION_TEMP.set(x + 0.5, y + 0.5, z + 0.5).sub(this.physicsOrigin);
                    SableMathUtils.fmaOuterProduct(POSITION_TEMP,POSITION_TEMP,1,this.outerProduct);
                }
            }
        }

        this.leaking = false;
    }

    @Override
    protected void onHotAirAdded(final BlockPos pos) {

        POSITION_TEMP.set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                .sub(this.physicsOrigin);

        this.averagePosition.mul(this.getCapacity() - 1)
                .add(POSITION_TEMP)
                .div(this.getCapacity());
        SableMathUtils.fmaOuterProduct(POSITION_TEMP,POSITION_TEMP,1,this.outerProduct);
    }

    @Override
    protected void onHotAirRemoved(final BlockPos pos) {

        POSITION_TEMP.set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                .sub(this.physicsOrigin);

        this.averagePosition.mul(this.getCapacity() + 1)
                .sub(POSITION_TEMP)
                .div(this.getCapacity());
        SableMathUtils.fmaOuterProduct(POSITION_TEMP,POSITION_TEMP,-1,this.outerProduct);
    }

    @Override
    protected void onHotAirRemoved(final Iterable<BlockPos> iterable) {
        super.onHotAirRemoved(iterable);


        for (final BlockPos blockPos : iterable) {
            final int y = blockPos.getY();
            final int x = blockPos.getX();
            final int z = blockPos.getZ();

            POSITION_TEMP.set(x + 0.5, y + 0.5, z + 0.5).sub(this.physicsOrigin);

            this.averagePosition.mul(this.capacity)
                    .sub(POSITION_TEMP)
                    .div(this.capacity - 1);

            SableMathUtils.fmaOuterProduct(POSITION_TEMP,POSITION_TEMP,-1,this.outerProduct);

            this.capacity--;
        }
    }

    @Override
    public boolean isValid() {
        return this.totalTargetVolume > 0.05 || this.totalFilledVolume > 0.05;
    }

    public void tick() {
        super.tick();
        this.updateGasAmounts();
    }

    public void updateGasAmounts() {
        final int capacity = this.getCapacity();

        // we'll allow the balloon to temporarily be over the capacity
        // so that situations such as nuking half of the balloon won't cause instant changes in lift

        // sum total target
        this.totalTargetVolume = 0;
        for (final LiftingGasData data : this.gasAmounts.values()) {
            this.totalTargetVolume += data.target;
        }

        // get nudges
        final double scale = Math.min(capacity / this.totalTargetVolume, 1);
        double totalDesiredVolume = 0;
        for (final Map.Entry<LiftingGasType, LiftingGasData> entry : this.gasAmounts.entrySet()) {
            final LiftingGasData data = entry.getValue();
            final LiftingGasType type = entry.getKey();
            final double diff = data.target * scale - data.amount;
            data.nudge = diff > 0 ? diff / type.getFillingTime() : (diff < 0 ? diff / type.getEmptyingTime() : 0);

            if (type.getResponsivenessAdjustmentFactor() > 0 && type.getResponsivenessAdjustmentRange() > 0) {
                final double x = diff / (capacity * type.getResponsivenessAdjustmentRange());
                data.nudge *= 1 + type.getResponsivenessAdjustmentFactor() / (1 + 3 * x * x);
            }

            totalDesiredVolume += data.amount + data.nudge;
        }

        // apply nudges and calculate total lift
        this.totalLift = 0;
        this.totalFilledVolume = 0;
        this.totalVolumeChange = 0;

        for (final Map.Entry<LiftingGasType, LiftingGasData> entry : this.gasAmounts.entrySet()) {
            final LiftingGasData data = entry.getValue();
            data.amount += data.nudge;
            this.totalLift += data.amount * entry.getKey().getLiftStrength();
            this.totalFilledVolume += data.amount;
            this.totalVolumeChange += data.nudge;
        }

        this.totalTargetVolume = Math.min(this.totalTargetVolume, capacity);
    }

    @Override
    public void merge(final Balloon other) {
        super.merge(other);

        if (other instanceof final ServerBalloon otherServerBalloon) {
            for (final Map.Entry<LiftingGasType, LiftingGasData> entry : otherServerBalloon.gasAmounts.entrySet()) {
                final LiftingGasType type = entry.getKey();
                final LiftingGasData data = entry.getValue();

                this.gasAmounts.computeIfAbsent(type, x -> new LiftingGasData()).amount += data.amount;
            }
        }
    }

    @Override
    public void setLeaking() {
        this.leaking = true;
    }

    public Vec3 getCenter() {
        return JOMLConversion.toMojang(this.averagePosition).add(this.physicsOrigin.x(), this.physicsOrigin.y(), this.physicsOrigin.z());
    }

    public double getTotalLift() {
        return this.totalLift;
    }

    public double getTotalFilledVolume() {
        return this.totalFilledVolume;
    }

    public double getTotalTargetVolume() {
        return this.totalTargetVolume;
    }

    public double getTotalVolumeChange() {
        return this.totalVolumeChange;
    }

    @Override
    public boolean shouldSpawnGust(final BlockPos pos) {
        final float percentHeight = (pos.getY() + 0.5f - this.bounds.minY) / this.getHeight();
        return percentHeight > 1.0 - Math.clamp(this.totalFilledVolume / this.getCapacity(), 0, 1);
    }

    @Override
    public void spawnGust(final Level level, final BlockPos pos, final Direction dir) {
        int contributingGases = 0;
        for (final LiftingGasHolder liftingGasHolder : this.getLiftingGasHolders()) {
            if (liftingGasHolder.data().amount > 0) {
                contributingGases++;
            }
        }

        if (contributingGases == 0) {
            return;
        }

        boolean canSpawnGust = true;
        final double nudge = 1d / contributingGases;
        for (final LiftingGasHolder liftingGasHolder : this.getLiftingGasHolders()) {
            if (liftingGasHolder.data().amount < nudge) {
                canSpawnGust = false;
                liftingGasHolder.data().amount = 0;
            } else {
                liftingGasHolder.data().amount -= nudge;
            }
        }
        if (canSpawnGust) {
            super.spawnGust(level, pos, dir);
        }
    }

    @Override
    public void setAssembling(final SubLevelAssemblyHelper.AssemblyTransform transform) {
        super.setAssembling(transform);
        this.physicsOrigin.set(this.controllerPos.getX(), this.controllerPos.getY(), this.controllerPos.getZ());
    }

    public void loadFrom(final SavedBalloon unloaded) {
        for (final LiftingGasHolder entry : unloaded.gasData()) {
            final LiftingGasType type = entry.type();
            final LiftingGasData data = entry.data();

            this.gasAmounts.put(type, data);
        }
    }

    public List<LiftingGasHolder> getLiftingGasHolders() {
        final List<LiftingGasHolder> holders = new ObjectArrayList<>();

        for (final Map.Entry<LiftingGasType, LiftingGasData> entry : this.gasAmounts.entrySet()) {
            holders.add(new LiftingGasHolder(entry.getKey(), entry.getValue()));
        }

        return holders;
    }
}
