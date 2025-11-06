package fr.cnes.sirius.patrius.stela.forces.drag;

import fr.cnes.sirius.patrius.math.special.Erf;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Class for drag coefficients using Cook formula.
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaCookDragCoef extends AbstractStelaDragCoef {

    /** Serializable UID. */
    private static final long serialVersionUID = -6837030015371381651L;
    
    /** Cook wall temperature (K). */
    private final double wallTemperature;
    
    /** Cook accommodation constant. */
    private final double accommodation;

    /**
     * Constructor.
     * 
     * @param wallTemperature Cook wall temperature (K)
     * @param accommodation Cook accommodation constant
     */
    public StelaCookDragCoef(final double wallTemperature, final double accommodation) {
        this(StelaDragCoefType.COOK, wallTemperature, accommodation);
    }

    /**
     * Protected constructor which allow a child different dragCoefType.
     *
     * @param dragCoefType child class drag coef type
     * @param wallTemperature Cook wall temperature (K)
     * @param accommodation Cook accommodation constant
     */
    protected StelaCookDragCoef(final StelaDragCoefType dragCoefType, final double wallTemperature,
                                final double accommodation) {
        super(dragCoefType);
        this.wallTemperature = wallTemperature;
        this.accommodation = accommodation;
    }

    /** {@inheritDoc} */
    @Override
    public double getDragCoef(final StelaDragCoefInput stelaDragCoefInput) {

        // Temporary variables
        final double sqrtPi = MathLib.sqrt(MathLib.PI);
        final double ratioT = 2. * MathLib.sqrt(this.wallTemperature / stelaDragCoefInput.getTemperature());
        final double vm =
            MathLib.sqrt(
                2. * Constants.STELA_COOK_GAZ_CONSTANT / stelaDragCoefInput.getMolarMass()
                        * stelaDragCoefInput.getTemperature());
        final double s = stelaDragCoefInput.getVelocity() / vm;
        final double s2 = s * s;
        final double s3 = s2 * s;
        final double s4 = s2 * s2;
        double mu = stelaDragCoefInput.getMolarMass() / Constants.STELA_COOK_MOLAR_MASS_OXYGEN;
        // Bound mu as described by the algorithm
        mu = MathLib.min(mu, 1.);
        double alpha = this.accommodation * mu / ((1. + mu) * (1. + mu));
        // Bound alpha because of next square root
        alpha = MathLib.min(alpha, 1.);
        final double erfs = Erf.erf(s);

        // Absorption coefficient computation
        final double cda = 2. * (1. + 1. / s2 - 1. / (4. * s4)) * erfs
                + (2. * s2 + 1.) * MathLib.exp(-s2) / (sqrtPi * s3);

        // Re-emission coefficient computation
        final double temp = erfs + (1. + (2. * s2 - 1.) * MathLib.exp(-s2)) / (2. * sqrtPi * s3);
        final double cdr =
            sqrtPi / (3. * s) * (ratioT + MathLib.sqrt(1. - alpha) * (s + 1. - ratioT + (s - 1.) * temp));

        // Total drag coefficient
        return cda + cdr;
    }

    /** {@inheritDoc} */
    @Override
    public StelaCookDragCoef copy() {
        return new StelaCookDragCoef(this.wallTemperature, this.accommodation);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Drag Coefficient Type : " + this.stelaDragCoefType;
    }

}
