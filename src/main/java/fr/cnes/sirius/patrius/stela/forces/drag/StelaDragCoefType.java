package fr.cnes.sirius.patrius.stela.forces.drag;

/**
 * Drag coefficient types. They can be either constant or variable.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public enum StelaDragCoefType {
    /**
     * Constant drag coefficient type.
     */
    CONSTANT,
    /**
     * Variable drag coefficient type.
     */
    VARIABLE,
    /**
     * Variable dispersed drag coefficient type.
     */
    VARIABLE_DISPERSED,
    /**
     * Cook drag coefficient type.
     */
    COOK,
    /**
     * Cook dispersed drag coefficient type.
     */
    COOK_DISPERSED
}
