package io.github.blamp26.core86.compat.jei;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.RbmkFuelRodItem;
import io.github.blamp26.core86.foundation.registry.CoreItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.Collections;

@JeiPlugin
public class CoreJEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation(Core86.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Register RBMK Fuel Rod recipe manually for JEI
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(CoreItems.EMPTY_FUEL_ROD.get()));
        for (int i = 0; i < 8; i++) {
            ingredients.add(Ingredient.of(CoreItems.URANIUM_FUEL_PELLET.get()));
        }

        ItemStack result = new ItemStack(CoreItems.RBMK_FUEL_ROD.get());
        RbmkFuelRodItem.setFuel(result, RbmkFuelRodItem.MAX_FUEL);

        ShapelessRecipe jeiRecipe = new ShapelessRecipe(
                new ResourceLocation(Core86.MODID, "rbmk_fuel_rod_jei"),
                "", // Group
                CraftingBookCategory.MISC,
                result,
                ingredients
        );

        registration.addRecipes(RecipeTypes.CRAFTING, Collections.singletonList(jeiRecipe));
    }
}
