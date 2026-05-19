package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import io.github.blamp26.core86.foundation.registry.CoreBlocks;
import io.github.blamp26.core86.foundation.registry.CoreFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReactorSteamChannelBlockEntity extends BlockEntity {
    private BlockPos controllerPos;
    private BlockPos columnBase = BlockPos.ZERO;

    public ReactorSteamChannelBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.REACTOR_STEAM_CHANNEL.get(), pos, state);
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

    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos != null ? pos.immutable() : null;
        setChanged();
    }

    public void linkColumn(Level level) {
        if (level == null || level.isClientSide()) {
            return;
        }
        ReactorColumnHelper.linkColumnSegments(level, worldPosition,
                s -> s.is(CoreBlocks.REACTOR_STEAM_CHANNEL.get()),
                (segment, base) -> {
                    BlockEntity be = level.getBlockEntity(segment);
                    if (be instanceof ReactorSteamChannelBlockEntity steam) {
                        steam.setColumnBase(base);
                    }
                });
    }

    private ReactorConsoleBlockEntity getConsole() {
        if (level == null || controllerPos == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof ReactorConsoleBlockEntity console ? console : null;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return LazyOptional.of(() -> new SteamChannelFluidHandler(side)).cast();
        }
        return super.getCapability(cap, side);
    }

    /** DOWN = water in, UP = steam out, sides = both (Create pipes). */
    private class SteamChannelFluidHandler implements IFluidHandler {
        private final Direction side;

        SteamChannelFluidHandler(@Nullable Direction side) {
            this.side = side;
        }
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            ReactorConsoleBlockEntity console = getConsole();
            return console != null ? console.getTankFluid(tank) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return CoreReactorConstants.TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank == 0) {
                return stack.getFluid().isSame(Fluids.WATER);
            }
            return stack.getFluid().isSame(CoreFluids.STEAM_SOURCE.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!resource.getFluid().isSame(Fluids.WATER)) {
                return 0;
            }
            if (side != null && side == Direction.UP) {
                return 0;
            }
            if (side != null && side.getAxis().isVertical() && side != Direction.DOWN) {
                return 0;
            }
            ReactorConsoleBlockEntity console = getConsole();
            return console != null ? console.fillWater(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!resource.getFluid().isSame(CoreFluids.STEAM_SOURCE.get())) {
                return FluidStack.EMPTY;
            }
            if (side != null && side == Direction.DOWN) {
                return FluidStack.EMPTY;
            }
            ReactorConsoleBlockEntity console = getConsole();
            return console != null ? console.drainSteam(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (side != null && side == Direction.DOWN) {
                return FluidStack.EMPTY;
            }
            ReactorConsoleBlockEntity console = getConsole();
            return console != null ? console.drainSteam(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ControllerPos")) {
            controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
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
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
        if (!isColumnBase()) {
            tag.putLong("ColumnBase", getColumnBasePos().asLong());
        }
    }
}
