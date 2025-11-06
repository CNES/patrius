package fr.cnes.sirius.patrius.stela.forces.gravity;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This abstract class represents the zonal harmonics.
 * 
 * @concurrency immutable
 * 
 * @author Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-391:25/04/2025:[STELA-PATRIUS] Implementation zonaux par recurrence
 * END-HISTORY
 * @since 4.16
 */
public abstract class AbstractStelaZonalAttraction extends AbstractStelaLagrangeContribution {

    /** Serializable UID. */
    private static final long serialVersionUID = 7719614002926569028L;

    /** Degree of development for zonal perturbations. */
    protected final int zonalDegreeMaxPerturbation;

    /** Indicate if J2 should be computed or not. */
    protected final boolean isJ2SquareComputed;

    /**
     * Constructor.
     * 
     * @param zonalDegreeMaxPerturbation
     *        degree of development for zonal perturbations
     * @param isJ2SquareComputed
     *        {@code true} if J2² is computed, {@code false} otherwise
     * @throws NotPositiveException
     *         if {@code zonalDegreeMaxPerturbation < 0}
     */
    public AbstractStelaZonalAttraction(final int zonalDegreeMaxPerturbation, final boolean isJ2SquareComputed) {
        super();

        // Check the degree of development for zonal perturbations
        if (zonalDegreeMaxPerturbation < 0) {
            throw new NotPositiveException(zonalDegreeMaxPerturbation);
        }

        this.zonalDegreeMaxPerturbation = zonalDegreeMaxPerturbation;
        this.isJ2SquareComputed = isJ2SquareComputed;
    }

    /**
     * Getter for the degree of development for zonal perturbations.
     * 
     * @return the degree of development for zonal perturbations
     */
    public int getZonalDegreeMaxPerturbation() {
        return this.zonalDegreeMaxPerturbation;
    }

    /**
     * Indicate if J2 should be computed or not.
     * 
     * @return {@code true} if J2² is computed, {@code false} otherwise
     */
    public boolean isJ2SquareComputed() {
        return this.isJ2SquareComputed;
    }

    /**
     * Compute the effect of the J2² of the Zonal Perturbation.
     * 
     * @param orbit
     *        an orbit
     * @return the J2² perturbation
     * @throws PatriusException
     *         if the provider doesn't support 2nd degree
     */
    public abstract double[] computeJ2Square(final StelaEquinoctialOrbit orbit) throws PatriusException;
}
