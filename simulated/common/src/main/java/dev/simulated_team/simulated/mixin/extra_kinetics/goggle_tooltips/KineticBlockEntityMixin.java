package dev.simulated_team.simulated.mixin.extra_kinetics.goggle_tooltips;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(KineticBlockEntity.class)
public class KineticBlockEntityMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    public void addExtraKineticsInfo(final List<Component> tooltip, final boolean isPlayerSneaking, final CallbackInfoReturnable<Boolean> cir) {
            if (this instanceof final ExtraKinetics ek) {
                final KineticBlockEntity extraKinetics = ek.getExtraKinetics();
                if (extraKinetics instanceof final ExtraKinetics.ExtraKineticsBlockEntity ekb) {
                    final ArrayList<Component> extraKineticsTooltips = new ArrayList<>();
                    final boolean applied = extraKinetics.addToGoggleTooltip(extraKineticsTooltips, isPlayerSneaking);

                    if (applied) {
                        if (cir.getReturnValue()) {
                            tooltip.add(Component.empty());
                        }

                        SimLang.translate("extra_kinetics.information", SimLang.builder().add(ekb.getKey()).style(ChatFormatting.AQUA)).style(ChatFormatting.WHITE)
                                .forGoggles(tooltip);

                        tooltip.addAll(extraKineticsTooltips);
                        cir.setReturnValue(true);
                    }
                }
            }
    }
}
