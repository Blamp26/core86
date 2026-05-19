package io.github.blamp26.core86.foundation.registry;

import io.github.blamp26.core86.Core86;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public class CoreFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Core86.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Core86.MODID);

    public static final ResourceLocation STEAM_STILL_RL = new ResourceLocation("minecraft", "block/water_still");
    public static final ResourceLocation STEAM_FLOWING_RL = new ResourceLocation("minecraft", "block/water_flow");

    public static final RegistryObject<FluidType> STEAM_TYPE = FLUID_TYPES.register("steam", () -> new FluidType(FluidType.Properties.create()
            .density(-10).viscosity(10).temperature(400).canSwim(false).canDrown(false).canPushEntity(false)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return STEAM_STILL_RL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return STEAM_FLOWING_RL;
                }

                @Override
                public int getTintColor() {
                    return 0xFFE0E0E0; // Светло-серый цвет пара
                }
            });
        }
    });

    public static final RegistryObject<ForgeFlowingFluid.Source> STEAM_SOURCE = FLUIDS.register("steam",
            () -> new ForgeFlowingFluid.Source(CoreFluids.STEAM_PROPERTIES));

    public static final RegistryObject<ForgeFlowingFluid.Flowing> STEAM_FLOWING = FLUIDS.register("steam_flowing",
            () -> new ForgeFlowingFluid.Flowing(CoreFluids.STEAM_PROPERTIES));

    public static final ForgeFlowingFluid.Properties STEAM_PROPERTIES = new ForgeFlowingFluid.Properties(
            STEAM_TYPE, STEAM_SOURCE, STEAM_FLOWING);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
