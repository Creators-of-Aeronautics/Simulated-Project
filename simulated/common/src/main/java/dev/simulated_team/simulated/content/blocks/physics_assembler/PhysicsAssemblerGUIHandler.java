package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PhysicsAssemblerGUIHandler extends BlockHoldInteraction {
    private static final double PULLED_THRESHOLD = 0.015;
    public static int lastSignal = 0;
    public static float animatedVelocity;
    public static float animatedValue;
    public static float lastAnimatedValue;


    @Override
    public void startHold(final Level level, final Player player, final BlockPos blockPos) {
        super.startHold(level, player, blockPos);

        final BlockEntity be = level.getBlockEntity(blockPos);
        if (!(be instanceof final PhysicsAssemblerBlockEntity assembler)) {
            return;
        }

        animatedValue = 0.0f;
        if (Sable.HELPER.getContaining(assembler) != null) animatedValue = 1.0f;
        animatedVelocity = 0.0f;
    }

    @Override
    public void release() {
        if (SableDistUtil.getClientLevel().getBlockEntity(this.getInteractionPos()) instanceof final PhysicsAssemblerBlockEntity be) {
            if (be.holdingLever) return;

            boolean inPlot = this.getSubLevelHolding() != null;

            if ((inPlot && animatedValue < PULLED_THRESHOLD && lastAnimatedValue < PULLED_THRESHOLD) ||
                    (!inPlot && lastAnimatedValue > 1.0 - PULLED_THRESHOLD && animatedValue > 1.0 - PULLED_THRESHOLD)) {
                VeilPacketManager.server().sendPacket(new AssemblePacket(this.getInteractionPos()));
                inPlot = !inPlot;
                be.setClientHoldLeverInPlace(true);
            }

            be.visualAngle.setValue(animatedValue * 45.0);
            be.clientFlickLeverTo(inPlot);
            be.stopControllingPlayer();
        }
    }

    @Override
    public boolean activeTick(final Level level, final LocalPlayer player) {
        if (level == null) {
            return true;
        }

        if (level.getBlockEntity(this.getInteractionPos()) instanceof final PhysicsAssemblerBlockEntity be) {
            if (BlockHoldInteraction.inInteractionRange(player, this.getInteractionPos().getCenter(), 2)) {
                lastAnimatedValue = animatedValue;
                animatedValue += animatedVelocity;
                animatedVelocity *= 0.8f;

                be.updateControlledByPlayer(animatedValue * 45.0f);
                return false;
            }

            final boolean inPlot = this.getSubLevelHolding() != null;
            be.visualAngle.setValue(animatedValue * 45.0);
            be.clientFlickLeverTo(inPlot);
            be.stopControllingPlayer();
            return true;
        }



        return true;
    }

    @Override
    public boolean activeOnMouseMove(final double yaw, final double pitch) {
        final double scalar = 0.5 - Math.abs(0.5 - animatedValue) + 0.05;

        if (!(SableDistUtil.getClientLevel().getBlockEntity(this.getInteractionPos()) instanceof PhysicsAssemblerBlockEntity)) {
            return false;
        }

        animatedValue -= (float) ((pitch / 80.0) * scalar);

        if (animatedValue > 1.0) {
            animatedValue = 1.0f;
        } else if (animatedValue < 0.0) {
            animatedValue = 0.0f;
            animatedVelocity = 0.0f;
        }

        final int signal = Math.round(animatedValue * 4.0f);

        if (signal != lastSignal) {
            lastSignal = signal;
            if (signal == 0.0f || signal == 4.0f)
                SimSoundEvents.ASSEMBLER_SHIFT.playAt(Minecraft.getInstance().level, this.getInteractionPos(), 0.5f, 0.8f + animatedValue * 0.3f, false);
            else
                SimSoundEvents.ASSEMBLER_TICK.playAt(Minecraft.getInstance().level, this.getInteractionPos(), 0.5f, 0.8f + animatedValue * 0.3f, false);
        }

        return true;
    }

    @Override
    public void renderOverlay(final GuiGraphics graphics, final int width1, final int height1, final boolean hideGui) {
        if (hideGui)
            return;
        final PoseStack ps = graphics.pose();

        ps.pushPose();

        ps.translate(graphics.guiWidth() / 2, graphics.guiHeight() / 2, 0);

        final int height = 6 + 10 * 6 + 6;

        ps.translate(10, -height / 2, 0);

        graphics.blit(SimGUITextures.ASSEMBLER_TRACK_START.location, 0, 0, 0, 0, 14, 6, 32, 32);
        ps.translate(0, 6, 0);

        for (int c = 0; c < 6; c++) {
            graphics.blit(SimGUITextures.ASSEMBLER_TRACK_MIDDLE.location, 0, 0, 0, 7, 14, 10, 32, 32);
            ps.translate(0, 10, 0);
        }

        graphics.blit(SimGUITextures.ASSEMBLER_TRACK_END.location, 0, 0, 0, 18, 14, 6, 32, 32);

        final float value = Mth.lerp(AnimationTickHolder.getPartialTicks(), lastAnimatedValue, animatedValue);
        ps.translate(-2, -12 - (51 * value), 0);
        graphics.blit(SimGUITextures.ASSEMBLER_TRACK_MIDDLE.location, 0, 0, 14, 0, 18, 14, 32, 32);

        ps.popPose();
    }

    public ClientSubLevel getSubLevelHolding() {
        return Sable.HELPER.getContainingClient(this.getInteractionPos());
    }

    public double getFraction() {
        return animatedValue;
    }
}
