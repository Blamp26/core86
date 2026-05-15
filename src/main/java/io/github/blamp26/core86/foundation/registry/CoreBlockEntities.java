package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import io.github.blamp26.core86.content.reactor.ReactorConsoleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CoreBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Core86.MODID);

    public static final RegistryObject<BlockEntityType<ReactorConsoleBlockEntity>> REACTOR_CONSOLE = BLOCK_ENTITIES.register("reactor_console",
            () -> BlockEntityType.Builder.of(ReactorConsoleBlockEntity::new, CoreBlocks.REACTOR_CONSOLE.get()).build(null));

    private CoreBlockEntities() {
    }
}
