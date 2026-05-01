package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class DockingConnectorDuoInventory implements AbstractContainer {
    private final DockingConnectorSoloInventory ourInventory;
    private final DockingConnectorSoloInventory theirInventory;

    public DockingConnectorDuoInventory(final DockingConnectorBlockEntity ourConnector, final DockingConnectorBlockEntity theirConnector) {
        this.ourInventory = ourConnector.inventory;
        this.theirInventory = theirConnector.inventory;
    }

    @Override
    public int insertGeneral(final ItemInfoWrapper info, final int amountToInsert, final boolean simulate) {
        return this.theirInventory.insertGeneral(info, amountToInsert, simulate);
    }

    @Override
    public ItemStack insertSlot(final ItemStack stack, final int slot, final boolean simulate) {
        if (slot == 0) {
            return this.theirInventory.insertSlot(stack, 0, simulate);
        }
        return stack;
    }

    @Override
    public int extractGeneral(final ItemInfoWrapper info, final int amountToExtract, final boolean simulate) {
        return this.ourInventory.extractGeneral(info, amountToExtract, simulate);
    }

    @Override
    public ItemStack extractSlot(final int index, final int amountToExtract, final boolean simulate) {
        if (index == 1) {
            return this.ourInventory.extractSlot(0, amountToExtract, simulate);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public @NotNull ItemStack getItem(final int slot) {
        return switch (slot) {
            case 0 -> this.theirInventory.getItem(0);
            case 1 -> this.ourInventory.getItem(0);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItem(final int slot, @NotNull final ItemStack stack) {
        switch (slot) {
            case 0 -> this.theirInventory.setItem(0, stack);
            case 1 -> this.ourInventory.setItem(0, stack);
        }
    }

    @Override
    public void clearContent() {

    }

    @Override
    public void setChanged() {
        this.ourInventory.setChanged();
        this.theirInventory.setChanged();
    }

    @Override
    public CompoundTag write(final HolderLookup.Provider provider) {
        throw new NotImplementedException();
    }

    @Override
    public void read(final HolderLookup.Provider provider, final CompoundTag nbt) {
        throw new NotImplementedException();
    }

    // these methods are icky and are prone to give incorrect answers bleh
    @Override
    public boolean isEmpty() {
        return this.ourInventory.isEmpty() && this.theirInventory.isEmpty();
    }

    @Override
    public List<ContainerSlot> getInventoryAsList() {
        return List.of(this.ourInventory.slot, this.theirInventory.slot);
    }

    @Override
    public Set<ContainerSlot> getPopulatedSlots() {
        if (this.theirInventory.isEmpty()) {
            return Set.of();
        } else {
            return Set.of(this.ourInventory.slot);
        }
    }
}
