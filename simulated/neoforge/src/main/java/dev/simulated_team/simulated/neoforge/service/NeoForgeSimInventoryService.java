package dev.simulated_team.simulated.neoforge.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.neoforge.InventoryLoaderWrapperImpl;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class NeoForgeSimInventoryService implements SimInventoryService {

    public static Set<InventoryGetterHolder<? extends BlockEntity>> inventoryGetters = new HashSet<>();
    public static Set<TankGetterHolder<? extends BlockEntity>> fluidTankGetters = new HashSet<>();
    public static Set<EnergyGetterHolder<? extends BlockEntity>> energyGetters = new HashSet<>();

    public static HashMap<BlockEntityType<BlockEntity>, Function<BlockEntity, SingleTank>> tankGetters = new HashMap<>();

    @Override
    public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerInventory(final BiFunction<T, Direction, AbstractContainer> getter) {
        return (type) -> inventoryGetters.add(new InventoryGetterHolder<>(getter, type));
    }

    @Override
    public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerTank(final BiFunction<T, Direction, SingleTank> getter) {
        return (type) -> fluidTankGetters.add(new TankGetterHolder<>(getter, type));
    }

    @Override
    public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerBattery(final BiFunction<T, Direction, SingleBattery> getter) {
        return (type) -> energyGetters.add(new EnergyGetterHolder<>(getter, type));
    }

    @Override
    public <T extends InventoryLoaderWrapper> T getInventory(@Nullable final BlockEntity be, @Nullable final Direction dir) {
        if (be != null) {
            final IItemHandler handler = be.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), dir);
            if (handler != null) {
                return (T) new InventoryLoaderWrapperImpl(handler);
            }
        }

        return null;
    }

    @Override
    public <T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(final MountedStorageManager manager) {
        return (T) new InventoryLoaderWrapperImpl(manager.getAllItems());
    }

    @Override
    public <T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(final MountedStorageManager manager) {
        return (T) new InventoryLoaderWrapperImpl(manager.getMountedItems());
    }


    public record InventoryGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, AbstractContainer> getter, BlockEntityType<T> type) {
        public AbstractContainer castBlockEntityAndGetInv(final BlockEntity be, final Direction dir) {
            final T casted = (T) be;
            return this.getter.apply(casted, dir);
        }
    }

    public record TankGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, SingleTank> getter, BlockEntityType<T> type) {
        public SingleTank castBlockEntityAndGetInv(final BlockEntity be, final Direction dir) {
            final T casted = (T) be;
            return this.getter.apply(casted, dir);
        }
    }

    public record EnergyGetterHolder<T extends BlockEntity>(BiFunction<T, Direction, SingleBattery> getter, BlockEntityType<T> type) {
        public SingleBattery castBlockEntityAndGetInv(final BlockEntity be, final Direction dir) {
            final T casted = (T) be;
            return this.getter.apply(casted, dir);
        }
    }
}
