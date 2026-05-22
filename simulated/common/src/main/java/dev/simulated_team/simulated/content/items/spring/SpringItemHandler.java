package dev.simulated_team.simulated.content.items.spring;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.network.packets.PlaceSpringPacket;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class SpringItemHandler implements InteractCallback {
    public static final double MAX_LENGTH = 9.0;

    public BlockPos linkPos;
    public Direction linkDirection;

    public boolean tryStartPlacement(final UseOnContext context) {
        final LocalPlayer player = (LocalPlayer) SimDistUtil.getClientPlayer();
        final Level level = player.level();
        final Direction dir = context.getClickedFace();
        final BlockPos pos = context.getClickedPos();
        final BlockPos relative = pos.relative(dir);

        if (this.linkPos != null) {
            return false;
        }

        if (!this.testPlacementAndSendError(level, relative, pos, dir)) {
            return false;
        }

        this.linkPos = pos;
        this.linkDirection = dir;
        return true;
    }

    @Override
    public Result onUse(final int modifiers, final int action, final KeyMapping rightKey) {
        final LocalPlayer player = (LocalPlayer) SimDistUtil.getClientPlayer();
        final Level level = player.level();

        if (action == GLFW.GLFW_PRESS) {
            final InteractionHand hand = this.getHandOrNull(player);
            if (hand == null) {
                this.reset(true);
                return Result.empty();
            }

            if (this.linkPos != null) {
                if (player.isShiftKeyDown()) {
                    player.swing(hand);

                    this.reset(true);
                    return new Result(true);
                }
            }

            final HitResult clientHit = Minecraft.getInstance().hitResult;
            if (clientHit instanceof final BlockHitResult hit && hit.getType() != HitResult.Type.MISS && this.linkPos != null) {
                final Direction dir = hit.getDirection();
                final BlockPos pos = hit.getBlockPos();

                final BlockPos childCenter = pos.relative(dir);
                final BlockPos parentCenter = this.linkPos.relative(this.linkDirection);

                if (this.testExceedsRange(level, childCenter, parentCenter)) {
                    this.sendMessage("out_of_range", SimColors.NUH_UH_RED);
                    return Result.empty();
                }

                if (parentCenter.equals(childCenter)) {
                    this.sendMessage("same_block", SimColors.NUH_UH_RED);
                    return Result.empty();
                }

                if (!this.testPlacementAndSendError(level, childCenter, pos, dir)) {
                    return Result.empty();
                }

                player.swing(hand);
                VeilPacketManager.server().sendPacket(new PlaceSpringPacket(this.linkPos, pos, this.linkDirection, dir, hand));
                this.reset(false);
                return new Result(true);
            }
        }

        return Result.empty();
    }

    private boolean testExceedsRange(final Level level, final BlockPos childPos, final BlockPos parentPos) {
        return Sable.HELPER.distanceSquaredWithSubLevels(
                level,
                childPos.getX() + 0.5,
                childPos.getY() + 0.5,
                childPos.getZ() + 0.5,
                parentPos.getX() + 0.5,
                parentPos.getY() + 0.5,
                parentPos.getZ() + 0.5) > MAX_LENGTH * MAX_LENGTH;
    }

    private boolean testPlacementAndSendError(final Level level, final BlockPos relative, final BlockPos pos, final Direction dir) {
        if (!level.getBlockState(relative).canBeReplaced()) {
            this.sendMessage("block_exists", SimColors.NUH_UH_RED);
            return false;
        }

        if (!Block.canSupportCenter(level, pos, dir)) {
            this.sendMessage("not_enough_support", SimColors.NUH_UH_RED);
            return false;
        }

        return true;
    }

    @Nullable
    public InteractionHand getHandOrNull(final LocalPlayer player) {
        final ItemStack mainItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        final ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);

        InteractionHand hand = null;
        if (mainItem.getItem() instanceof SpringItem) {
            hand = InteractionHand.MAIN_HAND;
        } else if (offHandItem.getItem() instanceof SpringItem) {
            hand = InteractionHand.OFF_HAND;
        }

        return hand;
    }


    public void reset(final boolean sayMessage) {
        if (sayMessage && this.linkPos != null) {
            this.sendMessage("connection_terminated", SimColors.NUH_UH_RED);
        }

        this.linkPos = null;
        this.linkDirection = null;
    }

    public void sendMessage(final String message, final int color) {
        SimLang.translate("spring." + message)
                .color(color)
                .sendStatus(SimDistUtil.getClientPlayer());
    }

    @Override
    public void clientTick(final Level level, final LocalPlayer player) {
        if (!player.getMainHandItem().is(SimItems.SPRING) && !player.getOffhandItem().is(SimItems.SPRING)) {
            this.reset(true);
            return;
        }

        if (this.linkPos != null) {
            final Vec3 linkVec = new Vec3(this.linkDirection.getStepX(), this.linkDirection.getStepY(), this.linkDirection.getStepZ());
            final AABB linkAABB = new AABB(this.linkPos).inflate(-0.3).move(linkVec.scale(0.65));
            Outliner.getInstance().showAABB(this.linkPos + "Spring", linkAABB)
                    .colored(SimColors.SUCCESS_LIME)
                    .lineWidth(1 / 16f);

            final HitResult clientHit = Minecraft.getInstance().hitResult;
            if (clientHit != null && clientHit.getType() != HitResult.Type.MISS && clientHit instanceof final BlockHitResult hit) {
                final BlockPos pos = hit.getBlockPos();
                final Direction dir = hit.getDirection();

                final BlockPos childCenter = pos.relative(dir);
                final BlockPos parentCenter = this.linkPos.relative(this.linkDirection);

                int color = SimColors.SUCCESS_LIME;
                if (!level.getBlockState(pos.relative(dir)).canBeReplaced()
                        || !Block.canSupportCenter(level, pos, dir)
                        || this.linkPos.relative(this.linkDirection).equals(pos.relative(dir))
                        || this.testExceedsRange(level, childCenter, parentCenter)) { //spring already exists here
                    color = SimColors.NUH_UH_RED;
                }

                final AABB hitAABB = new AABB(pos).inflate(-0.3).move(new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()).scale(0.65));

                final Vec3 globalFirstPoint = Sable.HELPER.projectOutOfSubLevel(level, linkAABB.getCenter());
                final Vec3 globalTarget = Sable.HELPER.projectOutOfSubLevel(level, hitAABB.getCenter());

                final DustParticleOptions data = new DustParticleOptions(new net.createmod.catnip.theme.Color(color).asVectorF(), 1);
                final double totalFlyingTicks = 10;
                final int segments = (((int) totalFlyingTicks) / 3) + 1;

                for (int i = 0; i < segments; i++) {
                    final Vec3 vec = globalFirstPoint.lerp(globalTarget, level.getRandom().nextFloat());
                    level.addParticle(data, vec.x, vec.y, vec.z, 0, 0, 0);
                }


                Outliner.getInstance().showAABB(this.linkPos + " Spring Selection", hitAABB)
                        .colored(color)
                        .lineWidth(1 / 16f);
            }
        }
    }
}
