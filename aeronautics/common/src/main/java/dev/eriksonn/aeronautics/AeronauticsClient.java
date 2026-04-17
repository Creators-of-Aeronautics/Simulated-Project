package dev.eriksonn.aeronautics;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
import dev.eriksonn.aeronautics.content.ponder.AeroPonderPlugin;
import dev.eriksonn.aeronautics.index.AeroClickInteractions;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.eriksonn.aeronautics.index.client.AeroClientRegistries;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import dev.eriksonn.aeronautics.index.client.AeroSituationalMusic;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.createmod.ponder.foundation.PonderIndex;

public class AeronauticsClient {
    public static void init() {
        PonderIndex.addPlugin(new AeroPonderPlugin());

        AeroClientRegistries.init();
        AeroPartialModels.init();
        AeroSituationalMusic.init();
        AeroClickInteractions.init();

        registerEvents();
    }

    private static void registerEvents() {
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage((stage,
                                                           levelRenderer,
                                                           bufferSource,
                                                           matrixStack,
                                                           frustumMatrix,
                                                           projectionMatrix,
                                                           renderTick,
                                                           deltaTracker,
                                                           camera,
                                                           frustum) -> {
            ClientBalloonEffectRenderer.onRenderLevelStage(stage, frustumMatrix, projectionMatrix, renderTick);
        });

        VeilEventPlatform.INSTANCE.onVeilRegisterBlockLayers(registry -> {
            registry.registerBlockLayer(AeroRenderTypes.levitite());
            registry.registerBlockLayer(AeroRenderTypes.levititeGhosts());
        });

        VeilEventPlatform.INSTANCE.onVeilRegisterFixedBuffers(registry -> {
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, AeroRenderTypes.levitite());
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_WEATHER, AeroRenderTypes.levititeGhosts());
        });
    }
}
