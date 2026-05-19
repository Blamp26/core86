package io.github.blamp26.core86.content.reactor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class DeepslateUraniumOreBlock extends Block {
    public DeepslateUraniumOreBlock() {
        super(Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .sound(SoundType.DEEPSLATE)
                .strength(4.5F, 3.0F)
                .requiresCorrectToolForDrops());
    }
}
