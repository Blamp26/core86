package io.github.blamp26.core86.content.reactor;

public final class CoreReactorConstants {
    private CoreReactorConstants() {}

    // ========== КАРТА / СТРУКТУРА ==========
    public static final int MAP_SIZE = 31;
    public static final int MAP_RADIUS = 15;
    public static final int MAP_OFFSET = 15;
    public static final int MAP_CELLS = MAP_SIZE * MAP_SIZE;
    public static final int MIN_CHANNEL_HEIGHT = 4;
    /** Минимум горизонтальных соседей-графита у проверяемого сегмента колонны */
    public static final int MIN_GRAPHITE_NEIGHBORS = 2;
    public static final float REFLECTOR_EFFICIENCY_BONUS = 0.08f;

    public static final byte MAP_EMPTY = 0;
    public static final byte MAP_GRAPHITE = 1;
    public static final byte MAP_FUEL = 2;
    public static final byte MAP_CONTROL = 3;
    public static final byte MAP_STEAM = 4;
    public static final byte MAP_REFLECTOR = 5;

    // ========== ТЕПЛОФИЗИКА (потиково) ==========
    /** Тепловая мощность одного блока топлива при 100% эффективности (U/tick) */
    public static final float ROD_POWER_TICK = 5.0f;

    /** Сколько mB пара даёт 1 U тепла (1:10 соотношение) */
    public static final float STEAM_PER_HEAT = 10.0f;

    /** Коэффициент теплоотдачи парового канала: U/(°C * tick * канал) */
    public static final float TRANSFER_COEFF = 0.075f;

    /** Теплоёмкость реактора (U/°C) */
    public static final float THERMAL_MASS = 80.0f;

    /** Пассивное охлаждение в воздух U/(°C * tick) */
    public static final float PASSIVE_COOLING_TICK = 0.02f;

    // Положительный паровой коэффициент реактивности
    // 0.5 = при полном отсутствии воды эффективность растёт на 50%
    public static final float POSITIVE_VOID_COEFF = 0.5f;

    // Ксенон-135: накопление / спад
    public static final float XENON_GROWTH_RATE = 0.001f;
    public static final float XENON_DECAY_RATE_LOW_POWER = 0.004f;
    public static final float XENON_DECAY_RATE_NORMAL = 0.00125f;

    // Ксенон-135: штрафы к эффективности
    public static final float XENON_THRESHOLD_MINOR = 0.25f;
    public static final float XENON_THRESHOLD_NOTICEABLE = 0.50f;
    public static final float XENON_THRESHOLD_SEVERE = 0.75f;
    public static final float XENON_PENALTY_NONE = 1.0f;
    public static final float XENON_PENALTY_MINOR = 0.95f;
    public static final float XENON_PENALTY_NOTICEABLE = 0.85f;
    public static final float XENON_PENALTY_STRONG = 0.70f;

    public static final float AMBIENT_TEMP = 20.0f;
    public static final float SCRAM_TEMP = 1000.0f;
    public static final float MELTDOWN_TEMP = 1200.0f;

    public static final float MELTDOWN_EXPLOSION_RADIUS_STEAM = 4.0f;
    public static final float MELTDOWN_EXPLOSION_RADIUS_NUCLEAR = 8.0f;
    public static final int MELTDOWN_IRRADIATION_RADIUS = 15;
    public static final float IRRADIATION_DECAY_CHANCE = 0.0005f;
    public static final int IRRADIATION_EFFECT_RADIUS = 2;

    public static final float SCRAM_INSERTION_THRESHOLD_NO_RUSH = 0.80f;
    public static final float SCRAM_INSERTION_THRESHOLD_WEAK_RUSH = 0.40f;
    public static final float SCRAM_WEAK_TEMPERATURE_SPIKE = 10.0f;
    public static final float SCRAM_STRONG_TEMPERATURE_SPIKE = 35.0f;
    public static final int SCRAM_STRONG_COOLING_DELAY_TICKS = 80;

    // ========== ТОПЛИВО ==========
    public static final int MAX_FUEL_PER_ROD = 10000;
    public static final float FUEL_BURN_RATE_SEC = 1.0f;

    // ========== ЖИДКОСТИ ==========
    public static final int TANK_CAPACITY = 256000;

    // ========== ТУРБИНА (как fuel_type.normal в Create Diesel Generators) ==========
    public static final int TURBINE_TANK_CAPACITY = 4000;
    /** RPM при работе (diesel normal = 96) */
    public static final float TURBINE_MAX_RPM = 64.0f;
    /** Суммарная SU-мощность на полных оборотах (diesel = 6144) */
    public static final float TURBINE_STRENGTH = 4096.0f;
    /** mB пара за тик буфера: 1 mB каждые 1/burn_rate тиков (diesel = 0.05 → 20 тиков на 1 mB) */
    public static final float TURBINE_BURN_RATE = 0.05f;
    public static final float STRESS_PER_RPM = 256.0f;
    public static final float CONDENSER_EFFICIENCY = 0.9f;

    // ========== КОНДЕНСАТОР (после турбины / сброс избытка пара) ==========
    public static final int CONDENSER_TANK_CAPACITY = 32000;
    /** mB пара в tick — перекрывает типичный реактор (~170 mB/t при 10 каналах) */
    public static final int CONDENSER_MAX_STEAM_TICK = 400;
}
