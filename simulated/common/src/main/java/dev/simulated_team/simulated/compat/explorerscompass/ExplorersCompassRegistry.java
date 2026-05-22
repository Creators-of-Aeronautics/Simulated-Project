package dev.simulated_team.simulated.compat.explorerscompass;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import dev.simulated_team.simulated.Simulated;

public class ExplorersCompassRegistry {
    public static void init() {
        Simulated.getRegistrate().navTarget("explorers_compass", ExplorersCompassNavigationTarget::new, () -> ExplorersCompass.explorersCompass);
    }
}
