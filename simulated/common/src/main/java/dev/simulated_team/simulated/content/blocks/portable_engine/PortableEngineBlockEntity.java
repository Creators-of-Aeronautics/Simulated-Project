package dev.simulated_team.simulated.content.blocks.portable_engine;


import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.SimItemService;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

public class PortableEngineBlockEntity extends GeneratingKineticBlockEntity implements Clearable {

    /**
     * A month of burn timestamp is considered infinite
     */
    public static int INFINITE_THRESHOLD = 20 * 3600 * 24 * 30;

    public PortableEngineInventory inventory;

    /**
     * The burn timestamp remaining of the <b>current</b> fuel in ticks.
     *
     * @see PortableEngineBlockEntity#getCurrentBurnTime()
     * @see PortableEngineBlockEntity#getTotalBurnTime()
     */
    private int burnTime = 0;
    private boolean superHeated = false;

    /**
     * Cached generated speed to prevent oddities from occuring
     */
    protected float generatedSpeed;

    protected ScrollOptionBehaviour<IControlContraption.MovementMode> movementDirection;

    protected float clientAngle;
    public float lastHatchOpenTime = 0;
    public float hatchOpenTime = 0;
    protected boolean eatingCake = false;

    protected LerpedFloat visualSpeed = LerpedFloat.linear();
    protected LerpedFloat visualStrength = LerpedFloat.linear();
    public boolean openHatchOverride;

    public PortableEngineBlockEntity(final BlockEntityType<?> typeIn, final BlockPos pos, final BlockState state) {
        super(typeIn, pos, state);

        this.inventory = new PortableEngineInventory(this);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        this.movementDirection = new ScrollOptionBehaviour(WindmillBearingBlockEntity.RotationDirection.class,
                Component.translatable("create.contraptions.windmill.rotation_direction"), this, new PortableEngineValueBoxTransform());
        this.movementDirection.withCallback(t -> this.onDirectionChanged());

        behaviours.add(this.movementDirection);
        super.addBehaviours(behaviours);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    private static class PortableEngineValueBoxTransform extends ValueBoxTransform {

        @Override
        public Vec3 getLocalOffset(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState) {
            final Direction facing = blockState.getValue(PortableEngineBlock.HORIZONTAL_FACING);
            final float yRot = AngleHelper.horizontalAngle(facing);
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 13.5f, 7.4f), yRot, Direction.Axis.Y);
        }

        @Override
        public void rotate(final LevelAccessor levelAccessor, final BlockPos blockPos, final BlockState blockState, final PoseStack poseStack) {
            final float yRot = AngleHelper.horizontalAngle(blockState.getValue(BlockStateProperties.HORIZONTAL_FACING));
            TransformStack.of(poseStack)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(90)
                    .translate(0, 0.1, 0);
        }

    }

    private void onDirectionChanged() {
        if (!this.level.isClientSide) {
            this.updateGeneratedRotation();
        }
    }

    protected static BlockPos getCameraPos() {
        final Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
        if (renderViewEntity == null) {
            return BlockPos.ZERO;
        }
        final BlockPos playerLocation = renderViewEntity.blockPosition();
        return playerLocation;
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(this.generatedSpeed * (this.movementDirection.getValue() > 0 ? -1 : 1), this.getBlockState().getValue(HORIZONTAL_FACING)) * (this.superHeated ? 2 : 1);
    }

    @Override
    public void tick() {
        super.tick();
        final boolean isLit = this.burnTime > 0;

        //Update visualSpeed and play sounds
        if (this.level.isClientSide) {
            final float targetSpeed = this.isVirtual() ? this.speed : this.getGeneratedSpeed();
            this.visualSpeed.updateChaseTarget(targetSpeed);
            this.visualSpeed.tickChaser();

            final float heatTarget = isLit ? 1.0f : 0.0f;
            float heatSpeed = 0.02f;

            if (this.visualStrength.getValue() > heatTarget) {
                heatSpeed = 0.1f;
            }

            this.visualStrength.chase(heatTarget, heatSpeed, LerpedFloat.Chaser.EXP);
            this.visualStrength.tickChaser();

            final float s = this.visualSpeed.getValue() * 3 / 10f;
            final float soundAngle = Math.abs(this.clientAngle % 90) - 45;

            if (soundAngle > 0 && soundAngle < Math.abs(s)) {
                final double pRand = this.level.getRandom().nextDouble();
                final double distSq = Sable.HELPER.distanceSquaredWithSubLevels(this.level, JOMLConversion.atCenterOf(getCameraPos()), JOMLConversion.atCenterOf(this.worldPosition));
                final double dist = Math.sqrt(distSq);
                final double ratio = 1.0 - dist / 8.0;

                if (ratio > 0) {
                    SimSoundEvents.PORTABLE_ENGINE_PUFF.playAt(this.level, this.worldPosition, (float) ratio, 1.0f, false);
                }
                if (pRand < 0.05D) {
                    SimSoundEvents.PORTABLE_ENGINE_AMBIENT.playAt(this.level, this.worldPosition, 0.8f, 1.0f, false);
                }
            }

            this.clientAngle += s;
            this.clientAngle %= 360;

            if (isLit && !this.isVirtual()) {
                this.spawnParticles();
            }

            this.updateHatchTime();
        }

        if (this.isVirtual()) {
            return;
        }

        if (this.getGeneratedSpeed() != 0 && this.getSpeed() == 0) {
            this.updateGeneratedRotation();
        }

        final ContainerSlot slot = this.inventory.slot;
        final ItemStack stack = slot.getStack();

        final boolean previousSuperHeated = false;

        // Update burn timestamp
        if (this.burnTime > 0 && !this.isCurrentFuelInfinite()) {
            this.burnTime--;
        }
        if (this.burnTime <= 0 && !this.inventory.isEmpty()) {
            this.burnTime = SimItemService.INSTANCE.getBurnTime(stack);
            this.superHeated = this.getNextSuperHeated();
            if (this.burnTime > 0) {
                if (stack.getCount() == 1 && stack.getItem().hasCraftingRemainingItem()) {
                    slot.setStack(slot.getType().getCraftingRemainingItem().getDefaultInstance());
                } else {
                    slot.shrink(1);
                }
            }
        }

        if (this.burnTime <= 0) {
            this.superHeated = false;
        }

        final boolean isLitState = PortableEngineBlock.isLitState(this.getBlockState());

        // TODO: config?
        final int generatedSpeed = 32;

        if (this.generatedSpeed == 0 && isLit && this.getSpeed() != 0.0) {
            final float newSpeed = convertToDirection((this.movementDirection.getValue() > 0 ? -1 : 1), this.getBlockState().getValue(HORIZONTAL_FACING));

            if (Mth.sign(newSpeed) != Mth.sign(this.getSpeed())) {
                // swap direction if the speed would not be going the same way
                this.generatedSpeed = isLit ? generatedSpeed : 0;
                final IControlContraption.MovementMode[] directions = IControlContraption.MovementMode.values();
                final IControlContraption.MovementMode existingValue = directions[this.movementDirection.getValue()];
                this.movementDirection.setValue((existingValue.ordinal() + 1) % directions.length);
                this.updateGeneratedRotation();
            }
        }
        this.generatedSpeed = isLit ? generatedSpeed : 0;

        if (isLitState && !isLit) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(LIT, false), 2);
            this.updateGeneratedRotation();
        }

        if (!isLitState && isLit) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(LIT, true), 2);
            this.level.playSound(null, this.worldPosition, SimSoundEvents.PORTABLE_ENGINE_ROARS.event(), SoundSource.BLOCKS,
                    .125f + this.level.random.nextFloat() * .125f, .75f - this.level.random.nextFloat() * .25f);

            Vec3 pos = VecHelper.getCenterOf(this.worldPosition);

            final Direction direction = this.getBlockState()
                    .getValue(BlockStateProperties.HORIZONTAL_FACING);
            final Vec3i N = direction.getNormal();
            final Vec3 N2 = new Vec3(N.getX(), N.getY(), N.getZ());
            pos = pos.add(-N.getX() * 0.53, -0.1, -N.getZ() * 0.53);
            final Vec3 speed = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.01f).add(N2.scale(-0.03));
            for (int i = 0; i < 2; i++) {
                Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.1f);
                random = random.subtract(N2.scale(random.dot(N2)));
                pos = pos.add(random);
                this.level.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
            }
            this.updateGeneratedRotation();
        }

        if (previousSuperHeated != this.superHeated && (isLitState && isLit)) {
            //update if super heating changed in the middle of an ongoing burn
            this.updateGeneratedRotation();
        }

        if(!this.level.isClientSide()) {
            if(this.eatingCake) {
                this.eatingCake = false;
                this.sendData();
            }

            final Direction direction = this.getBlockState().getValue(PortableEngineBlock.HORIZONTAL_FACING)
                    .getOpposite();
            final BlockPos front = this.getBlockPos().relative(direction);
            final long time = this.level.getGameTime() % 60;
            if(time == 0 && this.level.getBlockState(front).is(Blocks.CAKE)) {
                this.eatingCake = true;
                this.sendData();

                final BlockState state = this.level.getBlockState(front);
                if(state.getValue(CakeBlock.BITES) < CakeBlock.MAX_BITES) {
                    this.level.setBlock(front, state.cycle(CakeBlock.BITES), 2);
                } else {
                    this.level.removeBlock(front, false);
                    final AABB aabb = new AABB(this.getBlockPos()).inflate(8);
                    final List<Player> nearbyPlayers = this.level.getEntitiesOfClass(Player.class, aabb);
                    for (final Player player : nearbyPlayers) {
                        SimStats.PORTABLE_ENGINES_FED.awardTo(player);
                    }
                }
                this.burnTime += 20 * 5;
                this.level.playSound(null, this.getBlockPos(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    public float getHatchOpenTime(final float partialTicks) {
        return Mth.lerp(partialTicks, this.lastHatchOpenTime, this.hatchOpenTime);
    }

    private void updateHatchTime() {
        boolean openHatch = false;

        final BlockPos pos = this.getBlockPos();
        final Vec3 center = pos.getCenter();
        final List<Player> players = this.level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(7.0));

        for (final Player player : players) {
            if (Sable.HELPER.distanceSquaredWithSubLevels(this.level, player.getEyePosition(), center) < Mth.square(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 0.7)) {
                openHatch = this.canOpenHatch(player);

                if (openHatch) break;
            }
        }

        openHatch |= this.openHatchOverride;

        final float speed = 1.35f;
        int dir = openHatch ? 1 : -1;
        if(this.eatingCake) {
            dir = -dir * 5;
        }
        this.lastHatchOpenTime = this.hatchOpenTime;
        this.hatchOpenTime = Math.clamp(this.hatchOpenTime + dir * speed, 0, 10);
    }

    private boolean canOpenHatch(final Player player) {
        final ItemStack heldItem = player.getMainHandItem();
        return this.inventory.insertGeneral(ItemInfoWrapper.generateFromStack(heldItem), heldItem.getCount(), true) > 0;
    }

    public void spawnParticles() {
        Vec3 hatchPos = VecHelper.getCenterOf(this.worldPosition);

        final Direction direction = this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        final Vec3i facingDirI = direction.getNormal();
        final Vec3 facingDir = new Vec3(facingDirI.getX(), facingDirI.getY(), facingDirI.getZ());
        final Vec3 rightDir = facingDir.yRot((float) (Math.PI / 2.0));

        hatchPos = hatchPos.add(-facingDirI.getX() * 0.53, -0.1, -facingDirI.getZ() * 0.53);


        if (Create.RANDOM.nextFloat() < 0.12) {
            Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.15f);
            random = random.subtract(facingDir.scale(random.dot(facingDir)));
            hatchPos = hatchPos.add(random);

            final ParticleOptions particle;
            if (this.isSuperHeated() && Create.RANDOM.nextFloat() < 0.3) {
                particle = ParticleTypes.FLAME;
            } else {
                particle = ParticleTypes.SMOKE;
            }

            final Vec3 speed = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.01f);
//            this.level.addParticle(particle, hatchPos.x, hatchPos.y, hatchPos.z, speed.x, speed.y, speed.z);
        }

        for (int i = -1; i < 2; i+=2) {
            if (Create.RANDOM.nextFloat() < 0.25) {
                final Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 1.0f / 16.0f);
                final Vec3 pos = Vec3.upFromBottomCenterOf(this.worldPosition, 11.0 / 16.0)
                        .add(facingDir.scale(0.5))
                        .add(rightDir.scale(0.5 * i))
                        .add(random);

                final Vec3 speed = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.01f);
                this.level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
            }
        }



        if (this.hatchOpenTime > 0 && Create.RANDOM.nextFloat() < 0.08) {
            Vec3 random = VecHelper.offsetRandomly(Vec3.ZERO, RandomSource.create(), 0.1f);
            random = random.subtract(facingDir.scale(random.dot(facingDir)));
            hatchPos = hatchPos.add(random);
            this.level.addParticle(this.isSuperHeated() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, hatchPos.x, hatchPos.y, hatchPos.z, 0, 0, 0);
        }
    }

    public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("SuperHeated", this.superHeated);
        compound.putFloat("GeneratedSpeed", this.generatedSpeed);
        compound.putBoolean("EatingCake", this.eatingCake);

        compound.put("Inventory", this.inventory.write(registries));

        compound.putInt("BurnTime", this.burnTime);
    }

    protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.superHeated = compound.getBoolean("SuperHeated");

        this.inventory.read(registries, compound.getCompound("Inventory"));

        this.burnTime = compound.getInt("BurnTime");
        this.generatedSpeed = compound.getFloat("GeneratedSpeed");
        this.eatingCake = compound.getBoolean("EatingCake");

        if (clientPacket || this.isVirtual()) {
            this.visualSpeed.chase(this.getGeneratedSpeed(), 1 / 8f, LerpedFloat.Chaser.EXP);
        }
    }


    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        SimLang.translate("portable_engine.tooltip_name").text(":").forGoggles(tooltip);

        final ItemStack currentStack = this.inventory.slot.getStack();
        final boolean hasByProduct = !currentStack.isEmpty() && SimItemService.INSTANCE.getBurnTime(currentStack) == 0;

        final LangBuilder noFuel = SimLang.translate("portable_engine.none").style(ChatFormatting.RED);
        final LangBuilder stackName = SimLang.builder().add(currentStack.getHoverName())
                .text(" x" + currentStack.getCount())
                .style(ChatFormatting.GREEN);

        if (!this.isCurrentFuelInfinite()) {
            final String langKey = hasByProduct ? "byproduct" : "fuel";
            SimLang.translate("portable_engine." + langKey, currentStack.isEmpty() ? noFuel : stackName)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
        }

        if(this.burnTime > 0) {
           final int seconds = this.getCurrentBurnTime() / 20;
           final int secondsTotal = this.getTotalBurnTime() / 20;

            final LangBuilder infiniteLang = SimLang.translate("portable_engine.infinite")
                    .style(ChatFormatting.LIGHT_PURPLE);
            final LangBuilder timeLang = SimLang.text(this.getTime(secondsTotal))
                    .style(this.isSuperHeated() ? ChatFormatting.GOLD : ChatFormatting.AQUA);

            SimLang.translate("portable_engine.time", this.isTotalFuelInfinite() ? infiniteLang : timeLang)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);

            if (this.superHeated) {
                if (this.isCurrentFuelInfinite()) {
                    SimLang.translate("portable_engine.superheated")
                            .style(ChatFormatting.GOLD)
                            .forGoggles(tooltip);
                } else {
                    SimLang.translate("portable_engine.superheated_time", this.getTime(this.getNextSuperHeated() ? secondsTotal : seconds))
                            .style(ChatFormatting.GOLD)
                            .forGoggles(tooltip);
                }
            }
        }
        return true;
    }

    private String getTime(int sec) {
        String s = "";
        int min = sec / 60;
        final int hour = min / 60;
        sec = Math.floorMod(sec, 60);
        min = Math.floorMod(min, 60);
        if (hour > 0) {
            s += hour + "h ";
        }
        if (min < 10 && hour > 0) {
            s += 0;
        }
        if (min > 0 || hour > 0) {
            s += min + "m ";
        }
        if (sec < 10 && min > 0) {
            s += 0;
        }
        s += sec + "s";
        return s;
    }

    /**
     * @return Whether the currently consumed item has infinite burn timestamp
     * @see PortableEngineBlockEntity#INFINITE_THRESHOLD
     */
    public boolean isCurrentFuelInfinite() {
        return this.burnTime >= INFINITE_THRESHOLD;
    }

    /**
     * @return Whether the current consumed or next item has "infinite" burn timestamp
     * @see PortableEngineBlockEntity#INFINITE_THRESHOLD
     */
    public boolean isTotalFuelInfinite() {
        return this.getNextBurnTime() >= INFINITE_THRESHOLD || this.isCurrentFuelInfinite();
    }

    /**
     * @return The remaining burn timestamp (in ticks) of the current consumed item
     */
    public int getCurrentBurnTime() {
        return this.burnTime;
    }

    /**
     * @param value The burn timestamp (in ticks) before checking to consume another item
     */
    public void setCurrentBurnTime(final int value) {
        this.burnTime = value;
    }

    /**
     * @return The total amount of timestamp (in ticks) that this portable engine will be on
     */
    public int getTotalBurnTime() {
        return this.getCurrentBurnTime() + (this.inventory.slot.getStack().getCount() * this.getNextBurnTime());
    }

    /**
     * @return The burn timestamp (in ticks) for the currently held stack
     */
    private int getNextBurnTime() {
        return SimItemService.INSTANCE.getBurnTime(this.inventory.slot.getStack());
    }

    public boolean isSuperHeated() {
        return this.superHeated;
    }

    public void setSuperHeated(final boolean value) {
        this.superHeated = value;
    }

    private boolean getNextSuperHeated() {
        return SimItemService.INSTANCE.getSuperheatedBurnTime(this.inventory.slot.getStack()) > 0;
    }
}
