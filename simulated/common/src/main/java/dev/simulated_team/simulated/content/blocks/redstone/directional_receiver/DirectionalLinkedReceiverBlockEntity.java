package dev.simulated_team.simulated.content.blocks.redstone.directional_receiver;

import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

public class DirectionalLinkedReceiverBlockEntity extends AbstractLinkedReceiverBlockEntity {

    private double angleToClosestLink;

    public DirectionalLinkedReceiverBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /**
     * This is updated every tick, so for now we set angle through this
     */
    // TODO: make angle updating proper
    @Override
    public Tuple<Integer, Double> getSignalFromLink(final Vec3 relativePosition, final int transmittedStrength) {
        final Direction dir = this.getBlockState().getValue(FACING);
        final Vec3 normal = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        final double length = relativePosition.length();

        if (length > AllConfigs.server().logistics.linkRange.get())
            return new Tuple<>(0, 0.0);

        final double dot = relativePosition.dot(normal) / length;

        if (dot < 0) return new Tuple<>(0, 0.0);

        // output to computer craft -> degrees; acos or asin - 90o
        final double angle = Math.asin(dot);
        this.angleToClosestLink = Math.acos(dot);

        final double strengthScalar = Math.clamp((angle / Math.PI) * 2, 0, 1);
        return new Tuple<>((int) Math.ceil(strengthScalar * transmittedStrength), Math.toDegrees(angle));
    }

    public double getAngleToClosestLink() {
        return this.angleToClosestLink;
    }
}
