package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import java.io.Serializable;
import java.util.List;

/**
 * Data necessary for past cycles solar activity.
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaPastCyclesData implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4570781473001611482L;

    /** Solar activity first day of first cycle (stored only if using random past cycles. */
    private final int solarActivityFirstDay;

    /** Solar activity cycles list (stored only if using random past cycles). */
    private final List<Integer> solarActivityCycles;

    /** True if an additional cycle is appended before the first cycle. */
    private final boolean isAdditionalCycle;

    /**
     * Past cycles solar activity constructor.
     * 
     * @param solarActivityFirstDay
     *        solar activity first day of first cycle
     * @param solarActivityCycles
     *        solar activity cycles list
     * @param isAdditionalCycle
     *        true if a cycle is appended before the first cycle
     */
    public StelaPastCyclesData(final int solarActivityFirstDay, final List<Integer> solarActivityCycles,
                               final boolean isAdditionalCycle) {
        this.solarActivityFirstDay = solarActivityFirstDay;
        this.solarActivityCycles = solarActivityCycles;
        this.isAdditionalCycle = isAdditionalCycle;
    }

    /**
     * Getter for the solar activity cycles list
     * 
     * @return the solar activity cycles list
     */
    public List<Integer> getSolarActivityCycles() {
        return this.solarActivityCycles;
    }

    /**
     * Getter for the solar activity first day.
     * 
     * @return the solar activity first day
     */
    public int getSolarActivityFirstDay() {
        return this.solarActivityFirstDay;
    }

    /**
     * @return true if an additional cycle is appended at the beginning of the solar activity cycles' list
     */
    public boolean isAdditionalCycle() {
        return this.isAdditionalCycle;
    }
}
