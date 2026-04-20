package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.source.ItemThroughputDisplaySource;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups.AugerDistributor;
import dev.simulated_team.simulated.content.particle.AugerIndicatorParticleData;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.SimInventoryService;
import dev.simulated_team.simulated.util.Observable;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public class AugerShaftBlockEntity extends KineticBlockEntity implements ItemReciever, Observable, Clearable {

    /**
     * The Maximum amount of ticks it takes for the slot this auger shaft is transferring to move to the next auger.
     */
    private static final float MAX_AUGER_SPEED_TICKS = 8; //5 would be equivalent to a chute (4 chutes / second for a stack)


    private LevelAccelerator accelerator;

    /**
     * The attached distribution group for this auger shaft.
     */
    private AugerDistributor attachedGroup;

    /**
     * The actor inventory of this auger, used to store gathered items.
     */
    public AugerActorInventory actorInventory;
    /**
     * The slot this auger is moving.
     */
    public AugerInventory inventory;

    /**
     * The progress the held item has made across this auger shaft. Used for both rendering and item transportation. <br/>
     * Ranges from 0-1.
     */
    private final LerpedFloat updateTracker = LerpedFloat.linear();

    /**
     * The interger representation of flow direction for this auger.
     */
    public int intDirection;
    /**
     * The Direction view of flow for this auger
     */
    public Direction flowDirection;

    @ApiStatus.Internal
    public boolean beingWrenched;

    /**
     * The amount of items moved in the pass second period. reset every second.
     */
    private int itemsMoved;
    /**
     * Whether this auger shaft is at max speed.
     */
    private boolean maxSpeed;

    /**
     * Set by {@link AugerShaftBlockEntity#addToGoggleTooltip(List, boolean)} for indicator particles
     */
    private boolean observed = false;
    /**
     * Ensures no particle spam
     */
    private int particleCooldown = 100;

    public AugerShaftBlockEntity(final BlockEntityType<?> type,
                                 final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.inventory = new AugerInventory(this);
        this.actorInventory = new AugerActorInventory(this, 8);

        this.setLazyTickRate(20);

        this.updateTracker.chase(1, 0, LerpedFloat.Chaser.LINEAR);
        this.effects = new AugerKineticEffectHandler(this);
    }

    @Override
    public void tick() {
        assert this.level != null;
        if (this.accelerator == null) {
            this.accelerator = new LevelAccelerator(this.level);
        }

        this.intDirection = Mth.sign(this.getSpeed());
        this.flowDirection = Direction.get(this.intDirection == 1 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE, this.getBlockState().getValue(AugerShaftBlock.AXIS));

        super.tick();

        this.handleUpdateTracking();

        this.accelerator.clearCache();

        if (this.level.isClientSide) {
            this.particleCooldown--;
            if (this.observed && this.particleCooldown < 0) {
                this.particleCooldown = 100;
                this.effects.spawnRotationIndicators();
            }
            this.observed = false;
        }
    }

    private void handleUpdateTracking() {
        assert this.level != null;
        if (this.getSpeed() == 0) {
            this.resetUpdateTracker();
            return;
        }

        if (this.inventory.isEmpty()) {
            if (!this.level.isClientSide) {
                this.extract();
            }

            //if we're still empty after attempting to extract, return early
            if (this.inventory.isEmpty()) {
                this.resetUpdateTracker();
                return;
            }
        }

        this.updateTracker.chase(1, this.getItemSpeed(), LerpedFloat.Chaser.LINEAR);
        this.updateTracker.tickChaser();

        if (this.updateTracker.settled()) {
            this.handleItemPassed();
            this.resetUpdateTracker();
        }
    }

    private void extract() {
        assert this.level != null;

        if (!this.actorInventory.isEmpty()) {
            ContainerSlot largestSlot = null;
            for (final ContainerSlot populatedSlot : this.actorInventory.getPopulatedSlots()) {
                if (largestSlot == null || largestSlot.getStack().getCount() < populatedSlot.getStack().getCount()) {
                    largestSlot = populatedSlot;
                }
            }

            if (largestSlot != null) {
                final ItemStack extracted = this.actorInventory.extractSlot(largestSlot.getIndex(), largestSlot.getStack().getCount(), false);
                this.inventory.insertSlot(extracted, 0, false);
            }
        }

        //no extraction from actor inventory, extract from storage instead
        if (this.inventory.isEmpty()) {
            final Direction antiFlowDir = this.flowDirection.getOpposite();
            final BlockPos antiFlowPos = this.worldPosition.relative(antiFlowDir);
            if (!(this.accelerator.getBlockState(antiFlowPos).getBlock() instanceof AugerShaftBlock)) {
                final InventoryLoaderWrapper wrapped = SimInventoryService.INSTANCE.getInventory(this.level.getBlockEntity(antiFlowPos), antiFlowDir);
                if (wrapped != null) {
                    final ItemStack extracted = wrapped.extractAny(16, true, false);
                    if (!extracted.isEmpty()) {
                        this.inventory.insertSlot(wrapped.extractAny(16, false, false), 0, false);
                    }
                }
            }
        }
    }

    /**
     * Handles movement of the current held item stack across augers
     */
    private void handleItemPassed() {
        assert this.level != null && !this.level.isClientSide;

        final BlockEntity gatheredBE = this.level.getBlockEntity(this.worldPosition.relative(this.flowDirection));
        if (gatheredBE instanceof final AugerShaftBlockEntity abe) {
            final int beforeCount = this.inventory.getItem(0).getCount();

            this.inventory.slot.setStack(abe.inventory.insertSlot(this.inventory.slot.getStack(), 0, false));

            final int totalMoved = beforeCount - this.inventory.slot.getStack().getCount();
            this.itemsMoved += totalMoved;

            if (totalMoved != 0) {
                this.notifyUpdate();
            }
        }
    }

    private float getItemSpeed() {
        final float totalTicks = MAX_AUGER_SPEED_TICKS / (Math.abs(this.getSpeed()) / 256f);
        this.maxSpeed = totalTicks == MAX_AUGER_SPEED_TICKS;

        return 1 / totalTicks;
    }

    private void resetUpdateTracker() {
        this.updateTracker.startWithValue(0);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        if (!this.level.isClientSide && this.getSpeed() != 0) {
            this.refreshActors();
        }

        if (!this.level.isClientSide) {
            DisplayLinkBlock.sendToGatherers(this.level, this.getBlockPos(),
                    (dlbe, a) -> a.itemReceived(dlbe, this.itemsMoved), ItemThroughputDisplaySource.class);
            this.itemsMoved = 0;
        }

        if (this.level.isClientSide && this.maxSpeed && this.itemsMoved > 0) {
            this.sendObserved(this.getBlockPos());
        }
    }

    private void refreshActors() {
        final Direction dir = this.flowDirection.getOpposite();
        final Direction.Axis axis = dir.getAxis();

        final BlockPos relPos = this.getBlockPos().relative(dir);
        if (this.accelerator.getBlockState(relPos).hasBlockEntity()) {
            final BlockEntity be = this.level.getBlockEntity(relPos);
            if (be instanceof final BlockHarvester harvester) {
                if (this.attachedGroup != null) {
                    this.attachedGroup.removeReceiver(this);
                }

                final AugerDistributor distributor = harvester.simulated$getAssociatedDistributor();
                if (distributor != null) {
                    this.attachedGroup = distributor;
                    this.attachedGroup.addReceiver(this);
                } else {
                    this.attachedGroup = new AugerDistributor();
                    this.attachedGroup.addReceiver(this);
                }
            }
        } else {
            return;
        }

        if (this.attachedGroup != null) {
            this.attachedGroup.gatherAndAssociateHarvesters(SimDirectionUtil.getSurroundingDirections(axis), relPos, this.getLevel(), this.accelerator);
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        if (!this.level.isClientSide && !this.beingWrenched) {
            Containers.dropContents(this.level, this.worldPosition, this.inventory);
            this.inventory.clearContent();

            this.actorInventory.clearAndDropContents(this.level, this.worldPosition);
            this.resetUpdateTracker();
        }
    }

    @Override
    protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        compound.put("Inventory", this.inventory.write(registries));
        compound.put("ActorInventory", this.actorInventory.write(registries));

        if (!clientPacket) {
            compound.putFloat("Progress", this.updateTracker.getValue());
        }
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        this.inventory.read(registries, compound.getCompound("Inventory"));
        this.actorInventory.read(registries, compound.getCompound("ActorInventory"));

        if (!clientPacket) {
            this.updateTracker.setValue(compound.getFloat("Progress"));
        }
    }

    public AugerInventory getInventory() {
        return this.inventory;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (this.getSpeed() > 0) {
            final float perTickSpeed = this.getItemSpeed();
            final float perSecondSpeed = perTickSpeed * 20;
            SimLang.translate("auger_shaft.item_flow", Math.floor(perSecondSpeed * 100) / 100)
                    .style(ChatFormatting.YELLOW)
                    .forGoggles(tooltip);
        }

        final int actorItems = this.actorInventory.storedItemCount;
        if (actorItems > 0) {
            SimLang.translate("auger_shaft.actor_items", actorItems)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
        }

        final int count = this.inventory.slot.getStack().getCount();
        if (count > 0) {
            CreateLang.translate("tooltip.chute.contains", Component.translatable(this.inventory.slot.getStack().getDescriptionId())
                            .getString(), count)
                    .style(ChatFormatting.GREEN)
                    .forGoggles(tooltip);
        }

        this.observed = true;
        return true;
    }

    @Override
    public ItemStack onRecieveItem(ItemStack item, final BlockPos fromPos) {
        if (!this.isSpeedRequirementFulfilled() || this.isOverStressed()) {
            return item;
        }

        final ItemInfoWrapper info = ItemInfoWrapper.generateFromStack(item);
        final long amountInserted = this.actorInventory.insertGeneral(info, item.getCount(), true);
        if (amountInserted > 0) {
            this.actorInventory.insertGeneral(info, item.getCount(), false);
            item = item.copy();
            item.shrink((int) amountInserted);
            return item;
        }

        return item;
    }

    @Override
    public boolean removed() {
        return this.isRemoved();
    }

    @Override
    public boolean isActive() {
        return this.getSpeed() != 0;
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
        this.actorInventory.clearContent();
    }

    public class AugerKineticEffectHandler extends KineticEffectHandler {
        public AugerKineticEffectHandler(final KineticBlockEntity kte) {
            super(kte);
        }

        public void spawnRotationIndicators() {
            final AugerShaftBlockEntity auger = AugerShaftBlockEntity.this;
            final float speed = auger.getSpeed();
            if (speed == 0)
                return;

            final BlockState state = auger.getBlockState();
            final Block block = state.getBlock();
            if (!(block instanceof final KineticBlock kb))
                return;

            final float radius1 = kb.getParticleInitialRadius();
            final float radius2 = kb.getParticleTargetRadius();

            final Direction direction = auger.flowDirection;

            final BlockPos pos = auger.getBlockPos();
            final Level level = auger.getLevel();
            if (direction == null || auger.speed == 0)
                return;
            if (level == null)
                return;

            final Vec3 vec = VecHelper.getCenterOf(pos);
            final IRotate.SpeedLevel speedLevel = IRotate.SpeedLevel.of(speed);
            final int color = speedLevel.getColor();
            int particleSpeed = speedLevel.getParticleSpeed();
            particleSpeed *= (int) Math.signum(speed);

            for (int i = 0; i < 3; i++) {
                final AugerIndicatorParticleData particleData =
                        new AugerIndicatorParticleData(color, particleSpeed, radius1, radius2, i / 3f, 10, direction);
                if (level instanceof final ServerLevel serverLevel) {
                    serverLevel.sendParticles(particleData, vec.x, vec.y, vec.z, 20, 0, 0, 0, 1);
                } else {
                    for (int j = 0; j < 20; j++) {
                        level.addParticle(particleData, vec.x, vec.y, vec.z, 0, 0, 0);
                    }
                }
            }
        }
    }
}
