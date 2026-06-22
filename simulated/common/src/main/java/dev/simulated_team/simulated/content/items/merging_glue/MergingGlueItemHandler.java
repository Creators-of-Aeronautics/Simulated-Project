package dev.simulated_team.simulated.content.items.merging_glue;

import com.simibubi.create.AllSpecialTextures;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.network.packets.PlaceMergingGluePacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDistUtil;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MergingGlueItemHandler {

    public BlockPos firstPos;
    public Direction firstDirection;

    public boolean onItemUseBlock(final Level level, final Player player, final ItemStack itemStack, final InteractionHand hand) {
        if (itemStack.isEmpty() || !itemStack.is(SimTags.Items.MERGING_GLUE)) {
            return false;
        }

        if (player.isShiftKeyDown()) {
            this.reset(true);
            return false;
        }

        final HitResult clientHit = Minecraft.getInstance().hitResult;
        if (clientHit instanceof final BlockHitResult hit && hit.getType() != HitResult.Type.MISS) {
            final Direction normal = hit.getDirection();
            final BlockPos pos = hit.getBlockPos();

            if (this.firstPos != null && Sable.HELPER.getContaining(level, this.firstPos) == null) {
                sendMessage("only_between_sub_levels", SimColors.NUH_UH_RED);
                return false;
            }

            final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
            if (subLevel == null) {
                sendMessage("only_between_sub_levels", SimColors.NUH_UH_RED);
                return false;
            }

            final float maxRange = SimConfigService.INSTANCE.server().assembly.mergingGlueRange.getF();
            if (this.firstPos != null && Sable.HELPER.distanceSquaredWithSubLevels(level, Vec3.atCenterOf(pos), Vec3.atCenterOf(this.firstPos)) > maxRange * maxRange) {
                sendMessage("out_of_range", SimColors.NUH_UH_RED);
                return false;
            }

            final BlockPos relative = pos.relative(normal);
            if (this.firstPos != null && this.firstPos.relative(this.firstDirection).equals(relative)) {
                sendMessage("same_block", SimColors.NUH_UH_RED);
                return false;
            }

            if (!level.getBlockState(relative).canBeReplaced()) {
                sendMessage("block_exists", SimColors.NUH_UH_RED);
                return false;
            }

            if (this.firstDirection != null) {
                if (this.firstDirection.getAxis().isHorizontal() != normal.getAxis().isHorizontal()) {
                    sendMessage("invalid_directions.horizontal_vertical", SimColors.NUH_UH_RED);
                    return false;
                }
                if ((normal.getAxis().isVertical() && normal == this.firstDirection)) {
                    if (this.firstDirection == Direction.UP) {
                        sendMessage("invalid_directions.up_up", SimColors.NUH_UH_RED);
                    } else {
                        sendMessage("invalid_directions.down_down", SimColors.NUH_UH_RED);
                    }
                    return false;
                }
            }

            if (this.firstPos != null && subLevel == Sable.HELPER.getContaining(level, this.firstPos)) {
                sendMessage("same_sub_level", SimColors.NUH_UH_RED);
                return false;
            }

            //we can actually place the spring block here

            if (!canSupportGlue(level, pos, normal)) {
                if (this.firstPos == null) {
                    this.firstPos = pos;
                    this.firstDirection = normal;

                    return true;
                } else if (!this.firstPos.relative(this.firstDirection).equals(relative)) { //we are connecting!
                    player.swing(hand);
                    VeilPacketManager.server().sendPacket(new PlaceMergingGluePacket(this.firstPos, pos, this.firstDirection, normal, hand));
                    this.reset(false);
                    return true;
                }
            } else if (this.firstPos != null) {
                sendMessage("not_enough_support", SimColors.NUH_UH_RED);
            }
        }

        return false;
    }

    public void resetWhenShiftRC(final Player player, final ItemStack stack) {
        if (player.isShiftKeyDown() && stack.is(SimTags.Items.MERGING_GLUE)) {
            this.reset(true);
        }
    }

    public void clientTick(final Level level, final LocalPlayer player) {
        if (!player.getMainHandItem().is(SimTags.Items.MERGING_GLUE) && !player.getOffhandItem().is(SimTags.Items.MERGING_GLUE)) {
            this.reset(true);
            return;
        }

        if (this.firstPos != null) {
            final Vec3 linkVec = new Vec3(this.firstDirection.getStepX(), this.firstDirection.getStepY(), this.firstDirection.getStepZ());
            final AABB linkAABB = new AABB(this.firstPos).contract(-linkVec.x, -linkVec.y, -linkVec.z).inflate(-0.1);
            Outliner.getInstance().showAABB(this.firstPos + "MergingGlue", linkAABB)
                    .colored(SimColors.SUCCESS_LIME)
                    .withFaceTexture(AllSpecialTextures.GLUE)
                    .lineWidth(1.0f / 16.0f);

            final HitResult clientHit = Minecraft.getInstance().hitResult;
            if (clientHit.getType() != HitResult.Type.MISS && clientHit instanceof final BlockHitResult hit && Sable.HELPER.getContaining(level, hit.getBlockPos()) != null) {
                final BlockPos pos = hit.getBlockPos();
                final Direction normal = hit.getDirection();

                final float maxRange = SimConfigService.INSTANCE.server().assembly.mergingGlueRange.getF();

                int color = SimColors.SUCCESS_LIME;
                final BlockState replaceState = level.getBlockState(pos.relative(normal));
                boolean invalid = !replaceState.canBeReplaced()
                        || canSupportGlue(level, pos, normal)
                        || this.firstPos.relative(this.firstDirection).equals(pos.relative(normal))
                        || Sable.HELPER.distanceSquaredWithSubLevels(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        this.firstPos.getX() + 0.5,
                        this.firstPos.getY() + 0.5,
                        this.firstPos.getZ() + 0.5) > maxRange * maxRange;

                if (this.firstDirection != null && (this.firstDirection.getAxis().isHorizontal() != normal.getAxis().isHorizontal() || (normal.getAxis().isVertical() && normal == this.firstDirection))) {
                    invalid = true;
                }

                final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
                if (subLevel == null || subLevel == Sable.HELPER.getContaining(level, this.firstPos)) {
                    invalid = true;
                }

                if (invalid) {
                    color = SimColors.NUH_UH_RED;
                }

                final AABB hitAABB = new AABB(pos).contract(-normal.getStepX(), -normal.getStepY(), -normal.getStepZ()).inflate(-0.1);

                final Vec3 globalFirstPoint = Sable.HELPER.projectOutOfSubLevel(level, linkAABB.getCenter());
                final Vec3 globalTarget = Sable.HELPER.projectOutOfSubLevel(level, hitAABB.getCenter());

                final DustParticleOptions data = new DustParticleOptions(new net.createmod.catnip.theme.Color(color).asVectorF(), 1);
                final int segments = 1;

                for (int i = 0; i < segments; i++) {
                    Vec3 vec = globalFirstPoint.lerp(globalTarget, level.getRandom().nextFloat() * 0.8 + 0.1);
                    final float variation = 0.8f;
                    vec = vec.add(variation * (level.getRandom().nextFloat() * 0.5f - 0.25f),
                            variation * (level.getRandom().nextFloat() * 0.5f - 0.25f),
                            variation * (level.getRandom().nextFloat() * 0.5f - 0.25f));
                    level.addParticle(data, vec.x, vec.y, vec.z, 0, 0, 0);
                }

                Outliner.getInstance().showAABB(this.firstPos + " Merging Glue Selection", hitAABB)
                        .colored(color)
                        .withFaceTexture(AllSpecialTextures.GLUE)
                        .lineWidth(1 / 16f);
            }
        }
    }

    public void reset(final boolean sayMessage) {
        if (sayMessage && this.firstPos != null) {
            sendMessage("connection_terminated", SimColors.DISCARDABLE_ORANGE);
        }

        this.firstPos = null;
        this.firstDirection = null;
    }

    public static void sendMessage(final String message, final int color) {
        SimLang.translate("merging_glue." + message)
                .color(color)
                .sendStatus(SimDistUtil.getClientPlayer());
    }

    private static boolean canSupportGlue(final Level level, final BlockPos pos, final Direction normal) {
        return level.getBlockState(pos).getBlockSupportShape(level, pos).getFaceShape(normal).isEmpty();
    }
}
