package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.concrete.CementPowderItem;
import io.github.blamp26.core86.content.concrete.WetConcreteMixItem;
import io.github.blamp26.core86.content.metallurgy.SteelIngotItem;
import io.github.blamp26.core86.content.metallurgy.SteelRodItem;
import io.github.blamp26.core86.content.reactor.DosimeterItem;
import io.github.blamp26.core86.content.reactor.EmptyFuelRodItem;
import io.github.blamp26.core86.content.reactor.RbmkFuelRodItem;
import io.github.blamp26.core86.content.reactor.SpentFuelRodItem;
import io.github.blamp26.core86.content.reactor.UraniumDustItem;
import io.github.blamp26.core86.content.reactor.UraniumFuelPelletItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Core86.MODID);

    public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new SteelIngotItem(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_ROD = ITEMS.register("steel_rod", () -> new SteelRodItem(new Item.Properties()));
    public static final RegistryObject<Item> CEMENT_POWDER = ITEMS.register("cement_powder", () -> new CementPowderItem(new Item.Properties()));
    public static final RegistryObject<Item> WET_CONCRETE_MIX = ITEMS.register("wet_concrete_mix", () -> new WetConcreteMixItem(new Item.Properties()));
    public static final RegistryObject<Item> REINFORCED_CONCRETE_BLOCK_ITEM = ITEMS.register("reinforced_concrete_block", () -> new BlockItem(CoreBlocks.REINFORCED_CONCRETE_BLOCK.get(), new Item.Properties()));

    // Reactor Items
    public static final RegistryObject<Item> REACTOR_FUEL_ROD = ITEMS.register("reactor_fuel_rod", 
            () -> new BlockItem(CoreBlocks.REACTOR_FUEL_ROD.get(), new Item.Properties()));
    public static final RegistryObject<Item> REACTOR_GRAPHITE_MODERATOR = ITEMS.register("reactor_graphite_moderator", 
            () -> new BlockItem(CoreBlocks.REACTOR_GRAPHITE_MODERATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> REACTOR_NEUTRON_REFLECTOR = ITEMS.register("reactor_neutron_reflector",
            () -> new BlockItem(CoreBlocks.REACTOR_NEUTRON_REFLECTOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> REACTOR_STEAM_CHANNEL = ITEMS.register("reactor_steam_channel", 
            () -> new BlockItem(CoreBlocks.REACTOR_STEAM_CHANNEL.get(), new Item.Properties()));
    public static final RegistryObject<Item> REACTOR_CONTROL_ROD = ITEMS.register("reactor_control_rod", 
            () -> new BlockItem(CoreBlocks.REACTOR_CONTROL_ROD.get(), new Item.Properties()));
    public static final RegistryObject<Item> REACTOR_CONSOLE = ITEMS.register("reactor_console", 
            () -> new BlockItem(CoreBlocks.REACTOR_CONSOLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> IRRADIATED_BLOCK = ITEMS.register("irradiated_block",
            () -> new BlockItem(CoreBlocks.IRRADIATED_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> STEAM_TURBINE = ITEMS.register("steam_turbine",
            () -> new BlockItem(CoreBlocks.STEAM_TURBINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> STEAM_CONDENSER = ITEMS.register("steam_condenser",
            () -> new BlockItem(CoreBlocks.STEAM_CONDENSER.get(), new Item.Properties()));

    public static final RegistryObject<Item> URANIUM_ORE = ITEMS.register("uranium_ore", 
        () -> new BlockItem(CoreBlocks.URANIUM_ORE.get(), new Item.Properties())); 
 
    public static final RegistryObject<Item> DEEPSLATE_URANIUM_ORE = ITEMS.register("deepslate_uranium_ore", 
        () -> new BlockItem(CoreBlocks.DEEPSLATE_URANIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> URANIUM_FUEL_PELLET = 
            ITEMS.register("uranium_fuel_pellet", 
                    () -> new UraniumFuelPelletItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> URANIUM_DUST = ITEMS.register("uranium_dust", 
        () -> new UraniumDustItem(new Item.Properties()));

    public static final RegistryObject<Item> EMPTY_FUEL_ROD = ITEMS.register("empty_fuel_rod", 
        () -> new EmptyFuelRodItem(new Item.Properties())); 
 
    public static final RegistryObject<Item> RBMK_FUEL_ROD = ITEMS.register("rbmk_fuel_rod", 
        () -> new RbmkFuelRodItem(new Item.Properties().stacksTo(1))); 
 
    public static final RegistryObject<Item> SPENT_FUEL_ROD = ITEMS.register("spent_fuel_rod", 
        () -> new SpentFuelRodItem(new Item.Properties().stacksTo(16))); 
    public static final RegistryObject<Item> SIUR_KEY_TOKEN = ITEMS.register("siur_key_token",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DOSIMETER = ITEMS.register("dosimeter",
            () -> new DosimeterItem(new Item.Properties().stacksTo(1)));

    private CoreItems() {
    }
}
