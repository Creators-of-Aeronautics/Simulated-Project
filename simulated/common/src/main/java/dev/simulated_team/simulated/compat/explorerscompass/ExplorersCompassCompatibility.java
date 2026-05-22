package dev.simulated_team.simulated.compat.explorerscompass;

import dev.simulated_team.simulated.service.SimModCompatibilityService;

public class ExplorersCompassCompatibility implements SimModCompatibilityService {
	@Override
	public void init() {
		ExplorersCompassRegistry.init();
	}

	@Override
	public String getModId() {
		return "explorerscompass";
	}
}
