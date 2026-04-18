package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class AugerShaftBlock extends RotatedPillarKineticBlock implements IBE<AugerShaftBlockEntity> {

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public static final EnumProperty<BarrelSection> SECTION = EnumProperty.create("section", BarrelSection.class);
    public static final BooleanProperty COG = BooleanProperty.create("cog");

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final BooleanProperty ENCASED = BooleanProperty.create("encased");

    public static final Map<Direction, BooleanProperty>
            PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), (map) -> {
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
    }));

    public AugerShaftBlock(final Properties properties) {
        super(properties);

        this.registerDefaultState(super.defaultBlockState()
                .setValue(SECTION, BarrelSection.SINGLE)
                .setValue(COG, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(ENCASED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(SECTION, COG, NORTH, EAST, SOUTH, WEST, UP, DOWN, ENCASED));
    }

    private boolean connects(final BlockPos pos, final BlockState state, final BlockPos otherPos, final BlockState otherState) {
        return (otherState.getBlock() == this &&
                otherState.getValue(AXIS) == state.getValue(AXIS));
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldItem, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        final IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        if (helper.matchesItem(heldItem))
            return helper.getOffset(player, level, blockState, blockPos, blockHitResult)
                    .placeInWorld(level, (BlockItem) heldItem.getItem(), player, interactionHand, blockHitResult);

        if (!(blockState.getBlock() instanceof AugerCogBlock)) {
            final Boolean encased = blockState.getValue(ENCASED);
            if (encased && player.getItemInHand(interactionHand).is(AllItems.WRENCH.get())) {
                if (level.isClientSide)
                    return ItemInteractionResult.SUCCESS;

                level.setBlockAndUpdate(blockPos, blockState.cycle(ENCASED));
                level.levelEvent(2001, blockPos, Block.getId(AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState()));
                return ItemInteractionResult.SUCCESS;
            } else if (!encased && player.getItemInHand(interactionHand).is(AllBlocks.INDUSTRIAL_IRON_BLOCK.asItem())) {
                if (level.isClientSide)
                    return ItemInteractionResult.SUCCESS;

                level.setBlockAndUpdate(blockPos, blockState.cycle(ENCASED));
                level.playSound(null, blockPos, SimSoundEvents.AUGER_SHAFT_ENCASING.event(), SoundSource.BLOCKS, 0.5F, 1.05F);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return super.useItemOn(heldItem, blockState, level, blockPos, player, interactionHand, blockHitResult);
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        return this.transformAuger(state, SimBlocks.AUGER_COG.getDefaultState(), context, level);
    }

    @Nullable
    protected InteractionResult transformAuger(final BlockState state, final BlockState newState, final UseOnContext context, final Level level) {
        final RegistryAccess reg = level.registryAccess();

        final AugerShaftBlockEntity abe = this.getBlockEntity(level, context.getClickedPos());
        if (abe != null) {
            final CompoundTag tag = new CompoundTag();
            abe.write(tag, reg, false);
            abe.beingWrenched = true;

            KineticBlockEntity.switchToBlockState(level, context.getClickedPos(), newState.setValue(AXIS, state.getValue(AXIS)));
            final AugerShaftBlockEntity newBE = this.getBlockEntity(level, context.getClickedPos());
            if (newBE != null) {
                newBE.read(tag, reg, false);
                newBE.notifyUpdate();

                IWrenchable.playRotateSound(level, context.getClickedPos());
                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction dir, final BlockState neighborState, final LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
        //gather axis information
        final Direction.Axis axis = state.getValue(AXIS);
        final Direction directionPos = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        final Direction directionNegative = Direction.get(Direction.AxisDirection.NEGATIVE, axis);

        //gather block positions
        final BlockPos posPos = pos.relative(directionPos);
        final BlockPos posNeg = pos.relative(directionNegative);

        //gather associated states
        final BlockState statePos = level.getBlockState(posPos);
        final BlockState stateNeg = level.getBlockState(posNeg);

        // set section based on if the block is connected to another barrel
        BarrelSection section = BarrelSection.SINGLE;
        if (this.connects(pos, state, posPos, statePos) && !this.connects(pos, state, posNeg, stateNeg)) {
            section = BarrelSection.END;
        } else if (!this.connects(pos, state, posPos, statePos) && this.connects(pos, state, posNeg, stateNeg)) {
            section = BarrelSection.FRONT;
        } else if (this.connects(pos, state, posPos, statePos) && this.connects(pos, state, posNeg, stateNeg)) {
            section = BarrelSection.MIDDLE;
        }


        BlockState mutState = state.setValue(SECTION, section);

        final boolean isFunnel = neighborState.getBlock() instanceof AbstractFunnelBlock;
        final boolean hasHorizontalFacing = neighborState.hasProperty(HORIZONTAL_FACING);
        final boolean hasFacing = neighborState.hasProperty(FACING);
        if ((isFunnel && ((hasHorizontalFacing && neighborState.getValue(HORIZONTAL_FACING) == dir) ||
                (hasFacing && neighborState.getValue(FACING) == dir))) ||
                (dir.getAxis().isVertical() && neighborState.getBlock() instanceof ChuteBlock)) {
            mutState = mutState.setValue(PROPERTY_BY_DIRECTION.get(dir), true);
        } else {
            mutState = mutState.setValue(PROPERTY_BY_DIRECTION.get(dir), false);
        }

        return super.updateShape(mutState, dir, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
        final BarrelSection section = state.getValue(SECTION);
        if (state.getValue(AugerShaftBlock.ENCASED) || section.equals(BarrelSection.SINGLE)) {
            return Shapes.block();
        }

        final Direction.Axis axis = state.getValue(AXIS);
        if (!section.equals(BarrelSection.MIDDLE)) {
            return SimBlockShapes.AUGER_END_SHAPE.get(section.equals(BarrelSection.FRONT) ? Direction.get(Direction.AxisDirection.NEGATIVE, axis) : Direction.get(Direction.AxisDirection.POSITIVE, axis));
        }

        return SimBlockShapes.FOURTEEN_VOXEL_POLE.get(axis);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
//        this.withBlockEntityDo(level, pos, (be) -> be.stopped = false);

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    @Override
    public Direction.Axis getRotationAxis(final BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<AugerShaftBlockEntity> getBlockEntityClass() {
        return AugerShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AugerShaftBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.AUGER_SHAFT.get();
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        return face.getAxis() == this.getRotationAxis(state);
    }

    public enum BarrelSection implements StringRepresentable {
        FRONT,
        MIDDLE,
        END,
        SINGLE;

        @Override
        public String getSerializedName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper extends PoleHelper<Direction.Axis> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof AugerShaftBlock, state -> state.getValue(AXIS), AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof final BlockItem bi && bi.getBlock() instanceof AugerShaftBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return Predicates.or(SimBlocks.AUGER_SHAFT::has, SimBlocks.AUGER_COG::has);
        }

        @Override
        public PlacementOffset getOffset(final Player player, final Level world, final BlockState state, final BlockPos pos, final BlockHitResult ray) {
            return super.getOffset(player, world, state, pos, ray);
        }

    }
}
