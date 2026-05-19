package io.github.blamp26.core86.content.reactor;

public enum ReactorState {
    /** Not assembled, or assembled but zero total fuel. */
    OFFLINE,
    /** Assembled, has fuel, power > 0. Actively producing. */
    OPERATING,
    /** All control rods at 100% insertion. Safe shutdown state.
     *  Fuel may still be present. */
    SCRAM
}
