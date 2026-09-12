package dev.simulated_team.simulated.data.neoforge;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class SimMechanicalCraftingRecipes extends MechanicalCraftingRecipeGen {
    private final GeneratedRecipe LINKED_TYPEWRITER = this.create(SimBlocks.LINKED_TYPEWRITER::get)
            .returns(1)
            .recipe(b -> b
                    .patternLine("BBBBT")
                    .patternLine("BBBBB")
                    .patternLine(" GPG ")
                    .key('B', Ingredient.of(ItemTags.BUTTONS))
                    .key('T', AllItems.TRANSMITTER)
                    .key('G', CommonMetal.GOLD.plates)
                    .key('P', AllItems.PRECISION_MECHANISM)
            );

    private final GeneratedRecipe PLUNGER_LAUNCHER = this.create(SimItems.PLUNGER_LAUNCHER::get)
            .returns(1)
            .recipe(b -> b
                    .patternLine("   P")
                    .patternLine("AMFR")
                    .patternLine("CC P")
                    .key('C', CommonMetal.COPPER.ingots)
                    .key('R', SimItems.ROPE_COUPLING)
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .key('M', AllItems.PRECISION_MECHANISM)
                    .key('P', Tags.Items.SLIME_BALLS)
                    .key('F', AllBlocks.FLUID_PIPE)
            );

    private final GeneratedRecipe DOCKING_CONNECTOR = this.create(SimBlocks.DOCKING_CONNECTOR::get)
            .returns(2)
            .recipe(b -> b
                    .patternLine("ICI")
                    .patternLine(" C ")
                    .patternLine("PAP")
                    .patternLine("BEB")
                    .key('B', CommonMetal.BRASS.plates)
                    .key('E', AllItems.ELECTRON_TUBE)
                    .key('P', Blocks.PISTON)
                    .key('A', AllBlocks.BRASS_CASING)
                    .key('C', AllBlocks.CHUTE)
                    .key('I', CommonMetal.IRON.plates)
            );


    public SimMechanicalCraftingRecipes(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Simulated.MOD_ID);
    }


    @Override
    public String getName() {
        return "Simulated's Marvelous Mechanical Crafting Recipes";
    }
}
