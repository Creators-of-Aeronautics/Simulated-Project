package dev.simulated_team.simulated.content.items.plunger_launcher;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerServerHandler;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin_interface.PlayerLaunchedPlungerExtension;
import dev.simulated_team.simulated.network.packets.PlungerLauncherShootPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.service.SimEntityService;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlungerLauncherItem extends Item implements CustomArmPoseItem {

    public static boolean reloadCooldown = false;

    public PlungerLauncherItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand interactionHand) {
        final ItemStack heldStack = player.getItemInHand(interactionHand);
        if (SimEntityService.INSTANCE.isFake(player)) {
            return InteractionResultHolder.fail(heldStack);
        }

        if (ShootableGadgetItemMethods.shouldSwap(player, heldStack, interactionHand, s -> s.getItem() instanceof PlungerLauncherItem)) {
            return InteractionResultHolder.fail(heldStack);
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                LaunchedPlungerServerHandler.removePlayerPlungers(player);
                player.displayClientMessage(SimLang.translate("plunger_launcher.clear_plungers").color(0xaaaaaa).component(),true);
                return InteractionResultHolder.success(heldStack);
            }

            final BarrelAndCorrectionInfo info = this.getCorrectionInfo(player, interactionHand);
            final Vec3 barrelPos = info.barrelPos();
            level.playSound(null, barrelPos.x, barrelPos.y, barrelPos.z, SimSoundEvents.PLUNGER_LAUNCH.event(), SoundSource.PLAYERS, 1.0f, 1.0f);

            // add new plunger and set relevant data
            final LaunchedPlungerEntity newPlunger = SimEntityTypes.PLUNGER.create(level);
            newPlunger.setPos(barrelPos);

            newPlunger.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.5F, 0.0F);
            newPlunger.setOldPosAndRot();
            newPlunger.setDeltaMovement(info.motion());

            newPlunger.setOwner(player);
            level.addFreshEntity(newPlunger);

            final PlayerLaunchedPlungerExtension duck = (PlayerLaunchedPlungerExtension) player;

            final LaunchedPlungerEntity plunger = duck.simulated$getLaunchedPlunger();
            if (plunger == null || plunger.isRemoved()) {
                newPlunger.setData(LaunchedPlungerEntity.IS_FIRST, true);
                duck.simulated$setLaunchedPlunger(newPlunger);
                ShootableGadgetItemMethods.applyCooldown(player, heldStack, interactionHand, b -> b.getItem() instanceof PlungerLauncherItem, 4);
                reloadCooldown = false;
            } else { // we're linking
                duck.simulated$setLaunchedPlunger(null);
                newPlunger.setOther(plunger);
                plunger.setOther(newPlunger);
                ShootableGadgetItemMethods.applyCooldown(player, heldStack, interactionHand, b -> b.getItem() instanceof PlungerLauncherItem, 16);
                reloadCooldown = true;
            }

            player.awardStat(Stats.ITEM_USED.get(heldStack.getItem()));
            // send shoot packet
            VeilPacketManager.player((ServerPlayer) player).sendPacket(new PlungerLauncherShootPacket(interactionHand));
            // todo: send to all tracking players for playing (to be implemented) external animation
//            VeilPacketManager.tracking(player).sendPacket(new PlungerLauncherShootPacket(interactionHand));

            if (!BacktankUtil.canAbsorbDamage(player, maxUses()))
                heldStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(interactionHand));
        } else {
            SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER.dontAnimateItem(interactionHand);
        }

        return InteractionResultHolder.success(heldStack);
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return BacktankUtil.isBarVisible(stack, maxUses());
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        return BacktankUtil.getBarWidth(stack, maxUses());
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        return BacktankUtil.getBarColor(stack, maxUses());
    }

    private static int maxUses() {
        return SimConfigService.INSTANCE.server().equipment.maxPlungerLauncherShots.get();
    }

    public @NotNull BarrelAndCorrectionInfo getCorrectionInfo(final Player player, final InteractionHand interactionHand) {
        Vec3 barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(player, interactionHand == InteractionHand.MAIN_HAND,
                new Vec3(.825f, -0.3f, 1.5f));

        final Level level = player.level();
        barrelPos = level.clip(new ClipContext(
                player.getEyePosition(),
                barrelPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        )).getLocation();
        barrelPos = Sable.HELPER.projectOutOfSubLevel(level, barrelPos);

        Vec3 motion = player.getLookAngle();

        final BlockHitResult hit = RaycastHelper.rayTraceRange(level, player, 48);

        if (hit != null) {
            final Vec3 projectedHit = Sable.HELPER.projectOutOfSubLevel(level, hit.getLocation());
            motion = projectedHit.subtract(barrelPos).normalize().scale(1.35f);
        }

        return new BarrelAndCorrectionInfo(barrelPos, motion);
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(final ItemStack stack, final AbstractClientPlayer player, final InteractionHand hand) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean canAttackBlock(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    public record BarrelAndCorrectionInfo(Vec3 barrelPos, Vec3 motion) {
    }
}
