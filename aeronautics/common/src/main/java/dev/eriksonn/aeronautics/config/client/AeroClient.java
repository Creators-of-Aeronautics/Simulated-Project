package dev.eriksonn.aeronautics.config.client;

import net.createmod.catnip.config.ConfigBase;

public class AeroClient extends ConfigBase {

	public final ConfigBool enableHotAirRendering = this.b(true, "hotAirRendering", Comments.hotAirRendering);

	@Override
	public String getName() {
		return "client";
	}

	private static class Comments {
		static String hotAirRendering = "If the wiggly hot air effect inside of balloons should be rendered";
	}
}
