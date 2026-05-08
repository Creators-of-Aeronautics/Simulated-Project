package dev.simulated_team.simulated.content.entities.diagram.screen;

import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimSoundEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DiagramButton extends AbstractWidget {
    private SimGUITextures texture;
    private final Runnable onClick;
    private Supplier<List<Component>> diagramTooltip;

    private BooleanSupplier iconSwitch;

    public DiagramButton(final SimGUITextures texture, final int x, final int y, final Component message, final Runnable onClick) {
        super(x, y, texture.width, texture.height, message);
        this.texture = texture;
        this.onClick = onClick;

        this.iconSwitch = this::isHovered;
    }

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        super.onClick(mouseX, mouseY);
        this.onClick.run();
    }

    public void setTexture(final SimGUITextures texture) {
        this.texture = texture;
    }

    public SimGUITextures getTexture() {
        return this.texture;
    }

    @Override
    protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.texture.render(guiGraphics, this.getX() - 1, this.getY() - 1, this.isHovered() || this.iconSwitch.getAsBoolean() ? DiagramScreen.BUTTON_COLOR : DiagramScreen.DULL_BUTTON_COLOR);

        if (this.diagramTooltip != null && this.isHovered()) {
            DiagramScreen.renderTooltip(guiGraphics, mouseX, mouseY, (List<FormattedText>) (List<?>) this.diagramTooltip.get());
        }
    }

    @Override
    public void playDownSound(final SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(SimSoundEvents.DIAGRAM_TAP.event(), 1.0F));
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {

    }

    public DiagramButton setDiagramTooltip(final Supplier<List<Component>> diagramTooltip) {
        this.diagramTooltip = diagramTooltip;
        return this;
    }

    public DiagramButton setIconSwitch(final BooleanSupplier switcher) {
        this.iconSwitch = switcher;
        return this;
    }
}
