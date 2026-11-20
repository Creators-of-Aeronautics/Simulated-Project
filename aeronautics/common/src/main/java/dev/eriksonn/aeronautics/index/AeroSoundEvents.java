package dev.eriksonn.aeronautics.index;

import com.simibubi.create.AllSoundEvents;
import dev.simulated_team.simulated.api.sound.SimSoundEntry;
import dev.simulated_team.simulated.api.sound.SoundEventRegistry;
import dev.eriksonn.aeronautics.Aeronautics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.UnaryOperator;

public class AeroSoundEvents {

    public static final SoundEventRegistry REGISTRY = new SoundEventRegistry(Aeronautics.MOD_ID);

    public static final String BLOCK_BROKEN = "subtitles.block.generic.break";
    public static final String BLOCK_PLACED = "subtitles.block.generic.place";
    public static final String BLOCK_HIT = "subtitles.block.generic.hit";
    public static final String BUCKET_FILLS = "subtitles.item.bucket.fill";
    public static final String BUCKET_EMPTIES = "subtitles.item.bucket.empty";

    public static final SimSoundEntry
        // Block & Fluid Sounds

        ENVELOPE_BREAK = REGISTRY.create("block.envelope.break", definition -> definition
            .defaultSubtitle(BLOCK_BROKEN)
            .addFileVariants("block/envelope/place", 4)),

        ENVELOPE_HIT = REGISTRY.create("block.envelope.hit", definition -> definition
            .defaultSubtitle(BLOCK_HIT)
            .addFileVariants("block/envelope/place", 4)),

        ENVELOPE_PLACE = REGISTRY.create("block.envelope.place", definition -> definition
            .defaultSubtitle(BLOCK_PLACED)
            .addFileVariants("block/envelope/place", 4)),

        LEVITITE_BLEND_CRYSTALLIZE = REGISTRY.create("fluid.levitite_blend.crystallize", definition -> definition
            .subtitle("Levitite crystallizes")
            .addFileVariants("fluid/levitite_blend/crystallize", 4)),

        LEVITITE_BLEND_EMPTY = REGISTRY.create("fluid.levitite_blend.empty", definition -> definition
            .defaultSubtitle(BUCKET_EMPTIES)
            .addFileVariants("fluid/levitite_blend/empty", 3)),

        LEVITITE_BLEND_FILL = REGISTRY.create("fluid.levitite_blend.fill", definition -> definition
            .defaultSubtitle(BUCKET_FILLS)
            .addFileVariants("fluid/levitite_blend/fill", 3)),

        LEVITITE_BREAK = REGISTRY.create("block.levitite.break", definition -> definition
                .defaultSubtitle(BLOCK_BROKEN)
                .addFileVariants("block/levitite/break", 4)),

        LEVITITE_PLACE = REGISTRY.create("block.levitite.place", definition -> definition
                .defaultSubtitle(BLOCK_PLACED)
                .addFileVariants("block/levitite/place", 4)),

        PROPELLER_LARGE_LOOP = REGISTRY.create("block.propeller_bearing.large_loop", definition -> definition
                .addFileVariant("block/propeller_bearing/large_loop", sound -> sound.setAttenuationDistance(48))),

        PROPELLER_SMALL_LOOP = REGISTRY.create("block.propeller_bearing.small_loop", definition -> definition
                .addFileVariant("block/propeller_bearing/small_loop", sound -> sound.setAttenuationDistance(48))),

        HOT_AIR_BURNER_HEAT = REGISTRY.create("block.hot_air_burner.head", definition -> definition
                .addFileVariant("block/hot_air_burner/burner_heat", sound -> sound.setAttenuationDistance(16))),

        HOT_AIR_BURNER_IDLE = REGISTRY.create("block.hot_air_burner.idle", definition -> definition
                .addFileVariant("block/hot_air_burner/burner_idle", sound -> sound.setAttenuationDistance(8))),

        STEAM_VENT_HEAT = REGISTRY.create("block.steam_vent.head", definition -> definition
                .addFileVariant("block/steam_vent/vent_heat", sound -> sound.setAttenuationDistance(16))),

        STEAM_VENT_IDLE = REGISTRY.create("block.steam_vent.idle", definition -> definition
                .addFileVariant("block/steam_vent/vent_idle", sound -> sound.setAttenuationDistance(8))),

        STEAM_VENT_OPEN = REGISTRY.create("block.steam_vent.open", definition -> definition
                .subtitle("Steam Vent whooshes")
                .addFileVariants("block/steam_vent/open", 3)),

        STEAM_VENT_CLOSE = REGISTRY.create("block.steam_vent.close", definition -> definition
                .subtitle("Steam Vent shuts")
                .addEventVariant(AllSoundEvents.FROGPORT_CLOSE)),

        // Item Sounds

        CLOUD_SKIPPER_TRANSFORM = REGISTRY.create("item.cloud_skipper_transform", SoundSource.AMBIENT, definition -> definition
                .subtitle("Music disc transforms")
                .addFileVariants("item/cloud_skipper_transform", 3)),

        // Entity Sounds

        GUST = REGISTRY.create("entity.gust", definition -> definition
                .subtitle("Balloon leaks")
                .addEventVariant(SoundEvents.WIND_CHARGE_BURST.value(), sound -> sound
                        .setAttenuationDistance(16)
                        .setPitch(0.3f)
                        .setVolume(0.5f))),

        // Music

        MUSIC_DISC_CLOUD_SKIPPER = song("music_disc.cloud_skipper", "music/cloud_skipper"),
        MUSIC_WINDSTREAM = song("music.windstream", "music/windstream"),
        MUSIC_CIRRUS = song("music.cirrus", "music/cirrus"),
        MUSIC_GLIDE = song("music.glide", "music/glide"),
        MUSIC_MAMMATUS = song("music.mammatus", "music/mammatus"),

        MUSIC_AIRSHIP_CLEAR = REGISTRY.create("music.clear", SoundSource.MUSIC,definition -> definition
                .addEventVariant(MUSIC_WINDSTREAM, sound -> sound.setWeight(5).setStream(true))
                .addEventVariant(MUSIC_CIRRUS, sound -> sound.setWeight(5).setStream(true))
                .addEventVariant(MUSIC_GLIDE, sound -> sound.setWeight(5).setStream(true))
                .addFileVariant(mc("music/game/swamp/aerie"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/floating_dream"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/infinite_amethyst"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/echo_in_the_wind"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/clark"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/subwoofer_lullaby"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/watcher"), UnaryOperator.identity())
        ),

        MUSIC_AIRSHIP_RAIN = REGISTRY.create("music.rain", SoundSource.MUSIC, definition -> definition
                .addEventVariant(MUSIC_MAMMATUS, sound -> sound.setWeight(5).setStream(true))
                .addEventVariant(MUSIC_WINDSTREAM, sound -> sound.setWeight(1).setStream(true))
                .addFileVariant(mc("music/game/swamp/aerie"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/swamp/labyrinthine"), UnaryOperator.identity())
                .addFileVariant(mc("music/game/water/axolotl"), UnaryOperator.identity())
        );

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static SimSoundEntry song(String id, String path) {
        return REGISTRY.create(id, SoundSource.MUSIC, definition -> definition
                .addFileVariant(path, sound -> sound
                        .setStream(true)));
    }

    public static void init() {

    }
}
