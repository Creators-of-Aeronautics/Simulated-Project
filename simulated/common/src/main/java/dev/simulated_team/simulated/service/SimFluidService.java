package dev.simulated_team.simulated.service;

public interface SimFluidService {

    SimFluidService INSTANCE = ServiceUtil.load(SimFluidService.class);

    /**
     * Forge: mb -> mb (*1)
     * Fabric: mb -> FabricFluidUnits(TM) (*81)
     */
    long mbToLoaderUnits(final long mb);

}
