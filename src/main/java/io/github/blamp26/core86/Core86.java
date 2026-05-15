package io.github.blamp26.core86;

import io.github.blamp26.core86.foundation.registry.CoreBlocks;
import io.github.blamp26.core86.foundation.registry.CoreCreativeTabs;
import io.github.blamp26.core86.foundation.registry.CoreItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Core86.MODID)
public class Core86 {
    public static final String MODID = "core86";

    public Core86() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        CoreBlocks.BLOCKS.register(modEventBus);
        CoreItems.ITEMS.register(modEventBus);
        CoreCreativeTabs.TABS.register(modEventBus);
    }
}
