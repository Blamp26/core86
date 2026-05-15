package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import io.github.blamp26.core86.foundation.registry.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class ReactorConsoleBlockEntity extends BlockEntity {
    private boolean isAssembled = false;
    private int activeFuelRods = 0;
    private int validChannels = 0;
    private int controlRods = 0;

    public ReactorConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.REACTOR_CONSOLE.get(), pos, state);
    }

    public boolean isAssembled() {
        return isAssembled;
    }

    public int getActiveFuelRods() {
        return activeFuelRods;
    }

    public int getValidChannels() {
        return validChannels;
    }

    public int getControlRods() {
        return controlRods;
    }

    public float getEfficiency() {
        if (activeFuelRods == 0) return 0;
        // Простая формула: каждый стержень СУЗ снижает мощность на 10%, но не ниже 0
        float reduction = controlRods * 0.1f;
        return Math.max(0, 1.0f - reduction);
    }

    public void scanStructure(Level level, BlockPos pos) {
        this.activeFuelRods = 0;
        this.validChannels = 0;
        this.controlRods = 0;
        this.isAssembled = false;

        // 1. Ищем графит вплотную к консоли (с любой стороны)
        BlockPos coreStart = null;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).is(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get())) {
                coreStart = neighbor;
                break;
            }
        }

        if (coreStart == null) {
            setChanged();
            return;
        }

        // 2. Нашли графит. Теперь находим весь связанный массив графита (Flood Fill упрощенный)
        // Для начала просто пройдемся по области 15x15x15 вокруг точки старта
        Set<BlockPos> graphiteBlocks = new HashSet<>();
        for (int x = -7; x <= 7; x++) {
            for (int y = -7; y <= 7; y++) {
                for (int z = -7; z <= 7; z++) {
                    BlockPos p = coreStart.offset(x, y, z);
                    if (level.getBlockState(p).is(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get())) {
                        graphiteBlocks.add(p);
                    }
                }
            }
        }

        if (graphiteBlocks.isEmpty()) return;

        // 3. Сканируем каналы. Канал должен быть ГОРДИНЗОНТАЛЬНО смежен с графитом.
        Set<BlockPos> checkedChannels = new HashSet<>();
        Set<BlockPos> checkedControlRods = new HashSet<>();
        
        for (BlockPos gPos : graphiteBlocks) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isHorizontal()) {
                    BlockPos sidePos = gPos.relative(dir);
                    // Если нашли топливный стержень сбоку от графита
                    if (level.getBlockState(sidePos).is(CoreBlocks.REACTOR_FUEL_ROD.get()) && !checkedChannels.contains(sidePos)) {
                        validateChannel(level, sidePos, checkedChannels);
                    }
                    // Если нашли управляющий стержень сбоку от графита
                    if (level.getBlockState(sidePos).is(CoreBlocks.REACTOR_CONTROL_ROD.get()) && !checkedControlRods.contains(sidePos)) {
                        validateControlRod(level, sidePos, checkedControlRods);
                    }
                }
            }
        }

        this.isAssembled = this.validChannels > 0;
        setChanged();
    }

    private void validateControlRod(Level level, BlockPos startPos, Set<BlockPos> checkedControlRods) {
        // Управляющий стержень тоже должен быть в канале (хотя бы на графите)
        BlockPos current = startPos;
        while (level.getBlockState(current).is(CoreBlocks.REACTOR_CONTROL_ROD.get())) {
            current = current.below();
        }
        
        if (!level.getBlockState(current).is(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get())) return;

        current = startPos;
        while (level.getBlockState(current).is(CoreBlocks.REACTOR_CONTROL_ROD.get())) {
            checkedControlRods.add(current);
            this.controlRods++; // Считаем каждый блок стержня как единицу поглощения
            current = current.above();
        }
    }

    private void validateChannel(Level level, BlockPos startPos, Set<BlockPos> checkedChannels) {
        // Идем вниз до основания
        BlockPos current = startPos;
        while (level.getBlockState(current).is(CoreBlocks.REACTOR_FUEL_ROD.get())) {
            current = current.below();
        }
        
        // Основание должно быть графитом
        if (!level.getBlockState(current).is(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get())) return;

        // Теперь идем вверх и считаем стержни
        current = current.above();
        int rodsInChannel = 0;
        while (level.getBlockState(current).is(CoreBlocks.REACTOR_FUEL_ROD.get())) {
            checkedChannels.add(current);
            rodsInChannel++;
            current = current.above();
        }

        // Канал должен заканчиваться Steam Channel
        if (rodsInChannel > 0 && level.getBlockState(current).is(CoreBlocks.REACTOR_STEAM_CHANNEL.get())) {
            this.validChannels++;
            this.activeFuelRods += rodsInChannel;
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isAssembled = tag.getBoolean("IsAssembled");
        activeFuelRods = tag.getInt("ActiveFuelRods");
        validChannels = tag.getInt("ValidChannels");
        controlRods = tag.getInt("ControlRods");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsAssembled", isAssembled);
        tag.putInt("ActiveFuelRods", activeFuelRods);
        tag.putInt("ValidChannels", validChannels);
        tag.putInt("ControlRods", controlRods);
    }
}
