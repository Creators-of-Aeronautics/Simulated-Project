package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.config.server.blocks.SimAssembly;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import dev.simulated_team.simulated.content.blocks.physics_assembler.assembly_preventer.DisassemblyPrevention;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.mixin_interface.assembly_preventer.PrimaryAssemblerExtension;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFailedPacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFlickAndHoldLeverPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.simulated_team.simulated.util.assembly.SimAssemblyException;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.List;

public class PhysicsAssemblerBlockEntity extends SmartBlockEntity implements IDisplayAssemblyExceptions {
    private static final float FLICKED_ANGLE_DEGREES = 45.0f;
    private static final double LEVER_CHASE_SPEED = 0.75f;

    private static final double LINEAR_STIFFNESS = 1000.0;
    private static final double LINEAR_DAMPING = 50.0;

    private static final double ANGULAR_STIFFNESS = 13000.0;
    private static final double ANGULAR_DAMPING = 1000.0;
    private static final MutableComponent ASSEMBLE_TIP = SimLang.translate("gui.hold_tip.hold_to_assemble").component();
    private static final MutableComponent DISASSEMBLE_TIP = SimLang.translate("gui.hold_tip.hold_to_disassemble").component();

    protected AssemblyException lastException;
    protected boolean primaryAssembler;
    protected LerpedFloat visualAngle = LerpedFloat.linear();

    /**
     * When the player lets go of the lever when assembling / disassembling, we want to hold the lever in place
     * until we either receive an assembly failure
     */
    protected boolean holdingLever = false;
    private boolean leverInitialized = false;

    private boolean disassembling = false;
    private int disassemblingTicks = 0;
    private int disassemblyReadyTicks = 0;
    private int disassemblyAngle = 0;
    private Quaterniondc disassemblyOrientation;

    private boolean controlledByPlayer = false;
    private float playerAngle = 0.0f;

    @Nullable
    private FreeConstraintHandle alignmentConstraint;
    private HoldTipBehaviour holdTipBehaviour;

    public PhysicsAssemblerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        behaviours.add(this.holdTipBehaviour = new HoldTipBehaviour(this, ASSEMBLE_TIP));
    }

    @Override
    public void initialize() {
        super.initialize();

        if (this.primaryAssembler) {
            this.setParent(this.level);
        }

        if (!this.isVirtual()) {
            this.initializeLeverPosition();
            this.holdTipBehaviour.setHoverTip(this.getSubLevel() != null ? DISASSEMBLE_TIP : ASSEMBLE_TIP);
        }
    }

    protected void initializeLeverPosition() {
        if (!this.leverInitialized) {
            this.clientFlickLeverTo(this.getSubLevel() != null);
            this.jerkLever();
            this.leverInitialized = true;
        }
    }

    private @Nullable SubLevel getSubLevel() {
        return Sable.HELPER.getContaining(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.disassembling) {
            this.tickDisassembling();
        }

        if (this.holdingLever) {
            this.visualAngle.setValue(this.visualAngle.getValue());
        } else {
            if (this.controlledByPlayer) {
                this.visualAngle.setValue(this.visualAngle.getValue());
                this.visualAngle.setValueNoUpdate(this.playerAngle);
            } else {
                this.visualAngle.tickChaser();
            }
        }
    }

    private void tickDisassembling() {
        this.disassemblingTicks++;

        final SimAssembly config = SimConfigService.INSTANCE.server().assembly;
        if (this.disassemblingTicks >= config.maxDisassemblyTicks.get() * 5) {
            this.assemblyFailed(SimAssemblyException.couldNotAlign());
            this.stopDisassembling();
            return;
        }

        final SubLevel subLevel = this.getSubLevel();
        if (subLevel instanceof ServerSubLevel) {
            final Pose3d pose = subLevel.logicalPose();
            final double angle = pose.orientation().div(this.disassemblyOrientation, new Quaterniond()).angle();

            final Vector3d current = pose.transformPosition(new Vector3d(pose.rotationPoint()).floor().add(0.5, 0.5, 0.5));
            final Vector3d goal = current.floor(new Vector3d()).add(0.5, 0.5, 0.5);
            final Vector3d localGoal = this.disassemblyOrientation.transformInverse(goal, new Vector3d());

            this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0);
            this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0);
            this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0);

            if (Math.toDegrees(Math.abs(angle)) <= config.disassemblyDegreeTolerance.get() && current.distance(goal) < 0.2) {
                this.disassemblyReadyTicks++;
            } else {
                this.disassemblyReadyTicks = 0;
            }

            if (this.disassemblyReadyTicks > 5) {
                this.placeIntoWorld();
            }
        }
    }

    private void placeIntoWorld() {
        final SubLevel subLevel = this.getSubLevel();
        assert subLevel != null;

        try {
            this.throwDisassemblyExceptions((ServerSubLevel) subLevel);
        } catch (final AssemblyException e) {
            this.assemblyFailed(e);
            this.stopDisassembling();
            return;
        }

        final BlockPos goal = BlockPos.containing(subLevel.logicalPose().transformPosition(Vec3.atCenterOf(this.getBlockPos())));
        final Rotation rotation = SimAssemblyHelper.rotationFrom90DegRots(this.disassemblyAngle);
        SimAssemblyHelper.disassembleSubLevel(this.level, subLevel, this.getBlockPos(), goal, rotation, true);
        this.stopDisassembling();
    }

    private void throwDisassemblyExceptions(final ServerSubLevel subLevel) throws AssemblyException {
        final BoundingBox3dc bounds = subLevel.boundingBox();
        if (bounds.maxY() > this.level.getMaxBuildHeight()
                || bounds.minY() < this.level.getMinBuildHeight()) {
            throw SimAssemblyException.outOfWorld();
        }

        final SimAssembly config = SimConfigService.INSTANCE.server().assembly;

        final RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle.getLinearVelocity(new Vector3d()).lengthSquared() > Mth.square(config.disassemblyMaxVelocity.getF()) ||
                handle.getAngularVelocity(new Vector3d()).lengthSquared() > Mth.square(config.disassemblyMaxAngularVelocity.getF())) {
            throw SimAssemblyException.tooFast();
        }

        final BoundingBox3i chunkBounds = new BoundingBox3i(
                (Mth.floor(bounds.minX()) >> 4) - 1,
                (Mth.floor(bounds.minY()) >> 4) - 1,
                (Mth.floor(bounds.minZ()) >> 4) - 1,
                (Mth.floor(bounds.maxX()) >> 4) + 1,
                (Mth.floor(bounds.maxY()) >> 4) + 1,
                (Mth.floor(bounds.maxZ()) >> 4) + 1
        );

        if (config.disallowMidAirDisassembly.get()) {
            boolean nearGround = false;

            scanSectionsLoop:
            for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
                for (int z = chunkBounds.minZ(); z <= chunkBounds.maxZ(); z++) {
                    final LevelChunk chunk = this.level.getChunk(x, z);

                    for (int y = chunkBounds.minY(); y <= chunkBounds.maxY(); y++) {
                        final int index = chunk.getSectionIndexFromSectionY(y);

                        if (index < 0 || index >= chunk.getSectionsCount()) {
                            continue;
                        }

                        if (!chunk.getSection(index).hasOnlyAir()) {
                            nearGround = true;
                            break scanSectionsLoop;
                        }
                    }
                }
            }

            if (!nearGround) {
                throw SimAssemblyException.tooFarFromGround();
            }
        }
    }

    private void stopDisassembling() {
        if (this.alignmentConstraint != null && this.alignmentConstraint.isValid()) {
            this.alignmentConstraint.remove();
            this.alignmentConstraint = null;
        }

        this.disassemblingTicks = 0;
        this.disassembling = false;
    }

    public void setClientHoldLeverInPlace(final boolean holding) {
        this.holdingLever = holding;
    }

    public void updateControlledByPlayer(final float angle) {
        if (!this.controlledByPlayer) {
            this.controlledByPlayer = true;
        }
        this.playerAngle = angle;
    }

    public boolean stopControllingPlayer() {
        if (!this.controlledByPlayer) return false;
        this.controlledByPlayer = false;
        return true;
    }

    public void clientFlickLeverTo(final boolean flicked) {
        this.visualAngle.chase(flicked ? FLICKED_ANGLE_DEGREES : 0.0f, LEVER_CHASE_SPEED, LerpedFloat.Chaser.EXP);
    }

    public void jerkLever() {
        this.visualAngle.setValue(this.visualAngle.getChaseTarget());
        this.visualAngle.setValue(this.visualAngle.getChaseTarget());
    }

    public void assembleOrDisassemble() {
        final SubLevel subLevel = Sable.HELPER.getContaining(this);

        final Level level = this.getLevel();
        assert level != null;
        try {
            VeilPacketManager.tracking(this).sendPacket(new PhysicsAssemblerFlickAndHoldLeverPacket(this.worldPosition, subLevel == null));

            if (subLevel instanceof final ServerSubLevel serverSubLevel) {
                if (DisassemblyPrevention.checkSubLevelForPrimary(level, this.getBlockPos())) {
                    this.throwDisassemblyExceptions(serverSubLevel);
                    this.startDisassembling(serverSubLevel, (ServerLevel) level, subLevel);
                    this.disassembling = true;
                }
            } else {
                this.primaryAssembler = true;

                final BlockPos toAssemble = this.getBlockPos().relative(PhysicsAssemblerBlock.getStickyFacing(this.getBlockState()));
                SimAssemblyHelper.assembleFromSingleBlock(level, this.getBlockPos(), toAssemble, true, true);

                this.lastException = null;
                this.sendData();
            }
        } catch (final AssemblyException e) {
            if (!(subLevel instanceof ServerSubLevel)) {
                this.primaryAssembler = false;
            }

            this.assemblyFailed(e);
        }
    }

    private void assemblyFailed(final AssemblyException exception) {
        this.lastException = exception;
        VeilPacketManager.tracking(this).sendPacket(new PhysicsAssemblerFailedPacket(this.worldPosition));
        this.sendData();
    }

    private void startDisassembling(final ServerSubLevel serverSubLevel, final ServerLevel level, final SubLevel subLevel) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        final PhysicsPipeline pipeline = container.physicsSystem().getPipeline();

        // Setup constraint
        final MassData massTracker = serverSubLevel.getMassTracker();

        final double closestYRotation = SimMathUtils.getClosestYaw(subLevel.logicalPose().orientation());
        final double ninety = Math.PI / 2.0;
        final int turns = -(Mth.floor(closestYRotation / ninety + 0.5));
        this.disassemblyAngle = turns;

        final FreeConstraintConfiguration config = new FreeConstraintConfiguration(new Vector3d(),
                new Vector3d(massTracker.getCenterOfMass()).floor().add(0.5, 0.5, 0.5),
                this.disassemblyOrientation = new Quaterniond().rotateY(turns * ninety));
        this.alignmentConstraint = pipeline.addConstraint(null, serverSubLevel, config);

        this.alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_X, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0);
        this.alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_Z, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0);
        this.alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_Y, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0);

        this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, 0.0, 0.000001, LINEAR_DAMPING, false, 0.0);
        this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0, 0.000001, LINEAR_DAMPING, false, 0.0);
        this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, 0.0, 0.000001, LINEAR_DAMPING, false, 0.0);

        this.disassembling = true;
        this.disassemblingTicks = 0;

        // Remove any locks on the sub-level
        PhysicsStaffServerHandler.get(level).removeLock(serverSubLevel);
    }

    @Override
    public void remove() {
        //if we're the primary assembler, set the parent's primary assembler to null to ensure any assembler can disassemble
        if (this.primaryAssembler) {
            if (!this.level.isClientSide) {
                final SubLevel subLevel = this.getSubLevel();
                if (subLevel instanceof final ServerSubLevel ssb) {
                    ((PrimaryAssemblerExtension) ssb).simulated$setPrimaryAssembler(null);
                }
            }
        }

        this.stopDisassembling();

        super.remove();
    }

    @Override
    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        AssemblyException.write(compound, registries, this.lastException);
        compound.putBoolean("IsPrimary", this.primaryAssembler);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.lastException = AssemblyException.read(tag, registries);
        this.primaryAssembler = tag.getBoolean("IsPrimary");
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return this.lastException;
    }

    public boolean isPrimaryAssembler() {
        return this.primaryAssembler;
    }

    protected void setParent(final Level level) {
        this.lastException = null;

        final SubLevel subLevel = Sable.HELPER.getContaining(level, this.getBlockPos());
        if (!level.isClientSide && this.primaryAssembler && subLevel instanceof ServerSubLevel) {
            final PrimaryAssemblerExtension duck = (PrimaryAssemblerExtension) subLevel;
            if (duck.simulated$getPrimaryAssembler() == null) {
                duck.simulated$setPrimaryAssembler(this.getBlockPos());
            }
        } else {
            // we disassembled, so no assembler is primary
            this.primaryAssembler = false;
        }
    }

    public float getClientAngle(final float partialTicks) {
        return this.visualAngle.getValue(partialTicks);
    }
}
