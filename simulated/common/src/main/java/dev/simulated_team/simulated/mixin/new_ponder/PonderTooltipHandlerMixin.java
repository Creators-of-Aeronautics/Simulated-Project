package dev.simulated_team.simulated.mixin.new_ponder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import dev.simulated_team.simulated.service.SimConfigService;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PonderTooltipHandler.class)
public class PonderTooltipHandlerMixin {
	@WrapOperation(method = "makeProgressBar", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/lang/LangBuilder;component()Lnet/minecraft/network/chat/MutableComponent;"))
	private static MutableComponent simulated$addToTooltip(final LangBuilder instance, final Operation<MutableComponent> original) {
		final MutableComponent component = original.call(instance);
		final ItemStack stack = PonderTooltipHandlerAccessor.getTrackingStack();
		if(SimConfigService.INSTANCE.client().itemConfig.showNewPonderTag.get() && stack != null) {
			if(!NewPonderTooltipManager.hasWatchedAllScenes(stack.getItem())) {
				component.append(" ").append(SimLang.translate("tooltip.new_ponder").style(ChatFormatting.GOLD).component());
			}
		}

		return component;
	}

}
