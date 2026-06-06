package dev.eriksonn.aeronautics.content.blocks.hot_air.envelope;


import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EnvelopeEncasedPipeBlock extends EncasedPipeBlock implements Envelope, SpecialBlockItemRequirement {

    protected final DyeColor color;

    protected EnvelopeEncasedPipeBlock(final Properties properties, final DyeColor color) {
        super(properties, () -> AeroBlocks.ENVELOPE_ENCASED_SHAFTS.get(color).get());
        this.color = color;
    }

    public static EnvelopeEncasedPipeBlock withCanvas(final Properties properties, final DyeColor color) {
        return new EnvelopeEncasedPipeBlock(properties, color);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack itemStack, final BlockState blockState, final Level level, final BlockPos blockPos, final Player player, final InteractionHand interactionHand, final BlockHitResult blockHitResult) {
        return EnvelopeBlockHelper.useItemOnHelper(itemStack, blockState, level, blockPos);
    }

    @Override
    public InteractionResult onSneakWrenched(final BlockState state, final UseOnContext context) {
        super.onSneakWrenched(state, context);
        return EnvelopeBlockHelper.onSneakWrenchedHelper(context);
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return AeroBlockEntityTypes.ENVELOPE_ENCASED_FLUID_PIPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return AeroBlockEntityTypes.ENVELOPE_ENCASED_FLUID_PIPE.create(pos, state);
    }

    @Override
    public void fallOn(final Level pLevel, final BlockState pState, final BlockPos pPos, final Entity pEntity, final float pFallDistance) {
        if (pEntity.isSuppressingBounce()) {
            super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance);
        } else {
            pEntity.causeFallDamage(pFallDistance, 0.5F, pLevel.damageSources().fall());
        }
    }

    @Override
    public void updateEntityAfterFallOn(final BlockGetter pLevel, final Entity pEntity) {
        if (pEntity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(pLevel, pEntity);
        } else {
            EnvelopeBlockHelper.encasedBounceUp(pEntity);
        }
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level, final BlockPos pos, final Player player) {
        return this.getCasing().asItem().getDefaultInstance();
    }

    @Override
    public Block getCasing() {
        return AeroBlocks.DYED_ENVELOPE_BLOCKS.get(this.color).get();
    }

    @Override
    public void handleEncasing(final BlockState state, final Level level, final BlockPos pos, final ItemStack heldItem, final Player player, final InteractionHand hand, final BlockHitResult ray) {
        super.handleEncasing(state, level, pos, heldItem, player, hand, ray);
        if (!player.hasInfiniteMaterials()) {
            player.getItemInHand(hand).shrink(1);
        }
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        ItemStack stack = AeroBlocks.WHITE_ENVELOPE_BLOCK.asStack();
        return super.getRequiredItems(state, be).union(new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, stack));
    }
}