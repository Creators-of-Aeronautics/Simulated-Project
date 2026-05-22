package dev.simulated_team.simulated.config.client.items;

import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.config.client.SimClient;
import net.createmod.catnip.config.ConfigBase;

public class SimItemConfigs extends ConfigBase {
    public final ConfigEnum<BlockPropertiesTooltip.Condition> displayProperties =
            this.e(BlockPropertiesTooltip.Condition.GOGGLES, "displayProperties", Comments.displayProperties);

    public final ConfigFloat physicsStaffScrollSensitivity = this.f(0.6f, 0, Float.MAX_VALUE, "physics_staff_scroll_sensitivity", SimItemConfigs.Comments.physicsStaffScrollSensitivity);
    public final ConfigFloat physicsStaffRotateSensitivity = this.f(0.35f, 0, Float.MAX_VALUE, "physics_staff_rotate_sensitivity", SimItemConfigs.Comments.physicsStaffRotateSensitivity);
    public final ConfigBool showNewPonderTag = this.b(true, "show_new_ponders", Comments.showNewPonderTag);

    @Override
    public String getName() {
        return "items";
    }

    public static class Comments {
        static String displayProperties = "When to display physics properties in block tooltips";
        static String physicsStaffScrollSensitivity = "The sensitivity of scrolling when holding a sub-level with the Creative Physics Staff";
        static String physicsStaffRotateSensitivity = "The sensitivity of rotation when holding a sub-level with the Creative Physics Staff";
        static String showNewPonderTag = "If ponders that have new additions should display the \"(New!)\" text";
    }
}
