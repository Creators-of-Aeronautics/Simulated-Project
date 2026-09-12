package dev.eriksonn.aeronautics.neoforge.compat.jei;

import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.eriksonn.aeronautics.neoforge.index.AeroFluidsNeoForge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public final class AeroJEI implements IModPlugin {

    private static final ResourceLocation ID = Aeronautics.path("jei_plugin");
    private static final RecipeType<RecipeHolder<ConversionRecipe>> MYSTERY_CONVERSION =
            RecipeType.createRecipeHolderType(Create.asResource("mystery_conversion"));

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registration) {
        final ItemStack levititeBlend = AeroFluidsNeoForge.LEVITITE_BLEND.getBucket().get().getDefaultInstance();
        registration.addRecipes(MYSTERY_CONVERSION, List.of(
                createConversion("levitite", Ingredient.of(levititeBlend), AeroBlocks.LEVITITE.asStack()),
                createConversion("pearlescent_levitite", Ingredient.of(levititeBlend), AeroBlocks.PEARLESCENT_LEVITITE.asStack()),
                createConversion(
                        "music_disc_cloud_skipper",
                        Ingredient.of(AeroTags.ItemTags.CONVERTS_TO_CLOUD_SKIPPER),
                        AeroItems.MUSIC_DISC_CLOUD_SKIPPER.asStack()
                )
        ));
    }

    private static RecipeHolder<ConversionRecipe> createConversion(
            final String name,
            final Ingredient input,
            final ItemStack output
    ) {
        final ResourceLocation recipeId = Aeronautics.path("conversion_" + name);
        final ConversionRecipe recipe = new StandardProcessingRecipe.Builder<>(ConversionRecipe::new, recipeId)
                .withItemIngredients(input)
                .withSingleItemOutput(output)
                .build();
        return new RecipeHolder<>(recipeId, recipe);
    }
}
