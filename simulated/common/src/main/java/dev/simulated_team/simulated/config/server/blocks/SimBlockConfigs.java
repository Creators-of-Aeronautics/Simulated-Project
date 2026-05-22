package dev.simulated_team.simulated.config.server.blocks;

import net.createmod.catnip.config.ConfigBase;

public class SimBlockConfigs extends ConfigBase {

    public final ConfigInt opticalSensorRange = this.i(15, 0, Integer.MAX_VALUE, "optical_sensor_max_range", Comments.opticalSensorRange);
    public final ConfigInt laserPointerRange = this.i(100, 0, Integer.MAX_VALUE, "laser_pointer_max_range", Comments.laserPointerRange);

    public final ConfigFloat maxRopeRange = this.f(40f, 0f, 1000f, "max_rope_range", Comments.maxRopeRange);
    public final ConfigFloat maxRopeStretchAllowed = this.f(25f, 0f, 100f, "max_rope_winch_stretch_allowed", Comments.maxRopeWinchStretch);
    public final ConfigFloat maxRopeZiplineAngle = this.f(85f, 0f, 90f, "max_rope_zipline_angle", Comments.maxRopeZiplineAngle);

    public final ConfigFloat maxSwivelBearingSpeed = this.f(96f, 0f, 256f, "max_swivel_bearing_speed", Comments.maxSwivelBearingSpeed);

    public final ConfigInt dockingConnectorFECapacity = this.i(10000, 0, Integer.MAX_VALUE, "docking_connector_fe_capacity", Comments.dockingConnectorFECapacity);
    public final ConfigInt dockingConnectorFEThroughput = this.i(10000, 0, Integer.MAX_VALUE, "docking_connector_fe_throughput", Comments.dockingConnectorFEThroughput);

    @Override
    public String getName() {
        return "blocks";
    }

    private static class Comments {
        private static final String opticalSensorRange = "Maximum range for the Optical Sensor";

        private static final String laserPointerRange = "Maximum range for the Laser Pointer";

        private static final String maxRopeRange = "Maximum range for rope connections";
        private static final String maxRopeWinchStretch = "Maximum percent the rope mounted on a Rope Winch is allowed to stretch before not accepting input";
        private static final String maxRopeZiplineAngle = "Steepest angle at which a rope can be grabbed onto using a wrench in degrees";

        private static final String maxSwivelBearingSpeed = "The maximum RPM a Swivel Bearing is allowed to rotate at";

        public static final String dockingConnectorFECapacity = "The maximum FE capacity of Docking Connectors";
        public static final String dockingConnectorFEThroughput = "The maximum FE/t throughput of Docking Connectors";
    }
}
