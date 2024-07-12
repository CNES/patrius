/**
 * HISTORY
 * VERSION:4.11:DM:DM-3268:22/05/2023:[PATRIUS] Creation d'une classe GeodeticTargetDirection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;

/**
 * This class extends the {@link GenericTargetDirection} to create a direction with a target which is a
 * {@link GeodeticPoint} (FIXME) on a {@link BodyShape}.
 * 
 * @author Pierre Préault
 */

public class BodyPointTargetDirection extends GenericTargetDirection {

    /**
     * Serializable UID.
     */
    private static final long serialVersionUID = 8950906884858285070L;

    /**
     * Simple constructor
     * 
     * @param body
     *        the body shape on which the target is defined
     * @param targetIn
     *        the GeodeticPoint (FIXME) target direction
     */
    public BodyPointTargetDirection(final BodyShape body, final GeodeticPoint targetIn) {
        // FIXME Rename GeodeticPoint in GeodeticCoordinates DM 3248
        // Initialization with super constructor
        super(new ConstantPVCoordinatesProvider(body.transform(targetIn), body.getBodyFrame()));
    }
}
