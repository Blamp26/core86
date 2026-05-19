package io.github.blamp26.core86.content.turbine;

import io.github.blamp26.core86.content.reactor.CoreReactorConstants;
import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
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
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SteamCondenserBlockEntity extends BlockEntity {

    private final FluidTank steamTank = new FluidTank(CoreReactorConstants.CONDENSER_TANK_CAPACITY,
            stack -> stack.getFluid().isSame(CoreFluids.STEAM_SOURCE.get()));
    private final FluidTank waterTank = new FluidTank(CoreReactorConstants.CONDENSER_TANK_CAPACITY,
            stack -> stack.getFluid().isSame(Fluids.WATER));

    private final LazyOptional<IFluidHandler> steamInput = LazyOptional.of(() -> new SteamInputHandler());
    private final LazyOptional<IFluidHandler> waterOutput = LazyOptional.of(() -> new WaterOutputHandler());

    public SteamCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.STEAM_CONDENSER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamCondenserBlockEntity be) {
        if (level.isClientSide) return;

        int steamAvail = be.steamTank.getFluidAmount();
        if (steamAvail <= 0) return;

        int waterSpace = be.waterTank.getCapacity() - be.waterTank.getFluidAmount();
        if (waterSpace <= 0) return;

        int toCondense = Math.min(steamAvail, CoreReactorConstants.CONDENSER_MAX_STEAM_TICK);
        int waterProduced = (int) (toCondense * CoreReactorConstants.CONDENSER_EFFICIENCY);
        waterProduced = Math.min(waterProduced, waterSpace);
        if (waterProduced <= 0) return;

        int steamUsed = (int) Math.ceil(waterProduced / CoreReactorConstants.CONDENSER_EFFICIENCY);
        steamUsed = Math.min(steamUsed, steamAvail);

        be.steamTank.drain(steamUsed, IFluidHandler.FluidAction.EXECUTE);
        be.waterTank.fill(new FluidStack(Fluids.WATER, waterProduced), IFluidHandler.FluidAction.EXECUTE);
        be.setChanged();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        steamInput.invalidate();
        waterOutput.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.UP) return steamInput.cast();
            // DOWN + все горизонтальные стороны → выход воды
            return waterOutput.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
    }

  private final class SteamInputHandler implements IFluidHandler {
        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) { return steamTank.getFluid(); }

        @Override
        public int getTankCapacity(int tank) { return steamTank.getCapacity(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid().isSame(CoreFluids.STEAM_SOURCE.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return steamTank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    private final class WaterOutputHandler implements IFluidHandler {
        @Override
        public int getTanks() { return 1; }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluid(); }

        @Override
        public int getTankCapacity(int tank) { return waterTank.getCapacity(); }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid().isSame(Fluids.WATER)) {
                return waterTank.drain(resource, action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return waterTank.drain(maxDrain, action);
        }
    }
}
