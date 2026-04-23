package dev.simulated_team.simulated.neoforge.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NeoForgeSimPeripheralService implements SimPeripheralService {

    private static final List<Entry<BlockEntity, IPeripheral>> PERIPHERALS = new ArrayList<>();
    private static final List<Entry<BlockEntity, WiredElement>> WIRED_ELEMENTS = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> void addPeripheral(final Supplier<BlockEntityType<T>> typeSupplier, final CapabilityGetter<T, IPeripheral> getter) {
        PERIPHERALS.add((Entry<BlockEntity, IPeripheral>) new Entry<>(typeSupplier, getter));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> typeSupplier, CapabilityGetter<T, WiredElement> getter) {
        WIRED_ELEMENTS.add((Entry<BlockEntity, WiredElement>) new Entry<>(typeSupplier, getter));
    }

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        for (final Entry<BlockEntity, IPeripheral> entry : PERIPHERALS) {
            event.registerBlockEntity(PeripheralCapability.get(), entry.typeSupplier.get(), (be, direction) ->
                    entry.peripheralFunction().get(be, direction)
            );
        }

        for (final Entry<BlockEntity, WiredElement> entry : WIRED_ELEMENTS) {
            event.registerBlockEntity(WiredElementCapability.get(), entry.typeSupplier.get(), (be, direction) ->
                    entry.peripheralFunction().get(be, direction)
            );
        }
    }

    private record Entry<T extends BlockEntity, V>(Supplier<BlockEntityType<T>> typeSupplier,
                                                   CapabilityGetter<T, V> peripheralFunction) {
    }
}
