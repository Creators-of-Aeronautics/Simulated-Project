package dev.simulated_team.simulated.config.client.misc;

import net.createmod.catnip.config.ConfigBase;

public class SimMiscConfigs extends ConfigBase {
    public final ConfigInt steeringWheelXOffset = this.i(0, -1000, 1000, "steering_wheel_x_offset", SimMiscConfigs.Comments.steeringWheelXOffset);
    public final ConfigInt steeringWheelYOffset = this.i(0, -1000, 1000, "steering_wheel_y_offset", SimMiscConfigs.Comments.steeringWheelYOffset);

    @Override
    public String getName() {
        return "misc";
    }

    public static class Comments {
        static String steeringWheelXOffset = "Offsets the Steering Wheel GUI on the X axis.";
        static String steeringWheelYOffset = "Offsets the Steering Wheel GUI on the Y axis.";
    }
}
