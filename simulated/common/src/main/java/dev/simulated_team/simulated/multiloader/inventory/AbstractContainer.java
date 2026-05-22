package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * An abstract container that inventories can extend from. Used for multiloader compatibility through wrappers that each loader creates. <p>
 * Inspired by {@link net.neoforged.neoforge.items.ItemStackHandler ItemStackHandler} In <b>Porting lib</b> by the <b>fabricators of create</b> team
 */
public interface AbstractContainer extends NBTSerializable, Container {

    /**
     * Common method called for both slot and general insert methods.
     *
     * @param info         Info about the stack to be inserted
     * @param slot         The slot to insert into
     * @param insertAmount The amount to insert
     * @param simulate     Whether this action is simulated
     * @return The amount inserted into this slot
     */
    default int commonInsert(final ItemInfoWrapper info, final ContainerSlot slot, final int insertAmount, final boolean simulate) {
        return slot.insertStack(info, insertAmount, simulate);
    }

    /**
     * Common method called on both slot and general extract methods.
     *
     * @param info          The info about the stack desired to be extracted
     * @param slot          The slot to insert into
     * @param extractAmount The amount to insert
     * @param simulate      Whether this action is simulated
     * @return The amount inserted into this slot
     */
    default int commonExtract(final ItemInfoWrapper info, final ContainerSlot slot, final int extractAmount, final boolean simulate) {
        return slot.extractStack(info, extractAmount, simulate);
    }

    /**
     * Attempts to insert the given Item up to the desired amount into this inventory.
     *
     * @param info           info about the stack to be inserted
     * @param amountToInsert The max amount to insert into this inventory
     * @param simulate       Whether this action is simulated; A replacement for Transactions
     * @return The amount that was inserted into this inventory
     */
    int insertGeneral(ItemInfoWrapper info, int amountToInsert, boolean simulate);

    /**
     * Attempts to insert the given ItemStack into desired slot.
     *
     * @param stack     The stack to insert into this inventory
     * @param slotIndex The slot to insert this stack into
     * @param simulate  Whether this action is simulated
     * @return The passed ItemStack or EMPTY if insertion was not successful, or a copy of the ItemStack with a shrunken count
     */
    ItemStack insertSlot(ItemStack stack, int slotIndex, boolean simulate);

    /**
     * Attempts to extract the given Item up to the maxAmount from this inventory.
     *
     * @param info            The stack to insert into this inventory, Has NO count
     * @param amountToExtract The max amount to insert into this inventory
     * @param simulate        Whether this action is simulated; A replacement for Transactions
     * @return The amount that was inserted into this inventory
     */
    int extractGeneral(ItemInfoWrapper info, int amountToExtract, boolean simulate);

    /**
     * Attempts to extract an item from the given index, up to the given amount
     *
     * @param slotIndex       The index to extract from
     * @param amountToExtract The max amount to extract from the given index
     * @param simulate        Whether this action is simulated
     * @return The item extracted
     */
    ItemStack extractSlot(int slotIndex, int amountToExtract, boolean simulate);

    /**
     * Checks to see if the incoming item can be inserted into the desired slot of this container
     *
     * @param info The stack to insert
     * @param slot The desired container slot
     * @return Whether to attempt to insert this item
     */
    default boolean canInsertItem(final ItemInfoWrapper info, final ContainerSlot slot) {
        return true;
    }

    /**
     * Checks to see if the desired slot can be extracted from
     *
     * @param slot The desired container slot
     * @return Whether to attempt to extract from this slot
     */
    default boolean canExtractFromSlot(final ContainerSlot slot) {
        return true;
    }

    /**
     * Called from ContainerSlot when instantiated.
     *
     * @param containerSlot The slot that has been instantiated
     */
    default void populateFields(final ContainerSlot containerSlot) {
    }

    /**
     * Called when changing the count or item in the given slot.
     *
     * @param slot         The slot that is changing
     * @param oldSlotStack The old stack associated with that slot
     * @param newSlotStack The new stack taking over that slot
     */
    default void onStackItemChange(final ContainerSlot slot, final ItemStack oldSlotStack, final ItemStack newSlotStack) {
    }

    /**
     * Remove the desired amount of items from the given slot
     *
     * @param slot   The slot to remove items from
     * @param amount The amount of items to remove
     * @return An item stack with UP-TO the given amount of items in it
     */
    @Override
    default @NotNull ItemStack removeItem(final int slot, final int amount) {
        final ItemStack item = this.getItem(slot);
        return item.split(amount);
    }

    /**
     * Completely removes the given item from the given slot, clearing it
     *
     * @param slot The slot to remove the item from
     * @return The item stack that was in the slot
     */
    @Override
    default @NotNull ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack item = this.getItem(slot);
        this.setItem(slot, ItemStack.EMPTY);
        return item;
    }

    @Override
    default boolean stillValid(final @NotNull Player player) {
        return true;
    }

    @Override
    int getContainerSize();

    @Override
    int getMaxStackSize();

    @Override
    boolean isEmpty();

    @Override
    @NotNull
    ItemStack getItem(int slot);

    @Override
    void setItem(int slot, @NotNull ItemStack stack);

    List<ContainerSlot> getInventoryAsList();

    Set<ContainerSlot> getPopulatedSlots();

    @Override
    void clearContent();

    @Override
    void setChanged();
}
