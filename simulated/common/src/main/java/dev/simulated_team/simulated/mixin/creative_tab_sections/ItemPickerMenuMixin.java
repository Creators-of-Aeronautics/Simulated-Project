package dev.simulated_team.simulated.mixin.creative_tab_sections;

import dev.simulated_team.simulated.mixin.accessor.CreativeModeInventoryScreenSelectedTabAccessor;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import dev.simulated_team.simulated.service.SimTabService;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuMixin {

	@Shadow protected abstract int getRowIndexForScroll(float f);

	@Shadow public NonNullList<ItemStack> items;

	@Inject(method = "scrollTo", at = @At("HEAD"))
	private void simulated$scrollTo(final float f, final CallbackInfo ci) {
		if(SimTabService.INSTANCE.getCreativeTab() == CreativeModeInventoryScreenSelectedTabAccessor.simulated$getSelectedTab()) {
			SimulatedCreativeTab.padMenuItems(this.items);
		}
		SimulatedCreativeTab.CURRENT_ROW = this.getRowIndexForScroll(f);
	}
}
