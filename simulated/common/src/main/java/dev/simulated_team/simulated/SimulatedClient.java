package dev.simulated_team.simulated;

import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelRenderer;
import dev.simulated_team.simulated.content.end_sea.EndSeaFadeTransformer;
import dev.simulated_team.simulated.content.end_sea.EndSeaShadowRenderer;
import dev.simulated_team.simulated.content.items.merging_glue.MergingGlueItemHandler;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.index.ponder.SimPonderPlugin;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.createmod.ponder.foundation.PonderIndex;

public class SimulatedClient {

    public static final PhysicsStaffClientHandler PHYSICS_STAFF_CLIENT_HANDLER = new PhysicsStaffClientHandler();
    public static PlungerLauncherItemRenderer.RenderHandler PLUNGER_LAUNCHER_RENDER_HANDLER = new PlungerLauncherItemRenderer.RenderHandler();
	public static final MergingGlueItemHandler MERGING_GLUE_ITEM_HANDLER = new MergingGlueItemHandler();

    public static void init() {
        SimPartialModels.init();
        BlockPropertiesTooltip.init();
        SimResourceManagers.init();
        registerRadialWrenchMenuBlacklist();

        PonderIndex.addPlugin(new SimPonderPlugin());

        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(EndSeaShadowRenderer::renderShadowMap);

        VeilEventPlatform.INSTANCE.onVeilAddShaderProcessors((provider, registry) -> {
            registry.addPreprocessor(new EndSeaFadeTransformer(), false);
        });
        VeilEventPlatform.INSTANCE.onVeilRegisterFixedBuffers(registry -> {
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES, SimRenderTypes.laser());
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, SimRenderTypes.lens());
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_LEVEL, SimRenderTypes.staffOverlay());
        });

        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(SimulatedCommonClientEvents::onRenderLevelStage);

        SuperByteBufferCache.getInstance().registerCompartment(SteeringWheelRenderer.STEERING_WHEEL);
    }

    private static void registerRadialWrenchMenuBlacklist() {
        RadialWrenchMenu.registerBlacklistedBlock(SimBlocks.SWIVEL_BEARING.getId());
        RadialWrenchMenu.registerBlacklistedBlock(SimBlocks.SWIVEL_BEARING_LINK_BLOCK.getId());
        RadialWrenchMenu.registerBlacklistedBlock(SimBlocks.PAIRED_DOCKING_CONNECTOR.getId());
    }
}
