package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public abstract class MultiSlotContainer implements AbstractContainer {


    /**
     * The inventory associated with this inventory. Will always have slots with Item stacks associated with them inside.
     */
    private final List<ContainerSlot> inventory;

    /**
     * A non-EMPTY view of this inventory. Sorted with ascending Indices.
     */
    private final Set<ContainerSlot> populatedSlots;

    /**
     * A Filtered view of this inventory
     */
    private final Map<Item, Set<ContainerSlot>> filteredSlots;

    public int maxStackSize;
    public int storedItemCount;

    public MultiSlotContainer(final int size) {
        this(size, 64);
    }

    public MultiSlotContainer(final int size, final int maxStackSize) {
        this.maxStackSize = maxStackSize;

        this.inventory = new ArrayList<>(size);
        this.populatedSlots = new HashSet<>();
        this.filteredSlots = new HashMap<>();

        //populate all item stacks and slots
        for (int i = 0; i < size; i++) {
            this.inventory.add(i, ContainerSlot.of(i, ItemStack.EMPTY, this));
        }

        this.maxStackSize = maxStackSize;
    }

    @Override
    public int insertGeneral(final ItemInfoWrapper info, final int amountToInsert, final boolean simulate) {
        int inserted = 0;

        for (final ContainerSlot slot : this.getInsertableSlotsFor(info.type(), true)) {
            inserted += this.commonInsert(info, slot, amountToInsert - inserted, simulate);
            if (inserted >= amountToInsert) {
                break;
            }
        }

        return inserted;
    }

    @Override
    public ItemStack insertSlot(final ItemStack stack, final int slot, final boolean simulate) {
        final int amountInserted = this.commonInsert(ItemInfoWrapper.generateFromStack(stack), this.inventory.get(slot), stack.getCount(), simulate);

        if (amountInserted > 0) {
            final ItemStack copyIncoming = stack.copy();
            copyIncoming.shrink(amountInserted);

            return copyIncoming;
        } else {
            return stack;
        }
    }

    @Override
    public int extractGeneral(final ItemInfoWrapper info, final int amountToExtract, final boolean simulate) {
        final Set<ContainerSlot> populatedSlots = this.getFilteredSlots(info.type());
        if (populatedSlots.isEmpty())
            return 0; // no slots hold this item

        int extracted = 0;
        for (final ContainerSlot slot : populatedSlots) {
            extracted += this.commonExtract(info, slot, amountToExtract - extracted, simulate);
            if (extracted >= amountToExtract) {
                break;
            }
        }

        return extracted;
    }

    @Override
    public ItemStack extractSlot(final int index, final int amountToExtract, final boolean simulate) {
        final ContainerSlot slot = this.getSlot(index);
        if (slot.isEmpty())
            return ItemStack.EMPTY;

        final ItemStack toExtract = slot.getStack().copy();
        final long extracted = this.commonExtract(ItemInfoWrapper.generateFromStack(toExtract), slot, amountToExtract, simulate);
        if (extracted > 0) {
            toExtract.setCount((int) extracted);
            return toExtract;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Attempts to extract items from this inventory UP TO the given amountToExtract. <br/>
     * Starts extraction from the first NON-EMPTY slot of this inventory.
     *
     * @param amountToExtract The amount of items to extract
     * @param simulated Whether this action is simulated
     * @return A set of all item stacks extracted from this inventory. The count of ALL items will add up to the given amountToExtract
     */
    public Set<ItemStack> extractAny(int amountToExtract, final boolean simulated) {
        final HashSet<ItemStack> extracted = new HashSet<>();

        for (final ContainerSlot slot : this.populatedSlots) {
            if (amountToExtract <= 0) {
                break;
            }

            final int amountExtracted = slot.extractStack(null, amountToExtract, simulated);
            if (amountExtracted > 0) {
                amountToExtract -= amountExtracted;
                extracted.add(new ItemStack(slot.getType(), amountExtracted));
            }
        }

        return extracted;
    }

    /**
     * Attempts to extract an item from the first POPULATED slot
     *
     * @param entireStack Whether the entire stack should be extracted, or a single item
     * @param simulated Whether this action is simulated
     * @return The extracted item
     */
    public ItemStack extractSingle(final boolean entireStack, final boolean simulated) {
        //TODO: don't use stream here
        final Optional<ContainerSlot> first = this.populatedSlots.stream().findFirst(); //I don't like using a stream here

        if (first.isPresent()) {
            final ContainerSlot slot = first.get();

            final Item beforeType = slot.getType();
            final long extracted = slot.extractStack(null, entireStack ? slot.getStack().getCount() : 1, simulated);
            if (extracted > 0) {
                return new ItemStack(beforeType, (int) extracted);
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Shifts the items in this inventory by the desired amount. Shifting will fail when attempting to move into a populated slot <p>
     * When A slot goes outside the inventory indices, the onEnd consumer will be called. By default, items will not be moved.
     *
     * @param shiftBy the amount of indices to shift by
     * @param onEnd   Called when a ContainerSlot attempts to shift outside of inventory indices. Ex, Slots moved outside of inventory moves to another inventory.
     */
    public void shiftSlots(final int shiftBy, final BiFunction<ContainerSlot, Integer, Boolean> onEnd) {
        if (shiftBy == 0) { //If we're attempting to shift by 0, don't do anything
            return;
        }

        final List<SlotAndItemHolder> holders = new ArrayList<>();

        //-1 is backwards, 1 is forwards
        final int direction = Mth.sign(shiftBy);
        for (final ContainerSlot slot : this.inventory) {
            if (slot.isEmpty()) {
                continue;
            }

            //We are attempting to move out of the inventory
            int newIndex = slot.getIndex() + shiftBy;
            if (newIndex > this.getContainerSize() - 1 || newIndex < 0) {
                if (onEnd.apply(slot, direction)) {
                    continue;
                } else {
                    newIndex = direction == 1 ? this.getContainerSize() - 1 : 0;
                }
            }

            holders.add(new SlotAndItemHolder(slot.getIndex(), newIndex, slot.getStack()));
        }

        //Don't try to shift slots if we're completely full, or there's no valid slots to shift
        if (!holders.isEmpty() && holders.size() != this.getContainerSize()) {
            this.processHolders(holders, direction);
        }
    }

    private void processHolders(final List<SlotAndItemHolder> holders, final int direction) {
        if (direction == 1) {
            for (int i = holders.size() - 1; i >= 0; i--) {
                this.shiftSlot(holders.get(i));
            }
        } else {
            for (final SlotAndItemHolder holder : holders) {
                this.shiftSlot(holder);
            }
        }
    }

    private void shiftSlot(final SlotAndItemHolder holder) {
        final ContainerSlot next = this.getSlot(holder.nextIndex());
        if (next.isEmpty()) {
            next.setStack(holder.stack());
            this.getSlot(holder.currentIndex()).setStack(ItemStack.EMPTY);
        }
    }

    public static void setOtherAndEmptyCurrent(final ContainerSlot current, final ContainerSlot other) {
//        if (other.isEmpty()) {
        other.setStack(current.getStack());
        current.setStack(ItemStack.EMPTY);
//        }
    }

    /**
     * Gets all slots that contain the desired Item
     *
     * @param type The item to filter for
     * @return A set of filtered slots
     */
    @NotNull
    public Set<ContainerSlot> getFilteredSlots(@Nullable final Item type) {
        if (type == null) {
            return this.populatedSlots;
        }

        if (this.filteredSlots.containsKey(type)) {
            return new HashSet<>(this.filteredSlots.get(type));
        } else {
            return new HashSet<>();
        }
    }

    /**
     * Grabs all slots that can be inserted into for the given Item
     *
     * @param type               The item to filter for
     * @param shouldIncludeEmpty Whether EMPTY slots should also be included
     * @return A set of all valid insertable slots
     */
    public Set<ContainerSlot> getInsertableSlotsFor(final Item type, final boolean shouldIncludeEmpty) {
        final Set<ContainerSlot> filteredSlots = this.getFilteredSlots(type);

        if (shouldIncludeEmpty) {
            filteredSlots.addAll(this.getFilteredSlots(Items.AIR));
        }

        return filteredSlots;
    }

    /**
     * @return The size of this inventory
     */
    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    /**
     * @return Whether this inventory is empty
     */
    @Override
    public boolean isEmpty() {
        return this.populatedSlots.isEmpty();
    }

    /**
     * Gets the item stack in the desired slot
     *
     * @param slot The slot to get the item stack from
     * @return The item stack in that slot
     */
    @Override
    public @NotNull ItemStack getItem(final int slot) {
        return this.inventory.get(slot).getStack();
    }

    /**
     * Gets the {@link ContainerSlot ContainerSlot} with the desired
     *
     * @param slot The slot to get
     * @return The ContainerSlot in that slot
     */
    public ContainerSlot getSlot(final int slot) {
        return this.inventory.get(slot);
    }

    /**
     * Sets the item in the specified slot, and updates appropriate fields
     *
     * @param slot  The slot to override its item
     * @param stack The replacement stack
     */
    @Override
    public void setItem(final int slot, final @NotNull ItemStack stack) {
        this.inventory.get(slot).setStack(stack);
    }

    public void clearAndDropContents(final Level level, final BlockPos dropPos) {
        for (final ContainerSlot slot : this.populatedSlots) {
            Containers.dropItemStack(level, dropPos.getX(), dropPos.getY(), dropPos.getZ(), slot.getStack());
        }

        this.clearContent();
    }

    /**
     * Clears the contents of this inventory and calls {@link Container#setChanged() setChanged()}
     */
    @Override
    public void clearContent() {
        Collections.fill(this.inventory, ContainerSlot.EMPTY);
        this.populatedSlots.clear();
        this.filteredSlots.clear();

        this.setChanged();
    }

    /**
     * Called whenever an ItemStack in a given {@link ContainerSlot ContainerSlot} changes. <p>
     * Most updates only happen when the {@link Item Item} of the stack changes
     *
     * @param slot         The slot this change is taking place in
     * @param oldSlotStack The old stack associated with this slot
     * @param newSlotStack The new stack associated with this slot
     */
    public void onStackItemChange(final ContainerSlot slot, final ItemStack oldSlotStack, final ItemStack newSlotStack) {
        //Update item count
        final int oldcount = oldSlotStack.getCount();
        final int newCount = newSlotStack.getCount();

        this.storedItemCount += newCount - oldcount;
        //

        //If we're the same item as before
        if (ItemStack.isSameItem(oldSlotStack, newSlotStack))
            return;

        final Item newItem = newSlotStack.getItem();
        final Item oldItem = oldSlotStack.getItem();

        //Add the new item to the filtered view, and remove the old item from the filtered view
        this.filteredSlots.computeIfAbsent(newItem, $ -> new HashSet<>()).add(slot);
        if (this.filteredSlots.containsKey(oldItem)) {
            final Set<ContainerSlot> oldFilteredSlot = this.filteredSlots.get(oldItem);
            oldFilteredSlot.remove(slot);
            if (oldFilteredSlot.isEmpty()) {
                this.filteredSlots.remove(oldItem);
            }
        }

        //If we're now empty, remove us from the filledSlots
        //Else, if we're not empty now, and we're not the same empty state as before, add us to the filledSlots
        if (newSlotStack.isEmpty()) {
            this.populatedSlots.remove(slot);
        } else if (oldSlotStack.isEmpty() != newSlotStack.isEmpty()) {
            this.populatedSlots.add(slot);
        }
    }

    @Override
    public CompoundTag write(final HolderLookup.Provider provider) {
        final CompoundTag invCompound = new CompoundTag();

        invCompound.putInt("Stored Count", this.storedItemCount);

        final ListTag inv = new ListTag();
        for (final ContainerSlot slot : this.inventory) {
            inv.add(slot.write(provider));
        }

        invCompound.put("Items", inv);
        return invCompound;
    }

    //This will be called after this container has been instantiated.
    @Override
    public void read(final HolderLookup.Provider provider, final CompoundTag nbt) {
        this.storedItemCount = nbt.getInt("Stored Count");

        final ListTag inv = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (final Tag tag : inv) {
            final CompoundTag itemTag = (CompoundTag) tag;
            final ContainerSlot slot = this.inventory.get(itemTag.getInt("index"));
            slot.read(provider, itemTag);
            this.populatedSlots.add(slot);
        }
    }

    @Override
    public void populateFields(final ContainerSlot slot) {
        this.filteredSlots.computeIfAbsent(slot.getType(), $ -> new HashSet<>()).add(slot);
        if (!slot.getStack().isEmpty())
            this.populatedSlots.add(slot);
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    @Override
    public List<ContainerSlot> getInventoryAsList() {
        return this.inventory;
    }

    @Override
    public Set<ContainerSlot> getPopulatedSlots() {
        return this.populatedSlots;
    }

    /**
     * Gets the fill level of this inventory, on a scale between 0 - 1.
     *
     * @return How full this inventory is, with 1 being completely filled
     */
    public float getFillLevel() {
        return (this.getContainerSize() * this.getMaxStackSize()) / (float) (this.storedItemCount);
    }

    /**
     * Gets whether this inventory is completely filled or not
     *
     * @return Whether this inventory is full
     */
    public boolean isFull() {
        return this.getFillLevel() == 1;
    }

    public ContainerSlot getFirst() {
        return this.getSlot(0);
    }

    public ContainerSlot getLast() {
        return this.getSlot(this.getContainerSize() - 1);
    }

    public record SlotAndItemHolder(int currentIndex, int nextIndex, ItemStack stack) {

    }
}
