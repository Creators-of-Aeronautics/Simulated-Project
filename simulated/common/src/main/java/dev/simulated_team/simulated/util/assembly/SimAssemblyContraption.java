package dev.simulated_team.simulated.util.assembly;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.chassis.ChassisBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.service.SimAssemblyService;
import dev.simulated_team.simulated.service.SimConfigService;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.UniqueLinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;

import java.util.*;

import static com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.isExtensionPole;
import static com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.isPistonHead;

/**
 * It absolutely sucks that we have to do this. Talk to thunder about making assembly API separated out from contraptions?
 */
public class SimAssemblyContraption {
    private static final BlockPos[] DIRECTION_OFFSETS = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(1, 1, 0), new BlockPos(-1, -1, 0), new BlockPos(1, -1, 0), new BlockPos(-1, 1, 0), new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(0, 1, 1), new BlockPos(0, -1, -1), new BlockPos(0, -1, 1), new BlockPos(0, 1, -1)};

    public final BlockPos anchor;
    public final boolean ignoreEnclosingGlue;
    private final ObjectOpenHashSet<BlockPos> blocks = new ObjectOpenHashSet<>(1 << 12);
    private final ObjectOpenHashSet<SuperGlueEntity> glueCache = new ObjectOpenHashSet<>();
    private final ObjectOpenHashSet<HoneyGlueEntity> honeyGlueCache = new ObjectOpenHashSet<>();

    public SimAssemblyContraption(final BlockPos selfPos, final boolean includeStart, boolean ignoreEnclosingGlue) {
        this.anchor = includeStart ? null : selfPos;
        this.ignoreEnclosingGlue = ignoreEnclosingGlue;
        if (includeStart) {
            this.blocks.add(selfPos);
        }
    }

    public boolean checkAndCacheGlue(final LevelAccessor level, final BlockPos blockPos, final BlockPos offsetDir) {
        final BlockPos targetPos = blockPos.offset(offsetDir);

        boolean inHoneyGlue = false;

        boolean containedByAnyHoneyGlue = false;
        for (final HoneyGlueEntity honeyGlueEntity : this.honeyGlueCache) {
            final boolean firstContained = honeyGlueEntity.contains(blockPos);
            final boolean targetContained = honeyGlueEntity.contains(targetPos);
            containedByAnyHoneyGlue |= firstContained;
            containedByAnyHoneyGlue |= targetContained;
            if (firstContained && targetContained)
                inHoneyGlue = true;
        }

        if (containedByAnyHoneyGlue) {
            final int honeyGlueRange = SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();
            for (final HoneyGlueEntity honeyGlueEntity : level.getEntitiesOfClass(HoneyGlueEntity.class,
                    SuperGlueEntity.span(blockPos, targetPos).inflate(honeyGlueRange))) {
                if (this.anchor != null && this.ignoreEnclosingGlue && honeyGlueEntity.contains(this.anchor)) {
                    continue;
                }

                if (!honeyGlueEntity.contains(blockPos) || !honeyGlueEntity.contains(targetPos))
                    continue;

                this.honeyGlueCache.add(honeyGlueEntity);
                inHoneyGlue = true;
            }
        }

        for (final SuperGlueEntity glueEntity : this.glueCache)
            if (glueEntity.contains(blockPos) && glueEntity.contains(targetPos))
                return true;


        for (final SuperGlueEntity glueEntity : level.getEntitiesOfClass(SuperGlueEntity.class,
                SuperGlueEntity.span(blockPos, targetPos).inflate(16))) {
            if (!glueEntity.contains(blockPos) || !glueEntity.contains(targetPos))
                continue;
            this.glueCache.add(glueEntity);
            return true;
        }

        return inHoneyGlue;
    }

    public boolean searchMovedStructure(final Level level, final BlockPos pos)
            throws AssemblyException {

        // add initial honey glue
        SimAssemblyContraption.addInitialHoneyGlue(level, this, this.anchor, pos, this.ignoreEnclosingGlue);

        final Queue<BlockPos> frontier = new UniqueLinkedList<>();
        final Set<BlockPos> visited = new HashSet<>();
        final Set<BlockPos> immutableVisited = Collections.unmodifiableSet(visited);

        if (!BlockMovementChecks.isBrittle(level.getBlockState(pos)))
            frontier.add(pos);

        final int maxBlocksMoved = SimConfigService.INSTANCE.server().assembly.maxBlocksMoved.get();
        for (int limit = maxBlocksMoved; limit > 0; limit--) {
            if (frontier.isEmpty())
                return true;
            if (!this.moveBlock(level, frontier, visited, immutableVisited))
                return false;
        }
        throw SimAssemblyException.structureTooLarge();
    }

    protected static void addInitialHoneyGlue(final Level level, final SimAssemblyContraption contraption, final BlockPos anchor, final BlockPos pos, final boolean ignoreEnclosingGlue) {
        final int honeyGlueRange = SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();
        for (final HoneyGlueEntity honeyGlueEntity : level.getEntitiesOfClass(HoneyGlueEntity.class,
                SuperGlueEntity.span(pos, pos).inflate(honeyGlueRange))) {

            if (anchor != null) {
                if (ignoreEnclosingGlue && honeyGlueEntity.contains(anchor)) {
                    continue;
                }

                if (!honeyGlueEntity.contains(pos) && !honeyGlueEntity.contains(anchor)) {
                    continue;
                }
            } else {
                if (!honeyGlueEntity.contains(pos)) {
                    continue;
                }
            }

            contraption.honeyGlueCache.add(honeyGlueEntity);
        }
    }

    /**
     * add the first block in frontier queue
     */
    protected boolean moveBlock(final Level world, final Queue<BlockPos> frontier,
                                final Set<BlockPos> visited, final Set<BlockPos> immutableVisitedView) throws AssemblyException {
        final BlockPos pos = frontier.poll();
        if (pos == null) {
            return false;
        }

        visited.add(pos);

        if (world.isOutsideBuildHeight(pos)) {
            return true;
        }

        if (!world.isLoaded(pos)) {
            throw AssemblyException.unloadedChunk(pos);
        }

        if (this.isAnchoringBlockAt(pos)) {
            return true;
        }

        final BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }

        if (!this.movementAllowed(state, world, pos)) {
            throw AssemblyException.unmovableBlock(pos, state);
        }

        //chassis require frontier, so they're handled here, outside of our API
        if (state.getBlock() instanceof AbstractChassisBlock
                && !this.moveChassis(world, pos, null, frontier, visited)) {
            return false;
        }

        //swivel bearings have special cases with honey glue and glue, ignored in our API
        if (SimBlocks.SWIVEL_BEARING.has(state)) {
            this.moveSwivelBearing(world, pos, frontier, visited, state);
        }

        if (world.getBlockEntity(pos) instanceof final ChainConveyorBlockEntity ccbe) {
            ccbe.notifyConnectedToValidate();
        }

        // Double Chest halves stick together
        if (state.hasProperty(ChestBlock.TYPE) && state.hasProperty(ChestBlock.FACING)
                && (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)) {
            final Direction offset = ChestBlock.getConnectedDirection(state);
            final BlockPos attached = pos.relative(offset);
            if (!visited.contains(attached))
                frontier.add(attached);
        }

        // Bogeys tend to have sticky sides
        if (state.getBlock() instanceof final AbstractBogeyBlock<?> bogey) {
            for (final Direction d : bogey.getStickySurfaces(world, pos, state)) {
                if (!visited.contains(pos.relative(d))) {
                    frontier.add(pos.relative(d));
                }
            }
        }

        // Cart assemblers attach themselves
        final BlockPos posDown = pos.below();
        final BlockState stateBelow = world.getBlockState(posDown);
        if (!visited.contains(posDown) && AllBlocks.CART_ASSEMBLER.has(stateBelow)) {
            frontier.add(posDown);
        }

        //add additional block positons to the frontier through our API
        SimBlockMovementChecks.addAdditionalBlocks(state, world, pos, frontier, immutableVisitedView);

        // Slime blocks and super glue drag adjacent blocks if possible
        for (final BlockPos offsetDirection : DIRECTION_OFFSETS) {
            final int absTotal = Math.abs(offsetDirection.getX()) + Math.abs(offsetDirection.getY()) + Math.abs(offsetDirection.getZ());
            final Direction offsetDirectionNullable = absTotal == 1 ? Direction.fromDelta(offsetDirection.getX(), offsetDirection.getY(), offsetDirection.getZ()) : null;

            final BlockPos offsetPos = pos.offset(offsetDirection);
            final BlockState blockState = world.getBlockState(offsetPos);
            if (this.isAnchoringBlockAt(offsetPos)) {
                continue;
            }

            if (!this.movementAllowed(blockState, world, offsetPos)) {
                continue;
            }

            final boolean wasVisited = visited.contains(offsetPos);
            final boolean faceHasGlue = this.checkAndCacheGlue(world, pos, offsetDirection);
            boolean blockAttachedTowardsFace = offsetDirectionNullable != null && BlockMovementChecks.isBlockAttachedTowards(blockState, world, offsetPos, offsetDirectionNullable.getOpposite());
            blockAttachedTowardsFace |= SimBlockMovementChecks.checkIsBlockAttachedTowards(blockState, world, offsetPos, offsetDirection.multiply(-1));

            final boolean brittle = BlockMovementChecks.isBrittle(blockState);

            boolean canStick = !brittle &&
                    SimAssemblyService.INSTANCE.canStickTo(state, blockState) &&
                    SimAssemblyService.INSTANCE.canStickTo(blockState, state);

            if (canStick) {
                if (state.getPistonPushReaction() == PushReaction.PUSH_ONLY
                        || blockState.getPistonPushReaction() == PushReaction.PUSH_ONLY) {
                    canStick = false;
                }

                if (offsetDirectionNullable != null) {
                    if (BlockMovementChecks.isNotSupportive(state, offsetDirectionNullable)) {
                        canStick = false;
                    }

                    if (BlockMovementChecks.isNotSupportive(blockState, offsetDirectionNullable.getOpposite())) {
                        canStick = false;
                    }
                }
            }

            if (!wasVisited && (canStick || blockAttachedTowardsFace || faceHasGlue)) {
                frontier.add(offsetPos);
            }
        }

        this.blocks.add(pos);
        if (this.blocks.size() <= SimConfigService.INSTANCE.server().assembly.maxBlocksMoved.get()) {
            return true;
        } else {
            throw SimAssemblyException.structureTooLarge();
        }
    }

    private void moveSwivelBearing(final Level level, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited, final BlockState state) {
        final Direction facing = state.getValue(SwivelBearingBlock.FACING);
        final BlockPos attachPos = pos.relative(facing);

        SimAssemblyContraption.addInitialHoneyGlue(level, this, pos, attachPos, true);

        frontier.add(attachPos);
    }

    protected void movePistonHead(final Level world, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited,
                                  final BlockState state) {
        final Direction direction = state.getValue(MechanicalPistonHeadBlock.FACING);
        final BlockPos offset = pos.relative(direction.getOpposite());
        if (!visited.contains(offset)) {
            final BlockState blockState = world.getBlockState(offset);
            if (isExtensionPole(blockState) && blockState.getValue(PistonExtensionPoleBlock.FACING)
                    .getAxis() == direction.getAxis())
                frontier.add(offset);
            if (blockState.getBlock() instanceof MechanicalPistonBlock) {
                final Direction pistonFacing = blockState.getValue(MechanicalPistonBlock.FACING);
                if (pistonFacing == direction
                        && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED)
                    frontier.add(offset);
            }
        }
        if (state.getValue(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY) {
            final BlockPos attached = pos.relative(direction);
            if (!visited.contains(attached))
                frontier.add(attached);
        }
    }

    protected void movePistonPole(final Level world, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited,
                                  final BlockState state) {
        for (final Direction d : Iterate.directionsInAxis(state.getValue(PistonExtensionPoleBlock.FACING)
                .getAxis())) {
            final BlockPos offset = pos.relative(d);
            if (!visited.contains(offset)) {
                final BlockState blockState = world.getBlockState(offset);
                if (isExtensionPole(blockState) && blockState.getValue(PistonExtensionPoleBlock.FACING)
                        .getAxis() == d.getAxis())
                    frontier.add(offset);
                if (isPistonHead(blockState) && blockState.getValue(MechanicalPistonHeadBlock.FACING)
                        .getAxis() == d.getAxis())
                    frontier.add(offset);
                if (blockState.getBlock() instanceof MechanicalPistonBlock) {
                    final Direction pistonFacing = blockState.getValue(MechanicalPistonBlock.FACING);
                    if (pistonFacing == d || pistonFacing == d.getOpposite()
                            && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED)
                        frontier.add(offset);
                }
            }
        }
    }

    protected void moveGantryPinion(final Level world, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited,
                                    final BlockState state) {
        BlockPos offset = pos.relative(state.getValue(GantryCarriageBlock.FACING));
        if (!visited.contains(offset))
            frontier.add(offset);
        final Axis rotationAxis = ((IRotate) state.getBlock()).getRotationAxis(state);
        for (final Direction d : Iterate.directionsInAxis(rotationAxis)) {
            offset = pos.relative(d);
            final BlockState offsetState = world.getBlockState(offset);
            if (AllBlocks.GANTRY_SHAFT.has(offsetState) && offsetState.getValue(GantryShaftBlock.FACING)
                    .getAxis() == d.getAxis())
                if (!visited.contains(offset))
                    frontier.add(offset);
        }
    }

    protected void moveGantryShaft(final Level world, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited,
                                   final BlockState state) {
        for (final Direction d : Iterate.directions) {
            final BlockPos offset = pos.relative(d);
            if (!visited.contains(offset)) {
                final BlockState offsetState = world.getBlockState(offset);
                final Direction facing = state.getValue(GantryShaftBlock.FACING);
                if (d.getAxis() == facing.getAxis() && AllBlocks.GANTRY_SHAFT.has(offsetState)
                        && offsetState.getValue(GantryShaftBlock.FACING) == facing)
                    frontier.add(offset);
                else if (AllBlocks.GANTRY_CARRIAGE.has(offsetState)
                        && offsetState.getValue(GantryCarriageBlock.FACING) == d)
                    frontier.add(offset);
            }
        }
    }

//    private void movePulley(final Level world, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited) {
//        int limit = AllConfigs.server().kinetics.maxRopeLength.get();
//        BlockPos ropePos = pos;
//        while (limit-- >= 0) {
//            ropePos = ropePos.below();
//            if (!world.isLoaded(ropePos))
//                break;
//            final BlockState ropeState = world.getBlockState(ropePos);
//            final Block block = ropeState.getBlock();
//            if (!(block instanceof RopeBlock) && !(block instanceof MagnetBlock)) {
//                if (!visited.contains(ropePos))
//                    frontier.add(ropePos);
//                break;
//            }
//            this.blocks.add(ropePos);
//        }
//    }

    private boolean moveMechanicalPiston(final Level world, final BlockPos pos, final Queue<BlockPos> frontier, final Set<BlockPos> visited,
                                         final BlockState state) throws AssemblyException {
        final Direction direction = state.getValue(MechanicalPistonBlock.FACING);
        final PistonState pistonState = state.getValue(MechanicalPistonBlock.STATE);
        if (pistonState == PistonState.MOVING)
            return false;

        BlockPos offset = pos.relative(direction.getOpposite());
        if (!visited.contains(offset)) {
            final BlockState poleState = world.getBlockState(offset);
            if (AllBlocks.PISTON_EXTENSION_POLE.has(poleState) && poleState.getValue(PistonExtensionPoleBlock.FACING)
                    .getAxis() == direction.getAxis())
                frontier.add(offset);
        }

        if (pistonState == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state)) {
            offset = pos.relative(direction);
            if (!visited.contains(offset))
                frontier.add(offset);
        }

        return true;
    }

    private boolean moveChassis(final Level world, final BlockPos pos, final Direction movementDirection, final Queue<BlockPos> frontier,
                                final Set<BlockPos> visited) {
        final BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof final ChassisBlockEntity chassis))
            return false;
        chassis.addAttachedChasses(frontier, visited);
        final List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(movementDirection, false);
        if (includedBlockPositions == null)
            return false;
        for (final BlockPos blockPos : includedBlockPositions)
            if (!visited.contains(blockPos))
                frontier.add(blockPos);
        return true;
    }

    protected boolean movementAllowed(final BlockState state, final Level world, final BlockPos pos) {
        return state.getDestroySpeed(world, pos) != -1 && !state.is(SimTags.Blocks.NON_MOVABLE);
    }

    protected boolean isAnchoringBlockAt(final BlockPos pos) {
        return pos.equals(this.anchor);
    }

    public Collection<SuperGlueEntity> getGlues() {
        return this.glueCache;
    }


    public Collection<HoneyGlueEntity> getHoneyGlues() {
        return this.honeyGlueCache;
    }

    public Collection<BlockPos> getBlocks() {
        return this.blocks;
    }
}
