package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.config.server.AeroBlockConfigs;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.GasEmitterRenderHandler;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerValueBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroLiftingGasTypes;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.eriksonn.aeronautics.util.AeroSoundDistUtil;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SteamVentBlockEntity extends SmartBlockEntity implements BlockEntityLiftingGasProvider, IHaveGoggleInformation {
    // Steam vents can only be placed on top of fluid tanks, so this is valid
    public static final Direction CHECKING_DIR = Direction.DOWN;
    private static final MutableComponent SCROLL_OPTION_TITLE = AeroLang.translate("scroll_option.hot_air_amount").component();
    private static final String VALUE_FORMAT = "%s m³";
    public int signalStrength = 0;
    public int rawSignalStrength = 0;
    protected ScrollValueBehaviour steamAmountBehaviour;
    private GasEmitterRenderHandler renderHandler;
    private Balloon currentBalloon;
    private ClientBalloonInfo clientBalloonInfo;
    private WeakReference<FluidTankBlockEntity> source;
    private double efficiency = 0;
    private int ticksSinceSync;
    private int maxCapacity;
    protected LerpedFloat intensity = LerpedFloat.linear();

    private @Nullable BlockPos castPosition;

    public SteamVentBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);

        this.source = new WeakReference<>(null);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        final AeroBlockConfigs config = AeroConfig.server().blocks;
        this.setMaxCapacity(config.steamVentMaxHotAir.get());
        this.steamAmountBehaviour = new SteamVentValueBehaviour(SCROLL_OPTION_TITLE, this,
                new SteamVentValueBoxTransform())
                .between(() -> 50, config.steamVentMaxHotAir::get)
                .withFormatter(VALUE_FORMAT::formatted);
        this.steamAmountBehaviour.value = this.maxCapacity;

        behaviours.add(this.steamAmountBehaviour);
    }

    public void setMaxCapacity(final int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        this.getAndCacheTank();
        if (!this.isVirtual() && this.canOutputGas()) {
            this.tickBalloonLogic();
            this.notifyUpdate();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksSinceSync++;

        final FluidTankBlockEntity fluidTank = this.source.get();

        if (fluidTank != null) {
            final FluidTankBlockEntity controller = fluidTank.getControllerBE();
            if (controller != null) {
                this.efficiency = Mth.clamp(controller.boiler.getEngineEfficiency(controller.getTotalTankSize()), 0, 1);
            }
        } else {
            this.efficiency = 0;
        }

        final double intensityGoal = Math.max(0, this.signalStrength / 15.0);
        this.intensity.chase(intensityGoal, 0.1, LerpedFloat.Chaser.EXP);
        this.intensity.tickChaser();

        if (this.level.isClientSide) {
            final GasEmitterRenderHandler renderHandler = this.getRenderHandler();

            if (this.isVirtual()) {
                renderHandler.targetFromRedstoneSignal(this.signalStrength);
            } else {
                renderHandler.targetFromRedstoneSignal(this.getGasOutput() > 0 ? this.signalStrength : 0);
            }

            renderHandler.tick();

            if (this.canOutputGas()) {
                AeroSoundDistUtil.addPosSteamVentSound(this.getBlockPos());
            } else {
                AeroSoundDistUtil.removePosSteamVentSound(this.getBlockPos());
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!this.isVirtual() && this.canOutputGas()) {
            this.tickBalloonLogic();
            this.notifyUpdate();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (this.level.isClientSide) {
            AeroSoundDistUtil.removePosSteamVentSound(this.getBlockPos());
        } else {
            this.removeFromBalloon();
        }
    }

    @Override
    public @Nullable BlockPos getCastPosition() {
        return this.castPosition;
    }

    @Override
    public void doRaycast() {
        final BlockPos pos = this.getBlockPos();

        final AeroBlockConfigs blocks = AeroConfig.server().blocks;
        final int range = blocks.steamVentMaxRange.get();

        this.castPosition = this.getRaycastedPosition(this.level,
                Vec3.upFromBottomCenterOf(pos, 1.0),
                Vec3.upFromBottomCenterOf(pos, 1.0 + range));
    }

    public boolean updateRawSignal() {
        final int newStrength = this.level.getBestNeighborSignal(this.getBlockPos());

        if (newStrength != this.rawSignalStrength) {
            if (!this.level.isClientSide) {
                final BlockState existentState = this.level.getBlockState(this.getBlockPos()); // this.getBlockState() might not be up to date with the variant yet
                if (newStrength > 0 && this.rawSignalStrength == 0) { //if new signal is not 0, and current signal is 0 power
                    this.level.setBlockAndUpdate(this.worldPosition, existentState.setValue(SteamVentBlock.POWERED, true));
                } else if (newStrength == 0 && this.rawSignalStrength > 0) {
                    this.level.setBlockAndUpdate(this.worldPosition, existentState.setValue(SteamVentBlock.POWERED, false));
                }
            }

            this.rawSignalStrength = newStrength;
            this.signalSync();
            return true;
        }
        return false;
    }

    public void updateSignal(final int signal) {
        if (signal != this.signalStrength) {

            if (this.signalStrength == 0 && signal != 0) {
                this.level.playSound(null, this.worldPosition, AeroSoundEvents.STEAM_VENT_OPEN.event(),  SoundSource.BLOCKS,
                        .25f, 1.1f - this.level.random.nextFloat() * .2f);
            } else if (signal == 0) {
                this.level.playSound(null, this.worldPosition, AeroSoundEvents.STEAM_VENT_CLOSE.event(), SoundSource.BLOCKS,
                        .5f, 0.7f - this.level.random.nextFloat() * .2f);
            }
            this.signalStrength = signal;
            this.sendData();
        }
    }

    public static boolean inTankBounds(final BlockPos pos, final FluidTankBlockEntity controller) {
        final int minX = controller.getBlockPos().getX();
        final int minZ = controller.getBlockPos().getZ();
        final int maxX = minX + controller.getWidth();
        final int maxZ = minZ + controller.getWidth();
        return pos.getX() >= minX && pos.getX() < maxX &&
                pos.getZ() >= minZ && pos.getZ() < maxZ;
    }

    public void signalSync() {
        final FluidTankBlockEntity fluidTank = this.source.get();
        if (fluidTank != null) {
            final FluidTankBlockEntity controller = fluidTank.getControllerBE();
            if (controller != null) {
                final List<SteamVentBlockEntity> adjacent = new ArrayList<>();
                adjacent.add(this);
                final int maxRaw = this.searchSignalSync(controller, new HashSet<>(), adjacent);

                for (final SteamVentBlockEntity steamVentBlockEntity : adjacent) {
                    steamVentBlockEntity.updateSignal(maxRaw);
                }
            }
        }
    }

    /**
     * @param visited set of blocks to not recursively go over ad infinitum
     * @param vents list to mutate of all vents connect adjacently
     * @return maximum raw power from all future visited vents
     */
    protected int searchSignalSync(final FluidTankBlockEntity controller, final Set<BlockPos> visited, final List<SteamVentBlockEntity> vents) {
        int maxRaw = this.rawSignalStrength;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (final Direction dir : Iterate.horizontalDirections) {
            mutablePos.setWithOffset(this.getBlockPos(), dir);
            if (inTankBounds(mutablePos, controller)) {
                if (!visited.contains(mutablePos)) {
                    visited.add(mutablePos.immutable());
                    if (this.level.getBlockEntity(mutablePos) instanceof final SteamVentBlockEntity vent) {
                        vents.add(vent);
                        maxRaw = Math.max(maxRaw, vent.searchSignalSync(controller, visited, vents));
                    }
                }
            }
        }
        return maxRaw;
    }

    // Make sure we are getting the fluid tank from the middle steam vent be position
    public void getAndCacheTank() {
        final FluidTankBlockEntity ftbe = this.source.get();
        if (ftbe == null || ftbe.isRemoved()) {
            //get new fluid tank block entity
            final BlockPos check = this.getBlockPos().relative(CHECKING_DIR);

            final BlockEntity be = this.level.getBlockEntity(check);
            if (be instanceof final FluidTankBlockEntity fluidTank) {
                this.source = new WeakReference<>(fluidTank);
            }
        }
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putInt("SignalStrength", this.signalStrength);
        tag.putInt("RawSignalStrength", this.rawSignalStrength);
        if (clientPacket) {
            ClientBalloonInfo.writeToNBT(tag, (ServerBalloon) this.currentBalloon);
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.signalStrength = tag.getInt("SignalStrength");
        this.rawSignalStrength = tag.getInt("RawSignalStrength");
        if (clientPacket) {
            this.ticksSinceSync = 0;
            this.clientBalloonInfo = ClientBalloonInfo.readFromNBT(tag);
        }
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (!this.canOutputGas()) return false;

        AeroLang.blockName(this.getBlockState()).text(":").forGoggles(tooltip, 1);
        if (this.clientBalloonInfo != null) {
            this.addBalloonGoggleInformation(tooltip, this.clientBalloonInfo, this.ticksSinceSync, this.getAirPressure(this.clientBalloonInfo, this.level));
        }
        return true;
    }

    @Override
    public @Nullable Balloon getBalloon() {
        return this.currentBalloon;
    }

    @Override
    public void setBalloon(final Balloon balloon) {
        this.currentBalloon = balloon;
    }

    @Override
    public double getGasOutput() {
        return this.steamAmountBehaviour.getValue() * this.efficiency /* boiler efficiency */ * (this.signalStrength / 15f);
    }

    @Override
    public LiftingGasType getLiftingGasType() {
        return AeroLiftingGasTypes.STEAM.get();
    }

    @Override
    public boolean canOutputGas() {
        return this.efficiency > 0 && this.signalStrength > 0 && !this.isRemoved();
    }

    @Override
    public double getClientPredictedVolume() {
        if (this.clientBalloonInfo == null)
            return 0.0;

        return BlockEntityLiftingGasProvider.getPredictedVolume(this.clientBalloonInfo, this.ticksSinceSync);
    }

    public LerpedFloat getClientIntensity() {
        return this.intensity;
    }

    public GasEmitterRenderHandler getRenderHandler() {
        if (this.renderHandler == null) {
            return this.renderHandler = new GasEmitterRenderHandler();
        }
        return this.renderHandler;
    }

    public static class SteamVentValueBoxTransform extends ValueBoxTransform.Sided {
        BlockEntity be;

        @Override
        public Sided fromSide(final Direction direction) {
            this.direction = direction;

            Level level = this.be.getLevel();
            if (level != null && level.isClientSide && direction == Direction.UP) {
                final Minecraft mc = Minecraft.getInstance();
                final HitResult target = mc.hitResult;
                if (target instanceof BlockHitResult) {
                    final Vec3 hit = target.getLocation();
                    final Vec3 localHit = hit.subtract(Vec3.atCenterOf(this.be.getBlockPos()));
                    if (localHit.y < 0.4) {
                        this.direction = Direction.getNearest(localHit.x, 0, localHit.z);
                    }
                }
            }
            return this;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8f, 12);
        }

        @Override
        public float getScale() {
            return 0.45f;
        }

        protected ValueBoxTransform getMovementModeSlot() {
            return new DirectionalExtenderScrollOptionSlot((state, d) -> {
                final Direction.Axis axis = d.getAxis();

                final Direction.Axis shaftAxis = ((IRotate) state.getBlock()).getRotationAxis(state);

                return shaftAxis != axis;
            });
        }

        @Override
        public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
            final float yRot = AngleHelper.horizontalAngle(this.getSide()) + 180;
            float xRot = this.getSide() == Direction.UP ? 90 : this.getSide() == Direction.DOWN ? 270 : 0;
            xRot += 22.5f;
            TransformStack.of(ms)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(xRot);
        }

        @Override
        protected boolean isSideActive(final BlockState state, final Direction direction) {
            Level level = this.be.getLevel();
            if (level != null && level.isClientSide && direction == Direction.UP) {
                final Minecraft mc = Minecraft.getInstance();
                final HitResult target = mc.hitResult;
                if (target instanceof BlockHitResult) {
                    final Vec3 hit = target.getLocation();
                    final Vec3 localHit = hit.subtract(Vec3.atCenterOf(this.be.getBlockPos()));
                    return localHit.y < 0.4;
                }
            }
            return true;
        }

        @Override
        public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
            if (this.getSide() == Direction.DOWN) {
                return VecHelper.voxelSpace(8, 0, 8);
            }

            Vec3 location = this.getSouthLocation();

            location = location.add(VecHelper.voxelSpace(0, -3, 1.75));
            location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(this.getSide()), Direction.Axis.Y);

            return location;
        }
    }

    public static class SteamVentValueBehaviour extends HotAirBurnerValueBehaviour {

        public SteamVentValueBehaviour(final Component label, final SmartBlockEntity be, final SteamVentValueBoxTransform slot) {
            super(label, be, slot);
            slot.be = be;
        }
    }
}
