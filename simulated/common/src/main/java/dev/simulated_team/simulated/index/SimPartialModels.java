package dev.simulated_team.simulated.index;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.Simulated;

public class SimPartialModels {

    public static final PartialModel
            QUARTER_SHAFT = block("quarter_shaft"),
            SWIVEL_BEARING_COG = block("swivel_bearing/ironcog"),
            MODULATING_RECEIVER_PLATE = block("modulating_linked_receiver/gold_plate"),
            ROPE_WINCH_SHAFT = block("rope_winch/shaft"),
            ROPE_WINCH_ROPE_COIL = block("rope_winch/rope_coil"),
            ROPE_CONNECTOR_KNOT = block("rope_connector/knot"),
            ASSEMBLER_LEVER = block("physics_assembler/lever"),
            REDSTONE_INDUCTOR_INDICATOR = block("redstone_inductor/redstone_indicator"),
            REDSTONE_ACCUMULATOR_DIODE = block("redstone_accumulator/diode"),
            ALTITUDE_SENSOR_RADIAL_HAND = block("altitude_sensor/radial_hand"),
            ALTITUDE_SENSOR_LINEAR_HAND = block("altitude_sensor/linear_hand"),
            ALTITUDE_SENSOR_RADIAL_CASE = block("altitude_sensor/radial_case"),
            ALTITUDE_SENSOR_LINEAR_CASE = block("altitude_sensor/linear_case"),
            ALTITUDE_SENSOR_INDICATOR = block("altitude_sensor/indicator"),
            ANALOG_TRANSMISSION_COG = block("analog_transmission/gear"),
            STEERING_WHEEL = block("steering_wheel/wheel"),
            SHAFT_SIXTEENTH = block("pedal_shaft/shaft_sixteenth"),
            AUGER_COG = block("auger_shaft/cog"),
            AUGER_REDSTONE_ON = block("auger_shaft/redstone_top_on"),
            AUGER_REDSTONE_OFF = block("auger_shaft/redstone_top_off"),
            THROTTLE_LEVER_BUTTON = block("throttle_lever/button"),
            THROTTLE_LEVER_HANDLE = block("throttle_lever/handle"),
            THROTTLE_LEVER_DIODE = block("throttle_lever/diode"),
            VELOCITY_SENSOR_FAN = block("velocity_sensor/fan"),
            VELOCITY_SENSOR_DIODE = block("velocity_sensor/diode"),
            NAV_TABLE_POINTER = block("navigation_table/nav_table_pointer"),
            NAV_TABLE_INDICATOR = block("navigation_table/redstone_indicator"),
            GIMBAL_SENSOR_GIMBAL = block("gimbal_sensor/gimbal"),
            GIMBAL_SENSOR_COMPASS = block("gimbal_sensor/compass"),
            GIMBAL_SENSOR_NEEDLE = block("gimbal_sensor/needle"),
            GIMBAL_SENSOR_INDICATOR = block("gimbal_sensor/redstone_indicator"),
            CONTRAPTION_DIAGRAM_1x1 = entity("contraption_diagram_small"),
            CONTRAPTION_DIAGRAM_2x2 = entity("contraption_diagram_medium"),
            CONTRAPTION_DIAGRAM_3x3 = entity("contraption_diagram_large"),
            SPRING_MIDDLE = block("spring/middle"),
            TORSION_SPRING = block("torsion_spring/spring"),
            TORSION_SPRING_BRASS = block("torsion_spring/spring_brass"),
            ROPE = block("rope/rope"),
            ROPE_KNOT = block("rope/knot"),
            LINKED_TYPEWRITER_KEY = block("linked_typewriter/key"),
            LINKED_TYPEWRITER_KEY_SPACEBAR = block("linked_typewriter/key_spacebar"),
            LASER_POINTER_LENS_OFF = block("laser_pointer/lens_off"),
            LASER_POINTER_LENS_ON = block("laser_pointer/lens_on"),
            PHYSICS_STAFF_CORE_GLOW = item("creative_physics_staff/core_glow"),
            PHYSICS_STAFF_CORE = item("creative_physics_staff/core"),
            PHYSICS_STAFF_RING = item("creative_physics_staff/ring"),
            PHYSICS_STAFF_SIGMA = item("creative_physics_staff/sigma"),
            PHYSICS_STAFF_INNER_CUBE = item("creative_physics_staff/inner_cube"),
            PHYSICS_STAFF_OUTER_CUBE = item("creative_physics_staff/outer_cube"),
            DOCKING_CONNECTOR_MAIN_PISTON_BOTTOM = block("docking_connector/main_piston_1"),
            DOCKING_CONNECTOR_MAIN_PISTON_TOP = block("docking_connector/main_piston_2"),
            DOCKING_CONNECTOR_SIDE_PISTON_BOTTOM = block("docking_connector/side_piston_1"),
            DOCKING_CONNECTOR_SIDE_PISTON_TOP = block("docking_connector/side_piston_2"),
            DOCKING_CONNECTOR_FOOT = block("docking_connector/foot"),
            ABSORBER_HAT = block("absorber/hat"),
            ABSORBER_PIVOT = block("absorber/pivot"),
            ABSORBER_ARM = block("absorber/arm"),
            ABSORBER_SPONGE_DRY = block("absorber/sponge_dry"),
            ABSORBER_SPONGE_WET = block("absorber/sponge_wet"),
            DIRECTIONAL_GEARSHIFT_CENTER = block("directional_gearshift/barrel"),
            DIRECTIONAL_GEARSHIFT_BARREL_SHAFT = block("directional_gearshift/barrel_shaft"),

            //launch.... plunge....
            LAUNCHED_PLUNGER_SPOOL = item("plunger_launcher/tether_spool"),
            LAUNCHED_PLUNGER_JOINT = item("plunger_launcher/spool_joint"),
            LAUNCHED_PLUNGER_BODY = item("plunger_launcher/plunger_tether");

    public static final EngineParts
            ENGINE_PARTS = new EngineParts(""),
            ENGINE_PARTS_HEATED = new EngineParts("heated/"),
            ENGINE_PARTS_SUPERHEATED = new EngineParts("superheated/");

    private static PartialModel block(final String path) {
        return PartialModel.of(Simulated.path("block/" + path));
    }

    private static PartialModel entity(final String path) {
        return PartialModel.of(Simulated.path("entity/" + path));
    }

    private static PartialModel item(final String path) {
        return PartialModel.of(Simulated.path("item/" + path));
    }

    public static class EngineParts {
        public final PartialModel pipeLeft, pipeRight, outletLeft, outletRight, hatchBottom, hatchTop, mouth;

        public EngineParts(final String prefix) {
            this.pipeLeft = block("portable_engine/" + prefix + "exhaust_pipe_left");
            this.pipeRight = block("portable_engine/" + prefix + "exhaust_pipe_right");
            this.outletLeft = block("portable_engine/" + prefix + "exhaust_outlet_left");
            this.outletRight = block("portable_engine/" + prefix + "exhaust_outlet_right");
            this.hatchBottom = block("portable_engine/" + prefix + "hatch_bottom");
            this.hatchTop = block("portable_engine/" + prefix + "hatch_top");
            this.mouth = block("portable_engine/" + prefix + "mouth");
        }
    }

    public static void init() {
        // init static fields
    }
}
