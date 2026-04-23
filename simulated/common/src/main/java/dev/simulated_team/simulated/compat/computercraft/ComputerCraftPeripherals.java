package dev.simulated_team.simulated.compat.computercraft;

import dan200.computercraft.api.network.wired.WiredElement;
import dev.simulated_team.simulated.compat.computercraft.peripherals.*;
import dev.simulated_team.simulated.compat.computercraft.wired.DockingConnectorWiredElementImpl;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;

public class ComputerCraftPeripherals implements SimModCompatibilityService {

    @Override
    public void init() {
        final SimPeripheralService service = ServiceUtil.load(SimPeripheralService.class);

        service.addPeripheral(SimBlockEntityTypes.ALTITUDE_SENSOR, AltitudeSensorPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.GIMBAL_SENSOR, GimbalSensorPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.NAVIGATION_TABLE, NavTablePeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.LINKED_TYPEWRITER, LinkedTypewriterPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.OPTICAL_SENSOR, OpticalSensorPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.SWIVEL_BEARING, SwivelBearingPeripheral::new);

        service.addPeripheral(SimBlockEntityTypes.VELOCITY_SENSOR, VelocitySensorPeripheral::new);

        service.addPeripheral(SimBlockEntityTypes.DIRECTIONAL_LINKED_RECEIVER, DirectionalLinkPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.MODULATING_LINKED_RECEIVER, ModulatingLinkPeripheral::new);

        service.addPeripheral(SimBlockEntityTypes.DOCKING_CONNECTOR, DockingConnectorPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.TORSION_SPRING, TorsionSpringPeripheral::new);
        service.addPeripheral(SimBlockEntityTypes.NAMEPLATE, NamePlatePeripheral::new);

        service.addWired(SimBlockEntityTypes.DOCKING_CONNECTOR, (blockEntity, direction) -> {
            if (blockEntity.getBlockState().getValue(DockingConnectorBlock.FACING) == direction) {
                return null;
            }

            return (WiredElement) blockEntity.ccWiredElement;
        });
    }

    @Override
    public String getModId() {
        return "computercraft";
    }
}
