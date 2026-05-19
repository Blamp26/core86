package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.concrete.ReinforcedConcreteBlock;
import io.github.blamp26.core86.content.reactor.ReactorBaseBlock;
import io.github.blamp26.core86.content.reactor.ReactorConsoleBlock;
import io.github.blamp26.core86.content.reactor.ReactorControlRodBlock;
import io.github.blamp26.core86.content.reactor.ReactorFuelRodBlock;
import io.github.blamp26.core86.content.reactor.IrradiatedBlock;
import io.github.blamp26.core86.content.reactor.ReactorNeutronReflectorBlock;
import io.github.blamp26.core86.content.reactor.ReactorSteamChannelBlock;
import io.github.blamp26.core86.content.reactor.UraniumOreBlock;
import io.github.blamp26.core86.content.reactor.DeepslateUraniumOreBlock;
import io.github.blamp26.core86.content.turbine.SteamCondenserBlock;
import io.github.blamp26.core86.content.turbine.SteamTurbineBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Core86.MODID);

    public static final RegistryObject<Block> REINFORCED_CONCRETE_BLOCK = BLOCKS.register("reinforced_concrete_block", ReinforcedConcreteBlock::new);

    // Reactor Blocks
    public static final RegistryObject<Block> REACTOR_FUEL_ROD = BLOCKS.register("reactor_fuel_rod", 
            ReactorFuelRodBlock::new);
    
    public static final RegistryObject<Block> REACTOR_GRAPHITE_MODERATOR = BLOCKS.register("reactor_graphite_moderator", 
            () -> new ReactorBaseBlock(MapColor.COLOR_BLACK, SoundType.STONE, 3.0F));

    public static final RegistryObject<Block> REACTOR_NEUTRON_REFLECTOR = BLOCKS.register("reactor_neutron_reflector",
            ReactorNeutronReflectorBlock::new);
            
    public static final RegistryObject<Block> REACTOR_STEAM_CHANNEL = BLOCKS.register("reactor_steam_channel", 
            ReactorSteamChannelBlock::new);
            
    public static final RegistryObject<Block> REACTOR_CONTROL_ROD = BLOCKS.register("reactor_control_rod", 
            ReactorControlRodBlock::new);
            
    public static final RegistryObject<Block> REACTOR_CONSOLE = BLOCKS.register("reactor_console", ReactorConsoleBlock::new);
    public static final RegistryObject<Block> IRRADIATED_BLOCK = BLOCKS.register("irradiated_block", IrradiatedBlock::new);

    public static final RegistryObject<Block> STEAM_TURBINE =
            BLOCKS.register("steam_turbine",
                    () -> new SteamTurbineBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .sound(SoundType.METAL)
                            .strength(5.0F)
                            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> STEAM_CONDENSER = BLOCKS.register("steam_condenser", SteamCondenserBlock::new);

    public static final RegistryObject<Block> URANIUM_ORE = BLOCKS.register("uranium_ore", 
        UraniumOreBlock::new); 
 
    public static final RegistryObject<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.register("deepslate_uranium_ore", 
        DeepslateUraniumOreBlock::new);

    private CoreBlocks() {
    }
}
