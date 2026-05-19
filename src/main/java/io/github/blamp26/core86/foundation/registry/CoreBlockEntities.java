package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.ReactorConsoleBlockEntity;
import io.github.blamp26.core86.content.reactor.ReactorControlRodBlockEntity;
import io.github.blamp26.core86.content.reactor.ReactorFuelRodBlockEntity;
import io.github.blamp26.core86.content.reactor.ReactorSteamChannelBlockEntity;
import io.github.blamp26.core86.content.turbine.SteamCondenserBlockEntity;
import io.github.blamp26.core86.content.turbine.SteamTurbineBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Core86.MODID);

    public static final RegistryObject<BlockEntityType<ReactorConsoleBlockEntity>> REACTOR_CONSOLE = BLOCK_ENTITIES.register("reactor_console",
            () -> BlockEntityType.Builder.of(ReactorConsoleBlockEntity::new, CoreBlocks.REACTOR_CONSOLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SteamCondenserBlockEntity>> STEAM_CONDENSER = BLOCK_ENTITIES.register("steam_condenser",
            () -> BlockEntityType.Builder.of(SteamCondenserBlockEntity::new, CoreBlocks.STEAM_CONDENSER.get()).build(null));

    public static final RegistryObject<BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE = BLOCK_ENTITIES.register("steam_turbine",
            () -> BlockEntityType.Builder.of(
                    (pos, state) -> new SteamTurbineBlockEntity(CoreBlockEntities.STEAM_TURBINE.get(), pos, state),
                    CoreBlocks.STEAM_TURBINE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<ReactorControlRodBlockEntity>> REACTOR_CONTROL_ROD = 
            BLOCK_ENTITIES.register("reactor_control_rod", 
                    () -> BlockEntityType.Builder.of(ReactorControlRodBlockEntity::new, 
                            CoreBlocks.REACTOR_CONTROL_ROD.get()).build(null));

    public static final RegistryObject<BlockEntityType<ReactorFuelRodBlockEntity>> REACTOR_FUEL_ROD = 
            BLOCK_ENTITIES.register("reactor_fuel_rod", 
                    () -> BlockEntityType.Builder.of(ReactorFuelRodBlockEntity::new, 
                            CoreBlocks.REACTOR_FUEL_ROD.get()).build(null));

    public static final RegistryObject<BlockEntityType<ReactorSteamChannelBlockEntity>> REACTOR_STEAM_CHANNEL = 
            BLOCK_ENTITIES.register("reactor_steam_channel", 
                    () -> BlockEntityType.Builder.of(ReactorSteamChannelBlockEntity::new, 
                            CoreBlocks.REACTOR_STEAM_CHANNEL.get()).build(null));

    private CoreBlockEntities() {
    }
}
