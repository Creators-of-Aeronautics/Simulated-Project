package dev.simulated_team.simulated.mixin.filter_stack_fluid;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.simulated_team.simulated.mixin_interface.filter_stack_fluid.FilterStackFluidExtension;
import dev.simulated_team.simulated.mixin_interface.filter_stack_fluid.FilteringBehaviourFluidExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FilteringBehaviour.class, remap = false)
public abstract class FilteringBehaviourMixin extends BlockEntityBehaviour implements FilteringBehaviourFluidExtension {
    @Shadow
    @Final
    protected FilterItemStack filter;

    public FilteringBehaviourMixin(SmartBlockEntity be) {
        super(be);
    }

    @Override
    public boolean simulated$hasFluid() {
        return ((FilterStackFluidExtension) filter).simulated$hasFluid(this.blockEntity.getLevel());
    }
}
