package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreBlocks;
import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ReactorControlRodBlock extends BaseEntityBlock {
    public ReactorControlRodBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .sound(SoundType.METAL)
                .strength(5.0F)
                .requiresCorrectToolForDrops());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorControlRodBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CoreBlockEntities.REACTOR_CONTROL_ROD.get(), ReactorControlRodBlockEntity::serverTick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        linkColumn(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        linkColumn(level, pos);
    }

    private static void linkColumn(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        ReactorColumnHelper.linkColumnSegments(level, pos, s -> s.is(CoreBlocks.REACTOR_CONTROL_ROD.get()),
                (segment, base) -> {
                    BlockEntity be = level.getBlockEntity(segment);
                    if (be instanceof ReactorControlRodBlockEntity rod) {
                        rod.setColumnBase(base);
                    }
                });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReactorControlRodBlockEntity clicked) {
                ReactorControlRodBlockEntity rod = clicked.getBaseEntity();
                rod.cycleInsertion();
                int newLevel = rod.getInsertionLevel();

                if (newLevel == 0) {
                    player.sendSystemMessage(Component.literal("§8[СУЗ] §aКолонна выведена (0%)§r"));
                } else if (newLevel == 100) {
                    player.sendSystemMessage(Component.literal("§8[СУЗ] §cКолонна вставлена полностью — АЗ (100%)§r"));
                } else {
                    player.sendSystemMessage(Component.literal("§8[СУЗ] §eКолонна: " + newLevel + "%§r"));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
