package io.github.blamp26.core86.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import io.github.blamp26.core86.content.reactor.ReactorConsoleScreen;
import io.github.blamp26.core86.foundation.registry.CoreBlockEntities;
import io.github.blamp26.core86.foundation.registry.CoreMenus;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CoreMenus.REACTOR_CONSOLE.get(), ReactorConsoleScreen::new);
            BlockEntityRenderers.register(CoreBlockEntities.STEAM_TURBINE.get(), KineticBlockEntityRenderer::new);
        });
    }


}
