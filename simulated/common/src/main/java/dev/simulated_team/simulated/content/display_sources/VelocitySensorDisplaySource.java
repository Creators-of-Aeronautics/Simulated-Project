package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class VelocitySensorDisplaySource extends AbstractNumericDisplaysource {

    @Override
    List<Component> getOptions() {
        return SimLang.translatedOptions("display_source.velocity_sensor", "speed");
    }

    @Override
    String getKey() {
        return "velocity_sensor.data";
    }

    @Override
    String getSelectionKey() {
        return "VeclotySensorSelection";
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext displayLinkContext, final DisplayTargetStats displayTargetStats) {
        if (!(displayLinkContext.getSourceBlockEntity() instanceof final VelocitySensorBlockEntity vbe)) {
            return ZERO.copy();
        }

        if (displayLinkContext.sourceConfig().getInt(this.getSelectionKey()) == 0) {
            return SimLang.number(Math.abs(vbe.getAdjustedVelocity())).text(" m/s").component();
        }

        return ZERO.copy();
    }
}
