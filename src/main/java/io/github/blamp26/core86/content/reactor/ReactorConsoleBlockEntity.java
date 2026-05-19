package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.config.CommonConfig;
import io.github.blamp26.core86.foundation.network.CorePackets;
import io.github.blamp26.core86.foundation.network.packets.ReactorMapSyncPacket;
import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import io.github.blamp26.core86.foundation.registry.CoreBlocks;
import io.github.blamp26.core86.foundation.registry.CoreFluids;
import io.github.blamp26.core86.foundation.registry.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReactorConsoleBlockEntity extends BlockEntity implements MenuProvider {
    public static final float BASE_FUEL_BURN_RATE = 1.0f;
    public static final float BASE_POWER_PER_ROD = 1.0f;

    private boolean isAssembled = false;
    private int activeFuelChannels = 0;
    private int validChannels = 0;
    private int controlChannels = 0;
    private int steamChannels = 0;

    private int totalFuel = 0;
    private int totalInsertionSum = 0;
    private final List<BlockPos> controlChannelBases = new ArrayList<>();
    private final List<BlockPos> fuelChannelBases = new ArrayList<>();
    private final List<BlockPos> steamChannelBases = new ArrayList<>();
    private final List<String> lastScanErrors = new ArrayList<>();
    private float reflectorEfficiencyBonus = 0f;

    private byte[][] reactorMap = new byte[CoreReactorConstants.MAP_SIZE][CoreReactorConstants.MAP_SIZE];
    private final Map<BlockPos, Integer> rodGroupColors = new HashMap<>();

    private ReactorState reactorState = ReactorState.OFFLINE;
    private float powerOutput = 0f;
    private int tickCount = 0;
    private boolean meltdownTriggered = false;
    private int targetInsertion = 100;
    private float temperature = 20.0f;
    private int spikeTicksRemaining = 0;
    private float spikeMultiplier = 1.0f;
    private float xenonLevel = 0f;
    private float steamRemainder = 0f;
    private boolean experimentMode = false;
    private boolean siurAuthorized = false;
    private int coolingDelayTicks = 0;
    private int meltdownStage = 0;
    private boolean nuclearCountdownRecoveryChecked = false;
    private int az5TransientCooldownTicks = 0;
    private boolean scramInProgress = false;
    private int scramLockoutTicks = 0;
    private boolean scramSpikeApplied = false;
    private final FluidTank waterTank = new FluidTank(CoreReactorConstants.TANK_CAPACITY,
            fluid -> fluid.getFluid().isSame(Fluids.WATER));
    private final FluidTank steamTank = new FluidTank(CoreReactorConstants.TANK_CAPACITY,
            fluid -> fluid.getFluid().isSame(CoreFluids.STEAM_SOURCE.get()));
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidHandlers = Arrays.stream(Direction.values())
            .collect(Collectors.toMap(dir -> dir, dir -> LazyOptional.of(() -> new InternalFluidHandler(dir))));
    private final LazyOptional<IFluidHandler> defaultHandler = LazyOptional.of(() -> new InternalFluidHandler(null));

    protected final ContainerData data;

    public ReactorConsoleBlockEntity(BlockPos pos, BlockState state) {
        this(CoreBlockEntities.REACTOR_CONSOLE.get(), pos, state);
    }

    protected ReactorConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> targetInsertion;
                    case 1 -> (int) temperature;
                    case 2 -> (int) (powerOutput * 100);
                    case 3 -> totalFuel;
                    case 4 -> reactorState.ordinal();
                    case 5 -> validChannels;
                    case 6 -> getAverageInsertion();
                    case 7 -> controlChannels;
                    case 8 -> waterTank.getFluidAmount();
                    case 9 -> steamTank.getFluidAmount();
                    case 10 -> Math.round(xenonLevel * 100);
                    case 11 -> experimentMode ? 1 : 0;
                    case 12 -> Math.round((1.0f - (waterTank.getCapacity() > 0 ? (float) waterTank.getFluidAmount() / waterTank.getCapacity() : 0f)) * 100f);
                    case 13 -> coolingDelayTicks;
                    case 14 -> siurAuthorized ? 1 : 0;
                    case 15 -> scramInProgress ? 1 : 0;
                    case 16 -> isAnyControlRodMoving() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> targetInsertion = value;
                    case 1 -> temperature = value;
                    case 2 -> powerOutput = value / 100f;
                    case 3 -> totalFuel = value;
                    case 4 -> reactorState = ReactorState.values()[value];
                    case 5 -> validChannels = value;
                    case 8 -> waterTank.setFluid(new FluidStack(Fluids.WATER, value));
                    case 9 -> steamTank.setFluid(new FluidStack(CoreFluids.STEAM_SOURCE.get(), value));
                    case 10 -> xenonLevel = value / 100f;
                    case 11 -> experimentMode = value > 0;
                    case 13 -> coolingDelayTicks = Math.max(0, value);
                    case 14 -> siurAuthorized = value > 0;
                    case 15 -> scramInProgress = value > 0;
                }
            }

            @Override
            public int getCount() {
                return 17;
            }
        };
    }

    // --- Fluid API for steam columns and console ---

    public FluidStack getTankFluid(int tank) {
        return tank == 0 ? waterTank.getFluid() : steamTank.getFluid();
    }

    public int fillWater(FluidStack resource, IFluidHandler.FluidAction action) {
        int filled = waterTank.fill(resource, action);
        if (filled > 0 && action.execute()) {
            setChanged();
        }
        return filled;
    }

    public FluidStack drainSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        FluidStack drained = steamTank.drain(resource, action);
        if (!drained.isEmpty() && action.execute()) {
            setChanged();
        }
        return drained;
    }

    public FluidStack drainSteam(int maxDrain, IFluidHandler.FluidAction action) {
        FluidStack drained = steamTank.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute()) {
            setChanged();
        }
        return drained;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null) {
                return defaultHandler.cast();
            }
            return fluidHandlers.get(side).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandlers.values().forEach(LazyOptional::invalidate);
        defaultHandler.invalidate();
    }

    private class InternalFluidHandler implements IFluidHandler {
        private final Direction side;

        InternalFluidHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluid() : steamTank.getFluid();
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
            if (side != Direction.DOWN && resource.getFluid().isSame(Fluids.WATER)) {
                return fillWater(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid().isSame(CoreFluids.STEAM_SOURCE.get())) {
                return drainSteam(resource, action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drainSteam(maxDrain, action);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.core86.reactor_console");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ReactorConsoleMenu(containerId, inventory, this, this.data);
    }

    public List<String> getLastScanErrors() {
        return lastScanErrors;
    }

    public boolean isAssembled() {
        return isAssembled;
    }

    public int getActiveFuelRods() {
        return activeFuelChannels;
    }

    public int getValidChannels() {
        return validChannels;
    }

    public int getTotalFuel() {
        return totalFuel;
    }

    public int getControlRodCount() {
        return controlChannels;
    }

    public int getAverageInsertion() {
        return controlChannels > 0 ? totalInsertionSum / controlChannels : 0;
    }

    public float getEfficiency() {
        if (activeFuelChannels == 0) {
            return 0f;
        }
        float insertionFactor = 1.0f - (getAverageInsertion() / 100.0f);
        float waterFill = waterTank.getCapacity() > 0
                ? (float) waterTank.getFluidAmount() / waterTank.getCapacity()
                : 0f;
        float voidBonus = (1.0f - waterFill) * CommonConfig.positiveVoidCoeff();
        float baseEfficiency = Math.max(0f, insertionFactor) * (1.0f + voidBonus + reflectorEfficiencyBonus);
        if (experimentMode && canEnableExperimentMode()) {
            baseEfficiency *= 1.0f + CommonConfig.experimentPowerBonus();
        }
        if (spikeTicksRemaining > 0) {
            baseEfficiency *= spikeMultiplier;
        }

        float xenonPenalty = CoreReactorConstants.XENON_PENALTY_NONE;
        if (xenonLevel > CoreReactorConstants.XENON_THRESHOLD_SEVERE) {
            xenonPenalty = CoreReactorConstants.XENON_PENALTY_STRONG;
        } else if (xenonLevel > CoreReactorConstants.XENON_THRESHOLD_NOTICEABLE) {
            xenonPenalty = CoreReactorConstants.XENON_PENALTY_NOTICEABLE;
        } else if (xenonLevel > CoreReactorConstants.XENON_THRESHOLD_MINOR) {
            xenonPenalty = CoreReactorConstants.XENON_PENALTY_MINOR;
        }
        baseEfficiency *= xenonPenalty;
        return Math.max(0f, baseEfficiency);
    }

    public ReactorState getReactorState() {
        return reactorState;
    }

    public float getPowerOutput() {
        return powerOutput;
    }

    public float getTemperature() {
        return temperature;
    }

    public boolean isReactorActive() {
        return reactorState == ReactorState.OPERATING;
    }

    public int getTargetInsertion() {
        return targetInsertion;
    }

    public float getXenonLevel() {
        return xenonLevel;
    }

    public void setXenonLevel(float xenonLevel) {
        this.xenonLevel = Math.max(0f, Math.min(1f, xenonLevel));
    }

    public boolean isExperimentMode() {
        return experimentMode;
    }

    public boolean canEnableExperimentMode() {
        return siurAuthorized;
    }

    public void setExperimentMode(boolean enabled) {
        this.experimentMode = enabled && canEnableExperimentMode();
        setChanged();
    }

    public void toggleExperimentMode() {
        setExperimentMode(!experimentMode);
    }

    public boolean isSiurAuthorized() {
        return siurAuthorized;
    }

    public boolean tryAuthorizeWithKey(Player player, InteractionHand hand) {
        if (siurAuthorized) {
            return false;
        }
        var held = player.getItemInHand(hand);
        if (!held.is(CoreItems.SIUR_KEY_TOKEN.get())) {
            return false;
        }
        siurAuthorized = true;
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        setChanged();
        return true;
    }

    public void cycleTargetInsertion() {
        targetInsertion -= 25;
        if (targetInsertion < 0) {
            targetInsertion = 100;
        }
        setChanged();
    }

    public void scheduleTransientSpike(int ticks, float multiplier) {
        this.spikeTicksRemaining = ticks;
        this.spikeMultiplier = multiplier;
        setChanged();
    }

    public void setTargetInsertion(int value) {
        if (scramInProgress && value < 100) {
            return;
        }
        if (value == 100) {
            applyAz5Transient();
        }
        this.targetInsertion = Math.max(0, Math.min(100, value));
        if (level != null) {
            if (controlChannelBases.isEmpty()) {
                rediscoverControlChannels(level);
            }
            setAllControlRods(level, this.targetInsertion, scramInProgress ? ReactorControlRodBlockEntity.DriveMode.SCRAM : ReactorControlRodBlockEntity.DriveMode.NORMAL);
        }
        setChanged();
    }

    private void applyAz5Transient() {
        if (CommonConfig.scramSpikeOncePerCycle() && scramSpikeApplied) {
            return;
        }
        if (reactorState != ReactorState.OPERATING || targetInsertion >= 100 || az5TransientCooldownTicks > 0) {
            return;
        }
        int avg = getAverageInsertion();
        if (avg >= 80) {
            return;
        }
        az5TransientCooldownTicks = 40;
        scramSpikeApplied = true;
        if (avg < 15) {
            temperature += CommonConfig.scramStrongSpikeC();
            coolingDelayTicks = Math.max(coolingDelayTicks, CommonConfig.scramStrongCoolingDelayTicks());
        } else {
            temperature += CommonConfig.scramWeakSpikeC();
        }
    }

    public void triggerScram() {
        scramInProgress = true;
        scramLockoutTicks = CommonConfig.scramLockoutSec() * 20;
        scramSpikeApplied = false;
        setTargetInsertion(100);
        reactorState = ReactorState.SCRAM;
        setChanged();
    }

    public void setChannelInsertion(int index, int value) {
        if (scramInProgress && value < 100) {
            return;
        }
        if (level == null) {
            return;
        }
        if (controlChannelBases.isEmpty()) {
            rediscoverControlChannels(level);
        }
        if (index < 0 || index >= controlChannelBases.size()) {
            return;
        }
        BlockEntity be = level.getBlockEntity(controlChannelBases.get(index));
        if (be instanceof ReactorControlRodBlockEntity rod) {
            rod.setInsertionLevel(value, scramInProgress ? ReactorControlRodBlockEntity.DriveMode.SCRAM : ReactorControlRodBlockEntity.DriveMode.NORMAL);
        }
        setChanged();
    }

    public int getChannelInsertion(int index) {
        if (level != null && index >= 0 && index < controlChannelBases.size()) {
            BlockEntity be = level.getBlockEntity(controlChannelBases.get(index));
            if (be instanceof ReactorControlRodBlockEntity rod) {
                return rod.getInsertionLevel();
            }
        }
        return 0;
    }

    public int getChannelTargetInsertion(int index) {
        if (level != null && index >= 0 && index < controlChannelBases.size()) {
            BlockEntity be = level.getBlockEntity(controlChannelBases.get(index));
            if (be instanceof ReactorControlRodBlockEntity rod) {
                return rod.getTargetInsertionLevel();
            }
        }
        return getChannelInsertion(index);
    }

    public boolean isChannelMoving(int index) {
        if (level != null && index >= 0 && index < controlChannelBases.size()) {
            BlockEntity be = level.getBlockEntity(controlChannelBases.get(index));
            if (be instanceof ReactorControlRodBlockEntity rod) {
                return rod.isMoving();
            }
        }
        return false;
    }

    public int getControlChannelCount() {
        return controlChannelBases.size();
    }

    public BlockPos getControlChannelPos(int index) {
        if (index >= 0 && index < controlChannelBases.size()) {
            return controlChannelBases.get(index);
        }
        return null;
    }

    public void setChannelColor(int index, int colorId) {
        if (index >= 0 && index < controlChannelBases.size()) {
            rodGroupColors.put(controlChannelBases.get(index), colorId);
            setChanged();
        }
    }

    public int getChannelColor(int index) {
        if (index >= 0 && index < controlChannelBases.size()) {
            return rodGroupColors.getOrDefault(controlChannelBases.get(index), 0);
        }
        return 0;
    }

    public void setGroupInsertion(int colorId, int insertion) {
        if (scramInProgress && insertion < 100) {
            return;
        }
        for (int i = 0; i < controlChannelBases.size(); i++) {
            if (getChannelColor(i) == colorId) {
                setChannelInsertion(i, insertion);
            }
        }
    }

    private void updateMap(BlockPos worldPos, byte type) {
        int relX = worldPos.getX() - this.worldPosition.getX() + CoreReactorConstants.MAP_OFFSET;
        int relZ = worldPos.getZ() - this.worldPosition.getZ() + CoreReactorConstants.MAP_OFFSET;
        if (relX < 0 || relX >= CoreReactorConstants.MAP_SIZE
                || relZ < 0 || relZ >= CoreReactorConstants.MAP_SIZE) {
            return;
        }
        byte existing = reactorMap[relX][relZ];
        if (mapPriority(type) >= mapPriority(existing)) {
            reactorMap[relX][relZ] = type;
        }
    }

    private static int mapPriority(byte type) {
        return switch (type) {
            case CoreReactorConstants.MAP_FUEL,
                 CoreReactorConstants.MAP_CONTROL,
                 CoreReactorConstants.MAP_STEAM -> 5;
            case CoreReactorConstants.MAP_REFLECTOR -> 3;
            case CoreReactorConstants.MAP_GRAPHITE -> 2;
            default -> 0;
        };
    }

    public byte[][] getReactorMap() {
        return reactorMap;
    }

    public void setReactorMapFromFlat(byte[] flat) {
        if (flat.length == CoreReactorConstants.MAP_CELLS) {
            for (int i = 0; i < CoreReactorConstants.MAP_SIZE; i++) {
                System.arraycopy(flat, i * CoreReactorConstants.MAP_SIZE, reactorMap[i], 0, CoreReactorConstants.MAP_SIZE);
            }
        } else if (flat.length == 225) {
            clearMap();
            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    reactorMap[i + 8][j + 8] = flat[i * 15 + j];
                }
            }
        }
    }

    private void clearMap() {
        for (int i = 0; i < CoreReactorConstants.MAP_SIZE; i++) {
            Arrays.fill(reactorMap[i], CoreReactorConstants.MAP_EMPTY);
        }
    }

    public byte[] getFlatReactorMap() {
        byte[] mapFlat = new byte[CoreReactorConstants.MAP_CELLS];
        for (int i = 0; i < CoreReactorConstants.MAP_SIZE; i++) {
            System.arraycopy(reactorMap[i], 0, mapFlat, i * CoreReactorConstants.MAP_SIZE, CoreReactorConstants.MAP_SIZE);
        }
        return mapFlat;
    }

    public void scanStructure(Level level, BlockPos consolePos) {
        activeFuelChannels = 0;
        validChannels = 0;
        controlChannels = 0;
        steamChannels = 0;
        isAssembled = false;
        totalFuel = 0;
        totalInsertionSum = 0;
        reflectorEfficiencyBonus = 0f;
        controlChannelBases.clear();
        fuelChannelBases.clear();
        steamChannelBases.clear();
        powerOutput = 0f;
        lastScanErrors.clear();
        clearMap();

        Set<Long> registeredFuel = new HashSet<>();
        Set<Long> registeredControl = new HashSet<>();
        Set<Long> registeredSteam = new HashSet<>();
        int reflectorBlocks = 0;

        int yMin = consolePos.getY() - 12;
        int yMax = consolePos.getY() + 20;

        for (int dx = -CoreReactorConstants.MAP_RADIUS; dx <= CoreReactorConstants.MAP_RADIUS; dx++) {
            for (int dz = -CoreReactorConstants.MAP_RADIUS; dz <= CoreReactorConstants.MAP_RADIUS; dz++) {
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos p = consolePos.offset(dx, y - consolePos.getY(), dz);
                    BlockState state = level.getBlockState(p);

                    if (state.is(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get())) {
                        updateMap(p, CoreReactorConstants.MAP_GRAPHITE);
                    } else if (state.is(CoreBlocks.REACTOR_NEUTRON_REFLECTOR.get())) {
                        reflectorBlocks++;
                        updateMap(p, CoreReactorConstants.MAP_REFLECTOR);
                    }
                }
            }
        }

        for (int dx = -CoreReactorConstants.MAP_RADIUS; dx <= CoreReactorConstants.MAP_RADIUS; dx++) {
            for (int dz = -CoreReactorConstants.MAP_RADIUS; dz <= CoreReactorConstants.MAP_RADIUS; dz++) {
                int worldX = consolePos.getX() + dx;
                int worldZ = consolePos.getZ() + dz;
                BlockPos fuelPos = findBlockInColumn(level, worldX, worldZ, yMin, yMax, CoreBlocks.REACTOR_FUEL_ROD.get());
                if (fuelPos != null) {
                    long key = ReactorColumnHelper.columnKey(fuelPos);
                    if (!registeredFuel.contains(key)) {
                        registeredFuel.add(key);
                        validateFuelChannel(level, fuelPos);
                    }
                }

                BlockPos controlPos = findBlockInColumn(level, worldX, worldZ, yMin, yMax, CoreBlocks.REACTOR_CONTROL_ROD.get());
                if (controlPos != null) {
                    long key = ReactorColumnHelper.columnKey(controlPos);
                    if (!registeredControl.contains(key)) {
                        registeredControl.add(key);
                        validateControlChannel(level, controlPos);
                    }
                }

                BlockPos steamPos = findBlockInColumn(level, worldX, worldZ, yMin, yMax, CoreBlocks.REACTOR_STEAM_CHANNEL.get());
                if (steamPos != null) {
                    long key = ReactorColumnHelper.columnKey(steamPos);
                    if (!registeredSteam.contains(key)) {
                        registeredSteam.add(key);
                        validateSteamChannel(level, steamPos);
                    }
                }
            }
        }

        reflectorEfficiencyBonus = Math.min(0.5f, reflectorBlocks * CoreReactorConstants.REFLECTOR_EFFICIENCY_BONUS);
        isAssembled = validChannels > 0;

        if (controlChannelBases.isEmpty() && validChannels > 0) {
            lastScanErrors.add("ÐŸÑ€ÐµÐ´ÑƒÐ¿Ñ€ÐµÐ¶Ð´ÐµÐ½Ð¸Ðµ: Ð½ÐµÑ‚ ÐºÐ°Ð½Ð°Ð»Ð¾Ð² Ð¡Ð£Ð— â€” Ñ€ÐµÐ°ÐºÑ‚Ð¾Ñ€ Ð½ÐµÑƒÐ¿Ñ€Ð°Ð²Ð»ÑÐµÐ¼");
        }
        if (validChannels == 0) {
            lastScanErrors.add("ÐÐµ Ð½Ð°Ð¹Ð´ÐµÐ½Ð¾ Ð½Ð¸ Ð¾Ð´Ð½Ð¾Ð³Ð¾ Ð²Ð°Ð»Ð¸Ð´Ð½Ð¾Ð³Ð¾ ÐºÐ°Ð½Ð°Ð»Ð° Ð¢Ð’Ð¡ (ÐºÐ¾Ð»Ð¾Ð½Ð½Ð° â‰¥ "
                    + CoreReactorConstants.MIN_CHANNEL_HEIGHT + " Ð±Ð»Ð¾ÐºÐ¾Ð²)");
        }

        updateReactorState();

        if (!level.isClientSide) {
            CorePackets.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new ReactorMapSyncPacket(this.worldPosition, getFlatReactorMap(), xenonLevel));
            BlockState selfState = level.getBlockState(this.worldPosition);
            level.sendBlockUpdated(this.worldPosition, selfState, selfState, 3);
            for (BlockPos steamBase : steamChannelBases) {
                BlockEntity be = level.getBlockEntity(steamBase);
                if (be instanceof ReactorSteamChannelBlockEntity steam) {
                    steam.setControllerPos(this.worldPosition);
                    steam.linkColumn(level);
                }
            }
        }

        setChanged();
    }

    @Nullable
    private static BlockPos findBlockInColumn(Level level, int worldX, int worldZ, int yMin, int yMax,
                                              net.minecraft.world.level.block.Block target) {
        for (int y = yMin; y <= yMax; y++) {
            BlockPos p = new BlockPos(worldX, y, worldZ);
            if (level.getBlockState(p).is(target)) {
                return p;
            }
        }
        return null;
    }

    private void validateFuelChannel(Level level, BlockPos anyPos) {
        ReactorColumnHelper.ColumnInfo column = ReactorColumnHelper.scanColumn(level, anyPos,
                s -> s.is(CoreBlocks.REACTOR_FUEL_ROD.get()));
        BlockPos base = column.base();

        if (column.height() < CoreReactorConstants.MIN_CHANNEL_HEIGHT) {
            lastScanErrors.add("Ð¢Ð’Ð¡ " + base.toShortString() + ": Ð²Ñ‹ÑÐ¾Ñ‚Ð° " + column.height()
                    + " â€” Ð½ÑƒÐ¶Ð½Ð¾ â‰¥ " + CoreReactorConstants.MIN_CHANNEL_HEIGHT + " Ð±Ð»Ð¾ÐºÐ¾Ð² Ð¿Ð¾ Ð²ÐµÑ€Ñ‚Ð¸ÐºÐ°Ð»Ð¸");
            return;
        }

        if (!hasEnoughGraphite(level, column)) {
            int neighbors = countGraphiteNeighbors(level, column.segments().get(column.height() / 2));
            lastScanErrors.add("Ð¢Ð’Ð¡ " + base.toShortString() + ": Ð¼Ð°Ð»Ð¾ Ð³Ñ€Ð°Ñ„Ð¸Ñ‚Ð° ("
                    + neighbors + "/" + CoreReactorConstants.MIN_GRAPHITE_NEIGHBORS + " Ñƒ ÑÐµÑ€ÐµÐ´Ð¸Ð½Ñ‹ ÐºÐ¾Ð»Ð¾Ð½Ð½Ñ‹)");
            return;
        }

        validChannels++;
        fuelChannelBases.add(base.immutable());
        updateMap(base, CoreReactorConstants.MAP_FUEL);

        BlockEntity be = level.getBlockEntity(base);
        if (be instanceof ReactorFuelRodBlockEntity fuel) {
            if (fuel.hasFuel()) {
                activeFuelChannels++;
                totalFuel += fuel.getFuelAmount();
            }
        }

        for (BlockPos segment : column.segments()) {
            BlockEntity segBe = level.getBlockEntity(segment);
            if (segBe instanceof ReactorFuelRodBlockEntity rod) {
                rod.setColumnBase(base);
            }
        }
    }

    private void validateControlChannel(Level level, BlockPos anyPos) {
        ReactorColumnHelper.ColumnInfo column = ReactorColumnHelper.scanColumn(level, anyPos,
                s -> s.is(CoreBlocks.REACTOR_CONTROL_ROD.get()));
        BlockPos base = column.base();

        if (column.height() < CoreReactorConstants.MIN_CHANNEL_HEIGHT) {
            lastScanErrors.add("Ð¡Ð£Ð— " + base.toShortString() + ": Ð²Ñ‹ÑÐ¾Ñ‚Ð° " + column.height()
                    + " â€” Ð½ÑƒÐ¶Ð½Ð¾ â‰¥ " + CoreReactorConstants.MIN_CHANNEL_HEIGHT + " Ð±Ð»Ð¾ÐºÐ¾Ð² Ð¿Ð¾ Ð²ÐµÑ€Ñ‚Ð¸ÐºÐ°Ð»Ð¸");
            return;
        }

        if (!hasEnoughGraphite(level, column)) {
            int neighbors = countGraphiteNeighbors(level, column.segments().get(column.height() / 2));
            lastScanErrors.add("Ð¡Ð£Ð— " + base.toShortString() + ": Ð¼Ð°Ð»Ð¾ Ð³Ñ€Ð°Ñ„Ð¸Ñ‚Ð° ("
                    + neighbors + "/" + CoreReactorConstants.MIN_GRAPHITE_NEIGHBORS
                    + " ÑÐ¾ÑÐµÐ´ÐµÐ¹ Ñƒ ÑÐµÑ€ÐµÐ´Ð¸Ð½Ñ‹ ÐºÐ¾Ð»Ð¾Ð½Ð½Ñ‹)");
            return;
        }

        controlChannels++;
        controlChannelBases.add(base.immutable());
        updateMap(base, CoreReactorConstants.MAP_CONTROL);

        BlockEntity be = level.getBlockEntity(base);
        if (be instanceof ReactorControlRodBlockEntity rod) {
            totalInsertionSum += rod.getInsertionLevel();
        }

        for (BlockPos segment : column.segments()) {
            BlockEntity segBe = level.getBlockEntity(segment);
            if (segBe instanceof ReactorControlRodBlockEntity segRod) {
                segRod.setColumnBase(base);
            }
        }
    }

    private void validateSteamChannel(Level level, BlockPos anyPos) {
        ReactorColumnHelper.ColumnInfo column = ReactorColumnHelper.scanColumn(level, anyPos,
                s -> s.is(CoreBlocks.REACTOR_STEAM_CHANNEL.get()));
        BlockPos base = column.base();

        if (column.height() < CoreReactorConstants.MIN_CHANNEL_HEIGHT) {
            lastScanErrors.add("ÐŸÐ°Ñ€Ð¾Ð²Ð¾Ð¹ ÐºÐ°Ð½Ð°Ð» " + base.toShortString() + ": ÐºÐ¾Ð»Ð¾Ð½Ð½Ð° ÑÐ»Ð¸ÑˆÐºÐ¾Ð¼ ÐºÐ¾Ñ€Ð¾Ñ‚ÐºÐ°Ñ");
            return;
        }

        steamChannels++;
        steamChannelBases.add(base.immutable());
        updateMap(base, CoreReactorConstants.MAP_STEAM);

        for (BlockPos segment : column.segments()) {
            BlockEntity segBe = level.getBlockEntity(segment);
            if (segBe instanceof ReactorSteamChannelBlockEntity steam) {
                steam.setColumnBase(base);
                steam.setControllerPos(this.worldPosition);
            }
        }
    }

  /**
     * ÐŸÑ€Ð¾Ð²ÐµÑ€ÑÐµÐ¼ Ð³Ñ€Ð°Ñ„Ð¸Ñ‚ Ñƒ Ð¾ÑÐ½Ð¾Ð²Ð°Ð½Ð¸Ñ, ÑÐµÑ€ÐµÐ´Ð¸Ð½Ñ‹ Ð¸ Ð²ÐµÑ€Ñ…Ð° ÐºÐ¾Ð»Ð¾Ð½Ð½Ñ‹ (Ð½Ðµ Ñƒ ÐºÐ°Ð¶Ð´Ð¾Ð³Ð¾ Ð±Ð»Ð¾ÐºÐ° â€”
     * Ð¸Ð½Ð°Ñ‡Ðµ Ð²ÐµÑ€Ñ…/Ð½Ð¸Ð· Ñ€ÐµÐ°ÐºÑ‚Ð¾Ñ€Ð° Ð±ÐµÐ· ÐºÐ¾Ð»ÑŒÑ†Ð° Ð³Ñ€Ð°Ñ„Ð¸Ñ‚Ð° Ð»Ð¾Ð¼Ð°ÑŽÑ‚ Ð²Ð°Ð»Ð¸Ð´Ð½Ñ‹Ð¹ Ð¼Ð°Ð»Ñ‹Ð¹ ÐºÐ¾Ñ€Ð¿ÑƒÑ).
     */
    private boolean hasEnoughGraphite(Level level, ReactorColumnHelper.ColumnInfo column) {
        List<BlockPos> segments = column.segments();
        if (segments.isEmpty()) {
            return false;
        }
        int[] checkIndices = {0, segments.size() / 2, segments.size() - 1};
        int passed = 0;
        int checks = 0;
        for (int idx : checkIndices) {
            if (idx < 0 || idx >= segments.size()) {
                continue;
            }
            checks++;
            if (countGraphiteNeighbors(level, segments.get(idx)) >= CoreReactorConstants.MIN_GRAPHITE_NEIGHBORS) {
                passed++;
            }
        }
        return checks > 0 && passed >= Math.min(2, checks);
    }

    private static int countGraphiteNeighbors(Level level, BlockPos segment) {
        int graphiteNeighbors = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(segment.relative(dir));
            if (neighbor.is(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get())) {
                graphiteNeighbors++;
            }
        }
        return graphiteNeighbors;
    }

    /** ÐŸÐ¾Ð²Ñ‚Ð¾Ñ€Ð½Ñ‹Ð¹ Ð¿Ð¾Ð¸ÑÐº ÐºÐ°Ð½Ð°Ð»Ð¾Ð² Ð¡Ð£Ð—, ÐµÑÐ»Ð¸ ÑÐ¿Ð¸ÑÐ¾Ðº Ð¿ÑƒÑÑ‚ (Ð¿Ð¾ÑÐ»Ðµ ÑÐ¼ÐµÐ½Ñ‹ ÐºÐ¾Ð½ÑÐ¾Ð»Ð¸ / Ð±ÐµÐ· Ð¿ÐµÑ€ÐµÑÐºÐ°Ð½Ð°). */
    public void rediscoverControlChannels(Level level) {
        controlChannelBases.clear();
        controlChannels = 0;
        totalInsertionSum = 0;

        int yMin = worldPosition.getY() - 12;
        int yMax = worldPosition.getY() + 20;
        Set<Long> registered = new HashSet<>();

        for (int dx = -CoreReactorConstants.MAP_RADIUS; dx <= CoreReactorConstants.MAP_RADIUS; dx++) {
            for (int dz = -CoreReactorConstants.MAP_RADIUS; dz <= CoreReactorConstants.MAP_RADIUS; dz++) {
                int worldX = worldPosition.getX() + dx;
                int worldZ = worldPosition.getZ() + dz;
                BlockPos controlPos = findBlockInColumn(level, worldX, worldZ, yMin, yMax,
                        CoreBlocks.REACTOR_CONTROL_ROD.get());
                if (controlPos != null) {
                    long key = ReactorColumnHelper.columnKey(controlPos);
                    if (!registered.contains(key)) {
                        registered.add(key);
                        validateControlChannel(level, controlPos);
                    }
                }
            }
        }
        setChanged();
    }

    public void updateReactorState() {
        if (!isAssembled || totalFuel == 0) {
            reactorState = ReactorState.OFFLINE;
            powerOutput = 0f;
            return;
        }

        totalInsertionSum = 0;
        if (level != null) {
            for (BlockPos base : controlChannelBases) {
                BlockEntity be = level.getBlockEntity(base);
                if (be instanceof ReactorControlRodBlockEntity rod) {
                    totalInsertionSum += rod.getInsertionLevel();
                }
            }
        }

        if (getAverageInsertion() >= 100) {
            reactorState = ReactorState.SCRAM;
            powerOutput = 0f;
            return;
        }
        reactorState = ReactorState.OPERATING;
        powerOutput = activeFuelChannels * BASE_POWER_PER_ROD * getEfficiency();
    }

    public void tickSimulation() {
        if (level == null || level.isClientSide) {
            return;
        }
        ensureNuclearCountdownScheduledAfterReload();

        tickCount++;

        if (spikeTicksRemaining > 0) {
            spikeTicksRemaining--;
        }
        if (az5TransientCooldownTicks > 0) {
            az5TransientCooldownTicks--;
        }
        if (scramLockoutTicks > 0) {
            scramLockoutTicks--;
        }
        if (scramInProgress && scramLockoutTicks <= 0 && !isAnyControlRodMoving() && getAverageInsertion() >= 100) {
            scramInProgress = false;
            scramSpikeApplied = false;
        }

        float efficiency = getEfficiency();
        // powerRatio uses the current effective reactor efficiency, including any xenon penalty.
        // If xenon growth should instead depend on the pre-penalty efficiency, this logic
        // needs to be separated into a different metric.
        float powerRatio = activeFuelChannels > 0
                ? efficiency
                : 0f;
        int avgInsertion = getAverageInsertion();
        if (powerRatio > 0.75f && avgInsertion < 40) {
            xenonLevel = Math.min(1f, xenonLevel + CoreReactorConstants.XENON_GROWTH_RATE);
        } else if (powerRatio < 0.50f) {
            xenonLevel = Math.max(0f, xenonLevel - CoreReactorConstants.XENON_DECAY_RATE_LOW_POWER);
        } else {
            xenonLevel = Math.max(0f, xenonLevel - CoreReactorConstants.XENON_DECAY_RATE_NORMAL);
        }

        efficiency = getEfficiency();
        float thermalPower = activeFuelChannels * CoreReactorConstants.ROD_POWER_TICK * efficiency;

        int steamChannelCount = Math.max(1, steamChannels);
        float maxSteamByTransfer = 0.0f;
        if (temperature > 100.0f && steamChannels > 0) {
            maxSteamByTransfer = (temperature - 100.0f) * CoreReactorConstants.TRANSFER_COEFF * steamChannelCount;
        }

        int waterAvail = waterTank.getFluidAmount();
        int steamSpace = steamTank.getCapacity() - steamTank.getFluidAmount();

        float rawSteam = Math.min(maxSteamByTransfer, Math.min(waterAvail, steamSpace)) + steamRemainder;
        int steamProduced = (int) rawSteam;
        steamRemainder = rawSteam - steamProduced;
        steamProduced = Math.max(0, steamProduced);

        if (steamProduced > 0) {
            waterTank.drain(steamProduced, IFluidHandler.FluidAction.EXECUTE);
            steamTank.fill(new FluidStack(CoreFluids.STEAM_SOURCE.get(), steamProduced), IFluidHandler.FluidAction.EXECUTE);
        }

        float heatRemoved = steamProduced / CoreReactorConstants.STEAM_PER_HEAT;
        float passiveCooling = coolingDelayTicks > 0 ? 0f : Math.max(0, temperature - CoreReactorConstants.AMBIENT_TEMP)
                * CoreReactorConstants.PASSIVE_COOLING_TICK;
        float heating = thermalPower - heatRemoved - passiveCooling;
        if (experimentMode && canEnableExperimentMode()
                && waterTank.getFluidAmount() < waterTank.getCapacity() / 4
                && reactorState == ReactorState.OPERATING) {
            heating += CommonConfig.experimentInstabilityHeatBonus();
        }
        temperature += heating / CoreReactorConstants.THERMAL_MASS;
        if (coolingDelayTicks > 0) {
            coolingDelayTicks--;
        }

        if (efficiency > 0 && tickCount % 20 == 0 && reactorState == ReactorState.OPERATING) {
            int totalBurn = Math.round(activeFuelChannels * CoreReactorConstants.FUEL_BURN_RATE_SEC * efficiency);
            if (totalBurn > 0) {
                burnFuelInChannels(totalBurn);
            }
        }

        if (!experimentMode && temperature > CoreReactorConstants.SCRAM_TEMP && reactorState != ReactorState.SCRAM) {
            setTargetInsertion(100);
            reactorState = ReactorState.SCRAM;
            level.players().forEach(p -> p.sendSystemMessage(
                    Component.literal("Â§c[ÐÐ—-5] Ð ÐµÐ°ÐºÑ‚Ð¾Ñ€ Ð°Ð²Ð°Ñ€Ð¸Ð¹Ð½Ð¾ Ð·Ð°Ð³Ð»ÑƒÑˆÐµÐ½! Ð¢ÐµÐ¼Ð¿ÐµÑ€Ð°Ñ‚ÑƒÑ€Ð°: " + (int) temperature + "Â°C")
            ));
        }

        if (temperature > CoreReactorConstants.MELTDOWN_TEMP) {
            meltdown();
        }

        if (reactorState == ReactorState.OPERATING) {
            powerOutput = activeFuelChannels * BASE_POWER_PER_ROD * getEfficiency();
        }

        setChanged();
    }

    private void burnFuelInChannels(int totalBurn) {
        if (level == null || fuelChannelBases.isEmpty()) {
            return;
        }

        int perChannel = Math.max(1, Math.round((float) totalBurn / fuelChannelBases.size()));
        int totalBurned = 0;
        int fueled = 0;

        for (BlockPos base : fuelChannelBases) {
            BlockEntity be = level.getBlockEntity(base);
            if (be instanceof ReactorFuelRodBlockEntity fuel && fuel.hasFuel()) {
                totalBurned += fuel.burnFuel(perChannel);
                fueled++;
            }
        }

        totalFuel = Math.max(0, totalFuel - totalBurned);
        activeFuelChannels = fueled;

        if (totalFuel <= 0) {
            reactorState = ReactorState.OFFLINE;
            powerOutput = 0f;
            activeFuelChannels = 0;
        }
        updateReactorState();
    }

    private void meltdown() {
        if (level == null || meltdownTriggered) {
            return;
        }
        meltdownTriggered = true;
        meltdownStage = 1;
        triggerScram();
        level.explode(null,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                CoreReactorConstants.MELTDOWN_EXPLOSION_RADIUS_STEAM,
                Level.ExplosionInteraction.BLOCK);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            IrradiationTicker.scheduleNuclearCountdown(serverLevel, worldPosition, CommonConfig.meltdownNuclearDelayTicks());
        }
        meltdownStage = 2;
        nuclearCountdownRecoveryChecked = true;
        level.players().forEach(p -> p.sendSystemMessage(
                Component.literal("§4[MELTDOWN] Steam explosion detected. Nuclear stage pending.")
        ));
    }

    private void ensureNuclearCountdownScheduledAfterReload() {
        if (nuclearCountdownRecoveryChecked || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        nuclearCountdownRecoveryChecked = true;
        if (!meltdownTriggered || meltdownStage < 2) {
            return;
        }
        RadiationSavedData radiation = RadiationSavedData.get(serverLevel);
        if (radiation.hasExplosionTriggered(worldPosition)) {
            return;
        }
        if (!IrradiationTicker.hasPendingCountdown(serverLevel, worldPosition)) {
            IrradiationTicker.scheduleNuclearCountdown(serverLevel, worldPosition, CommonConfig.meltdownNuclearDelayTicks());
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorConsoleBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.tickSimulation();
    }

    public void setAllControlRods(Level level, int targetLevel, ReactorControlRodBlockEntity.DriveMode mode) {
        for (BlockPos base : controlChannelBases) {
            BlockEntity be = level.getBlockEntity(base);
            if (be instanceof ReactorControlRodBlockEntity rod) {
                rod.setInsertionLevel(targetLevel, mode);
            }
        }
    }

    public boolean isScramInProgress() {
        return scramInProgress;
    }

    private boolean isAnyControlRodMoving() {
        if (level == null) {
            return false;
        }
        for (BlockPos base : controlChannelBases) {
            BlockEntity be = level.getBlockEntity(base);
            if (be instanceof ReactorControlRodBlockEntity rod) {
                if (rod.isMoving()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isAssembled = tag.getBoolean("IsAssembled");
        activeFuelChannels = tag.contains("ActiveFuelRods") ? tag.getInt("ActiveFuelRods") : tag.getInt("ActiveFuelChannels");
        validChannels = tag.getInt("ValidChannels");
        controlChannels = tag.contains("ControlChannels") ? tag.getInt("ControlChannels") : tag.getInt("ControlRods");
        steamChannels = tag.getInt("SteamChannels");
        totalFuel = tag.getInt("TotalFuel");
        targetInsertion = tag.getInt("TargetInsertion");
        temperature = tag.getFloat("Temperature");
        spikeTicksRemaining = tag.getInt("SpikeTicksRemaining");
        spikeMultiplier = tag.getFloat("SpikeMultiplier");
        steamRemainder = tag.getFloat("SteamRemainder");
        reflectorEfficiencyBonus = tag.getFloat("ReflectorBonus");
        xenonLevel = tag.contains("XenonLevel") ? tag.getFloat("XenonLevel") : 0f;
        xenonLevel = Math.max(0f, Math.min(1f, xenonLevel));
        experimentMode = tag.getBoolean("ExperimentMode");
        siurAuthorized = tag.getBoolean("SiurAuthorized");
        coolingDelayTicks = tag.getInt("CoolingDelayTicks");
        meltdownTriggered = tag.getBoolean("MeltdownTriggered");
        meltdownStage = tag.getInt("MeltdownStage");
        if (!meltdownTriggered && meltdownStage > 0) {
            meltdownTriggered = true;
        }
        nuclearCountdownRecoveryChecked = false;
        scramInProgress = tag.getBoolean("ScramInProgress");
        scramLockoutTicks = tag.getInt("ScramLockoutTicks");
        scramSpikeApplied = tag.getBoolean("ScramSpikeApplied");
        if (!siurAuthorized && tag.getBoolean("SiurKeyAuthorized")) {
            siurAuthorized = true;
        }
        if (!siurAuthorized && tag.contains("SiurKey")) {
            CompoundTag siurKey = tag.getCompound("SiurKey");
            if (siurKey.contains("Items") && !siurKey.getList("Items", 10).isEmpty()) {
                siurAuthorized = true;
            }
        }
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        steamTank.readFromNBT(tag.getCompound("SteamTank"));
        try {
            reactorState = ReactorState.valueOf(tag.getString("ReactorState"));
        } catch (IllegalArgumentException e) {
            reactorState = ReactorState.OFFLINE;
        }

        controlChannelBases.clear();
        fuelChannelBases.clear();
        steamChannelBases.clear();
        rodGroupColors.clear();

        int count = tag.getInt("ChannelCount");
        for (int i = 0; i < count; i++) {
            BlockPos channelBase = BlockPos.of(tag.getLong("Channel_" + i));
            controlChannelBases.add(channelBase);
            rodGroupColors.put(channelBase, tag.getInt("ChannelColor_" + i));
        }

        int fCount = tag.getInt("FuelChannelCount");
        for (int i = 0; i < fCount; i++) {
            fuelChannelBases.add(BlockPos.of(tag.getLong("FuelChannel_" + i)));
        }

        int sCount = tag.getInt("SteamChannelCount");
        for (int i = 0; i < sCount; i++) {
            steamChannelBases.add(BlockPos.of(tag.getLong("SteamChannel_" + i)));
        }

        if (tag.contains("ReactorMap")) {
            setReactorMapFromFlat(tag.getByteArray("ReactorMap"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsAssembled", isAssembled);
        tag.putInt("ActiveFuelChannels", activeFuelChannels);
        tag.putInt("ValidChannels", validChannels);
        tag.putInt("ControlChannels", controlChannels);
        tag.putInt("SteamChannels", steamChannels);
        tag.putInt("TotalFuel", totalFuel);
        tag.putInt("TargetInsertion", targetInsertion);
        tag.putFloat("Temperature", temperature);
        tag.putInt("SpikeTicksRemaining", spikeTicksRemaining);
        tag.putFloat("SpikeMultiplier", spikeMultiplier);
        tag.putFloat("SteamRemainder", steamRemainder);
        tag.putFloat("ReflectorBonus", reflectorEfficiencyBonus);
        tag.putBoolean("ExperimentMode", experimentMode);
        tag.putBoolean("SiurAuthorized", siurAuthorized);
        tag.putInt("CoolingDelayTicks", coolingDelayTicks);
        tag.putBoolean("MeltdownTriggered", meltdownTriggered);
        tag.putInt("MeltdownStage", meltdownStage);
        tag.putBoolean("ScramInProgress", scramInProgress);
        tag.putInt("ScramLockoutTicks", scramLockoutTicks);
        tag.putBoolean("ScramSpikeApplied", scramSpikeApplied);
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.put("SteamTank", steamTank.writeToNBT(new CompoundTag()));
        tag.putString("ReactorState", reactorState.name());

        tag.putInt("ChannelCount", controlChannelBases.size());
        for (int i = 0; i < controlChannelBases.size(); i++) {
            BlockPos channelBase = controlChannelBases.get(i);
            tag.putLong("Channel_" + i, channelBase.asLong());
            tag.putInt("ChannelColor_" + i, rodGroupColors.getOrDefault(channelBase, 0));
        }

        tag.putInt("FuelChannelCount", fuelChannelBases.size());
        for (int i = 0; i < fuelChannelBases.size(); i++) {
            tag.putLong("FuelChannel_" + i, fuelChannelBases.get(i).asLong());
        }

        tag.putInt("SteamChannelCount", steamChannelBases.size());
        for (int i = 0; i < steamChannelBases.size(); i++) {
            tag.putLong("SteamChannel_" + i, steamChannelBases.get(i).asLong());
        }

        tag.putFloat("XenonLevel", xenonLevel);
        tag.putByteArray("ReactorMap", getFlatReactorMap());
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

