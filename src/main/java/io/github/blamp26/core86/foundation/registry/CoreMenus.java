package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.ReactorConsoleMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Core86.MODID);

    public static final RegistryObject<MenuType<ReactorConsoleMenu>> REACTOR_CONSOLE = MENUS.register("reactor_console",
            () -> IForgeMenuType.create(ReactorConsoleMenu::new));

    private CoreMenus() {}
}
