package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DockingConnectorTank extends SingleTank {

    private final DockingConnectorBlockEntity blockEntity;
    private BlockPos connectedPos;
    private DockingConnectorTank connectedTank;
    private boolean inserting = false;

    public DockingConnectorTank(final DockingConnectorBlockEntity blockEntity) {
        super(1000);
        this.blockEntity = blockEntity;
    }

    public void connect(final BlockPos pos, final DockingConnectorTank other) {
        if (pos.equals(this.connectedPos) && this.connectedTank == other) {
            return;
        }
        this.connectedPos = pos;
        this.connectedTank = other;
        this.blockEntity.onFluidConnectionChanged();
    }

    public void disconnect() {
        if (this.connectedPos == null && this.connectedTank == null) {
            return;
        }
        this.connectedPos = null;
        this.connectedTank = null;
        this.blockEntity.onFluidConnectionChanged();
    }

    private boolean canInteract() {
        final Level level = this.blockEntity.getLevel();
        if (level == null) {
            return false;
        }

        if (this.connectedPos == null || !(level.getBlockEntity(this.connectedPos) instanceof final DockingConnectorBlockEntity other)) {
            return false;
        }

        return this.connectedTank == other.tank;
    }

    boolean isConnectedTo(final DockingConnectorTank other) {
        return this.connectedTank == other && this.canInteract();
    }

    @Override
    public long insert(final CFluidType insertedType, final long maxAmount, final boolean simulate, final Runnable beforeApply) {
        if (!this.canInteract()) {
            return 0;
        }

        this.inserting = true;
        final long v = calculateInsert(this.connectedTank, insertedType, maxAmount);
        if (!simulate) {
            if (beforeApply != null) {
                beforeApply.run();
            }
            applyInsert(this.connectedTank, insertedType, v);
        }
        return v;
    }

    @Override
    public long extract(final CFluidType extractedType, final long maxAmount, final boolean simulate, @Nullable final Runnable beforeApply) {
        this.inserting = false;
        return super.extract(extractedType, maxAmount, simulate, beforeApply);
    }

    @Override
    public Tuple<CFluidType, Long> createSnapshot() {
        if (this.inserting && this.canInteract()) {
            return new Tuple<>(this.connectedTank.type, this.connectedTank.amount);
        } else {
            return super.createSnapshot();
        }
    }

    @Override
    public void readSnapshot(final CFluidType type, final long amount) {
        if (this.inserting && this.canInteract()) {
            this.connectedTank.type = type;
            this.connectedTank.amount = amount;
        } else {
            super.readSnapshot(type, amount);
        }
    }
}
