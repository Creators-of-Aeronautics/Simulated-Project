package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import dev.eriksonn.aeronautics.network.packets.LevititeCatalystCrystallizationPacket;
import dev.eriksonn.aeronautics.util.CatalyzerHelper;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import static com.simibubi.create.foundation.utility.RaycastHelper.getTraceTarget;

public class LevititeClientCatalyzerHandler implements InteractCallback {
    @NotNull
    private static ClipContext gatherContext(final Player player) {
        final Vec3 origin = player.getEyePosition();
        final Vec3 target = getTraceTarget(player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), origin);
        return new ClipContext(
                origin,
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.SOURCE_ONLY,
                player);
    }

    @Override
    public Result onUse(final int modifiers, final int action, final KeyMapping rightKey) {

        if (action == GLFW.GLFW_PRESS) {
            final LocalPlayer player = (LocalPlayer) SimDistUtil.getClientPlayer();
            final Level level = player.level();

            //Gather catalyzer
            final InteractionHand hand = InteractionHand.MAIN_HAND;
            final ItemStack catalyzer = player.getItemInHand(hand);
            if (!CatalyzerHelper.isCatalyzer(catalyzer))
                return Result.empty();

            final ClipContext context = gatherContext(player);
            final BlockHitResult ray = level.clip(context);
            if (ray.getType() != HitResult.Type.MISS && level.getFluidState(ray.getBlockPos()).getType() == LevititeBlendHelper.getFluid()) {
                VeilPacketManager.server().sendPacket(new LevititeCatalystCrystallizationPacket(ray.getBlockPos(), hand));
                player.swing(hand);
                return new Result(true);
            }
        }

        return Result.empty();
    }
}
