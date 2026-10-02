package dev.simulated_team.simulated.mixin.filter_stack_fluid;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import dev.simulated_team.simulated.mixin_interface.filter_stack_fluid.FilterStackFluidExtension;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = FilterItemStack.ListFilterItemStack.class, remap = false)
public abstract class ListFilterItemStackMixin implements FilterStackFluidExtension {
    @Shadow
    @Final
    public List<FilterItemStack> containedItems;

    @Override
    public boolean simulated$hasFluid(Level world) {
        return containedItems.stream().anyMatch(stack -> {
            FilterStackFluidExtension duck = (FilterStackFluidExtension) stack;
            return duck.simulated$hasFluid(world);
        });
    }
}
