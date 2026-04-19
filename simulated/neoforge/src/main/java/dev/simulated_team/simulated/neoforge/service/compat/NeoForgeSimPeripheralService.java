package dev.simulated_team.simulated.neoforge.service.compat;

import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.simulated_team.simulated.compat.computercraft.wired.DockingConnectorWiredElement;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoForgeSimPeripheralService implements SimPeripheralService {

	private static final List<Peripheral<BlockEntity>> PERIPHERALS = new ArrayList<>();

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> void addPeripheral(final Supplier<BlockEntityType<T>> typeSupplier, final Function<T, IPeripheral> peripheralFunction) {
		PERIPHERALS.add((Peripheral<BlockEntity>) new Peripheral<>(typeSupplier, peripheralFunction));
	}

	@SubscribeEvent
	public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
		for (final Peripheral<BlockEntity> peripheral : PERIPHERALS) {
			event.registerBlockEntity(PeripheralCapability.get(), peripheral.typeSupplier.get(), (be, direction) ->
				peripheral.peripheralFunction().apply(be)
			);
		}

		event.registerBlockEntity(WiredElementCapability.get(), SimBlockEntityTypes.DOCKING_CONNECTOR.get(), (e, d) -> {
			if (e.getBlockState().getValue(DockingConnectorBlock.FACING) == d)
				return null;
			return (DockingConnectorWiredElement) e.ccWiredElement;
		});
	}

	private record Peripheral<T extends BlockEntity>(Supplier<BlockEntityType<T>> typeSupplier, Function<T, IPeripheral> peripheralFunction) {

	}
}
