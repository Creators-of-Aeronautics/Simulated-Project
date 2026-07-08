package dev.simulated_team.simulated.service;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface SimPlatformService {

	SimPlatformService INSTANCE = ServiceUtil.load(SimPlatformService.class);

	boolean isLoaded(String modId);

	void invalidateCapabilities(BlockEntity blockEntity);
}
