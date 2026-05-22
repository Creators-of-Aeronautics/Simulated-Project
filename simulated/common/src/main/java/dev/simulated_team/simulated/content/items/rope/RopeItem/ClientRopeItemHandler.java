package dev.simulated_team.simulated.content.items.rope.RopeItem;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.config.server.blocks.SimBlockConfigs;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ClientRopeItemHandler {

    public static void tick() {
        final Player player = Minecraft.getInstance().player;
        final Level level = Minecraft.getInstance().level;

        if (player == null || level == null)
            return;
        if (Minecraft.getInstance().screen != null)
            return;

        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack heldItem = player.getItemInHand(hand);

            if (!SimItems.ROPE_COUPLING.isIn(heldItem))
                continue;

            if (!heldItem.has(SimDataComponents.ROPE_FIRST_CONNECTION))
                continue;

            final BlockPos firstBlock = heldItem.get(SimDataComponents.ROPE_FIRST_CONNECTION);

            final HitResult rayTrace = Minecraft.getInstance().hitResult;

            if (rayTrace instanceof final BlockHitResult hitResult) {
                final BlockPos hitBlock = hitResult.getBlockPos();
                final Vec3 firstPoint = firstBlock.getCenter();

                final SimBlockConfigs blockConfig = SimConfigService.INSTANCE.server().blocks;
                final double maxRopeRange = blockConfig.maxRopeRange.get();

                boolean inRange = Sable.HELPER.distanceSquaredWithSubLevels(level, firstPoint, hitResult.getLocation()) < maxRopeRange * maxRopeRange;

                boolean valid = RopeItem.isValidRopeAttachment(level, hitBlock) && !hitBlock.equals(firstBlock) && inRange;

                final RopeStrandHolderBehavior holderA = RopeItem.getRopeHolder(level, hitBlock);
                final RopeStrandHolderBehavior holderB = RopeItem.getRopeHolder(level, firstBlock);

                // disallow 2 spools- in the future it would be nice if there was a method / api / something for canConnectTo or the like
                if (valid &&
                        holderA != null && holderA.blockEntity instanceof RopeWinchBlockEntity &&
                        holderB != null && holderB.blockEntity instanceof RopeWinchBlockEntity)
                    valid = false;

                final Vec3 target = valid ? hitBlock.getCenter() : hitResult.getLocation();

                final Color color;
                if (valid) {
                    color = new Color(SimColors.SUCCESS_LIME);
                } else {
                    color = new Color(inRange ? SimColors.PERCHANCE_ORANGE : SimColors.NUH_UH_RED);
                }

                Outliner.getInstance().chaseAABB("FirstRopeAttachmentPoint", new AABB(firstPoint, firstPoint))
                        .colored(color)
                        .lineWidth(1 / 3f)
                        .disableLineNormals();

                final Vec3 globalFirstPoint = Sable.HELPER.projectOutOfSubLevel(level, firstPoint);
                Vec3 globalTarget = Sable.HELPER.projectOutOfSubLevel(level, target);

                if (valid) {
                    Outliner.getInstance().chaseAABB("SecondRopeAttachmentPoint", new AABB(target, target))
                            .colored(color)
                            .lineWidth(1 / 3f)
                            .disableLineNormals();

                    final double points = Math.floor(globalFirstPoint.distanceTo(globalTarget));

                    final Vec3 backwardsDiff = globalFirstPoint.subtract(globalTarget).normalize();
                    for (int i = 0; i < points; i++) {
                        final Vec3 point = globalTarget.add(backwardsDiff.scale(i));

                        Outliner.getInstance().chaseAABB("RopePoint" + i, new AABB(point, point))
                                .colored(color)
                                .lineWidth(1 / 8f)
                                .disableLineNormals();
                    }
                } else if (!inRange) {
                    globalTarget = globalTarget.subtract(globalFirstPoint).normalize().scale(maxRopeRange - 0.5).add(globalFirstPoint);
                    Outliner.getInstance().chaseAABB("SecondRopeAttachmentPoint", new AABB(globalTarget, globalTarget))
                            .colored(color)
                            .lineWidth(1 / 3f)
                            .disableLineNormals();
                }

                final DustParticleOptions data = new DustParticleOptions(color.asVectorF(), 1);
                final double totalFlyingTicks = 10;
                final int segments = (((int) totalFlyingTicks) / 3) + 1;

                for (int i = 0; i < segments; i++) {
                    final Vec3 vec = globalFirstPoint.lerp(globalTarget, level.random.nextFloat());
                    level.addParticle(data, vec.x, vec.y, vec.z, 0, 0, 0);
                }
            }
            return;
        }
    }
}
