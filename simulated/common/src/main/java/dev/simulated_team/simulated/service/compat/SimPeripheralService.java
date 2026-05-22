package dev.simulated_team.simulated.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface SimPeripheralService {

    <T extends BlockEntity> void addPeripheral(Supplier<BlockEntityType<T>> typeSupplier, CapabilityGetter<T, IPeripheral> getter);

    default <T extends BlockEntity> void addPeripheral(Supplier<BlockEntityType<T>> typeSupplier, SimpleCapabilityGetter<T, IPeripheral> getter) {
        this.addPeripheral(typeSupplier, (CapabilityGetter<T, IPeripheral>) getter);
    }

    <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> typeSupplier, CapabilityGetter<T, WiredElement> getter);

    default <T extends BlockEntity> void addWired(Supplier<BlockEntityType<T>> typeSupplier, SimpleCapabilityGetter<T, WiredElement> getter) {
        this.addWired(typeSupplier, (CapabilityGetter<T, WiredElement>) getter);
    }

    @FunctionalInterface
    interface CapabilityGetter<T extends BlockEntity, V> {

        @Nullable
        V get(T blockEntity, Direction direction);
    }

    @FunctionalInterface
    interface SimpleCapabilityGetter<T extends BlockEntity, V> extends CapabilityGetter<T, V> {

        @Nullable
        V get(T blockEntity);

        @Override
        default V get(T blockEntity, Direction direction) {
            return this.get(blockEntity);
        }
    }
}
