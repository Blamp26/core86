package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.RbmkFuelRodRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = 
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Core86.MODID);

    public static final RegistryObject<RecipeSerializer<RbmkFuelRodRecipe>> RBMK_FUEL_ROD = 
            SERIALIZERS.register("rbmk_fuel_rod", () -> RbmkFuelRodRecipe.SERIALIZER);

    private CoreRecipeSerializers() {}
}
