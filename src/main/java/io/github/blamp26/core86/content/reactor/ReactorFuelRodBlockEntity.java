package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import io.github.blamp26.core86.foundation.registry.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorFuelRodBlockEntity extends BlockEntity {
    public static final int MAX_FUEL = CoreReactorConstants.MAX_FUEL_PER_ROD;

    private ItemStack insertedRod = ItemStack.EMPTY;
    /** Bottom of column; equals worldPosition when this block is the base. */
    private BlockPos columnBase = BlockPos.ZERO;

    public ReactorFuelRodBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.REACTOR_FUEL_ROD.get(), pos, state);
    }

    public boolean isColumnBase() {
        return columnBase.equals(BlockPos.ZERO) || columnBase.equals(worldPosition);
    }

    public BlockPos getColumnBasePos() {
        return isColumnBase() ? worldPosition : columnBase;
    }

    public void setColumnBase(BlockPos base) {
        this.columnBase = base.equals(worldPosition) ? BlockPos.ZERO : base.immutable();
        setChanged();
    }

    public ReactorFuelRodBlockEntity getBaseEntity() {
        if (level == null || isColumnBase()) {
            return this;
        }
        BlockEntity be = level.getBlockEntity(getColumnBasePos());
        if (be instanceof ReactorFuelRodBlockEntity base) {
            return base;
        }
        return this;
    }

    public int getFuelAmount() {
        return getBaseEntity().readFuelAmount();
    }

    private int readFuelAmount() {
        return !insertedRod.isEmpty() ? RbmkFuelRodItem.getFuel(insertedRod) : 0;
    }

    public int getMaxFuel() {
        return MAX_FUEL;
    }

    public boolean hasFuel() {
        return getBaseEntity().readFuelAmount() > 0;
    }

    public int getFuelPercent() {
        return (int) ((float) getFuelAmount() / MAX_FUEL * 100);
    }

    public int burnFuel(int amount) {
        if (!isColumnBase()) {
            return getBaseEntity().burnFuel(amount);
        }
        if (insertedRod.isEmpty()) {
            return 0;
        }

        int current = RbmkFuelRodItem.getFuel(insertedRod);
        int burned = Math.min(current, amount);

        if (burned > 0) {
            int newFuel = current - burned;
            RbmkFuelRodItem.setFuel(insertedRod, newFuel);

            if (newFuel <= 0) {
                insertedRod = new ItemStack(CoreItems.SPENT_FUEL_ROD.get());
            }
            syncColumn();
        }
        return burned;
    }

    public boolean insertRod(ItemStack stack) {
        ReactorFuelRodBlockEntity base = getBaseEntity();
        if (!base.isColumnBase()) {
            return false;
        }
        if (base.insertedRod.isEmpty() && stack.getItem() instanceof RbmkFuelRodItem) {
            base.insertedRod = stack.split(1);
            base.syncColumn();
            return true;
        }
        return false;
    }

    public ItemStack extractRod() {
        ReactorFuelRodBlockEntity base = getBaseEntity();
        if (!base.isColumnBase()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = base.insertedRod;
        base.insertedRod = ItemStack.EMPTY;
        base.syncColumn();
        return stack;
    }

    public boolean isRodPresent() {
        return !getBaseEntity().insertedRod.isEmpty();
    }

    private void syncColumn() {
        update();
        if (level == null) {
            return;
        }
        ReactorColumnHelper.linkColumnSegments(level, worldPosition,
                state -> state.is(getBlockState().getBlock()),
                (segment, base) -> {
                    BlockEntity be = level.getBlockEntity(segment);
                    if (be instanceof ReactorFuelRodBlockEntity rod) {
                        rod.setColumnBase(base);
                    }
                });
    }

    private void update() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("InsertedRod")) {
            insertedRod = ItemStack.of(tag.getCompound("InsertedRod"));
        } else {
            insertedRod = ItemStack.EMPTY;
        }
        if (tag.contains("ColumnBase")) {
            columnBase = BlockPos.of(tag.getLong("ColumnBase"));
        } else {
            columnBase = BlockPos.ZERO;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!insertedRod.isEmpty()) {
            tag.put("InsertedRod", insertedRod.save(new CompoundTag()));
        }
        if (!isColumnBase()) {
            tag.putLong("ColumnBase", getColumnBasePos().asLong());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
