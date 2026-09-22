package dev.simulated_team.simulated.mixin.filter_stack_fluid;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import dev.simulated_team.simulated.mixin_interface.filter_stack_fluid.FilterStackFluidExtension;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FilterItemStack.class, remap = false)
public abstract class FilterItemStackMixin implements FilterStackFluidExtension {
    @Shadow
    abstract void resolveFluid(Level world);

    @Shadow
    @Final
    private FluidStack filterFluidStack;

    @Override
    public boolean simulated$hasFluid(Level world) {
        resolveFluid(world);
        return filterFluidStack.getFluid() != Fluids.EMPTY;
    }
}
