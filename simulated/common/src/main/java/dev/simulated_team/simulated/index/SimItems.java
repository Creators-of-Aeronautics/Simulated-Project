package dev.simulated_team.simulated.index;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramItem;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueItem;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItem;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import dev.simulated_team.simulated.content.items.spring.SpringItem;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.CreativeTabItemTransforms;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags;

public class SimItems {
    public static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final ItemEntry<DiagramItem> CONTRAPTION_DIAGRAM =
            REGISTRATE.item("contraption_diagram", DiagramItem::new)
                    .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                            .requires(Items.PAPER)
                            .requires(SimBlocks.PHYSICS_ASSEMBLER.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimBlocks.PHYSICS_ASSEMBLER.get()))
                            .save(p))
                    .register();

    public static final ItemEntry<SpringItem> SPRING =
            REGISTRATE.item("spring", SpringItem::new)
                    .recipe((ctx, prov) ->
                            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 2)
                                    .pattern("S")
                                    .pattern("N")
                                    .pattern("S")
                                    .define('S', AllItems.IRON_SHEET)
                                    .define('N', Items.IRON_NUGGET)
                                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.IRON_SHEET))
                                    .save(prov)
                    )
                    .register();

    public static ItemEntry<RopeItem> ROPE_COUPLING = REGISTRATE
            .item("rope_coupling", RopeItem::new)
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 1)
                    .pattern(" S ")
                    .pattern("NSN")
                    .pattern(" S ")
                    .define('S', Tags.Items.STRINGS)
                    .define('N', Tags.Items.NUGGETS_IRON)
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(Tags.Items.STRINGS))
                    .save(prov))
            .register();

    public static ItemEntry<Item> GYRO_MECHANISM = ingredient("gyroscopic_mechanism");

    public static ItemEntry<SequencedAssemblyItem> INCOMPLETE_GYRO_MECHANISM =
            REGISTRATE.item("incomplete_gyroscopic_mechanism", SequencedAssemblyItem::new)
                    .transform(CreativeTabItemTransforms.VisibilityType.INVISIBLE.applyItem())
                    .register();

    public static final ItemEntry<Item> ENGINE_ASSEMBLY = ingredient("engine_assembly");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_ENGINE_ASSEMBLY =
            REGISTRATE.item("incomplete_engine_assembly", SequencedAssemblyItem::new)
                    .transform(CreativeTabItemTransforms.VisibilityType.INVISIBLE.applyItem())
                    .register();

    static { REGISTRATE.addExtraItem(ResourceLocation.withDefaultNamespace("slime_ball")); }

    public static final ItemEntry<HoneyGlueItem> HONEY_GLUE =
            REGISTRATE.item("honey_glue", HoneyGlueItem::new)
                    .properties(p -> p.stacksTo(1)
                            .durability(100))
                    .tag(ItemTags.DURABILITY_ENCHANTABLE)
                    .register();

    public static final ItemEntry<PhysicsStaffItem> PHYSICS_STAFF =
            REGISTRATE.item("creative_physics_staff", PhysicsStaffItem::new)
                    .properties(p -> p.rarity(Rarity.EPIC).stacksTo(1))
                    .model(AssetLookup.itemModelWithPartials())
                    .register();

    public static final ItemEntry<PlungerLauncherItem> PLUNGER_LAUNCHER =
            REGISTRATE.item("plunger_launcher", PlungerLauncherItem::new)
                    .properties(p -> p.stacksTo(1).durability(200))
                    .model(AssetLookup.itemModelWithPartials())
                    .tag(Tags.Items.ENCHANTABLES, ItemTags.DURABILITY_ENCHANTABLE)
                    .register();

    private static ItemEntry<Item> ingredient(final String name) {
        return REGISTRATE.item(name, Item::new)
                .register();
    }

    private static ItemBuilder<Item, CreateRegistrate> ingredientNoRegister(final String name) {
        return REGISTRATE.item(name, Item::new);
    }

    public static void register() {

    }
}
