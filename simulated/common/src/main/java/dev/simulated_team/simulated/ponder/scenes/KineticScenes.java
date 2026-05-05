package dev.simulated_team.simulated.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlock;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.instructions.*;
import dev.simulated_team.simulated.service.SimItemService;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.*;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class KineticScenes {

    public static void portableEngine(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final SelectionUtil select = util.select();
        final OverlayInstructions overlay = scene.overlay();
        final CreateSceneBuilder.EffectInstructions effects = scene.effects();
        final VectorUtil vector = util.vector();

        scene.title("portable_engine", "Generating Rotational Force using the Portable Engine");
        scene.configureBasePlate(0, 0, 5);

        final BlockPos enginePos = util.grid().at(2, 2, 1);
        //world.modifyTileEntity(enginePos, PortableEngineTileEntity.class, portableEngineTileEntity -> portableEngineTileEntity.tick());
        final BlockPos shaftPos = util.grid().at(2, 2, 2);
        final Vec3 textPos = vector.centerOf(enginePos);

        scene.world().modifyBlockEntity(enginePos, PortableEngineBlockEntity.class, be -> {
            be.openHatchOverride = true;
            be.hatchOpenTime = 1.0f;
            be.lastHatchOpenTime = 1.0f;
        });

        world.showSection(select.layer(0), Direction.UP);
        scene.idle(10);

        world.showSection(select.fromTo(2, 1, 1, 2, 2, 3), Direction.DOWN);


        scene.idle(10);
        overlay.showText(80)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(textPos)
                .text("The Portable Engine generates rotational force by burning fuel");
        scene.idle(35);
        overlay.showControls(textPos, Pointing.DOWN, 30).withItem(Items.COAL.getDefaultInstance());
        world.cycleBlockProperty(enginePos, AbstractFurnaceBlock.LIT);
        world.modifyBlockEntity(enginePos, PortableEngineBlockEntity.class, be -> {
            be.setCurrentBurnTime(SimItemService.INSTANCE.getBurnTime(Items.COAL.getDefaultInstance()));
            be.openHatchOverride = false;
        });
        world.setKineticSpeed(select.fromTo(2, 2, 1, 2, 2, 3), -32);
        effects.rotationDirectionIndicator(shaftPos);
        effects.emitParticles(vector.of(2.5, 2.2, 0.9), effects.simpleParticleEmitter(ParticleTypes.LAVA, Vec3.ZERO), 3, 1);

        scene.idle(70);
        scene.addKeyframe();
        final Vec3 configBox = new Vec3(2.5, 2.75, 1.6);
        final AABB bb = new AABB(2.4, 2.75, 1.5, 2.6, 2.75, 1.7);
        overlay.chaseBoundingBoxOutline(PonderPalette.WHITE, bb, bb, 60);
        overlay.showText(60)
                .placeNearTarget()
                .pointAt(configBox)
                .text("It can be configured to run in either direction.");
        scene.idle(40);
        overlay.showControls(configBox, Pointing.DOWN, 20).rightClick();
        scene.idle(10);
        world.multiplyKineticSpeed(select.fromTo(2, 2, 1, 2, 2, 3), -1);
        effects.rotationDirectionIndicator(shaftPos);
        scene.idle(40);

        world.hideSection(select.position(shaftPos), Direction.DOWN);
        scene.idle(15);
        final BlockState cogState = AllBlocks.COGWHEEL.getDefaultState();
        world.setBlock(shaftPos, cogState.setValue(CogWheelBlock.AXIS, Direction.Axis.Z), false);

        world.showSection(select.position(shaftPos).add(select.fromTo(0, 1, 1, 1, 2, 2)), Direction.DOWN);
        world.setKineticSpeed(select.fromTo(0, 1, 1, 1, 2, 2), -16);

        scene.idle(20);
        overlay.showText(80)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.topOf(new BlockPos(0, 1, 1)))
                .text("Fuel can be inserted by automated means");

        final ItemStack stack = Items.CHARCOAL.getDefaultInstance();
        world.createItemOnBelt(new BlockPos(0, 1, 1), Direction.WEST, stack);
        scene.idle(10);
        for (int i = 0; i < 2; i++) {
            scene.idle(24);
            if (i < 1) {
                world.createItemOnBelt(new BlockPos(0, 1, 1), Direction.WEST, stack);
            }
            scene.idle(10);
            world.removeItemsFromBelt(util.grid().at(1, 1, 1));
            world.flapFunnel(util.grid().at(1, 2, 1), false);
        }
        scene.idle(20);

        overlay.showText(80)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(textPos)
                .text("Using a Blaze Cake, the Portable Engine can be superheated for additional power output");


        scene.idle(10);
        world.modifyBlockEntity(enginePos, PortableEngineBlockEntity.class, be -> {
            be.openHatchOverride = true;
        });
        scene.idle(10);
        overlay.showControls(textPos, Pointing.DOWN, 30).withItem(AllItems.BLAZE_CAKE.asStack());
        world.modifyBlockEntity(enginePos, PortableEngineBlockEntity.class, be -> {
            be.setCurrentBurnTime(SimItemService.INSTANCE.getBurnTime(AllItems.BLAZE_CAKE.asStack()));
            be.setSuperHeated(true);
            be.openHatchOverride = false;
        });
        scene.idle(3);
        effects.rotationDirectionIndicator(shaftPos);
        effects.emitParticles(vector.of(2.5, 2.2, 0.9), effects.simpleParticleEmitter(ParticleTypes.LAVA, Vec3.ZERO), 3,
                1);
        world.multiplyKineticSpeed(select.everywhere(), 2);
        scene.idle(10);
    }

    public static void directionalGearshift(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final SelectionUtil select = util.select();
        scene.title("directional_gearshift", "Controlling rotational direction using Directional Gearshift");
        scene.configureBasePlate(0, 0, 5);

        world.showSection(select.layer(0), Direction.UP);

        final BlockPos rightLeverPos = new BlockPos(3, 1, 0);
        final BlockPos leftLeverPos = new BlockPos(3, 1, 4);
        final Selection rightLever = select.fromTo(rightLeverPos, rightLeverPos.south());
        final Selection leftLever = select.fromTo(leftLeverPos, leftLeverPos.north());

        world.showSection(rightLever, Direction.UP);
        world.showSection(leftLever, Direction.UP);
        scene.idle(10);

        final BlockPos backCogSmall = new BlockPos(5, 1, 2);
        final BlockPos frontCogSmall = new BlockPos(1, 1, 2);
        final BlockPos dirGearshift = new BlockPos(3, 1, 2);

        world.showSection(select.position(backCogSmall), Direction.DOWN);
        scene.idle(5);
        world.showSection(select.position(backCogSmall.west()), Direction.DOWN);
        scene.idle(5);
        final ElementLink<WorldSectionElement> dirGearshiftElement =
                world.showIndependentSection(select.position(dirGearshift), Direction.DOWN);
        scene.idle(5);
        world.showSection(select.position(dirGearshift.west()), Direction.DOWN);
        scene.idle(5);
        world.showSection(select.position(dirGearshift.west(2)), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("Unpowered Directional Gearshifts do not relay rotational power")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(dirGearshift))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        final Selection beyondDirGearshift = select.fromTo(dirGearshift.west(), dirGearshift.west(2));

        world.toggleRedstonePower(rightLever);
        scene.effects().indicateRedstone(rightLeverPos);
        world.cycleBlockProperty(dirGearshift, DirectionalGearshiftBlock.LEFT_POWERED);
        world.modifyKineticSpeed(beyondDirGearshift, f -> 32f);
        scene.effects().rotationDirectionIndicator(backCogSmall);
        scene.effects().rotationDirectionIndicator(frontCogSmall);
        scene.idle(30);

        scene.overlay().showText(80)
                .text("When the straight side is powered, it will relay rotation")
                .pointAt(util.vector().topOf(dirGearshift))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        world.toggleRedstonePower(rightLever);
        scene.effects().indicateRedstone(rightLeverPos);
        world.cycleBlockProperty(dirGearshift, DirectionalGearshiftBlock.LEFT_POWERED);
        world.modifyKineticSpeed(beyondDirGearshift, f -> 0f);
        scene.idle(10);

        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(dirGearshiftElement, new Vec3(0, 0.5, 0), 20,
                SmoothMovementUtils.quadraticJump()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(dirGearshiftElement, new Vec3(-180, 0, 0), 20,
                SmoothMovementUtils.cubicSmoothing()));
        scene.idle(40);

        world.toggleRedstonePower(rightLever);
        scene.effects().indicateRedstone(rightLeverPos);
        world.cycleBlockProperty(dirGearshift, DirectionalGearshiftBlock.RIGHT_POWERED);
        world.modifyKineticSpeed(beyondDirGearshift, f -> -32f);
        scene.effects().rotationDirectionIndicator(backCogSmall);
        scene.effects().rotationDirectionIndicator(frontCogSmall);
        scene.idle(30);

        scene.overlay().showText(80)
                .text("When the reverse side is powered, it will reverse rotation")
                .pointAt(util.vector().topOf(dirGearshift))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        world.toggleRedstonePower(leftLever);
        scene.effects().indicateRedstone(leftLeverPos);
        world.cycleBlockProperty(dirGearshift, DirectionalGearshiftBlock.LEFT_POWERED);
        world.modifyKineticSpeed(beyondDirGearshift, f -> 0f);
        scene.idle(30);

        scene.overlay().showText(80)
                .text("When both sides are powered, it will stop relaying rotation")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(dirGearshift))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        world.toggleRedstonePower(rightLever);
        scene.effects().indicateRedstone(rightLeverPos);
        world.cycleBlockProperty(dirGearshift, DirectionalGearshiftBlock.RIGHT_POWERED);
        world.modifyKineticSpeed(beyondDirGearshift, f -> 32f);
        scene.effects().rotationDirectionIndicator(backCogSmall);
        scene.effects().rotationDirectionIndicator(frontCogSmall);
        scene.markAsFinished();
    }

    public static void analogTransmission(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final CreateSceneBuilder.SpecialInstructions special = scene.special();
        final CreateSceneBuilder.EffectInstructions effects = scene.effects();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();

        scene.title("analog_transmission", "Controlling rotational speed using Analog Transmission");
        scene.configureBasePlate(0, 0, 5);
        //scene.setSceneOffsetY(-1);
        world.showSection(select.fromTo(0, 0, 0, 5, 0, 4), Direction.UP);

        final BlockPos analogLeverPos = new BlockPos(2, 1, 0);
        final BlockPos redstonePos = new BlockPos(2, 1, 1);
        final BlockPos transmissionPos = new BlockPos(2, 1, 2);
        final BlockPos bottomGauge = new BlockPos(0, 1, 2);
        final BlockPos topGauge = new BlockPos(0, 2, 2);

        final ElementLink<WorldSectionElement> redstoneSection = world.showIndependentSection(select.position(redstonePos), Direction.UP);
        final ElementLink<WorldSectionElement> analogLeverSection = world.showIndependentSection(select.position(analogLeverPos), Direction.UP);

        scene.idle(8);

        for (int i = 5; i >= 2; i--) {
            scene.idle(3);
            world.showSection(select.position(i, 1, 2), Direction.DOWN);
        }
        scene.idle(10);
        world.showSection(select.position(transmissionPos.above()), Direction.DOWN);
        scene.idle(5);
        overlay.showText(60)
                .text("Unpowered Analog Transmissions behave exactly like Encased Cogwheels")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.blockSurface(transmissionPos, Direction.NORTH));
        scene.idle(60);
        world.showSection(select.fromTo(0, 1, 2, 1, 2, 3), Direction.EAST);
        scene.idle(10);
        overlay.showText(50)
                .sharedText("rpm16")
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(vector.blockSurface(bottomGauge, Direction.NORTH));
        scene.idle(5);
        overlay.showText(50)
                .sharedText("rpm16")
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(vector.blockSurface(topGauge, Direction.NORTH));

        scene.idle(60);
        effects.indicateRedstone(analogLeverPos);
        world.toggleRedstonePower(select.position(transmissionPos));
        world.modifyBlock(redstonePos, s -> s.setValue(RedStoneWireBlock.POWER, 5), false);
        world.modifyBlockEntityNBT(select.position(analogLeverPos), AnalogLeverBlockEntity.class, nbt -> {
            nbt.putInt("State", 5);
        });

        world.modifyBlockEntityNBT(select.position(transmissionPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.getCompound("ExtraCogwheel").putFloat("Speed", 20f);
        });
        world.setKineticSpeed(select.fromTo(0, 2, 2, 2, 2, 2), -20f);
        scene.idle(10);
        overlay.showText(60)
                .text("Analog redstone inputs slows down the cogwheel relative to the shaft")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.blockSurface(transmissionPos, Direction.NORTH));
        scene.idle(70);

        overlay.showText(50)
                .sharedText("rpm16")
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(vector.blockSurface(bottomGauge, Direction.NORTH));
        scene.idle(5);
        overlay.showText(50)
                .text("10 RPM")
                .colored(PonderPalette.SLOW)
                .placeNearTarget()
                .pointAt(vector.blockSurface(topGauge, Direction.NORTH));

        scene.idle(60);
        effects.indicateRedstone(analogLeverPos);
        world.modifyBlock(redstonePos, s -> s.setValue(RedStoneWireBlock.POWER, 11), false);
        world.modifyBlockEntityNBT(select.position(analogLeverPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.putInt("State", 11);
        });

        world.modifyBlockEntityNBT(select.position(transmissionPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.getCompound("ExtraCogwheel").putFloat("Speed", 8f);
        });
        world.setKineticSpeed(select.fromTo(0, 2, 2, 2, 2, 2), -8f);
        scene.idle(10);
        overlay.showText(60)
                .text("Higher signal strengths results in larger gear ratios")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.blockSurface(transmissionPos, Direction.NORTH));
        scene.idle(70);
        overlay.showText(50)
                .sharedText("rpm16")
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(vector.blockSurface(bottomGauge, Direction.NORTH));
        scene.idle(5);
        overlay.showText(50)
                .text("4 RPM")
                .colored(PonderPalette.SLOW)
                .placeNearTarget()
                .pointAt(vector.blockSurface(topGauge, Direction.NORTH));
        scene.idle(60);


        world.toggleRedstonePower(select.position(transmissionPos));
        world.modifyBlock(redstonePos, s -> s.setValue(RedStoneWireBlock.POWER, 0), false);
        world.modifyBlockEntityNBT(select.position(analogLeverPos), AnalogLeverBlockEntity.class, nbt -> {
            nbt.putInt("State", 0);
        });

        world.modifyBlockEntityNBT(select.position(transmissionPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.getCompound("ExtraCogwheel").putFloat("Speed", 0f);
        });
        world.setKineticSpeed(select.fromTo(0, 1, 2, 2, 2, 2), 0f);

        final ElementLink<WorldSectionElement> transmissionSection = world.makeSectionIndependent(select.fromTo(transmissionPos, transmissionPos.above()));

        /*world.moveSection(transmissionSection,new Vec3(0,0.5,0),5);
        scene.idle(5);
        world.rotateSection(transmissionSection,180,0,0,10);
        scene.idle(10);
        world.moveSection(transmissionSection,new Vec3(0,-0.5,0),5);
        scene.idle(5);*/
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(transmissionSection, new Vec3(0, 0.5, 0), 20, SmoothMovementUtils.quadraticJump()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(transmissionSection, new Vec3(180, 0, 0), 20, SmoothMovementUtils.quinticSmoothing()));
        scene.idle(20);
        world.moveSection(transmissionSection, new Vec3(0, -2.5, 0), 0);

        final BlockPos newTransmissionPos = new BlockPos(0, 2, 0);

        //world.hideIndependentSection(transmissionSection,null);
        final ElementLink<WorldSectionElement> newTransmissionSection = world.showIndependentSectionImmediately(select.fromTo(newTransmissionPos, newTransmissionPos.below()));
        world.moveSection(newTransmissionSection, new Vec3(2, 0, 2), 0);

        world.setKineticSpeed(select.fromTo(0, 2, 2, 1, 2, 2), -32f);
        world.setKineticSpeed(select.fromTo(0, 1, 2, 1, 1, 2), 32f);
        scene.idle(10);
        world.moveSection(redstoneSection, new Vec3(0, 1, 0), 10);
        world.moveSection(analogLeverSection, new Vec3(0, 1, 0), 10);
        scene.idle(10);
        final ElementLink<WorldSectionElement> casingSection = world.showIndependentSection(select.fromTo(3, 1, 0, 3, 1, 1), Direction.WEST);
        world.moveSection(casingSection, new Vec3(-1, 0, 0), 0);

        scene.idle(20);
        effects.indicateRedstone(analogLeverPos.above());
        world.toggleRedstonePower(select.position(newTransmissionPos));
        world.modifyBlock(redstonePos, s -> s.setValue(RedStoneWireBlock.POWER, 11), false);
        world.modifyBlockEntityNBT(select.position(analogLeverPos), AnalogLeverBlockEntity.class, nbt -> {
            nbt.putInt("State", 11);
        });
        world.setKineticSpeed(select.fromTo(0, 2, 2, 1, 2, 2), -128f);
        world.setKineticSpeed(select.position(newTransmissionPos), -128f);

        scene.idle(10);
        overlay.showText(60)
                .text("When the Transmission gets its input through the cogwheel, the shaft speeds up instead")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.blockSurface(transmissionPos.above(), Direction.NORTH));
        scene.idle(70);
        overlay.showText(50)
                .sharedText("rpm16")
                .colored(PonderPalette.MEDIUM)
                .placeNearTarget()
                .pointAt(vector.blockSurface(bottomGauge, Direction.NORTH));
        scene.idle(5);
        overlay.showText(50)
                .text("64 RPM")
                .colored(PonderPalette.FAST)
                .placeNearTarget()
                .pointAt(vector.blockSurface(topGauge, Direction.NORTH));
        scene.idle(60);

        world.toggleRedstonePower(select.position(newTransmissionPos));
        world.modifyBlock(redstonePos, s -> s.setValue(RedStoneWireBlock.POWER, 0), false);
        world.modifyBlockEntityNBT(select.position(analogLeverPos), AnalogLeverBlockEntity.class, nbt -> {
            nbt.putInt("State", 0);
        });
        world.setKineticSpeed(select.fromTo(0, 2, 2, 1, 2, 2), -32f);
        world.setKineticSpeed(select.position(newTransmissionPos), -32f);
        scene.idle(5);
        world.hideIndependentSection(casingSection, Direction.EAST);
        scene.idle(20);
        world.moveSection(redstoneSection, new Vec3(0, -1, 0), 10);
        world.moveSection(analogLeverSection, new Vec3(0, -1, 0), 10);
        scene.idle(15);

        world.setKineticSpeed(select.fromTo(0, 1, 2, 1, 2, 2), 0);

        world.moveSection(newTransmissionSection, new Vec3(0, -2.5, 0), 0);
        world.moveSection(transmissionSection, new Vec3(0, 2.5, 0), 0);
        /*world.moveSection(transmissionSection,new Vec3(0,0.5,0),5);
        scene.idle(5);
        world.rotateSection(transmissionSection,180,0,0,10);
        scene.idle(10);
        world.moveSection(transmissionSection,new Vec3(0,-0.5,0),5);
        scene.idle(5);*/
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(transmissionSection, new Vec3(0, 0.5, 0), 20, SmoothMovementUtils.quadraticJump()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(transmissionSection, new Vec3(180, 0, 0), 20, SmoothMovementUtils.quinticSmoothing()));
        scene.idle(20);

        world.setKineticSpeed(select.fromTo(0, 1, 2, 2, 1, 2), 32);
        world.setKineticSpeed(select.fromTo(0, 2, 2, 2, 2, 2), -32);
        world.modifyBlockEntityNBT(select.position(transmissionPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.getCompound("ExtraCogwheel").putFloat("Speed", 32f);
        });
        scene.idle(10);
        world.hideIndependentSection(analogLeverSection, Direction.UP);
        scene.idle(20);
        final ElementLink<WorldSectionElement> leverSection = world.showIndependentSection(select.position(analogLeverPos.west()), Direction.DOWN);
        world.moveSection(leverSection, new Vec3(1, 0, 0), 0);
        scene.idle(20);
        effects.indicateRedstone(analogLeverPos);
        world.toggleRedstonePower(select.fromTo(1, 1, 0, 2, 1, 2));
        world.modifyBlockEntityNBT(select.position(transmissionPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.getCompound("ExtraCogwheel").putFloat("Speed", 0f);
        });
        world.setKineticSpeed(select.fromTo(0, 2, 2, 2, 2, 2), 0);
        scene.idle(20);
        overlay.showText(60)
                .text("At full signal input or breaking speeds, the shaft and cogwheel disconnect...")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.blockSurface(transmissionPos, Direction.NORTH));
        scene.idle(70);

        final Selection sideInput = select.fromTo(2, 1, 3, 4, 1, 5).add(select.position(4, 0, 5));

        world.showSection(sideInput, Direction.NORTH);
        world.modifyKineticSpeed(sideInput, x -> x * -2);
        scene.idle(8);
        world.modifyBlockEntityNBT(select.position(transmissionPos), AnalogTransmissionBlockEntity.class, nbt -> {
            nbt.getCompound("ExtraCogwheel").putFloat("Speed", -64f);
        });
        world.setKineticSpeed(select.fromTo(0, 2, 2, 2, 2, 2), 64);
        scene.idle(20);
        overlay.showText(60)
                .text("...and can then rotate independently of each other")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(vector.blockSurface(transmissionPos, Direction.NORTH));
        scene.idle(60);

    }

    // Steering Wheel Scenes

    public static void steeringWheelIntro(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final CreateSceneBuilder.EffectInstructions effects = scene.effects();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();
        final PositionUtil grid = util.grid();

        scene.title("steering_wheel_intro", "Using the Steering Wheel");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        final BlockPos steeringWheelPos = grid.at(2, 2, 2);
        final Selection steeringWheel = select.fromTo(2, 2, 1, 2, 2, 2);

        final Selection rudderKinetics = select.fromTo(2, 1, 2, 2, 1, 3);
        final BlockPos rudderBearing = grid.at(2, 2, 3);
        final Selection rudderSails = select.fromTo(2, 3, 2, 2, 5, 4);

        scene.idle(5);
        world.showSection(rudderKinetics, Direction.DOWN);
        scene.idle(10);
        world.showSection(select.position(steeringWheelPos), Direction.DOWN);
        scene.idle(20);

        overlay.showText(60)
                .text("Steering Wheels provide precise rotational output")
                .placeNearTarget()
                .pointAt(vector.centerOf(steeringWheelPos).add(0, 0, 0.1));
        scene.idle(80);
        overlay.showText(60)
                .text("Hold right click to grab the wheel...")
                .placeNearTarget()
                .pointAt(vector.centerOf(steeringWheelPos).add(0, 0, 0.1))
                .attachKeyFrame();
        overlay.showControls(vector.topOf(steeringWheelPos).add(0, 0.25, -0.5), Pointing.DOWN, 40).rightClick();
        scene.idle(80);

        overlay.showText(60)
                .text("...and move the mouse to rotate")
                .placeNearTarget()
                .pointAt(vector.centerOf(steeringWheelPos).add(0.25, 0, 0.1));

        world.setKineticSpeed(select.everywhere(), 16);
        effects.rotationDirectionIndicator(steeringWheelPos);

        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, -60, 50));

        scene.idle(60);

        world.setKineticSpeed(select.everywhere(), 0);

        scene.idle(40);

        overlay.showText(80)
                .text("Sneaking while turning the wheel snaps to 45° intervals")
                .placeNearTarget()
                .colored(PonderPalette.BLUE)
                .pointAt(vector.centerOf(steeringWheelPos).add(0.25, 0, 0.1))
                .attachKeyFrame();

        scene.idle(20);

        world.setKineticSpeed(select.everywhere(), -16);
        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, 15, 2));
        scene.idle(15);

        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, 45, 2));
        scene.idle(15);

        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, 45, 2));
        scene.idle(15);

        world.setKineticSpeed(select.everywhere(), 16);
        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, -45, 2));
        scene.idle(15);

        world.setKineticSpeed(select.everywhere(), 0);
        scene.idle(20);

        world.showSection(select.position(rudderBearing), Direction.NORTH);
        final ElementLink<WorldSectionElement> rudder = world.showIndependentSection(rudderSails, Direction.NORTH);
        scene.idle(40);

        overlay.showText(80)
                .text("The maximum turning angle can be configured using the value panel")
                .placeNearTarget()
                .pointAt(vector.topOf(steeringWheelPos))
                .attachKeyFrame();

        scene.overlay().showControls(vector.topOf(steeringWheelPos), Pointing.DOWN, 60).rightClick();
        overlay.showScrollInput(vector.topOf(steeringWheelPos), Direction.UP, 60);

        scene.idle(90);

        world.setKineticSpeed(steeringWheel, -16);
        world.setKineticSpeed(rudderKinetics, 16);
        effects.rotationDirectionIndicator(steeringWheelPos);
        world.rotateBearing(rudderBearing, 30, 12);
        world.rotateSection(rudder, 0, 30, 0, 12);
        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, -30, 10));
        scene.idle(12);

        world.setKineticSpeed(steeringWheel, 0);
        world.setKineticSpeed(rudderKinetics, 0);
        scene.idle(30);

        world.setKineticSpeed(steeringWheel, 16);
        world.setKineticSpeed(rudderKinetics, -16);
        effects.rotationDirectionIndicator(steeringWheelPos);
        world.rotateBearing(rudderBearing, -60, 24);
        world.rotateSection(rudder, 0, -60, 0, 24);
        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, 60, 20));
        scene.idle(24);

        world.setKineticSpeed(steeringWheel, 0);
        world.setKineticSpeed(rudderKinetics, 0);
        scene.idle(10);

        overlay.showText(60)
                .text("Mind that Bearings have to be specifically told not to disassemble")
                .placeNearTarget()
                .pointAt(vector.blockSurface(rudderBearing, Direction.WEST).subtract(2 / 16f, 2 / 16f, 0));

        overlay.showFilterSlotInput(vector.blockSurface(rudderBearing, Direction.WEST).subtract(0, 2 / 16f, 0), Direction.EAST, 80);

        scene.idle(90);

        // Thank you John Create for using ambiguous enough phrasing in the waterwheel ponder
        final ItemStack crimsonPlanks = new ItemStack(Items.CRIMSON_PLANKS);
        scene.overlay().showControls(util.vector().topOf(steeringWheelPos).add(0, 0.25, -0.5), Pointing.DOWN, 20).rightClick()
                .withItem(crimsonPlanks);
        scene.idle(7);
        scene.world().modifyBlockEntity(steeringWheelPos, SteeringWheelBlockEntity.class, be -> be.applyMaterialIfValid(crimsonPlanks));

        scene.effects().emitParticles(util.vector().topOf(steeringWheelPos)
                        .add(0, -.25, 0),
                scene.effects().particleEmitterWithinBlockSpace(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRIMSON_PLANKS.defaultBlockState()),
                        util.vector().of(0, 0, 0)),
                25, 1);

        scene.overlay().showText(50)
                .text("Use wood planks on the wheel to change its appearance")
                .colored(PonderPalette.BLUE)
                .placeNearTarget()
                .pointAt(util.vector().centerOf(steeringWheelPos).add(0, 0, 0.1))
                .attachKeyFrame();
        scene.idle(40);

        final ItemStack birchPlanks = new ItemStack(Items.BIRCH_PLANKS);
        scene.overlay().showControls(util.vector().topOf(steeringWheelPos).add(0, 0.25, -0.5), Pointing.DOWN, 20).rightClick()
                .withItem(birchPlanks);

        scene.idle(7);
        scene.world().modifyBlockEntity(steeringWheelPos, SteeringWheelBlockEntity.class, be -> be.applyMaterialIfValid(birchPlanks));

        scene.effects().emitParticles(util.vector().topOf(steeringWheelPos)
                        .add(0, -.25, -.25),
                scene.effects().particleEmitterWithinBlockSpace(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BIRCH_PLANKS.defaultBlockState()),
                        util.vector().of(0, 0, 0)),
                25, 1);

        scene.idle(40);

        final ItemStack junglePlanks = new ItemStack(Items.JUNGLE_PLANKS);
        scene.overlay().showControls(util.vector().topOf(steeringWheelPos).add(0, 0.25, -0.5), Pointing.DOWN, 20).rightClick()
                .withItem(junglePlanks);

        scene.idle(7);
        scene.world().modifyBlockEntity(steeringWheelPos, SteeringWheelBlockEntity.class, be -> be.applyMaterialIfValid(junglePlanks));

        scene.effects().emitParticles(util.vector().topOf(steeringWheelPos)
                        .add(0, -.25, -.25),
                scene.effects().particleEmitterWithinBlockSpace(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.JUNGLE_PLANKS.defaultBlockState()),
                        util.vector().of(0, 0, 0)),
                25, 1);

        scene.effects().emitParticles(util.vector().topOf(steeringWheelPos)
                        .add(0, -.25, -.25),
                scene.effects().particleEmitterWithinBlockSpace(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.JUNGLE_PLANKS.defaultBlockState()),
                        util.vector().of(0, 0, 0)),
                25, 1);

        scene.idle(60);
        //

        scene.markAsFinished();

        world.setKineticSpeed(steeringWheel, -16);
        world.setKineticSpeed(rudderKinetics, 16);
        world.rotateBearing(rudderBearing, 30, 12);
        world.rotateSection(rudder, 0, 30, 0, 12);
        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, -30, 10));
        scene.idle(12);
        world.setKineticSpeed(steeringWheel, 0);
        world.setKineticSpeed(rudderKinetics, 0);
    }

    public static void steeringWheelComparator(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final CreateSceneBuilder.EffectInstructions effects = scene.effects();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();
        final PositionUtil grid = util.grid();

        scene.title("steering_wheel_comparator", "Using Comparators With Steering Wheels");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        final BlockPos steeringWheelPos = grid.at(2, 1, 2);
        scene.special().movePointOfInterest(vector.topOf(steeringWheelPos));

        final Selection leftComparator = select.position(3, 1, 2);
        final Selection rightComparator = select.position(1, 1, 2);

        final Selection leftNixie = select.position(4, 1, 2);
        final Selection rightNixie = select.position(0, 1, 2);

        final Selection backRedstone = select.fromTo(2, 1, 3, 2, 1, 4);
        final BlockPos redstoneLamp = new BlockPos(2, 1, 4);

        scene.idle(10);
        world.showSection(select.position(steeringWheelPos), Direction.DOWN);
        scene.idle(5);
        world.showSection(select.position(steeringWheelPos.east()), Direction.DOWN);
        world.showSection(select.position(steeringWheelPos.west()), Direction.DOWN);
        scene.idle(5);
        world.showSection(leftNixie, Direction.DOWN);
        world.showSection(rightNixie, Direction.DOWN);

        scene.idle(20);

        overlay.showText(80)
                .text("Redstone Comparators can be used to read the current angle of the Steering Wheel")
                .placeNearTarget()
                .pointAt(steeringWheelPos.getCenter())
                .attachKeyFrame();

        scene.idle(60);

        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, -80, 16));

        scene.idle(4);

        effects.indicateRedstone(new BlockPos(4, 1, 2));
        world.toggleRedstonePower(leftComparator);

        for (int i = 1; i < 9; i++) {
            final int finalI = i;
            world.modifyBlockEntityNBT(leftNixie, NixieTubeBlockEntity.class,
                    nbt -> nbt.putInt("RedstoneStrength", finalI));
            scene.idle(2);
        }

        scene.idle(10);

        scene.addInstruction(SimAnimateBEInstruction.steeringWheel(steeringWheelPos, 120, 24));

        for (int i = 1; i < 9; i++) {
            final int finalI = 8 - i;
            world.modifyBlockEntityNBT(leftNixie, NixieTubeBlockEntity.class,
                    nbt -> nbt.putInt("RedstoneStrength", finalI));
            scene.idle(2);
        }

        world.toggleRedstonePower(leftComparator);

        effects.indicateRedstone(new BlockPos(0, 1, 2));
        world.toggleRedstonePower(rightComparator);

        for (int i = 1; i < 5; i++) {
            final int finalI = i;
            world.modifyBlockEntityNBT(rightNixie, NixieTubeBlockEntity.class,
                    nbt -> nbt.putInt("RedstoneStrength", finalI));
            scene.idle(2);
        }

        scene.idle(30);

        overlay.showText(80)
                .text("The output Signal is proportional to the configured maximum Angle")
                .placeNearTarget()
                .pointAt(vector.of(0, 1.5, 2.5));

        scene.idle(40);

        overlay.showControls(vector.topOf(steeringWheelPos), Pointing.DOWN, 40).rightClick();
        overlay.showScrollInput(vector.topOf(steeringWheelPos), Direction.UP, 40);

        scene.idle(6);

        effects.indicateRedstone(new BlockPos(0, 1, 2));
        world.modifyBlockEntityNBT(rightNixie, NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 8));

        scene.idle(54);

        scene.rotateCameraY(-90);

        scene.idle(10);

        world.showSection(backRedstone, Direction.DOWN);

        scene.idle(10);

        scene.addKeyframe();

        scene.idle(20);

        final ElementLink<ParrotElement> birb = scene.special().createBirb(steeringWheelPos.north().getBottomCenter().subtract(0, 2, 0.25), ParrotPose.FacePointOfInterestPose::new);

        scene.idle(4);

        scene.special().moveParrot(birb, vector.of(0, 2.15, 0), 0);
        scene.special().moveParrot(birb, vector.of(0, -0.15, 0), 6);

        scene.idle(6);

        world.toggleRedstonePower(backRedstone);
        effects.indicateRedstone(redstoneLamp);

        overlay.showText(100)
                .text("Redstone Comparators placed on the back of the Steering Wheel will read if it is in use")
                .placeNearTarget()
                .pointAt(redstoneLamp.getCenter());

        scene.idle(40);

        scene.markAsFinished();
    }

    //

    public static void torsionSpring(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final CreateSceneBuilder.EffectInstructions effects = scene.effects();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();
        final PositionUtil grid = util.grid();

        scene.title("torsion_spring", "Controlling Rotational Speed using Torsion Springs");
        scene.configureBasePlate(1, 0, 5);
        world.showSection(select.layer(0), Direction.UP);
        scene.idle(5);
        world.showSection(select.fromTo(6, 1, 2, 4, 1, 2), Direction.DOWN);
        final BlockPos springPos = grid.at(3, 1, 2);
        final BlockPos bearingPos = grid.at(1, 1, 2);
        final Selection springSelection = select.position(springPos);
        final Selection inputKinetics1 = select.fromTo(3, 1, 2, 6, 1, 2);
        final Selection inputKinetics2 = select.position(6, 0, 3);
        final Selection outputKinetics = select.fromTo(3, 1, 2, 1, 1, 2);
        final Selection redstone = select.fromTo(5, 1, 0, 5, 1, 1);
        final Selection redstoneLeft = select.fromTo(2, 1, 0, 3, 1, 1);
        final Selection redstoneRight = select.fromTo(3, 1, 3, 4, 1, 4);
        final BlockPos leverPos = grid.at(3, 1, 0);
        scene.idle(10);

        world.showSection(springSelection, Direction.DOWN);
        scene.idle(10);

        world.showSection(select.fromTo(2, 1, 2, 1, 1, 2), Direction.EAST);
        scene.idle(10);

        final Vec3 top = vector.topOf(springPos);
        overlay.showText(60)
                .text("Torsion Springs relay rotation within a set range of angles")
                .attachKeyFrame()
                .pointAt(top)
                .placeNearTarget();
        scene.idle(80);

        final ElementLink<WorldSectionElement> contraption =
                world.showIndependentSection(select.fromTo(0, 3, 2, 0, 1, 2), Direction.EAST);
        world.configureCenterOfRotation(contraption, vector.centerOf(bearingPos));
        scene.idle(20);
        world.setKineticSpeed(springSelection, 16);
        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });
        world.setKineticSpeed(inputKinetics1, 16);
        world.setKineticSpeed(inputKinetics2, -8);
        world.rotateSection(contraption, 90, 0, 0, 30);
        world.rotateBearing(bearingPos, 90, 30);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 90, 30));
        scene.idle(30);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);
        world.setKineticSpeed(outputKinetics, -16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", -16);
        });
        world.setKineticSpeed(inputKinetics1, -16);
        world.setKineticSpeed(inputKinetics2, 8);
        world.rotateSection(contraption, -180, 0, 0, 60);
        world.rotateBearing(bearingPos, -180, 60);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, -180, 60));
        scene.idle(60);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(10);
        overlay.showText(60)
                .text("When the input stops rotating, the Spring returns to its starting angle")
                .attachKeyFrame()
                .pointAt(top)
                .placeNearTarget();
        scene.idle(40);
        world.setKineticSpeed(inputKinetics1, 0);
        world.setKineticSpeed(inputKinetics2, 0);
        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });
        world.rotateSection(contraption, 90, 0, 0, 30);
        world.rotateBearing(bearingPos, 90, 30);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 90, 30));
        scene.idle(30);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(10);
        final Vec3 blockSurface = vector.centerOf(springPos)
                .add(5 / 16f, 8 / 16f, 0);
        AABB point = new AABB(blockSurface, blockSurface);
        AABB expanded = point.inflate(1 / 8f, 1 / 16f, 1 / 8f);

        overlay.chaseBoundingBoxOutline(PonderPalette.WHITE, blockSurface, point, 1);
        scene.idle(1);
        overlay.chaseBoundingBoxOutline(PonderPalette.WHITE, blockSurface, expanded, 80);
        overlay.showControls(blockSurface, Pointing.DOWN, 60).rightClick();
        scene.idle(10);

        overlay.showText(60)
                .text("The angle limit can be configured on the value panel")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(blockSurface);
        scene.idle(70);

        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });
        world.setKineticSpeed(inputKinetics1, 16);
        world.setKineticSpeed(inputKinetics2, -8);
        world.rotateSection(contraption, 30, 0, 0, 10);
        world.rotateBearing(bearingPos, 30, 10);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 30, 10));
        scene.idle(10);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);

        final Vec3 bearingSurface = vector.topOf(new BlockPos(1, 1, 2))
                .add(1 / 8f, 0, 0);
        point = new AABB(bearingSurface, bearingSurface);
        expanded = point.inflate(1 / 8f, 0, 1 / 8f);

        overlay.chaseBoundingBoxOutline(PonderPalette.WHITE, bearingSurface, point, 1);
        scene.idle(1);
        overlay.chaseBoundingBoxOutline(PonderPalette.WHITE, bearingSurface, expanded, 80);
        scene.idle(10);
        overlay.showText(70)
                .text("Mind that Bearings have to be specifically told not to disassemble")
                .placeNearTarget()
                .pointAt(bearingSurface);

        scene.idle(60);
        world.setKineticSpeed(inputKinetics1, 0);
        world.setKineticSpeed(inputKinetics2, 0);
        world.setKineticSpeed(outputKinetics, -16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", -16);
        });
        world.rotateSection(contraption, -30, 0, 0, 10);
        world.rotateBearing(bearingPos, -30, 10);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, -30, 10));
        scene.idle(10);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);
        overlay.showControls(blockSurface, Pointing.DOWN, 10).rightClick();
        scene.idle(20);
        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });
        world.setKineticSpeed(inputKinetics1, 16);
        world.setKineticSpeed(inputKinetics2, -8);
        world.rotateSection(contraption, 120, 0, 0, 40);
        world.rotateBearing(bearingPos, 120, 40);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 120, 40));
        scene.idle(40);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);

        world.setKineticSpeed(inputKinetics1, 0);
        world.setKineticSpeed(inputKinetics2, 0);
        world.setKineticSpeed(outputKinetics, -16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", -16);
        });
        world.rotateSection(contraption, -90, 0, 0, 30);
        world.rotateBearing(bearingPos, -90, 30);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, -90, 30));
        scene.idle(10);
        final ElementLink<WorldSectionElement> lever = world.showIndependentSection(redstone, Direction.DOWN);
        world.moveSection(lever, new Vec3(-2, 0, 0), 0);
        scene.idle(20);
        world.toggleRedstonePower(redstone);
        effects.indicateRedstone(leverPos);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);
        overlay.showText(60)
                .text("When powered by Redstone, Torsion Springs will not spring back")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(blockSurface);
        scene.idle(40);

        world.setKineticSpeed(outputKinetics, -16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", -16);
        });
        world.setKineticSpeed(inputKinetics1, -16);
        world.setKineticSpeed(inputKinetics2, 8);
        world.rotateSection(contraption, -90, 0, 0, 30);
        world.rotateBearing(bearingPos, -90, 30);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, -90, 30));
        scene.idle(30);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        world.setKineticSpeed(inputKinetics1, 0);
        world.setKineticSpeed(inputKinetics2, 0);
        scene.idle(20);
        world.toggleRedstonePower(redstone);
        effects.indicateRedstone(leverPos);
        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });
        world.rotateSection(contraption, 60, 0, 0, 20);
        world.rotateBearing(bearingPos, 60, 20);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 60, 20));
        scene.idle(20);
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(10);
        world.hideIndependentSection(lever, Direction.UP);
        scene.idle(20);
        world.showSection(redstoneLeft, Direction.DOWN);
        world.showSection(redstoneRight, Direction.DOWN);
        scene.idle(20);

        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });
        world.setKineticSpeed(inputKinetics1, 16);
        world.setKineticSpeed(inputKinetics2, -8);
        world.rotateSection(contraption, 90, 0, 0, 30);
        world.rotateBearing(bearingPos, 90, 30);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 90, 30));

        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.addInstruction(new RedstoneSignalInstruction(redstoneRight, i + 1));
            scene.idle(1);
        }

        world.setKineticSpeed(outputKinetics, -16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", -16);
        });
        world.setKineticSpeed(inputKinetics1, -16);
        world.setKineticSpeed(inputKinetics2, 8);

        world.rotateSection(contraption, -210, 0, 0, 75);
        world.rotateBearing(bearingPos, -210, 75);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, -210, 75));


        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.addInstruction(new RedstoneSignalInstruction(redstoneRight, 9 - i));
            scene.idle(1);
            if (i == 3) {
                overlay.showText(80)
                        .text("Redstone Comparators can be used to read the current angle of the spring")
                        .attachKeyFrame()
                        .placeNearTarget()
                        .pointAt(vector.topOf(grid.at(3, 0, 1)));
            }
        }

        for (int i = 0; i < 15; i++) {
            scene.idle(2);
            scene.addInstruction(new RedstoneSignalInstruction(redstoneLeft, i + 1));
            scene.idle(1);
        }

        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);
        world.setKineticSpeed(inputKinetics1, 0);
        world.setKineticSpeed(inputKinetics2, 0);
        world.setKineticSpeed(outputKinetics, 16);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 16);
        });

        world.rotateSection(contraption, 120, 0, 0, 45);
        world.rotateBearing(bearingPos, 120, 45);
        scene.addInstruction(SimAnimateBEInstruction.torsionSpring(springPos, 120, 45));
        for (int i = 0; i < 15; i++) {
            scene.idle(2);
            scene.addInstruction(new RedstoneSignalInstruction(redstoneLeft, 14 - i));
            scene.idle(1);
        }
        world.setKineticSpeed(outputKinetics, 0);
        world.modifyBlockEntityNBT(springSelection, TorsionSpringBlockEntity.class, nbt -> {
            nbt.getCompound("TorsionSpringOutput").putFloat("Speed", 0);
        });
        scene.idle(20);

        scene.overlay().showControls(util.vector().blockSurface(springPos, Direction.NORTH), Pointing.RIGHT, 30)
                .withItem(new ItemStack(AllItems.BRASS_SHEET.get()));
        scene.idle(5);
        scene.world().setBlock(springPos,
                SimBlocks.TORSION_SPRING.getDefaultState()
                        .setValue(TorsionSpringBlock.VARIANT, TorsionSpringBlock.Variant.BRASS)
                        .setValue(TorsionSpringBlock.FACING, Direction.WEST),
                true);
        scene.idle(5);
        scene.overlay().showText(80)
                .text("For aesthetic purposes, the metal can be changed with an Brass Sheet")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(springPos.west(), Direction.WEST))
                .placeNearTarget();
        scene.idle(40);
    }

    public static void nozzle(final SceneBuilder builder, final SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final Outliner outliner = builder.getScene().getOutliner();
        scene.title("nozzle", "Moving Simulated Contraptions using Nozzles");
        scene.configureBasePlate(5, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();

        final Selection fanNozzles = util.select().fromTo(7, 2, 2, 7, 3, 2)
                .add(util.select().fromTo(13, 2, 2, 13, 3, 2))
                .add(util.select().fromTo(7, 2, 6, 7, 3, 6))
                .add(util.select().fromTo(13, 2, 6, 13, 3, 6));
        final Selection theThing = util.select().fromTo(6, 2, 2, 12, 4, 6);

        scene.idle(10);

        final ElementLink<WorldSectionElement> hoverCraft = world.showIndependentSection(fanNozzles, Direction.DOWN);
        world.moveSection(hoverCraft, new Vec3(0, -1, 0), 0);
        world.showSectionAndMerge(theThing.substract(fanNozzles), Direction.DOWN, hoverCraft);
        scene.addInstruction(new PullTheAssemblerKronkInstruction(new BlockPos(8, 4, 5), true, true));

        scene.idle(20);

        final BlockPos portableEngine = new BlockPos(8, 4, 3);

        scene.overlay().showControls(util.vector().topOf(portableEngine.below()), Pointing.DOWN, 10)
                .withItem(Items.COAL.getDefaultInstance());

        scene.idle(6);

        final Selection forwards32 = util.select().fromTo(8, 4, 3, 10, 4, 3);
        final Selection forwards64 = util.select().position(6, 3, 3) // side big cog
                .add(util.select().position(12, 3, 3)); // other side big cog
        final Selection reverse64 = util.select().fromTo(6, 3, 4, 12, 3, 4) // central shaft
                .add(util.select().position(6, 3, 5)) // side big cog number 3
                .add(util.select().position(12, 3, 5)) // ????
                .add(util.select().position(6, 4, 2)) // top cogs
                .add(util.select().position(12, 4, 2))
                .add(util.select().position(6, 4, 6))
                .add(util.select().position(12, 4, 6));

        world.cycleBlockProperty(portableEngine, AbstractFurnaceBlock.LIT);
        world.setKineticSpeed(forwards32, 32);
        world.setKineticSpeed(forwards64, 64);
        world.setKineticSpeed(reverse64, -64);

        scene.idle(4);

        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(hoverCraft, new Vec3(0, 1.5, 0), 40, SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(30);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(hoverCraft, new Vec3(0, -0.7, 0), 30, SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(30);

        scene.overlay().showText(70)
                .pointAt(new Vec3(6.5, 2.5, 2.5))
                .placeNearTarget()
                .text("Nozzles attached to fans on Simulated Contraptions push away from nearby surfaces")
                .attachKeyFrame();
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(hoverCraft, new Vec3(0, 0.2, 0), 30, SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(30);

        scene.addInstruction(new RotateSceneInstruction(25f, 25, true));
        scene.idle(50);

        final Selection worl = util.select().fromTo(14, 0, 0, 14, 4, 8);
        world.showSection(worl, Direction.WEST);
        scene.addInstruction(new CustomToggleBaseShadowInstruction());
        scene.idle(20);

        scene.overlay().showText(70)
                .pointAt(new Vec3(14, 2.5, 2.5))
                .placeNearTarget()
                .text("Nearby walls will be pushed away from horizontally")
                .attachKeyFrame();
        scene.idle(70);

        scene.addInstruction(new RotateSceneInstruction(-25f, -25, true));
        final Selection worterArea = util.select().fromTo(0, 0, 0, 4, 0, 8);
        final ElementLink<WorldSectionElement> theWorld = world.makeSectionIndependent(util.select().fromTo(5, 0, 0, 8, 0, 8));
        final ElementLink<WorldSectionElement> theWorldThatDisappears = world.makeSectionIndependent(
                util.select().fromTo(9, 0, 0, 13, 0, 8)
                        .add(util.select().fromTo(14, 0, 0, 14, 4, 8))
        );
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(theWorld, new Vec3(1, 0, 0), 20, SmoothMovementUtils.quadraticRise()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(theWorldThatDisappears, new Vec3(1, 0, 0), 20, SmoothMovementUtils.quadraticRise()));
        scene.idle(20);

        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(theWorld, new Vec3(4, 0, 0), 80, SmoothMovementUtils.quadraticRiseOut()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(theWorldThatDisappears, new Vec3(4, 0, 0), 80, SmoothMovementUtils.quadraticRiseOut()));
        final ElementLink<WorldSectionElement> worter = world.showIndependentSection(worterArea, Direction.EAST);
        world.moveSection(worter, new Vec3(1, 0, 0), 0);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(worter, new Vec3(4, 0, 0), 80, SmoothMovementUtils.quadraticRiseOut()));
        scene.idle(60);

        scene.overlay().showText(70)
                .pointAt(new Vec3(7.5, 0.5, 4.5))
                .placeNearTarget()
                .text("Fluids also act as a surface")
                .attachKeyFrame();
        scene.idle(80);

        world.hideIndependentSection(theWorldThatDisappears, Direction.DOWN);
        scene.addInstruction(new CustomToggleBaseShadowInstruction());
        scene.markAsFinished();
    }
}
