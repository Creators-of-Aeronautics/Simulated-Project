package dev.simulated_team.simulated.compat.naturescompass;

import dev.simulated_team.simulated.service.SimModCompatibilityService;

public class NaturesCompassCompatibility implements SimModCompatibilityService {
	@Override
	public void init() {
		NaturesCompassIntegration.register();
	}

	@Override
	public String getModId() {
		return "naturescompass";
	}
}
