package dev.ryanhcode.offroad.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.content.items.tire.TireItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class OffroadItems {
    private static final SimulatedRegistrate REGISTRATE = Offroad.getRegistrate();

    public static final ItemEntry<TireItem> SMALL_TIRE = REGISTRATE.item("small_tire", TireItem::new)
            .properties(x -> x.component(OffroadDataComponents.TIRE, TireLike.SMALL_TIRE))
            .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                    .requires(AllBlocks.SHAFT)
                    .requires(Items.DRIED_KELP)
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.SHAFT.get()))
                    .save(p))
            .model(AssetLookup.itemModelWithPartials())
            .register();

    public static final ItemEntry<TireItem> TIRE = REGISTRATE.item("tire", TireItem::new)
            .properties(x -> x.component(OffroadDataComponents.TIRE, TireLike.TIRE))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                    .pattern(" K ")
                    .pattern("KSK")
                    .pattern(" K ")
                    .define('K', Items.DRIED_KELP.asItem())
                    .define('S', AllBlocks.SHAFT.asItem())
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.SHAFT.get()))
                    .save(p))
            .model(AssetLookup.itemModelWithPartials())
            .register();

    public static final ItemEntry<TireItem> LARGE_TIRE = REGISTRATE.item("large_tire", TireItem::new)
            .properties(x -> x.component(OffroadDataComponents.TIRE, TireLike.LARGE_TIRE))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                    .pattern(" B ")
                    .pattern("BSB")
                    .pattern(" B ")
                    .define('B', AllBlocks.BELT.asItem())
                    .define('S', AllBlocks.SHAFT.asItem())
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.SHAFT.get()))
                    .save(p))
            .model(AssetLookup.itemModelWithPartials())
            .register();

    public static final ItemEntry<TireItem> MONSTROUS_TIRE = REGISTRATE.item("monstrous_tire", TireItem::new)
            .properties(x -> x.component(OffroadDataComponents.TIRE, TireLike.MONSTROUS_TIRE))
            .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                    .pattern(" K ")
                    .pattern("KSK")
                    .pattern(" K ")
                    .define('K', Blocks.DRIED_KELP_BLOCK.asItem())
                    .define('S', AllBlocks.SHAFT.asItem())
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.SHAFT.get()))
                    .save(p))
            .model(AssetLookup.itemModelWithPartials())
            .register();

    public static void init() {
    }
}
