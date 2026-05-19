package io.github.blamp26.core86.foundation.config;

public final class CommonConfig {
    private CommonConfig() {}

    public static float positiveVoidCoeff() {
        return 0.2f;
    }

    public static float experimentPowerBonus() {
        return 0.15f;
    }

    public static float experimentInstabilityHeatBonus() {
        return 4.0f;
    }

    public static float scramWeakSpikeC() {
        return 65.0f;
    }

    public static float scramStrongSpikeC() {
        return 175.0f;
    }

    public static int scramStrongCoolingDelayTicks() {
        return 80;
    }

    public static int meltdownNuclearDelayTicks() {
        return 60;
    }

    public static float irradiationDecayChance() {
        return 0.0005f;
    }

    public static int radiationZoneRadius() {
        return 32;
    }

    public static float radiationZoneInitialIntensityRadPerSec() {
        return 1000.0f;
    }

    public static long radiationZoneHalfLifeTicks() {
        return 24000L * 7L;
    }

    public static float radiationMinIntensityRadPerSec() {
        return 0.001f;
    }

    public static float radiationDoseDecayPerSecond() {
        return 0.002f;
    }

    public static float maxRadiationDoseSv() {
        return 10.0f;
    }

    public static int radiationTickInterval() {
        return 20;
    }

    public static float gameRadPerSecToMillisievertPerHour() {
        return 1.0f;
    }

    public static int rodMoveTimeNormalSec() {
        return 20;
    }

    public static int rodMoveTimeScramSec() {
        return 20;
    }

    public static int scramLockoutSec() {
        return 22;
    }

    public static boolean scramSpikeOncePerCycle() {
        return true;
    }
}
