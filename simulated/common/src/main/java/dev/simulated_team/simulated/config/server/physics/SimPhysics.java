package dev.simulated_team.simulated.config.server.physics;

import net.createmod.catnip.config.ConfigBase;

public class SimPhysics extends ConfigBase {

    public final ConfigFloat redstoneMagnetStrength = this.f(1000, 0, Float.MAX_VALUE, "redstoneMagnetStrength", Comments.redstoneMagnetStrength);
    public final ConfigFloat dockingConnectorStrength = this.f(1000, 0, Float.MAX_VALUE, "dockingConnectorStrength", Comments.dockingConnectorStrength);

    public final ConfigFloat redstoneMagnetLinearAccelerationClamping = this.f(500, 0, Float.MAX_VALUE, "redstoneMagnetLinearAccelerationClamping", Comments.redstoneMagnetLinearAccelerationClamping );
    public final ConfigFloat redstoneMagnetAngularAccelerationClamping = this.f(50, 0, Float.MAX_VALUE, "redstoneMagnetAngularAccelerationClamping", Comments.redstoneMagnetAngularAccelerationClamping);
    public final ConfigFloat dockingConnectorLinearAccelerationClamping = this.f(500, 0, Float.MAX_VALUE, "dockingConnectorLinearAccelerationClamping", Comments.dockingConnectorLinearAccelerationClamping );
    public final ConfigFloat dockingConnectorAngularAccelerationClamping = this.f(50, 0, Float.MAX_VALUE, "dockingConnectorAngularAccelerationClamping", Comments.dockingConnectorAngularAccelerationClamping);

    public final ConfigFloat swivelBearingStiffness = this.f(1600.0f, 0, Float.MAX_VALUE, "swivel_stiffness", Comments.swivelBearingStiffness);
    public final ConfigFloat swivelBearingFriction = this.f(0.3f, 0, Float.MAX_VALUE, "swivel_friction", Comments.swivelBearingFriction);
    public final ConfigFloat swivelBearingDamping = this.f(40.0f, 0, Float.MAX_VALUE, "swivel_damping", Comments.swivelBearingDamping);
    public final ConfigFloat dockingConnectorAngleTolerance = this.f(20.0f, 0, 365.0F, "docking_connector_angle", Comments.dockingConnectorAngleTolerance);
    public final ConfigFloat dockingConnectorDistanceTolerance = this.f(0.5F, 0, 4, "docking_connector_distance", Comments.dockingConnectorDistanceTolerance);
    public final ConfigFloat handleMaxForce = this.f(120.0f, 0, Float.MAX_VALUE,"handleMaxForce", Comments.handleMaxForce);

    public final ConfigFloat swivelBearingRotorThrust = this.f(0.2f, 0, Float.MAX_VALUE, "swivel_rotor_thrust", Comments.swivelBearingRotorThrust);
    public final ConfigFloat swivelBearingAirflowMult = this.f(0.5f, 0, Float.MAX_VALUE, "swivel_rotor_airflow_mult", Comments.swivelBearingAirflowMult);
    public final ConfigFloat swivelBearingSymmetricMult = this.f(4.0f, 0, Float.MAX_VALUE, "swivel_rotor_symmetric_mult", Comments.swivelBearingSymmetricMult);

    public final ConfigFloat physicsStaffLinearStiffness = this.f(2650.0f, 0, Float.MAX_VALUE, "physics_staff_linear_stiffness", SimPhysics.Comments.physicsStaffLinearStiffness);
    public final ConfigFloat physicsStaffLinearDamping = this.f(125.0f, 0, Float.MAX_VALUE, "physics_staff_linear_damping", SimPhysics.Comments.physicsStaffLinearDamping);
    public final ConfigFloat physicsStaffAngularStiffness = this.f(10000.0f, 0, Float.MAX_VALUE, "physics_staff_angular_stiffness", SimPhysics.Comments.physicsStaffAngularStiffness);
    public final ConfigFloat physicsStaffAngularDamping = this.f(850.0f, 0, Float.MAX_VALUE, "physics_staff_angular_damping", SimPhysics.Comments.physicsStaffAngularDamping);


    @Override
    public String getName() {
        return "physics";
    }

    private static class Comments {
        private static final String redstoneMagnetStrength = "The maximum force two magnets will apply towards each other";
        private static final String dockingConnectorStrength = "The maximum force two docking connectors will apply towards each other";
        private static final String redstoneMagnetLinearAccelerationClamping = "Limit for linear acceleration for a magnet pair";
        private static final String redstoneMagnetAngularAccelerationClamping = "Limit for angular acceleration for a magnet pair";
        private static final String dockingConnectorLinearAccelerationClamping = "Limit for linear acceleration for a docking connector pair";
        private static final String dockingConnectorAngularAccelerationClamping = "Limit for angular acceleration for a docking connector pair";

        private static final String swivelBearingStiffness = "The stiffness of locked swivel bearing joints";
        private static final String swivelBearingDamping = "The damping of locked swivel bearing joints";
        private static final String swivelBearingFriction = "The friction / damping of unlocked swivel bearing joints";
        private static final String dockingConnectorAngleTolerance = "The angle tolerance in degrees for docking connectors to link";
        private static final String dockingConnectorDistanceTolerance = "The distance tolerance in blocks for docking connectors to link";
        private static final String handleMaxForce = "The maximum force handles are allowed to apply to the contraption they are attached to";

        private static final String swivelBearingRotorThrust = "Thrust scaling for Swivel Bearings acting as rotors";
        private static final String swivelBearingAirflowMult = "Airflow multiplier for Swivel Bearing rotor velocity feedback (prevents runaway)";
        private static final String swivelBearingSymmetricMult = "Symmetric sail effectiveness multiplier (4.0 = matches lift sails at 15 deg tilt)";

        public static String physicsStaffLinearStiffness = "The linear stiffness of the joint motors used to hold sub-levels by the Creative Physics Staff";
        public static String physicsStaffLinearDamping = "The linear damping of the joint motors used to hold sub-levels by the Creative Physics Staff";
        public static String physicsStaffAngularStiffness = "The angular stiffness of the joint motors used to hold sub-levels by the Creative Physics Staff";
        public static String physicsStaffAngularDamping = "The angular damping of the joint motors used to hold sub-levels by the Creative Physics Staff";
    }
}
