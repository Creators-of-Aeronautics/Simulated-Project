package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class AeroMechanicalCraftingRecipes extends MechanicalCraftingRecipeGen {
	public AeroMechanicalCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, Aeronautics.MOD_ID);
	}

	private final GeneratedRecipe MOUNTED_POTATO_CANNON = this.create(AeroBlocks.MOUNTED_POTATO_CANNON::get)
			.returns(1)
			.recipe(b -> b
					.patternLine("SR  ")
					.patternLine("KCPP")
					.patternLine("SR  ")
					.key('S', CommonMetal.COPPER.plates)
					.key('R', Tags.Items.DUSTS_REDSTONE)
					.key('K', Blocks.DRIED_KELP_BLOCK)
					.key('C', AllBlocks.COGWHEEL)
					.key('P', AllBlocks.FLUID_PIPE)
			);

	@Override
	public String getName() {
		return "Aero's Mischievous Mechanical Crafting Recipes";
	}
}
