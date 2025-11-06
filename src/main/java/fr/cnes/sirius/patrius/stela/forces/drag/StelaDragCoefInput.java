package fr.cnes.sirius.patrius.stela.forces.drag;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

import java.io.Serializable;

/**
 * Class for drag coefficients inputs.
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaDragCoefInput implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4121667986188240921L;

    /** Position vector (m). */
    private final Vector3D position;

    /** Relative velocity of the spacecraft with respect to the atmosphere (m/s). */
    private final double velocity;

    /** Temperature of the atmosphere (K). */
    private final double temperature;

    /** Mean molar mass of the atmosphere. */
    private final double molarMass;

    /**
     * Full constructor.
     * 
     * @param position position (m)
     * @param velocity relative velocity of the spacecraft with respect to the atmosphere (m/s)
     * @param temperature temperature of the atmosphere (K)
     * @param molarMass mean molar mass of the atmosphere
     */
    public StelaDragCoefInput(final Vector3D position, final double velocity, final double temperature,
                              final double molarMass) {
        this.position = position;
        this.velocity = velocity;
        this.temperature = temperature;
        this.molarMass = molarMass;
    }

    /**
     * Constructor for constant drag coef.
     */
    public StelaDragCoefInput() {
        this(Vector3D.ZERO, 0, 0, 0);
    }

    /**
     * Constructor for variable drag coef.
     * 
     * @param position altitude (m)
     */
    public StelaDragCoefInput(final Vector3D position) {
        this(position, 0, 0, 0);
    }

    /**
     * Constructor for Cook drag coefficient.
     * 
     * @param velocity relative velocity of the spacecraft with respect to the atmosphere (m/s)
     * @param temperature temperature of the atmosphere (K)
     * @param molarMass mean molar mass of the atmosphere
     */
    public StelaDragCoefInput(final double velocity, final double temperature, final double molarMass) {
        this(Vector3D.ZERO, velocity, temperature, molarMass);
    }

    /**
     * Getter for the position.
     * 
     * @return the position (m)
     */
    public Vector3D getPosition() {
        return this.position;
    }

    /**
     * Getter for the relative velocity of the spacecraft with respect to the atmosphere.
     * 
     * @return the relative velocity of the spacecraft with respect to the atmosphere (m/s)
     */
    public double getVelocity() {
        return this.velocity;
    }

    /**
     * Getter for temperature of the atmosphere.
     * 
     * @return the temperature of the atmosphere (K)
     */
    public double getTemperature() {
        return this.temperature;
    }

    /**
     * Getter for mean molar mass of the atmosphere.
     * 
     * @return the mean molar mass of the atmosphere
     */
    public double getMolarMass() {
        return this.molarMass;
    }
}
