package dev.simulated_team.simulated.index;

import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public abstract class SimStats {
    public static StatFormatter FORMAT_MEALS = val -> val == 0 ? String.valueOf(val) :
            Component.translatable("stat.simulated.format.meals", val).getString();
    private static final double CHARACTERS_PER_SHORT_STORY = 5000 * 6; // ~5000 words per story, ~6 characters per word
    public static StatFormatter FORMAT_SHORT_STORIES = val -> {
        if (val < CHARACTERS_PER_SHORT_STORY * 0.25) {
            return String.valueOf(val);
        }
        return Component.translatable("stat.simulated.format.short_stories",
                LangNumberFormat.format(val / CHARACTERS_PER_SHORT_STORY)
        ).getString();
    };

    public static Stat INTERACT_WITH_ASSEMBLER;
    public static Stat INTERACT_WITH_CONTRAPTION_DIAGRAM;
    public static Stat INTERACT_WITH_HANDLE;
    public static Stat INTERACT_WITH_STEERING_WHEEL;
    public static Stat PORTABLE_ENGINES_FED;
    public static Stat SIMULATED_CONTRAPTIONS_NAMED;
    public static Stat TYPEWRITER_KEY_PRESSES;

    public void init() {
        INTERACT_WITH_ASSEMBLER = this.makeCustomStat("interact_with_assembler", StatFormatter.DEFAULT);
        INTERACT_WITH_CONTRAPTION_DIAGRAM = this.makeCustomStat("interact_with_contraption_diagram", StatFormatter.DEFAULT);
        INTERACT_WITH_HANDLE = this.makeCustomStat("interact_with_handle", StatFormatter.DEFAULT);
        INTERACT_WITH_STEERING_WHEEL = this.makeCustomStat("interact_with_steering_wheel", StatFormatter.DEFAULT);
        PORTABLE_ENGINES_FED = this.makeCustomStat("portable_engines_fed", FORMAT_MEALS);
        SIMULATED_CONTRAPTIONS_NAMED = this.makeCustomStat("simulated_contraptions_named", StatFormatter.DEFAULT);
        TYPEWRITER_KEY_PRESSES = this.makeCustomStat("typewriter_key_presses", FORMAT_SHORT_STORIES);
    }

    protected abstract SimStats.Stat makeCustomStat(final String key, final StatFormatter formatter);

    public record Stat(Supplier<ResourceLocation> identifier, StatFormatter formatter) {
        public void awardTo(final Player player) {
            player.awardStat(this.identifier.get());
        }
    }
}
