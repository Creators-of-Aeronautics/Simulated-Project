package dev.simulated_team.simulated.content.blocks.nav_table.navigation_target;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

public interface NavigationTarget {

    /**
     * Implementation to allow Items to directly get desired target
     *
     * @param navBE Nav-Table the item is currently situated in
     * @param self  ItemStack instance of this item
     * @return desired target
     */
    @Nullable
    Vec3 getTarget(NavTableBlockEntity navBE, ItemStack self);

    /**
     * Minimum Operating Range of the Nav-Table item, centered on the nav table
     */
    default float getDeadzone() {
        return 2;
    }

    /**
     * Maximum operating range of the Nav-Table item. <br>
     * Ignored if range is 0 or below.
     */
    default float getMaxRange() {
        return 200;
    }

    default float getModulatingRange() {
        return 200;
    }

    /**
     * @return Redstone Strength depending on direction being checked, or depending on absolute distance
     */
    default int getRedstoneStrength(final NavTableBlockEntity navBE, final Direction direction, final ItemStack self) {
        return this.calculateSideStrength(navBE, direction, self);
    }

    /**
     * @return Redstone Strength depending on distance between target position and Nav-Table Position
     */
    default int calculateModulatingStrength(final NavTableBlockEntity navBE, final ItemStack self) {
        final Vec3 currentTarget = navBE.getTargetPosition(false);
        if (currentTarget == null)
            return 0;

        final Vec3 target = navBE.getTargetPosition(true);
        final Vec3 navPos = navBE.getProjectedSelfPos();

        final double distance = target.distanceTo(navPos);
        return (int) Math.round((this.getModulatingRange() - distance) * (15f / this.getModulatingRange()));
    }

    /**
     * @return Redstone Strength depending on checked direction and position of the target
     */
    default int calculateSideStrength(final NavTableBlockEntity navBE, final Direction direction, final ItemStack self) {
        final Vec3 currentTarget = navBE.getTargetPosition(false);

        //If there is currently no targeted position, return 0
        if (currentTarget == null)
            return 0;

        //NavTable facing information
        final Direction facing = navBE.getBlockState().getValue(NavTableBlock.FACING);
        final Vec3i normal = facing.getNormal();

        //Global positions
        final Vec3 projectedTarget = navBE.getTargetPosition(true);
        final Vec3 navPos = navBE.getProjectedSelfPos();
        Vec3 differenceVec = projectedTarget.subtract(navPos);

        //Rotate Vector
        final Quaterniond worldshellRot = navBE.getSublevelRot();
        differenceVec = SimMathUtils.rotateQuat(differenceVec, worldshellRot);

        //Deadzone Check and range check
        final Vec3 projectedPos = getPlaneProjectedPos(differenceVec, normal);

        final double distance = projectedPos.length();
        if (this.getMaxRange() > 0 && distance > this.getMaxRange() - 0.0001) //Distance
            return 0;

        if (distance < this.getDeadzone() - 0.0001) // Dead-zone
            return 0;

        final double dot = -projectedPos.dot(Vec3.atLowerCornerOf(direction.getNormal())) / distance;
        return (int) (Math.asin(dot) / Math.PI * 30 + 0.5);
    }

    default double distanceToTarget(final NavTableBlockEntity blockEntity) {
        final Vec3 targetPosition = blockEntity.getTargetPosition(true);
        if(targetPosition != null) {
            return blockEntity.getProjectedSelfPos().distanceTo(targetPosition);
        }
        return -1.0f;
    }

    default void onInsert(final ItemStack itemStack, final NavTableBlockEntity be, final Player player) { }

    /**
     * Metadata to surface on the ComputerCraft peripheral for this target. Implementations should return
     * only information the target wants to expose (e.g. search state, kind, source), not absolute world
     * coordinates. Returned as a Lua table. Empty map means "no extra metadata".
     */
    default java.util.Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
        return java.util.Map.of();
    }

    static Vec3 getPlaneProjectedPos(final Vec3 targetPos, final Vec3i normal) {
        final double dot = targetPos.dot(Vec3.atLowerCornerOf(normal));
        return targetPos.subtract(Vec3.atLowerCornerOf(normal).scale(dot));
    }

    @Nullable
    static NavigationTarget ofStack(final ItemStack itemStack) {
        return itemStack.get(SimDataComponents.TARGET);
    }
}
