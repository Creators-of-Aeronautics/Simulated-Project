package dev.simulated_team.simulated.content.entities.diagram;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Set;

public class CenterOfLiftCalculator {

    // the assumed freestream and the lift axis, in the contraption's frame rather than the camera's
    private static final Vector3dc FORWARD = new Vector3d(0.0, 0.0, -1.0);
    private static final Vector3dc UP = new Vector3d(0.0, 1.0, 0.0);

    private static final Vector3dc ZERO_ANGULAR_VELOCITY = new Vector3d();

    // resultants below this fraction of the summed panel strengths count as cancelling out
    private static final double DEGENERATE_FRACTION = 0.001;

    public enum Status {
        OK,
        NO_SURFACES,
        CANCELLED
    }

    public record Result(Status status, Vector3d position) {
    }

    /**
     * Finds where lift acts on a sub-level, for straight and level flight along its forward axis
     *
     * @param subLevel the sub-level to measure
     * @return the center of lift in sub-level coordinates, or a degenerate status if it has none
     */
    public static Result compute(final ServerSubLevel subLevel) {
        // lift is linear in airspeed, so the resultant acts in the same place at any speed and can be
        // found at rest by running the physics tick's own routine at an assumed unit airspeed
        final Vector3d worldVelocity = subLevel.logicalPose().transformNormal(FORWARD, new Vector3d());

        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(subLevel.getLevel());
        final double timeStep = 1.0 / 20.0 / physicsSystem.getConfig().substepsPerTick;

        final ServerLevelPlot plot = subLevel.getPlot();
        final LiftAccumulator accumulator = new LiftAccumulator();

        for (final BlockSubLevelLiftProvider.LiftProviderContext context : plot.getLiftProviders()) {
            accumulator.addPanel(subLevel, context, null, timeStep, worldVelocity);
        }

        // surfaces on bearings count at the bearing's current angle
        final Pose3d localContraptionPose = new Pose3d();
        for (final KinematicContraption contraption : plot.getContraptions()) {
            contraption.sable$getLocalPose(localContraptionPose, 1.0f);

            for (final BlockSubLevelLiftProvider.LiftProviderContext context : contraption.sable$liftProviders().values()) {
                accumulator.addPanel(subLevel, context, localContraptionPose, timeStep, worldVelocity);
            }
        }

        return accumulator.finish();
    }

    private static final class LiftAccumulator {
        private final Vector3d weightedPosition = new Vector3d();
        private double signedLift;
        private double absoluteLift;

        void addPanel(final ServerSubLevel subLevel,
                      final BlockSubLevelLiftProvider.LiftProviderContext context,
                      final Pose3d localContraptionPose,
                      final double timeStep,
                      final Vector3dc worldVelocity) {
            // a group of one makes the routine report this panel's lift by itself, and the impulses it
            // sums into are discarded so that nothing reaches the body
            final BlockSubLevelLiftProvider.LiftProviderGroup group = new BlockSubLevelLiftProvider.LiftProviderGroup(Set.of(context.pos()));
            final Vector3d discardedLinear = new Vector3d();
            final Vector3d discardedAngular = new Vector3d();

            ((BlockSubLevelLiftProvider) context.state().getBlock()).sable$contributeLiftAndDrag(
                    context, subLevel, localContraptionPose, timeStep,
                    worldVelocity, ZERO_ANGULAR_VELOCITY, discardedLinear, discardedAngular, group);

            if (group.totalLiftStrength <= 0.0) {
                return;
            }

            // weighting by the lift axis drops vertical fins and lets down-force pull the other way
            final double lift = group.totalLift().dot(UP);
            final Vector3d position = group.liftCenter().div(group.totalLiftStrength);

            this.weightedPosition.fma(lift, position);
            this.signedLift += lift;
            this.absoluteLift += Math.abs(lift);
        }

        Result finish() {
            if (this.absoluteLift <= 0.0) {
                return new Result(Status.NO_SURFACES, new Vector3d());
            }

            if (Math.abs(this.signedLift) < DEGENERATE_FRACTION * this.absoluteLift) {
                return new Result(Status.CANCELLED, new Vector3d());
            }

            return new Result(Status.OK, this.weightedPosition.div(this.signedLift));
        }
    }
}
