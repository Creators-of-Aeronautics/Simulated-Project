package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.compat.computercraft.AttachedComputerHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin_interface.PlayerTypewriterExtension;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.SimPlatformService;
import net.createmod.catnip.platform.services.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LinkedTypewriterBlockEntity extends SmartBlockEntity implements MenuProvider, ClipboardCloneable {

    private LinkedTypewriterEntries entryMap;
    private final List<Integer> pressedKeys = new ArrayList<>();
    private UUID currentUser;
    private String typedEntry = "";

    public boolean powered;
    public final AttachedComputerHandler computerHandler = new AttachedComputerHandler();

    public LinkedTypewriterBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.entryMap = new LinkedTypewriterEntries();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> list) {
    }

    @Override
    public void tick() {
        super.tick();

        assert this.level != null;
        this.entryMap.updateNetworks(this.level);

        if (!this.level.isClientSide) {
            if (this.getBlockState().getValue(LinkedTypewriterBlock.POWERED) != this.powered) {
                this.level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(LinkedTypewriterBlock.POWERED, this.powered));
            }

            if (this.currentUser != null) {
                final Player currentPlayer = this.level.getPlayerByUUID(this.currentUser);
                if (currentPlayer == null || !playerInRange(currentPlayer, this.level, this.getBlockPos())) {
                    this.disconnectUser();
                }
            }
        }
    }

    public static boolean playerInRange(final Player player, final Level world, final BlockPos pos) {
        final double range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();

        // Make sure we take into account sub-levels! We are a sable addon after all!
        return Sable.HELPER.distanceSquaredWithSubLevels(world, player.getEyePosition(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < range * range;
    }

    public LinkedTypewriterEntries getTypewriterEntries() {
        return this.entryMap;
    }

    /**
     * Attempts to start using this typewriter if there is not already another user interacting with it. <br>
     * Disconnects the previous typewriter if the given user starts using a new one.
     *
     * @param userID The user interacting with this typewriter
     * @return Whether the user was able to successfully start using this typewriter
     */
    public boolean checkAndStartUsing(final UUID userID) {
        if (this.currentUser == null) {
            final PlayerTypewriterExtension player = ((PlayerTypewriterExtension) this.level.getPlayerByUUID(userID));
            if (player != null) {
                this.currentUser = userID;
                final BlockPos previousTypewriter = player.simulated$getCurrentTypewriter();
                if (previousTypewriter != null) {
                    if (this.level.getBlockEntity(previousTypewriter) instanceof final LinkedTypewriterBlockEntity nbe && nbe != this) {
                        nbe.disconnectUser();
                    }
                }

                this.powered = true;
                player.simulated$setCurrentTypewriter(this.getBlockPos());
                if (this.level.isClientSide) {
                    LinkedTypewriterInteractionHandler.associateTypewriter(this);
                } else {
                    this.level.playSound(null, this.worldPosition, AllSoundEvents.CONTROLLER_PUT.getMainEvent(), SoundSource.BLOCKS, 1.0F, 0.95F + 0.1F * this.level.getRandom().nextFloat());
                }

                return true;
            }
        }

        return false;
    }

    public boolean checkUser(final UUID user) {
        return user.equals(this.currentUser);
    }

    /**
     * Disconnects the current user.
     */
    public void disconnectUser() {
        this.powered = false;
        this.currentUser = null;

        if (!this.level.isClientSide) {
            this.pressedKeys.clear();
            this.entryMap.deactivateAll();
            this.setChanged();
            this.sendData();

            this.level.playSound(null, this.worldPosition, SimSoundEvents.LINKED_TYPEWRITER_DING.event(), SoundSource.BLOCKS, 1.0F, 0.95F + 0.1F * this.level.getRandom().nextFloat());
        } else {
            LinkedTypewriterInteractionHandler.associateTypewriter(null);
        }
    }

    public List<Integer> getPressedKeys() {
        return this.pressedKeys;
    }

    /**
     * Called whenever a key is interacted with. Used to bind a frequency to a key, or press / release the given key
     *
     * @param user   The user attempting to interact with this typewwriter
     * @param toBind The frequency to bind to this key. Nullable
     * @param key    They key to interact with
     * @param press  Whether this action is a press or release action
     */
    public void onKeyInteraction(final UUID user, @Nullable final LinkedTypewriterEntries.KeyboardEntry toBind, final int key, final boolean press) {
        if (!this.checkUser(user)) {
            return;
        }

        if (press && toBind != null) {
            this.entryMap.setKey(key, toBind);
            return;
        }

        if (press) {
            this.pressKey(key);
        } else {
            this.releaseKey(key);
        }
    }

    /**
     * @param key the key to activate
     */
    public void pressKey(final int key) {
        this.pressedKeys.add(key);
        if (key == 259) {
            if (!this.typedEntry.isEmpty()) {
                this.typedEntry = this.typedEntry.substring(0, this.typedEntry.length() - 1);
            }
        } else {
            this.typedEntry = this.typedEntry + ((char) key);
        }
        if (this.typedEntry.length() >= 25) {
            this.typedEntry = this.typedEntry.substring(1);
        }
        if (SimPlatformService.INSTANCE.isLoaded("computercraft"))
            this.computerHandler.queueEvent("key", key, entryMap.getEntry(key).isAlive());
        this.entryMap.activateKey(key, this);
    }

    /**
     * @param key The key to deactivate
     */
    public void releaseKey(final int key) {
        this.pressedKeys.remove((Integer) key);
        this.entryMap.deactivateKey(key);
        if (SimPlatformService.INSTANCE.isLoaded("computercraft"))
            this.computerHandler.queueEvent("key_up", key);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putString("typedEntry", this.typedEntry);
        tag.put("Keys", this.entryMap.saveKeys(registries));

        if (this.currentUser != null) {
            tag.putUUID("CurrentUser", this.currentUser);
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.typedEntry = tag.getString("typedEntry");
        this.entryMap = LinkedTypewriterEntries.readKeys(registries, tag.getList("Keys", 10), this.getBlockPos());
        if (tag.contains("CurrentUser")) {
            this.currentUser = tag.getUUID("CurrentUser");
        } else {
            this.currentUser = null;
        }
    }

    public String getTypedEntry() {
        return this.typedEntry;
    }

    @Override
    public void invalidate() {
        this.pressedKeys.clear();
        this.entryMap.deactivateAll();
        this.entryMap.updateNetworks(this.level);

        super.invalidate();
    }

    @Override
    public void destroy() {
        this.pressedKeys.clear();
        this.entryMap.deactivateAll();
        this.entryMap.updateNetworks(this.level);

        super.destroy();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return LinkedTypewriterMenuCommon.create(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return SimBlocks.LINKED_TYPEWRITER.get().getName();
    }

    @Override
    public String getClipboardKey() {
        return "TypewriterKeys";
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider registries, final CompoundTag tag, final Direction side) {
        tag.put("Keys", this.entryMap.saveKeys(registries));
        return true;
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.@NotNull Provider registries, final CompoundTag tag, final Player player, final Direction side, final boolean simulate) {
        if (simulate)
            return true;
        this.entryMap = LinkedTypewriterEntries.readKeys(registries, tag.getList("Keys", 10), this.getBlockPos());
        return true;
    }

}
