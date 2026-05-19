package io.github.blamp26.core86.content.reactor;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** Perimeter block — boosts efficiency of adjacent fuel/control channels. */
public class ReactorNeutronReflectorBlock extends ReactorBaseBlock {
    public ReactorNeutronReflectorBlock() {
        super(MapColor.METAL, SoundType.METAL, 4.0F);
    }
}
