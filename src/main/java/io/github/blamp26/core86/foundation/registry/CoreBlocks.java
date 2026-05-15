package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.concrete.ReinforcedConcreteBlock;
import io.github.blamp26.core86.content.reactor.ReactorBaseBlock;
import io.github.blamp26.core86.content.reactor.ReactorConsoleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Core86.MODID);

    public static final RegistryObject<Block> REINFORCED_CONCRETE_BLOCK = BLOCKS.register("reinforced_concrete_block", ReinforcedConcreteBlock::new);

    // Reactor Blocks
    public static final RegistryObject<Block> REACTOR_FUEL_ROD = BLOCKS.register("reactor_fuel_rod", 
            () -> new ReactorBaseBlock(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 5.0F));
    
    public static final RegistryObject<Block> REACTOR_GRAPHITE_MODERATOR = BLOCKS.register("reactor_graphite_moderator", 
            () -> new ReactorBaseBlock(MapColor.COLOR_BLACK, SoundType.STONE, 3.0F));
            
    public static final RegistryObject<Block> REACTOR_STEAM_CHANNEL = BLOCKS.register("reactor_steam_channel", 
            () -> new ReactorBaseBlock(MapColor.COLOR_GRAY, SoundType.METAL, 4.0F));
            
    public static final RegistryObject<Block> REACTOR_CONTROL_ROD = BLOCKS.register("reactor_control_rod", 
            () -> new ReactorBaseBlock(MapColor.COLOR_RED, SoundType.METAL, 5.0F));
            
    public static final RegistryObject<Block> REACTOR_CONSOLE = BLOCKS.register("reactor_console", ReactorConsoleBlock::new);

    private CoreBlocks() {
    }
}
