package io.github.blamp26.core86;

import com.mojang.logging.LogUtils;
import io.github.blamp26.core86.foundation.registry.*;
import io.github.blamp26.core86.foundation.network.CorePackets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Core86.MODID)
public class Core86 {
    public static final String MODID = "core86";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Core86() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        CoreBlocks.BLOCKS.register(modEventBus);
        CoreItems.ITEMS.register(modEventBus);
        CoreFluids.register(modEventBus);
        CoreBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CoreMenus.MENUS.register(modEventBus);
        CoreCreativeTabs.TABS.register(modEventBus);
        CoreRecipeSerializers.SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CorePackets::register);
    }
}
