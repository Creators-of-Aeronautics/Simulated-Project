package dev.simulated_team.simulated.content.blocks.spring;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpringBlock extends WrenchableDirectionalBlock implements IBE<SpringBlockEntity>, BlockSubLevelAssemblyListener, IWrenchable {
    public static final EnumProperty<Size> SIZE = EnumProperty.create("size", Size.class);

    public SpringBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(SIZE, Size.MEDIUM));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(SIZE));
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader levelReader, final BlockPos blockPos, final BlockState blockState) {
        return SimItems.SPRING.asStack();
    }

    @Override
    public void onRemove(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pNewState, final boolean pMovedByPiston) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
    }

    @Override
    public boolean canSurvive(final BlockState pState, final LevelReader pLevel, final BlockPos pPos) {
        return canAttach(pLevel, pPos, pState.getValue(FACING).getOpposite());
    }

    @Override
    public BlockState updateShape(final BlockState pState, final Direction pFacing, final BlockState pFacingState, final LevelAccessor pLevel, final BlockPos pCurrentPos, final BlockPos pFacingPos) {
        return pState.getValue(FACING).getOpposite() == pFacing && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    public static boolean canAttach(final LevelReader pReader, final BlockPos pPos, final Direction pDirection) {
        final BlockPos blockpos = pPos.relative(pDirection);
        return pReader.getBlockState(blockpos).isFaceSturdy(pReader, blockpos, pDirection.getOpposite());
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext pContext) {
        return switch (pState.getValue(SpringBlock.SIZE)) {
            case SMALL -> SimBlockShapes.SMALL_SPRING.get(pState.getValue(FACING));
            case MEDIUM -> SimBlockShapes.SPRING.get(pState.getValue(FACING));
            case LARGE -> SimBlockShapes.LARGE_SPRING.get(pState.getValue(FACING));
        };
    }

    public static boolean tryAdjustSpring(final Level level, final BlockPos pos, final Player player) {
        if (level.getBlockEntity(pos) instanceof final SpringBlockEntity spring) {
            final String error = spring.tryChangeLengthOrError(level, player.isShiftKeyDown() ? -0.25 : 0.25);
            if (error == null) {
                sendLengthMessage("new_length", SimColors.SUCCESS_LIME, spring, player);
                return true;
            }
            sendLengthMessage(error, SimColors.NUH_UH_RED, spring, player);
        }
        return false;
    }

    private static void sendLengthMessage(final String suffix, final int color, final SpringBlockEntity spring, Player player) {
        SimLang.translate("spring." + suffix, String.format("%.2f", spring.desiredLength))
                .color(color)
                .sendStatus(player);
    }

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();

        final SpringBlockEntity be = this.getBlockEntity(level, pos);

        if (be == null) {
            return InteractionResult.SUCCESS;
        }

        final SpringBlockEntity partner = be.getPairedSpring();
        if (partner == null) {
            return InteractionResult.SUCCESS;
        }

        final BlockState partnerState = partner.getBlockState();
        final BlockPos partnerPos = partner.getBlockPos();
        final Size size = state.getValue(SIZE);
        final Size newSize = size.cycle();

        final BlockState newState = state.setValue(SIZE, newSize);
        final BlockState newPartnerState = partnerState.setValue(SIZE, newSize);

        level.setBlockAndUpdate(pos, newState);
        level.setBlockAndUpdate(partnerPos, newPartnerState);
        AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, Create.RANDOM.nextFloat() + .5f);

        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<SpringBlockEntity> getBlockEntityClass() {
        return SpringBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SpringBlockEntity> getBlockEntityType() {
        return SimBlockEntityTypes.SPRING.get();
    }

    @Override
    protected void spawnAfterBreak(final BlockState blockState, final ServerLevel serverLevel, final BlockPos blockPos, final ItemStack itemStack, final boolean bl) {
        super.spawnAfterBreak(blockState, serverLevel, blockPos, itemStack, bl);
    }

    @Override
    public void beforeMove(final ServerLevel originLevel, final ServerLevel newLevel, final BlockState newState, final BlockPos oldPos, final BlockPos newPos) {
        if (newLevel.getBlockEntity(oldPos) instanceof final SpringBlockEntity spring) {
            spring.assembling = true;
        }
    }

    @Override
    public void afterMove(final ServerLevel oldLevel, final ServerLevel newLevel, final BlockState state, final BlockPos oldPos, final BlockPos newPos) {
        if (newLevel.getBlockEntity(newPos) instanceof final SpringBlockEntity spring) {
            final SpringBlockEntity partner = spring.getPairedSpring();

            if (partner != null) {
                final SubLevel subLevel = Sable.HELPER.getContaining(newLevel, newPos);
                partner.setPartnerPos(newPos, subLevel != null ? subLevel.getUniqueId() : null);
            }
        }
    }

    public enum Size implements StringRepresentable {
        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large");

        private static final Size[] VALUES = values();
        private final String name;

        Size(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Size cycle() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }

}
