package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.MutableComponent;

public class OpticalSensorDisplaySource extends NumericSingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context, final DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof final OpticalSensorBlockEntity be)) {
            return ZERO.copy();
        }

        switch (context.sourceConfig().getInt("OpticalSensorSelection")) {
            case 0 -> {
                return be.hasHit() ? be.getHitBlock().getName() : SimLang.text("No Block Detected").component();
            }
            case 1 -> {
                if (!be.hasHit()) {
                    return SimLang.text("No Block Detected").component();
                }
                final float rayDistance = be.getRayDistance();
                return SimLang.number(rayDistance)
                        .space()
                        .text("block" + (rayDistance != 1 ? "s" : ""))
                        .component();
            }
        }

        return ZERO.copy();
    }

    @Override
    public void initConfigurationWidgets(final DisplayLinkContext context, final ModularGuiLineBuilder builder, final boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (isFirstLine) {
            return;
        }

        builder.addSelectionScrollInput(0, 85, (selectionScrollInput, label) -> {
            selectionScrollInput
                    .forOptions(SimLang.translatedOptions("display_source.optical_sensor", "detected_block", "block_distance"));
        }, "OpticalSensorSelection");
    }

    @Override
    protected String getTranslationKey() {
        return "optical_sensor.data";
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext context) {
        return true;
    }
}
