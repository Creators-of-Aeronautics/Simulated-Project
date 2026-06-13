package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class AltitudeSensorBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<AltitudeSensorBlockEntity>, IWrenchable, CommonRedstoneBlock {
    public static final EnumProperty<FaceType> DIAL = EnumProperty.create("dial", FaceType.class);
    public static final MapCodec<AltitudeSensorBlock> CODEC = simpleCodec(AltitudeSensorBlock::new);

    public AltitudeSensorBlock(final Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();

        AttachFace face = AttachFace.FLOOR;

        if (context.getClickedFace() == Direction.DOWN) {
            face = AttachFace.CEILING;
        } else if (context.getClickedFace().getAxis().isHorizontal()) {
            face = AttachFace.WALL;
            facing = context.getClickedFace();
        }

        return this.defaultBlockState()
                .setValue(HORIZONTAL_FACING, facing)
                .setValue(FACE, face)
                .setValue(DIAL, FaceType.LINEAR);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, FACE, DIAL);
        super.createBlockStateDefinition(pBuilder);
    }

    @Override
    public @NotNull VoxelShape getShape(final BlockState state,
                                        final @NotNull BlockGetter level,
                                        final @NotNull BlockPos pos,
                                        final @NotNull CollisionContext context) {
        if (state.getValue(FACE) == AttachFace.FLOOR)
            return SimBlockShapes.ALTITUDE_SENSOR_FLOOR.get(state.getValue(HORIZONTAL_FACING));
        if (state.getValue(FACE) == AttachFace.CEILING)
            return SimBlockShapes.ALTITUDE_SENSOR_CEILING.get(state.getValue(HORIZONTAL_FACING));
        return SimBlockShapes.ALTITUDE_SENSOR_WALL.get(state.getValue(HORIZONTAL_FACING));
    }

    @Override
    public Class<AltitudeSensorBlockEntity> getBlockEntityClass() {
        return AltitudeSensorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AltitudeSensorBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.ALTITUDE_SENSOR.get();
    }

    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    public int getSignal(final @NotNull BlockState state,
                         final BlockGetter level,
                         final @NotNull BlockPos pos,
                         final @NotNull Direction direction) {
        final AltitudeSensorBlockEntity be = (AltitudeSensorBlockEntity) level.getBlockEntity(pos);
        return be.signal;
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
        if (direction != Direction.UP) {
            return 0;
        }

        return this.getSignal(state, level, pos, direction);
    }

    @Override
    public boolean commonConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
        return direction != null;
    }

    @Override
    public boolean commonCheckWeakPower(final BlockState state, final SignalGetter level, final BlockPos pos, final Direction side) {
        return true;
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        if (context.getClickedFace() == state.getValue(HORIZONTAL_FACING)) {
            IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());

            // swap face type
            FaceType faceType = state.getValue(DIAL);

            if (faceType == FaceType.LINEAR) {
                faceType = FaceType.RADIAL;
            } else {
                faceType = FaceType.LINEAR;
            }

            context.getLevel().setBlock(context.getClickedPos(), state.setValue(DIAL, faceType), 3);

            return InteractionResult.SUCCESS;
        }
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(final @NotNull ItemStack stack,
                                                       final @NotNull BlockState state,
                                                       final @NotNull Level level,
                                                       final @NotNull BlockPos pos,
                                                       final @NotNull Player player,
                                                       final @NotNull InteractionHand hand,
                                                       final @NotNull BlockHitResult hitResult) {
        return AllItems.WRENCH.isIn(stack) ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : this.onBlockEntityUseItemOn(level, pos, (be) -> {

            if (level.isClientSide) {
                this.withBlockEntityDo(level, pos, AltitudeSensorScreen::open);
            }

            return ItemInteractionResult.SUCCESS;
        });
    }

    public enum FaceType implements StringRepresentable {
        LINEAR,
        RADIAL;

        @Override
        public @NotNull String getSerializedName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }
    }


}
