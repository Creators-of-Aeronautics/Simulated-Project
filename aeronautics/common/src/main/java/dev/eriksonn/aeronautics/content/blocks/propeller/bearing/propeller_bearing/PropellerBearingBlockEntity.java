package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound.PropellerBearingSoundHolder;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.util.AeroSoundDistUtil;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.simulated_team.simulated.api.BearingSlowdownController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropellerBearingBlockEntity extends MechanicalBearingBlockEntity implements MechanicalBearingTileEntityExtension, BlockEntitySubLevelPropellerActor, IHaveGoggleInformation, BlockEntityPropeller {
    private static final MutableComponent SCROLL_OPTION_TITLE = AeroLang.translate("scroll_option.thrust_direction").component();
    public final Vector3d thrustDirection;
    public final Vector3d facingDirection = new Vector3d();
    public float totalSailPower;
    public boolean disassemblySlowdown = false;
    /**
     * client previous angle
     */
    public float prevAngle;
    public BearingSlowdownController slowdownController = new BearingSlowdownController();
    protected PropellerActorBehaviour behavior;

    /**
     * amount of sails on this propeller
     */
    protected List<BlockPos> sailPositions;
    /**
     * last generated speed for server client syncing
     */
    protected float lastGeneratedSpeed;
    private ScrollOptionBehaviour<ThrustDirection> thrustDirectionOption;
    /**
     * client smoothed rotation speed
     */
    private float rotationSpeed = 0;
    private boolean insideMainTick = false;

    /**
     * The current {@link PropellerBearingSoundHolder}, if any.
     */
    @Nullable
    private Object currentSoundInstance;

    public PropellerBearingBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);

        this.sailPositions = new ArrayList<>();
        this.thrustDirection = new Vector3d();

        this.behavior.setThrustDirection(this.thrustDirection);
    }

    private static double getConfigAirflowMult() {
        return AeroConfig.server().physics.propellerBearingAirflowMult.get();
    }

    private static double getConfigThrust() {
        return AeroConfig.server().physics.propellerBearingThrust.get();
    }

    @Override
    public float calculateStressApplied() {

        if (!this.running || this.disassemblySlowdown) {
            this.lastStressApplied = 0;
            return 0;
        }
        int sails = 0;
        if (this.movedContraption != null) {
            sails = ((BearingContraption) this.movedContraption.getContraption()).getSailBlocks();
        }
        sails = Math.max(sails, 2);
        final float stress = sails * (float) BlockStressValues.getImpact(this.getStressConfigKey());
        this.lastStressApplied = stress;
        return stress;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        //remove normal mech bearing value box
        this.movementMode.setValue(2);
        behaviours.remove(this.movementMode);

        this.thrustDirectionOption = new ScrollOptionBehaviour<>(ThrustDirection.class, SCROLL_OPTION_TITLE, this, this.getMovementModeSlot());

        this.getThrustDirectionOption().withCallback($ -> this.onDirectionChanged());
        behaviours.add(this.getThrustDirectionOption());
        behaviours.add(this.behavior = this.getAndPreparePropBehaviour());
    }

    public PropellerActorBehaviour createProp() {
        return new PropellerActorBehaviour(this, this);
    }

    public PropellerActorBehaviour getAndPreparePropBehaviour() {
        final PropellerActorBehaviour prop = this.createProp();

        prop.setParticleAmountUpdater(() -> 0.02 * Math.abs(this.getClampedRotationRate()) * this.totalSailPower);
        prop.setParticleCountProperties(50, 10);
        prop.setParticlePositionUpdater((v, random) -> this.getRandomSailPosition(random, v).add(this.facingDirection));
        return prop;
    }

    @Override
    public Direction getBlockDirection() {
        return this.getBlockState().getValue(PropellerBearingBlock.FACING);
    }

    @Override
    public double getThrust() {
        return Math.pow(this.totalSailPower, 1.5f) * this.getDirectionIndependentSpeed() * getConfigThrust();
    }

    @Override
    public boolean isActive() {
        return Math.abs(this.rotationSpeed) > 0.01f && this.movedContraption != null;
    }

    @Override
    public double getAirflow() {
        return Math.sqrt(this.totalSailPower) * this.getDirectionIndependentSpeed() * getConfigAirflowMult();
    }

    public float getDirectionIndependentSpeed() {
        /*rotation speed is multiplied by direction, so we need to multiply by the direction again*/
        return this.getBlockState().getValue(BlockStateProperties.FACING).getAxisDirection().getStep() * this.getClampedRotationRate() * (10f / 3) * (this.getThrustDirectionOption().value == 1 ? -1 : 1);
    }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public void tick() {
        this.prevAngle = this.angle;
        final Vec3i normal = this.getBlockState().getValue(BlockStateProperties.FACING).getNormal();
        this.facingDirection.set(normal.getX(), normal.getY(), normal.getZ());

//        SubLevelHelper.getContaining(this.level, this.getBlockPos());
        //currentContext = .getMovementContext(level, getBlockPos());

        if (this.disassemblySlowdown) {
            this.updateSlowdownSpeed();
        } else {
            this.updateRotationSpeed();
        }

        this.insideMainTick = true;
        super.tick();
        this.insideMainTick = false;

        if (this.movedContraption != null && !this.movedContraption.isAlive()) {
            this.movedContraption = null;
        }

        if (this.movedContraption == null && !this.isVirtual()) {
            this.angle = 0;
            this.setRotationSpeed(0);
            this.disassemblySlowdown = false;
        }

        if (this.speed != 0) {
            this.lastGeneratedSpeed = this.speed;
        }

        if (this.isActive()) {
            this.activeTick();
        }
    }

    public void activeTick() {
        this.behavior.pushEntities();

        if (this.level.isClientSide) {
            this.behavior.spawnParticles();
        }
    }

    public void onDirectionChanged() {
        if (!this.level.isClientSide && this.running) {
            this.updateGeneratedRotation();
        }
    }

    @Override
    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        compound.putFloat("LastGenerated", this.lastGeneratedSpeed);
        compound.putFloat("RotationSpeed", this.getRotationSpeed());

        compound.putBoolean("DisassemblySlowdown", this.disassemblySlowdown);
        if (this.disassemblySlowdown) {
            this.slowdownController.serializeIntoNBT(compound);
        }

        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        if (!this.wasMoved) {
            this.lastGeneratedSpeed = compound.getFloat("LastGenerated");
        }

        this.setRotationSpeed(compound.getFloat("RotationSpeed"));
        this.disassemblySlowdown = compound.getBoolean("DisassemblySlowdown");
        if (this.disassemblySlowdown) {
            this.slowdownController.deserializeFromNBT(compound);
        }

        super.read(compound, registries, clientPacket);
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (this.isVirtual()) {
            return Mth.lerp(partialTicks + .5f, this.prevAngle, this.angle);
        }

        if (this.movedContraption == null || this.movedContraption.isStalled() || !this.running) {
            partialTicks = 0;
        }

        if (this.disassemblySlowdown) {
            return this.slowdownController.getAngle(partialTicks);
        }

        return Mth.lerp(partialTicks, this.angle, this.angle + this.getAngularSpeed());
    }

    public float getAngularSpeed() {
        float speed = this.getRotationSpeed();

        if (this.insideMainTick && this.disassemblySlowdown) {
            speed = this.slowdownController.getSpeed(1);
        }

        if (this.level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += this.clientAngleDiff / 3f;
        }

        return speed;
    }

    private void updateRotationSpeed() {
        float nextSpeed = convertToAngular(this.getSpeed());
        if (this.isVirtual()) {
            this.setRotationSpeed(nextSpeed);
        }
        if (this.getSpeed() == 0) {
            nextSpeed = 0;
        }

        if (this.totalSailPower > 0) {
            //Larger propellers accelerate slower
            this.setRotationSpeed(Mth.lerp(0.4f / (float) Math.sqrt(this.totalSailPower), this.getRotationSpeed(), nextSpeed));
        } else {
            this.setRotationSpeed(nextSpeed);
        }
    }

    private void updateSlowdownSpeed() {
        if (this.slowdownController.stepGoal() && !this.level.isClientSide) {
            this.disassemble();
            return;
        }

        this.setRotationSpeed(this.slowdownController.getSpeed(0));
        this.angle = this.slowdownController.getAngle(0);
    }

    @Override
    public void attach(final ControlledContraptionEntity contraption) {
        super.attach(contraption);
        this.contraptionInitialize();

        if (this.level.isClientSide) {
            this.currentSoundInstance = AeroSoundDistUtil.tickPropellerSounds(this, this.currentSoundInstance);
        }
    }

    @Override
    public void assemble() {
        if (!(this.level.getBlockState(this.worldPosition).getBlock() instanceof BearingBlock)) {
            return;
        }

        final Direction direction = this.getBlockState().getValue(PropellerBearingBlock.FACING);
        final BearingContraption contraption = new BearingContraption(this.isWindmill(), direction);
        try {
            if (this.isPropeller()) {
                ((BearingContraptionExtension) contraption).aeronautics$setPropeller();
            }
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
        this.movedContraption = PropellerBearingContraptionEntity.create(this.level, this, contraption);
        final BlockPos anchor = this.worldPosition.relative(direction);
        this.movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        this.movedContraption.setRotationAxis(direction.getAxis());
        this.level.addFreshEntity(this.movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(this.level, this.worldPosition);
        AeroAdvancements.IN_THRUST_WE_TRUST.awardToNearby(this.getBlockPos(), this.getLevel());

        this.running = true;
        this.angle = 0;

        this.updateGeneratedRotation();
        this.setRotationSpeed(0);
        this.contraptionInitialize();

        this.sendData();
    }

    @Override
    public void disassemble() {
        if (this.running && this.movedContraption != null) {
            this.angle = 0;
            this.behavior.getLayers().clear();
            this.applyRotation();
            super.disassemble();
        }
    }

    public void setAssembleNextTick(final boolean value) {
        this.assembleNextTick = value;
    }

    public void startDisassemblySlowdown() {
        if (!this.disassemblySlowdown && this.movedContraption != null) {
            this.slowdownController.generate(1 + BearingSlowdownController.TIMER_SCALE * (float) Math.sqrt(this.totalSailPower),
                    this.getInterpolatedAngle(0),
                    this.getRotationSpeed(),
                    this.getBlockState().getValue(PropellerBearingBlock.FACING),
                    this.getMovedContraption().getContraption());

            this.disassemblySlowdown = true;
            this.updateGeneratedRotation();
            this.sendData();
        }
    }

    public void contraptionInitialize() {
        final Direction direction = this.getBlockState().getValue(PropellerBearingBlock.FACING);
        this.thrustDirection.set(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        this.findSails();
    }

    public float getSailPower(final StructureTemplate.StructureBlockInfo info) {
        BlockState state = info.state();
        if (AllBlocks.COPYCAT_PANEL.has(state)) {
            final BlockState newState = NbtUtils.readBlockState(this.blockHolderGetter(), info.nbt().getCompound("Material"));
            if (!newState.isAir()) {
                state = newState;
            }
        }

        float power = 0;

        if (state.is(AllTags.AllBlockTags.WINDMILL_SAILS.tag)) {
            power += 1;
        }

        return power;
    }

    public void findSails() {
        this.sailPositions = new ArrayList<>();
        this.totalSailPower = 0;
        this.behavior.getLayers().clear();

        if (this.movedContraption != null) {
            final Map<BlockPos, StructureTemplate.StructureBlockInfo> Blocks = this.movedContraption.getContraption().getBlocks();
            final Vec3i direction = this.getBlockState().getValue(PropellerBearingBlock.FACING).getNormal();
            final HashMap<Integer, Tuple<Integer, Integer>> layerHashMap = new HashMap<>();

            for (final Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : Blocks.entrySet()) {
                final float sailPower = this.getSailPower(entry.getValue());

                if (sailPower > 0) {
                    BlockPos currentPos = entry.getKey();
                    this.sailPositions.add(currentPos);
                    final int offset = direction.getX() * currentPos.getX() + direction.getY() * currentPos.getY() + direction.getZ() * currentPos.getZ();
                    this.totalSailPower += sailPower;
                    currentPos = currentPos.offset(direction.multiply(-offset));
                    final int radius = currentPos.getX() * currentPos.getX() + currentPos.getY() * currentPos.getY() + currentPos.getZ() * currentPos.getZ();
                    if (layerHashMap.containsKey(offset)) {
                        final Tuple<Integer, Integer> tuple = layerHashMap.get(offset);
                        if (radius < tuple.getA()) {
                            tuple.setA(radius);
                        }
                        if (radius > tuple.getB()) {
                            tuple.setB(radius);
                        }
                    } else {
                        layerHashMap.put(offset, new Tuple<>(radius, radius));
                    }
                }
            }

            for (final Map.Entry<Integer, Tuple<Integer, Integer>> entry : layerHashMap.entrySet()) {
                final Tuple<Integer, Integer> tuple = entry.getValue();
                final double inner = Math.max(Math.sqrt(tuple.getA()) - 0.5, 0);
                final double outer = Math.sqrt(tuple.getB()) + 0.5;
                this.behavior.addPropellerLayer(new PropellerActorBehaviour.PropellerLayer(entry.getKey() + 1, inner, outer));
            }
        }
    }

    private Vector3d getRandomSailPosition(final RandomSource random, final Vector3d pos) {
        //random sail block with contraption orientation
        final BlockPos sailPos = this.sailPositions.get(random.nextInt(this.sailPositions.size()));
        Vec3 floatPos = new Vec3(sailPos.getX(), sailPos.getY(), sailPos.getZ());
        floatPos = this.movedContraption.applyRotation(floatPos, 0);

        //random offset projected onto sail plane
        pos.set(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1).mul(0.5);
        pos.fma(-this.thrustDirection.dot(pos), this.thrustDirection);
        pos.add(floatPos.x, floatPos.y, floatPos.z);

        return pos;
    }

    /**
     * clamps the rotation while the disassembly slowdown is ongoing, to prevent the propeller from giving pulses of force that are too strong or in the wrong direction,compared to what it did just before the disassembly started;
     */
    public float getClampedRotationRate() {
        if (this.disassemblySlowdown) {
            final float max = Math.max(this.slowdownController.getInitialVelocity(), 0);
            final float min = Math.min(this.slowdownController.getInitialVelocity(), 0);
            return Math.min(Math.max(this.getRotationSpeed(), min), max);
        }

        return this.getRotationSpeed();
    }


    @Override
    public boolean isWoodenTop() {
        return false;
    }

    @Override
    public boolean isPropeller() {
        return true;
    }

    public float getRotationSpeed() {
        return this.rotationSpeed;
    }

    public void setRotationSpeed(final float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public ScrollOptionBehaviour<ThrustDirection> getThrustDirectionOption() {
        return this.thrustDirectionOption;
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        if (!super.addToGoggleTooltip(tooltip, isPlayerSneaking)) {
            return false;
        }

        return this.behavior.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    public PropellerBearingContraptionEntity getMovedContraption() {
        if(this.movedContraption instanceof PropellerBearingContraptionEntity propellerBearingContraptionEntity)
            return propellerBearingContraptionEntity;
        return null;
    }

    public enum ThrustDirection implements INamedIconOptions {
        RIGHT_HANDED(AllIcons.I_REFRESH, "pull_when_clockwise"), LEFT_HANDED(AllIcons.I_ROTATE_CCW, "push_when_clockwise");

        private final String translationKey;
        private final AllIcons icon;

        ThrustDirection(final AllIcons icon, final String name) {
            this.icon = icon;
            this.translationKey = Aeronautics.MOD_ID + ".generic." + name;
        }

        @Override
        public AllIcons getIcon() {
            return this.icon;
        }

        @Override
        public String getTranslationKey() {
            return this.translationKey;
        }
    }
}
