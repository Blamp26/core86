package io.github.blamp26.core86.content.reactor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ReactorBaseBlock extends Block {
    public ReactorBaseBlock(MapColor color, SoundType sound, float strength) {
        super(BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(sound)
                .strength(strength)
                .requiresCorrectToolForDrops());
    }
}
