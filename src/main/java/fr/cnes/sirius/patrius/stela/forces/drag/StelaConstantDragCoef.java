package fr.cnes.sirius.patrius.stela.forces.drag;

import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Class defining constant drag coefficients. Whatever the value of space object's height is, drag coefficients from
 * this class take a constant value.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaConstantDragCoef extends AbstractStelaDragCoef {

    /** Serializable UID. */
    private static final long serialVersionUID = 1065993834260504077L;
    
    /** The spacecraft drag coefficient. */
    private final double dragCoef;

    /**
     * Constructor initializing drag coefficient's value to a value specified in a file.
     */
    public StelaConstantDragCoef() {
        this(Constants.STELA_CONSTANT_DRAG_COEFFICIENT);
    }

    /**
     * Simple constructor for a constant Cd.
     * 
     * @param dragCoef
     *        the constant value of the cd
     */
    public StelaConstantDragCoef(final double dragCoef) {
        super(StelaDragCoefType.CONSTANT);
        this.dragCoef = dragCoef;
    }

    /** {@inheritDoc} */
    @Override
    public double getDragCoef(final StelaDragCoefInput stelaDragCoefInput) {
        return this.dragCoef;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {

        final String CR = System.lineSeparator();

        return "Drag Coefficient Type : " + this.stelaDragCoefType + CR + "Constant Drag Coef : " + this.dragCoef;
    }

    /**
     * Get statistical drag coefficient information.
     *
     * @return statistical drag coefficient information
     */
    public String getStatInformation() {
        return getStatInformation(this.dragCoef);
    }

    /** {@inheritDoc} */
    @Override
    public StelaConstantDragCoef copy() {
        return new StelaConstantDragCoef(this.dragCoef);
    }

}
