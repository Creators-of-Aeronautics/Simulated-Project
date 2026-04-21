package dev.simulated_team.simulated.util;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin.accessor.ContraptionAccessor;
import dev.simulated_team.simulated.mixin.accessor.ControlledContraptionEntityAccessor;
import dev.simulated_team.simulated.mixin_interface.create_assembly.IControlContraptionExtension;
import dev.simulated_team.simulated.util.assembly.SimAssemblyContraption;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class SimAssemblyHelper {
    public static void disassembleSubLevel(@NotNull final Level level,
                                           @NotNull final SubLevel toDisassemble,
                                           @NotNull final BlockPos subLevelAnchor,
                                           @NotNull final BlockPos disassemblyGoal,
                                           @NotNull final Rotation rotation,
                                           @NotNull final boolean playSound) {
        if (playSound) {
            level.playSound(null, subLevelAnchor, SimSoundEvents.SIMULATED_CONTRAPTION_STOPS.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        final BoundingBox3i plotBounds = new BoundingBox3i(toDisassemble.getPlot().getBoundingBox());
        final SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(subLevelAnchor, disassemblyGoal, rotation == Rotation.NONE ? 0 : (4 - rotation.ordinal()), rotation, (ServerLevel) level);

        final ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
        final ObjectArrayList<BlockPos> brittleBlocks = new ObjectArrayList<>();

        final LevelPlot plot = toDisassemble.getPlot();
        for (final PlotChunkHolder chunk : plot.getLoadedChunks()) {
            final BoundingBox3ic localChunkBounds = chunk.getBoundingBox();

            if (localChunkBounds == null || localChunkBounds == BoundingBox3i.EMPTY) {
                continue;
            }

            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
                for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                    for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                        final BlockPos pos = new BlockPos(
                                x + chunk.getPos().getMinBlockX(),
                                y,
                                z + chunk.getPos().getMinBlockZ()
                        );
                        final BlockState state = level.getBlockState(pos);
                        if (state.isAir()) { continue; }

                        final Block block = state.getBlock();

                        // Dirty way of checking for affected blocks.
                        if (state.is(AllTags.AllBlockTags.BRITTLE.tag) || state.is(AllTags.AllBlockTags.WRENCH_PICKUP.tag)
                                || block instanceof DiodeBlock || block instanceof TorchBlock || block instanceof SignBlock ) { brittleBlocks.add(pos); continue; }

                        // Add non-brittle block.
                        blocks.add(pos);
                    }
                }
            }
        }

        // Add all the brittle blocks in the front so they are processed first.
        blocks.addAll(0, brittleBlocks);

        disassembleAndAddCreateContraptions(level, plot.getBoundingBox(), blocks, false, null);

        // move glue
        final PersistentEntitySectionManager<Entity> manager = ((ServerLevel) toDisassemble.getLevel()).entityManager;
        for (final PlotChunkHolder chunk : toDisassemble.getPlot().getLoadedChunks()) {
            final Stream<EntitySection<Entity>> sections = manager.sectionStorage.getExistingSectionsInChunk(chunk.getPos().toLong());

            for (final EntitySection<Entity> section : sections.toList()) {
                final List<Entity> entities = section.getEntities().toList();

                for (final Entity entity : entities) {
                    AABB box = entity.getBoundingBox();

                    box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)),
                            transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));

                    if (entity instanceof SuperGlueEntity) {
                        entity.remove(Entity.RemovalReason.KILLED);
                        level.addFreshEntity(new SuperGlueEntity(level, box));
                        continue;
                    }

                    if (entity instanceof HoneyGlueEntity) {
                        entity.remove(Entity.RemovalReason.KILLED);
                        final HoneyGlueEntity newHoneyGlue = new HoneyGlueEntity(level, box);
                        level.addFreshEntity(newHoneyGlue);
                        newHoneyGlue.setBoundsAndSync(box);
                        continue;
                    }

                    final Vec3 newPos = transform.apply(entity.position());
                    entity.setPos(newPos);
                    entity.setYRot(entity.rotate(transform.getRotation()));
                    entity.yRotO = entity.getYRot();

                    if (entity instanceof final HangingEntity hangingEntity) {
                        hangingEntity.recalculateBoundingBox();
                    }

                    entity.levelCallback.onRemove(Entity.RemovalReason.CHANGED_DIMENSION);
                    ((ServerLevel) level).addDuringTeleport(entity);
                }
            }
        }

        // if there's no blocks in the given sublevel, don't attempt to move the blocks
        if (!blocks.isEmpty()) {
            ((ServerLevelPlot) toDisassemble.getPlot()).kickAllEntities();
            SubLevelAssemblyHelper.moveBlocks((ServerLevel) level, transform, blocks);
        }

        SubLevelAssemblyHelper.moveTrackingPoints((ServerLevel) level, plotBounds, null, transform);
    }

    public static AssemblyResult assembleFromSingleBlock(final Level level, final BlockPos selfPos, final BlockPos toAssemble, final boolean includeStart, final boolean includeEncasingGlue) throws AssemblyException {
        if (level.getBlockState(toAssemble).isAir()) {
            return null;
        }

        final SimAssemblyContraption contraption = new SimAssemblyContraption(includeStart ? null : selfPos, !includeEncasingGlue);

        contraption.searchMovedStructure(level, toAssemble);

        final Collection<BlockPos> blocks = contraption.getBlocks();
        if (!blocks.isEmpty()) {
            final BoundingBox3i bounds = BoundingBox3i.from(blocks);
            final Collection<SuperGlueEntity> superGlues = contraption.getGlues();
            final Collection<HoneyGlueEntity> honeyGlues = contraption.getHoneyGlues();

            final ObjectArrayList<AABB> collectedContraptionGlues = new ObjectArrayList<>();
            disassembleAndAddCreateContraptions(level, bounds, blocks, true, collectedContraptionGlues);

            final BlockPos anchor = blocks.stream().findFirst().get();
            final SubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel) level, anchor, blocks, bounds);

            if (subLevel != null) {
                final BlockPos offsetBlocks = subLevel.getPlot().getCenterBlock().subtract(anchor);

                // move glues from contraptions
                for (final AABB box : collectedContraptionGlues) {
                    level.addFreshEntity(new SuperGlueEntity(level, box.move(Vec3.atLowerCornerOf(offsetBlocks))));
                }

                for (final SuperGlueEntity glue : superGlues) {
                    glue.remove(Entity.RemovalReason.KILLED);
                    level.addFreshEntity(new SuperGlueEntity(level, glue.getBoundingBox().move(Vec3.atLowerCornerOf(offsetBlocks))));
                }

                for (final HoneyGlueEntity glue : honeyGlues) {
                    glue.remove(Entity.RemovalReason.KILLED);
                    final AABB newBB = glue.getBoundingBox().move(Vec3.atLowerCornerOf(offsetBlocks));
                    final HoneyGlueEntity entity = new HoneyGlueEntity(level, newBB);
                    level.addFreshEntity(entity);
                    entity.setBoundsAndSync(newBB);
                }

                level.playSound(null, selfPos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0f, 1.0f);
                return new AssemblyResult(subLevel, offsetBlocks);
            }
        }

        return null;
    }

    private static void disassembleAndAddCreateContraptions(final Level level, final BoundingBox3ic assemblyBounds, final Collection<BlockPos> blocks, final boolean passGluesBack, final List<AABB> collectedGlues) {
        assert assemblyBounds != null;

        final AABB assemblyBoundsD = new AABB(
                assemblyBounds.minX(), assemblyBounds.minY(), assemblyBounds.minZ(),
                assemblyBounds.maxX() + 1, assemblyBounds.maxY() + 1, assemblyBounds.maxZ() + 1
        );

        // disassemble all of the contraptions controlled by these blocks
        // and add their blocks to be moved as-well
        final List<ControlledContraptionEntity> intersectingContraptions = level.getEntitiesOfClass(ControlledContraptionEntity.class, assemblyBoundsD.inflate(2.0));

        for (final ControlledContraptionEntity contraptionEntity : intersectingContraptions) {
            final ControlledContraptionEntityAccessor accessor = ((ControlledContraptionEntityAccessor) contraptionEntity);
            final BlockPos controllerPos = accessor.getControllerPos();

            if (blocks.contains(controllerPos)) {
                // We need to disassemble this contraption & assemble all of its blocks
                final Contraption contraption = contraptionEntity.getContraption();
                final StructureTransform transform = accessor.invokeMakeStructureTransform();

                for (final BlockPos contraptionBlock : contraption.getBlocks().keySet()) {
                    final BlockPos targetPos = transform.apply(contraptionBlock);
                    blocks.add(targetPos);
                }

                if (passGluesBack) {
                    final List<AABB> superGlue = ((ContraptionAccessor) contraption).getSuperGlue();
                    for (AABB aabb : superGlue) {
                        aabb = new AABB(transform.apply(new Vec3(aabb.minX, aabb.minY, aabb.minZ)),
                                transform.apply(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)));

                        collectedGlues.add(aabb);
                    }
                    superGlue.clear();
                }
                contraptionEntity.disassemble();

                final BlockEntity blockEntity = level.getBlockEntity(controllerPos);
                if (blockEntity instanceof final IControlContraptionExtension controlContraption) {
                    controlContraption.sable$disassemble();
                }
            }
        }
    }

    public static Rotation rotationFrom90DegRots(final int rots) {
        return switch (Math.floorMod(rots, 4)) {
            case 0 -> Rotation.NONE;
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> throw new AssertionError(); // unreachable
        };
    }

    public static void register() {
        SubLevelHeatMapManager.addSplitListener(SimAssemblyHelper::addSplitBlocks);
    }

    private static void addSplitBlocks(final Level level, final BoundingBox3ic boundingBox3ic, final Collection<BlockPos> blocks) {
        disassembleAndAddCreateContraptions(level, boundingBox3ic, blocks, false, null);
    }

    public record AssemblyResult(SubLevel subLevel, BlockPos offset) { }
}