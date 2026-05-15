package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.concrete.CementPowderItem;
import io.github.blamp26.core86.content.concrete.WetConcreteMixItem;
import io.github.blamp26.core86.content.metallurgy.SteelIngotItem;
import io.github.blamp26.core86.content.metallurgy.SteelRodItem;
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

    private CoreItems() {
    }
}
