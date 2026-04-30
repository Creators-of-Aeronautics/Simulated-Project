package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.item.TooltipHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.rotary.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.rotary.RotaryConstraintHandle;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimLevelUtil;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

import static net.minecraft.ChatFormatting.GOLD;

public class SwivelBearingBlockEntity extends KineticBlockEntity implements ExtraKinetics, IDisplayAssemblyExceptions, BlockEntitySubLevelActor {
    private static final MutableComponent SCROLL_OPTION_TITLE = Component.translatable(Simulated.MOD_ID + ".scroll_option.swivel_default_locked");

    @NotNull
    private final SwivelBearingCogwheelBlockEntity cogwheel;

    /**
     * If the bearing should assemble next tick
     */
    public boolean assembleNextTick;
    protected AssemblyException lastException;
    /**
     * The target angle degrees from the last tick
     */
    private double lastTargetAngleDegrees = 0;
    /**
     * The current target angle in degrees
     */
    private double targetAngleDegrees = 0;
    /**
     * The angle limit from sequenced contexts
     */
    private double sequencedAngleLimit = -1;
    /**
     * The ID of the attached sub-level
     */
    @Nullable
    private UUID subLevelID;
    /**
     * The block position of the attached {@link SwivelBearingPlateBlock}
     */
    @Nullable
    private BlockPos swivelPlatePos;
    /**
     * The current constraint handle between this swivel and the attached sub-level
     */
    @Nullable
    private RotaryConstraintHandle handle;
    /**
     * If this BE is being destroyed as a part of assembly
     */
    private boolean assembling;
    /**
     * The locked default scroll option
     */
    private ScrollOptionBehaviour<LockingSetting> lockedDefaultOption;

    public SwivelBearingBlockEntity(final BlockEntityType<?> typeIn, final BlockPos pos, final BlockState state) {
        super(typeIn, pos, state);
        this.assembleNextTick = false;

        this.cogwheel = new SwivelBearingCogwheelBlockEntity(typeIn, pos, state, this);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        this.lockedDefaultOption = new ScrollOptionBehaviour<>(LockingSetting.class, SCROLL_OPTION_TITLE, this, new SelectionModeValueBox(this::isValidForOptionPanel));
        this.lockedDefaultOption.value = 1;
        behaviours.add(this.lockedDefaultOption);
    }

    /**
     * @return if a direction is valid for a selector to be placed on
     */
    private boolean isValidForOptionPanel(final BlockState state, final Direction direction) {
        final Direction facing = state.getValue(SwivelBearingBlock.FACING);
        final Direction.Axis currentAxis = facing.getAxis();

        return direction.getAxis() != currentAxis;
    }

    @Override
    public void tick() {
        final Level level = this.getLevel();

        super.tick();
        this.cogwheel.tick();

        if (level.isClientSide) {
            if (this.isTooFast()) {
                this.playGrindingEffect();
            }

            return;
        }

        // assemble or disassemble
        if (this.assembleNextTick) {
            if (!this.isAssembled()) {
                this.assemble();
            } else {
                this.disassemble();
            }
        }

        final SubLevel attached = this.getAttachedSubLevel();

        // update our powered state and reattach constraints
        final int bestSignal = this.level.getBestNeighborSignal(this.getBlockPos());
        final boolean shouldLock = this.lockedDefaultOption.get().shouldLock(bestSignal);

        if (shouldLock && !this.isLocking()) {
            this.level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(BlockStateProperties.POWERED, true));

            if (this.handle != null && attached != null) {
                //update our constraint
                this.reattachConstraint(attached, false);
            }

            if (attached != null && this.getPlatePos() != null) {
                final BlockState plateBlock = this.level.getBlockState(this.getPlatePos());

                if (plateBlock.is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
                    this.setTargetAngleFromCurrentOrientation(plateBlock, attached);
                }
            }
        } else if (!shouldLock && this.isLocking()) {
            this.level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(BlockStateProperties.POWERED, false));

            if (this.handle != null && attached != null) {
                //update our constraint
                this.reattachConstraint(attached, false);
            }
        }

        // check persistence to make sure we keep our sublevel after reload
        if (this.getSubLevelID() != null) {
            this.checkPersistence(this.getSubLevelID());
        }

        // update our target angles
        this.lastTargetAngleDegrees = this.targetAngleDegrees;
        float angularSpeed = convertToAngular(this.limitCogSpeed(this.cogwheel.getSpeed()));

        boolean shouldUpdateAngle = this.isAssembled();

        if (this.sequencedAngleLimit >= 0) {
            angularSpeed = (float) Mth.clamp(angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
            this.sequencedAngleLimit = Math.max(0, this.sequencedAngleLimit - Math.abs(angularSpeed));
        } else {
            final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(this.level);
            // if rotation is not sequenced (go to a set angle) and physics is paused, do not update target angle
            if (physicsSystem == null || physicsSystem.getPaused()) {
                shouldUpdateAngle = false;
            }
        }

        if (shouldUpdateAngle) {
            // for negative facing directions, we need to negate the angular speed
            if (this.getBlockState().getValue(SwivelBearingBlock.FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                angularSpeed *= -1.0f;
            }

            this.targetAngleDegrees += angularSpeed;
            this.targetAngleDegrees %= 360;

            if (attached != null && this.isAssembled() && this.handle != null) {
                final SubLevel containing = this.getContainingSubLevel();

                if (angularSpeed != 0.0) {
                    final PhysicsPipeline pipeline = ((ServerSubLevelContainer) SubLevelContainer.getContainer(this.level)).physicsSystem().getPipeline();

                    if (containing instanceof final ServerSubLevel serverSubLevel) {
                        pipeline.wakeUp(serverSubLevel);
                    }

                    if (attached instanceof final ServerSubLevel serverSubLevel) {
                        pipeline.wakeUp(serverSubLevel);
                    }
                }
            }
        }

        this.assembleNextTick = false;
    }

    private void playGrindingEffect() {
        final Direction facing = this.getBlockState().getValue(SwivelBearingBlock.FACING);

        final RandomSource random = this.level.random;

        final int stepX = facing.getStepX();
        final int stepY = facing.getStepY();
        final int stepZ = facing.getStepZ();

        for (int i = 0; i < 2; i++) {
            final Vec3 particlePos = this.getBlockPos().getCenter()
                    .add(stepX * 7.0 / 16.0, stepY * 7.0 / 16.0, stepZ * 7.0 / 16.0)
                    .add((random.nextFloat() - 0.5f) * (stepX == 0 ? 1 : 0), (random.nextFloat() - 0.5f) * (stepY == 0 ? 1 : 0), (random.nextFloat() - 0.5f) * (stepZ == 0 ? 1 : 0));

            this.level.addParticle(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 0.0f, 0.0f, 0.0f);
        }
    }

    @Override
    public boolean addToTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (super.addToTooltip(tooltip, isPlayerSneaking))
            return true;

        if (isPlayerSneaking)
            return false;

        if (this.cogwheel.getSpeed() == 0)
            return false;

        if (this.isAssembled()) {
            if (this.isTooFast()) {
                SimLang.translate("swivel_bearing.too_fast")
                        .style(GOLD)
                        .forGoggles(tooltip);

                final MutableComponent component = SimLang.translate("swivel_bearing.too_fast_error")
                        .component();

                final List<Component> cutString = TooltipHelper.cutTextComponent(component, FontHelper.Palette.GRAY_AND_WHITE);
                tooltip.addAll(cutString);

                return true;
            }

            return false;
        }
        final BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof SwivelBearingBlock))
            return false;

        final BlockState attachedState = this.level.getBlockState(this.worldPosition.relative(state.getValue(BearingBlock.FACING)));
        if (attachedState.canBeReplaced())
            return false;
        TooltipHelper.addHint(tooltip, "hint.empty_bearing");
        return true;
    }

    private boolean isTooFast() {
        final float maxSwivelRPM = SimConfigService.INSTANCE.server().blocks.maxSwivelBearingSpeed.getF();
        return Math.abs(this.cogwheel.getSpeed()) > maxSwivelRPM;
    }

    private float limitCogSpeed(final float speed) {
        final float maxSwivelRPM = SimConfigService.INSTANCE.server().blocks.maxSwivelBearingSpeed.getF();
        return Mth.clamp(speed, -maxSwivelRPM, maxSwivelRPM);
    }

    /**
     * Updates the target angle to reflect the current orientation of the containing and attached sub-levels.
     * Called when the swivel bearing starts locking, as to keep the angle it is currently at.
     *
     * @param attached the attached sublevel
     */
    private void setTargetAngleFromCurrentOrientation(final BlockState attachedState, final SubLevel attached) {
        assert attached != null : "Attached sub-level is null!";

        final Quaterniond orientationA = new Quaterniond();
        final Quaterniond blockOrientationA = new Quaterniond(this.getBlockState().getValue(SwivelBearingPlateBlock.FACING).getRotation());
        final Quaterniond blockOrientationB = new Quaterniond(attachedState.getValue(SwivelBearingPlateBlock.FACING).getRotation());
        final Quaterniond orientationB = new Quaterniond(attached.logicalPose().orientation());
        final SubLevel containing = this.getContainingSubLevel();
        if (containing != null) {
            orientationA.set(containing.logicalPose().orientation());
        }

        final Quaterniond localB = new Quaterniond(orientationA).mul(blockOrientationA).conjugate().mul(new Quaterniond(orientationB).mul(blockOrientationB));

        final double d = new Vec3(0.0, 1.0, 0.0).dot(new Vec3(localB.x(), localB.y(), localB.z()));
        final double currentAngle = -2.0 * (float) Math.toDegrees(Math.atan2(-d, localB.w()));
        this.targetAngleDegrees = currentAngle;
        this.lastTargetAngleDegrees = currentAngle;
    }

    public void updateServoCoefficients() {
        if (!this.isAssembled() || this.handle == null) {
            return;
        }

        final SimPhysics config = SimConfigService.INSTANCE.server().physics;

        if (!this.isLocking()) {
            // Passive un-locked damping
            this.handle.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, 0.0, 0.0, config.swivelBearingFriction.get(), false, 0.0);
            return;
        }

        final SubLevel subLevelA = this.getContainingSubLevel();
        final SubLevel subLevelB = this.getAttachedSubLevel();

        final Vec3i facingVec3I = this.getBlockState().getValue(DirectionalKineticBlock.FACING).getNormal();
        final Vector3dc facingVec = new Vector3d(facingVec3I.getX(), facingVec3I.getY(), facingVec3I.getZ());

        double inertiaA = Double.MAX_VALUE;
        double inertiaB = Double.MAX_VALUE;
        final Vector3d temp = new Vector3d();
        if (subLevelA instanceof final ServerSubLevel serverSubLevel) {
            inertiaA = serverSubLevel.getMassTracker().getInertiaTensor().transform(facingVec, temp).dot(facingVec);
        }

        if (subLevelB instanceof final ServerSubLevel serverSubLevel) {
            inertiaB = serverSubLevel.getMassTracker().getInertiaTensor().transform(facingVec, temp).dot(facingVec);
        }

        final double totalInertia = Math.max(10.0,
                subLevelA != null && subLevelB != null ?
                        Math.max(inertiaA, inertiaB) : Math.min(inertiaA, inertiaB)
        );

        final SubLevelPhysicsSystem physicsSystem = ((ServerSubLevelContainer) SubLevelContainer.getContainer(this.level)).physicsSystem();

        final double kP = config.swivelBearingStiffness.get() * totalInertia;
        final double kD = config.swivelBearingDamping.get() * totalInertia;
        final float goal = AngleHelper.rad(AngleHelper.angleLerp(physicsSystem.getPartialPhysicsTick(), this.lastTargetAngleDegrees, this.targetAngleDegrees));

        this.handle.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, goal, kP, kD, false, 0.0);
        this.handle.setContactsEnabled(false);
    }

    public void assemble() {
        final BlockPos pos = this.getBlockPos();
        final BlockPos toAssemble = pos.relative(this.getBlockState().getValue(SwivelBearingBlock.FACING));
        final SimAssemblyHelper.AssemblyResult result;

        try {
            result = SimAssemblyHelper.assembleFromSingleBlock(this.level, pos, toAssemble, false, false);
            this.lastException = null;
        } catch (final AssemblyException e) {
            this.lastException = e;
            this.sendData();
            return;
        }

        this.sendData();

        final ServerSubLevel assembledSubLevel;
        final BlockPos assembleOffset;
        final BlockState link = SimBlocks.SWIVEL_BEARING_LINK_BLOCK.getDefaultState()
                .setValue(SwivelBearingPlateBlock.FACING, this.getBlockState().getValue(SwivelBearingBlock.FACING));

        if (result != null) {
            assembledSubLevel = (ServerSubLevel) result.subLevel();
            assembleOffset = result.offset();
        } else {
            final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.level);

            final Pose3d pose = new Pose3d();
            pose.position().set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

            assembledSubLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
            final LevelPlot plot = assembledSubLevel.getPlot();

            final ChunkPos center = plot.getCenterChunk();
            plot.newEmptyChunk(center);
            plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, link, 3);

            final BlockPos plotAnchor = plot.getCenterBlock();
            final Vector3dc centerOfMass = assembledSubLevel.getMassTracker().getCenterOfMass();
            final Vector3d subLevelCenter = JOMLConversion.atLowerCornerOf(pos);

            if (centerOfMass != null) {
                subLevelCenter.add(centerOfMass.x() - plotAnchor.getX(), centerOfMass.y() - plotAnchor.getY(), centerOfMass.z() - plotAnchor.getZ());
            } else {
                assembledSubLevel.logicalPose().rotationPoint()
                        .set(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
            }

            assembledSubLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);
            assembleOffset = plotAnchor.subtract(pos);

            final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            final PhysicsPipeline pipeline = physicsSystem.getPipeline();

            final SubLevel containingSubLevel = this.getContainingSubLevel();
            if (containingSubLevel != null) {
                SubLevelAssemblyHelper.kickFromContainingSubLevel((ServerLevel) this.level, physicsSystem, pipeline, assembledSubLevel, containingSubLevel);
            }

            pipeline.teleport(assembledSubLevel, assembledSubLevel.logicalPose().position(), assembledSubLevel.logicalPose().orientation());
            assembledSubLevel.updateLastPose();

            this.level.playSound(null, pos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        this.getLevel().setBlockAndUpdate(pos, this.getBlockState().setValue(SwivelBearingBlock.ASSEMBLED, true));

        this.attachConstraints(assembledSubLevel, this.getConstraintPos(toAssemble, assembleOffset));
        this.setSubLevelID(assembledSubLevel.getUniqueId());

        final BlockPos plotPos = pos.offset(assembleOffset);
        if (result != null) {
            this.getLevel().setBlockAndUpdate(plotPos, link);
        }
        final BlockEntity be = this.getLevel().getBlockEntity(plotPos);

        if (be instanceof final SwivelBearingPlateBlockEntity plateBE) {
            plateBE.setParent(this);
            this.setPlatePos(plotPos);
        }

        SimAdvancements.YOU_SPIN_ME_RIGHT_ROUND.awardToNearby(pos, this.getLevel());
    }

    public void disassemble() {
        if (this.isRemoved()) {
            return;
        }

        this.removeHandle();
        if (this.getSubLevelID() != null) {
            final SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.getSubLevelID());
            if (subLevel != null) {
                final BlockPos platePos = this.getPlatePos();
                if (platePos != null) {
                    this.destroyPlate();

                    // if destroying the plate removed the sub-level, skip disassembling
                    if (!subLevel.isRemoved()) {
                        SimAssemblyHelper.disassembleSubLevel(this.level, subLevel, platePos, this.getBlockPos(), Rotation.NONE, true);
                    } else {
                        this.level.playSound(null, platePos, SimSoundEvents.SIMULATED_CONTRAPTION_STOPS.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }
            }
        }

        this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(SwivelBearingBlock.ASSEMBLED, false));

        this.setSubLevelID(null);
        this.setPlatePos(null);
        this.targetAngleDegrees = 0;
    }

    private void checkPersistence(final UUID id) {
        if (this.getPlatePos() != null && SimLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.getPlatePos(), 1)) {
            if (!this.getLevel().getBlockState(this.getPlatePos()).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
                return;
            }
        }

        final SubLevel subLevel = SubLevelContainer.getContainer(this.getLevel()).getSubLevel(id);
        if (this.handle != null && !this.handle.isValid()) {
            this.handle = null;
        }

        if (subLevel != null && this.handle == null) {
            this.reattachConstraint(subLevel, true);
        }
    }

    public void reattachConstraint(final SubLevel toAttach, final boolean updatePlate) {
        //we also want to "reset" the plate BE here too, so it's correct
        final BlockPos platePos = this.getPlatePos();
        if (platePos != null) {
            if (this.handle != null) {
                this.handle.remove();
            }

            if (updatePlate) {
                this.associatePlateWithParent();
            }

            final BlockState plateState = this.level.getBlockState(platePos);
            if (!plateState.is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) return;

            final Direction plateFacing = plateState.getValue(SwivelBearingPlateBlock.FACING);
            this.attachConstraints(toAttach, JOMLConversion.toJOML(platePos.relative(plateFacing).getCenter()));
        }
    }

    public void associatePlateWithParent() {
        if (this.getPlatePos() != null) {
            if (this.getLevel().getBlockState(this.getPlatePos()).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
                final SwivelBearingPlateBlockEntity plate = (SwivelBearingPlateBlockEntity) this.getLevel().getBlockEntity(this.getPlatePos());
                plate.setParent(this);
            }
        }
    }

    private void attachConstraints(final SubLevel toAttach, final Vector3d attachPos) {
        final BlockPos platePos = this.getPlatePos();

        if (platePos == null) return;
        final BlockState plateState = this.level.getBlockState(platePos);

        if (!plateState.is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) return;

        final Vector3d anchorPos = JOMLConversion.toJOML(this.getBlockPos().relative(this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getCenter());
        final Vec3 facingVec = Vec3.atLowerCornerOf(this.getBlockState().getValue(DirectionalKineticBlock.FACING).getNormal());
        final Vec3 plateFacingVec = Vec3.atLowerCornerOf(plateState.getValue(DirectionalKineticBlock.FACING).getNormal());

        final RotaryConstraintConfiguration constraint = new RotaryConstraintConfiguration(
                anchorPos,
                attachPos.sub(JOMLConversion.toJOML(plateFacingVec.scale(0.001f))),
                JOMLConversion.toJOML(facingVec),
                JOMLConversion.toJOML(plateFacingVec)
        );

        final ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) this.getLevel());
        final PhysicsPipeline pipeline = container.physicsSystem().getPipeline();

        this.handle = pipeline.addConstraint((ServerSubLevel) Sable.HELPER.getContaining(this), (ServerSubLevel) toAttach, constraint);
    }

    @Override
    protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("TargetAngle", this.targetAngleDegrees);

        BlockPos platePos = this.getPlatePos();
        UUID id = this.getSubLevelID();

        // Handle serializing assembled swivels to schematics
        final SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();

        if (id != null && schematicContext != null) {
            final SubLevelSchematicSerializationContext.SchematicMapping mapping = schematicContext.getMapping(id);

            if (mapping != null) {
                id = mapping.newUUID();
                platePos = mapping.transform().apply(platePos);
            } else {
                id = null;
                platePos = null;
            }
        }

        if (id != null) {
            compound.putUUID("SubLevelID", id);
        }

        if (platePos != null) {
            compound.put("SwivelPlate", NbtUtils.writeBlockPos(platePos));
        }

        if (this.sequencedAngleLimit >= 0)
            compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);

        AssemblyException.write(compound, registries, this.lastException);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.targetAngleDegrees = compound.getDouble("TargetAngle");

        final SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();

        SubLevelSchematicSerializationContext.SchematicMapping mapping = null;

        if (compound.hasUUID("SubLevelID")) {
            UUID subLevelID = compound.getUUID("SubLevelID");

            if (schematicContext != null) {
                mapping = schematicContext.getMapping(subLevelID);
            }

            if (mapping != null) {
                subLevelID = mapping.newUUID();
            }

            this.setSubLevelID(subLevelID);
        }

        if (compound.contains("SwivelPlate")) {
            final BlockPos blockPos = NbtUtils.readBlockPos(compound, "SwivelPlate").orElseThrow();
            this.setPlatePos(blockPos);
        }

        this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1;
        this.lastException = AssemblyException.read(compound, registries);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.removeHandle();
    }

    /**
     * Called before we assemble the swivel bearing base into a sub-level
     */
    public void beforeAssembly() {
        this.assembling = true;
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && !this.assembling) {
            // If we're actually removed and not just unloaded, let's break the plate as well
            this.destroyPlate();
        }

        super.remove();
    }

    public boolean isAssembled() {
        return this.getBlockState().getValue(SwivelBearingBlock.ASSEMBLED);
    }

    private @Nullable SubLevel getAttachedSubLevel() {
        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
        return container.getSubLevel(this.subLevelID);
    }

    private @Nullable SubLevel getContainingSubLevel() {
        return Sable.HELPER.getContaining(this);
    }

    private boolean isLocking() {
        return this.getBlockState().getValue(BlockStateProperties.POWERED);
    }

    private @NotNull Vector3d getConstraintPos(final BlockPos relative, final BlockPos offset) {
        return JOMLConversion.toJOML(relative.offset(offset).getCenter());
    }

    private void destroyPlate() {
        final BlockPos platePos = this.getPlatePos();
        if (platePos != null) {
            final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
            if (container == null) return;

            final SubLevel subLevel = container.getSubLevel(this.subLevelID);
            if (subLevel == null) return;

            if (this.getLevel().getBlockState(platePos).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
                SimBlocks.SWIVEL_BEARING_LINK_BLOCK.get().withBlockEntityDo(this.level, platePos, SwivelBearingPlateBlockEntity::beforeAssembly);
                this.getLevel().setBlock(platePos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private void removeHandle() {
        if (this.handle != null) {
            this.handle.remove();
            this.handle = null;
        }
    }

    public double getTargetAngleDegrees() {
        return this.targetAngleDegrees;
    }

    @Override
    public @NotNull KineticBlockEntity getExtraKinetics() {
        return this.cogwheel;
    }

    @Override
    public boolean shouldConnectExtraKinetics() {
        return false;
    }

    @Override
    public String getExtraKineticsSaveName() {
        return "SwivelCog";
    }

    @Override
    public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
        return this.getPlatePos() != null && stateTo.getBlock() instanceof SwivelBearingPlateBlock ? 1 : super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public boolean isCustomConnection(final KineticBlockEntity other, final BlockState state, final BlockState otherState) {
        return this.getPlatePos() != null && otherState.getBlock() instanceof SwivelBearingPlateBlock;
    }

    @Override
    public List<BlockPos> addPropagationLocations(final IRotate block, final BlockState state, final List<BlockPos> neighbours) {
        if (this.getPlatePos() != null) {
            neighbours.add(this.getPlatePos());
        }

        return super.addPropagationLocations(block, state, neighbours);
    }

    public @Nullable BlockPos getPlatePos() {
        return this.swivelPlatePos;
    }

    public void setPlatePos(@Nullable final BlockPos swivelPlatePos) {
        this.swivelPlatePos = swivelPlatePos;
    }

    public @Nullable UUID getSubLevelID() {
        return this.subLevelID;
    }

    public void setSubLevelID(@Nullable final UUID subLevelID) {
        this.subLevelID = subLevelID;
    }

    // passthrough shaft should not cost stress
    @Override
    public float calculateStressApplied() {
        return 0;
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return this.lastException;
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        final SubLevel attachedSubLevel = this.getAttachedSubLevel();

        if (attachedSubLevel == null) {
            return null;
        }

        return List.of(attachedSubLevel);
    }

    /**
     * TODO: separate icon for the locked settings
     */
    public enum LockingSetting implements INamedIconOptions {
        LOCKED_ALWAYS(AllIcons.I_CONFIG_LOCKED, "swivel_default_always_locked"),
        LOCKED_DEFAULT(AllIcons.I_CONFIG_LOCKED, "swivel_default_locked"),
        UNLOCKED_DEFAULT(AllIcons.I_CONFIG_UNLOCKED, "swivel_default_unlocked"),
        UNLOCKED_ALWAYS(AllIcons.I_CONFIG_UNLOCKED, "swivel_default_always_unlocked");

        private final String translationKey;
        private final AllIcons icon;

        LockingSetting(final AllIcons icon, final String name) {
            this.icon = icon;
            this.translationKey = Simulated.MOD_ID + ".generic." + name;
        }

        @Override
        public AllIcons getIcon() {
            return this.icon;
        }

        @Override
        public String getTranslationKey() {
            return this.translationKey;
        }

        public boolean shouldLock(final int signal) {
            if (this == UNLOCKED_ALWAYS) return false;
            if (this == LOCKED_ALWAYS) return true;
            return signal > 0 != (this == LockingSetting.LOCKED_DEFAULT);
        }
    }

    private static class SelectionModeValueBox extends CenteredSideValueBoxTransform {
        public SelectionModeValueBox(final BiPredicate<BlockState, Direction> allowedDirections) {
            super(allowedDirections);
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            return super.getLocalOffset(level, pos, state)
                    .subtract(Vec3.atLowerCornerOf(state.getValue(SwivelBearingBlock.FACING).getNormal())
                            .scale(5 / 16f));
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.75);
        }

        @Override
        public float getScale() {
            return 0.35f;
        }
    }

    public static class SwivelBearingCogwheelBlockEntity extends KineticBlockEntity implements ExtraKineticsBlockEntity {
        public static final ICogWheel EXTRA_COGWHEEL_CONFIG = new ICogWheel() {
            @Override
            public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
                return false;
            }

            @Override
            public Direction.Axis getRotationAxis(final BlockState state) {
                return state.getValue(SwivelBearingBlock.FACING).getAxis();
            }
        };

        private final SwivelBearingBlockEntity parent;

        public SwivelBearingCogwheelBlockEntity(final BlockEntityType<?> typeIn, final BlockPos pos, final BlockState state, final SwivelBearingBlockEntity parent) {
            super(typeIn, new ExtraBlockPos(pos), state);
            this.parent = parent;
        }

        @Override
        public void onSpeedChanged(final float previousSpeed) {
            super.onSpeedChanged(previousSpeed);

            if (this.speed != 0.0 && !this.parent.isAssembled()) {
                this.parent.assembleNextTick = true;
            }

            this.parent.sequencedAngleLimit = -1;

            if (this.sequenceContext != null && this.sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE) {
                this.parent.sequencedAngleLimit = this.sequenceContext.getEffectiveValue(this.getTheoreticalSpeed());
            }
        }

        @Override
        public KineticBlockEntity getParentBlockEntity() {
            return this.parent;
        }

        @Override
        protected void addStressImpactStats(final List<Component> tooltip, final float stressAtBase) {
            super.addStressImpactStats(tooltip, stressAtBase);
        }

        @Override
        protected boolean canPropagateDiagonally(final IRotate block, final BlockState state) {
            return true;
        }

        @Override
        public Component getKey() {
            return SimLang.translate("extra_kinetics.extra_cogwheel").component();
        }
    }
}