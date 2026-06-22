package dev.simulated_team.simulated.content.blocks.rope.rope_connector;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.simulated_team.simulated.content.blocks.rope.RopeHolderBlock;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RopeConnectorBlock extends AbstractDirectionalAxisBlock implements IBE<RopeConnectorBlockEntity>, RopeHolderBlock<RopeConnectorBlockEntity>, BlockSubLevelAssemblyListener, BlockSubLevelCollisionShape {
    public static final MapCodec<RopeConnectorBlock> CODEC = simpleCodec(RopeConnectorBlock::new);
    private static final DirectionalAxisShaper SHAPE = DirectionalAxisShaper.make(SimBlockShapes.ROPE_CONNECTOR);
    private static final DirectionalAxisShaper PHYSICS_COLLIDER = DirectionalAxisShaper.make(SimBlockShapes.ROPE_CONNECTOR_COLLIDER);

    static {
        BlockMovementChecksImpl.registerAttachedCheck(
                (state, world, pos, direction) -> {
                    final BlockState relativeState = world.getBlockState(pos.relative(direction));
                    if (state.getBlock() instanceof RopeConnectorBlock && state.getValue(RopeConnectorBlock.FACING) == direction.getOpposite()) {
                        return BlockMovementChecks.CheckResult.SUCCESS;
                    }
                    if (relativeState.getBlock() instanceof RopeConnectorBlock && relativeState.getValue(RopeConnectorBlock.FACING) == direction) {
                        return BlockMovementChecks.CheckResult.SUCCESS;
                    }

                    return BlockMovementChecks.CheckResult.PASS;
                }
        );
    }

    public RopeConnectorBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(final BlockGetter blockGetter, final BlockState state) {
        return PHYSICS_COLLIDER.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
    }

    @Override
    public void onRemove(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pNewState, final boolean pIsMoving) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
    }

    @Override
    public Class<RopeConnectorBlockEntity> getBlockEntityClass() {
        return RopeConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RopeConnectorBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.ROPE_CONNECTOR.get();
    }

    @Override
    protected Direction getFacingForPlacement(final BlockPlaceContext context) {
        return context.getClickedFace();
    }

    @Override
    protected boolean getAxisAlignmentForPlacement(final BlockPlaceContext context) {
        return context.getHorizontalDirection()
                .getAxis() != Direction.Axis.X;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
        return SHAPE.get(state.getValue(FACING), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (!level.isClientSide() && stack.is(SimTags.Items.DESTROYS_ROPE)) {
            return RopeHolderBlock.shearRope(this, level, pos, (ServerPlayer) player);
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
