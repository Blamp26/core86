package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreBlocks;
import io.github.blamp26.core86.foundation.registry.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ReactorFuelRodBlock extends BaseEntityBlock {
    public ReactorFuelRodBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
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
        return new ReactorFuelRodBlockEntity(pos, state);
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
        ReactorColumnHelper.linkColumnSegments(level, pos, s -> s.is(CoreBlocks.REACTOR_FUEL_ROD.get()),
                (segment, base) -> {
                    BlockEntity be = level.getBlockEntity(segment);
                    if (be instanceof ReactorFuelRodBlockEntity rod) {
                        rod.setColumnBase(base);
                    }
                });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ReactorFuelRodBlockEntity clicked)) {
                return InteractionResult.PASS;
            }
            ReactorFuelRodBlockEntity rod = clicked.getBaseEntity();
            ItemStack held = player.getItemInHand(hand);

            if (held.getItem() instanceof RbmkFuelRodItem && !rod.isRodPresent()) {
                if (rod.insertRod(held)) {
                    player.sendSystemMessage(Component.literal("§a[ТВС] Стержень вставлен в колонну"));
                    return InteractionResult.SUCCESS;
                }
            } else if (rod.isRodPresent() && (held.isEmpty() || player.isShiftKeyDown())) {
                if (player.isShiftKeyDown()) {
                    int fuel = rod.getFuelAmount();
                    int max = rod.getMaxFuel();
                    int pct = max > 0 ? (int) ((float) fuel / max * 100) : 0;
                    String color = pct > 50 ? "§a" : pct > 20 ? "§e" : "§c";
                    player.sendSystemMessage(Component.literal(
                            "§8[ТВС] §7Топливо: " + color + fuel + " / " + max + " (" + pct + "%)§r"));
                    return InteractionResult.SUCCESS;
                }

                ItemStack extracted = rod.extractRod();
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                    player.sendSystemMessage(Component.literal("§7[ТВС] Стержень извлечён"));
                    return InteractionResult.SUCCESS;
                }
            } else if (held.isEmpty() && !rod.isRodPresent()) {
                player.sendSystemMessage(Component.literal("§c[ТВС] Стержень не вставлен"));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
