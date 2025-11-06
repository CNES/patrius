package fr.cnes.sirius.patrius.stela.forces.solaractivity;

/**
 * Solar activity type enumeration
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public enum StelaSolarActivityType {
    /** Constant solar activity */
    CONSTANT,

    /** LOS constant solar activity */
    MEAN_CONSTANT,

    /** Variable solar activity */
    VARIABLE,

    /** Variable dispersed solar activity */
    VARIABLE_DISPERSED,

    /** Past cycles solar activity */
    RANDOM_CYCLES,

    /** 3 steps solar activity */
    MIXED_3DATE_RANGES
}
