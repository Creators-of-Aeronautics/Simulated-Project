package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.compat.computercraft.wired.DockingConnectorWiredElement;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.*;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.simulated_team.simulated.util.SimMovementContext;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DockingConnectorBlockEntity extends SmartBlockEntity implements SimMagnet, BlockEntitySubLevelActor, Clearable {
    public static MagnetMap<DockingConnectorBlockEntity> MAGNET_CONTROLLER = new MagnetMap<>();
    public boolean powered;
    public LerpedFloat extension = LerpedFloat.linear().chase(0, 0.1, LerpedFloat.Chaser.LINEAR);
    public LerpedFloat feet = LerpedFloat.linear().chase(0, 0.15, LerpedFloat.Chaser.LINEAR);
    public DockingConnectorSoloInventory inventory;
    public DockingConnectorTank tank;
    public BlockPos otherConnectorPosition = null;
    public UUID otherConnectorSubLevelId = null;
    protected DockingConnectorState state = DockingConnectorState.UNPOWERED;
    protected boolean virtualLock = false;
    protected double closestPairDistance = 0;
    private MagnetBehaviour magnetBehaviour;
    private FixedConstraintHandle constraintHandle;
    public final DockingConnectorWiredElement ccWiredElement;

    private ConstraintSmoother constraintSmoother = null;

    public DockingConnectorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.inventory = new DockingConnectorSoloInventory();
        this.tank = new DockingConnectorTank(this);
        this.ccWiredElement = DockingConnectorWiredElement.create(this);
    }

    @Nullable
    public DockingConnectorBlockEntity getOtherConnector() {
        if (this.otherConnectorPosition == null)
            return null;

        return this.level.getBlockEntity(this.otherConnectorPosition) instanceof final DockingConnectorBlockEntity be ? be : null;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        this.magnetBehaviour = new MagnetBehaviour(this, MAGNET_CONTROLLER);
        behaviours.add(this.magnetBehaviour);
    }

    @Override
    public void initialize() {
        super.initialize();

        // dock on re-loading
        final DockingConnectorBlockEntity otherConnector = this.getOtherConnector();

        if (otherConnector != null && this.constraintHandle == null && otherConnector.constraintHandle == null) {
            final MagnetMap<DockingConnectorBlockEntity> controller = DockingConnectorBlockEntity.MAGNET_CONTROLLER;
            if (controller.getPair(this.level, this.getBlockPos(), this.otherConnectorPosition) == null) {
                controller.tryAddPair(this.level, this.getBlockPos(), this.otherConnectorPosition, DockingConnectorPair::new);
                final DockingConnectorPair pair = (DockingConnectorPair) controller.getPair(this.level, this.getBlockPos(), this.otherConnectorPosition);

                if (pair != null) {
                    pair.dock(true);
                    this.notifyUpdate();
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        final BlockState state = this.getBlockState();
        final Direction direction = state.getValue(BlockStateProperties.FACING);
        final BlockPos pos = this.getBlockPos();

        final BlockPos frontPos = pos.relative(direction);
        final BlockState frontState = this.level.getBlockState(frontPos);

        this.powered = state.getValue(BlockStateProperties.POWERED);
        if (!frontState.isAir() && (!frontState.is(SimBlocks.PAIRED_DOCKING_CONNECTOR.get()) || frontState.getValue(BlockStateProperties.FACING).getOpposite() != direction)) {
            this.powered = false;
        }
        if (!this.level.isClientSide() && this.isExtended()) {
            this.searchForPairs();

            if (!frontState.is(SimBlocks.PAIRED_DOCKING_CONNECTOR.get())) {
                this.level.setBlock(frontPos, SimBlocks.PAIRED_DOCKING_CONNECTOR.get().defaultBlockState().setValue(BlockStateProperties.FACING, direction.getOpposite()), 3);
            }
        }

        if (this.otherConnectorPosition != null && !this.level.isClientSide) {
            if (!(this.level.getBlockEntity(this.otherConnectorPosition) instanceof final DockingConnectorBlockEntity be && Objects.equals(be.otherConnectorPosition, this.getBlockPos()))) {
                this.unDock();
                this.state = DockingConnectorState.EXTENDED;
                this.sendData();
            }
        }

        final float previousExtensionTarget = this.extension.getChaseTarget();
        this.extension.updateChaseTarget(this.powered ? 1 : 0);
        if (this.extension.getChaseTarget() != previousExtensionTarget) {
            if (this.level.isClientSide()) {
                this.level.playLocalSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        this.powered ? SimSoundEvents.DOCKING_CONNECTOR_EXTENDS.event() : SimSoundEvents.DOCKING_CONNECTOR_RETRACTS.event(),
                        SoundSource.BLOCKS,
                        0.75f,
                        1.0f,
                        false);
            } else {
                if (!this.powered && frontState.is(SimBlocks.PAIRED_DOCKING_CONNECTOR.get())) {
                    this.level.setBlock(frontPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        final boolean previousExtended = this.isExtended();
        this.extension.tickChaser();
        if (previousExtended != this.isExtended())
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(DockingConnectorBlock.EXTENDED, this.isExtended()), 6);
        final float previousFeetValue = this.feet.getValue();
        this.feet.updateChaseTarget(this.hasOtherConnector() || this.virtualLock ? 1 : 0);
        this.feet.tickChaser();
        /*if (this.level.isClientSide() && this.isFeetExtended() && previousFeetValue != 1.0F) {
            final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
            this.level.playLocalSound(
                    pos.getX() + 0.5 + facing.getStepX() * 1.5,
                    pos.getY() + 0.5 + facing.getStepY() * 1.5,
                    pos.getZ() + 0.5 + facing.getStepZ() * 1.5,
                    SimSoundEvents.DOCKING_CONNECTOR_DOCKS.getMainEvent(),
                    SoundSource.BLOCKS,
                    0.75f,
                    1.0f,
                    false);
        }*/

        this.updateState();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        if (this.state == DockingConnectorState.EXTENDED || this.state == DockingConnectorState.LOCKING) {
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    public void setVirtualLock(final boolean lock) {
        this.virtualLock = lock;
    }

    private void removeConstraint() {
        if (this.constraintHandle != null) {
            this.constraintHandle.remove();
            this.constraintHandle = null;
        }
        this.constraintSmoother = null;
    }

    private void attachConstraints(final DockingConnectorBlockEntity other, final Quaterniondc targetOrientation, final Vector3dc relativePos, final Quaterniondc relativeOrientation, final boolean isLocked) {
        final BlockPos pos = this.getBlockPos();
        final BlockPos otherPos = other.getBlockPos();

        final ServerSubLevel thisSubLevel = (ServerSubLevel) Sable.HELPER.getContaining(this.level, pos);
        final ServerSubLevel otherSubLevel = (ServerSubLevel) Sable.HELPER.getContaining(this.level, otherPos);
        assert thisSubLevel != null;

        final Vector3d anchorPos = JOMLConversion.toJOML(this.getTipPosition());
        final Vector3d otherAnchorPos = JOMLConversion.toJOML(other.getTipPosition());

        final ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) this.level);
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

        final double partialPhysicsTick = physicsSystem.getPartialPhysicsTick();
        final double physicsTime = this.feet.getValue((float) partialPhysicsTick);


        double lerpFactor = Mth.clamp(physicsTime * physicsTime, 0.0, 1.0);

        if (isLocked)
            lerpFactor = 1;

        final double rotationLerpFactor = Mth.clamp(lerpFactor * 2.0, 0.0, 1.0);
        if (this.constraintHandle != null)
            this.constraintHandle.remove();

        otherAnchorPos.fma(1 - lerpFactor, relativePos);

        final FixedConstraintConfiguration constraint = new FixedConstraintConfiguration(
                anchorPos,
                otherAnchorPos,
                relativeOrientation.slerp(targetOrientation, rotationLerpFactor, new Quaterniond()));

        this.constraintHandle = container.physicsSystem().getPipeline().addConstraint(thisSubLevel, otherSubLevel, constraint);
    }

    private void searchForPairs() {
        final Direction direction = this.getBlockState().getValue(BlockStateProperties.FACING);
        final MagnetMap<DockingConnectorBlockEntity> controller = DockingConnectorBlockEntity.MAGNET_CONTROLLER;
        if (this.hasOtherConnector()) {
            controller.tryAddPair(this.level, this.getBlockPos(), this.otherConnectorPosition, DockingConnectorPair::new);
            return;
        }

        final Vector3d tempRelativePos = new Vector3d();
        this.closestPairDistance = Double.MAX_VALUE;

        final SubLevel subLevel = Sable.HELPER.getContaining(this);

        final SimMovementContext context = SimMovementContext.getMovementContext(this.level, this.getBlockPos().getCenter());
        final List<SimMovementContext> contexts = controller.findNearby(context);

        for (final SimMovementContext movementContext : contexts) {
            if (movementContext.subLevel() != subLevel && this.level.getBlockEntity(movementContext.localBlockPos()) instanceof final DockingConnectorBlockEntity other) {
                if (!other.hasOtherConnector() && other.magnetActive()) {
                    controller.tryAddPair(this.level, this.getBlockPos(), movementContext.localBlockPos(), DockingConnectorPair::new);
                    //MagnetPair.getRelativePosition(this, other, tempRelativePos);
                    DockingConnectorPair.getRelativePosition(this, other, tempRelativePos);
                    this.closestPairDistance = Math.min(tempRelativePos.length(), this.closestPairDistance);
                }
            }
        }

        // connection in the same block grid
        final BlockPos sameGridConnection = this.getBlockPos().offset(direction.getNormal().multiply(3));

        if (this.isExtended() && this.level.getBlockEntity(sameGridConnection) instanceof final DockingConnectorBlockEntity other) {
            if (other.getBlockState().getValue(BlockStateProperties.FACING).getOpposite() == direction && other.isExtended()) {
                controller.tryAddPair(this.level, this.getBlockPos(), other.getBlockPos(), DockingConnectorPair::new);
            }
        }
    }

    private void updateState() {

        if (this.powered) {
            if (this.state != DockingConnectorState.LOCKED && this.isExtended()) {
                this.state = this.hasOtherConnector() ? DockingConnectorState.LOCKING : DockingConnectorState.EXTENDED;
            }
        } else {
            if (this.state != DockingConnectorState.UNPOWERED && this.hasOtherConnector()) {
                final MagnetMap<DockingConnectorBlockEntity> controller = DockingConnectorBlockEntity.MAGNET_CONTROLLER;
                final Map<MagnetPairIdentifier, MagnetPair<DockingConnectorBlockEntity>> map = controller.pairMap.get(this.level);
                if (map != null && map.get(new MagnetPairIdentifier(this.otherConnectorPosition, this.getBlockPos())) instanceof final DockingConnectorPair pair) {
                    pair.unDock();
                }
            }
            this.state = DockingConnectorState.UNPOWERED;
        }
    }

    /**
     * Forcefully sets the paired docking connector to the specified block entity.
     *
     * @param other The other connector to dock to
     */
    public void pairTo(final DockingConnectorBlockEntity other) {
        if (other.getBlockPos().equals(this.otherConnectorPosition)) {
            return;
        }

        final DockingConnectorBlockEntity otherConnector = this.getOtherConnector();
        if (otherConnector != null && this.getBlockPos().equals(otherConnector.otherConnectorPosition)) {
            otherConnector.unDock();
        }

        this.unDock();
        final MagnetMap<DockingConnectorBlockEntity> controller = DockingConnectorBlockEntity.MAGNET_CONTROLLER;
        controller.tryAddPair(this.level, this.getBlockPos(), other.getBlockPos(), DockingConnectorPair::new);
        final DockingConnectorPair pair = (DockingConnectorPair) controller.getPair(this.level, this.getBlockPos(), other.getBlockPos());

        if (pair != null) {
            pair.dock(true);
            this.notifyUpdate();
        }
    }

    public boolean isLocked() {
        return this.state == DockingConnectorState.LOCKED;
    }

    public float getPlateOffset() {
        return 0.5f + this.getExtensionDistance(0);
    }

    public boolean isExtended() {
        return this.extension.getValue() == 1 && this.powered;
    }

    public boolean isRetracted() {
        return this.extension.getValue() == 0;
    }

    public boolean isFeetExtended() {
        return this.otherConnectorPosition != null && this.feet.getValue() == 1;
    }

    public boolean hasOtherConnector() {
        return this.otherConnectorPosition != null;
    }

    public float getExtensionDistance(final float partialTick) {
        return SimMathUtils.smoothStep(this.extension.getValue(partialTick));
    }

    public float getFeetRotation(final float partialTick) {
        float rotation = this.feet.getValue(partialTick);
        final float rotationTarget = this.feet.getChaseTarget();
        if (rotationTarget == 1) {
            rotation *= rotation;
        }
        return rotation;
    }

    public void setDock(final DockingConnectorBlockEntity otherConnector, final boolean isLocked, @Nullable final Quaterniondc targetOrientation, final Vector3dc relativePos, final Quaterniondc relativeOrientation) {
        final BlockPos previous = this.otherConnectorPosition;

        final SubLevel otherSubLevel = Sable.HELPER.getContaining(otherConnector);
        this.otherConnectorPosition = otherConnector.getBlockPos();
        this.otherConnectorSubLevelId = otherSubLevel != null ? otherSubLevel.getUniqueId() : null;

        this.updateState();

        if (this.state == DockingConnectorState.LOCKING) {

            if (targetOrientation != null && this.constraintSmoother == null) {
                this.constraintSmoother = new ConstraintSmoother(otherConnector, targetOrientation, relativePos, relativeOrientation);
            }
            if (isLocked) {
                this.state = DockingConnectorState.LOCKED;
                this.tank.connect(this.otherConnectorPosition, otherConnector.tank);
                this.ccWiredElement.connect(otherConnector.ccWiredElement);

                this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
                if (this.constraintSmoother != null) {
                    final ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) this.level);
                    this.constraintSmoother.step(container, this, 1);
                }
                this.constraintSmoother = null;
            }
        }

        if (previous != this.otherConnectorPosition) {
            if (targetOrientation == null) {
                this.removeConstraint();
            }
            this.sendData();
        }
    }

    public void unDock() {
        final DockingConnectorBlockEntity otherConnector = this.getOtherConnector();
        if (otherConnector != null) {
            this.ccWiredElement.disconnect(otherConnector.ccWiredElement);
        }

        this.closestPairDistance = Double.MAX_VALUE;

        this.otherConnectorSubLevelId = null;
        this.otherConnectorPosition = null;

        this.state = this.isExtended() ? DockingConnectorState.EXTENDED : DockingConnectorState.UNPOWERED;
        this.tank.disconnect();
        this.removeConstraint();
        this.sendData();

        this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {

        if (this.constraintSmoother != null)
            this.constraintSmoother.partialStep(this);
    }

    public void updateSignal() {
        final boolean shouldPower = this.level.hasNeighborSignal(this.worldPosition);
        if (this.powered != shouldPower) {
            this.powered = shouldPower;
            this.sendData();
        }
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        tag.putBoolean("IsPowered", this.powered);
        tag.putFloat("Extension", this.extension.getValue());
        tag.putFloat("Target", this.extension.getChaseTarget());
        tag.putFloat("Feet", this.feet.getValue());

        if (this.otherConnectorPosition != null) {
            tag.put("OtherConnector", NbtUtils.writeBlockPos(this.otherConnectorPosition));
        }

        if (this.otherConnectorSubLevelId != null) {
            tag.putUUID("OtherConnectorSubLevelId", this.otherConnectorSubLevelId);
        }

        tag.put("Inventory", this.inventory.write(registries));
        tag.put("Tank", this.tank.write());
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.powered = tag.getBoolean("IsPowered");
        this.extension.setValue(tag.getFloat("Extension"));
        this.extension.updateChaseTarget(tag.getFloat("Target"));
        this.feet.setValue(tag.getFloat("Feet"));

        // ensure current = old value for visual lerping
        this.extension.setValue(this.extension.getValue());
        this.feet.setValue(this.feet.getValue());

        if (tag.contains("OtherConnector")) {
            this.otherConnectorPosition = NbtUtils.readBlockPos(tag, "OtherConnector").orElse(null);
        } else {
            this.otherConnectorPosition = null;
        }

        if (tag.contains("OtherConnectorSubLevelId")) {
            this.otherConnectorSubLevelId = tag.getUUID("OtherConnectorSubLevelId");
        }

        this.inventory.read(registries, tag.getCompound("Inventory"));
        this.tank.read(tag.getCompound("Tank"));
        super.read(tag, registries, clientPacket);
    }

    @Override
    public boolean triggerEvent(final int id, final int type) {
        if (id == 1) {
            this.extension.updateChaseTarget(this.powered ? 1 : 0);
            this.feet.updateChaseTarget(this.hasOtherConnector() ? 1 : 0);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public void remove() {
        super.remove();
        this.removeConstraint();
        if (this.level == null || !this.level.isClientSide) {
            this.ccWiredElement.remove();
        }
    }

    @Override
    public Quaternionfc getOrientation() {
        return this.getBlockState().getValue(BlockStateProperties.FACING).getRotation();
    }

    @Override
    public SubLevel getLatestSubLevel() {
        return Sable.HELPER.getContaining(this);
    }

    @Override
    public Vector3d setMagneticMoment(final Vector3d v) {
        v.set(JOMLConversion.toJOML(Vec3.atLowerCornerOf(this.getBlockState().getValue(DockingConnectorBlock.FACING).getNormal())));
        v.mul(Math.sqrt(SimConfigService.INSTANCE.server().physics.dockingConnectorStrength.get()));
        return v;
    }

    @Override
    public Vec3 getMagnetPosition() {
        return Vec3.atCenterOf(this.getBlockPos()).add(Vec3.atLowerCornerOf(this.getBlockState().getValue(RedstoneMagnetBlock.FACING).getNormal()).scale(1.4));
    }

    public Vec3 getTipPosition() {
        return Vec3.atCenterOf(this.getBlockPos()).add(Vec3.atLowerCornerOf(this.getBlockState().getValue(RedstoneMagnetBlock.FACING).getNormal()).scale(1.5));
    }

    @Override
    public boolean magnetActive() {
        return this.isExtended() && this.constraintHandle == null;
    }

    public AABB getBoundingBox(final BlockState state) {
        return Shulker.getProgressAabb(1, state.getValue(ShulkerBoxBlock.FACING), this.getExtensionDistance(1.0F));
    }

    @Override
    public AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(1);
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        if (this.otherConnectorSubLevelId == null)
            return null;

        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
        final SubLevel otherSubLevel = container.getSubLevel(this.otherConnectorSubLevelId);

        if (otherSubLevel == null) {
            return null;
        }

        return List.of(otherSubLevel);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    private record ConstraintSmoother(
            BlockPos otherConnectorPos,
            Quaterniond targetRelativeOrientation,
            Vector3d initialRelativePosition,
            Quaterniond initialRelativeOrientation) {

        private ConstraintSmoother(
                final DockingConnectorBlockEntity otherConnectorPos,
                final Quaterniondc targetRelativeOrientation,
                final Vector3dc initialRelativePosition,
                final Quaterniondc initialRelativeOrientation) {
            this(otherConnectorPos.getBlockPos(),
                    new Quaterniond(targetRelativeOrientation),
                    new Vector3d(initialRelativePosition),
                    new Quaterniond(initialRelativeOrientation));
        }

        public void partialStep(final DockingConnectorBlockEntity connector) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) connector.level);
            final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();

            final double partialPhysicsTick = physicsSystem.getPartialPhysicsTick();
            final double physicsTime = connector.feet.getValue((float) partialPhysicsTick);


            final double lerpFactor = Mth.clamp(physicsTime * physicsTime, 0.0, 1.0);

            this.step(container, connector, lerpFactor);
        }

        public void step(final ServerSubLevelContainer container, final DockingConnectorBlockEntity connector, final double lerpFactor) {
            final BlockPos pos = connector.getBlockPos();

            if (connector.level.getBlockEntity(this.otherConnectorPos) instanceof final DockingConnectorBlockEntity other) {

                final ServerSubLevel thisSubLevel = (ServerSubLevel) Sable.HELPER.getContaining(connector.level, pos);
                final ServerSubLevel otherSubLevel = (ServerSubLevel) Sable.HELPER.getContaining(connector.level, this.otherConnectorPos);
                assert thisSubLevel != null;

                final Vector3d anchorPos = JOMLConversion.toJOML(connector.getTipPosition());
                final Vector3d otherAnchorPos = JOMLConversion.toJOML(other.getTipPosition());


                final double rotationLerpFactor = Mth.clamp(lerpFactor * 2.0, 0.0, 1.0);
                if (connector.constraintHandle != null)
                    connector.constraintHandle.remove();

                otherAnchorPos.fma(1 - lerpFactor, this.initialRelativePosition);

                final FixedConstraintConfiguration constraint = new FixedConstraintConfiguration(
                        anchorPos,
                        otherAnchorPos,
                        this.initialRelativeOrientation.slerp(this.targetRelativeOrientation, rotationLerpFactor, new Quaterniond()));

                connector.constraintHandle = container.physicsSystem().getPipeline().addConstraint(thisSubLevel, otherSubLevel, constraint);
            }
        }
    }

    // if any other mod caches this i am going to kill them with my mind
    public AbstractContainer getInventory() {
        final DockingConnectorBlockEntity other = this.getOtherConnector();
        if (other != null) {
            this.inventory.dock();
            return new DockingConnectorDuoInventory(this, other);
        } else {
            this.inventory.unDock();
            return this.inventory;
        }
    }

    public enum DockingConnectorState {
        UNPOWERED,
        EXTENDED,
        LOCKING,
        LOCKED
    }
}
