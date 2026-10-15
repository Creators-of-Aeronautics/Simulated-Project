package dev.eriksonn.aeronautics.neoforge.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

@GameTestHolder(Aeronautics.MOD_ID)
@PrefixGameTestTemplate(false)
public class PropellerInterpolationTest {

    private static final String TEMPLATE = "propellerinterpolationtest.empty";
    private static final float[] PARTIAL_TICKS = {0, 0.25f, 0.5f, 1};
    private static final BlockPos BEARING_POS = new BlockPos(1, 1, 1);

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void staleEntityHistory(final GameTestHelper helper) {
        final PropellerBearingBlockEntity bearing = assembleBearing(helper, false);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        bearing.setAngle(80);
        entity.setAngle(80);
        bearing.setSpeed(64);

        // Deliberately omit entity.tick(): its inherited previous angle stays at zero.
        for (int tick = 0; tick < 4; tick++) {
            final float previous = entity.getAngle(1);
            bearing.tick();
            final float current = entity.getAngle(1);
            helper.assertTrue(current > previous, "The bearing must advance while entity history is stale");
            assertClose(helper, bearing.prevAngle, previous, "Bearing history");
            assertTrajectory(helper, entity, previous, current, "Culled tick " + tick);

            entity.setControllerPos(null);
            assertTrajectory(helper, entity, 0, current, "Entity history must remain stale");
            entity.setControllerPos(bearing.getBlockPos());
        }
        entity.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void normalTickOrdering(final GameTestHelper helper) {
        helper.setBlock(BEARING_POS.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        final CreativeMotorBlockEntity motor = helper.getBlockEntity(BEARING_POS.below());
        motor.generatedSpeed.setValue(0);
        final PropellerBearingBlockEntity bearing = assembleBearing(helper, false);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        final GameTestSequence sequence = helper.startSequence().thenExecuteAfter(2, () -> {
            bearing.setAngle(80);
            entity.setAngle(80);
        });
        final int[] speeds = {16, 64, 128, 16, 0, -64, -128, 0};
        for (final int speed : speeds) {
            final float[] previous = new float[1];
            sequence.thenExecute(() -> {
                motor.generatedSpeed.setValue(speed);
                previous[0] = entity.getAngle(1);
            }).thenExecuteAfter(1, () -> {
                // Let the world schedule both ticks; do not impose an order in this test.
                final float current = entity.getAngle(1);
                assertClose(helper, bearing.getSpeed(), speed, "Bearing RPM");
                assertClose(helper, bearing.prevAngle, previous[0], "Previous endpoint at RPM " + speed);
                assertAngle(helper, current, previous[0] + bearing.getAngularSpeed(), "Bearing advance at RPM " + speed);
                assertTrajectory(helper, entity, previous[0], current, "Normal tick at RPM " + speed);
                for (final float partialTick : PARTIAL_TICKS) {
                    final float actual = entity.getAngle(partialTick);
                    entity.setControllerPos(null);
                    final float inherited = entity.getAngle(partialTick);
                    entity.setControllerPos(bearing.getBlockPos());
                    assertAngle(helper, actual, inherited, "Agreement with inherited history at RPM " + speed);
                }
            });
        }
        sequence.thenExecute(entity::discard).thenSucceed();
    }

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void wraparound(final GameTestHelper helper) {
        final PropellerBearingBlockEntity bearing = assembleBearing(helper, false);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        bearing.prevAngle = 350;
        entity.setAngle(10);
        assertTrajectory(helper, entity, 350, 10, "Forward wrap");
        assertAngle(helper, entity.getAngle(0.25f), 355, "Forward quarter tick");
        assertAngle(helper, entity.getAngle(0.5f), 0, "Forward half tick");
        bearing.prevAngle = 10;
        entity.setAngle(350);
        assertTrajectory(helper, entity, 10, 350, "Reverse wrap");
        assertAngle(helper, entity.getAngle(0.25f), 5, "Reverse quarter tick");
        assertAngle(helper, entity.getAngle(0.5f), 0, "Reverse half tick");
        entity.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void controllerLifecycle(final GameTestHelper helper) {
        final PropellerBearingBlockEntity bearing = assembleBearing(helper, false);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        entity.setAngle(35);
        entity.tick();
        entity.setAngle(95);
        bearing.prevAngle = 75;

        entity.setControllerPos(null);
        assertTrajectory(helper, entity, 35, 95, "No controller");
        final BlockPos missingPos = new BlockPos(3, 1, 1);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(missingPos)) == null,
                "Missing controller position must be empty");
        entity.setControllerPos(helper.absolutePos(missingPos));
        assertTrajectory(helper, entity, 35, 95, "Missing controller");

        final BlockPos unloadedPos = new BlockPos(29999984, bearing.getBlockPos().getY(), 29999984);
        helper.assertTrue(!helper.getLevel().isLoaded(unloadedPos), "Controller chunk must be unloaded");
        entity.setControllerPos(unloadedPos);
        assertTrajectory(helper, entity, 35, 95, "Unloaded controller");

        helper.setBlock(missingPos, AllBlocks.MECHANICAL_BEARING.getDefaultState());
        entity.setControllerPos(helper.absolutePos(missingPos));
        assertTrajectory(helper, entity, 35, 95, "Non-propeller controller");

        entity.setControllerPos(bearing.getBlockPos());
        bearing.attach(entity);
        helper.assertTrue(bearing.isAttachedTo(entity), "Bearing must be reattached");
        assertTrajectory(helper, entity, 75, 95, "Reattached bearing history");
        entity.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void disassemblySlowdown(final GameTestHelper helper) {
        final PropellerBearingBlockEntity bearing = assembleBearing(helper, false);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        bearing.setAngle(80);
        entity.setAngle(80);
        bearing.setRotationSpeed(12);
        bearing.startDisassemblySlowdown();
        helper.assertTrue(bearing.disassemblySlowdown, "Slowdown must be active");
        final float previous = bearing.slowdownController.getAngle(0);
        bearing.tick();
        assertClose(helper, entity.getAngle(0), previous, "Slowdown starts at the previous endpoint");
        for (final float partialTick : PARTIAL_TICKS) {
            final float expected = bearing.slowdownController.getAngle(partialTick - 1);
            assertClose(helper, entity.getAngle(partialTick), expected,
                    "Slowdown uses partial tick minus one at " + partialTick);
            assertAxialRotation(helper, entity, expected, partialTick);
        }

        setStalled(entity, true);
        helper.assertTrue(entity.isStalled(), "Contraption must be stalled");
        assertFrozenSlowdown(helper, bearing, entity, "Stalled slowdown");
        setStalled(entity, false);
        helper.assertTrue(!entity.isStalled(), "Stopped slowdown must be tested independently of stalling");
        final CompoundTag stopped = bearing.saveWithoutMetadata(helper.getLevel().registryAccess());
        stopped.putBoolean("Running", false);
        bearing.loadWithComponents(stopped, helper.getLevel().registryAccess());
        helper.assertTrue(!bearing.isRunning(), "Bearing must be stopped");
        assertFrozenSlowdown(helper, bearing, entity, "Stopped slowdown");
        entity.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void stoppedAndStalled(final GameTestHelper helper) {
        final PropellerBearingBlockEntity bearing = assembleBearing(helper, false);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        bearing.setAngle(80);
        entity.setAngle(80);
        bearing.setSpeed(0);
        bearing.setRotationSpeed(0);
        entity.tick();
        bearing.tick();
        assertTrajectory(helper, entity, 80, 80, "Zero RPM");

        setStalled(entity, true);
        helper.assertTrue(entity.isStalled(), "Contraption must be stalled");
        bearing.setSpeed(64);
        bearing.tick();
        assertTrajectory(helper, entity, 80, 80, "Stalled bearing with nonzero RPM");
        entity.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = Aeronautics.MOD_ID, template = TEMPLATE)
    public static void gyroscopicTransforms(final GameTestHelper helper) {
        final GyroscopicPropellerBearingBlockEntity bearing =
                (GyroscopicPropellerBearingBlockEntity) assembleBearing(helper, true);
        final PropellerBearingContraptionEntity entity = bearing.getMovedContraption();
        bearing.tick();
        bearing.setStrictTilt(new Vector3d(0.08, 1, -0.06), 1, 1);
        entity.previousTiltQuat.set(entity.tiltQuat);
        bearing.setStrictTilt(new Vector3d(-0.05, 1, 0.1), 1, 1);
        helper.assertTrue(Math.abs(entity.tiltQuat.x) + Math.abs(entity.tiltQuat.z) > 0.01,
                "Gyro tilt must be nontrivial");
        bearing.prevAngle = 80;
        entity.setAngle(100);
        final Vec3 local = new Vec3(0.8, 0.3, -0.4);
        for (final float partialTick : PARTIAL_TICKS) {
            final float angle = 80 + 20 * partialTick;
            final Quaternionf tilt = new Quaternionf(entity.previousTiltQuat).slerp(entity.tiltQuat, partialTick);
            final Vector3f expected = new Vector3f((float) local.x, (float) local.y, (float) local.z)
                    .rotateY(angle * Mth.DEG_TO_RAD).rotate(tilt);
            final Vec3 rotated = entity.applyRotation(local, partialTick);
            assertVector(helper, rotated, new Vec3(expected.x, expected.y, expected.z), "Tilted collision orientation");
            assertVector(helper, entity.reverseRotation(rotated, partialTick), local, "Forward/reverse round trip");
            assertVector(helper, entity.applyRotation(entity.reverseRotation(local, partialTick), partialTick), local,
                    "Reverse/forward round trip");
            // applyLocalTransforms is stripped on the dedicated GameTest server.
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientTransforms.assertRenderedRotation(helper, entity, local, rotated, partialTick);
            }
        }
        entity.discard();
        helper.succeed();
    }

    private static PropellerBearingBlockEntity assembleBearing(final GameTestHelper helper, final boolean gyroscopic) {
        helper.setBlock(BEARING_POS, (gyroscopic ? AeroBlocks.GYROSCOPIC_PROPELLER_BEARING.getDefaultState()
                : AeroBlocks.PROPELLER_BEARING.getDefaultState()).setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(BEARING_POS.above(), AllBlocks.SAIL.getDefaultState());
        final PropellerBearingBlockEntity bearing = helper.getBlockEntity(BEARING_POS);
        bearing.assemble();
        helper.assertTrue(bearing.getMovedContraption() != null, "Propeller must assemble");
        helper.assertTrue(bearing.isAttachedTo(bearing.getMovedContraption()), "Contraption must be attached");
        helper.assertTrue(bearing.getMovedContraption().getBearingEntity() == bearing, "Controller must resolve to bearing");
        return bearing;
    }

    private static void setStalled(final PropellerBearingContraptionEntity entity, final boolean stalled) {
        final CompoundTag data = entity.saveWithoutId(new CompoundTag());
        data.putBoolean("Stalled", stalled);
        entity.load(data);
    }

    private static void assertFrozenSlowdown(final GameTestHelper helper, final PropellerBearingBlockEntity bearing,
                                             final PropellerBearingContraptionEntity entity, final String message) {
        for (final float partialTick : PARTIAL_TICKS) {
            assertClose(helper, entity.getAngle(partialTick), bearing.slowdownController.getAngle(0), message);
            assertAxialRotation(helper, entity, bearing.slowdownController.getAngle(0), partialTick);
        }
    }

    private static void assertTrajectory(final GameTestHelper helper, final PropellerBearingContraptionEntity entity,
                                         final float previous, final float current, final String message) {
        for (final float partialTick : PARTIAL_TICKS) {
            final float expected = previous + partialTick * Mth.wrapDegrees(current - previous);
            assertAngle(helper, entity.getAngle(partialTick), expected,
                    message + " at partial tick " + partialTick);
            assertAxialRotation(helper, entity, expected, partialTick);
        }
        helper.assertTrue(entity.getAngle(1) == current, message + " must return the exact current endpoint");
    }

    private static void assertAxialRotation(final GameTestHelper helper, final PropellerBearingContraptionEntity entity,
                                            final float angle, final float partialTick) {
        final Vec3 local = new Vec3(1, 0, 0);
        final Vector3f expected = new Vector3f(1, 0, 0).rotateY(angle * Mth.DEG_TO_RAD);
        final Vec3 rotated = entity.applyRotation(local, partialTick);
        assertVector(helper, rotated, new Vec3(expected.x, expected.y, expected.z), "Axial collision orientation");
        assertVector(helper, entity.reverseRotation(rotated, partialTick), local, "Axial round trip");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientTransforms.assertRenderedRotation(helper, entity, local, rotated, partialTick);
        }
    }

    private static void assertAngle(final GameTestHelper helper, final float actual, final float expected, final String message) {
        assertClose(helper, Mth.wrapDegrees(actual - expected), 0, message + ": expected " + expected + ", got " + actual);
    }

    private static void assertClose(final GameTestHelper helper, final float actual, final float expected, final String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-4, message + ": expected " + expected + ", got " + actual);
    }

    private static void assertVector(final GameTestHelper helper, final Vec3 actual, final Vec3 expected, final String message) {
        helper.assertTrue(actual.distanceTo(expected) < 1e-4, message + ": expected " + expected + ", got " + actual);
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientTransforms {

        private static void assertRenderedRotation(final GameTestHelper helper, final PropellerBearingContraptionEntity entity,
                                                    final Vec3 local, final Vec3 expected, final float partialTick) {
            final PoseStack poseStack = new PoseStack();
            entity.applyLocalTransforms(poseStack, partialTick);
            // A direction ignores centering, gyro pivot translation and the renderer's Z-fighting nudge.
            final Vector3f rendered = poseStack.last().pose()
                    .transformDirection(new Vector3f((float) local.x, (float) local.y, (float) local.z));
            assertVector(helper, new Vec3(rendered.x, rendered.y, rendered.z), expected, "Render/collision agreement");
        }
    }
}
