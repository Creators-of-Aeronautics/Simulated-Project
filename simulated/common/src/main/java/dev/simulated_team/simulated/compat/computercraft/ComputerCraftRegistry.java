package dev.simulated_team.simulated.compat.computercraft;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.simulated_team.simulated.compat.computercraft.peripherals.*;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

// All CC-touching code lives here. Only class-loaded when the compat service's
// init() runs, which happens only when CC is on the classpath.
public class ComputerCraftRegistry {

    public static void init() {
        final SimPeripheralService service = ServiceUtil.load(SimPeripheralService.class);

        add(service, SimBlockEntityTypes.ALTITUDE_SENSOR, AltitudeSensorPeripheral::new);
        add(service, SimBlockEntityTypes.GIMBAL_SENSOR, GimbalSensorPeripheral::new);
        add(service, SimBlockEntityTypes.NAVIGATION_TABLE, NavTablePeripheral::new);
        add(service, SimBlockEntityTypes.LINKED_TYPEWRITER, LinkedTypewriterPeripheral::new);
        add(service, SimBlockEntityTypes.OPTICAL_SENSOR, OpticalSensorPeripheral::new);
        add(service, SimBlockEntityTypes.SWIVEL_BEARING, SwivelBearingPeripheral::new);

        add(service, SimBlockEntityTypes.VELOCITY_SENSOR, VelocitySensorPeripheral::new);

        add(service, SimBlockEntityTypes.DIRECTIONAL_LINKED_RECEIVER, DirectionalLinkPeripheral::new);
        add(service, SimBlockEntityTypes.MODULATING_LINKED_RECEIVER, ModulatingLinkPeripheral::new);

        add(service, SimBlockEntityTypes.DOCKING_CONNECTOR, DockingConnectorPeripheral::new);
        add(service, SimBlockEntityTypes.TORSION_SPRING, TorsionSpringPeripheral::new);
        add(service, SimBlockEntityTypes.NAMEPLATE, NamePlatePeripheral::new);

        service.addWired(SimBlockEntityTypes.DOCKING_CONNECTOR, (blockEntity, direction) -> {
            if (blockEntity.getBlockState().getValue(DockingConnectorBlock.FACING) == direction) {
                return null;
            }

            return (WiredElement) blockEntity.ccWiredElement;
        });
    }

    private static <T extends BlockEntity> void add(final SimPeripheralService service, final Supplier<BlockEntityType<T>> supplier, final SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> peripheralGetter) {
        service.addPeripheral(supplier, peripheralGetter);
    }
}
