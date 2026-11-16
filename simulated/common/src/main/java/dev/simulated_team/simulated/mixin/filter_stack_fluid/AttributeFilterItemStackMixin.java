package dev.simulated_team.simulated.mixin.filter_stack_fluid;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import dev.simulated_team.simulated.mixin_interface.filter_stack_fluid.FilterStackFluidExtension;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FilterItemStack.AttributeFilterItemStack.class)
public abstract class AttributeFilterItemStackMixin implements FilterStackFluidExtension {
    @Override
    public boolean simulated$hasFluid(Level world) {
        return false;
    }
}
