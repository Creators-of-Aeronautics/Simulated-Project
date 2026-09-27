package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import dev.simulated_team.simulated.network.packets.ThrottleLeverSignalPacket;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ThrottleLeverHandler extends BlockHoldInteraction {
    protected boolean inverted = false;
    protected int lastSignal = 0;
    protected int signal = 0;
    protected float value = 0;
    protected float animatedValue;
    protected float lastAnimatedValue;

    @Override
    public void startHold(final Level level, final Player player, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof final ThrottleLeverBlockEntity be) {
            this.lastSignal = be.state;
            this.inverted = be.getBlockState().getValue(ThrottleLeverBlock.INVERTED);
            this.signal = this.inverted ? 15 - be.state : be.state;
            this.value = this.signal / 15f;
            this.animatedValue = this.lastAnimatedValue = this.value;
        }
        super.startHold(level, player, pos);
    }

    @Override
    public boolean activeTick(final Level level, final LocalPlayer player) {
        if (level.getBlockEntity(this.getInteractionPos()) instanceof ThrottleLeverBlockEntity &&
                BlockHoldInteraction.inInteractionRange(player, this.getInteractionPos().getCenter(), 4)) {
            final float speed = 0.85f;
            this.lastAnimatedValue = this.animatedValue;
            this.animatedValue = this.animatedValue * (1 - speed) + this.signal / 15f * speed;
            return false;
        }
        return true;
    }

    @Override
    public void renderOverlay(final GuiGraphics graphics, final int width, final int height, final boolean hideGui) {
        if (hideGui)
            return;

        final int h = 14;
        final int w = 100;
        final int x;
        final int y;
        x = width / 2 - w / 2 + 16;
        y = height / 2 - h / 2;
        final PoseStack ps = graphics.pose();

        ps.pushPose();

        ps.translate(x + w / 2, y + h / 2, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(90.0f));
        ps.translate(-x - w / 2, -y - h / 2, 0);

        AllGuiTextures.BRASS_FRAME_TL.render(graphics, x, y);
        AllGuiTextures.BRASS_FRAME_TR.render(graphics, x + w - 4, y);
        AllGuiTextures.BRASS_FRAME_BL.render(graphics, x, y + h - 4);
        AllGuiTextures.BRASS_FRAME_BR.render(graphics, x + w - 4, y + h - 4);
        final int zLevel = 2;

        UIRenderHelper.drawStretched(graphics, x, y + 4, 3, h - 8, zLevel, AllGuiTextures.BRASS_FRAME_LEFT);
        UIRenderHelper.drawStretched(graphics, x + w - 3, y + 4, 3, h - 8, zLevel, AllGuiTextures.BRASS_FRAME_RIGHT);
        UIRenderHelper.drawCropped(graphics, x + 4, y, w - 8, 3, zLevel, AllGuiTextures.BRASS_FRAME_TOP);
        UIRenderHelper.drawCropped(graphics, x + 4, y + h - 3, w - 8, 3, zLevel, AllGuiTextures.BRASS_FRAME_BOTTOM);

        final int valueBarX = x + 3;
        final int valueBarWidth = w - 6;

        for (int w1 = 0; w1 < valueBarWidth; w1 += AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1)
            UIRenderHelper.drawCropped(graphics, valueBarX + w1, y + 3,
                    Math.min(AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1, valueBarWidth - w1), 8,
                    zLevel, AllGuiTextures.VALUE_SETTINGS_BAR);

        ps.popPose();


        ps.pushPose();
        ps.translate(0.0, 0.0, 4.0);
        final float partialTick = AnimationTickHolder.getPartialTicks();
        final float currentValue = this.lastAnimatedValue * (1 - partialTick) + this.animatedValue * partialTick;

        final float cursorY = ((1.0f - 2.0f * currentValue) * 3.0f * h) + 2;
        final int cx = x + w / 2 - 7;
        final float cy = y + h / 2 - 9 + cursorY;
        final int cursorWidth = 14;
        ps.pushPose();
        ps.translate(0, cy, 0);

        AllGuiTextures.VALUE_SETTINGS_CURSOR_LEFT.render(graphics, cx - 3, 0);
        UIRenderHelper.drawCropped(graphics, cx, 0, cursorWidth, 14, zLevel, AllGuiTextures.VALUE_SETTINGS_CURSOR);
        AllGuiTextures.VALUE_SETTINGS_CURSOR_RIGHT.render(graphics, cx + cursorWidth, 0);

        ps.translate(0.0, 0.0, 4.0);

        graphics.drawString(Minecraft.getInstance().font, String.valueOf(this.inverted ? 15 - this.signal : this.signal), cx + 1, 3, SimColors.THROTTLE_VALUE_BROWN, false);

        ps.popPose();

        ps.popPose();
    }

    @Override
    public boolean activeOnMouseMove(final double yaw, final double pitch) {
        this.value -= (float) (pitch / 180.0);

        this.value = Math.min(1.0f, Math.max(0.0f, this.value));

        final int newSignal = Math.round(this.value * 15.0f);
        this.signal = Math.min(15, Math.max(0, newSignal));

        if (this.signal != this.lastSignal) {
            this.lastSignal = this.signal;
            this.changed();
        }

        return true;
    }

    private void changed() {
        VeilPacketManager.server().sendPacket(new ThrottleLeverSignalPacket(this.getInteractionPos(), this.signal));
    }
}
