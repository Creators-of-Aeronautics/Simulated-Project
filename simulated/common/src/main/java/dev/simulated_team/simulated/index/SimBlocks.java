package dev.simulated_team.simulated.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.item.ItemDescription;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.config.server.blocks.SimStress;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlock;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorMovementBehaviour;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerCogBlock;
import dev.simulated_team.simulated.content.blocks.auger_shaft.AugerShaftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftGenerator;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.content.blocks.docking_connector.PairedDockingConnectorBlock;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlock;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlock;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlock;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlock;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlock;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlock;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlock;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockTarget;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterItem;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlock;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlock;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlockStateGen;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlock;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlock;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlock;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelGenerator;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlock;
import dev.simulated_team.simulated.data.SimBlockStateGen;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.CreativeTabItemTransforms;
import dev.simulated_team.simulated.service.SimBlockStateService;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.*;

@SuppressWarnings("removal")
public class SimBlocks {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final BlockEntry<PhysicsAssemblerBlock> PHYSICS_ASSEMBLER = REGISTRATE.block("physics_assembler", PhysicsAssemblerBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
                    .item()
                    .transform(customItemModel())
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("   ")
                            .pattern(" N ")
                            .pattern("ARA")
                            .define('A', AllItems.ANDESITE_ALLOY.get())
                            .define('N', Items.LEVER)
                            .define('R', AllBlocks.ANDESITE_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING.get()))
                            .save(p))
                    .register();

    public static final BlockEntry<SwivelBearingBlock> SWIVEL_BEARING =
            REGISTRATE.block("swivel_bearing", SwivelBearingBlock::new)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .initialProperties(SharedProperties::netheriteMetal)
                    .properties((properties -> properties.destroyTime(5f)))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(),
                            blockState -> prov.models().getExistingFile(
                                    prov.modLoc("block/swivel_bearing/block" + (blockState.getValue(SwivelBearingBlock.ASSEMBLED) ? "_assembled" : "")))))
                    .transform(SimStress.setImpact(4.0))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .item().transform(customItemModel("swivel_bearing", "item"))
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern(" A ")
                            .pattern(" B ")
                            .pattern(" C ")
                            .define('A', ItemTags.WOODEN_SLABS)
                            .define('B', AllBlocks.INDUSTRIAL_IRON_BLOCK.get())
                            .define('C', AllBlocks.COGWHEEL.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING.get()))
                            .save(p))
                    .register();

    public static final BlockEntry<SwivelBearingPlateBlock> SWIVEL_BEARING_LINK_BLOCK =
            REGISTRATE.block("swivel_bearing_link_block", SwivelBearingPlateBlock::new)
                    .blockstate((ctx, prov) ->
                            prov.directionalBlock(ctx.getEntry(), blockState -> prov.models().getExistingFile(prov.modLoc("block/swivel_bearing/bearing_plate"))))
                    .initialProperties(SharedProperties::netheriteMetal)
                    .properties(properties -> properties
                            .destroyTime(5f))
                    .loot((p, b) -> p.dropOther(b, SWIVEL_BEARING.get()))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

    public static final BlockEntry<MergingGlueBlock> MERGING_GLUE =
            REGISTRATE.block("merging_glue", MergingGlueBlock::new)
                    .blockstate((ctx, prov) ->
                            prov.directionalBlock(ctx.getEntry(), blockState -> prov.models().getExistingFile(prov.modLoc("block/merging_glue/block"))))
                    .properties(p -> p.noOcclusion().instabreak().mapColor(MapColor.GRASS).friction(0.8F).sound(SoundType.SLIME_BLOCK))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .register();

    public static final BlockEntry<RopeWinchBlock> ROPE_WINCH =
            REGISTRATE.block("rope_winch", RopeWinchBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate(SimBlockStateGen::directionalKineticAxisBlockstate)
                    .properties(Block.Properties::noOcclusion)
                    .transform(SimStress.setImpact(4.0))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("I")
                            .pattern("H")
                            .pattern("S")
                            .define('I', AllItems.IRON_SHEET)
                            .define('H', AllBlocks.SHAFT)
                            .define('S', AllBlocks.INDUSTRIAL_IRON_BLOCK.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.IRON_SHEET))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<RopeConnectorBlock> ROPE_CONNECTOR =
            REGISTRATE.block("rope_connector", RopeConnectorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate(SimBlockStateGen::directionalAxisBlock)
                    .properties(Block.Properties::noOcclusion)
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE, AllTags.AllBlockTags.BRITTLE.tag, SimTags.Blocks.SUPER_LIGHT)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("I")
                            .pattern("S")
                            .define('I', AllItems.IRON_SHEET)
                            .define('S', AllBlocks.INDUSTRIAL_IRON_BLOCK.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.IRON_SHEET))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<HandleBlock> IRON_HANDLE = createHandle(null, HandleBlock.Variant.IRON)
            .recipe((c, p) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                    .pattern("N")
                    .pattern("A")
                    .define('N', Items.IRON_NUGGET)
                    .define('A', AllItems.ANDESITE_ALLOY.get())
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                    .save(p);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                    .requires(SimTags.Items.HANDLE_VARIANTS)
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimTags.Items.HANDLE_VARIANTS))
                    .save(p, Simulated.path("handle_undye"));
            })
            .register();

    public static final BlockEntry<HandleBlock> COPPER_HANDLE = createHandle(null, HandleBlock.Variant.COPPER)
            .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                    .requires(IRON_HANDLE)
                    .requires(AllItems.COPPER_NUGGET)
                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(IRON_HANDLE))
                    .group("handle_variants")
                    .save(p))
            .transform(CreativeTabItemTransforms.VisibilityType.SEARCH_ONLY.applyBlock())
            .register();

    public static final DyedBlockList<HandleBlock> DYED_HANDLES = new DyedBlockList<>(color -> {
        return createHandle(color, HandleBlock.Variant.DYED)
                .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                        .requires(IRON_HANDLE)
                        .requires(DyeItem.byColor(color))
                        .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(IRON_HANDLE))
                        .group("handle_variants")
                        .save(p))
                .transform(CreativeTabItemTransforms.VisibilityType.SEARCH_ONLY.applyBlock())
                .register();
    });

    public static final BlockEntry<DirectionalGearshiftBlock> DIRECTIONAL_GEARSHIFT = REGISTRATE
            .block("directional_gearshift", DirectionalGearshiftBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion()
                    .isRedstoneConductor(SimBlocks::never)
                    .mapColor(MapColor.PODZOL))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(axeOrPickaxe())
            .recipe((c, p) -> {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get())
                        .requires(AllBlocks.ANDESITE_CASING)
                        .requires(AllBlocks.COGWHEEL)
                        .requires(Items.REDSTONE_TORCH)
                        .requires(AllBlocks.SHAFT)
                        .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                        .save(p);
            })
            .blockstate(DirectionalGearshiftGenerator::generate)
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<TorsionSpringBlock> TORSION_SPRING =
            REGISTRATE.block("torsion_spring", TorsionSpringBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate((c, p) -> p.directionalBlock(c.get(),
                            blockState -> p.models().getExistingFile(p.modLoc("block/torsion_spring/block_" + (blockState.getValue(TorsionSpringBlock.VARIANT).getSerializedName())))))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .transform(SimStress.setImpact(16.0))
                    .transform(SimStress.setCapacity(8.0))
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("A")
                            .pattern("S")
                            .pattern("C")
                            .define('A', AllBlocks.SHAFT)
                            .define('S', SimItems.SPRING)
                            .define('C', AllBlocks.ANDESITE_CASING)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                            .save(p))
                    .item().transform(customItemModel())
                    .register();
    public static final BlockEntry<AugerShaftBlock> AUGER_SHAFT =
            REGISTRATE.block("auger_shaft", AugerShaftBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate(SimBlockStateService.INSTANCE.augerShaftGenerate("auger_shaft", false))
                    .transform(pickaxeOnly())
                    .transform(SimStress.setImpact(0.5))
                    .recipe((c, p) -> {
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 2)
                                .requires(AllBlocks.CHUTE)
                                .requires(AllBlocks.SHAFT)
                                .requires(AllItems.IRON_SHEET)
                                .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.IRON_SHEET))
                                .save(p);

                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                                .requires(SimBlocks.AUGER_COG.get())
                                .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.IRON_SHEET))
                                .save(p, Simulated.path(c.getName() + "_from_auger_cogwheel"));
                    })
                    .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .transform(DisplaySource.displaySource(SimDisplaySources.AUGER_DISPLAY))
                    .item().transform(customItemModel())
                    .register();
    public static final BlockEntry<AugerCogBlock> AUGER_COG =
            REGISTRATE.block("auger_cog", AugerCogBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate(SimBlockStateService.INSTANCE.augerShaftGenerate("auger_cog", true))
                    .lang("Auger Cogwheel")
                    .transform(pickaxeOnly())
                    .transform(SimStress.setImpact(0.5))
                    .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                            .requires(SimBlocks.AUGER_SHAFT.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.IRON_SHEET))
                            .save(p, Simulated.path(c.getName() + "_from_auger_shaft")))
                    .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .transform(DisplaySource.displaySource(SimDisplaySources.AUGER_DISPLAY))
                    .item().transform(customItemModel())
                    .register();


    public static final DyedBlockList<PortableEngineBlock> PORTABLE_ENGINES = new DyedBlockList<>(color -> {
        final String colorName = color.getSerializedName();

        if (color == DyeColor.RED) {
            return createPortableEngine(color)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("G")
                            .pattern("E")
                            .pattern("B")
                            .define('G', AllItems.IRON_SHEET.get())
                            .define('E', SimItems.ENGINE_ASSEMBLY)
                            .define('B', Blocks.BLAST_FURNACE)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING.get()))
                            .save(p))
                    .item()
                    .model((c, p) -> p
                            .withExistingParent(colorName + "_portable_engine", p.modLoc("block/portable_engine/item"))
                            .texture("0", p.modLoc("block/portable_engine/" + colorName))
                            .texture("particle", p.modLoc("block/portable_engine/" + colorName))
                    )
                    .build()
                    .register();
        } else {
            return createPortableEngine(color)
                    .transform(CreativeTabItemTransforms.VisibilityType.SEARCH_ONLY.applyBlock())
                    .item()
                    .model((c, p) -> p
                            .withExistingParent(colorName + "_portable_engine", p.modLoc("block/portable_engine/item"))
                            .texture("0", p.modLoc("block/portable_engine/" + colorName))
                            .texture("particle", p.modLoc("block/portable_engine/" + colorName))
                    )
                    .build()
                    .register();
        }
    });

    public static final ItemLike RED_PORTABLE_ENGINE = PORTABLE_ENGINES.get(DyeColor.RED);

    private static BlockBuilder<PortableEngineBlock, CreateRegistrate> createPortableEngine(final DyeColor color) {
        final String colorName = color.getSerializedName();
        return REGISTRATE.block(colorName + "_portable_engine", p -> new PortableEngineBlock(p, color))
                .initialProperties(SharedProperties::stone)
                .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).lightLevel((state) -> PortableEngineBlock.isLitState(state) ? 6 : 0))
                .properties(BlockBehaviour.Properties::noOcclusion)
                .addLayer(() -> RenderType::cutoutMipped)
                .blockstate((c, p) -> p.horizontalBlock(c.get(), blockState -> p.models()
                        .withExistingParent(colorName + "_portable_engine", p.modLoc("block/portable_engine/block"))
                                .texture("0", p.modLoc("block/portable_engine/" + colorName))
                                .texture("particle", p.modLoc("block/portable_engine/" + colorName))
                        ))
                .transform(SimStress.setCapacity(64.0))
                .onRegister(BlockStressValues.setGeneratorSpeed(32))
                .transform(DisplaySource.displaySource(SimDisplaySources.PORTABLE_ENGINE_DISPLAY))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public static final BlockEntry<LaserPointerBlock> LASER_POINTER =
            REGISTRATE.block("laser_pointer", LaserPointerBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .blockstate(SimBlockStateGen::facingPoweredAxisBlockstate)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("A")
                            .pattern("T")
                            .pattern("C")
                            .define('A', SimTags.Items.LASER_POINTER_LENS)
                            .define('T', Items.REDSTONE_TORCH)
                            .define('C', AllBlocks.ANDESITE_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                            .save(p))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .item().transform(customItemModel())
                    .register();

    public static final BlockEntry<LaserSensorBlock> LASER_SENSOR =
            REGISTRATE.block("laser_sensor", LaserSensorBlock::new)
                    .transform(DisplaySource.displaySource(SimDisplaySources.LASER_SENSOR_DISPLAY))
                    .initialProperties(SharedProperties::wooden)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate((c, p) -> SimBlockStateService.INSTANCE.genericModelBuilder(c, p, SimBlockStateGen::xyLaser, (state) -> AssetLookup.forPowered(c, p).apply(state)))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("G")
                            .pattern("A")
                            .pattern("C")
                            .define('G', Blocks.TINTED_GLASS)
                            .define('A', SimTags.Items.LASER_POINTER_LENS)
                            .define('C', AllBlocks.ANDESITE_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<VelocitySensorBlock> VELOCITY_SENSOR =
            REGISTRATE.block("velocity_sensor", VelocitySensorBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .transform(axeOrPickaxe())
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate(SimBlockStateGen::directionalAxisBlock)
                    .transform(DisplaySource.displaySource(SimDisplaySources.VELO_DISPLAY))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("P")
                            .pattern("B")
                            .pattern("A")
                            .define('P', AllItems.PROPELLER)
                            .define('B', Blocks.BARREL)
                            .define('A', AllBlocks.ANDESITE_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING.get()))
                            .save(p))
                    .item().transform(customItemModel())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .register();

    public static final BlockEntry<AltitudeSensorBlock> ALTITUDE_SENSOR =
            REGISTRATE.block("altitude_sensor", AltitudeSensorBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .blockstate((c, p) -> SimBlockStateService.INSTANCE.genericModelBuilder(c, p,
                            SimBlockStateGen::xyAltitudeSensor,
                            (state) -> p.models().getExistingFile(p.modLoc("block/" + c.getName() + "/block"))))
                    .transform(DisplaySource.displaySource(SimDisplaySources.ALTITUDE_SENSOR_DISPLAY))
                    .onRegister(movementBehaviour(new AltitudeSensorMovementBehaviour()))
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("P")
                            .pattern("S")
                            .pattern("A")
                            .define('P', Items.PAPER)
                            .define('S', AllItems.IRON_SHEET)
                            .define('A', AllBlocks.ANDESITE_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING.get()))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<GimbalSensorBlock> GIMBAL_SENSOR =
            REGISTRATE.block("gimbal_sensor", GimbalSensorBlock::new)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .transform(DisplaySource.displaySource(SimDisplaySources.GIMBAL_DISPLAY))
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("C")
                            .pattern("G")
                            .pattern("B")
                            .define('C', Items.COMPASS)
                            .define('G', SimItems.GYRO_MECHANISM)
                            .define('B', AllBlocks.BRASS_CASING)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimItems.GYRO_MECHANISM.get()))
                            .save(p)
                    )
                    .blockstate(BlockStateGen.horizontalAxisBlockProvider(true))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<NavTableBlock> NAVIGATION_TABLE =
            REGISTRATE.block("navigation_table", NavTableBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .transform(axeOrPickaxe())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .blockstate((c, p) -> p.directionalBlock(c.get(), blockState -> p.models()
                            .getExistingFile(p.modLoc("block/navigation_table/block"))))
                    .transform(DisplaySource.displaySource(SimDisplaySources.NAV_TABLE_DISPLAY))
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("S")
                            .pattern("P")
                            .pattern("B")
                            .define('S', AllItems.BRASS_SHEET)
                            .define('P', AllItems.PRECISION_MECHANISM)
                            .define('B', AllBlocks.BRASS_CASING)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.PRECISION_MECHANISM))
                            .save(p))
                    .item().transform(customItemModel())
                    .register();

    public static final BlockEntry<OpticalSensorBlock> OPTICAL_SENSOR =
            REGISTRATE.block("optical_sensor", OpticalSensorBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .transform(DisplaySource.displaySource(SimDisplaySources.OPTICAL_SENSOR_DISPLAY))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate((c, p) -> SimBlockStateService.INSTANCE.genericModelBuilder(c, p, SimBlockStateGen::xyLaser, (state) -> AssetLookup.forPowered(c, p).apply(state)))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .item().transform(customItemModel())
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern(" A ")
                            .pattern(" C ")
                            .pattern(" B ")
                            .define('A', Items.AMETHYST_SHARD)
                            .define('B', AllBlocks.BRASS_CASING.get())
                            .define('C', AllItems.ELECTRON_TUBE.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING.get()))
                            .save(p))
                    .register();

    public static final BlockEntry<DockingConnectorBlock> DOCKING_CONNECTOR =
            REGISTRATE.block("docking_connector", DockingConnectorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .transform(pickaxeOnly())
                    .properties(p -> p
                            .sound(SoundType.NETHERITE_BLOCK)
                            .isRedstoneConductor(SimBlocks::never)
                            .forceSolidOn()
                    )
                    .addLayer(() -> RenderType::cutoutMipped)
                    .transform(DisplaySource.displaySource(SimDisplaySources.DOCKING_CONNECTOR_DISPLAY))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .properties(BlockBehaviour.Properties::dynamicShape)
                    .blockstate(SimBlockStateGen::facingPoweredAxisBlockstate)
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<PairedDockingConnectorBlock> PAIRED_DOCKING_CONNECTOR =
            REGISTRATE.block("paired_docking_connector", PairedDockingConnectorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .blockstate((c, p) -> p.simpleBlock(c.get(), p.models().getExistingFile(p.modLoc("block/docking_connector/block"))))
                    .transform(pickaxeOnly())
                    .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
                    .properties(properties -> properties
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.BLOCK)
                            .forceSolidOff())
                    .register();

    public static final BlockEntry<AnalogTransmissionBlock> ANALOG_TRANSMISSION =
            REGISTRATE.block("analog_transmission", AnalogTransmissionBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties((p) -> p.noOcclusion()
                            .isRedstoneConductor(SimBlocks::never))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, s -> {
                        String suffix = s.getValue(AnalogTransmissionBlock.POWERED) ? "_on" : "";
                        ResourceLocation path = Simulated.path("block/" + c.getName() + "/block" + suffix);
                        return p.models().getExistingFile(path);
                    }))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .item().transform(customItemModel())
                    .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                            .requires(AllBlocks.BRASS_CASING.get())
                            .requires(AllBlocks.SHAFT.get())
                            .requires(AllBlocks.COGWHEEL.get())
                            .requires(AllItems.ELECTRON_TUBE.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING.get()))
                            .save(p))
                    .register();

    public static final BlockEntry<SteeringWheelBlock> STEERING_WHEEL =
            REGISTRATE.block("steering_wheel", SteeringWheelBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .properties(p -> p.mapColor(MapColor.PODZOL))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .transform(axeOrPickaxe())
                    .blockstate(new SteeringWheelGenerator()::generate)
                    .onRegister(ItemUseOverrides::addBlock)
                    .transform(SimStress.setCapacity(16.0))
                    .onRegister(BlockStressValues.setGeneratorSpeed(SteeringWheelBlockEntity.RPM))
                    .tag(SimTags.Blocks.LIGHT)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("C")
                            .pattern("A")
                            .pattern("S")
                            .define('C', AllBlocks.LARGE_COGWHEEL)
                            .define('A', AllBlocks.ANDESITE_CASING)
                            .define('S', AllBlocks.SHAFT.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.SHAFT.get()))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<ThrottleLeverBlock> THROTTLE_LEVER =
            REGISTRATE.block("throttle_lever", ThrottleLeverBlock::new)
                    .initialProperties(() -> Blocks.LEVER)
                    .transform(axeOrPickaxe())
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("S")
                            .pattern("B")
                            .define('S', Items.STICK)
                            .define('B', AllBlocks.BRASS_CASING)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING.get()))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<LinkedTypewriterBlock> LINKED_TYPEWRITER =
            REGISTRATE.block("linked_typewriter", LinkedTypewriterBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate((ctx, prov) -> {
                        prov.horizontalBlock(ctx.get(), blockState -> prov.models()
                                .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : ""))));
                    })
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .transform(DisplaySource.displaySource(SimDisplaySources.LINKED_TYPEWRITER__DISPLAY))
                    .transform(axeOrPickaxe())
                    .item(LinkedTypewriterItem::new)
                    .transform(customItemModel())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .register();

    public static final BlockEntry<DirectionalLinkedReceiverBlock> DIRECTIONAL_LINKED_RECEIVER =
            REGISTRATE.block("directional_linked_receiver", DirectionalLinkedReceiverBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate(SimBlockStateGen::facingPoweredAxisBlockstate)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag, SimTags.Blocks.SUPER_LIGHT)
                    .transform(axeOrPickaxe())
                    .item().transform(customItemModel())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("A")
                            .pattern("B")
                            .pattern("C")
                            .define('A', AllItems.TRANSMITTER.get())
                            .define('B', AllItems.IRON_SHEET.get())
                            .define('C', AllBlocks.BRASS_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING.get()))
                            .save(p))
                    .register();

    public static final BlockEntry<ModulatingLinkedReceiverBlock> MODULATING_LINKED_RECEIVER =
            REGISTRATE.block("modulating_linked_receiver", ModulatingLinkedReceiverBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate(SimBlockStateGen::facingPoweredAxisBlockstate)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag, SimTags.Blocks.SUPER_LIGHT) //Dono what this tag means (contraption safe?).
                    .transform(axeOrPickaxe())
                    .item().transform(customItemModel())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("A")
                            .pattern("B")
                            .pattern("C")
                            .define('A', AllItems.TRANSMITTER.get())
                            .define('B', AllItems.GOLDEN_SHEET.get())
                            .define('C', AllBlocks.BRASS_CASING.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING.get()))
                            .save(p))
                    .register();

    public static final BlockEntry<RedstoneAccumulatorBlock> REDSTONE_ACCUMULATOR =
            REGISTRATE.block("redstone_accumulator", RedstoneAccumulatorBlock::new)
                    .initialProperties(() -> Blocks.REPEATER)
                    .blockstate(RedstoneAccumulatorBlockStateGen.generate())
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag, SimTags.Blocks.DIODE)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern(" Q ")
                            .pattern("RBT")
                            .pattern("SSS")
                            .define('T', Blocks.REDSTONE_TORCH)
                            .define('B', AllItems.BRASS_SHEET.get())
                            .define('R', SimTags.Items.REDSTONE_DUST)
                            .define('Q', AllItems.POLISHED_ROSE_QUARTZ)
                            .define('S', SimTags.Items.STONE)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimTags.Items.REDSTONE_DUST))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .register();

    public static final BlockEntry<RedstoneInductorBlock> REDSTONE_INDUCTOR =
            REGISTRATE.block("redstone_inductor", RedstoneInductorBlock::new)
                    .initialProperties(() -> Blocks.REPEATER)
                    .blockstate(SimBlockStateGen::redstoneInductorBlockstate)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag, SimTags.Blocks.DIODE)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern(" C ")
                            .pattern("RBT")
                            .pattern("SSS")
                            .define('T', Blocks.REDSTONE_TORCH)
                            .define('R', SimTags.Items.REDSTONE_DUST)
                            .define('B', AllItems.BRASS_SHEET.get())
                            .define('C', AllItems.COPPER_SHEET.get())
                            .define('S', SimTags.Items.STONE)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimTags.Items.REDSTONE_DUST))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .addLayer(() -> RenderType::cutoutMipped)
                    .register();

/*    public static final BlockEntry<AbsorberBlock> ABSORBER =
            REGISTRATE.block("absorber", AbsorberBlock::new)
                    .initialProperties(SharedProperties::copperMetal)
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate((ctx, prov) -> {
                        prov.horizontalBlock(ctx.get(), blockState -> prov.models()
                                .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block" + (blockState.getValue(BlockStateProperties.POWERED) ? "_powered" : ""))));
                    })
//                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
//                            .pattern("G")
//                            .pattern("S")
//                            .pattern("R")
//                            .define('G', Blocks.COPPER_GRATE)
//                            .define('S', Blocks.SPONGE)
//                            .define('R', Items.REDSTONE)
//                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(SimTags.Items.REDSTONE_DUST))
//                            .save(p))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .item().transform(customItemModel())
                    .register();
 */
    public static final BlockEntry<RedstoneMagnetBlock> REDSTONE_MAGNET =
            REGISTRATE.block("redstone_magnet", RedstoneMagnetBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .addLayer(() -> RenderType::cutoutMipped)
                    .transform(pickaxeOnly())
                    .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .blockstate(SimBlockStateGen::facingPoweredAxisBlockstate)
                    .item()
                    .tag(SimTags.Items.ROTATE_WITH_NAV_ARROW)
                    .transform(customItemModel())
                    .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 1)
                            .requires(Items.REDSTONE)
                            .requires(AllItems.COPPER_SHEET.get())
                            .requires(AllBlocks.INDUSTRIAL_IRON_BLOCK.get())
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(Items.COPPER_INGOT))
                            .save(p))
                    .register();

    public static final BlockEntry<SymmetricSailBlock> WHITE_SYMMETRIC_SAIL =
            REGISTRATE.block("white_symmetric_sail", (prov) -> SymmetricSailBlock.withCanvas(prov, DyeColor.WHITE))
                    .initialProperties(SharedProperties::wooden)
                    .properties(p -> p.sound(SoundType.SCAFFOLDING))
                    .properties(BlockBehaviour.Properties::noOcclusion)
                    .lang("Symmetric Sail")
                    .blockstate((c, p) ->
                            SimBlockStateService.INSTANCE.genericModelBuilder(c, p, SimBlockStateGen::xySymmetricSail,
                                    (state) -> p.models().getExistingFile(Simulated.path("block/symmetric_sail/block"))))
                    .recipe((c, p) -> {
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get(), 2)
                                .requires(AllBlocks.SAIL.get())
                                .requires(AllBlocks.SAIL.get())
                                .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.SAIL.get()))
                                .save(p);

                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllBlocks.SAIL.get(), 1)
                                .requires(c.get())
                                .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(c.get()))
                                .save(p);
                    })
                    .tag(BlockTags.MINEABLE_WITH_AXE, AllTags.AllBlockTags.WINDMILL_SAILS.tag, SimTags.Blocks.SYMMETRIC_SAILS)
                    .item()
                    .transform(customItemModel("symmetric_sail/item"))
                    .register();

    public static final DyedBlockList<SymmetricSailBlock> DYED_SYMMETRIC_SAILS = new DyedBlockList<>(colour -> {
        if (colour == DyeColor.WHITE) {
            return WHITE_SYMMETRIC_SAIL;
        } else {
            String colorName = colour.getSerializedName();
            return REGISTRATE.block(colorName + "_symmetric_sail", p -> SymmetricSailBlock.withCanvas(p, colour))
                    .initialProperties(SharedProperties::wooden)
                    .properties(p -> p.sound(SoundType.SCAFFOLDING))
                    .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, blockState -> p.models()
                            .withExistingParent(colorName + "_symmetric_sail",
                                    p.modLoc("block/symmetric_sail/block"))
                            .texture("0", Create.asResource("block/sail/canvas_" + colorName))
                            .texture("1", p.modLoc("block/symmetric_sail/side_" + colorName))
                            .texture("particle", Create.asResource("block/sail/canvas_" + colorName))))
                    .tag(BlockTags.MINEABLE_WITH_AXE, AllTags.AllBlockTags.WINDMILL_SAILS.tag, SimTags.Blocks.SYMMETRIC_SAILS)
                    .loot((p, b) -> p.dropOther(b, WHITE_SYMMETRIC_SAIL.asItem()))
                    .register();
        }
    });

    public static final DyedBlockList<NameplateBlock> NAMEPLATES = new DyedBlockList<>(color -> {
        String colorName = color.getSerializedName();
        return REGISTRATE.block(colorName + "_nameplate", p -> new NameplateBlock(p, color))
                .initialProperties(SharedProperties::wooden)
                .transform(axeOnly())
                .tag(SimTags.Blocks.NAMEPLATE_BLOCKS)
                .addLayer(() -> RenderType::cutoutMipped)
                .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.get(), state -> {
                    NameplateBlock.Position position = state.getValue(NameplateBlock.POSITION);
                    return prov.models()
                            .withExistingParent(colorName + "_nameplate_" + position.getSerializedName(), prov.modLoc("block/nameplate/block_" + position.getSerializedName()))
                            .texture("0", Simulated.path("block/nameplate/" + colorName + "_nameplate"))
                            .texture("particle", Simulated.path("block/nameplate/" + colorName + "_nameplate"));
                }))
                .transform(DisplayTarget.displayTarget(NameplateBlockTarget.NAMEPLATE))
                .transform(x -> {
                    // Only a recipe for the white one
                    return x.recipe((c, p) -> {
                        if (color == DyeColor.WHITE) {
                            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get(), 4)
                                    .requires(Items.PAPER)
                                    .requires(Items.STICK)
                                    .requires(AllItems.ANDESITE_ALLOY)
                                    .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY.get()))
                                    .save(p);
                        }
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get())
                                .requires(SimTags.DYE_MAP.get(colorName))
                                .requires(SimTags.Items.NAMEPLATE_ITEMS)
                                .unlockedBy("has_nameplate", RegistrateRecipeProvider.has(SimTags.Items.NAMEPLATE_ITEMS))
                                .save(p, Simulated.path("crafting/" + c.getName() + "_from_other_nameplate"));
                    });
                })
                .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.simulated.nameplate"))
                .item()
                .tag(SimTags.Items.NAMEPLATE_ITEMS)
                .transform(b -> b.model(SimBlockStateGen.coloredBlockItemModel("nameplate/" + colorName + "_nameplate", "nameplate/item")).build())
                .transform(CreativeTabItemTransforms.VisibilityType.SEARCH_ONLY.conditionalApplyBlock(() -> !colorName.equals(DyeColor.WHITE.getSerializedName())))
                .register();
    });

    public static final BlockEntry<SpringBlock> SPRING =
            REGISTRATE.block("spring", SpringBlock::new)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .transform(pickaxeOnly())
                    .initialProperties(SharedProperties::softMetal)
                    .blockstate((ctx, prov) -> prov.directionalBlock(ctx.getEntry(),
                            blockState -> prov.models().getExistingFile(
                                    prov.modLoc("block/spring/" + (blockState.getValue(SpringBlock.SIZE) == SpringBlock.Size.MEDIUM ? "" : (blockState.getValue(SpringBlock.SIZE).getSerializedName() + "_")) + "block"))))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag, AllTags.AllBlockTags.BRITTLE.tag, SimTags.Blocks.LIGHT)
                    .loot((tables, block) -> {
                        tables.add(block, tables.createSingleItemTable(SimItems.SPRING));
                    })
                    .register();

    private static BlockBuilder<HandleBlock, CreateRegistrate> createHandle(@Nullable final DyeColor color, final HandleBlock.Variant variant) {
        final String prefix;
        if(variant == HandleBlock.Variant.DYED && color != null) {
            prefix = color.getSerializedName();
        } else {
            prefix = variant.getSerializedName();
        }

        final String name = prefix + "_handle";

        final BlockBuilder<HandleBlock, CreateRegistrate> builder = REGISTRATE.block(name, p -> new HandleBlock(p, color, variant))
                .initialProperties(SharedProperties::stone)
                .properties(p -> p.sound(SoundType.COPPER))
                .tag(SimTags.Blocks.HANDLES)
                .properties(BlockBehaviour.Properties::noOcclusion)
                .onRegister(ItemUseOverrides::addBlock);

        builder.blockstate((ctx, prov) -> {
            SimBlockStateService.INSTANCE.directionalAxisBlock(ctx, prov, (blockState, vertical) -> {
                final String suffix = vertical ? "vertical" : "horizontal";
                if(variant == HandleBlock.Variant.IRON) {
                    return prov.models()
                            .getExistingFile(prov.modLoc("block/handle/block_" + suffix));
                } else {
                    return prov.models()
                            .withExistingParent(ctx.getName() + "_" + suffix, prov.modLoc("block/handle/block_" + suffix))
                            .texture("0", prov.modLoc("block/handle/" + name));
                }
            });
        });

        builder.onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.simulated.handle"));

        final ItemBuilder<BlockItem, BlockBuilder<HandleBlock, CreateRegistrate>> itemBuilder = builder.item();
        itemBuilder.model((ctx, prov) -> {
                    prov.withExistingParent(ctx.getName(), prov.modLoc("block/handle/item"))
                            .texture("0", prov.modLoc("block/handle/" + name));
                });

        if(variant != HandleBlock.Variant.IRON) {
            itemBuilder.tag(SimTags.Items.HANDLE_VARIANTS);
        }

        itemBuilder.build();

        return builder;
    }

    private static Boolean never(final BlockState state, final BlockGetter blockGetter, final BlockPos pos) {
        return false;
    }

    public static void register() {
    }

}