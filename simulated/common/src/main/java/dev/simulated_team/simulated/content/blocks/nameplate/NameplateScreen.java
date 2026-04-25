package dev.simulated_team.simulated.content.blocks.nameplate;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.network.packets.name_plate.NameplateChangeNamePacket;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class NameplateScreen extends Screen {
   public static final int MAX_WIDTH = 8 * 16 - 6;
   private final NameplateBlockEntity be;
   private String message;
   private int frame;
   @Nullable
   private TextFieldHelper nameField;

   private Button button;

   public NameplateScreen(final NameplateBlockEntity pSign) {
      this(pSign, SimLang.translate("nameplate.edit").component());
   }

   public NameplateScreen(final NameplateBlockEntity pSign, final Component pTitle) {
      super(pTitle);
      this.be = pSign;
      this.message = pSign.getName();
   }

   @Override
   protected void init() {
       this.button = this.addWidget(Button.builder(CommonComponents.GUI_DONE,
              (button) -> this.onDone()).bounds(this.width / 2 - 100,
              this.height / 4 + 144,
              200,
              20).build());

      this.nameField = new TextFieldHelper(() -> this.message,
              this::setMessage,
              TextFieldHelper.createClipboardGetter(this.minecraft),
              TextFieldHelper.createClipboardSetter(this.minecraft),
              (string) -> this.minecraft.font.width(string) <= MAX_WIDTH);
   }

   private void setMessage(final String s) {
      this.message = s;
   }

   @Override
   public void tick() {
      ++this.frame;
      if (!this.isValid()) {
         this.onDone();
      }
   }

   private boolean isValid() {
      return this.minecraft != null &&
              this.minecraft.player != null &&
              !this.be.isRemoved() &&
              NameplateBlockEntity.canPlayerReach(this.be, this.minecraft.player);
   }

   @Override
   public boolean keyPressed(final int pKeyCode, final int pScanCode, final int pModifiers) {
      if (pKeyCode != 264 && pKeyCode != 257 && pKeyCode != 335) {
         return this.nameField.keyPressed(pKeyCode, pScanCode, pModifiers) || super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
      return false;
   }

   @Override
   public boolean charTyped(final char pCodePoint, final int pModifiers) {
       if (!this.nameField.charTyped(pCodePoint)) {
           return super.charTyped(pCodePoint, pModifiers);
       }
       return true;
   }

   @Override
   public void render(final GuiGraphics gui, final int pMouseX, final int pMouseY, final float pPartialTick) {
      Lighting.setupForFlatItems();
      this.renderBackground(gui, pMouseX, pMouseY, pPartialTick);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);

      this.renderSign(gui);
      Lighting.setupFor3DItems();

      //we have to do this now because we are manually rendering the background, so we can't call super otherwise the background will be rendered twice
      this.button.render(gui, pMouseX, pMouseY, pPartialTick);
   }

   @Override
   public void onClose() {
      this.onDone();
   }

   @Override
   public void removed() {
      VeilPacketManager.server().sendPacket(new NameplateChangeNamePacket(this.be.findController().getBlockPos(), this.message));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   protected void renderSignBackground(final GuiGraphics gui, final BlockState pState) {
      final String color = ((NameplateBlock) pState.getBlock()).getColor().getSerializedName();

      final PoseStack ps = gui.pose();

      ps.pushPose();
      final float sy = 15.0f / 12.0f;
      ps.scale(sy, sy, 1.0f);
      ps.translate(8.0 - 16.0 * 4, 5.7, 0.0);

      gui.blit(Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 0.0F, 12, 16, 10, 32, 32);

      for (int i = 0; i < 6; i++) {
         ps.translate(16.0, 0.0, 0.0);
         gui.blit(Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8,  8, 12, 16, 10, 32, 32);
      }
      ps.translate(16.0, 0.0, 0.0);
      gui.blit(Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 16, 12, 16, 10, 32, 32);

      ps.popPose();
   }

   protected void offsetSign(final GuiGraphics pGuiGraphics, final BlockState pState) {
      pGuiGraphics.pose().translate((float)this.width / 2.0F, this.height / 2f - 26, 50.0F);
   }

   private void renderSign(final GuiGraphics pGuiGraphics) {
      final PoseStack ps = pGuiGraphics.pose();

      ps.pushPose();

      final BlockState blockstate = this.be.getBlockState();
      pGuiGraphics.pose().pushPose();
      this.offsetSign(pGuiGraphics, blockstate);
      final float scale = 2.0f;
      ps.scale(scale, scale, scale);
      pGuiGraphics.pose().pushPose();
      this.renderSignBackground(pGuiGraphics, blockstate);
      pGuiGraphics.pose().popPose();
      this.renderSignText(pGuiGraphics);
      pGuiGraphics.pose().popPose();

      ps.popPose();
   }

   private void renderSignText(final GuiGraphics pGuiGraphics) {
      final int lineHeight = 8;
      pGuiGraphics.pose().translate(0.0F, 0.0F, 4.0F);

      final int color = this.be.getDarkColor(this.be.getTextColor());

      final boolean cursorFlash = this.frame / 6 % 2 == 0;
      final int cursorPos = this.nameField.getCursorPos();
      final int selectionPos = this.nameField.getSelectionPos();

      if (this.message != null) {
         if (this.font.isBidirectional()) {
             this.message = this.font.bidirectionalShaping(this.message);
         }

         final int w = -this.font.width(this.message) / 2;
         pGuiGraphics.drawString(this.font, this.message, w, 0, color, false);
         if (cursorPos >= 0 && cursorFlash) {
            final int l1 = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
            final int i2 = l1 - this.font.width(this.message) / 2;
            if (cursorPos >= this.message.length()) {
               pGuiGraphics.drawString(this.font, "_", i2, 0, color, false);
            }
         }
      }

      if (this.message != null && cursorPos >= 0) {
         final int width = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
         final int cen = width - this.font.width(this.message) / 2;
         if (cursorFlash && cursorPos < this.message.length()) {
            pGuiGraphics.fill(cen, -1, cen + 1, lineHeight, -16777216 | color);
         }

         if (selectionPos != cursorPos) {
            final int min = Math.min(cursorPos, selectionPos);
            final int max = Math.max(cursorPos, selectionPos);
            final int minWidth = this.font.width(this.message.substring(0, min)) - this.font.width(this.message) / 2;
            final int maxWith = this.font.width(this.message.substring(0, max)) - this.font.width(this.message) / 2;
            final int selMin = Math.min(minWidth, maxWith);
            final int selMax = Math.max(minWidth, maxWith);
            pGuiGraphics.fill(RenderType.guiTextHighlight(), selMin, -1, selMax, lineHeight, -16776961);
         }
      }
   }

   private void onDone() {
      this.minecraft.setScreen(null);
   }

   public static void setScreen(final NameplateBlockEntity be) {
      if (be != null && NameplateBlockEntity.canPlayerReach(be, Minecraft.getInstance().player)) {
         final NameplateScreen screen = new NameplateScreen(be.findController());
         Minecraft.getInstance().setScreen(screen);
      }
   }
}