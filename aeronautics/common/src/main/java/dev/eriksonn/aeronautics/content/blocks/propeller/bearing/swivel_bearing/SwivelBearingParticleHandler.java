package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.swivel_bearing;

import dev.eriksonn.aeronautics.content.particle.PropellerAirParticle;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Vector3d;
import org.joml.Vector3fc;

/**
 * Spawns PropellerAirParticle for SwivelBearing rotors,
 * matching the visual style of PropellerBearing.
 * Uses the synced thrust vector for direction and magnitude.
 */
public final class SwivelBearingParticleHandler {

    private SwivelBearingParticleHandler() {}

    public static void init() {
        SwivelBearingBlockEntity.rotorParticleSpawner = SwivelBearingParticleHandler::spawnParticles;
    }

    private static void spawnParticles(final SwivelBearingBlockEntity be) {
        if (be.getLevel() == null || !be.getLevel().isClientSide()) return;

        final float radius = be.getRotorRadius();
        if (radius < 0.5f) return;

        // Use synced thrust vector for direction and magnitude
        final Vector3fc thrust = be.getThrustVector();
        final float thrustMag = thrust.length();
        if (thrustMag < 0.01f) return;

        final RandomSource random = be.getLevel().random;
        // Particle count proportional to thrust magnitude
        final double particleChance = thrustMag * 0.5;
        if (random.nextFloat() > particleChance) return;

        final int count = Math.min((int) Math.ceil(particleChance), 5);

        // Airflow direction is opposite to thrust (Newton's 3rd law)
        final Vector3d thrustDir = new Vector3d(-thrust.x() / thrustMag, -thrust.y() / thrustMag, -thrust.z() / thrustMag);

        // Particle speed proportional to thrust magnitude (blocks/tick)
        final double airflowSpeed = Math.min(thrustMag * 0.1, 2.0);

        // Origin = bearing center + 1 block in facing direction
        final Direction facing = be.getFacing();
        final Vector3d origin = new Vector3d(
                be.getBlockPos().getX() + 0.5 + facing.getStepX(),
                be.getBlockPos().getY() + 0.5 + facing.getStepY(),
                be.getBlockPos().getZ() + 0.5 + facing.getStepZ()
        );

        // Build perpendicular axes for the disc (perpendicular to thrust direction)
        final Vector3d perpA = new Vector3d();
        final Vector3d perpB = new Vector3d();
        buildPerpAxes(thrustDir, perpA, perpB);

        // Transform origin to world if inside a SubLevel
        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        if (subLevel != null) {
            subLevel.logicalPose().transformPosition(origin);
        }

        final Vector3d pos = new Vector3d();
        final Vector3d vel = new Vector3d();

        for (int i = 0; i < count; i++) {
            final double R = radius * Math.sqrt(random.nextFloat());
            final double angle = Math.PI * 2.0 * random.nextFloat();

            // Offset on disc perpendicular to thrust direction
            final double ca = Math.cos(angle) * R;
            final double sa = Math.sin(angle) * R;

            // Position nudge along thrust direction
            final double nudge = airflowSpeed * random.nextFloat();
            pos.set(origin)
                    .fma(nudge, thrustDir)
                    .fma(ca, perpA)
                    .fma(sa, perpB);

            // Velocity along thrust direction with friction decay
            final double decay = Math.exp(-PropellerAirParticle.frictionScale * nudge);
            vel.set(thrustDir).mul(airflowSpeed * decay);

            be.getLevel().addParticle(new PropellerAirParticleData(true, false),
                    pos.x, pos.y, pos.z,
                    vel.x, vel.y, vel.z);
        }
    }

    /**
     * Build two perpendicular unit vectors to the given direction.
     */
    private static void buildPerpAxes(final Vector3d dir, final Vector3d outA, final Vector3d outB) {
        // Pick a helper vector not parallel to dir
        if (Math.abs(dir.y) < 0.9) {
            outA.set(0, 1, 0);
        } else {
            outA.set(1, 0, 0);
        }
        dir.cross(outA, outA).normalize();
        dir.cross(outA, outB).normalize();
    }
}
