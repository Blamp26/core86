package io.github.blamp26.core86.content.reactor;

import io.github.blamp26.core86.foundation.registry.CoreItems;
import io.github.blamp26.core86.foundation.registry.CoreRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RbmkFuelRodRecipe extends CustomRecipe {
    public static final RecipeSerializer<RbmkFuelRodRecipe> SERIALIZER = new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(RbmkFuelRodRecipe::new);

    public RbmkFuelRodRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        boolean hasEmptyRod = false;
        int pelletCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(CoreItems.EMPTY_FUEL_ROD.get())) {
                if (hasEmptyRod) return false;
                hasEmptyRod = true;
            } else if (stack.is(CoreItems.URANIUM_FUEL_PELLET.get())) {
                pelletCount++;
            } else {
                return false;
            }
        }

        return hasEmptyRod && pelletCount == 8;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        ItemStack result = new ItemStack(CoreItems.RBMK_FUEL_ROD.get());
        RbmkFuelRodItem.setFuel(result, RbmkFuelRodItem.MAX_FUEL);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CoreRecipeSerializers.RBMK_FUEL_ROD.get();
    }
}
