package fr.cnes.sirius.patrius.stela.forces.drag;

import java.util.Map;

import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class for drag coefficients depending on space object's height,
 * with a multiplicative coefficient applied to the read drag coefficients.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableDispersedDragCoef extends StelaVariableDragCoef {

    /** Serializable UID. */
    private static final long serialVersionUID = -5716572525691734484L;

    /** Coefficient applied to the drag coef. */
    private final double coef;

    /**
     * Constructor with default coefficient = 1
     *
     * @param cdMap the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param rEq the Earth radius
     * @param f the Earth flattening
     */
    public StelaVariableDispersedDragCoef(final Map<Double, Double> cdMap, final double rEq, final double f) {
        this(cdMap, rEq, f, 1);
    }

    /**
     * Constructor for a Cd model depending on spacecraft altitude.
     *
     * @param cdMap the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param rEq the Earth radius
     * @param f the Earth flattening
     * @param coef the coefficient applied to the drag coef
     */
    public StelaVariableDispersedDragCoef(final Map<Double, Double> cdMap, final double rEq, final double f,
                                          final double coef) {
        this(cdMap, new GeodPosition(rEq, f), coef);
    }

    /**
     * Constructor for a Cd model depending on spacecraft altitude with a {@link GeodPosition}.
     *
     * @param cdMap the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param geodeticPosition the geodetic model
     * @param coef the coefficient applied to the drag coef
     */
    public StelaVariableDispersedDragCoef(final Map<Double, Double> cdMap, final GeodPosition geodeticPosition,
                                          final double coef) {
        super(StelaDragCoefType.VARIABLE_DISPERSED, cdMap, geodeticPosition);
        this.coef = coef;
    }

    /**
     * Compute the value of the Cd coefficient depending on spacecraft altitude and the dispersed coefficient.
     *
     * @param stelaDragCoefInput the input which gives the spacecraft position in the inertial frame
     *
     * @return the value of the drag coefficient
     *
     * @throws PatriusException
     *         if error while computing geodetic altitude
     */
    @Override
    public double getDragCoef(final StelaDragCoefInput stelaDragCoefInput) throws PatriusException {
        return this.coef * super.getDragCoef(stelaDragCoefInput);
    }

    /** {@inheritDoc} */
    @Override
    public StelaVariableDispersedDragCoef copy() {
        return new StelaVariableDispersedDragCoef(super.getCdMap(), super.getGeodPosition(), this.coef);
    }

    /**
     * Get the coefficient of the variable drag coef.
     *
     * @return the coefficient of the variable drag coef.
     */
    public double getCoef() {
        return this.coef;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {

        final String CR = System.lineSeparator();

        return "Drag Coefficient Type : " + this.stelaDragCoefType + CR + "Dispersion coef : " + this.coef + CR;
    }

    /**
     * Get statistical coefficient information.
     *
     * @return statistical coefficient information
     */
    public String getStatInformation() {
        return getStatInformation(this.coef);
    }

}
