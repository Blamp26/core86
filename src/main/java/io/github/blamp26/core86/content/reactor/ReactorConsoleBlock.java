package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import io.github.blamp26.core86.foundation.network.CorePackets;
import io.github.blamp26.core86.foundation.network.packets.ReactorMapSyncPacket;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import io.github.blamp26.core86.foundation.registry.CoreItems;

public class ReactorConsoleBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ReactorConsoleBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.METAL)
                .strength(3.0F)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CoreBlockEntities.REACTOR_CONSOLE.get(), ReactorConsoleBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ReactorConsoleBlockEntity console) {
                if (console.tryAuthorizeWithKey(player, hand)) {
                    player.sendSystemMessage(Component.literal("§a[SIUR] Access authorized."));
                    return InteractionResult.SUCCESS;
                }
                if (player.getItemInHand(hand).is(CoreItems.SIUR_KEY_TOKEN.get()) && console.isSiurAuthorized()) {
                    player.sendSystemMessage(Component.literal("§e[SIUR] Access already authorized."));
                }
                console.scanStructure(level, pos);
                
                if (console.isAssembled()) {
                    if (console.getControlChannelCount() == 0) {
                        player.sendSystemMessage(Component.literal(
                                "Â§e[CORE86] Ð¢Ð¾Ð¿Ð»Ð¸Ð²Ð¾ Ð½Ð°Ð¹Ð´ÐµÐ½Ð¾, Ð½Ð¾ ÐºÐ°Ð½Ð°Ð»Ñ‹ Ð¡Ð£Ð— Ð½Ðµ Ð·Ð°Ñ€ÐµÐ³Ð¸ÑÑ‚Ñ€Ð¸Ñ€Ð¾Ð²Ð°Ð½Ñ‹. "
                                        + "ÐÑƒÐ¶Ð½Ð° ÐºÐ¾Ð»Ð¾Ð½Ð½Ð° Ð¡Ð£Ð— â‰¥4 Ð±Ð»Ð¾ÐºÐ¾Ð² Ð² Ð³Ñ€Ð°Ñ„Ð¸Ñ‚Ðµ. Ð¡Ð¼. Ð¾ÑˆÐ¸Ð±ÐºÐ¸ Ð½Ð¸Ð¶Ðµ."));
                    }
                    if (player.isShiftKeyDown()) {
                        console.triggerScram();
                        console.scanStructure(level, pos);
                        player.sendSystemMessage(Component.literal("Â§c[ÐÐ—] Â§fÐÐ²Ð°Ñ€Ð¸Ð¹Ð½Ð°Ñ Ð·Ð°Ñ‰Ð¸Ñ‚Ð° Ð°ÐºÑ‚Ð¸Ð²Ð¸Ñ€Ð¾Ð²Ð°Ð½Ð°. Ð’ÑÐµ ÑÑ‚ÐµÑ€Ð¶Ð½Ð¸ Ð¾Ð¿ÑƒÑ‰ÐµÐ½Ñ‹.Â§r"));
                    } else {
                        // Ð¢ÐµÐ¿ÐµÑ€ÑŒ Ð¾Ñ‚ÐºÑ€Ñ‹Ð²Ð°ÐµÐ¼ GUI Ð²Ð¼ÐµÑÑ‚Ð¾ Ð¿ÐµÑ€ÐµÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ð² Ñ‡Ð°Ñ‚Ðµ
                        // ÐžÑ‚Ð¿Ñ€Ð°Ð²Ð»ÑÐµÐ¼ ÐºÐ°Ñ€Ñ‚Ñƒ Ð¸Ð³Ñ€Ð¾ÐºÑƒ Ð¿ÐµÑ€ÐµÐ´ Ð¾Ñ‚ÐºÑ€Ñ‹Ñ‚Ð¸ÐµÐ¼ ÑÐºÑ€Ð°Ð½Ð°
                        CorePackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), 
                            new ReactorMapSyncPacket(pos, console.getFlatReactorMap(), console.getXenonLevel()));
                        
                        NetworkHooks.openScreen((ServerPlayer) player, console, pos);
                    }
                } else {
                    player.sendSystemMessage(Component.literal( 
                            "Â§c[CORE86] Ð¡Ñ‚Ñ€ÑƒÐºÑ‚ÑƒÑ€Ð° Ð½Ðµ ÑÐ¾Ð±Ñ€Ð°Ð½Ð°Â§r")); 
  
                    List<String> errors = console.getLastScanErrors(); 
                    if (errors.isEmpty()) { 
                        player.sendSystemMessage(Component.literal( 
                                "Â§7ÐŸÑ€Ð¸Ñ‡Ð¸Ð½Ð° Ð½ÐµÐ¸Ð·Ð²ÐµÑÑ‚Ð½Ð°. ÐŸÐ¾Ð¿Ñ€Ð¾Ð±ÑƒÐ¹Ñ‚Ðµ Ð¿ÐµÑ€ÐµÑÐºÐ°Ð½Ð¸Ñ€Ð¾Ð²Ð°Ñ‚ÑŒ.")); 
                    } else { 
                        // Show up to 5 errors to avoid chat spam 
                        int shown = Math.min(errors.size(), 5); 
                        for (int i = 0; i < shown; i++) { 
                            player.sendSystemMessage(Component.literal("Â§7â€¢ " + errors.get(i))); 
                        } 
                        if (errors.size() > 5) { 
                            player.sendSystemMessage(Component.literal( 
                                    "Â§7... Ð¸ ÐµÑ‰Ñ‘ " + (errors.size() - 5) + " Ð¾ÑˆÐ¸Ð±Ð¾Ðº")); 
                        } 
                    } 
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
