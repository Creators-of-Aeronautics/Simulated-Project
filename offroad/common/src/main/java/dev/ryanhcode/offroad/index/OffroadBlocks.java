package dev.ryanhcode.offroad.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlockItem;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.config.server.OffroadStress;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlock;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelActor;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.simulated_team.simulated.data.SimBlockStateGen;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class OffroadBlocks {
    private static final SimulatedRegistrate REGISTRATE = Offroad.getRegistrate();

    public static final BlockEntry<BoreheadBearingBlock> BOREHEAD_BEARING_BLOCK =
            REGISTRATE.block("borehead_bearing", BoreheadBearingBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .requiresCorrectToolForDrops()
                            .noOcclusion())
                    .transform(pickaxeOnly())
                    .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                            .noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false))
                    .transform(pickaxeOnly())
                    .transform(OffroadStress.setImpact(8))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate(BlockStateGen.directionalAxisBlockProvider())
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("S")
                            .pattern("G")
                            .pattern("I")
                            .define('S', ItemTags.WOODEN_SLABS)
                            .define('G', AllBlocks.GEARBOX)
                            .define('I', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.INDUSTRIAL_IRON_BLOCK))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<RockCuttingWheelBlock> ROCK_CUTTER_BLOCK =
            REGISTRATE.block("rockcutting_wheel", RockCuttingWheelBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .requiresCorrectToolForDrops()
                            .noOcclusion())
                    .transform(pickaxeOnly())
                    .onRegister(movementBehaviour(new RockCuttingWheelActor()))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate(SimBlockStateGen::directionalAxisBlock)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("C")
                            .pattern("G")
                            .pattern("S")
                            .define('C', AllBlocks.CRUSHING_WHEEL)
                            .define('G', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                            .define('S', CommonMetal.IRON.plates)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.INDUSTRIAL_IRON_BLOCK))
                            .save(p))
                    .item()
                    .properties(x -> x.component(OffroadDataComponents.TIRE, TireLike.ROCKCUTTING_WHEEL))
                    .transform(customItemModel())
                    .lang("Rock Cutting Wheel")
                    .register();

    public static final BlockEntry<WheelMountBlock> WHEEL_MOUNT =
            REGISTRATE.block("wheel_mount", WheelMountBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                            .noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false))
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .blockstate(BlockStateGen.horizontalBlockProvider(true))
                    .transform(OffroadStress.setImpact(16.0))
                    .item(RollerBlockItem::new)
                    .transform(customItemModel())
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("C")
                            .pattern("S")
                            .pattern("P")
                            .define('C', AllBlocks.ANDESITE_CASING)
                            .define('S', SimItems.SPRING)
                            .define('P', CommonMetal.IRON.plates)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                            .save(p))
                    .register();

    public static void init() {
    }
}
