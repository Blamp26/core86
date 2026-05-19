package io.github.blamp26.core86.content.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utilities for vertical reactor columns (fuel, control, steam).
 * One logical channel = one stack of blocks; data lives on the base (bottom) block.
 */
public final class ReactorColumnHelper {
    private ReactorColumnHelper() {}

    public record ColumnInfo(BlockPos base, int height, List<BlockPos> segments) {}

    public static BlockPos findColumnBase(Level level, BlockPos pos, Predicate<BlockState> isSameBlock) {
        BlockPos current = pos;
        while (isSameBlock.test(level.getBlockState(current.below()))) {
            current = current.below();
        }
        return current;
    }

    public static ColumnInfo scanColumn(Level level, BlockPos anyPos, Predicate<BlockState> isSameBlock) {
        BlockPos base = findColumnBase(level, anyPos, isSameBlock);
        List<BlockPos> segments = new ArrayList<>();
        BlockPos current = base;
        while (isSameBlock.test(level.getBlockState(current))) {
            segments.add(current.immutable());
            current = current.above();
        }
        return new ColumnInfo(base, segments.size(), segments);
    }

    public static void linkColumnSegments(Level level, BlockPos anyPos, Predicate<BlockState> isSameBlock,
                                          ColumnLinker linker) {
        ColumnInfo info = scanColumn(level, anyPos, isSameBlock);
        for (BlockPos segment : info.segments()) {
            linker.link(segment, info.base());
        }
    }

    @FunctionalInterface
    public interface ColumnLinker {
        void link(BlockPos segmentPos, BlockPos basePos);
    }

    public static long columnKey(BlockPos base) {
        return BlockPos.asLong(base.getX(), 0, base.getZ());
    }
}
