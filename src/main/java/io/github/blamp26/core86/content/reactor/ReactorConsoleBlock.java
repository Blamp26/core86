package io.github.blamp26.core86.content.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ReactorConsoleBlock extends BaseEntityBlock {
    public ReactorConsoleBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.METAL)
                .strength(3.0F)
                .requiresCorrectToolForDrops());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorConsoleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReactorConsoleBlockEntity console) {
                console.scanStructure(level, pos);
                
                if (console.isAssembled()) {
                    player.sendSystemMessage(Component.literal("§a[CORE86] Реактор собран успешно!§r"));
                    player.sendSystemMessage(Component.literal("§7- Активных каналов: §f" + console.getValidChannels()));
                    player.sendSystemMessage(Component.literal("§7- Топливных стержней: §f" + console.getActiveFuelRods()));
                    player.sendSystemMessage(Component.literal("§7- Стержней СУЗ: §f" + console.getControlRods()));
                    
                    float efficiency = console.getEfficiency() * 100;
                    String color = efficiency > 70 ? "§a" : (efficiency > 30 ? "§e" : "§c");
                    player.sendSystemMessage(Component.literal("§7- Эффективность: " + color + String.format("%.1f%%", efficiency) + "§r"));
                } else {
                    player.sendSystemMessage(Component.literal("§c[CORE86] Ошибка структуры!§r"));
                    player.sendSystemMessage(Component.literal("§7Убедитесь, что консоль примыкает к графиту, а каналы ТВС завершены паровыми заглушками."));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
