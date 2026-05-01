package dev.ryanhcode.offroad.content.blocks.borehead_bearing;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.offroad.config.OffroadConfig;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlock;
import dev.ryanhcode.offroad.content.contraptions.borehead_contraption.BoreheadBearingContraption;
import dev.ryanhcode.offroad.content.entities.BoreheadContraptionEntity;
import dev.ryanhcode.offroad.data.OffroadLang;
import dev.ryanhcode.offroad.data.OffroadTags;
import dev.ryanhcode.offroad.handlers.server.MultiMiningServerManager;
import dev.ryanhcode.offroad.handlers.server.MultiMiningSupplier;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.simulated_team.simulated.api.BearingSlowdownController;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BoreheadBearingBlockEntity extends MechanicalBearingBlockEntity implements MultiMiningSupplier {

    private static final Vector3dc IMMUT_ZERO = new Vector3d();

    private static final BoundingBox3d TEMP_BOUNDING_BOX_DOUBLE = new BoundingBox3d();
    private static final BoundingBox3i TEMP_BOUNDING_BOX_INT = new BoundingBox3i();
    private static final BlockPos.MutableBlockPos TEMP_CURSOR = new BlockPos.MutableBlockPos();
    private static final Vector3d TEMP_POSITION = new Vector3d();

    private final BearingSlowdownController slowdownController = new BearingSlowdownController();
    /**
     * A list of origin positions for each rock cutter on the connected contraption.
     */
    private final ObjectArrayList<Vector3d> centerMiningPositions = new ObjectArrayList<>();
    private final Set<BlockPos> visitedPositions = new ObjectOpenHashSet<>();
    /**
     * The next index available for rock cutter actors
     */
    private final AtomicInteger nextAvailableIndex = new AtomicInteger();

    private boolean disassemblySlowdown = false;
    private float rotationSpeed;
    /**
     * awesome sable accelerator
     */
    private LevelAccelerator accelerator;
    /**
     * The amount of rock cutter clientside
     */
    private int clientRockCutters = 0;
    /**
     * States whether the contraption associated with this borehead bearing is initialized
     */
    private boolean initialized = false;
    private boolean stalled = false;
    /**
     * the amount of ticks left before excavation starts again after being stalled.
     */
    private int stalledRecoveryTimer = 0;
    private boolean insideMainTick;

    public BoreheadBearingBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        this.movementMode.setValue(2);
        behaviours.remove(this.movementMode);
    }

    @Override
    public float calculateStressApplied() {
        final float stress = super.calculateStressApplied() * Math.max(this.getRockCuttingAmount(0), 1);
        this.lastStressApplied = stress;
        return stress;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.accelerator = new LevelAccelerator(this.getLevel());
    }

    @Override
    public void tick() {
        this.updateSpeed(); // update speed before main tick

        this.insideMainTick = true;
        super.tick();
        this.insideMainTick = false;

        if (this.isVirtual()) {
            final float angularSpeed = this.getAngularSpeed();
            final float newAngle = this.angle + angularSpeed;
            this.angle = newAngle % 360;
            return;
        }

        if (this.movedContraption != null && !this.movedContraption.isAlive()) {
            this.movedContraption = null;
        }

        if (!this.level.isClientSide) {
            this.visitedPositions.clear();
        }

        if (this.movedContraption == null) {
            this.angle = 0;
            this.setRotationSpeed(0);
            this.disassemblySlowdown = false;
            return;
        }

        if (!this.level.isClientSide) {
            this.updateMiningBlocks();
        }
    }

    /**
     * Updates how many blocks are being actively mined by this borehead contraption and ticks already mined blocks.
     */
    public void updateMiningBlocks() {
        assert this.getLevel() != null; // I'm going to kill IJ if I see this god damn yellow box one more time
        assert this.level != null;

        final int timerBefore = this.stalledRecoveryTimer;
        if (this.stalledRecoveryTimer > 0) {
            this.stalledRecoveryTimer--;
        }

        if (this.stalledRecoveryTimer == 0 && timerBefore != this.stalledRecoveryTimer) {
            this.sendData();
        }

        if (this.isActive() && Math.abs(this.getSpeed()) > 0.1f) {
            final double searchRadius = OffroadConfig.server().blocks.boreheadBearingSearchRadius.get();

            //always global, never local
            final Vector3d localPoint = new Vector3d();
            for (final Vector3d pos : this.centerMiningPositions) {
                TEMP_BOUNDING_BOX_DOUBLE.set(
                        (pos.x - (searchRadius * 2)),
                        (pos.y - (searchRadius * 2)),
                        (pos.z - (searchRadius * 2)),
                        (pos.x + (searchRadius * 2)),
                        (pos.y + (searchRadius * 2)),
                        (pos.z + (searchRadius * 2))
                );

                TEMP_BOUNDING_BOX_INT.set(TEMP_BOUNDING_BOX_DOUBLE);
                for (int x = TEMP_BOUNDING_BOX_INT.minX(); x <= TEMP_BOUNDING_BOX_INT.maxX(); x++) {
                    for (int z = TEMP_BOUNDING_BOX_INT.minZ(); z <= TEMP_BOUNDING_BOX_INT.maxZ(); z++) {
                        for (int y = TEMP_BOUNDING_BOX_INT.minY(); y <= TEMP_BOUNDING_BOX_INT.maxY(); y++) {

                            final double r = 1;
                            TEMP_POSITION.set(x + 0.5, y + 0.5, z + 0.5).sub(pos, TEMP_POSITION);
                            TEMP_POSITION.absolute(localPoint).sub(searchRadius, searchRadius, searchRadius, localPoint).add(r, r, r);
                            final double boxSDF = localPoint.max(IMMUT_ZERO, TEMP_POSITION).length() +
                                    Math.min(Math.max(localPoint.x, Math.max(localPoint.y, localPoint.z)), 0);
                            if (boxSDF - r > 0) {
                                continue;
                            }

                            TEMP_CURSOR.set(x, y, z);

                            final BlockState state = this.accelerator.getBlockState(TEMP_CURSOR);
                            if (BlockBreakingKineticBlockEntity.isBreakable(state, state.getDestroySpeed(this.accelerator, TEMP_CURSOR)) && !state.getCollisionShape(this.accelerator, TEMP_CURSOR).isEmpty()) {
                                final BlockPos cursor = TEMP_CURSOR.immutable();
                                MultiMiningServerManager.addOrRefreshPos(this.level, cursor, this);
                                this.visitedPositions.add(cursor);
                            }
                        }
                    }
                }
            }
        }

        this.accelerator.clearCache();
    }

    @Override
    public float getBreakingSpeed(final Level level, final BlockPos pos, final BlockState state) {
        if (this.isActive()) {

            final int multiplier;
            if (state.is(OffroadTags.Blocks.BOREHEAD_SUPER_EFFECTIVE)) {
                multiplier = 100;
            } else if (state.is(OffroadTags.Blocks.BOREHEAD_EFFECTIVE)) {
                multiplier = 10;
            } else {
                multiplier = 5;
            }

            //we only call this on the server
            double baseMiningSpeed = Math.clamp((Math.abs(this.getRotationSpeed() * multiplier)) / 100d, 0.01d, 16d);

            final float blockAmount = this.visitedPositions.size() / 50f;
            if (blockAmount != 0) {
                final float inversePercentage = Math.abs(this.getSpeed() / AllConfigs.server().kinetics.maxRotationSpeed.get());
                baseMiningSpeed /= Math.max(blockAmount * inversePercentage, 1);
            }


            return (float) baseMiningSpeed;
        }

        return 0;
    }

    @Override
    public BlockPos getLocation() {
        return this.isActive() ? this.worldPosition : null;
    }

    @Override
    public boolean isActive() {
        return this.initialized && this.getSpeed() != 0 && !this.isSlowingDown() && !this.isRemoved() && !this.isStalled();
    }

    @Override
    public void itemCallback(final ItemStack stack) {
        final BoreheadContraptionEntity bce = this.getContraptionEntity();
        if (!stack.isEmpty() && bce != null) {
            final BoreheadBearingContraption contraption = bce.getContraption();
            final BoreheadAttachedStorage attachedStorage = (BoreheadAttachedStorage) contraption.getStorage();
            attachedStorage.setInsertAllowed(true);

            final int inserted = this.getContraptionWrappedInventory() //we can't be null here!
                    .insertGeneral(ItemInfoWrapper.generateFromStack(stack), stack.getCount(), false);
            if (inserted == 0 && OffroadConfig.server().blocks.boreheadBearingStallingEnabled.get()) {
                this.setStalled(true);
            } else {
                stack.shrink(inserted);
            }

            attachedStorage.setInsertAllowed(false);
        }
    }

    public void updateSpeed() {
        assert this.level != null;

        if (this.isSlowingDown()) {
            if (this.slowdownController.stepGoal() && !this.level.isClientSide) {
                this.disassemble();
                return;
            }

            this.setRotationSpeed(this.slowdownController.getSpeed(0));
            this.angle = this.slowdownController.getAngle(0);
        } else {
            float targetSpeed;
            if (this.isVirtual()) {
                targetSpeed = super.getAngularSpeed() / 4f;
            } else {
                targetSpeed = super.getAngularSpeed() / OffroadConfig.server().blocks.boreheadBearingRotationDivisor.getF();
            }

            final int currentRockCutters = this.getRockCuttingAmount(10);
            float fullMass = currentRockCutters * 5;

            if (this.isStalled()) {
                targetSpeed /= 4f;
                fullMass = currentRockCutters;
            } else if (this.getSpeed() == 0) {
                targetSpeed = 0;
                fullMass = currentRockCutters;
            }

            //if we're slowing down, don't have rotation, there's no need to try to lerp after a certain point
            if (Math.abs(this.rotationSpeed) <= 0.05 && this.getSpeed() == 0) {
                this.rotationSpeed = 0;
                return;
            }

            this.rotationSpeed = Mth.lerp(1f / fullMass, this.rotationSpeed, targetSpeed);
        }
    }

    private int getRockCuttingAmount(final int minimumRockcuttingWheelAmount) {
        assert this.level != null;
        int rockCuttingWheelAmount = this.level.isClientSide ? this.clientRockCutters : this.centerMiningPositions.size();

        if (minimumRockcuttingWheelAmount > 0) {
            rockCuttingWheelAmount = Math.max(rockCuttingWheelAmount, minimumRockcuttingWheelAmount);
        }

        return rockCuttingWheelAmount;
    }

    @Override
    public float getInterpolatedAngle(final float partialTicks) {
        if (!this.isVirtual() && this.isSlowingDown()) { //we can't be slowing down (disassembling) in a ponder
            return this.slowdownController.getAngle(partialTicks);
        }

        if (this.isVirtual()) {
            final float angSpeed = this.getAngularSpeed();
            return Mth.lerp(partialTicks, this.angle, this.angle + angSpeed);
        }

        return super.getInterpolatedAngle(partialTicks);
    }

    @Override
    public float getAngularSpeed() {
        assert this.level != null;
        if (this.insideMainTick && this.disassemblySlowdown) {
            float slowDownSpeed = this.slowdownController.getSpeed(1);

            if (this.level.isClientSide) {
                slowDownSpeed *= ServerSpeedProvider.get();
                slowDownSpeed += this.clientAngleDiff / 3f;
            }

            return slowDownSpeed;
        }

        return this.rotationSpeed;
    }

    public void startUnstalling() {
        if (this.stalled) {
            this.setStalled(false);
            this.stalledRecoveryTimer = OffroadConfig.server().blocks.boreheadBearingStallRecoveryTicks.get();
        }
    }

    public BoreheadContraptionEntity getContraptionEntity() {
        if (this.movedContraption instanceof final BoreheadContraptionEntity bce) {
            return bce;
        }

        return null;
    }

    @Nullable
    public InventoryLoaderWrapper getContraptionWrappedInventory() {
        if (this.movedContraption instanceof final BoreheadContraptionEntity bce) {
            return bce.getContraption().getSimWrappedStorage();
        }

        return null;
    }

    public void setAssembleNextTick(final boolean assembleNextTick) {
        this.assembleNextTick = assembleNextTick;
    }

    /**
     * most of this is copied from the bearing block...
     */
    @Override
    public void assemble() {
        if (!(this.level.getBlockState(this.worldPosition).getBlock() instanceof BoreheadBearingBlock)) {
            return;
        }

        final Direction direction = this.getBlockState().getValue(BlockStateProperties.FACING);
        final BoreheadBearingContraption contraption = new BoreheadBearingContraption(direction);

        try {
            if (!contraption.assemble(this.level, this.worldPosition)) {
                return;
            }

            this.lastException = null;
        } catch (final AssemblyException e) {
            this.lastException = e;
            this.sendData();
            return;
        }

        contraption.removeBlocksFromWorld(this.level, BlockPos.ZERO);
        this.movedContraption = BoreheadContraptionEntity.create(this.level, this, contraption);
        final BlockPos anchor = this.worldPosition.relative(direction);
        this.movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        this.movedContraption.setRotationAxis(direction.getAxis());
        this.level.addFreshEntity(this.movedContraption);

        this.running = true;
        this.angle = 0;

        this.setRotationSpeed(0);
        this.initializeContraption();
        this.updateGeneratedRotation();

        this.sendData();
    }

    @Override
    public void attach(final ControlledContraptionEntity contraption) {
        assert this.level != null;

        // copied from mechanical bearing super class, changed to account for specifically BlockStateProperties.FACING
        final BlockState blockState = this.getBlockState();

        if (!(contraption.getContraption() instanceof BearingContraption))
            return;

        if (!blockState.hasProperty(BlockStateProperties.FACING))
            return;

        this.movedContraption = contraption;
        this.setChanged();
        final BlockPos anchor = this.worldPosition.relative(blockState.getValue(BlockStateProperties.FACING));
        this.movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        if (!this.level.isClientSide) {
            this.running = true;
            this.sendData();
        }


        this.initializeContraption();
    }

    public void initializeContraption() {
        assert this.level != null;

        if (!this.level.isClientSide && this.movedContraption instanceof final BoreheadContraptionEntity bce) {
            ((BoreheadAttachedStorage) bce.getContraption().getStorage())
                    .attachBlockEntity(this);
        }

        if (!this.initialized && this.movedContraption != null && !this.level.isClientSide) {
            this.resetCenterMiningInfo();

            final Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks = this.movedContraption.getContraption().getBlocks();

            for (final Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> set : blocks.entrySet()) {
                if (set.getValue().state().getBlock() instanceof RockCuttingWheelBlock) {
                    // the actors themselves will populate the correct positions
                    // this also acts as the total number of connected rock cutting blocks
                    this.centerMiningPositions.add(new Vector3d());
                }
            }

            this.initialized = true;
        }
    }

    @Override
    public void disassemble() {
        this.assembleNextTick = false;

        if (this.running && this.movedContraption != null) {
            this.angle = 0;
            this.applyRotation();
            super.disassemble();
        }

        this.resetCenterMiningInfo();
        this.initialized = false;
        this.setStalled(false);

        final KineticNetwork network = this.getOrCreateNetwork();
        if (network != null) {
            network.updateStressFor(this, this.calculateStressApplied());
        }

        this.sendData();
    }

    private void resetCenterMiningInfo() {
        this.centerMiningPositions.clear();
        this.nextAvailableIndex.set(0);

        this.visitedPositions.clear();
    }

    public void startDisassemblySlowdown() {
        if (!this.isSlowingDown() && this.movedContraption != null) {
            final int rockCuttingAmount = this.getRockCuttingAmount(12);

            this.slowdownController.generate(1 + BearingSlowdownController.TIMER_SCALE * (rockCuttingAmount * rockCuttingAmount),
                    this.getInterpolatedAngle(0),
                    this.getRotationSpeed(),
                    this.getBlockState().getValue(BlockStateProperties.FACING),
                    this.getMovedContraption().getContraption());

            this.disassemblySlowdown = true;
            this.sendData();
        }
    }

    public float getRotationSpeed() {
        return this.rotationSpeed;
    }

    public void setRotationSpeed(final float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public boolean isSlowingDown() {
        return this.disassemblySlowdown;
    }

    public int requestNewIndexAndIncrement(final MovementContext context) {
        if (context.state.getBlock() instanceof RockCuttingWheelBlock) {
            // make sure we can't exceed the total number of rock cutters, even if this shouldn't be possible
            if (this.nextAvailableIndex.get() < this.centerMiningPositions.size()) {
                return this.nextAvailableIndex.getAndIncrement();
            }
        }

        return -1;
    }

    public void updatePosition(final int index, final Vec3 originPosition) {
        if (index < this.centerMiningPositions.size()) {
            this.centerMiningPositions.get(index).set(originPosition.x, originPosition.y, originPosition.z);
        }
    }

    @Override
    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("RotationSpeed", this.getRotationSpeed());
        if (clientPacket) {
            compound.putInt("BlockBreakingAmount", this.visitedPositions.size());
        }

        compound.putBoolean("DisassemblySlowdown", this.disassemblySlowdown);
        if (this.disassemblySlowdown) {
            this.slowdownController.serializeIntoNBT(compound);
        }

        compound.putBoolean("ContraptionInitialized", this.initialized);
        compound.putBoolean("Stalled", this.stalled);
        compound.putInt("StalledRecoveryTimer", this.stalledRecoveryTimer);

        if (this.initialized) {
            if (clientPacket) {
                compound.putInt("ClientRockCutterAmount", this.centerMiningPositions.size());
            } else {
                compound.putInt("OriginPositionSize", this.centerMiningPositions.size());

                final ListTag originListTag = new ListTag();
                for (final Vector3d originPos : this.centerMiningPositions) {
                    final CompoundTag originCompoundTag = new CompoundTag();
                    originCompoundTag.putDouble("x", originPos.x);
                    originCompoundTag.putDouble("y", originPos.y);
                    originCompoundTag.putDouble("z", originPos.z);
                    originCompoundTag.putInt("indexPosition", this.centerMiningPositions.indexOf(originPos));
                    originListTag.add(originCompoundTag);
                }

                compound.put("OriginPositions", originListTag);
                compound.putInt("NextAvailableIndex", this.nextAvailableIndex.get());
            }
        }
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.setRotationSpeed(compound.getFloat("RotationSpeed"));

        this.disassemblySlowdown = compound.getBoolean("DisassemblySlowdown");
        if (this.disassemblySlowdown) {
            this.slowdownController.deserializeFromNBT(compound);
        }

        this.initialized = compound.getBoolean("ContraptionInitialized");
        this.setStalled(compound.getBoolean("Stalled"));
        this.stalledRecoveryTimer = compound.getInt("StalledRecoveryTimer");
        if (this.initialized) {
            if (clientPacket) {
                this.clientRockCutters = compound.getInt("ClientRockCutterAmount");
            } else {
                for (int i = 0; i < compound.getInt("OriginPositionSize"); i++) {
                    this.centerMiningPositions.add(new Vector3d()); //populate the correct number of entries
                }

                final ListTag originTagList = compound.getList("OriginPositions", CompoundTag.TAG_COMPOUND);
                for (final Tag tag : originTagList) {
                    final CompoundTag originCompoundTag = (CompoundTag) tag;
                    final int indexPos = originCompoundTag.getInt("indexPosition");
                    final Vector3d originPos = this.centerMiningPositions.get(indexPos);
                    originPos.set(
                            originCompoundTag.getDouble("x"),
                            originCompoundTag.getDouble("y"),
                            originCompoundTag.getDouble("z")
                    );
                }

                this.nextAvailableIndex.set(compound.getInt("NextAvailableIndex"));
            }
        }
    }

    @Override
    public BoreheadContraptionEntity getMovedContraption() {
        return (BoreheadContraptionEntity) this.movedContraption;
    }

    public float handleAxisModification(final Direction direction) {
        return direction.getAxisDirection().getStep();
    }

    /**
     * @return Whether this borehead bearing is stalled due to a full contraption inventory
     */
    public boolean isStalled() {
        return this.stalled || this.stalledRecoveryTimer > 0;
    }

    public void setStalled(final boolean stalled) {
        this.stalled = stalled;
    }

    @Override
    public boolean addExceptionToTooltip(final List<Component> tooltip) {
        final boolean original = super.addExceptionToTooltip(tooltip);

        if (!original && this.isStalled()) {
            if (!tooltip.isEmpty()) {
                tooltip.add(CommonComponents.EMPTY);
            }

            OffroadLang.translate("exceptions.borehead_bearing.too_many_items")
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip);

            Arrays.stream(OffroadLang.translate("exceptions.borehead_bearing.too_many_items_description").component().getString().split("\n"))
                    .forEach(l -> TooltipHelper.cutStringTextComponent(l, FontHelper.Palette.GRAY_AND_WHITE)
                            .forEach(c -> OffroadLang.builder().add(c).forGoggles(tooltip)));

            return true;
        }

        return original;
    }
}
