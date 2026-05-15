package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.concrete.ReinforcedConcreteBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Core86.MODID);

    public static final RegistryObject<Block> REINFORCED_CONCRETE_BLOCK = BLOCKS.register("reinforced_concrete_block", ReinforcedConcreteBlock::new);

    private CoreBlocks() {
    }
}
