package dev.ryanhcode.offroad.handlers.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;

import java.util.Map;

public class MultiMiningBlockDestructionProgress extends BlockDestructionProgress {
    public final Map<BlockPos, BlockDestructionProgress> otherProgresses = new Object2ObjectOpenHashMap<>();

    public MultiMiningBlockDestructionProgress(final int id, final BlockPos pos) {
        super(id, pos);
    }
}

