package io.github.blamp26.core86.content.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.blamp26.core86.content.reactor.CoreReactorConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.List;

/**
 * Паровая турбина — по образцу {@code DieselEngineBlockEntity} (Create Diesel Generators).
 */
public class SteamTurbineBlockEntity extends GeneratingKineticBlockEntity implements ISteamEngine {

    private float remainingTicks = 0f;
    private float lastCapacity = 0f;
    private float lastSpeed = 0f;

    SmartFluidTankBehaviour tank;

    public SteamTurbineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, CoreReactorConstants.TURBINE_TANK_CAPACITY);
        behaviours.add(tank);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap != ForgeCapabilities.FLUID_HANDLER) {
            return super.getCapability(cap, side);
        }
        // Пар снизу и с боков (как дизель в CDG — не с торца вала)
        if (side == null || side != Direction.UP) {
            return tank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public FluidTank getEngineTank() {
        return tank.getPrimaryHandler();
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = getFuelCapacity() * (1f / getFuelSpeed()) * getFuelSpeed();
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!enabled()) {
            return 0;
        }
        return convertToDirection(getFuelSpeed(), getBlockState().getValue(SteamTurbineBlock.FACING));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability().cast());

        FluidStack fluid = tank.getPrimaryHandler().getFluid();
        tooltip.add(Component.literal("Enabled: " + enabled()));
        tooltip.add(Component.literal("Target RPM: " + Math.round(getGeneratedSpeed())));
        tooltip.add(Component.literal("Actual RPM: " + Math.round(getSpeed())));
        tooltip.add(Component.literal("Tank: " + fluid.getAmount() + " / " + CoreReactorConstants.TURBINE_TANK_CAPACITY + " mB"));
        tooltip.add(Component.literal("Fluid: " + fluid.getDisplayName().getString()));
        tooltip.add(Component.literal("Burn rate: " + getFuelBurnRate() + " mB/t"));
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        float fuelCapacity = getFuelCapacity() * (1f / getFuelSpeed()) * getFuelSpeed();
        if (!level.isClientSide && (lastSpeed != getGeneratedSpeed() || lastCapacity != fuelCapacity)) {
            reActivateSource = true;
            lastSpeed = getGeneratedSpeed();
            lastCapacity = fuelCapacity;
        }

        if (enabled()) {
            remainingTicks += getFuelBurnRate();
            int drainAmount = (int) remainingTicks;
            if (drainAmount > 0) {
                int drained = tank.getPrimaryHandler()
                        .drain(drainAmount, IFluidHandler.FluidAction.EXECUTE)
                        .getAmount();
                remainingTicks -= drained;
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("RemainingTicks", remainingTicks);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        remainingTicks = tag.getFloat("RemainingTicks");
    }
}
