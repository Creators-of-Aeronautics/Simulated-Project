package dev.simulated_team.simulated.content.blocks.lasers;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.index.SimTags;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class LaserBehaviour extends BlockEntityBehaviour {
    private static final BehaviourType<LaserBehaviour> TYPE = new BehaviourType<>();

    /**
     * Whether this laser behaviour should attempt to cast a ray.
     */
    private BooleanSupplier shouldCast;

    /**
     * The beginning and ending positions for this laser. Used in {@link LaserBehaviour#blockHitResult} and {@link LaserBehaviour#entityHitResult}.
     */
    private final Supplier<Couple<Vec3>> laserPositions;

    /**
     * Range of this laser behaviour
     */
    private final Supplier<Float> range;

    /**
     * Clip context used for {@link LaserBehaviour#blockHitResult}
     */
    private final Supplier<ClipContext> context;

    /**
     * The {@link BlockHitResult} for this laser behaviour. NOT guaranteed to be closer than {@link LaserBehaviour#entityHitResult}
     */
    private BlockHitResult blockHitResult;

    /**
     * The {@link EntityHitResult} for this laser behaviour. NOT guaranteed to be closer than {@link LaserBehaviour#blockHitResult}
     */
    private EntityHitResult entityHitResult;

    /**
     * The closest hit result between {@link LaserBehaviour#blockHitResult} and {@link LaserBehaviour#entityHitResult}
     */
    private HitResult closestHitResult;
    private ClipContext.Block blockCollide = ClipContext.Block.VISUAL;
    private ClipContext.Fluid fluidCollide = ClipContext.Fluid.NONE;
    //ponder hit position
    private Vec3 virtualHitPos = Vec3.ZERO;

    public LaserBehaviour(final SmartBlockEntity be, final Supplier<Couple<Vec3>> positions, final Supplier<Float> range) {
        super(be);

        this.shouldCast = () -> true;

        this.laserPositions = positions;
        this.range = range;

        this.context = () -> this.getClipContext(positions.get().getFirst(), positions.get().getSecond());
    }

    @Override
    public void tick() {
        if (!this.blockEntity.isVirtual() && this.shouldCast()) {
            this.castRay();
        }
    }

    @Override
    public void initialize() {
        if (this.shouldCast()) {
            this.castRay();
        }
    }

    private void castRay() {
        final Level level = this.blockEntity.getLevel();

        if (level != null) {
            this.blockHitResult = level.clip(this.context.get());

            final Couple<Vec3> positions = this.laserPositions.get();
            final Vec3 start = positions.getFirst();
            final Vec3 end = positions.getSecond();
            final AABB checkingBB = new AABB(start, start).inflate(0.5f)
                    .expandTowards(end.subtract(start));

            // ProjectileUtil.getEntityHitResult()'s location is the feet position instead of the actual clip position
            final EntityHitResult wrongHitResult = ProjectileUtil.getEntityHitResult(level, null, start, end, checkingBB, (e) -> !e.getType().is(SimTags.Misc.LASER_BLACKLIST) && !e.isSpectator(), 0.1f);
            if (wrongHitResult != null) {
                // slightly less wrong :p
                // todo probably some better math maybe
                this.entityHitResult = new EntityHitResult(wrongHitResult.getEntity(), wrongHitResult.getEntity().getBoundingBox().getCenter());
            } else {
                this.entityHitResult = null;
            }

            if (this.entityHitResult != null && Sable.HELPER.distanceSquaredWithSubLevels(this.getWorld(), positions.getFirst(), this.entityHitResult.getLocation())
                    < Sable.HELPER.distanceSquaredWithSubLevels(this.getWorld(), positions.getFirst(), this.blockHitResult.getLocation())) {
                this.closestHitResult = this.entityHitResult;
            } else {
                this.closestHitResult = this.blockHitResult;
            }
        } else {
            this.blockHitResult = null;
            this.entityHitResult = null;
            this.closestHitResult = null;
        }
    }

    @NotNull
    private ClipContext getClipContext(final Vec3 start, final Vec3 end) {
        return new ClipContext(
                start,
                end,
                this.blockCollide,
                this.fluidCollide,
                CollisionContext.empty()
        );
    }

    public void setBlockCollide(final ClipContext.Block blockCollide) {
        this.blockCollide = blockCollide;
    }

    public void setFluidCollide(final ClipContext.Fluid fluidCollide) {
        this.fluidCollide = fluidCollide;
    }

    public void setShouldCast(final BooleanSupplier shouldCast) {
        this.shouldCast = shouldCast;
    }

    public Supplier<Couple<Vec3>> getLaserPositions() {
        return this.laserPositions;
    }

    public BlockHitResult getBlockHitResult() {
        return this.blockHitResult;
    }

    public float getRange() {
        return this.range.get();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public void setVirtualHitPos(final Vec3 virtualHitPos) {
        this.virtualHitPos = virtualHitPos;
    }

    public Vec3 getVirtualHitPos() {
        return this.virtualHitPos;
    }

    public boolean shouldCast() {
        return this.shouldCast.getAsBoolean();
    }

    /**
     * The {@link EntityHitResult} for this laser behaviour. NOT guaranteed to be closer than {@link LaserBehaviour#blockHitResult}
     */
    public EntityHitResult getEntityHitResult() {
        return this.entityHitResult;
    }

    public HitResult getClosestHitResult() {
        return this.closestHitResult;
    }
}
