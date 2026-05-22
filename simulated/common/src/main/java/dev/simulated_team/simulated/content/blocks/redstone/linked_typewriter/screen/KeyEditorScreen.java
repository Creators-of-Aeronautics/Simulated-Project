package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets.ConfirmationWidgetBase;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimIcons;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class KeyEditorScreen {

    private static final SimGUITextures KEY_MENU = SimGUITextures.LINKED_TYPEWRITER_KEYS_MENU;
    private static final SimGUITextures KEY_ENTRY = SimGUITextures.LINKED_TYPEWRITER_KEY_ENTRY;

    private static final int MIN_SCROLL_Y = 50;
    private static final int ENTRY_HEIGHT_PADDING_PIXELS = 3;

    private final LinkedTypewriterScreen parentScreen;

    public boolean active;

    private int scroll = 0;
    private final LerpedFloat lerpedScroll = LerpedFloat.linear();

    /**
     * List of all currently active, editable entry wrappers
     */
    //list ensures we don't need to deal with nullability....
    ObjectArrayList<KeyEntryWidget> keyboardEntryWrappers = new ObjectArrayList<>();

    private final IconButton addWidget, confirmWidget, removeAllWidget;

    public KeyEditorScreen(final LinkedTypewriterScreen parentScreen) {
        this.parentScreen = parentScreen;

        this.addWidget = new IconButton(0, 0, AllIcons.I_ADD).withCallback(() -> {
            this.modifyEntry(null);
        });

        this.confirmWidget = new IconButton(0, 0, AllIcons.I_CONFIRM).withCallback(() -> {
            this.parentScreen.switchScreen(false);
        });

        this.removeAllWidget = new ConfirmationWidgetBase(0, 0, AllIcons.I_TRASH)
                .withMessage(Component.translatable("simulated.linked_typewriter.confirm_delete_all"))
                .withCallback(() -> parentScreen.sendNewKeys(true));

        this.resetPositions();
    }

    /**
     * Called when the linked typewriter starts editing all of its keys. This is where we add our widgets.
     */
    public void startEditing() {
        this.resetPositions();
        this.addAllWidgets();

        this.rebuildWrappers();

        this.active = true;
    }

    /**
     * Called when the linked typewriter stops editing all of its keys. removes our widgets from the screen.
     */
    public void endEditing() {
        this.removeAllWidgets();

        this.keyboardEntryWrappers.clear();
        this.active = false;
    }

    /**
     * Resets the positions of all widgets when the screen is rescaled
     */
    public void resetPositions() {
        final int widgetHeight = this.topPos() + KEY_MENU.height - 24;
        this.addWidget.setX(this.leftPos() + KEY_MENU.width - 54);
        this.addWidget.setY(widgetHeight);

        this.confirmWidget.setX(this.leftPos() + KEY_MENU.width - 25);
        this.confirmWidget.setY(widgetHeight);

        this.removeAllWidget.setX(this.leftPos() + 8);
        this.removeAllWidget.setY(widgetHeight);
    }

    public void activateAllWidgets() {
        this.addWidget.active = true;
        this.confirmWidget.active = true;
        this.removeAllWidget.active = true;

        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            wrapper.editWidget.active = true;
            wrapper.deleteWidget.active = true;
        }
    }

    public void deactivateAllWidgets() {
        this.addWidget.active = false;
        this.confirmWidget.active = false;
        this.removeAllWidget.active = false;

        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            wrapper.editWidget.active = false;
            wrapper.deleteWidget.active = false;
        }
    }

    public void addAllWidgets() {
        this.parentScreen.addWidget(this.addWidget);
        this.parentScreen.addWidget(this.confirmWidget);
        this.parentScreen.addWidget(this.removeAllWidget);

        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            this.parentScreen.addWidget(wrapper.editWidget);
            this.parentScreen.addWidget(wrapper.deleteWidget);
        }
    }

    public void removeAllWidgets() {
        this.parentScreen.removeWidget(this.addWidget);
        this.parentScreen.removeWidget(this.confirmWidget);
        this.parentScreen.removeWidget(this.removeAllWidget);

        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            this.parentScreen.removeWidget(wrapper.editWidget);
            this.parentScreen.removeWidget(wrapper.deleteWidget);
        }
    }

    public void tick() {
        this.lerpedScroll.chase(this.scroll, 0.8, LerpedFloat.Chaser.EXP);
        this.lerpedScroll.tickChaser();
        this.clampScroll();
    }

    /**
     * Attempts to shift all entries up or down by 1 value.</p>
     * Handles looping around.
     *
     * @param shiftLeft Whether entries should be shifted up
     */
    protected void shiftEntries(final boolean shiftLeft) {
        final int shiftBy = shiftLeft ? -1 : 1;

        this.scroll += shiftBy * 19;
        this.clampScroll();
    }

    private void clampScroll() {
        final int maxScroll = Math.max(0,
                (this.parentScreen.getNewEntries().getSize() - 4) * (SimGUITextures.LINKED_TYPEWRITER_KEY_ENTRY.height + ENTRY_HEIGHT_PADDING_PIXELS)
        );
        this.scroll = Math.clamp(this.scroll, 0, maxScroll);
    }

    /**
     * Attempts to add an entry to the entries list. Will fail if the list is full of entries already.
     */
    private void addEntry(final LinkedTypewriterEntries.KeyboardEntry entryFromModifier) {
        this.parentScreen.getNewEntries().setKey(entryFromModifier.glfwKeyCode, entryFromModifier);
        this.rebuildWrappers();
    }

    /**
     * Removes the given entry from the entries list.
     */
    private void removeWidget(final KeyEntryWidget wrapper) {
        final LinkedTypewriterEntries.KeyboardEntry entry = wrapper.entry;

        if (entry != null) {
            this.parentScreen.getNewEntries().setKey(entry.glfwKeyCode, null);
        }

        this.rebuildWrappers();
    }

    /**
     * Starts modifying the given entry through {@link EntryModifierScreen the modifier screen}.
     *
     * @param widget The wrapper to start modifying. Passing in null creates a new entry.
     */
    private void modifyEntry(@Nullable final KeyEditorScreen.KeyEntryWidget widget) {
        this.parentScreen.modifier.startModifying(widget == null ? null : widget.entry, (newEntry) -> {
            if (newEntry != null) {
                KeyEntryWidget alreadyPresent = null;
                for (final KeyEntryWidget wrapperEntry : this.keyboardEntryWrappers) {
                    if (wrapperEntry.entry.glfwKeyCode == newEntry.glfwKeyCode) {
                        alreadyPresent = wrapperEntry;
                        break;
                    }
                }

                if (alreadyPresent != null) {
                    this.parentScreen.getNewEntries().setKey(newEntry.glfwKeyCode, newEntry); // we still want to set the entry...
                    alreadyPresent.entry = newEntry;
                } else {
                    this.addEntry(newEntry);
                }
            }

            this.activateAllWidgets();
        });

        if (widget != null) {
            this.removeWidget(widget);
        }

        this.deactivateAllWidgets();
    }

    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float pt, final PoseStack ps) {
        guiGraphics.enableScissor(0, this.topPos() + 20, this.parentScreen.width, this.topPos() + KEY_MENU.height - 35);

        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            wrapper.render(guiGraphics, mouseX, mouseY, pt, ps);
        }

        guiGraphics.disableScissor();

        this.addWidget.render(guiGraphics, mouseX, mouseY, pt);
        this.confirmWidget.render(guiGraphics, mouseX, mouseY, pt);
        this.removeAllWidget.render(guiGraphics, mouseX, mouseY, pt);

        final int fadeOffColor = 0x00000000;
        final int fadeFromColor = 0x77000000;
        guiGraphics.fillGradient(this.leftPos() + 7, this.topPos() + 20, this.leftPos() + 231, this.topPos() + 30, fadeFromColor, fadeOffColor);
        guiGraphics.fillGradient(this.leftPos() + 7, this.topPos() + 150, this.leftPos() + 231, this.topPos() + 160, fadeOffColor, fadeFromColor);
    }

    public void rebuildWrappers() {
        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            this.parentScreen.removeWidget(wrapper.deleteWidget);
            this.parentScreen.removeWidget(wrapper.editWidget);
        }

        this.keyboardEntryWrappers.clear();

        final LinkedTypewriterEntries entries = this.parentScreen.getNewEntries();
        for (final LinkedTypewriterEntries.KeyboardEntry entry : entries.getEntries()) {
            final KeyEntryWidget wrapper = new KeyEntryWidget(entry);

            this.keyboardEntryWrappers.add(wrapper);

            this.parentScreen.addWidget(wrapper.editWidget);
            this.parentScreen.addWidget(wrapper.deleteWidget);
        }
    }

    public void renderBG(final GuiGraphics guiGraphics, final float v, final int i, final int i1) {
        KEY_MENU.render(guiGraphics, this.leftPos(), this.topPos());

        guiGraphics.enableScissor(0, this.topPos() + 20, this.parentScreen.width, this.topPos() + KEY_MENU.height - 35);
        for (final KeyEntryWidget wrapper : this.keyboardEntryWrappers) {
            wrapper.renderBackground(guiGraphics, v, i, i1);
        }
        guiGraphics.disableScissor();
    }

    private int leftPos() {
        return this.parentScreen.getLeftPos();
    }

    private int topPos() {
        return this.parentScreen.getTopPos() - 40;
    }

    /**
     * A wrapper entry for keys associated with a typewriter.<p/>
     * Handles Rendering, widget interaction. <p>
     * Holds a single redstone frequency for the key, self pointer, and widgets required for functionality. <p/>
     */
    private class KeyEntryWidget {
        private final NoXYButton editWidget;
        private final NoXYButton deleteWidget;
        private LinkedTypewriterEntries.KeyboardEntry entry;

        public KeyEntryWidget(final LinkedTypewriterEntries.KeyboardEntry entry) {
            this.editWidget = new NoXYButton(SimIcons.ADD_OR_EDIT)
                    .withCallback(() -> KeyEditorScreen.this.modifyEntry(this));

            this.deleteWidget = new NoXYButton(AllIcons.I_TRASH)
                    .withCallback(() -> KeyEditorScreen.this.removeWidget(this));

            this.entry = entry;
        }

        private float getCurrentHeight(final float partialTick) {
            final int index = KeyEditorScreen.this.keyboardEntryWrappers.indexOf(this);

            return (KeyEditorScreen.this.parentScreen.getTopPos() - 65)
                    + MIN_SCROLL_Y
                    - KeyEditorScreen.this.lerpedScroll.getValue(partialTick)
                    + (index * (SimGUITextures.LINKED_TYPEWRITER_KEY_ENTRY.height + ENTRY_HEIGHT_PADDING_PIXELS));
        }

        public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float pt, final PoseStack ps) {
            ps.pushPose();
            final int x = KeyEditorScreen.this.leftPos() + 12;
            final float y = this.getCurrentHeight(pt);
            ps.translate(x, y, 0);

            final int editIconOffset = 167;
            final float iconY = (float) KEY_ENTRY.height / 2;
            this.updateWidgetPositions(x, editIconOffset, y, iconY);

            this.renderItems(guiGraphics, ps);

            ps.popPose();
        }

        public void renderBackground(final GuiGraphics guiGraphics, final float pt, final int mouseX, final int mouseY) {
            final PoseStack ps = guiGraphics.pose();

            final int editIconOffset = 167;
            final float iconY = (float) KEY_ENTRY.height / 2;

            ps.pushPose();
            final int x = KeyEditorScreen.this.leftPos() + 12;
            final float y = this.getCurrentHeight(pt);
            ps.translate(x, y, 0);

            KEY_ENTRY.render(guiGraphics, 0, 0);
            this.renderText(guiGraphics, ps);
            this.renderWidgets(guiGraphics, mouseX, mouseY, pt, ps, editIconOffset, iconY);

            ps.popPose();
        }

        private void updateWidgetPositions(final int x, final int editIconOffset, final float y, final float iconY) {
            this.editWidget.setX(x + editIconOffset);
            this.editWidget.setY((int) (y + iconY - 9));
            this.deleteWidget.setX(x + editIconOffset + 23);
            this.deleteWidget.setY((int) (y + iconY - 9));
        }

        private void renderItems(final GuiGraphics guiGraphics, final PoseStack ps) {
            if (!KeyEditorScreen.this.parentScreen.modifier.modifying) {
                ps.pushPose();
                ps.translate(0, ((float) KEY_ENTRY.height / 2) - 8, 0);

                ps.translate(82, 0, 0);
                GuiGameElement.of(this.entry.getFirstAsItemStack()).render(guiGraphics);
                ps.translate(18, 0, 0);
                GuiGameElement.of(this.entry.getSecondAsItemStack()).render(guiGraphics);
                ps.popPose();
            }
        }

        private void renderText(final GuiGraphics guiGraphics, final PoseStack ps) {
            ps.pushPose();
            ps.translate((float) 9, 11, 0);
            guiGraphics.drawString(Minecraft.getInstance().font, InputConstants.getKey(this.entry.glfwKeyCode, -1).getDisplayName(), 0, 0, 0xFFFFFF, true);
            ps.popPose();
        }

        private void renderWidgets(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float pt, final PoseStack ps, final int editIconOffset, final float iconY) {
            ps.pushPose();
            ps.translate(editIconOffset, iconY - 9, 0);
            this.editWidget.render(guiGraphics, mouseX, mouseY, pt);

            ps.translate(23, 0, 0);
            this.deleteWidget.render(guiGraphics, mouseX, mouseY, pt);
            ps.popPose();
        }
    }

    public static class NoXYButton extends IconButton {
        public NoXYButton(final ScreenElement icon) {
            super(0, 0, icon);
        }

        @Override
        public void doRender(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
            if (this.visible) {
                this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

                final AllGuiTextures button = !this.active ? AllGuiTextures.BUTTON_DISABLED
                        : this.isHovered && AllKeys.isMouseButtonDown(0) ? AllGuiTextures.BUTTON_DOWN
                        : this.isHovered ? AllGuiTextures.BUTTON_HOVER
                        : this.green ? AllGuiTextures.BUTTON_GREEN : AllGuiTextures.BUTTON;

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                graphics.blit(button.location, 0, 0, button.getStartX(), button.getStartY(), button.getWidth(),
                        button.getHeight());
                this.icon.render(graphics, 1, 1);
            }
        }
    }

}
