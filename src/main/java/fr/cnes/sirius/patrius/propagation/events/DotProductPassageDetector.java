/**
 *      _______. __  .______       __   __    __       _______.
 *     /       ||  | |   _  \     |  | |  |  |  |     /       |
 *    |   (----`|  | |  |_)  |    |  | |  |  |  |    |   (----`
 *     \   \    |  | |      /     |  | |  |  |  |     \   \
 * .----)   |   |  | |  |\  \----.|  | |  `--'  | .----)   |
 * |_______/    |__| | _| `._____||__|  \______/  |_______/
 *
 * license_which_one
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the dot product value between a spacecraft and a target PV coordinates reaches a
 * threshold value.
 *
 * @since 4.11
 */
public final class DotProductPassageDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = -4175714577269422597L;

    /** Threshold triggering the event. */
    private final double dotProductThreshold;

    /** Reference frame to project vectors (null to use satellite frame). */
    private final Frame projectionFrame;

    /** Target coordinates provider. */
    private final PVCoordinatesProvider target;

    /** First coordinates normalize . */
    private final boolean normalizePos;

    /** Second coordinates normalize. */
    private final boolean normalizeTarget;

    /** Action performed at excess detection when ascending. */
    private final Action actionAtAscending;

    /** Action performed at excess detection when descending. */
    private final Action actionAtDescending;

    /**
     * Constructor for an DotProductPassageDetector instance.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     */
    public DotProductPassageDetector(final PVCoordinatesProvider target, final boolean normalizePos,
            final boolean normalizeTarget, final double dotProduct, final Frame projectionFrame,
            final int slopeSelectionIn) {
        this(target, normalizePos, normalizeTarget, dotProduct, projectionFrame, slopeSelectionIn, DEFAULT_MAXCHECK,
                DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     */
    public DotProductPassageDetector(final PVCoordinatesProvider target, final boolean normalizePos,
            final boolean normalizeTarget, final double dotProduct, final Frame projectionFrame,
            final int slopeSelectionIn, final double maxCheck, final double threshold) {
        this(target, normalizePos, normalizeTarget, dotProduct, projectionFrame, slopeSelectionIn, maxCheck, threshold,
                Action.CONTINUE, Action.STOP);

    }

    /**
     * Constructor for an DotProductPassageDetector instance with complementary parameters.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param dotProduct
     *        threshold value of the dot product
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param slopeSelectionIn
     *        g-function slope selection (0, 1, or 2)
     * @param ascending
     *        action performed when ascending
     * @param descending
     *        action performed when descending
     */
    public DotProductPassageDetector(final PVCoordinatesProvider target, final boolean normalizePos,
            final boolean normalizeTarget, final double dotProduct, final Frame projectionFrame,
            final int slopeSelectionIn, final double maxCheck, final double threshold, final Action ascending,
            final Action descending) {
        super(slopeSelectionIn, maxCheck, threshold);
        // initialize values
        this.target = target;
        this.normalizePos = normalizePos;
        this.normalizeTarget = normalizeTarget;
        this.dotProductThreshold = dotProduct;
        this.projectionFrame = projectionFrame;

        this.actionAtAscending = ascending;
        this.actionAtDescending = descending;

    }

    /**
     * Handle an angular momentum excess event and choose what to do next.
     *
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, the value of the switching function increases when times increases around
     *        event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when angular momentum excess is reached.
     * @exception PatriusException
     *            if some specific error occurs
     */
    @Override
    public final Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
        Action outputAction = this.actionAtDescending;
        if (increasing) {
            outputAction = this.actionAtAscending;
        }
        return outputAction;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {

        // get reference coordinates
        final PVCoordinates pv1;
        // get target coordinates in same frame as reference
        final PVCoordinates pv2;
        if (this.projectionFrame == null) {
            // no frame explicitly given, use state frame
            pv1 = s.getPVCoordinates();
            pv2 = this.target.getPVCoordinates(s.getDate(), s.getFrame());
        } else {
            // use desired user frame
            pv1 = s.getPVCoordinates(this.projectionFrame);
            pv2 = this.target.getPVCoordinates(s.getDate(), this.projectionFrame);
        }

        // normalize reference
        final Vector3D v1;
        if (this.normalizePos) {
            v1 = pv1.getPosition().normalize();
        } else {
            v1 = pv1.getPosition();
        }
        // normalize target
        final Vector3D v2;
        if (this.normalizeTarget) {
            v2 = pv2.getPosition().normalize();
        } else {
            v2 = pv2.getPosition();
        }

        // actual dot product value
        final double dotProduct = v1.dotProduct(v2);

        // computes the difference between the actual dot product and the threshold value:
        return dotProduct - this.dotProductThreshold;

    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new DotProductPassageDetector(this.target, this.normalizePos, this.normalizeTarget,
                this.dotProductThreshold, this.projectionFrame, getSlopeSelection(), getMaxCheckInterval(),
                getThreshold(), this.actionAtAscending, this.actionAtDescending);
    }
}
