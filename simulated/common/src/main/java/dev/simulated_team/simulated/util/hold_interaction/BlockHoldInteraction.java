package dev.simulated_team.simulated.util.hold_interaction;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.mixin.hold_interaction.KeyMappingInvoker;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.glfw.GLFW;

/**
 * A client interaction that requires holding right click<br>
 * Controlled using {@link HoldInteractionManager}<br>
 * Only one instance is ever expected to exist, so go wild with static variables
 */
public abstract class BlockHoldInteraction implements InteractCallback {
    /**
     * Called only when the interaction has just been started
     */
    @ApiStatus.OverrideOnly
    public void start() {
        ((KeyMappingInvoker)Minecraft.getInstance().options.keyAttack).invokeRelease();
    }

    /**
     * Called only when the interaction is about to be stopped
     */
    @ApiStatus.OverrideOnly
    public void stop() {
        this.interactionPos = null;
    }

    /**
     * Called only when the interaction is about to be stopped by releasing the use button
     */
    @ApiStatus.OverrideOnly
    public void release() {}

    public boolean isActive() {
        return HoldInteractionManager.isActive(this);
    }

    public boolean isBlockActive(final BlockPos pos) {
        return this.isActive() && pos.equals(this.interactionPos);
    }

    public void renderOverlay(final GuiGraphics graphics, final int width1, final int height1, final boolean hideGui) {}

    public static double getInteractionRange(final Player player) {
        return player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
    }

    public static boolean inInteractionRange(final Player player, final Position target, final double reachBuffer) {
        final double distance = getInteractionRange(player) + reachBuffer;
        final Vec3 eyePosition = player.getEyePosition();
        return Sable.HELPER.projectOutOfSubLevel(player.level(), JOMLConversion.toJOML(target))
                .distanceSquared(eyePosition.x, eyePosition.y, eyePosition.z) < distance * distance;
    }

    public static boolean inInteractionRange(final Player player, final Vector3dc target, final double reachBuffer) {
        final double distance = getInteractionRange(player) + reachBuffer;
        final Vec3 eyePosition = player.getEyePosition();
        return Sable.HELPER.projectOutOfSubLevel(player.level(), target, new Vector3d())
                .distanceSquared(eyePosition.x, eyePosition.y, eyePosition.z) < distance * distance;
    }

    public static boolean inInteractionRange(final Player player, final Position target) {
        return inInteractionRange(player, target, 0);
    }

    public int getCrouchBlockingTicks() {
        return 0;
    }

    private BlockPos interactionPos = null;
    public BlockPos getInteractionPos() {
        return this.interactionPos;
    }

    public void startHold(final Level level, final Player player, final BlockPos blockPos) {
        HoldInteractionManager.start(this);
        this.interactionPos = blockPos;
    }

    @Override
    public Result onAttack(final int modifiers, final int action, final KeyMapping leftKey) {
        if (this.isActive() && action != GLFW.GLFW_RELEASE) {
            return new Result(true);
        }

        return InteractCallback.super.onAttack(modifiers, action, leftKey);
    }

    @Override
    public Result onUse(final int modifiers, final int action, final KeyMapping rightKey) {
        if (action == GLFW.GLFW_RELEASE && this.isActive()) {
            this.release();
            HoldInteractionManager.stop();
            // sometimes minecraft can view keybinds as active even after a release event
            ((KeyMappingInvoker)Minecraft.getInstance().options.keyUse).invokeRelease();
        }

        return InteractCallback.super.onUse(modifiers, action, rightKey);
    }

    /**
     * @param level  the client level
     * @param player the client player
     * @return true to stop the interaction
     */
    public boolean activeTick(final Level level, final LocalPlayer player) {
        return false;
    }

    @Override
    public Result onMouseMove(final double yaw, final double pitch) {
        if (this.isActive()) {
            if (this.activeOnMouseMove(yaw, pitch)) {
                return new Result(true);
            }
        }
        return InteractCallback.super.onMouseMove(yaw, pitch);
    }

    public boolean activeOnMouseMove(final double yaw, final double pitch) {
        return false;
    }
}
