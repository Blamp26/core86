package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ReactorConsoleMenu extends AbstractContainerMenu {
    private final ReactorConsoleBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ReactorConsoleMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(17));
    }

    protected ReactorConsoleMenu(MenuType<? extends ReactorConsoleMenu> type, int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(type, containerId);
        this.blockEntity = (ReactorConsoleBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;
    }

    public ReactorConsoleMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        this(CoreMenus.REACTOR_CONSOLE.get(), containerId, inv, entity, data);

        if (!level.isClientSide && blockEntity != null) {
            blockEntity.scanStructure(level, blockEntity.getBlockPos());
        }

        addDataSlots(data);
    }

    public ContainerData getData() {
        return data;
    }

    public ReactorConsoleBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Упрощаем для интерфейса управления без предметов
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
