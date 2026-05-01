package dev.simulated_team.simulated.index;

import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorRenderer;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionRenderer;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionVisual;
import dev.simulated_team.simulated.content.blocks.auger_shaft.*;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlockEntity;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftRenderer;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorRenderer;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorVisual;
import dev.simulated_team.simulated.content.blocks.handle.HandleBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorRenderer;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueRenderer;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateRenderer;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableRenderer;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableVisual;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerRenderer;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkVisual;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorRenderer;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorVisual;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorRenderer;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchRenderer;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.spring.SpringRenderer;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelRenderer;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelVisual;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingRenderer;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingVisual;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockRenderer;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverRenderer;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverVisual;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringRenderer;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringVisual;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorRenderer;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimInventoryService;

public class SimBlockEntityTypes {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final BlockEntityEntry<AnalogTransmissionBlockEntity> SIMPLE_BE = REGISTRATE
            .blockEntity("simple", AnalogTransmissionBlockEntity::new)
            .visual(() -> AnalogTransmissionVisual::new)
            .validBlocks(SimBlocks.ANALOG_TRANSMISSION)
            .renderer(() -> AnalogTransmissionRenderer::new)
            .register();

    public static final BlockEntityEntry<TorsionSpringBlockEntity> TORSION_SPRING = REGISTRATE
            .blockEntity("torsion_spring", TorsionSpringBlockEntity::new)
            .visual(() -> TorsionSpringVisual::new)
            .validBlocks(SimBlocks.TORSION_SPRING)
            .renderer(() -> TorsionSpringRenderer::new)
            .register();

    public static final BlockEntityEntry<PhysicsAssemblerBlockEntity> PHYSICS_ASSEMBLER = REGISTRATE
            .blockEntity("physics_assembler", PhysicsAssemblerBlockEntity::new)
            .validBlocks(SimBlocks.PHYSICS_ASSEMBLER)
            .renderer(() -> PhysicsAssemblerRenderer::new)
            .register();

    public static final BlockEntityEntry<RopeWinchBlockEntity> ROPE_WINCH = REGISTRATE
            .blockEntity("rope_winch", RopeWinchBlockEntity::new)
            .validBlocks(SimBlocks.ROPE_WINCH)
            .renderer(() -> RopeWinchRenderer::new)
            .register();

    public static final BlockEntityEntry<RopeConnectorBlockEntity> ROPE_CONNECTOR = REGISTRATE
            .blockEntity("rope_connector", RopeConnectorBlockEntity::new)
            .validBlocks(SimBlocks.ROPE_CONNECTOR)
            .renderer(() -> RopeConnectorRenderer::new)
            .register();

    public static final BlockEntityEntry<DockingConnectorBlockEntity> DOCKING_CONNECTOR = REGISTRATE
            .blockEntity("docking_connector", DockingConnectorBlockEntity::new)
            .onRegister(SimInventoryService.INSTANCE.registerInventory((be, dir) -> be.getInventory()))
            .onRegister(SimInventoryService.INSTANCE.registerTank((be, dir) -> be.tank))
            .validBlocks(SimBlocks.DOCKING_CONNECTOR)
            .renderer(() -> DockingConnectorRenderer::new)
            .register();

    public static final BlockEntityEntry<AltitudeSensorBlockEntity> ALTITUDE_SENSOR = REGISTRATE
            .blockEntity("altitude_sensor", AltitudeSensorBlockEntity::new)
            .validBlocks(SimBlocks.ALTITUDE_SENSOR)
            .renderer(() -> AltitudeSensorRenderer::new)
            .register();

    public static final BlockEntityEntry<GimbalSensorBlockEntity> GIMBAL_SENSOR = REGISTRATE
            .blockEntity("gimbal_sensor", GimbalSensorBlockEntity::new)
            .visual(() -> GimbalSensorVisual::new)
            .validBlocks(SimBlocks.GIMBAL_SENSOR)
            .renderer(() -> GimbalSensorRenderer::new)
            .register();

    public static final BlockEntityEntry<HandleBlockEntity> HANDLE = REGISTRATE
            .blockEntity("handle", HandleBlockEntity::new)
            .validBlock(SimBlocks.IRON_HANDLE)
            .validBlock(SimBlocks.COPPER_HANDLE)
            .validBlocks(SimBlocks.DYED_HANDLES.toArray())
            .register();

    public static final BlockEntityEntry<PortableEngineBlockEntity> PORTABLE_ENGINE = REGISTRATE
            .blockEntity("portable_engine", PortableEngineBlockEntity::new)
            .onRegister(SimInventoryService.INSTANCE.registerInventory((be, dir) -> be.inventory))
            .validBlocks(SimBlocks.PORTABLE_ENGINES.toArray())
            .renderer(() -> PortableEngineRenderer::new)
            .register();

    public static final BlockEntityEntry<SwivelBearingBlockEntity> SWIVEL_BEARING = REGISTRATE
            .blockEntity("swivel_bearing", SwivelBearingBlockEntity::new)
            .visual(() -> SwivelBearingVisual::new)
            .validBlocks(SimBlocks.SWIVEL_BEARING)
            .renderer(() -> SwivelBearingRenderer::new)
            .register();

    public static final BlockEntityEntry<SteeringWheelBlockEntity> STEERING_WHEEL = REGISTRATE
            .blockEntity("steering_wheel", SteeringWheelBlockEntity::new)
            .visual(() -> SteeringWheelVisual::new)
            .validBlocks(SimBlocks.STEERING_WHEEL)
            .renderer(() -> SteeringWheelRenderer::new)
            .register();

    public static final BlockEntityEntry<SpringBlockEntity> SPRING = REGISTRATE
            .blockEntity("spring", SpringBlockEntity::new)
            .validBlock(SimBlocks.SPRING)
            .renderer(() -> SpringRenderer::new)
            .register();

    public static final BlockEntityEntry<SwivelBearingPlateBlockEntity> SWIVEL_BEARING_LINK_BLOCK = REGISTRATE
            .blockEntity("swivel_bearing_link_block", SwivelBearingPlateBlockEntity::new)
            .visual(() -> OrientedRotatingVisual.of(SimPartialModels.SHAFT_SIXTEENTH))
            .validBlocks(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)
            .renderer(() -> SwivelBearingPlateBlockRenderer::new)
            .register();

    public static final BlockEntityEntry<LinkedTypewriterBlockEntity> LINKED_TYPEWRITER = REGISTRATE
            .blockEntity("linked_typewriter", LinkedTypewriterBlockEntity::new)
            .validBlocks(SimBlocks.LINKED_TYPEWRITER)
            .renderer(() -> LinkedTypewriterRenderer::new)
            .register();

    // Lasers

    public static final BlockEntityEntry<OpticalSensorBlockEntity> OPTICAL_SENSOR = REGISTRATE
            .blockEntity("optical_sensor", OpticalSensorBlockEntity::new)
            .validBlocks(SimBlocks.OPTICAL_SENSOR)
            .renderer(() -> OpticalSensorRenderer::new)
            .register();
    public static final BlockEntityEntry<LaserPointerBlockEntity> LASER_POINTER = REGISTRATE
            .blockEntity("laser_pointer", LaserPointerBlockEntity::new)
            .validBlocks(SimBlocks.LASER_POINTER)
            .renderer(() -> LaserPointerRenderer::new)
            .register();
    public static final BlockEntityEntry<LaserSensorBlockEntity> LASER_SENSOR = REGISTRATE
            .blockEntity("ir_sensor", LaserSensorBlockEntity::new)
            .validBlocks(SimBlocks.LASER_SENSOR)
            .renderer(() -> SmartBlockEntityRenderer::new)
            .register();

    // Redstone

    public static final BlockEntityEntry<VelocitySensorBlockEntity> VELOCITY_SENSOR = REGISTRATE
            .blockEntity("velocity_sensor", VelocitySensorBlockEntity::new)
            .validBlocks(SimBlocks.VELOCITY_SENSOR)
            .renderer(() -> VelocitySensorRenderer::new)
            .register();

    //TODO technically doesn't even need a renderer?
    public static final BlockEntityEntry<DirectionalLinkedReceiverBlockEntity> DIRECTIONAL_LINKED_RECEIVER = REGISTRATE
            .blockEntity("directional_linked_receiver", DirectionalLinkedReceiverBlockEntity::new)
            .validBlocks(SimBlocks.DIRECTIONAL_LINKED_RECEIVER)
            .renderer(() -> DirectionalLinkedReceiverRenderer::new)
            .register();

    //TODO visual once new nameplate is in
    public static final BlockEntityEntry<NameplateBlockEntity> NAMEPLATE = REGISTRATE
            .blockEntity("nameplate", NameplateBlockEntity::new)
            .validBlocks(SimBlocks.NAMEPLATES.toArray())
            .renderer(() -> NameplateRenderer::new)
            .register();

    public static final BlockEntityEntry<ModulatingLinkedReceiverBlockEntity> MODULATING_LINKED_RECEIVER = REGISTRATE
            .blockEntity("modulating_linked_receiver", ModulatingLinkedReceiverBlockEntity::new)
            .visual(() -> ModulatingLinkVisual::new)
            .validBlocks(SimBlocks.MODULATING_LINKED_RECEIVER)
            .renderer(() -> ModulatingLinkedReceiverRenderer::new)
            .register();

    // TODO visual
    //is a visual for this even possible?
    public static final BlockEntityEntry<RedstoneAccumulatorBlockEntity> REDSTONE_ACCUMULATOR = REGISTRATE
            .blockEntity("redstone_accumulator", RedstoneAccumulatorBlockEntity::new)
            .validBlock(SimBlocks.REDSTONE_ACCUMULATOR)
            .renderer(() -> RedstoneAccumulatorRenderer::new)
            .register();

    public static final BlockEntityEntry<NavTableBlockEntity> NAVIGATION_TABLE = REGISTRATE
            .blockEntity("navigation_table", NavTableBlockEntity::new)
            .visual(() -> NavTableVisual::new)
            .onRegister(SimInventoryService.INSTANCE.registerInventory((be, dir) -> be.inventory))
            .validBlocks(SimBlocks.NAVIGATION_TABLE)
            .renderer(() -> NavTableRenderer::new)
            .register();

    public static final BlockEntityEntry<RedstoneInductorBlockEntity> REDSTONE_INDUCTOR = REGISTRATE
            .blockEntity("redstone_inductor", RedstoneInductorBlockEntity::new)
            .visual(() -> RedstoneInductorVisual::new)
            .validBlocks(SimBlocks.REDSTONE_INDUCTOR)
            .renderer(() -> RedstoneInductorRenderer::new)
            .register();

    public static final BlockEntityEntry<RedstoneMagnetBlockEntity> REDSTONE_MAGNET = REGISTRATE
            .blockEntity("redstone_magnet", RedstoneMagnetBlockEntity::new)
            .validBlocks(SimBlocks.REDSTONE_MAGNET)
            .register();

    public static final BlockEntityEntry<ThrottleLeverBlockEntity> THROTTLE_LEVER = REGISTRATE
            .blockEntity("throttle_lever", ThrottleLeverBlockEntity::new)
            .visual(() -> ThrottleLeverVisual::new)
            .validBlocks(SimBlocks.THROTTLE_LEVER)
            .renderer(() -> ThrottleLeverRenderer::new)
            .register();

    /*
    public static final BlockEntityEntry<AbsorberBlockEntity> ABSORBER = REGISTRATE
            .blockEntity("absorber", AbsorberBlockEntity::new)
            .validBlocks(SimBlocks.ABSORBER)
            .renderer(() -> AbsorberRenderer::new)
            .register();
*/

    public static final BlockEntityEntry<AugerShaftBlockEntity> AUGER_SHAFT = REGISTRATE
            .blockEntity("auger_shaft", AugerShaftBlockEntity::new)
            .visual(() -> (ctx, blockEntity, partialTick) -> {
                if (blockEntity.getBlockState().getBlock() instanceof AugerCogBlock) {
                    return new AugerCogVisual(ctx, blockEntity, partialTick);
                }
                return SingleAxisRotatingVisual.shaft(ctx, blockEntity, partialTick);
            })
            .onRegister(SimInventoryService.INSTANCE.registerInventory((be, dir) -> {
                if (dir == null) {
                    return null; //don't just give our inventory to non-directional requests.
                } else {
					if (be.getBlockState().is(SimBlocks.AUGER_COG)) {
						return null; //don't give out inventory to requests when we are a cog auger.
					}

                    return dir.getAxis() == be.getBlockState().getValue(AugerShaftBlock.AXIS) ? null : be.getInventory();
                }
            }))
            .validBlocks(SimBlocks.AUGER_SHAFT, SimBlocks.AUGER_COG)
            .renderer(() -> AugerShaftRenderer::new)
            .register();

    public static final BlockEntityEntry<MergingGlueBlockEntity> MERGING_GLUE = REGISTRATE
            .blockEntity("merging_glue", MergingGlueBlockEntity::new)
            .validBlocks(SimBlocks.MERGING_GLUE)
            .renderer(() -> MergingGlueRenderer::new)
            .register();

    public static final BlockEntityEntry<DirectionalGearshiftBlockEntity> DIRECTIONAL_GEARSHIFT = REGISTRATE
            .blockEntity("directional_gearshift", DirectionalGearshiftBlockEntity::new)
            .visual(() -> SplitShaftVisual::new)
            .renderer(() -> DirectionalGearshiftRenderer::new)
            .validBlock(SimBlocks.DIRECTIONAL_GEARSHIFT)
            .register();

    public static void register() {
    }
}