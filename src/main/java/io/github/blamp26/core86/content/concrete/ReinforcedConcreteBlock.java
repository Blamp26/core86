package io.github.blamp26.core86.content.concrete;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ReinforcedConcreteBlock extends Block {
    public ReinforcedConcreteBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 15.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }
}
