package io.github.blamp26.core86.content.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RbmkFuelRodItem extends Item {
    public static final int MAX_FUEL = CoreReactorConstants.MAX_FUEL_PER_ROD;

    public RbmkFuelRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        int fuel = getFuel(stack);
        int depletion = 100 - (int) ((float) fuel / MAX_FUEL * 100);
        return Component.translatable(this.getDescriptionId(stack))
                .append(" (")
                .append(String.valueOf(depletion))
                .append("%)");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int fuel = getFuel(stack);
        int depletion = 100 - (int) ((float) fuel / MAX_FUEL * 100);
        
        tooltip.add(Component.literal("§7Запас топлива: §f" + fuel + " / " + MAX_FUEL));
        tooltip.add(Component.literal("§7Выгорание: §f" + depletion + "%"));
        
        String status;
        if (fuel == MAX_FUEL) status = "§aСВЕЖИЙ (FRESH)";
        else if (fuel > 0) status = "§eАКТИВНЫЙ (ACTIVE)";
        else status = "§cОТРАБОТАННЫЙ (DEPLETED)";
        
        tooltip.add(Component.literal("§7Статус: " + status));
    }

    public static int getFuel(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return 0;
        return stack.getTag().getInt("Fuel");
    }

    public static void setFuel(ItemStack stack, int fuel) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("Fuel", Math.max(0, Math.min(MAX_FUEL, fuel)));
        tag.putInt("MaxFuel", MAX_FUEL);
        tag.putBoolean("Depleted", fuel <= 0);
    }

    public static boolean isDepleted(ItemStack stack) {
        return getFuel(stack) <= 0;
    }
}
