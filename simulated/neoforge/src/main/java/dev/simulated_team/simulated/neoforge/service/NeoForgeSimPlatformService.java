package dev.simulated_team.simulated.neoforge.service;

import dev.simulated_team.simulated.service.SimPlatformService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

public class NeoForgeSimPlatformService implements SimPlatformService {

	@Override
	public boolean isLoaded(final String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public void invalidateCapabilities(final BlockEntity blockEntity) {
		if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
			blockEntity.getLevel().invalidateCapabilities(blockEntity.getBlockPos());
		}
	}
}
