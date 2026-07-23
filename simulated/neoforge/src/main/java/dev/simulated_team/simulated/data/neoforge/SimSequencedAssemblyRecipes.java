package dev.simulated_team.simulated.data.neoforge;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class SimSequencedAssemblyRecipes extends SequencedAssemblyRecipeGen {

    private final GeneratedRecipe GYRO_MECHANISM = this.create("gyroscopic_mechanism", b -> b.require(CommonMetal.IRON.plates)
            .transitionTo(SimItems.INCOMPLETE_GYRO_MECHANISM)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllBlocks.COGWHEEL))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllBlocks.SHAFT))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(CommonMetal.BRASS.nuggets))
            .loops(5)
            .addOutput(SimItems.GYRO_MECHANISM, 200)
            .addOutput(AllItems.IRON_SHEET, 8)
            .addOutput(AllItems.ANDESITE_ALLOY, 8)
            .addOutput(AllItems.BRASS_NUGGET, 3)
            .addOutput(AllItems.CRUSHED_IRON, 2)
            .addOutput(Items.COMPASS.asItem(), 1));

    private final GeneratedRecipe ENGINE_ASSEMBLY = this.create("engine_assembly", b -> b.require(CommonMetal.IRON.plates)
            .transitionTo(SimItems.INCOMPLETE_ENGINE_ASSEMBLY)
            .addStep(CuttingRecipe::new, rb -> rb)
            .addStep(PressingRecipe::new, rb -> rb)
            .loops(8)
            .addOutput(SimItems.ENGINE_ASSEMBLY, 50)
            .addOutput(AllItems.IRON_SHEET, 16)
            .addOutput(Items.IRON_NUGGET, 15)
            .addOutput(AllBlocks.INDUSTRIAL_IRON_BLOCK, 10)
            .addOutput(Blocks.IRON_BARS, 8)
            .addOutput(Items.IRON_HELMET, 1)
    );

    public SimSequencedAssemblyRecipes(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Simulated.MOD_ID);
    }

    @Override
    public String getName() {
        return "Simulated's Splendid Sequenced Assembly Recipes";
    }
}
