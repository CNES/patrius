package fr.cnes.sirius.patrius.stela.forces.atmospheres;

import java.io.Serializable;

/**
 * Atmospheric model output specialized for the {@link Jacchia77} model.
 * 
 * @concurrency thread-safe (immutable)
 * 
 * @author Emmanuel Bignon , Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-390:25/04/2025:[STELA-PATRIUS] Modeles d'atmosphere additionnels
 * END-HISTORY
 * @since 4.16
 */
public class JacchiaOutput implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -4669486687155826088L;

    /** Atmospheric density (kg/m3). */
    private final double density;

    /** Atmospheric temperature (K). */
    private final double temperature;

    /** Atmospheric mean molar mass (kg/mol). */
    private final double meanMolarMass;

    /**
     * Constructor.
     * 
     * @param density
     *        atmospheric density (kg/m3)
     * @param temperature
     *        atmospheric temperature (K)
     * @param meanMolarMass
     *        atmospheric mean molar mass (kg/mol)
     */
    public JacchiaOutput(final double density, final double temperature, final double meanMolarMass) {
        this.density = density;
        this.temperature = temperature;
        this.meanMolarMass = meanMolarMass;
    }

    /**
     * Getter for the atmospheric density.
     * 
     * @return the atmospheric density (kg/m3)
     */
    public double getDensity() {
        return this.density;
    }

    /**
     * Getter for the atmospheric temperature.
     * 
     * @return the atmospheric temperature (K)
     */
    public double getTemperature() {
        return this.temperature;
    }

    /**
     * Getter for the atmospheric mean molar mass.
     * 
     * @return the atmospheric mean molar mass (kg/mol)
     */
    public double getMeanMolarMass() {
        return this.meanMolarMass;
    }
}
