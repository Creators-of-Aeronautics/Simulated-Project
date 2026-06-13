package dev.simulated_team.simulated.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface SimInventoryService {

	SimInventoryService INSTANCE = ServiceUtil.load(SimInventoryService.class);

	<T extends InventoryLoaderWrapper> T getInventory(@Nullable final BlockEntity be, @Nullable final Direction dir);

	//I don't think there's an easier way to do this...
	<T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(final MountedStorageManager manager);
	<T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(final MountedStorageManager manager);

	<T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerInventory(final BiFunction<T, Direction, AbstractContainer> getter);

	<T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerTank(final BiFunction<T, Direction, SingleTank> getter);

	<T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerBattery(final BiFunction<T, Direction, SingleBattery> getter);
}
