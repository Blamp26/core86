package io.github.blamp26.core86.content.turbine;

import io.github.blamp26.core86.content.reactor.CoreReactorConstants;
import io.github.blamp26.core86.foundation.registry.CoreFluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * Логика топлива/мощности как {@code IEngine} в Create Diesel Generators.
 */
public interface ISteamEngine {

    FluidTank getEngineTank();

    default boolean enabled() {
        FluidStack fluid = getEngineTank().getFluid();
        return !fluid.isEmpty() && fluid.getFluid().isSame(CoreFluids.STEAM_SOURCE.get());
    }

    default float getFuelSpeed() {
        return CoreReactorConstants.TURBINE_MAX_RPM;
    }

    /** SU-ёмкость на единицу скорости (strength / speed в терминах CDG). */
    default float getFuelCapacity() {
        float speed = getFuelSpeed();
        if (speed == 0f) {
            return 0f;
        }
        return CoreReactorConstants.TURBINE_STRENGTH / speed;
    }

    default float getFuelBurnRate() {
        return CoreReactorConstants.TURBINE_BURN_RATE;
    }
}
