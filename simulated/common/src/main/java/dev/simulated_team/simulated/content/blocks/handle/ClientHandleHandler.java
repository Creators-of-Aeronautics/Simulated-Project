package dev.simulated_team.simulated.content.blocks.handle;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.network.packets.UpdatePlayerUsingHandlePacket;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class ClientHandleHandler extends BlockHoldInteraction {

    private float desiredRange = -1;
    public int actuallyUsedBlockCountdown = 0;
    public boolean movingSubLevel = false;

    @Override
    public void startHold(final Level level, final Player player, final BlockPos blockPos) {
        final InteractionHand hand = this.getHandOrNull(player);

        if (hand == null) return;
        if (!(level.getBlockEntity(blockPos) instanceof final HandleBlockEntity handleBE)) return;

        final Vector3d grabCenter = handleBE.getGrabCenter();
        final Vector3d projected = Sable.HELPER.projectOutOfSubLevel(player.level(), grabCenter);

        final Vec3 eyePosition = player.getEyePosition();
        this.desiredRange = (float) Math.min(projected.distance(eyePosition.x, eyePosition.y, eyePosition.z), Math.min(HandleBlockEntity.MAX_HANDLE_RANGE, player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue()));
        this.movingSubLevel = player.isShiftKeyDown();
        player.swing(hand);

        super.startHold(level, player, blockPos);
        this.sendUpdate(false);
    }

    @Override
    public boolean activeTick(final Level level, final LocalPlayer player) {
        final BlockPos interactionPos = this.getInteractionPos();
        final ChunkPos chunk = new ChunkPos(interactionPos);

        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        // stops if targeting plot-yard coordinates but not sublevel
        if (container.inBounds(chunk) && Sable.HELPER.getContaining(level, chunk) == null)
            return true;

        final InteractionHand hand = this.getHandOrNull(player);

        if (hand == null || player.isDeadOrDying() || player.isSpectator())
            return true;

        if (!(level.getBlockEntity(interactionPos) instanceof final HandleBlockEntity handleBE))
            return true;

        final Vector3d globalTarget = Sable.HELPER.projectOutOfSubLevel(level, handleBE.getGrabCenter());

        if (!inInteractionRange(player, globalTarget, 4))
            return true;

        if (!HandleBlock.canInteractWithHandle(player))
            return true;

        final Minecraft minecraft = Minecraft.getInstance();

        // force constant swinging for "animation" in first person
        if (!minecraft.gameRenderer.getMainCamera().isDetached()) {
            player.swingTime = 0;
            player.swinging = true;
            player.swingingArm = InteractionHand.MAIN_HAND;
        }

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        final boolean crouchingOrFlying = this.movingSubLevel || player.getAbilities().flying;
        if (!crouchingOrFlying) {
            if (player.input.up) {
                this.deltaRange(player, -0.5f);
            }
            if (player.input.down) {
                this.deltaRange(player, 0.5f);
            }

            VeilPacketManager.server().sendPacket(new UpdatePlayerUsingHandlePacket(-1, false, interactionPos));

            final Vec3 eyePos = player.getEyePosition();
            final Vec3 goalEyePos = JOMLConversion.toMojang(globalTarget).add(player.getLookAngle().scale(-(this.desiredRange * 0.4f) - Math.max(-player.getLookAngle().y, 0)));

            Vec3 difference = goalEyePos.subtract(eyePos);
            final double differenceLength = difference.length();

            final double maxLength = 2.0;
            if (differenceLength > maxLength) {
                difference = difference.scale(maxLength / differenceLength);
            }

            player.setDeltaMovement(player.getDeltaMovement().scale(0.25).add(difference.scale(0.3)));
            player.resetFallDistance(); // prevents clientside impact sound
        } else {
            this.sendUpdate(false);
        }

        return false;
    }

    @Override
    public void clientTick(final Level level, final LocalPlayer player) {
        if (this.actuallyUsedBlockCountdown > 0) {
            this.actuallyUsedBlockCountdown--;
        }

        if (this.isActive()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();

        if (!minecraft.options.keyUse.isDown() || minecraft.options.keyShift.isDown()) {
            return;
        }
        if (player.isUsingItem()) {
            return;
        }
        if (AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return;
        }
        if (!HandleBlock.canInteractWithHandle(player))
            return;

        if (this.actuallyUsedBlockCountdown > 0) {
            return;
        }

        // raycasts at downwards increments, until it covers a line of the players movement for the next tick
        final double length = player.getDeltaMovement().length();
        final Vec3 moveNorm = player.getDeltaMovement().normalize();

        for (double i = -0.2; i < length; i += 0.2) {
            final Vec3 castOrigin = player.getEyePosition().add(moveNorm.scale(i));
            final Vec3 castDir = player.getLookAngle().scale(BlockHoldInteraction.getInteractionRange(player));
            final BlockHitResult clip = level.clip(
                    new ClipContext(
                            castOrigin,
                            castOrigin.add(castDir),
                            ClipContext.Block.OUTLINE,
                            ClipContext.Fluid.NONE,
                            player));
            final BlockState state = level.getBlockState(clip.getBlockPos());
            if (state.is(SimTags.Blocks.HANDLES)) {
                this.startHold(level, player, clip.getBlockPos());
                return;
            }
        }
    }

    @Override
    public Result onScroll(final double deltaX, final double deltaY) {
        final Player player = SimDistUtil.getClientPlayer();
        if (this.isActive() && player != null) {
            this.deltaRange(player, (float) deltaY);
            return new Result(true);
        }

        return Result.empty();
    }

    public void deltaRange(final Player player, final float delta) {
        this.desiredRange = (float) Math.clamp(this.desiredRange + delta, 1, Math.min(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue(), HandleBlockEntity.MAX_HANDLE_RANGE));
    }

    @Override
    public void stop() {
        this.sendUpdate(true);
        this.desiredRange = -1;
        super.stop();
    }

    @Nullable
    public InteractionHand getHandOrNull(final Player player) {
        final ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        final ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        return this.isEmptyOrExtendoGrip(mainItem) ? InteractionHand.MAIN_HAND : this.isEmptyOrExtendoGrip(offHandItem) ? InteractionHand.OFF_HAND : null;
    }

    public boolean isEmptyOrExtendoGrip(final ItemStack stack) {
        return stack.isEmpty() || AllItems.EXTENDO_GRIP.is(stack.getItem());
    }

    private void sendUpdate(final boolean remove) {
        VeilPacketManager.server().sendPacket(new UpdatePlayerUsingHandlePacket(remove ? -1 : this.desiredRange, remove, this.getInteractionPos()));
    }
}
