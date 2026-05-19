package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.RbmkFuelRodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CoreCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Core86.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.core86"))
            .icon(() -> new ItemStack(CoreItems.STEEL_INGOT.get()))
            .displayItems((features, output) -> {
                output.accept(CoreItems.STEEL_INGOT.get());
                output.accept(CoreItems.STEEL_ROD.get());
                output.accept(CoreItems.CEMENT_POWDER.get());
                output.accept(CoreItems.WET_CONCRETE_MIX.get());
                output.accept(CoreItems.REINFORCED_CONCRETE_BLOCK_ITEM.get());
                
                // Reactor
                output.accept(CoreItems.REACTOR_FUEL_ROD.get());
                output.accept(CoreItems.REACTOR_GRAPHITE_MODERATOR.get());
                output.accept(CoreItems.REACTOR_NEUTRON_REFLECTOR.get());
                output.accept(CoreItems.REACTOR_STEAM_CHANNEL.get());
                output.accept(CoreItems.REACTOR_CONTROL_ROD.get());
                output.accept(CoreItems.REACTOR_CONSOLE.get());
                output.accept(CoreItems.STEAM_TURBINE.get());
                output.accept(CoreItems.STEAM_CONDENSER.get());
                output.accept(CoreItems.URANIUM_ORE.get()); 
                output.accept(CoreItems.DEEPSLATE_URANIUM_ORE.get());
                output.accept(CoreItems.URANIUM_FUEL_PELLET.get());
                output.accept(CoreItems.URANIUM_DUST.get());
                output.accept(CoreItems.EMPTY_FUEL_ROD.get());
                
                ItemStack displayRod = new ItemStack(CoreItems.RBMK_FUEL_ROD.get()); 
                RbmkFuelRodItem.setFuel(displayRod, RbmkFuelRodItem.MAX_FUEL); 
                output.accept(displayRod);

                output.accept(CoreItems.SPENT_FUEL_ROD.get());
                output.accept(CoreItems.SIUR_KEY_TOKEN.get());
                output.accept(CoreItems.DOSIMETER.get());

            })
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .build());

    private CoreCreativeTabs() {
    }
}
