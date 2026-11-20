package dev.simulated_team.simulated.content.entities.launched_plunger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.CatmulRomSpline;
import foundry.veil.Veil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public class LaunchedPlungerEntityRenderer extends EntityRenderer<LaunchedPlungerEntity> {

    private static final Quaternionf POSITIVE_Y = new Quaternionf().setAngleAxis(Math.PI / 2, 1, 0, 0);
    private static final Quaternionf NEGATIVE_Y = new Quaternionf().setAngleAxis(-Math.PI / 2, 1, 0, 0);
    private static final Matrix4f FRUSTUM = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    private static final Quaternionf ORIENTATION = new Quaternionf();
    private static final Quaternionf NEXT_ORIENTATION = new Quaternionf();
    private static final Vector3f POS = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector3f NEXT_NORMAL = new Vector3f();
    private static final Vector3f FACE_NORMAL = new Vector3f();
    private static final Vector3f TARGET = new Vector3f();
    private static final Vector3f SELF = new Vector3f();

    private static final List<Vec3> CABLE_POINTS = new ArrayList<>();
    private static final List<Vec3> PREV_CABLE_POINTS = new ArrayList<>();

    private static final BlockPos.MutableBlockPos LIGHT_POS = new BlockPos.MutableBlockPos();

    public LaunchedPlungerEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }


    public static Vec3 getFirstPersonFocusPos(final float pt) {
        final GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        final Camera camera = gameRenderer.getMainCamera();

        final Vector3d focusPoint = new Vector3d(PlungerLauncherItemRenderer.focusPos);
        final Quaternionf orientation = camera.rotation();
        final boolean shadersActive = Veil.IRIS && Iris.isPackInUseQuick();
        if (!shadersActive) {
            orientation.transformInverse(focusPoint);
        }

        final Vector4f v4 = new Vector4f((float) focusPoint.x, (float) focusPoint.y, (float) focusPoint.z, 1.0f);

        final Matrix4f actualProjMat = gameRenderer.getProjectionMatrix(gameRenderer.getFov(camera, AnimationTickHolder.getPartialTicks(), true));
        actualProjMat.invert(new Matrix4f()).transform(v4);

        Matrix4f mat = PlungerLauncherItemRenderer.itemProjMat;
        if (shadersActive) {
            mat = mat.scaleLocal(1f, 1f, 1.0f / 0.125f, new Matrix4f());
        }

        mat.transform(v4);
        final Vec3 cameraPosition = camera.getPosition();
        focusPoint.set(v4.x, v4.y, v4.z);
        orientation.transform(focusPoint);

        final double fov = gameRenderer.getFov(camera, pt, true);
        focusPoint.mul(100 / fov);
        focusPoint.add(cameraPosition.x, cameraPosition.y, cameraPosition.z);

        return JOMLConversion.toMojang(focusPoint);
    }

    @Override
    public void render(final LaunchedPlungerEntity entity, final float f, final float pt, final PoseStack poseStack, final MultiBufferSource multiBufferSource, final int light) {
        super.render(entity, f, pt, poseStack, multiBufferSource, light);

        // render to sublevel and invert rotate rope to be accurate
        final LaunchedPlungerEntity other = entity.getOther();

        Vec3 selfNormal = Vec3.ZERO;
        Vec3 perpendicularNormal = Vec3.ZERO;
        final Direction dir = entity.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
        if (entity.isPlunged()) {
            selfNormal = Vec3.atLowerCornerOf(dir.getNormal());
            if (dir.getAxis().isHorizontal()) {
                perpendicularNormal = Vec3.atLowerCornerOf(Direction.UP.getNormal());
            } else {
                perpendicularNormal = Vec3.atLowerCornerOf(Direction.NORTH.getNormal());
            }
        } else {
            selfNormal = entity.calculateViewVector(-Mth.lerp(pt, entity.xRotO, entity.getXRot()),-Mth.lerp(pt, entity.yRotO, entity.getYRot())).reverse();
            perpendicularNormal = entity.calculateViewVector(-Mth.lerp(pt, entity.xRotO, entity.getXRot()),-Mth.lerp(pt, entity.yRotO, entity.getYRot())-90).reverse();
        }

        poseStack.pushPose();
        Vec3 oldPos = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 newPos = entity.position();
        final float scalingFactor = 0.6f;
        final SubLevel subLevel = Sable.HELPER.getContainingClient(newPos);
        if (subLevel != null) {
            final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;
            final Pose3dc clientPos = clientSubLevel.renderPose(pt);
            newPos = clientPos.transformPosition(newPos);
            selfNormal = clientPos.transformNormal(selfNormal);
            perpendicularNormal = clientPos.transformNormal(perpendicularNormal);

            final Quaterniondc quaterniondc = clientPos.orientation();
            poseStack.mulPose(new Quaternionf(quaterniondc.x(), quaterniondc.y(), quaterniondc.z(), quaterniondc.w()).conjugate());
        }

        final SubLevel oldSubLevel = Sable.HELPER.getContainingClient(oldPos);
        if (oldSubLevel != null) {
            final ClientSubLevel clientSubLevel = (ClientSubLevel) oldSubLevel;
            final Pose3dc clientPos = clientSubLevel.renderPose(pt);
            oldPos = clientPos.transformPosition(oldPos);
        }
        final Vec3 renderPos = oldPos.lerp(newPos, pt);
        final Vec3 pos = renderPos.add(selfNormal.scale(scalingFactor));

        poseStack.translate(-renderPos.x, -renderPos.y, -renderPos.z);

        final Vec3 target;
        if (other != null) {
            Vec3 otherNormal = Vec3.ZERO;
            if (other.isPlunged()) {
                final Direction otherDir = other.getData(LaunchedPlungerEntity.PLUNGED_DIRECTION);
                otherNormal = Vec3.atLowerCornerOf(otherDir.getNormal());
            } else {
                otherNormal = other.calculateViewVector(-Mth.lerp(pt, other.xRotO, other.getXRot()),-Mth.lerp(pt, other.yRotO, other.getYRot())).reverse();
            }

            Vec3 targetOldPos = new Vec3(other.xo, other.yo, other.zo);
            Vec3 targetNewPos = other.position();
            if (other.isRemoved()) {
                targetOldPos = entity.getEntityData().get(LaunchedPlungerEntity.TARGET_POS);
                targetNewPos = targetOldPos;
            }
            final SubLevel targetSublevel = Sable.HELPER.getContainingClient(targetNewPos);
            if (targetSublevel != null) {
                targetNewPos = ((ClientSubLevel) targetSublevel).renderPose(pt).transformPosition(targetNewPos);
                otherNormal = ((ClientSubLevel) targetSublevel).renderPose(pt).transformNormal(otherNormal);
            }
            final SubLevel targetOldSublevel = Sable.HELPER.getContainingClient(other.getPosition(pt));
            if (targetOldSublevel != null) {
                targetOldPos = ((ClientSubLevel) targetOldSublevel).renderPose(pt).transformPosition(targetOldPos);
            }
            target = targetOldPos.lerp(targetNewPos, pt).add(otherNormal.scale(scalingFactor));
        } else {
            final Entity owner = entity.getOwner();
            if (entity.getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty() && owner == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                target = getFirstPersonFocusPos(pt);
            } else {
                if (owner instanceof final AbstractClientPlayer player && entity.getData(LaunchedPlungerEntity.OTHER_PLUNGER).isEmpty()) {
                    final PlayerRenderer playerrenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher()
                            .getRenderer(player);
                    float headYDirection = Mth.lerp(pt, player.yHeadRotO, player.yHeadRot);
                    final float bodyDifference = Math.abs(headYDirection - player.getPreciseBodyRotation(pt)) / 50f;
                    final float headXDirection = Mth.lerp(pt, player.xRotO, player.getXRot());
                    final float lookDelta = Math.abs(Mth.map(headXDirection, 90, 0, 1f, 0f));
                    headYDirection = Mth.lerp(lookDelta, headYDirection, player.getPreciseBodyRotation(pt));
                    final Vec3 viewDirection = player.calculateViewVector(headXDirection, headYDirection);
                    final Vec3 handDirection = player.calculateViewVector(0, headYDirection + 90.0f);
                    target = player.getPosition(pt).add(0.0, 1.28, 0.0).add(viewDirection.scale(0.875)).add(handDirection.scale(Math.abs(Mth.map(headXDirection, 90, 0, 0.325f, 0f)) * (1)));
                } else {
                    target = Vec3.ZERO;
                }
            }
        }

        final Vector3f firstRotation = new Vector3f();
        final Vector3f secondRotation = new Vector3f();
        final Vector3f finalRotation = new Vector3f();

        poseStack.popPose();
        if ((entity.getData(LaunchedPlungerEntity.IS_FIRST) || other == null || other.isRemoved()) && !target.equals(Vec3.ZERO)) {
            final float renderTime = (entity.tickCount + pt + entity.getAnimationOffset());
            final List<Vec3> points = new ObjectArrayList<>();
            final Vec3 start = pos;
            final Vec3 end = target;
            final Vec3 toTarget = end.subtract(start);
            final Vec3 normalizedScalar = toTarget.normalize();
            final float length = (float) renderPos.distanceTo(target);
            points.add(start);
           
            if (length < 1000.0) {
                for (float j = 0.01f; j < length; j += 0.5f) {
                    finalRotation.set(0, 0, 0);
                    final float delta = j / length;
                    final Vec3 point = start.add(toTarget.scale(delta));
                    firstRotation.set(Math.cos((renderTime / 10) + j) * (1 - Math.abs(normalizedScalar.x)), Math.cos((renderTime / 10) + j) * (1 - Math.abs(normalizedScalar.y)), Math.cos((renderTime / 10) + j / 2) * (1 - Math.abs(normalizedScalar.z)));
                    secondRotation.set(Math.sin((renderTime / 10) + j / 4) * (1 - Math.abs(normalizedScalar.x)) * 2, Math.sin((renderTime / 10) + j / 4) * (1 - Math.abs(normalizedScalar.y)), Math.sin((renderTime / 10) + j / 4) * (1 - Math.abs(normalizedScalar.z)));
                    finalRotation.add(firstRotation);
                    finalRotation.add(secondRotation);
                    finalRotation.mul(Math.max(0, 1 - (entity.tickCount + pt) / 40f - ((entity.getPlungedTime() + (entity.getPlungedTime() > 0 ? pt : 0)) / 8f))); // Scales down the animation over time
                    finalRotation.mul((float) (1.0 - Math.pow(2 * delta - 1, 2.0)));
                    points.add(point.subtract(finalRotation.x, finalRotation.y, finalRotation.z));
                }
            }

            points.add(start.add(toTarget));
            points.add(start.add(toTarget));

            renderRope(points, multiBufferSource, Minecraft.getInstance().level, poseStack);
        }

        final double distanceIncludingSublevels = pos.distanceTo(target);

        final PoseTransformStack stack = TransformStack.of(poseStack);
        //render launched plunger and spool
        poseStack.pushPose();
        if (entity.isPlunged()) {
            stack.rotate(dir.getRotation());
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, entity.yRotO, entity.getYRot()) - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, entity.xRotO, entity.getXRot())));
            stack.rotateZDegrees(90f);
        }
        stack.rotateXDegrees(-90);

        stack.scale(1.75f, 1.75f, 1.75f);
        stack.translate(0, 0, 2.5f / 16f);

        final VertexConsumer vb = multiBufferSource.getBuffer(RenderType.solid());
        final SuperByteBuffer body = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_BODY, Blocks.AIR.defaultBlockState());
        final SuperByteBuffer spool = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_SPOOL, Blocks.AIR.defaultBlockState());
        final SuperByteBuffer joint = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_JOINT, Blocks.AIR.defaultBlockState());

        stack.rotateZDegrees(90f);
        body.light(light).renderInto(poseStack, vb);

        FACE_NORMAL.set(selfNormal.x, selfNormal.y, selfNormal.z);
        SELF.set(pos.x, pos.y, pos.z);
        TARGET.set(target.x, target.y, target.z);
        TARGET.add(SELF.mul(-1)).normalize();
        POS.set(perpendicularNormal.x, perpendicularNormal.y, perpendicularNormal.z);

        float angle = 0;
        if (entity.getData(LaunchedPlungerEntity.IS_PLUNGED)) {
            angle = (float) (POS.angleSigned(TARGET, FACE_NORMAL) + Math.PI / 2f);
        }
        if (Float.isNaN(angle)) {
            angle = 0;
        }

        poseStack.pushPose();
        stack.rotateZDegrees((float) Math.toDegrees(angle));
        joint.light(light).renderInto(poseStack, vb);
        stack.translate(0, 0, 3f / 16f);
        stack.rotateXDegrees((float) distanceIncludingSublevels * 90f * 2.6f);
        spool.light(light).renderInto(poseStack, vb);
        poseStack.popPose();

        poseStack.popPose();
    }

    public static void renderRope(final List<Vec3> positions, final MultiBufferSource multiBufferSource, final BlockAndTintGetter level, final PoseStack poseStack) {
        final Vec3 first = positions.getFirst();
        final Vector3d origin = new Vector3d();
        final Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        final RenderType renderType = SimRenderTypes.rope();
        final VertexConsumer builder = multiBufferSource.getBuffer(renderType);

        CABLE_POINTS.clear();
        for (final Vec3 position : positions) {
            CABLE_POINTS.add(position.subtract(cameraPosition));
        }

        final List<Vec3> splinePoints = CatmulRomSpline.generateSpline(CABLE_POINTS, 4);

        final int color = 0xFFFFFFFF;
        final float constantRadius = (2f / 16.0f) / 2.0f;
        final float u = 2f / 16F;
        float v = 0;
        float nextV;

        for (int i = 0; i < splinePoints.size() - 1; i++) {
            final float delta = (float) i / (splinePoints.size() - 1);
            final float nextDelta = (float) (i + 1) / (splinePoints.size() - 1);
            final float cableRadius = constantRadius - (0.001f * delta);
            final float nextCableRadius = constantRadius - (0.001f * nextDelta);
            final Vec3 point = splinePoints.get(i);
            final Vec3 nextPoint = splinePoints.get(i + 1);

            final double x = point.x;
            final double y = point.y;
            final double z = point.z;
            final double nextX = nextPoint.x;
            final double nextY = nextPoint.y;
            final double nextZ = nextPoint.z;

            if (i < splinePoints.size() - 2) {
                calculateOrientation(NEXT_ORIENTATION, nextX, nextY, nextZ, splinePoints.get(i + 2));
            } else {
                NEXT_ORIENTATION.set(ORIENTATION);
            }

            final int lightStart = LevelRenderer.getLightColor(level, LIGHT_POS.set(x + cameraPosition.x, y + cameraPosition.y, z + cameraPosition.z));
            final int lightEnd = LevelRenderer.getLightColor(level, LIGHT_POS.set(nextX + cameraPosition.x, nextY + cameraPosition.y, nextZ + cameraPosition.z));

            final double length = Math.sqrt((nextX - x) * (nextX - x) + (nextY - y) * (nextY - y) + (nextZ - z) * (nextZ - z));
            nextV = v + (float) (length * (17 / 16f));

            // Down
            ORIENTATION.transform(NORMAL.set(0, -1, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0, -1, 0));

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            // Up
            ORIENTATION.transform(NORMAL.set(0, 1, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(0, 1, 0));

            ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            // West
            ORIENTATION.transform(NORMAL.set(-1, 0, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(-1, 0, 0));

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(-nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.transform(POS.set(-cableRadius, cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            ORIENTATION.transform(POS.set(-cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            // East
            ORIENTATION.transform(NORMAL.set(1, 0, 0));
            NEXT_ORIENTATION.transform(NEXT_NORMAL.set(1, 0, 0));

            ORIENTATION.transform(POS.set(cableRadius, -cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(u, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            ORIENTATION.transform(POS.set(cableRadius, cableRadius, 0));
            builder.addVertex((float) (x - origin.x() + POS.x), (float) (y - origin.y() + POS.y), (float) (z - origin.z() + POS.z)).setColor(color).setUv(0, v).setLight(lightStart).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(0, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            NEXT_ORIENTATION.transform(POS.set(nextCableRadius, -nextCableRadius, 0));
            builder.addVertex((float) (nextX - origin.x() + POS.x), (float) (nextY - origin.y() + POS.y), (float) (nextZ - origin.z() + POS.z)).setColor(color).setUv(u, nextV).setLight(lightEnd).setNormal(NEXT_NORMAL.x, NEXT_NORMAL.y, NEXT_NORMAL.z);

            ORIENTATION.set(NEXT_ORIENTATION);
            v = nextV;
        }
    }

    private static void calculateOrientation(final Quaternionf store, final double x, final double y, final double z, final Vec3 nextPoint) {
        final double dx = nextPoint.x - x;
        final double dy = nextPoint.y - y;
        final double dz = nextPoint.z - z;
        final float factor = 0;//(float) Mth.smoothstep(1.0-Mth.clamp(8*Math.sqrt(dx * dx + dz * dz), 0.0, 1.0));
        store.identity().rotateAxis((float) Math.atan2(dx, dz), 0, 1, 0).rotateAxis((float) (Math.acos(dy / Math.sqrt(dx * dx + dy * dy + dz * dz)) - Math.PI / 2.0), 1, 0, 0).slerp(dy < 0 ? POSITIVE_Y : NEGATIVE_Y, factor);
    }

    @Override
    public ResourceLocation getTextureLocation(final LaunchedPlungerEntity entity) {
        return ResourceLocation.withDefaultNamespace("missing");
    }

    @Override
    public boolean shouldRender(final LaunchedPlungerEntity entity, final Frustum frustum, final double d, final double e, final double f) {
        return true;
    }
}
