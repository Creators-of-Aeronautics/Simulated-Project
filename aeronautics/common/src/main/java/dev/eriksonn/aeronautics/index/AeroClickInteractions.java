package dev.eriksonn.aeronautics.index;

import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeClientCatalyzerHandler;

public class AeroClickInteractions extends SimClickInteractions {

	public static LevititeClientCatalyzerHandler LEVITITE_CATALYZER_HANDLER = register(new LevititeClientCatalyzerHandler());

	public static void init() {

	}
}
