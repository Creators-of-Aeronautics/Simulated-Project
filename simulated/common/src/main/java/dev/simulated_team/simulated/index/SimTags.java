package dev.simulated_team.simulated.index;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.world.item.Items.*;

public class SimTags {

    public static void addGenerators() {
        Blocks.addGenerators();
        Items.addGenerators();
    }

    public static class Blocks {
        public static final TagKey<Block> NON_MOVABLE = create("non_movable");

        public static final TagKey<Block> SUPER_LIGHT = create("sable", "super_light");
        public static final TagKey<Block> LIGHT = create("sable", "light");
        public static final TagKey<Block> DIODE = create("sable", "diode");

        public static final TagKey<Block> AIRTIGHT = create("airtight");
        public static final TagKey<Block> NAMEPLATE_BLOCKS = create("nameplate_blocks");
        public static final TagKey<Block> SYMMETRIC_SAILS = create("symmetric_sails");
        public static final TagKey<Block> HANDLES = create("handles");

        private static TagKey<Block> create(final String path) {
            return TagKey.create(Registries.BLOCK, Simulated.path(path));
        }
        private static TagKey<Block> create(final String namespace, final String path) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
        }
        protected static void addGenerators() {
            Simulated.getRegistrate().addDataGenerator(ProviderType.BLOCK_TAGS, Blocks::genBlockTags);
        }
        private static void genBlockTags(final RegistrateTagsProvider<Block> provIn) {
            final TagGen.CreateTagsProvider<Block> prov = new TagGen.CreateTagsProvider<>(provIn, Block::builtInRegistryHolder);
            prov.tag(Blocks.NON_MOVABLE);

            prov.tag(SimTags.Blocks.SUPER_LIGHT).addTag(SimTags.Blocks.NAMEPLATE_BLOCKS);
            prov.tag(SimTags.Blocks.SUPER_LIGHT).addTag(HANDLES);
            prov.tag(AllTags.AllBlockTags.BRITTLE.tag).addTag(HANDLES);
            prov.tag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(HANDLES);

            prov.tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .addTag(NAMEPLATE_BLOCKS);
        }
    }

    public static class Items {
        public static final TagKey<Item> STONE = AllTags.commonItemTag("stones");
        public static final TagKey<Item> REDSTONE_DUST = AllTags.commonItemTag("dusts/redstone");
        public static final TagKey<Item> SLIME_BALLS = AllTags.commonItemTag("slime_balls");
        public static final TagKey<Item> AMETHYST_SHARDS = AllTags.commonItemTag("gems/amethyst");

        public static final TagKey<Item> NAMEPLATE_ITEMS = create("nameplate_items");
        public static final TagKey<Item> ROTATE_WITH_NAV_ARROW = create("rotate_with_nav_arrow");
        public static final TagKey<Item> DESTROYS_ROPE = create("destroys_rope");
        public static final TagKey<Item> MERGING_GLUE = create("merging_glue");
        public static final TagKey<Item> LASER_POINTER_LENS = create("laser_point_lens");
        public static final TagKey<Item> LASER_POINTER_RAINBOW = create("laser_point_rainbow");
        public static final TagKey<Item> HANDLE_VARIANTS = create("handle_variants");
        public static final TagKey<Item> SPRING_ADJUSTER = create("spring_adjuster");

        private static TagKey<Item> create(final String path) {
            return TagKey.create(Registries.ITEM, Simulated.path(path));
        }

        public static void addGenerators() {
            Simulated.getRegistrate().addDataGenerator(ProviderType.ITEM_TAGS, Items::genItemTags);
        }

        private static void genItemTags(final RegistrateTagsProvider<Item> provIn) {
            final TagGen.CreateTagsProvider<Item> prov = new TagGen.CreateTagsProvider<>(provIn, Item::builtInRegistryHolder);
            prov.tag(ROTATE_WITH_NAV_ARROW)
                    .add(COMPASS, RECOVERY_COMPASS)
                    .addOptional(ResourceLocation.fromNamespaceAndPath("naturescompass", "naturescompass"));
            prov.tag(ROTATE_WITH_NAV_ARROW)
                    .addOptional(ResourceLocation.fromNamespaceAndPath("explorerscompass", "explorerscompass"));
            prov.tag(DESTROYS_ROPE)
                    .add(SHEARS)
                    .add(AllItems.WRENCH.asItem());
            prov.tag(MERGING_GLUE)
                    .addTag(SLIME_BALLS);
            prov.tag(LASER_POINTER_LENS)
                    .addTag(AMETHYST_SHARDS);
            prov.tag(LASER_POINTER_RAINBOW)
                    .add(NETHER_STAR);
            prov.tag(SPRING_ADJUSTER)
                    .add(AllItems.IRON_SHEET.asItem());
        }
    }

    public static class Misc {
        public static final TagKey<MapDecorationType> NAV_TABLE_FINDABLE = TagKey.create(
                Registries.MAP_DECORATION_TYPE, Simulated.path("nav_table_findable"));

        // entities which won't block the placement of an armor stand
        public static final TagKey<EntityType<?>> ARMOR_STAND_IGNORE = TagKey.create(
                Registries.ENTITY_TYPE, Simulated.path("armor_stand_ignore")
        );

        //entities that will be ignored by laser pointers
        public static final TagKey<EntityType<?>> LASER_BLACKLIST = TagKey.create(
                Registries.ENTITY_TYPE, Simulated.path("laser_entity_blacklist")
        );
    }

    public static final Map<String, TagKey<Item>> DYE_MAP = new HashMap<>();
    static {
        for (final DyeColor color : DyeColor.values()) {
            DYE_MAP.put(color.getName(), AllTags.commonItemTag("dyes/" + color.getName()));
        }
    }

    public static void register() {

    }
}
