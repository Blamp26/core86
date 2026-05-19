package io.github.blamp26.core86.content.reactor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class UraniumOreBlock extends Block {
    public UraniumOreBlock() {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(3.0F, 3.0F)
                .requiresCorrectToolForDrops());
    }
}
