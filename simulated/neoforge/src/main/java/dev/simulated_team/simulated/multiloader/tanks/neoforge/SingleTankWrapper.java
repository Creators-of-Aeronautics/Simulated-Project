package dev.simulated_team.simulated.multiloader.tanks.neoforge;

import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

public class SingleTankWrapper extends FluidTank {
    private final SingleTank tank;

    public SingleTankWrapper(final SingleTank tank) {
        super((int) tank.capacity);
        this.tank = tank;
    }

    public static FluidStack fromCType(final CFluidType type, final int amount) {
        return new FluidStack(type.fluid().builtInRegistryHolder(), amount, type.data());
    }

    public static CFluidType toCType(final FluidStack stack) {
        return new CFluidType(stack.getFluid(), stack.getComponents().asPatch());
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        return (int) this.tank.insert(toCType(resource), resource.getAmount(), action.simulate());
    }

    @Override
    public @NotNull FluidStack drain(final int maxDrain, final FluidAction action) {
        return fromCType(this.tank.type, (int) this.tank.extract(this.tank.type, maxDrain, action.simulate()));
    }

    @Override
    public @NotNull FluidStack getFluid() {
        return fromCType(this.tank.type, (int) this.tank.amount);
    }
}
