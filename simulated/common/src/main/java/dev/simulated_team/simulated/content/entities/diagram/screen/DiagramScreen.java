package dev.simulated_team.simulated.content.entities.diagram.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramDataPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramSaveConfigPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.RequestDiagramDataPacket;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class DiagramScreen extends AbstractSimiScreen {
    public static int UPDATE_REQUEST_INTERVAL = 10;

    public static final Color TEXT_COLOR = new Color(79, 82, 87);
    public static final Color BUTTON_COLOR = new Color(109, 113, 119);
    public static final Color DULL_BUTTON_COLOR = new Color(181, 177, 168);
    public static final Color BG_COLOR = new Color(247, 240, 221);
    private static final int TOOLTIP_LABEL_COLOR = 0xffc2937d;

    private static final int MIN_ARROW_SIZE_PX = 6;

    public static final float PAPER_SLIDE_SPEED = 0.4f;
    public static final float TAB_SLIDE_SPEED = 0.1f;
    public static final int MAX_PAPER_OFFSET = SimGUITextures.DIAGRAM_PAPER.width + 3;
    public static final int MIN_PAPER_OFFSET = 10;

    private static final Vector3d LOCAL_CAMERA_POSITION = new Vector3d();
    private static final Vector3d CAMERA_POSITION = new Vector3d();
    private static final Matrix4f PROJECTION_MAT = new Matrix4f();
    public static final Quaternionf LOCAL_ORIENTATION = new Quaternionf();

    private static final Vector2d MAGNIFYING_CENTER = new Vector2d();
    private static final Vector2d MAGNIFYING_MAX = new Vector2d();
    private static final Vector2d MAGNIFYING_MIN = new Vector2d();
    private static final int MIN_MAGNIFICATION_PIXELS = 3;

    public static final SimGUITextures DIAGRAM_TEXTURE = SimGUITextures.DIAGRAM;
    public static final float FPS = 12.0f;

    private final DiagramEntity diagram;
    public final ClientSubLevel subLevel;

    protected DiagramConfig config;
    private boolean configDirty = false;

    private final List<DiagramForceGroupToggle> forceToggleWidgets = new ObjectArrayList<>();

    private AdvancedFbo fbo;
    private AdvancedFbo outlineFbo;
    private AdvancedFbo finalFbo;

    private float renderTime = FPS;

    private boolean paperVisible = false;
    private float lastPaperOffset = MIN_PAPER_OFFSET;
    private float paperOffset = MIN_PAPER_OFFSET;
    private float lastTabOffset = 0;
    private float tabOffset = 0;

    public final List<FormattedText> tooltipList = new ArrayList<>();

    @Nullable
    private DiagramDataPacket serverData = null;
    private float viewportRadius;

    private int ticksWithoutUpdate = 0;

    private DiagramButton turnUpButton;
    private DiagramButton turnDownButton;

    private DiagramButton mergeButton;

    private boolean magnifying = false;

    private DiagramStickyNote note;

    public DiagramScreen(final DiagramEntity diagramEntity, final ClientSubLevel subLevel) {
        this.diagram = diagramEntity;
        this.subLevel = subLevel;
    }

    public static void open(final DiagramEntity diagramEntity, final DiagramConfig config, final SubLevel subLevel) {
        final Minecraft minecraft = Minecraft.getInstance();
        final DiagramScreen screen = new DiagramScreen(diagramEntity, (ClientSubLevel) subLevel);

        screen.config = config;
        screen.updateViewportOrientation();

        minecraft.setScreen(screen);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0f));
    }

    private void updateViewportOrientation() {
        this.renderTime = Float.MAX_VALUE;
        LOCAL_ORIENTATION.identity().rotateY((float) Math.toRadians(this.config.yaw())).rotateX((float) Math.toRadians(this.config.pitch()));
    }

    private void freeFramebuffers() {
        if (this.note != null) {
            this.note.free();
        }

        if (this.fbo != null) {
            this.fbo.free();
            this.fbo = null;

            this.outlineFbo.free();
            this.outlineFbo = null;

            this.finalFbo.free();
            this.finalFbo = null;
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        this.freeFramebuffers();
    }

    @Override
    protected void init() {
        super.init();
        this.freeFramebuffers();
        this.fbo = AdvancedFbo.withSize(DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height).addColorTextureBuffer().setDepthTextureBuffer().build(true);
        this.outlineFbo = AdvancedFbo.withSize(DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height).addColorTextureBuffer().build(true);
        this.finalFbo = AdvancedFbo.withSize(DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height).addColorTextureBuffer().build(true);

        final int diagramX = this.width / 2 - DIAGRAM_TEXTURE.width / 2;
        final int diagramY = this.height / 2 - DIAGRAM_TEXTURE.height / 2;

        this.note = new DiagramStickyNote(this, diagramX, diagramY, Component.empty(), () -> {
        });
        this.note.create(this.config.getNoteConfigs());

        if (this.subLevel.isRemoved()) {
            this.onClose();
            return;
        }

        this.renderContents(this.subLevel, 0);

        for (int i = 0; i < 1; i++) {
            this.addGreebles(diagramX, diagramY);
        }

//        final DiagramButton glass = new DiagramButton(SimGUITextures.DIAGRAM_ICON_MAGNIFYING_GLASS, diagramX + 18 + 11, diagramY + 9, Component.empty(), () -> {
//            this.magnifying = !this.magnifying;
//        }).setDiagramTooltip(() -> SimLang.text("Magnify Selection").component()).setIconSwitch(this::isMagnifying);

        final DiagramButton forceButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_FORCES, diagramX + 9, diagramY + 9, Component.empty(), () -> {
            this.paperVisible = !this.paperVisible;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }).setDiagramTooltip(() -> SimLang.translate("contraption_diagram.toggle_paper").component());

        this.mergeButton = new DiagramButton(this.getMergeIcon(), diagramX + 9, diagramY + 9 + 20, Component.empty(), () -> {
            this.config.setMergeForces(!this.config.mergeForces());
            this.mergeButton.setTexture(this.getMergeIcon());
            this.setConfigDirty();
        }).setDiagramTooltip(() -> {
            return SimLang.translate("contraption_diagram.merge_forces").color(TOOLTIP_LABEL_COLOR).add(SimLang.translate(this.config.mergeForces() ? "contraption_diagram.merged" : "contraption_diagram.unmerged").color(0xffffffff)).component();
        });

        final DiagramButton centerOfMassButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_COM_TOGGLE, diagramX + 9, diagramY + 9 + 20 * 2, Component.empty(), () -> {
            this.config.setDisplayCenterOfMass(!this.config.displayCenterOfMass());
            this.setConfigDirty();
        }).setDiagramTooltip(() -> {
            return SimLang.translate("contraption_diagram.center_of_mass").color(TOOLTIP_LABEL_COLOR).add(SimLang.translate(this.config.displayCenterOfMass() ? "contraption_diagram.shown" : "contraption_diagram.hidden").color(0xffffffff)).component();
        });

        final DiagramButton massButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_MASS, diagramX + 9, diagramY + 9 + 20 * 3, Component.empty(), () -> {

        }).setDiagramTooltip(() -> {
            final String massString = this.serverData != null ? String.format("%,.2f", this.serverData.mass()) : "---";
            return SimLang.translate("contraption_diagram.total_mass").color(TOOLTIP_LABEL_COLOR).add(SimLang.translate("contraption_diagram.mass", massString).color(0xffffffff)).component();
        });

        massButton.active = false;

        this.addRenderableWidget(forceButton);
        this.addRenderableWidget(centerOfMassButton);
        this.addRenderableWidget(massButton);
        this.addRenderableWidget(this.mergeButton);

        this.addRotationGizmo(diagramX, diagramY);
        this.addForceToggleWidgets(diagramX, diagramY);

        this.addWidget(this.note);
    }

    private void addRotationGizmo(final int diagramX, final int diagramY) {
        this.turnUpButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_TURN_UP, diagramX + 236, diagramY + 8, Component.empty(), () -> {
            this.rotateDiagram(0, -1);
        });
        this.turnDownButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_TURN_DOWN, diagramX + 236, diagramY + 8 + 14, Component.empty(), () -> {
            this.rotateDiagram(0, 1);
        });
        final DiagramButton turnLeftButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_TURN_LEFT, diagramX + 228, diagramY + 12, Component.empty(), () -> {
            this.rotateDiagram(1, 0);
        });
        final DiagramButton turnRightButton = new DiagramButton(SimGUITextures.DIAGRAM_ICON_TURN_RIGHT, diagramX + 243, diagramY + 12, Component.empty(), () -> {
            this.rotateDiagram(-1, 0);
        });

        this.addRenderableWidget(this.turnUpButton);
        this.addRenderableWidget(this.turnDownButton);
        this.addRenderableWidget(turnLeftButton);
        this.addRenderableWidget(turnRightButton);
    }

    private void rotateDiagram(int yawSteps, final int pitchSteps) {
        if (this.config.pitch() > 45.0) {
            yawSteps = -yawSteps;
        }

        this.config.setYaw(this.config.yaw() + yawSteps * 90.0f);
        this.config.setPitch(Mth.clamp(this.config.pitch() + pitchSteps * 90.0f, -90.0f, 90.0f));
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0f));

        this.updateViewportOrientation();
        this.setConfigDirty();
    }

    private void addForceToggleWidgets(final int diagramX, final int diagramY) {
        final Iterable<ForceGroup> forceGroups = ForceGroups.REGISTRY;

        this.forceToggleWidgets.clear();

        int i = 0;
        for (final ForceGroup forceGroup : forceGroups) {
            final int yOffset = 11 * (i + 1) - 1;
            final int xOffset = -MAX_PAPER_OFFSET - 6;
            final DiagramForceGroupToggle widget = new DiagramForceGroupToggle(this, forceGroup, diagramX + xOffset, diagramY + yOffset);
            this.addWidget(widget);

            this.forceToggleWidgets.add(widget);
            i++;
        }
    }

    // this is horrid :(
    private HashMap<ResourceLocation, Tuple<Greeble, ArrayList<Greeble.TextureSlice>>> genGreebleSet(final RandomSource random) {
        final HashMap<ResourceLocation, Tuple<Greeble, ArrayList<Greeble.TextureSlice>>> greebleSet = new HashMap<>();

        for (final Map.Entry<ResourceLocation, Greeble> entry : SimResourceManagers.GREEBLE.entrySet()) {
            greebleSet.put(entry.getKey(), new Tuple<>(entry.getValue(), entry.getValue().shuffled()));
        }

        return greebleSet;
    }

    private ResourceLocation randomGreeble(final RandomSource random) {
        float weightSum = 0;

        for (final Greeble greeble : SimResourceManagers.GREEBLE.entries()) {
            weightSum += greeble.weight();
        }

        float weight = random.nextFloat() * weightSum;

        for (final Map.Entry<ResourceLocation, Greeble> greeble : SimResourceManagers.GREEBLE.entrySet()) {
            weight -= greeble.getValue().weight();

            if (weight <= 0) {
                return greeble.getKey();
            }
        }
        throw new RuntimeException();
    }

    private void addGreebles(final int diagramX, final int diagramY) {
        final RandomSource random = this.subLevel.getLevel().getRandom();

        final HashMap<ResourceLocation, Tuple<Greeble, ArrayList<Greeble.TextureSlice>>> greebleSet = this.genGreebleSet(random);
        final List<AABB> placed = new ObjectArrayList<>();

        // Avoid top-left region (diagram buttons are placed there)
        placed.add(new AABB(0, 0, 0, 26, 66, 1));

        // Avoid rotation gizmo
        placed.add(new AABB(227, 8, 0, 250, 28, 1));

        final int padding = 10;
        final int greebles = 8;

        this.finalFbo.bindRead();

        for (int i = 0; i < greebles; i++) {
            final ResourceLocation greebleID = this.randomGreeble(random);
            final Greeble greeble = SimResourceManagers.GREEBLE.get(greebleID);
            final ArrayList<Greeble.TextureSlice> slices = greebleSet.get(greebleID).getB();
            if (slices.isEmpty()) {
                continue;
            }
            final Greeble.TextureSlice slice = slices.removeFirst();

            final int x = random.nextInt(padding, DIAGRAM_TEXTURE.width - slice.width() - padding);
            final int y = random.nextInt(padding, DIAGRAM_TEXTURE.height - slice.height() - padding);

            final AABB box = new AABB(x, y, 0, x + slice.width(), y + slice.height(), 1);
            boolean intersects = false;
            for (final AABB aabb : placed) {
                if (box.intersects(aabb)) {
                    intersects = true;
                    break;
                }
            }

            if (intersects || this.aabbInFramebuffer(box)) {
                continue;
            }

            placed.add(box);
            this.addRenderableOnly(new GreebleRenderable(x + diagramX, y + diagramY, greeble.width(), greeble.height(), greeble.texture(), slice));
        }

        AdvancedFbo.unbind();
    }

    private boolean aabbInFramebuffer(final AABB aabb) {
        final int minX = (int) aabb.minX;
        final int minY = (int) (DIAGRAM_TEXTURE.height - aabb.minY);
        final int maxX = (int) aabb.maxX;
        final int maxY = (int) (DIAGRAM_TEXTURE.height - aabb.maxY);

        final int width = Math.abs(maxX - minX);
        final int height = Math.abs(maxY - minY);

        final int length = width * height;
        final int[] buffer = new int[length];
        glReadPixels(minX, minY - height, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        for (int i = 0; i < length; i++) {
            final int color = buffer[i] >> 24;
            if (color != 0) return true;
        }
        return false;
    }

    private void renderContents(final SubLevel subLevel, final float partialTicks) {
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();

        // Only render at framerate
        if (this.renderTime >= 20.0f / FPS) {
            this.renderTime = 0.0f;
        } else {
            this.renderTime += minecraft.getTimer().getRealtimeDeltaTicks();
            return;
        }

        if (this.fbo == null) {
            return;
        }

        final float zNear = 0.1f;
        final LevelPlot plot = subLevel.getPlot();

        final BoundingBox3ic plotBounds = plot.getBoundingBox();
        float radius = Math.max(Math.max(plotBounds.maxX() - plotBounds.minX(), plotBounds.maxY() - plotBounds.minY()), plotBounds.maxZ() - plotBounds.minZ()) + 1;
        radius *= 0.55F;

        radius = Math.max(radius, 2.0f);

        this.viewportRadius = radius;
        final Vector3d plotBoundsCenter = new Vector3d((plotBounds.minX() + plotBounds.maxX() + 1) / 2.0, (plotBounds.minY() + plotBounds.maxY() + 1) / 2.0, (plotBounds.minZ() + plotBounds.maxZ() + 1) / 2.0);

        final float aspect = (float) DIAGRAM_TEXTURE.width / DIAGRAM_TEXTURE.height;
        PROJECTION_MAT.identity().ortho(-radius * aspect, radius * aspect, -radius, radius, zNear, radius * 2.0f);

        // account for the smaller screen size
        LOCAL_CAMERA_POSITION.set(plotBoundsCenter.add(LOCAL_ORIENTATION.transform(new Vector3d(0, 0, radius))));

        final Pose3dc renderPose = ((ClientSubLevel) subLevel).renderPose(partialTicks);
        renderPose.transformPosition(CAMERA_POSITION.set(LOCAL_CAMERA_POSITION));

        draw(subLevel, partialTicks, LOCAL_ORIENTATION, PROJECTION_MAT, CAMERA_POSITION, DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height, this.fbo, this.outlineFbo, this.finalFbo, 0.25f, 1.0f, 0x2E3032, 0x696965);
    }

    public static void draw(final SubLevel subLevel, final float partialTicks, final Quaternionf localOrientation, final Matrix4f projMatrix, final Vector3d cameraPos, final float inWidth, final float inHeight, final AdvancedFbo fbo, final AdvancedFbo outlineFbo, final AdvancedFbo finalFbo, final float paletteOffset, final float fadeScale, final int lineColor, final int lineShadowColor) {
        fbo.bind(true);
        fbo.clear();

        final Pose3dc renderPose = ((ClientSubLevel) subLevel).renderPose(partialTicks);
        final Quaternionf orientation = new Quaternionf(renderPose.orientation()).conjugate();
        orientation.premul(localOrientation.conjugate(new Quaternionf()));

        SimpleSubLevelGroupRenderer.renderChain(subLevel, fbo, new Matrix4f(), projMatrix, cameraPos, orientation, partialTicks);

        final PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        final PostPipeline pipeline = manager.getPipeline(Simulated.path("diagram"));

        if (pipeline != null) {
            final Color LINE_SHADOW_COLOR = new Color(lineShadowColor);
            final Color LINE_COLOR = new Color(lineColor);

            pipeline.getUniformSafe("LineColor").setVector((LINE_COLOR.getRed()) / 255.0f, (LINE_COLOR.getGreen()) / 255.0f, (LINE_COLOR.getBlue()) / 255.0f, 1.0f);
            pipeline.getUniformSafe("LineShadowColor").setVector((LINE_SHADOW_COLOR.getRed()) / 255.0f, (LINE_SHADOW_COLOR.getGreen()) / 255.0f, (LINE_SHADOW_COLOR.getBlue()) / 255.0f, 1.0f);
            pipeline.getUniformSafe("InSize").setVector(inWidth, inHeight);
            pipeline.getUniformSafe("PaletteOffset").setFloat(paletteOffset);
            pipeline.getUniformSafe("FadeScale").setFloat(fadeScale);
        }

        final PostPipeline.Context context = manager.getPostPipelineContext();
        context.setFramebuffer(Simulated.path("diagram"), fbo);
        context.setFramebuffer(Simulated.path("diagram_outlined"), outlineFbo);
        context.setFramebuffer(Simulated.path("diagram_final"), finalFbo);

        manager.runPipeline(pipeline, false);
    }

    @Override
    protected void renderWindowBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, -10, 0x4fffffff);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.subLevel.isRemoved() || this.diagram.isRemoved()) {
            this.onClose();
            return;
        }

        if (this.configDirty) {
            VeilPacketManager.server().sendPacket(new DiagramSaveConfigPacket(this.diagram.getId(), this.config));
            this.configDirty = false;
        }

        if (this.ticksWithoutUpdate++ > UPDATE_REQUEST_INTERVAL) {
            this.ticksWithoutUpdate = 0;
            VeilPacketManager.server().sendPacket(new RequestDiagramDataPacket(this.subLevel.getUniqueId()));
        }

        this.lastPaperOffset = this.paperOffset;
        this.paperOffset = Mth.lerp(PAPER_SLIDE_SPEED, this.paperOffset, this.paperVisible ? MAX_PAPER_OFFSET : MIN_PAPER_OFFSET);
        this.lastTabOffset = this.tabOffset;
        //this.tabOffset = Mth.lerp(TAB_SLIDE_SPEED, this.tabOffset, (this.paperOffset-MIN_PAPER_OFFSET)/(MAX_PAPER_OFFSET-MIN_PAPER_OFFSET));
        this.tabOffset = Mth.lerp(this.paperVisible ? PAPER_SLIDE_SPEED : TAB_SLIDE_SPEED, this.tabOffset, this.paperVisible ? 1 : 0);
        //this.tabOffset = Math.max(this.tabOffset-0.1f,nextTabOffset);

        this.note.tick();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final boolean widgetPress = super.mouseClicked(mouseX, mouseY, button);

        final boolean withinNote = this.note.contains(mouseX, mouseY);
        if (withinNote || (!widgetPress && this.contains(mouseX, mouseY) /*&& this.isMagnifying()*/)) {
//            if (!withinNote) {
//                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SimSoundEvents.DIAGRAM_TAP.event(), 1.0f));
//            }

            MAGNIFYING_CENTER.set(mouseX, mouseY);
        }

        return widgetPress;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        final boolean parent = super.mouseReleased(mouseX, mouseY, button);

        this.updateNote(mouseX, mouseY, parent);

        // no matter what clear start and end positions
        MAGNIFYING_CENTER.set(0, 0);
        MAGNIFYING_MAX.set(0, 0);
        return parent;
    }

    private void updateNote(final double mouseX, final double mouseY, final boolean widgetRelease) {
        if (MAGNIFYING_CENTER.distanceSquared(MAGNIFYING_MAX) < MIN_MAGNIFICATION_PIXELS * MIN_MAGNIFICATION_PIXELS) {
            return;
        }

        this.updateMagnificationBox(mouseX, mouseY);

        if (this.note.contains(MAGNIFYING_CENTER.x, MAGNIFYING_CENTER.y)) {
            if (this.pointsWithinNote(MAGNIFYING_MAX, MAGNIFYING_MIN)) {
                this.note.handleInternalUpdate(MAGNIFYING_MAX, MAGNIFYING_MIN);

                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0f));
            }

            return;
        }

        if (!this.pointsWithinDiagram(MAGNIFYING_MAX, MAGNIFYING_MIN) || widgetRelease /*|| !this.isMagnifying()*/) {
            return;
        }

        final int diagramX = this.width / 2 - DIAGRAM_TEXTURE.width / 2;
        final int diagramY = this.height / 2 - DIAGRAM_TEXTURE.height / 2;

        this.config.getNoteConfigs().setNoteYaw(this.config.yaw());
        this.config.getNoteConfigs().setNotePitch(this.config.pitch());
        this.config.getNoteConfigs().setActive(true);

        this.note.updateCurrentScope(MAGNIFYING_MAX.sub(diagramX, diagramY, new Vector2d()), MAGNIFYING_MIN.sub(diagramX, diagramY, new Vector2d()), LOCAL_CAMERA_POSITION, PROJECTION_MAT);
        this.note.activate();

        this.setConfigDirty();
        this.magnifying = false;

        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0f));
    }

    /**
     * Updates the magnification box min/max from the given mouse x/y.
     */
    private void updateMagnificationBox(final double mouseX, final double mouseY) {
        MAGNIFYING_MAX.set(mouseX, mouseY);

        MAGNIFYING_MAX.sub(MAGNIFYING_CENTER);
        MAGNIFYING_MAX.absolute();

        final double max = Math.max(MAGNIFYING_MAX.x, MAGNIFYING_MAX.y);
        MAGNIFYING_MAX.set(max, max);

        MAGNIFYING_MAX.negate(MAGNIFYING_MIN).add(MAGNIFYING_CENTER);
        MAGNIFYING_MAX.add(MAGNIFYING_CENTER);
    }

    @Override
    protected void renderWindow(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final PoseStack ps = graphics.pose();

        if (this.subLevel.isRemoved() || this.diagram.isRemoved()) {
            this.onClose();
            return;
        }

        this.note.renderWidget(graphics, mouseX, mouseY, partialTicks);
        this.renderContents(this.subLevel, partialTicks);

        // genuinely how can this be null they are assigned values in the constructor
        if (this.turnDownButton != null && this.turnUpButton != null) {
            this.turnDownButton.visible = this.turnDownButton.active = this.config.pitch() < 45.0f;
            this.turnUpButton.visible = this.turnUpButton.active = this.config.pitch() > -45.0f;
        }

        ps.pushPose();

        for (final DiagramForceGroupToggle widget : this.forceToggleWidgets) {
            widget.active = this.paperVisible;
            widget.updateForceState(this.serverData);
            widget.renderTab(graphics, mouseX, mouseY, partialTicks);
        }

        final int diagramX = this.width / 2 - DIAGRAM_TEXTURE.width / 2;
        final int diagramY = this.height / 2 - DIAGRAM_TEXTURE.height / 2;

        // Render config paper
        ps.pushPose();
        ps.translate(diagramX, diagramY, 0);
        ps.translate(-this.getPaperOffset(partialTicks), 0, 0.0f);
        SimGUITextures.DIAGRAM_PAPER.render(graphics, 0, 0);
        ps.popPose();

        for (final DiagramForceGroupToggle widget : this.forceToggleWidgets) {
            widget.render(graphics, mouseX, mouseY, partialTicks);
        }

        // Render diagram
        ps.translate(diagramX, diagramY, 0);

        // Main background
        DIAGRAM_TEXTURE.render(graphics, 0, 0);

        renderFBO(graphics, this.finalFbo, DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height);

        final String text = this.subLevel.getName();

        ps.pushPose();
        ps.translate(0, 0, 1);
        if (text != null && !text.isEmpty()) {
            final int footerW = this.font.width(text);
            graphics.fill(DIAGRAM_TEXTURE.width - footerW - 7, DIAGRAM_TEXTURE.height - 5 - this.font.lineHeight, DIAGRAM_TEXTURE.width - 4, DIAGRAM_TEXTURE.height - 3, BG_COLOR.getRGB());
            graphics.drawString(this.font, text, DIAGRAM_TEXTURE.width - footerW - 5, DIAGRAM_TEXTURE.height - 3 - this.font.lineHeight, TEXT_COLOR.getRGB(), false);
        }
        ps.popPose();

        this.renderArrows(graphics,
                mouseX,
                mouseY,
                diagramX,
                diagramY,
                LOCAL_ORIENTATION,
                LOCAL_CAMERA_POSITION,
                PROJECTION_MAT,
                DIAGRAM_TEXTURE.width,
                DIAGRAM_TEXTURE.height);

        if (this.config.displayCenterOfMass()) {
            this.renderCenterOfMass(graphics);
        }

        ps.popPose();

    }

    @Override
    protected void renderWindowForeground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final PoseStack ps = graphics.pose();

        this.renderMagnificationHighlight(graphics, mouseX, mouseY, ps);

        if (!this.tooltipList.isEmpty()) {
            DiagramScreen.renderTooltip(graphics, mouseX, mouseY, this.tooltipList);
        }

        this.tooltipList.clear();

        super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderMagnificationHighlight(final GuiGraphics graphics, final int mouseX, final int mouseY, final PoseStack ps) {
        final boolean initiallyWithinNote = this.note.contains(MAGNIFYING_CENTER.x, MAGNIFYING_CENTER.y);

        this.updateMagnificationBox(mouseX, mouseY);

        if (MAGNIFYING_CENTER.distanceSquared(MAGNIFYING_MAX) < MIN_MAGNIFICATION_PIXELS * MIN_MAGNIFICATION_PIXELS) {
            return;
        }

        if (initiallyWithinNote || (/*this.isMagnifying() && */this.contains(MAGNIFYING_CENTER.x, MAGNIFYING_CENTER.y))) {
            ps.pushPose();
            ps.translate(0, 0, 1);

            final Vector2d min = new Vector2d(MAGNIFYING_MIN);
            final Vector2d max = new Vector2d(MAGNIFYING_MAX);

            final boolean valid = initiallyWithinNote ?
                    (this.note.contains(min.x, min.y) && this.note.contains(max.x, max.y)) :
                    (this.contains(min.x, min.y) && this.contains(max.x, max.y));

            if (initiallyWithinNote) {
                this.note.clamp(min);
                this.note.clamp(max);
            } else {
                this.clamp(min);
                this.clamp(max);
            }

            final double startX = min.x;
            final double startY = min.y;
            final double endX = max.x;
            final double endY = max.y;
            final int fillColor = valid ? 0x40fffcfc : 0x40aaaaaa;
            final int color = valid ? 0x90ffffff : 0x90ffaaaa;

            graphics.fill((int) startX, (int) startY, (int) endX, (int) endY, fillColor);
            graphics.hLine((int) startX, (int) endX, (int) startY, color);
            graphics.hLine((int) startX, (int) endX, (int) endY, color);
            graphics.vLine((int) startX, (int) startY, (int) endY, color);
            graphics.vLine((int) endX, (int) startY, (int) endY, color);

            ps.popPose();
        }
    }

    public boolean pointsWithinNote(final Vector2d target, final Vector2d inverse) {
        return this.note.contains(target.x, target.y) && this.note.contains(inverse.x, inverse.y);
    }

    public boolean pointsWithinDiagram(final Vector2d target, final Vector2d inverse) {
        return this.contains(target.x, target.y) && this.contains(inverse.x, inverse.y);
    }

    public boolean contains(double x, double y) {
        x -= (this.width / 2f - DIAGRAM_TEXTURE.width / 2f);
        y -= (this.height / 2f - DIAGRAM_TEXTURE.height / 2f);

        return x > 0 && x < DIAGRAM_TEXTURE.width && y > 0 && y < DIAGRAM_TEXTURE.height;
    }

    public Vector2d clamp(final Vector2d dest) {
        final float minX = (this.width / 2f - DIAGRAM_TEXTURE.width / 2f);
        final float minY = (this.height / 2f - DIAGRAM_TEXTURE.height / 2f);
        dest.max(new Vector2d(minX, minY));
        dest.min(new Vector2d(minX + DIAGRAM_TEXTURE.width - 1, minY + DIAGRAM_TEXTURE.height - 1));
        return dest;
    }

    public static void renderFBO(final GuiGraphics graphics, final AdvancedFbo fbo, final int width, final int height) {
        final int id = fbo.getColorTextureAttachment(0).getId();

        RenderSystem.setShaderTexture(0, id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        final Matrix4f matrix4f = graphics.pose().last().pose();
        final BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        final float x1 = 0.0f;
        final float y1 = 0.0f;
        bufferbuilder.addVertex(matrix4f, x1, y1, 0.0f).setUv(0.0f, 1.0f).setColor(0xFFFFFFFF);
        bufferbuilder.addVertex(matrix4f, x1, height, 0.0f).setUv(0.0f, 0.0f).setColor(0xFFFFFFFF);
        bufferbuilder.addVertex(matrix4f, width, height, 0.0f).setUv(1.0f, 0.0f).setColor(0xFFFFFFFF);
        bufferbuilder.addVertex(matrix4f, width, y1, 0.0f).setUv(1.0f, 1.0f).setColor(0xFFFFFFFF);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public void renderArrows(final GuiGraphics graphics,
                             final int mouseX,
                             final int mouseY,
                             final int areaOriginX,
                             final int areaOriginY,
                             final Quaternionfc orientation,
                             final Vector3dc cameraPos,
                             final Matrix4fc projMatrix,
                             final int areaWidth,
                             final int areaHeight) {
        if (this.serverData != null) {
            // Record max arrow length
            double maxArrowLengthSquared = 0.0;

            final Map<ForceGroup, List<ForceClusterFinder.Cluster>> clusters = new HashMap<>();

            for (final ResourceLocation groupId : this.config.enabledForceGroups()) {
                final ForceGroup group = ForceGroups.REGISTRY.get(groupId);
                assert group != null;

                final List<QueuedForceGroup.PointForce> forces = this.serverData.forces().get(group);

                if (forces == null) continue;
                final List<ForceClusterFinder.Cluster> cluster = this.config.mergeForces() ? ForceClusterFinder.getMergedClusters(forces) : ForceClusterFinder.passThrough(forces);

                clusters.put(group, cluster);

                for (final ForceClusterFinder.Cluster force : cluster) {
                    maxArrowLengthSquared = Math.max(maxArrowLengthSquared, force.force().lengthSquared());
                }
            }

            for (final ResourceLocation groupId : this.config.enabledForceGroups()) {
                final ForceGroup group = ForceGroups.REGISTRY.get(groupId);
                assert group != null;

                final List<ForceClusterFinder.Cluster> cluster = clusters.get(group);

                if (cluster == null) continue;

                for (final ForceClusterFinder.Cluster force : cluster) {
                    this.renderForceArrow(graphics,
                            group,
                            force,
                            Math.sqrt(maxArrowLengthSquared),
                            mouseX - areaOriginX,
                            mouseY - areaOriginY,
                            this.tooltipList,
                            orientation,
                            cameraPos,
                            projMatrix,
                            areaWidth,
                            areaHeight);
                }
            }
        }
    }

    /**
     * Renders a force arrow for a given point force and force group
     */
    private void renderForceArrow(final GuiGraphics graphics,
                                  final ForceGroup forceGroup,
                                  final ForceClusterFinder.Cluster pointForce,
                                  final double maxArrowLength,
                                  final int mouseX,
                                  final int mouseY,
                                  final List<FormattedText> tooltipLines,
                                  final Quaternionfc orientation,
                                  final Vector3dc cameraPos,
                                  final Matrix4fc projMatrix,
                                  final int areaWidth,
                                  final int areaHeight) {
        final double forceMagnitude = pointForce.force().length();

        if (forceMagnitude <= 0.01 || this.viewportRadius == 0.0) {
            return;
        }

        final Vector3d globalFirstDir = pointForce.force().normalize(new Vector3d());
        final Vector3d forceOffset = globalFirstDir.mul(Math.max(0.25, forceMagnitude / maxArrowLength) * this.viewportRadius * 0.5, new Vector3d());

        final Vector2d originCoords = getScreenCoords(new Vector3d(pointForce.pos()), orientation, cameraPos, projMatrix, areaWidth, areaHeight);
        if (!this.canDrawArrowAt((int) originCoords.x, (int) originCoords.y, areaWidth, areaHeight)) {
            return;
        }

        final Vector2d mousePos = new Vector2d(mouseX, mouseY);

        final int color = (255 << 24) | forceGroup.color();
        final int shadowColor = 0xfff9f2de;

        final double facingDot = orientation.transformInverse(globalFirstDir, new Vector3d()).dot(OrientedBoundingBox3d.FORWARD);

        if (Math.abs(facingDot) > 0.85) {
            final PoseStack ps = graphics.pose();
            ps.pushPose();

            ps.translate(0.0, 0.0, 1.0f);

            // tooltip time!
            if (mousePos.sub(originCoords, new Vector2d()).lengthSquared() < 8.0 * 8.0) {
                addForceArrowTooltip(forceGroup, pointForce.groupSize().getValue(), forceMagnitude, color, tooltipLines);
            }

            if (facingDot < 0.0) {
                SimGUITextures.DIAGRAM_ICON_ARROW_IN_PAGE_SHADOW.render(graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, new Color(shadowColor));
                SimGUITextures.DIAGRAM_ICON_ARROW_IN_PAGE.render(graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, new Color(color));
            } else {
                SimGUITextures.DIAGRAM_ICON_ARROW_OUT_PAGE_SHADOW.render(graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, new Color(shadowColor));
                SimGUITextures.DIAGRAM_ICON_ARROW_OUT_PAGE.render(graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, new Color(color));
            }

            ps.popPose();
            return;
        }

        final Vector2d resultCoords = getScreenCoords(pointForce.pos().add(forceOffset, new Vector3d()), orientation, cameraPos, projMatrix, areaWidth, areaHeight);

        final Vector2d arrowDir = resultCoords.sub(originCoords, new Vector2d());
        float arrowLength = (float) arrowDir.length();
        arrowDir.div(arrowLength);

        while (arrowLength > 0 && !this.canDrawArrowAt((int) resultCoords.x, (int) resultCoords.y, areaWidth, areaHeight)) {
            resultCoords.fma(-3.0, arrowDir);
            arrowLength -= 3.0f;
        }

        final int x1 = (int) originCoords.x();
        final int y1 = (int) originCoords.y();

        final int x2 = (int) resultCoords.x();
        final int y2 = (int) resultCoords.y();

        final MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        final VertexConsumer builder = bufferSource.getBuffer(RenderType.gui());
        final Matrix4f pose = graphics.pose().last().pose();

        final Vector2d arrowLeft = new Vector2d(-arrowDir.y(), arrowDir.x()).mul(4.0);
        final Vector2d arrowRight = new Vector2d(arrowDir.y(), -arrowDir.x()).mul(4.0);

        final float headLen = 6.0f;

        final boolean drawArrow = originCoords.distanceSquared(resultCoords) > MIN_ARROW_SIZE_PX * MIN_ARROW_SIZE_PX;

        double distanceAlongLine = mousePos.sub(originCoords, new Vector2d()).dot(arrowDir);
        distanceAlongLine = Mth.clamp(distanceAlongLine, 0.0, arrowLength);

        final boolean displayTooltip = new Vector2d(originCoords).fma(distanceAlongLine, arrowDir).distance(mousePos) < 5.0;
        if (displayTooltip) {
            // tooltip time!
            addForceArrowTooltip(forceGroup, pointForce.groupSize().getValue(), forceMagnitude, color, tooltipLines);
        }

        // Draw base dot
        final int z = 1;
        int inflation = 3;
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 - inflation, (float) z).setColor(shadowColor);
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 + 1 + inflation, (float) z).setColor(shadowColor);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 + 1 + inflation, (float) z).setColor(shadowColor);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 - inflation, (float) z).setColor(shadowColor);

        if (drawArrow) {
            // Arrow shadow
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowLeft.x), (int) (y2 - arrowDir.y * headLen + arrowLeft.y), shadowColor, 1);
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowRight.x), (int) (y2 - arrowDir.y * headLen + arrowRight.y), shadowColor, 1);
            drawLine(builder, pose, x1, y1, x2, y2, shadowColor, 1);

            // Actual arrow
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowLeft.x), (int) (y2 - arrowDir.y * headLen + arrowLeft.y), color, 0);
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowRight.x), (int) (y2 - arrowDir.y * headLen + arrowRight.y), color, 0);
            drawLine(builder, pose, x1, y1, x2, y2, color, 0);
        }

        inflation = 2;
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 - inflation, (float) z).setColor(color);
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 - inflation, (float) z).setColor(color);
    }

    private static void addForceArrowTooltip(final ForceGroup forceGroup, final int forceCount, final double forceMagnitude, final int color, final List<FormattedText> tooltipLines) {
        final LangBuilder forceNameText = SimLang.builder().add(forceGroup.name()).color(color);
        final LangBuilder forceMagnitudeText = SimLang.translate("contraption_diagram.force_arrow_magnitude", String.format("%,.2f", forceMagnitude)).color(0xffffffff);

        if (forceCount > 1)
            tooltipLines.add(SimLang.translate("contraption_diagram.merged_force_arrow", SimLang.translate("contraption_diagram.merging_numeral", Integer.toString(forceCount)).color(0xffffffff), forceNameText, forceMagnitudeText).color(TOOLTIP_LABEL_COLOR).component());
        else
            tooltipLines.add(SimLang.translate("contraption_diagram.force_arrow", forceNameText, forceMagnitudeText).color(TOOLTIP_LABEL_COLOR).component());
    }

    private boolean canDrawArrowAt(final int x, final int y, final int width, final int height) {
        final int padding = 8;
        return x >= padding && x < width - padding && y >= padding && y < height - padding;
    }

    private static void drawLine(final VertexConsumer builder, final Matrix4f pose, int x1, int y1, final int x2, final int y2, final int color, final int inflation) {
        // don't miss none of them pixels! you heard me!
        final int z = 1;
        final int dx = Math.abs(x2 - x1);
        final int dy = Math.abs(y2 - y1);
        final int sx = x1 < x2 ? 1 : -1;
        final int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            builder.addVertex(pose, (float) x1 - inflation, (float) y1 - inflation, (float) z).setColor(color);
            builder.addVertex(pose, (float) x1 - inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
            builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
            builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 - inflation, (float) z).setColor(color);

            if (x1 == x2 && y1 == y2) break;

            final int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private SimGUITextures getMergeIcon() {
        return this.config.mergeForces() ? SimGUITextures.DIAGRAM_ICON_FORCES_MERGED : SimGUITextures.DIAGRAM_ICON_FORCES_SEPARATED;
    }

    public float getPaperOffset(final float partialTicks) {
        return Mth.lerp(partialTicks, this.lastPaperOffset, this.paperOffset);
    }

    public float getTabOffset(final float partialTicks) {
        return Mth.lerp(partialTicks, this.lastTabOffset, this.tabOffset);
    }

    private void renderCenterOfMass(final GuiGraphics graphics) {
        final Vector3d centerOfMass = new Vector3d(this.subLevel.logicalPose().rotationPoint());
        final Vector2d screenCoords = getScreenCoords(centerOfMass, LOCAL_ORIENTATION, LOCAL_CAMERA_POSITION, PROJECTION_MAT, DIAGRAM_TEXTURE.width, DIAGRAM_TEXTURE.height);

        final SimGUITextures tex = SimGUITextures.DIAGRAM_ICON_COM;

        final PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(screenCoords.x - 8, screenCoords.y - 8, 0);
        graphics.blit(tex.location, 0, 0, 5, tex.startX, tex.startY, tex.width, tex.height, tex.texWidth, tex.texHeight);
        pose.popPose();
    }

    /**
     * Projects a 3D point into pixel coordinates
     *
     * @param plotSpacePoint the point to project
     * @param localPosition  the position used to project
     * @param projMatrix     the projection matrix
     * @return pixel coordinates in diagram-space
     */
    public static Vector2d getScreenCoords(final Vector3d plotSpacePoint, final Quaternionfc orientation, final Vector3dc localPosition, final Matrix4fc projMatrix, final int width, final int height) {
        plotSpacePoint.sub(localPosition);
        orientation.transformInverse(plotSpacePoint);

        final Vector4f clipSpace = new Vector4f((float) plotSpacePoint.x, (float) plotSpacePoint.y, (float) plotSpacePoint.z, 1.0f);
        clipSpace.mul(projMatrix);
        clipSpace.div(clipSpace.w);

        final double projectedX = ((clipSpace.x() * 0.5f + 0.5f) * width);
        final double projectedY = ((-clipSpace.y() * 0.5f + 0.5f) * height);
        return new Vector2d(projectedX, projectedY);
    }

    /**
     * Projects a diagram-space coordinate into plot-space
     *
     * @param diagramSpacePoint the point to project
     * @param localPosition     the position used to project
     * @param projMatrix        the projection matrix
     * @return 3D point in plot-space
     */
    public static Vector3d getPlotCoords(final Vector2dc diagramSpacePoint, final Quaternionfc orientation, final Vector3dc localPosition, final Matrix4fc projMatrix, final int width, final int height) {
        final Vector3d clipSpace = new Vector3d(2 * diagramSpacePoint.x() / width - 1, 1 - 2 * diagramSpacePoint.y() / height, 0);
        final Vector3d point = clipSpace.sub(projMatrix.getTranslation(new Vector3f())).div(projMatrix.m00(), projMatrix.m11(), projMatrix.m22());

        orientation.transform(point);
        point.add(localPosition);
        return point;
    }

    public void updateData(final DiagramDataPacket data) {
        this.serverData = data;
    }

    public static void renderTooltip(final GuiGraphics guiGraphics, final int x, final int y, final List<FormattedText> lines) {
        final Font font = Minecraft.getInstance().font;

        final Color colorBackground = new Color(0xff3d322a);
        final Color colorBorderTop = new Color(0xff5d483a);
        final Color colorBorderBot = new Color(0xff5d483a);
        RemovedGuiUtils.drawHoveringText(guiGraphics, lines, x, y, guiGraphics.guiWidth(), guiGraphics.guiHeight(), -1, colorBackground.getRGB(), colorBorderTop.getRGB(), colorBorderBot.getRGB(), font);
    }

    public void setConfigDirty() {
        this.configDirty = true;
    }

//    public boolean isMagnifying() {
//        return true;
//    }

    public record GreebleRenderable(int x, int y, int width, int height, ResourceLocation texture,
                                    Greeble.TextureSlice slice) implements Renderable {
        @Override
        public void render(final GuiGraphics guiGraphics, final int i, final int i1, final float v) {
            guiGraphics.blit(this.texture, this.x, this.y, this.slice.x(), this.slice.y(), this.slice.width(), this.slice.height(), this.width, this.height);
        }
    }
}
