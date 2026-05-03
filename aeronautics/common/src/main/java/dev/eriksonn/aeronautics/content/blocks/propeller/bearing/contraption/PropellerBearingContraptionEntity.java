package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import static net.createmod.catnip.math.AngleHelper.angleLerp;

public class PropellerBearingContraptionEntity extends ControlledContraptionEntity {

    public Quaternionf tiltQuat = new Quaternionf();
    public Quaternionf previousTiltQuat = new Quaternionf();
    public Direction direction = Direction.UP;

    public PropellerBearingContraptionEntity(final EntityType<?> type, final Level world) {
        super(type, world);
    }

    public static ControlledContraptionEntity create(final Level world, final IControlContraption controller,
                                                     final Contraption contraption,
                                                     final Direction.Axis rotationAxis) {
        final PropellerBearingContraptionEntity entity =
                new PropellerBearingContraptionEntity(AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION.get(), world);
        entity.setControllerPos(controller.getBlockPosition());
        entity.setContraption(contraption);
        entity.setRotationAxis(rotationAxis);
        return entity;
    }

    public PropellerBearingBlockEntity getBearingEntity() {
        if (this.controllerPos == null)
            return null;
        if (!this.level().isLoaded(this.controllerPos))
            return null;
        final BlockEntity te = this.level().getBlockEntity(this.controllerPos);
        if (!(te instanceof PropellerBearingBlockEntity))
            return null;
        return (PropellerBearingBlockEntity) te;
    }

    Quaternionf interpolatedQuat = new Quaternionf();

    Quaternionf getInterpolatedQuat(final float partialTick) {
        return this.previousTiltQuat.slerp(this.tiltQuat, partialTick, this.interpolatedQuat);
    }

    @Override
    public Vec3 applyRotation(Vec3 localPos, final float partialTicks) {
        localPos = VecHelper.rotate(localPos, this.getAngle(partialTicks), this.rotationAxis);
        localPos = SimMathUtils.rotateQuatReverse(localPos, this.getInterpolatedQuat(partialTicks));
        return localPos;
    }

    @Override
    public Vec3 reverseRotation(Vec3 localPos, final float partialTicks) {
        localPos = SimMathUtils.rotateQuat(localPos, this.getInterpolatedQuat(partialTicks));
        localPos = VecHelper.rotate(localPos, -this.getAngle(partialTicks), this.rotationAxis);
        return localPos;
    }

    @Override
    public float getAngle(final float partialTicks) {
        final IControlContraption controller = this.getController();
        if (controller instanceof final PropellerBearingBlockEntity tile) {
            if (tile.disassemblySlowdown) {
                return tile.getInterpolatedAngle(partialTicks - 1);
            }
        }

        return partialTicks == 1.0F ? this.angle : angleLerp(partialTicks, this.prevAngle, this.angle);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void applyLocalTransforms(final PoseStack poseStack, final float partialTicks) {
        //super.applyLocalTransforms(PoseStack, partialTicks);
        final float angle = this.getAngle(partialTicks);
        final Direction.Axis axis = this.getRotationAxis();
        Vec3 normal = new Vec3(this.direction.getStepX(), this.direction.getStepY(), this.direction.getStepZ());
        normal = normal.scale(12 / 16.0);
        TransformStack.of(poseStack)
                .nudge(this.getId())
                .center()
                .translate(normal.scale(-1))
                .rotate(this.getInterpolatedQuat(partialTicks))
                .translate(normal)
                .rotateDegrees(angle, axis)
                .uncenter();
    }


    public void setControllerPos(final BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public void setContraption(final Contraption contraption) {
        super.setContraption(contraption);
    }
}
