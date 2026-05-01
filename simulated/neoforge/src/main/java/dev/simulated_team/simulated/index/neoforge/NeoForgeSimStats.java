package dev.simulated_team.simulated.index.neoforge;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;

public class NeoForgeSimStats extends SimStats {
    public static final DeferredRegister<ResourceLocation> CUSTOM_STAT = DeferredRegister.create(BuiltInRegistries.CUSTOM_STAT, Simulated.MOD_ID);
    private static final ArrayList<SimStats.Stat> STATS_TO_LOAD = new ArrayList<>();

    // makes the statistics appear in the menu for a game session even before being awarded for the fist time :p
    public static void bootstrap() {
        for (final SimStats.Stat stat : STATS_TO_LOAD) {
            Stats.CUSTOM.get(stat.identifier().get(), stat.formatter());
        }
        STATS_TO_LOAD.clear();
    }

    public static void register(final IEventBus eventBus) {
        // this is mildly cursed but awawa in one trillion years when fabric happens you'll be thanking me for saving 30 seconds and one kilobyte of duplicated code
        new NeoForgeSimStats().init();
        CUSTOM_STAT.register(eventBus);
    }

    @Override
    protected SimStats.Stat makeCustomStat(final String key, final StatFormatter formatter) {
        final ResourceLocation resourcelocation = Simulated.path(key);
        final SimStats.Stat stat = new SimStats.Stat(CUSTOM_STAT.register(key, () -> resourcelocation), formatter);
        STATS_TO_LOAD.add(stat);
        return stat;
    }
}
