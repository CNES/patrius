package fr.cnes.sirius.patrius.stela.forces.drag;

import java.util.Iterator;
import java.util.Map;

import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class for drag coefficients depending on space object's altitude.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableDragCoef extends AbstractStelaDragCoef {

    /** Serializable UID. */
    private static final long serialVersionUID = -3175398944372793161L;

    /** The spacecraft drag coefficient map. */
    private final Map<Double, Double> cdMap;

    /** The geodetic model. */
    private final GeodPosition geodPosition;

    /**
     * Constructor for a Cd model depending on spacecraft altitude.
     * 
     * @param cdMap the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param rEq the Earth radius
     * @param f the Earth flattening
     */
    public StelaVariableDragCoef(final Map<Double, Double> cdMap, final double rEq, final double f) {
        this(cdMap, new GeodPosition(rEq, f));
    }

    /**
     * Constructor for a Cd model depending on spacecraft altitude.
     *
     * @param cdMap the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param geodeticPosition the geodetic model
     */
    public StelaVariableDragCoef(final Map<Double, Double> cdMap, final GeodPosition geodeticPosition) {
        this(StelaDragCoefType.VARIABLE, cdMap, geodeticPosition);
    }

    /**
     * Protected constructor which allow a child different dragCoefType
     *
     * @param dragCoefType child class drag coef type
     * @param cdMap the map containing the Cd value with respect to the geodetic altitude of the spacecraft
     * @param geodeticPosition the geodetic model
     */
    protected StelaVariableDragCoef(final StelaDragCoefType dragCoefType, final Map<Double, Double> cdMap,
                                    final GeodPosition geodeticPosition) {
        super(dragCoefType);
        this.cdMap = cdMap;
        this.geodPosition = geodeticPosition;
    }

    /**
     * Get the spacecraft drag coefficient map
     *
     * @return The spacecraft drag coefficient map
     */
    public Map<Double, Double> getCdMap() {
        return this.cdMap;
    }

    /**
     * Get the geodetic model
     *
     * @return The geodetic model
     */
    public GeodPosition getGeodPosition() {
        return this.geodPosition;
    }

    /**
     * Compute the value of the Cd coefficient depending on spacecraft altitude.
     * 
     * @param stelaDragCoefInput
     *        the input which gives the spacecraft position in the inertial frame
     * @return the value of the drag coefficient
     * 
     * @throws PatriusException
     *         if error while computing geodetic altitude
     */
    @Override
    public double getDragCoef(final StelaDragCoefInput stelaDragCoefInput)
        throws PatriusException {
        final double result;
        // Ff Cd is variable
        // Get geodetic altitude (in Km) from geodetic model and spacecraft position in the inertial frame
        final double geodeticAlt =
                this.geodPosition.getGeodeticAltitude(stelaDragCoefInput.getPosition()) / Constants.KM_TO_M;
        final Iterator<Double> iterator = this.cdMap.keySet().iterator();
        double previous = iterator.next();
        // loop on spacecraft drag coefficient map
        while (iterator.hasNext()) {
            final double current = iterator.next();
            if (current >= geodeticAlt) {
                break;
            }
            previous = current;
        }
        result = this.cdMap.get(previous);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Drag Coefficient Type : VARIABLE";
    }

    /** {@inheritDoc} */
    @Override
    public StelaVariableDragCoef copy() {
        return new StelaVariableDragCoef(this.cdMap, this.geodPosition);
    }

}
