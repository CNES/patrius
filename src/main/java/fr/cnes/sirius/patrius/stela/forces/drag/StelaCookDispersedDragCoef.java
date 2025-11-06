package fr.cnes.sirius.patrius.stela.forces.drag;

import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Class for drag coefficients using Cook formula with a multiplicative coefficient applied to the computed drag
 * coefficient.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaCookDispersedDragCoef extends StelaCookDragCoef {

    /** Serializable UID. */
    private static final long serialVersionUID = -7640221945824865029L;
    
    /** Coefficient applied to the drag coef. */
    private final double coef;

    // =============================== CONSTRUCTORS ================================ //

    /**
     * Constructor of a Cook dispersed drag coefficient with identity coefficient.
     */
    public StelaCookDispersedDragCoef() {
        this(1);
    }

    /**
     * Constructor of a Cook drag coefficient with a multiplicative coefficient.
     * 
     * @param coef
     *        multiplicative coefficient
     */
    public StelaCookDispersedDragCoef(final double coef) {
        this(coef, Constants.STELA_COOK_WALL_TEMPERATURE, Constants.STELA_COOK_ACCOMODATION);
    }

    /**
     * Constructor of a Cook drag coefficient with a multiplicative coefficient and with advanced parameters value for
     * wall temperature and accommodation.
     *
     * @param coef
     *        multiplicative coefficient
     * @param wallTemperature
     *        Cook wall temperature (K)
     * @param accommodation
     *        Cook accommodation constant
     */
    public StelaCookDispersedDragCoef(final double coef, final double wallTemperature, final double accommodation) {
        super(StelaDragCoefType.COOK_DISPERSED, wallTemperature, accommodation);
        this.coef = coef;
    }

    // =============================== METHODS ================================ //
    /** {@inheritDoc} */
    @Override
    public double getDragCoef(final StelaDragCoefInput stelaDragCoefInput) {
        return this.coef * super.getDragCoef(stelaDragCoefInput);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String CR = System.lineSeparator();
        return "Drag Coefficient Type : " + StelaDragCoefType.COOK_DISPERSED + CR + "Dispersion coef : " + this.coef
                + CR;
    }

    /**
     * Get statistical coefficient information.
     *
     * @return statistical coefficient information
     */
    public String getStatInformation() {
        return getStatInformation(this.coef);
    }

    /** {@inheritDoc} */
    @Override
    public StelaCookDispersedDragCoef copy() {
        return new StelaCookDispersedDragCoef(this.coef);
    }
}
