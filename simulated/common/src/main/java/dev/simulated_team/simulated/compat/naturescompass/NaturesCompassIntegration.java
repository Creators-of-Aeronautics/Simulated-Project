package dev.simulated_team.simulated.compat.naturescompass;

import com.chaosthedude.naturescompass.NaturesCompass;
import dev.simulated_team.simulated.Simulated;

// Isolates Nature's Compass class references away from NaturesCompassCompatibility so that
// ServiceLoader.getConstructor can reflect on the compat class when the mod is absent.
class NaturesCompassIntegration {
	static void register() {
		Simulated.getRegistrate().navTarget("natures_compass", NaturesCompassNavigationTarget::new, () -> NaturesCompass.naturesCompass);
	}
}
