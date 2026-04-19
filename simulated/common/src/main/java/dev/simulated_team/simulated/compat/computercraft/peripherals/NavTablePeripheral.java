package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.List;

public class NavTablePeripheral extends SimPeripheral<NavTableBlockEntity> {

    public NavTablePeripheral(final NavTableBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "navigation_table";
    }

    @LuaFunction
    public boolean hasTarget() {
        return this.blockEntity.currentTarget != null;
    }

    @LuaFunction
    public String getTargetType() {
        final NavigationTarget nti = this.blockEntity.getNavTableItem();
        if (nti == null) {
            return null;
        }
        final ResourceLocation key = SimRegistries.NAVIGATION_TARGET.getKey(nti);
        return key != null ? key.toString() : null;
    }

    @LuaFunction
    public Float getRelativeAngle() {
        return this.blockEntity.getRelativeAngle();
    }

    @LuaFunction
    public double getRelativeAngleRad() {
        return Math.toRadians(this.blockEntity.getRelativeAngle());
    }

    @LuaFunction
    public double getDistanceToTarget() {
        return this.blockEntity.distanceToTarget();
    }

    // Nav table samples distance every 11 ticks (0.55s); closure rate reflects that cadence.
    @LuaFunction
    public double getClosureRate() {
        return (this.blockEntity.lastDistanceToTarget() - this.blockEntity.distanceToTarget()) / (11.0 / 20.0);
    }

    @LuaFunction
    public double getVerticalOffsetToTarget() {
        final Vec3 target = this.blockEntity.getTargetPosition(true);
        if (target == null) {
            return 0.0;
        }
        return target.y - this.blockEntity.getProjectedSelfPos().y;
    }

    // Quaternion components in {x, y, z, w} order, matching JOML's constructor and
    // the convention used by CC quaternion libraries (e.g. TechTastic/Advanced-Math).
    @LuaFunction
    public List<Double> getOrientation() {
        final Quaterniondc q = this.blockEntity.getSublevelRot();
        return List.of(q.x(), q.y(), q.z(), q.w());
    }

    // Heading: ship's +Z axis rotated into world frame, yaw measured as atan2(x, z).
    // 0° = facing world +Z (Minecraft south), matches player-yaw convention.
    @LuaFunction
    public double getHeading() {
        return Math.toDegrees(this.computeHeadingRad());
    }

    @LuaFunction
    public double getHeadingRad() {
        return this.computeHeadingRad();
    }

    private double computeHeadingRad() {
        final Quaterniondc q = this.blockEntity.getSublevelRot();
        final Vector3d v = new Vector3d(0, 0, 1);
        q.transform(v);
        return Math.atan2(v.x, v.z);
    }
}
