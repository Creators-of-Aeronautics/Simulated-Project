package dev.simulated_team.simulated.content.blocks.merging_glue;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

public class MergingGlueBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {
    private static final int DURATION = 10;
    private static final int WAIT_ASSEMBLE = 2;
    private final Quaterniond startPartnerOrientation = new Quaterniond();
    private final Quaterniond endPartnerOrientation = new Quaterniond();
    private final Vector3d endPartnerPosition = new Vector3d();
    private final Vector3d startPartnerPosition = new Vector3d();
    private boolean hasControllingValues = false;
    private Rotation endRotation;

    @Nullable
    private BlockPos partnerPosition;
    private boolean isController;
    private int ageTicks;
    private FixedConstraintHandle lastConstraintHandle = null;

    public MergingGlueBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public MergingGlueBlockEntity getPartnerGlue() {
        if (this.partnerPosition == null) {
            return null;
        }

        final BlockEntity be = this.level.getBlockEntity(this.partnerPosition);
        if (be instanceof MergingGlueBlockEntity) {
            return (MergingGlueBlockEntity) be;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();

        final boolean serverSide = !this.level.isClientSide();
        if (serverSide && this.isController && this.ageTicks > DURATION + WAIT_ASSEMBLE) {

            final SubLevel subLevel = Sable.HELPER.getContaining(this);
            final SubLevel partnerSubLevel = Sable.HELPER.getContaining(this.level, this.partnerPosition);

            if (partnerSubLevel instanceof final ServerSubLevel partnerServerSubLevel) {
                if (partnerServerSubLevel.getMassTracker().getMass() < ((ServerSubLevel) subLevel).getMassTracker().getMass()) {
                    final MergingGlueBlockEntity partner = this.getPartnerGlue();

                    if (partner != null) {
                        this.breakGlue();
                        partner.disassembleToPartner(switch (this.endRotation) {
                            case NONE -> Rotation.NONE;
                            case CLOCKWISE_90 ->  Rotation.COUNTERCLOCKWISE_90;
                            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
                            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
                        });
                        return;
                    }
                }
            }

            this.breakGlue();
            this.disassembleToPartner(this.endRotation);
            return;
        }

        if (serverSide && (this.ageTicks > 10 * 20 || this.partnerPosition == null || (!this.hasControllingValues && this.isController))) {
            this.breakGlue();
        }

        this.ageTicks++;
    }

    private void disassembleToPartner(final Rotation rotation) {
        assert this.level != null;
        assert this.partnerPosition != null;

        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        final BlockPos pos = this.getBlockPos().relative(this.getBlockState().getValue(MergingGlueBlock.FACING).getOpposite());
        SimAssemblyHelper.disassembleSubLevel(this.level, subLevel, pos, this.partnerPosition, rotation, false);
    }

    private void breakGlue() {
        if (this.level.getBlockState(this.getBlockPos()).is(SimBlocks.MERGING_GLUE)) {
            this.level.destroyBlock(this.getBlockPos(), true);
        }

        if (this.partnerPosition != null && this.level.getBlockState(this.partnerPosition).is(SimBlocks.MERGING_GLUE)) {
            this.level.destroyBlock(this.partnerPosition, true);
        }
    }

    public Vector3d getCenter(final Vector3d dest) {
        final BlockState state = this.getBlockState();
        final Direction facing = state.getValue(MergingGlueBlock.FACING);

        return JOMLConversion.atCenterOf(this.worldPosition, dest).sub(facing.getStepX() * 0.5, facing.getStepY() * 0.5, facing.getStepZ() * 0.5);
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (!this.isController) {
            return;
        }

        if (!this.hasControllingValues) {
            return;
        }

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

        final double partialPhysicsTick = physicsSystem.getPartialPhysicsTick();
        final double physicsTime = this.ageTicks + partialPhysicsTick;

        final double lerpFactor = Mth.clamp(Math.pow(physicsTime / DURATION, 5.0), 0.0, 1.0);

        final double rotationLerpFactor = Mth.clamp(lerpFactor * 2.0, 0.0, 1.0);

        this.removeConstraint();

        final MergingGlueBlockEntity partner = this.getPartnerGlue();

        if (partner == null) {
            return;
        }

        final SubLevel partnerSubLevel = Sable.HELPER.getContaining(partner);

        if (!(partnerSubLevel instanceof final ServerSubLevel partnerServerSubLevel)) {
            return;
        }

        final RigidBodyHandle partnerHandle = RigidBodyHandle.of(partnerServerSubLevel);

        final Vector3d localPartnerCenter = partner.getCenter(new Vector3d());

        this.lastConstraintHandle = physicsSystem.getPipeline().addConstraint(
                subLevel,
                partnerServerSubLevel,
                new FixedConstraintConfiguration(
                        this.startPartnerPosition.lerp(this.endPartnerPosition, lerpFactor, new Vector3d()),
                        localPartnerCenter,
                        this.startPartnerOrientation.slerp(this.endPartnerOrientation, rotationLerpFactor, new Quaterniond())
                )
        );
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putBoolean("Controller", this.isController);

        if (this.partnerPosition != null) {
            tag.putLong("PartnerPosition", this.partnerPosition.asLong());
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.isController = tag.getBoolean("Controller");

        if (tag.contains("PartnerPosition")) {
            this.partnerPosition = BlockPos.of(tag.getLong("PartnerPosition"));
        }
    }

    public void removeConstraint() {
        if (this.lastConstraintHandle != null) {
            this.lastConstraintHandle.remove();
        }

        this.lastConstraintHandle = null;
    }

    @Override
    public void remove() {
        super.remove();
        this.removeConstraint();
        this.breakGlue();
    }

    public boolean isController() {
        return this.isController;
    }

    public void setPartnerPos(final BlockPos partnerPos) {
        this.partnerPosition = partnerPos;
    }

    /**
     * Starts controlling a partner glue & sets up the constraint
     */
    public void startControlling(final MergingGlueBlockEntity partner) {
        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        final SubLevel otherSubLevel = Sable.HELPER.getContaining(partner);

        if (subLevel == null || otherSubLevel == null) {
            return;
        }

        final Vector3d center = this.getCenter(new Vector3d());
        final Vector3d partnerCenter = partner.getCenter(new Vector3d());

        otherSubLevel.logicalPose().transformPosition(partnerCenter);
        subLevel.logicalPose().transformPositionInverse(partnerCenter);

        this.startPartnerPosition.set(partnerCenter);
        this.endPartnerPosition.set(center);

        final Quaterniond startOrientation = otherSubLevel.logicalPose().orientation();
        subLevel.logicalPose().orientation().conjugate(new Quaterniond()).mul(startOrientation, startOrientation);

        this.startPartnerOrientation.set(startOrientation);
        this.endPartnerOrientation.set(new Quaterniond());

        final Direction direction = this.getBlockState().getValue(MergingGlueBlock.FACING);
        final Direction partnerDirection = partner.getBlockState().getValue(MergingGlueBlock.FACING);

        if (direction.getAxis().isVertical()) {
            final double yRotation = SimMathUtils.getClosestYaw(startOrientation);
            final double ninety = Math.PI / 2.0;
            final int turns = -(Mth.floor(yRotation / ninety + 0.5));
            this.endPartnerOrientation.rotateY(turns * ninety);
            this.endRotation = SimAssemblyHelper.rotationFrom90DegRots(-turns);
        } else {
            final Vec3i normal = direction.getNormal();
            final Vec3i partnerNormal = partnerDirection.getNormal();

            double angle = Math.atan2(partnerNormal.getX(), partnerNormal.getZ()) - Math.atan2(normal.getX(), normal.getZ());
            if (direction.getAxis() == partnerDirection.getAxis()) {
                angle += Math.PI;
            }

            final double ninety = Math.PI / 2.0;
            final int turns = -(Mth.floor(angle / ninety + 0.5));

            this.endPartnerOrientation.rotateY(angle);
            this.endRotation = SimAssemblyHelper.rotationFrom90DegRots(turns);
        }

        this.isController = true;
        this.hasControllingValues = true;
    }

}
