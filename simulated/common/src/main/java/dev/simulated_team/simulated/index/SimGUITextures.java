package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simulated_team.simulated.Simulated;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public enum SimGUITextures implements ScreenElement {

    MODULATINGLINK("modulating_linked_receiver", 182, 99),
    MODULATINGLINK_MARKER("modulating_linked_receiver", 193, 4, 3, 20),
    MODULATINGLINK_POWERED_LANE("modulating_linked_receiver", 0, 100, 120, 16),
    MODULATINGLINK_TARGET("modulating_linked_receiver", 188, 4, 4, 4),
    ALTITUDE_SENSOR("altitude_sensor", 42, 206),
    ALTITUDE_SENSOR_BAR_LIT("altitude_sensor", 48, 0, 10, 200),
    ALTITUDE_SENSOR_GRABBY_THING("altitude_sensor", 64, 0, 26, 16),

    DIAGRAM("diagram", 0, 64, 256, 192, 512, 256),
    DIAGRAM_PAPER("diagram", 256, 0, 96, 128, 512, 256),
    DIAGRAM_STICKY_NOTE("diagram", 256, 128, 112, 112, 512, 256),
    DIAGRAM_TAB("diagram", 240, 0, 11, 10, 512, 256),
    DIAGRAM_ICON_MAGNIFYING_GLASS("diagram", 32, 16, 16, 16, 512, 256),
    DIAGRAM_ICON_FORCES("diagram", 0, 48, 16, 16, 512, 256),
    DIAGRAM_ICON_FORCES_SEPARATED("diagram", 32, 32, 16, 16, 512, 256),
    DIAGRAM_ICON_FORCES_MERGED("diagram", 48, 32, 16, 16, 512, 256),
    DIAGRAM_ICON_MASS("diagram", 16, 48, 16, 16, 512, 256),
    DIAGRAM_ICON_COM_TOGGLE("diagram", 32, 48, 16, 16, 512, 256),
    DIAGRAM_ICON_INERTIA_TENSOR("diagram", 0, 0, 16, 16, 512, 256),
    DIAGRAM_ICON_COM("diagram", 48, 48, 16, 16, 512, 256),
    DIAGRAM_ICON_COM_TINY("diagram", 48, 0, 16, 16, 512, 256),
    DIAGRAM_ICON_COM_ARROW("diagram", 48, 16, 16, 16, 512, 256),
    DIAGRAM_ICON_ARROW_IN_PAGE_SHADOW("diagram", 0, 16, 16, 16, 512, 256),
    DIAGRAM_ICON_ARROW_OUT_PAGE_SHADOW("diagram", 16, 16, 16, 16, 512, 256),
    DIAGRAM_ICON_ARROW_IN_PAGE("diagram", 0, 32, 16, 16, 512, 256),
    DIAGRAM_ICON_ARROW_OUT_PAGE("diagram", 16, 32, 16, 16, 512, 256),

    DIAGRAM_ICON_TURN_LEFT("diagram", 192, 48, 8, 13, 512, 256),
    DIAGRAM_ICON_TURN_RIGHT("diagram", 240, 48, 8, 13, 512, 256),
    DIAGRAM_ICON_TURN_DOWN("diagram", 208, 48, 7, 7, 512, 256),
    DIAGRAM_ICON_TURN_UP("diagram", 224, 48, 7, 7, 512, 256),

    LINKED_REMOTE("linked_remote", 7, 1, 108, 109),
    LINKED_REMOTE_COLOR("linked_remote", 16, 112, 9, 40),
    ASSEMBLER_TRACK_START("assembler", 0, 0, 14, 6),
    ASSEMBLER_TRACK_MIDDLE("assembler", 0, 7, 14, 10),
    ASSEMBLER_TRACK_END("assembler", 0, 18, 14, 5),

    LINKED_TYPEWRITER_MAIN("linked_typewriter/linked_typewriter", 0, 0, 246, 127),

    LINKED_TYPEWRITER_KEYS_MENU("linked_typewriter/linked_typewriter_keys", 0, 0, 238, 195),
    LINKED_TYPEWRITER_KEY_BINDING("linked_typewriter/linked_typewriter_keys", 0, 195, 191, 30),
    LINKED_TYPEWRITER_KEY_ENTRY("linked_typewriter/linked_typewriter_keys", 0, 225, 214, 30),
    LINKED_TYPEWRITER_KEY_MODIFICATION_MENU("linked_typewriter/linked_typewriter", 0, 145, 214, 80),

    LINKED_TYPEWRITER_FREQUENCY("linked_typewriter/linked_typewriter", 0, 127, 36, 18),
    LINKED_TYPEWRITER_TOOLTIP_ARROW("linked_typewriter/linked_typewriter", 36, 127, 18, 11),

    LINKED_TYPEWRITER_TOOLTIP_BACKGROUND(Simulated.path("widgets/item_background"), 0, 0 , 6, 6, 6, 6),

    KEY_START("linked_typewriter/linked_typewriter", 60, 127, 6, 14),
    KEY_MIDDLE("linked_typewriter/linked_typewriter", 66, 127, 2, 14),
    KEY_END("linked_typewriter/linked_typewriter", 68, 127, 6, 14),
    INACTIVE_KEY_START("linked_typewriter/linked_typewriter", 73, 127, 6, 14),
    INACTIVE_KEY_MIDDLE("linked_typewriter/linked_typewriter", 79, 127, 2, 14),
    INACTIVE_KEY_END("linked_typewriter/linked_typewriter", 81, 127, 6, 14),
    LINKED_TYPEWRITER_BIND("linked_typewriter/linked_typewriter", 0, 154, 212, 89),
    LINKED_TYPEWRITER_ARROW_LEFT("linked_typewriter/linked_typewriter", 0, 244, 9, 7),
    LINKED_TYPEWRITER_ARROW_RIGHT("linked_typewriter/linked_typewriter", 10, 244, 9, 7),
    LINKED_TYPEWRITER_TRASH_CONFIRM_HOVER("linked_typewriter/linked_typewriter", 113, 127, 18, 18);

    @NotNull
    public final ResourceLocation location;

    public final int width, height;
    public final int startX, startY;
    public final int texWidth, texHeight;

    SimGUITextures(final String location, final int width, final int height) {
        this(location, 0, 0, width, height);
    }

    SimGUITextures(final int startX, final int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }

    SimGUITextures(final String location, final int startX, final int startY, final int width, final int height) {
        this(Simulated.MOD_ID, location, startX, startY, width, height);
    }

    SimGUITextures(final String location, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        this(Simulated.MOD_ID, location, startX, startY, width, height, texWidth, texHeight);
    }

    SimGUITextures(final String namespace, final String location, final int startX, final int startY, final int width, final int height) {
        this(namespace, location, startX, startY, width, height, 256, 256);
    }

    SimGUITextures(final ResourceLocation location, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    SimGUITextures(final String namespace, final String location, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        final ResourceLocation loc = ResourceLocation.tryBuild(namespace, "textures/gui/" + location + ".png");
        assert loc != null; //location should never be null here, if it is, we messed up

        this.location = loc;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    public void bind() {
        RenderSystem.setShaderTexture(0, this.location);
    }

    public void render(final GuiGraphics graphics, final int x, final int y) {
        graphics.blit(this.location, x, y, this.startX, this.startY, this.width, this.height, this.texWidth, this.texHeight);
    }

    public void render (final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        graphics.blit(this.location, x, y, this.startX, this.startY, width, height, this.texWidth, this.texHeight);
    }

    public void render(final GuiGraphics graphics, final int x, final int y, final Color c) {
        this.bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, 0, this.startX, this.startY, this.width, this.height, this.texWidth, this.texHeight);
    }
}
