package dev.simulated_team.simulated.content.blocks.nameplate;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class NameplateBlockEntity extends SmartBlockEntity implements ClipboardCloneable {
    protected boolean glowing;
    protected boolean waxed;
    private DyeColor textColor = DyeColor.BLACK;
    private String name;
    private SubLevel connectedSubLevel;
    /**
     * The block position of the controller if this nameplate is not one. same as getBlockPos() if this nameplate is a controller <br/>
     * Used to prevent needless blockstate checks
     */
    private BlockPos controllerPos;
    /**
     * The block position of one of the sign blocks being supported <br/>
     * Used to reduce blockstate checks
     */
    private BlockPos supportingPos = null;
    private boolean controller;
    private int controllerWidth;

    public NameplateBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);

        //set ourselves to a controller by default
        this.controller = false;
        this.controllerWidth = -1;

        //I think both of these can be the same thing
        this.name = null;

        this.controllerPos = pos;
    }

    private static boolean checkNameplate(final DyeColor color, final Direction facing, final BlockState state) {
        return state.getBlock() instanceof final NameplateBlock npb && npb.getColor() == color && state.getValue(NameplateBlock.FACING) == facing;
    }

    public static boolean canPlayerReach(final NameplateBlockEntity be, final Player player) {
        return getClosestDistance(be, player.getEyePosition()) < player.blockInteractionRange() + 4;
    }

    @Override
    public void initialize() {
        super.initialize();

        final DyeColor color = this.getColor();
        final Direction facing = this.getBlockState().getValue(NameplateBlock.FACING);

        this.checkAndUpdateController(color, facing);

        this.connectedSubLevel = Sable.HELPER.getContaining(this);
        if (this.connectedSubLevel != null && this.allowsEditing()) {
            this.name = this.connectedSubLevel.getName();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {
            final DyeColor color = this.getColor();
            final Direction facing = this.getBlockState().getValue(NameplateBlock.FACING);

            if (this.controller && (this.controllerWidth == -1 || this.controllerWidth == 0)) {
                this.updateNameplates(color, facing);
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!this.level.isClientSide && this.controller && this.allowsEditing()
                    && this.connectedSubLevel != null && !Objects.equals(this.connectedSubLevel.getName(), this.name)) {
            this.setName(this.connectedSubLevel.getName(), true, null);
        }
    }

    /**
     * Called whenever a block is updated around this nameplate. Used to check if this nameplate needs to become a controller, or move its data to a newly placed nameplate to the left of it <br>
     * this method is only called on the server. Data is synced to the client when data is sent afterwards
     */
    public void checkAndUpdateController(final DyeColor color, final Direction facing) {
        final BlockPos leftPos = this.getBlockPos().offset(facing.getClockWise(Direction.Axis.Y).getNormal());

        final boolean wasController = this.controller;
        final BlockState leftState = this.getLevel().getBlockState(leftPos);
        this.controller = !(leftState.getBlock() instanceof final NameplateBlock npb && npb.getColor() == color && leftState.getValue(NameplateBlock.FACING) == facing);

        if (wasController && !this.controller) {
            if (checkNameplate(color, facing, leftState)) {
                final NameplateBlockEntity leftBE = (NameplateBlockEntity) this.getLevel().getBlockEntity(leftPos);

                this.moveController(leftBE);
                leftBE.checkAndUpdateController(color, facing);
            }
        }

        if (this.controller) {
            this.controllerPos = this.getBlockPos();
            this.updateNameplates(color, facing);
            this.invalidateRenderBoundingBox();
        }

        this.notifyUpdate();
    }

    public void updateNameplates(final DyeColor color, final Direction facing) {
        final int preControllerWidth = this.controllerWidth;
        this.controllerWidth = 1;
        final BlockPos.MutableBlockPos p = this.getBlockPos().mutable();
        while (checkNameplate(color, facing, this.level.getBlockState(p.setWithOffset(p, facing.getCounterClockWise(Direction.Axis.Y))))) {
            this.transferData((NameplateBlockEntity) this.getLevel().getBlockEntity(p));
            this.controllerWidth++;
        }
        this.invalidateRenderBoundingBox();

        if (this.controllerWidth != preControllerWidth) {
            this.notifyUpdate();
        }
    }

    private void transferData(final NameplateBlockEntity namePlate) {
        namePlate.resetData();
        namePlate.controllerPos = this.controllerPos;
        namePlate.name = this.getName();
        namePlate.textColor = this.textColor;
        namePlate.glowing = this.glowing;

        namePlate.invalidateRenderBoundingBox();
        namePlate.sendData();
    }

    private void moveController(final NameplateBlockEntity other) {
        other.controller = true;
        other.glowing = this.glowing;

        other.setName(this.getName(), false, null);
        other.setTextColor(this.textColor, false);

        this.resetData();
    }

    // https://math.stackexchange.com/questions/2193720/find-a-point-on-a-line-segment-which-is-the-closest-to-other-point-not-on-the-li
    // finds the closest point to a line segment defined by the controller position and its width
    // i love overkill math
    public static double getClosestDistance(final NameplateBlockEntity nbe, final Vec3 point) {
        if (!nbe.controller) {
            return getClosestDistance(nbe.findController(), point);
        }

        final Vec3i dir = nbe.getBlockState().getValue(NameplateBlock.FACING).getCounterClockWise().getNormal();
        Vec3 A = nbe.getBlockPos().getCenter();
        Vec3 B = A.add(dir.getX() * nbe.controllerWidth, dir.getY() * nbe.controllerWidth, dir.getZ() * nbe.controllerWidth);
        final SubLevel subLevel = Sable.HELPER.getContaining(nbe);
        if (subLevel != null) {
            A = subLevel.logicalPose().transformPosition(A);
            B = subLevel.logicalPose().transformPosition(B);
        }
        final Vec3 v = B.subtract(A);
        final Vec3 u = A.subtract(point);
        final double t = Math.clamp(-v.dot(u) / v.dot(v), 0, 1);
        final Vec3 closest = A.add(v.scale(t));
        return point.distanceTo(closest);
    }

    public boolean allowsEditing() {
        return !this.waxed;
    }

    private void resetData() {
        this.controller = false;
        this.name = null;
        this.glowing = false;
        this.controllerWidth = -1;
    }

    private DyeColor getColor() {
        return ((NameplateBlock) this.getBlockState().getBlock()).getColor();
    }

    public DyeColor getTextColor() {
        return this.textColor;
    }

    public void setTextColor(final DyeColor textColor, final boolean updateNameplates) {
        if (this.controller) {
            this.textColor = textColor;

            if (updateNameplates) {
                this.updateNameplates(this.getColor(), this.getBlockState().getValue(NameplateBlock.FACING));
            }
        }
    }

    public int getDarkColor(final DyeColor textColor) {
        final int i = textColor.getTextColor();
        if (i == DyeColor.BLACK.getTextColor() && this.glowing) {
            return -988212;
        } else {
            final double d = 0.4;
            final int j = (int) ((double) FastColor.ARGB32.red(i) * 0.4);
            final int k = (int) ((double) FastColor.ARGB32.green(i) * 0.4);
            final int l = (int) ((double) FastColor.ARGB32.blue(i) * 0.4);
            return FastColor.ARGB32.color(0, j, k, l);
        }
    }

    public NameplateBlockEntity findController() {
        if (!this.controller) {
            if (this.getLevel().getBlockEntity(this.controllerPos) instanceof final NameplateBlockEntity nbe) {
                nbe.controller = true;
                return nbe;
            }
            this.controller = true;
        }
        return this;
    }

    public boolean isController() {
        return this.getBlockPos().equals(this.controllerPos);
    }

    public int getControllerWidth() {
        return this.controllerWidth;
    }

    public String getName() {
        if (this.connectedSubLevel != null && this.connectedSubLevel.getName() != null && this.allowsEditing()) {
            return this.connectedSubLevel.getName();
        }

        return this.name == null ? "" : this.name;
    }

    /**
     * Applied regardless of whether {@link NameplateBlockEntity#allowsEditing()}
     */
    public void setName(final String name, final boolean updateNameplates, @Nullable final Player player) {
        this.name = name;
        if (this.connectedSubLevel != null && !this.waxed) {
            if (!Objects.equals(this.connectedSubLevel.getName(), name)) {
                this.connectedSubLevel.setName(name);
                if (player != null) {
                    SimStats.SIMULATED_CONTRAPTIONS_NAMED.awardTo(player);
                    SimAdvancements.I_DECLARE_THEE.awardTo(player);
                }
            }
        }

        if (updateNameplates) {
            this.updateNameplates(this.getColor(), this.getBlockState().getValue(NameplateBlock.FACING));
            this.sendData();
        }
    }

    public static boolean hasSupport(final NameplateBlockEntity nbe) {
        if (!nbe.controller) {
            return hasSupport(nbe.findController());
        }

        final Direction facing = nbe.getBlockState().getValue(NameplateBlock.FACING);

        if (nbe.supportingPos != null) {
            if (nbe.level.getBlockState(nbe.supportingPos).is(nbe.getBlockState().getBlock()) &&
                    NameplateBlock.hasBackSupport(facing, nbe.level, nbe.supportingPos)) {
                return true;
            }
            nbe.supportingPos = null;
        }

        final Direction next = facing.getCounterClockWise();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        pos.set(nbe.getBlockPos());

        for (int i = 0; i < nbe.controllerWidth; i++) {
            if (NameplateBlock.hasBackSupport(facing, nbe.level, pos)) {
                nbe.supportingPos = pos.immutable();
                break;
            }
            pos.move(next);
        }

        return nbe.supportingPos != null;
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        tag.putInt("TextColor", this.textColor.getId());
        tag.putBoolean("Glow", this.glowing);
        tag.putBoolean("Waxed", this.waxed);
        if (this.name != null) {
            tag.putString("Name", this.name);
        }

        if (this.controller) {
            tag.putInt("Width", this.controllerWidth);
        } else {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(this.controllerPos));
        }
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        this.textColor = DyeColor.byId(tag.getInt("TextColor"));
        this.glowing = tag.getBoolean("Glow");
        this.waxed = tag.getBoolean("Waxed");
        if (tag.contains("Name")) {
            this.name = tag.getString("Name");
        }

        if (tag.contains("ControllerPos")) {
            this.controller = false;
            this.controllerPos = NbtUtils.readBlockPos(tag, "ControllerPos").get();
        } else {
            this.controller = true;
            this.controllerPos = this.getBlockPos();
            this.controllerWidth = tag.getInt("Width");
        }

        if (clientPacket) {
            this.invalidateRenderBoundingBox();
        }
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!this.controller) {
            return new AABB(this.getBlockPos());
        }

        final Direction facing = this.getBlockState().getValue(NameplateBlock.FACING);
        final Vec3i off = facing.getCounterClockWise(Direction.Axis.Y).getNormal();

        final AABB bounds = AABB.encapsulatingFullBlocks(this.getBlockPos(), this.getBlockPos().offset(off.multiply(this.controllerWidth - 1)));
        return bounds;
    }

    @Override
    public String getClipboardKey() {
        return "Name";
    }

    @Override
    public boolean writeToClipboard(final HolderLookup.@NotNull Provider var1, final CompoundTag tag, final Direction var3) {
        final NameplateBlockEntity controller = this.findController();

        tag.putString("StoredName", controller.getName());
        tag.putInt("TextColor", controller.textColor.getId());

        return true;
    }

    @Override
    public boolean readFromClipboard(final HolderLookup.@NotNull Provider var1, final CompoundTag tag, final Player player, final Direction var4, final boolean simulate) {
        final NameplateBlockEntity controller = this.findController();
        if (!controller.allowsEditing()) {
            return false;
        }

        if (!tag.contains("StoredName")) {
            return false;
        }

        if (simulate) {
            return true;
        }

        controller.setName(tag.getString("StoredName"), true, player);
        controller.textColor = DyeColor.byId(tag.getInt("TextColor"));
        this.sendData();

        return true;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

    }
}
