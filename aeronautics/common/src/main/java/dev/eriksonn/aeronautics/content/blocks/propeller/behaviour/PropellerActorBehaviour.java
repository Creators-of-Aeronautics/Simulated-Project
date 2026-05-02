package dev.eriksonn.aeronautics.content.blocks.propeller.behaviour;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticle;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class PropellerActorBehaviour extends BlockEntityBehaviour implements IHaveGoggleInformation {
    public static final BehaviourType<PropellerActorBehaviour> TYPE = new BehaviourType<>("prop_behaviour");
    private static final int MAX_ACCELERATION = 5;
    private static final Vector3d STORED_MUT_POS = new Vector3d();
    private static final Vector3d STORED_TRANSFORMED_THRUST = new Vector3d();
    private static final Vector3d TEMP_CLIP_START = new Vector3d(), TEMP_CLIP_END = new Vector3d();
    private static final Vector3d globalThrust = new Vector3d();
    private static final Vector3d relativeDiff = new Vector3d();
    private static final Vector3d normal = new Vector3d();
    private static final Vector3d particleVelocity = new Vector3d();

    protected final BlockEntityPropeller propeller;

    public double updatedParticleAmount;
    public int maxParticleAmount = 20;
    public double particleSmoothing = 10;
    public double radius = 0;

    /**
     * An updater for particle amount called once per tick
     * The actual particle amount is smoothed and clamped using getParticleCount()
     */
    protected Supplier<Double> particleAmountUpdater;
    /**
     * An updater for random particle position called once per spawned particle
     */
    protected BiConsumer<Vector3d, RandomSource> particlePositionUpdater;
    protected List<PropellerLayer> propellerLayers = new ObjectArrayList<>();

    /**
     * The direction of thrust that is used for entity pushing and thrust application
     */
    private Vector3dc thrustDirection;

    public PropellerActorBehaviour(final SmartBlockEntity be, final BlockEntityPropeller propeller) {
        super(be);
        this.propeller = propeller;
    }

    public void addPropellerLayer(final PropellerLayer layer) {
        this.radius = Math.max(this.radius, layer.outerRadius);
        this.propellerLayers.add(layer);
        this.propellerLayers.sort(Comparator.comparingDouble(propellerLayer -> propellerLayer.offset));
    }

    public void addSimpleLayer(final double offset, final double radius) {
        this.addPropellerLayer(new PropellerLayer(offset, 0, radius));
    }

    public List<PropellerLayer> getLayers() {
        return this.propellerLayers;
    }

    @Override
    public void tick() {
        this.updatedParticleAmount = this.particleAmountUpdater.get();

        super.tick();
    }

    /**
     * Pushes entities within the given radius an amount dependent on airflow, thrust, and other parameters. <p>
     */
    public void pushEntities() {
        if (this.propellerLayers.isEmpty())
            return;

        final double thrust = this.propeller.getThrust();
        final Direction direction = this.propeller.getBlockDirection();

        final double thrustFlowMult = Math.signum(thrust);

        // TODO: make this based off of thrust vec
        final Quaternionf quat = direction.getRotation();

        final double dist = this.getParticleRange();
        double radius = 0;
        double offsetMax = Double.MIN_VALUE;
        double offsetMin = Double.MAX_VALUE;
        final double d1 = Math.max(dist, 0);
        final double d0 = Math.min(dist, 0);
        for (final PropellerLayer layer : this.propellerLayers) {
            radius = Math.max(radius, layer.outerRadius);
            offsetMax = Math.max(offsetMax, layer.offset + d1);
            offsetMin = Math.min(offsetMin, layer.offset + d0);
        }

        final Vector3d max = new Vector3d(radius, offsetMax, radius);
        final Vector3d min = new Vector3d(-radius, offsetMin, -radius);

        quat.transform(max);
        quat.transform(min);

        min.add(JOMLConversion.toJOML(this.getPos().getCenter()));
        max.add(JOMLConversion.toJOML(this.getPos().getCenter()));

        final BoundingBox3d aabb = new BoundingBox3d(min.x, min.y, min.z, max.x, max.y, max.z);

        STORED_TRANSFORMED_THRUST.set(this.thrustDirection);
        final SubLevel subLevel = Sable.HELPER.getContaining(this.getWorld(), this.getPos());
        if (subLevel != null) {
            aabb.transform(subLevel.logicalPose(), aabb);

            subLevel.logicalPose().transformNormal(STORED_TRANSFORMED_THRUST);
        }

        final List<Entity> entities = this.getWorld().getEntities(null, aabb.toMojang());
        if (!entities.isEmpty()) {
            for (final Entity entity : entities) {
                if (entity instanceof AbstractContraptionEntity ||
                        AirCurrent.isPlayerCreativeFlying(entity) ||
                        DivingBootsItem.isWornBy(entity)) {
                    continue;
                }

                final Vec3 qc = entity.getBoundingBox().getCenter();
                STORED_MUT_POS.set(qc.x, qc.y, qc.z);

                final Vector3d temp = new Vector3d().set(JOMLConversion.toJOML(this.getPos().getCenter()));
                if (subLevel != null) {
                    subLevel.logicalPose().transformPosition(temp);
                }

                STORED_MUT_POS.sub(temp);

                final double entityDistance = STORED_TRANSFORMED_THRUST.dot(STORED_MUT_POS);
                STORED_MUT_POS.fma(-entityDistance, STORED_TRANSFORMED_THRUST);
                final double radialDistanceSq = STORED_MUT_POS.lengthSquared();
                double layerForceScale = 0;
                double minLayerDistance = 100;
                for (final PropellerLayer layer : this.propellerLayers) {
                    double layerDistance = entityDistance - layer.offset;
                    layerDistance *= thrustFlowMult;
                    if (layerDistance > 0 && radialDistanceSq < layer.outerRadiusSquared()) {
                        final double distanceScale = layerDistance * PropellerAirParticle.frictionScale;
                        double innerRadiusScale = 0;
                        if (layer.innerRadius > 0 && radialDistanceSq < layer.innerRadiusSquared()) {
                            innerRadiusScale = (layer.innerRadiusSquared() - radialDistanceSq) / (layer.innerRadius * layer.outerRadius);
                            innerRadiusScale *= innerRadiusScale * 12;
                        }
                        minLayerDistance = layerDistance;
                        layerForceScale = Math.max(layerForceScale, Math.exp(-distanceScale - innerRadiusScale));
                    }
                }

                if (layerForceScale > 0) {
                    TEMP_CLIP_START.set(qc.x, qc.y, qc.z).fma(thrustFlowMult * -minLayerDistance, STORED_TRANSFORMED_THRUST);
                    TEMP_CLIP_END.set(qc.x, qc.y, qc.z);

                    final Vec3 mojStart = JOMLConversion.toMojang(TEMP_CLIP_START);
                    final Vec3 mojEnd = JOMLConversion.toMojang(TEMP_CLIP_END);
                    final ClipContext ctx = new ClipContext(
                            mojStart,
                            mojEnd,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.ANY,
                            CollisionContext.empty()
                    );

                    if (this.getWorld().clip(ctx).getType() == HitResult.Type.MISS) {
                        final float modifier = entity.isShiftKeyDown() ? 0.125f : 1;
                        // 0.55 is the acceleration required to keep living entities aloft,
                        // and this scaling causes those entities to float at the edge of the particle range at asymptotically high airflows
                        final double forceScale = PropellerAirParticle.frictionScale * PropellerAirParticle.lifeTime * 0.55;
                        final double acceleration = forceScale * this.getAirflowTickSpeed() * modifier * layerForceScale * Math.min(this.getAirPressure(), 1);

                        final Vec3 previousMotion = entity.getDeltaMovement();
                        entity.setDeltaMovement(previousMotion.add(
                                Math.min(Math.max(STORED_TRANSFORMED_THRUST.x() * acceleration - previousMotion.x, -MAX_ACCELERATION), MAX_ACCELERATION) * (1 / 8f),
                                Math.min(Math.max(STORED_TRANSFORMED_THRUST.y() * acceleration - previousMotion.y, -MAX_ACCELERATION), MAX_ACCELERATION) * (1 / 8f),
                                Math.min(Math.max(STORED_TRANSFORMED_THRUST.z() * acceleration - previousMotion.z, -MAX_ACCELERATION), MAX_ACCELERATION) * (1 / 8f)));

                        entity.fallDistance = 0;
                    }
                }
            }
        }
    }

    public double getParticleRange() {
        return Math.signum(this.getAirflowTickSpeed()) * Math.log(Math.abs(this.getAirflowTickSpeed()) * PropellerAirParticle.frictionScale * PropellerAirParticle.lifeTime + 1) / PropellerAirParticle.frictionScale;
    }

    /**
     * gets airflow in units of meters per tick instead of meters per second
     */
    public float getAirflowTickSpeed() {
        final double airflow = this.propeller.getAirflow();
        return (float) (airflow / 20f);
    }

    public float getParticleSpeed() {
        final float speed = this.getAirflowTickSpeed();
        return Math.clamp(speed, -5, 5);
    }

    /**
     * Spawn simple airflow particles
     */
    public void spawnParticles() {
        if (!this.getWorld().isClientSide)
            return;
        if (this.propellerLayers.isEmpty())
            return;

        final double speed = this.getParticleSpeed();

        final Vector3d mutSpeed = new Vector3d();

        final RandomSource random = this.getWorld().getRandom();
        int particleCount = this.getParticleCount();
        final SubLevel subLevel = Sable.HELPER.getContaining(this.getWorld(), this.getPos());
        final Vector3d origin = new Vector3d(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5);

        for (int i = 0; i < particleCount; i++) {
            this.particlePositionUpdater.accept(STORED_MUT_POS, random);
            STORED_MUT_POS.add(origin);
            final double positionNudge = speed * random.nextFloat();
            STORED_MUT_POS.fma(positionNudge, this.thrustDirection);
            this.thrustDirection.mul(speed * Math.exp(-PropellerAirParticle.frictionScale * positionNudge), mutSpeed);

            if (subLevel != null)
                subLevel.logicalPose().transformNormal(mutSpeed);

            this.getWorld().addParticle(new PropellerAirParticleData(true, false),
                    STORED_MUT_POS.x, STORED_MUT_POS.y, STORED_MUT_POS.z,
                    mutSpeed.x, mutSpeed.y, mutSpeed.z);
        }

        particleCount = particleCount / 4 + 1;
        final Vector3d endVector = new Vector3d();
        for (int i = 0; i < particleCount; i++) {
            this.particlePositionUpdater.accept(STORED_MUT_POS, random);
            STORED_MUT_POS.add(origin);
            STORED_MUT_POS.fma(1.3 * this.getParticleRange() * Math.sqrt(random.nextFloat()), this.thrustDirection, endVector);

            if (subLevel != null) {
                subLevel.logicalPose().transformPosition(STORED_MUT_POS);
                subLevel.logicalPose().transformPosition(endVector);
            }
            this.createHitParticle(subLevel, origin, STORED_MUT_POS, endVector);
        }
    }

    private void createHitParticle(final SubLevel subLevel, final Vector3d origin, final Vector3d start, final Vector3d end) {
        final BlockHitResult clip = this.getWorld().clip(new ClipContext(
                JOMLConversion.toMojang(start),
                JOMLConversion.toMojang(end),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                (Entity) null
        ));

        final Vec3 hitPos = clip.getLocation();

        if (clip.getType() != HitResult.Type.MISS && start.distanceSquared(hitPos.x, hitPos.y, hitPos.z) > 1) {
            final BlockState hitState = this.getWorld().getBlockState(clip.getBlockPos());
            final Fluid fluid = this.getWorld().getFluidState(clip.getBlockPos()).getType();

            globalThrust.set(this.thrustDirection);
            relativeDiff.set(origin);
            if (subLevel != null) {
                subLevel.logicalPose().orientation().transform(globalThrust);
                subLevel.logicalPose().transformPosition(relativeDiff);
            }
            normal.set(clip.getDirection().getStepX(), clip.getDirection().getStepY(), clip.getDirection().getStepZ());
            final SubLevel other = Sable.HELPER.getContaining(this.getWorld(), clip.getBlockPos());
            if (other != null)
                other.logicalPose().orientation().transform(normal);

            start.sub(relativeDiff, relativeDiff).div(this.radius);

            this.projectVector(relativeDiff, globalThrust, 1);//project offset onto propeller plane
            this.projectVector(globalThrust, normal, 1.2);//reflect thrust vector along normal
            this.projectVector(relativeDiff, normal, 1);//project offset onto tangent plane

            relativeDiff.mul(Math.signum(-this.getAirflowTickSpeed()));
            globalThrust.sub(relativeDiff, particleVelocity).mul(this.getAirflowTickSpeed() * 0.8);

            if (other != null)
                other.logicalPose().orientation().transformInverse(particleVelocity);

            this.getWorld().addParticle(ParticleTypes.DUST_PLUME, hitPos.x, hitPos.y, hitPos.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
            if (hitState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                this.getWorld().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, hitState), hitPos.x, hitPos.y, hitPos.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
            } else if (fluid.isSame(Fluids.WATER)) {
                this.getWorld().addParticle(ParticleTypes.SPLASH, hitPos.x, hitPos.y, hitPos.z, 0, 0, 0);
                if (this.getWorld().getRandom().nextDouble() < 0.2)
                    this.getWorld().addParticle(ParticleTypes.BUBBLE, hitPos.x, hitPos.y, hitPos.z, 0, 0, 0);

            } else if (fluid.isSame(Fluids.LAVA)) {
                this.getWorld().addParticle(ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 0, 0, 0);
                if (this.getWorld().getRandom().nextDouble() < 0.2)
                    this.getWorld().addParticle(ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 0, 0, 0);

            }
        }
    }

    private Vector3d projectVector(final Vector3d x, final Vector3d axis, final double scale) {
        return x.fma(-scale * x.dot(axis), axis);
    }

    public int getParticleCount() {
        double count = this.updatedParticleAmount * this.getAirPressure();

        if (this.particleSmoothing > 0)
            count = Math.log(count / this.particleSmoothing + 1) * this.particleSmoothing;

        return Math.min((int) (count + this.getWorld().random.nextFloat()), this.maxParticleAmount);
    }

    public void setThrustDirection(final Vector3dc thrustDirection) {
        this.thrustDirection = thrustDirection;
    }


    private double getAirPressure() {
        return DimensionPhysicsData.getAirPressure(this.getWorld(), Sable.HELPER.projectOutOfSubLevel(this.getWorld(), JOMLConversion.atCenterOf(this.getPos())));
    }

    public void setParticleAmountUpdater(final Supplier<Double> supp) {
        this.particleAmountUpdater = supp;
    }

    public void setParticlePositionUpdater(final BiConsumer<Vector3d, RandomSource> cons) {
        this.particlePositionUpdater = cons;
    }

    public void setParticleCountProperties(final int maxParticleAmount, final double particleSmoothing) {
        this.maxParticleAmount = maxParticleAmount;
        this.particleSmoothing = particleSmoothing;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (this.propeller.isActive()) {
            AeroLang.emptyLine(tooltip);
            AeroLang.blockName(this.blockEntity.getBlockState()).text(":")
                    .forGoggles(tooltip);

            // We use updaters here to ensure we have proper, client side information at hand, as this can be called multiple times per tick
            final MutableComponent thrustComponent = AeroLang.pixelNewton(Math.abs(this.propeller.getScaledThrust()))
                    .style(ChatFormatting.AQUA).component();
            AeroLang.translate("propeller.thrust", thrustComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            final MutableComponent airflowComponent = AeroLang.translate("unit.meters_per_second", String.format("%.2f", Math.abs(this.propeller.getAirflow())))
                    .style(ChatFormatting.AQUA).component();
            AeroLang.translate("propeller.airflow", airflowComponent)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            this.additionalTooltipInfo(tooltip, isPlayerSneaking);

            return true;
        }

        return false;
    }

    public void additionalTooltipInfo(final List<Component> tooltip, final boolean isPlayerSneaking) {

    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public record PropellerLayer(double offset, double innerRadius, double outerRadius) {
        public double innerRadiusSquared() {
            return this.innerRadius * this.innerRadius;
        }

        public double outerRadiusSquared() {
            return this.outerRadius * this.outerRadius;
        }
    }
}
