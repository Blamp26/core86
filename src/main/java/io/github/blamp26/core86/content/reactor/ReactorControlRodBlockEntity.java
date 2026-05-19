package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.config.CommonConfig;
import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorControlRodBlockEntity extends BlockEntity {
    public enum DriveMode { NORMAL, SCRAM }

    private int currentInsertionLevel = 0;
    private int targetInsertionLevel = 0;
    private DriveMode driveMode = DriveMode.NORMAL;
    private float moveRemainder = 0f;
    private BlockPos columnBase = BlockPos.ZERO;

    public ReactorControlRodBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.REACTOR_CONTROL_ROD.get(), pos, state);
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

    public ReactorControlRodBlockEntity getBaseEntity() {
        if (level == null || isColumnBase()) {
            return this;
        }
        BlockEntity be = level.getBlockEntity(getColumnBasePos());
        if (be instanceof ReactorControlRodBlockEntity base) {
            return base;
        }
        return this;
    }

    public int getInsertionLevel() {
        return getBaseEntity().currentInsertionLevel;
    }

    public int getTargetInsertionLevel() {
        return getBaseEntity().targetInsertionLevel;
    }

    public boolean isMoving() {
        ReactorControlRodBlockEntity base = getBaseEntity();
        return base.currentInsertionLevel != base.targetInsertionLevel;
    }

    public void cycleInsertion() {
        ReactorControlRodBlockEntity base = getBaseEntity();
        int next = base.targetInsertionLevel + 25;
        if (next > 100) {
            next = 0;
        }
        base.setInsertionLevel(next);
    }

    public void setInsertionLevel(int value) {
        setInsertionLevel(value, DriveMode.NORMAL);
    }

    public void setInsertionLevel(int value, DriveMode mode) {
        ReactorControlRodBlockEntity base = getBaseEntity();
        base.targetInsertionLevel = Math.max(0, Math.min(100, value));
        base.driveMode = mode;
        base.syncColumn();
    }

    public void engageScram() {
        setInsertionLevel(100, DriveMode.SCRAM);
    }

    private void tickServer() {
        if (!isColumnBase()) {
            return;
        }
        int from = currentInsertionLevel;
        int to = targetInsertionLevel;
        if (from == to) {
            return;
        }
        int sec = driveMode == DriveMode.SCRAM ? CommonConfig.rodMoveTimeScramSec() : CommonConfig.rodMoveTimeNormalSec();
        float stepPerTick = 100f / Math.max(1, sec * 20f);
        moveRemainder += stepPerTick;
        int step = (int) moveRemainder;
        if (step <= 0) {
            return;
        }
        moveRemainder -= step;
        if (from < to) {
            currentInsertionLevel = Math.min(to, from + step);
        } else {
            currentInsertionLevel = Math.max(to, from - step);
        }
        if (currentInsertionLevel == targetInsertionLevel) {
            moveRemainder = 0f;
            if (driveMode == DriveMode.SCRAM) {
                driveMode = DriveMode.NORMAL;
            }
        }
        syncColumn();
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
                    if (be instanceof ReactorControlRodBlockEntity rod) {
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
        currentInsertionLevel = tag.contains("CurrentInsertionLevel")
                ? tag.getInt("CurrentInsertionLevel")
                : tag.getInt("InsertionLevel");
        targetInsertionLevel = tag.contains("TargetInsertionLevel")
                ? tag.getInt("TargetInsertionLevel")
                : currentInsertionLevel;
        moveRemainder = tag.getFloat("MoveRemainder");
        if (tag.contains("DriveMode")) {
            try {
                driveMode = DriveMode.valueOf(tag.getString("DriveMode"));
            } catch (IllegalArgumentException ignored) {
                driveMode = DriveMode.NORMAL;
            }
        } else {
            driveMode = DriveMode.NORMAL;
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
        if (isColumnBase()) {
            tag.putInt("InsertionLevel", currentInsertionLevel);
            tag.putInt("CurrentInsertionLevel", currentInsertionLevel);
            tag.putInt("TargetInsertionLevel", targetInsertionLevel);
            tag.putString("DriveMode", driveMode.name());
            tag.putFloat("MoveRemainder", moveRemainder);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorControlRodBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.tickServer();
    }
}
