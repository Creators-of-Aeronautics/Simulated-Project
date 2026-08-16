package dev.simulated_team.simulated.network;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.network.packets.*;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramDataPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramOpenPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramSaveConfigPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.LiftMarkerDataPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.RequestDiagramDataPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.RequestLiftMarkerPacket;
import dev.simulated_team.simulated.network.packets.end_sea.ClientboundEndSeaPacket;
import dev.simulated_team.simulated.network.packets.handle.ClientboundPlayersHoldingHandlePacket;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueChangeBoundsPacket;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSpawnPacket;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSyncBoundsPacket;
import dev.simulated_team.simulated.network.packets.linked_typewriter.*;
import dev.simulated_team.simulated.network.packets.lodestone_compass.UpdateClientLodestonePositionPacket;
import dev.simulated_team.simulated.network.packets.name_plate.NameplateChangeNamePacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFailedPacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFlickAndHoldLeverPacket;
import dev.simulated_team.simulated.network.packets.physics_staff.*;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeStoppedPacket;
import foundry.veil.api.network.VeilPacketManager;

public class SimPacketManager {

    public static final VeilPacketManager INSTANCE = VeilPacketManager.create(Simulated.MOD_ID, "0.1");

    public static void init() {
        INSTANCE.registerServerbound(AssemblePacket.TYPE, AssemblePacket.CODEC, AssemblePacket::handle);
        INSTANCE.registerServerbound(BlockEntityObservedPacket.TYPE, BlockEntityObservedPacket.CODEC, BlockEntityObservedPacket::handle);
        INSTANCE.registerServerbound(ConfigureAltitudeSensorPacket.TYPE, ConfigureAltitudeSensorPacket.CODEC, ConfigureAltitudeSensorPacket::handle);
        INSTANCE.registerServerbound(ConfigureModulatingLinkedRecieverPacket.TYPE, ConfigureModulatingLinkedRecieverPacket.CODEC, ConfigureModulatingLinkedRecieverPacket::handle);
        INSTANCE.registerServerbound(ThrottleLeverSignalPacket.TYPE, ThrottleLeverSignalPacket.CODEC, ThrottleLeverSignalPacket::handle);

        INSTANCE.registerServerbound(NameplateChangeNamePacket.TYPE, NameplateChangeNamePacket.CODEC, NameplateChangeNamePacket::handle);

        INSTANCE.registerServerbound(TypewriterKeyInteractionPacket.TYPE, TypewriterKeyInteractionPacket.CODEC, TypewriterKeyInteractionPacket::handle);
        INSTANCE.registerServerbound(TypewriterKeySavePacket.TYPE, TypewriterKeySavePacket.CODEC, TypewriterKeySavePacket::handle);
        INSTANCE.registerServerbound(TypewriterDisconnectUser.TYPE, TypewriterDisconnectUser.CODEC, TypewriterDisconnectUser::handle);
        INSTANCE.registerServerbound(TypewriterMenuModifySlots.TYPE, TypewriterMenuModifySlots.CODEC, TypewriterMenuModifySlots::handle);
        INSTANCE.registerServerbound(TypewriterSaveKeyToItemPacket.TYPE, TypewriterSaveKeyToItemPacket.CODEC, TypewriterSaveKeyToItemPacket::handle);

        INSTANCE.registerServerbound(SteeringWheelPacket.TYPE, SteeringWheelPacket.CODEC, SteeringWheelPacket::handle);

        INSTANCE.registerServerbound(PlaceSpringPacket.TYPE, PlaceSpringPacket.CODEC, PlaceSpringPacket::handle);
        INSTANCE.registerServerbound(PlaceMergingGluePacket.TYPE, PlaceMergingGluePacket.CODEC, PlaceMergingGluePacket::handle);

        INSTANCE.registerServerbound(UpdatePlayerUsingHandlePacket.TYPE, UpdatePlayerUsingHandlePacket.CODEC, UpdatePlayerUsingHandlePacket::handle);

        INSTANCE.registerServerbound(HoneyGlueSpawnPacket.TYPE, HoneyGlueSpawnPacket.CODEC, HoneyGlueSpawnPacket::handle);
        INSTANCE.registerServerbound(HoneyGlueChangeBoundsPacket.TYPE, HoneyGlueChangeBoundsPacket.CODEC, HoneyGlueChangeBoundsPacket::handle);
        INSTANCE.registerClientbound(HoneyGlueSyncBoundsPacket.TYPE, HoneyGlueSyncBoundsPacket.CODEC, HoneyGlueSyncBoundsPacket::handle);

        INSTANCE.registerServerbound(RopeRidingPacket.TYPE, RopeRidingPacket.CODEC, RopeRidingPacket::handle);
        INSTANCE.registerServerbound(RopeBreakPacket.TYPE, RopeBreakPacket.CODEC, RopeBreakPacket::handle);

        INSTANCE.registerServerbound(PhysicsStaffActionPacket.TYPE, PhysicsStaffActionPacket.CODEC, PhysicsStaffActionPacket::handle);
        INSTANCE.registerServerbound(PhysicsStaffDragPacket.TYPE, PhysicsStaffDragPacket.CODEC, PhysicsStaffDragPacket::handle);
        INSTANCE.registerClientbound(PhysicsStaffBeamPacket.TYPE, PhysicsStaffBeamPacket.CODEC, PhysicsStaffBeamPacket::handle);
        INSTANCE.registerClientbound(PhysicsStaffLocksPacket.TYPE, PhysicsStaffLocksPacket.CODEC, PhysicsStaffLocksPacket::handle);
        INSTANCE.registerClientbound(PhysicsStaffDragSessionsPacket.TYPE, PhysicsStaffDragSessionsPacket.CODEC, PhysicsStaffDragSessionsPacket::handle);

        INSTANCE.registerClientbound(PlungerLauncherShootPacket.TYPE, PlungerLauncherShootPacket.CODEC, PlungerLauncherShootPacket::handle);
        INSTANCE.registerClientbound(PhysicsAssemblerFailedPacket.TYPE, PhysicsAssemblerFailedPacket.CODEC, PhysicsAssemblerFailedPacket::handle);
        INSTANCE.registerClientbound(PhysicsAssemblerFlickAndHoldLeverPacket.TYPE, PhysicsAssemblerFlickAndHoldLeverPacket.CODEC, PhysicsAssemblerFlickAndHoldLeverPacket::handle);
        INSTANCE.registerClientbound(ClientboundPlayersHoldingHandlePacket.TYPE, ClientboundPlayersHoldingHandlePacket.CODEC, ClientboundPlayersHoldingHandlePacket::handle);
        INSTANCE.registerClientbound(ClientboundRopeDataPacket.TYPE, ClientboundRopeDataPacket.CODEC, ClientboundRopeDataPacket::handle);
        INSTANCE.registerClientbound(ClientboundRopeStoppedPacket.TYPE, ClientboundRopeStoppedPacket.CODEC, ClientboundRopeStoppedPacket::handle);

        INSTANCE.registerClientbound(ClientboundEndSeaPacket.TYPE, ClientboundEndSeaPacket.CODEC, ClientboundEndSeaPacket::handle);

        INSTANCE.registerServerbound(RequestDiagramDataPacket.TYPE, RequestDiagramDataPacket.CODEC, RequestDiagramDataPacket::handle);
        INSTANCE.registerClientbound(DiagramDataPacket.TYPE, DiagramDataPacket.CODEC, DiagramDataPacket::handle);
        INSTANCE.registerServerbound(DiagramSaveConfigPacket.TYPE, DiagramSaveConfigPacket.CODEC, DiagramSaveConfigPacket::handle);
        INSTANCE.registerClientbound(DiagramOpenPacket.TYPE, DiagramOpenPacket.CODEC, DiagramOpenPacket::handle);
        INSTANCE.registerServerbound(RequestLiftMarkerPacket.TYPE, RequestLiftMarkerPacket.CODEC, RequestLiftMarkerPacket::handle);
        INSTANCE.registerClientbound(LiftMarkerDataPacket.TYPE, LiftMarkerDataPacket.CODEC, LiftMarkerDataPacket::handle);

		INSTANCE.registerClientbound(UpdateClientLodestonePositionPacket.TYPE, UpdateClientLodestonePositionPacket.STREAM_CODEC, UpdateClientLodestonePositionPacket::handle);
    }
}
