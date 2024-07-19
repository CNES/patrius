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
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the dot product reaches either a local minimum or a local maximum.
 * <p>
 * The local minimum or maximum is chosen through a constructor parameter, with values
 * {@link ExtremaDotProductDetector#MIN}, {@link ExtremaDotProductDetector#MAX} and
 * {@link ExtremaDotProductDetector#MIN_MAX} for both.
 *
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at minimum or/and
 * maximum dot-product depending on extremum type defined.
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment attributes are mutable and related to propagation.
 *
 * @see EventDetector
 *
 * @author GMV
 */
public class ExtremaDotProductDetector extends AbstractDetector {

    /** Flag for local minimum distance detection (g increasing). */
    public static final int MIN = 0;

    /** Flag for local maximum distance detection (g decreasing). */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum distance detection. */
    public static final int MIN_MAX = 2;

    /** Serial UID. */
    private static final long serialVersionUID = -6420170265427361960L;

    /** Reference frame to project vectors (null to use satellite frame). */
    private final Frame projectionFrame;

    /** Target coordinates provider. */
    private final PVCoordinatesProvider target;

    /** First coordinates normalize . */
    private final boolean normalizePos;

    /** Second coordinates normalize. */
    private final boolean normalizeTarget;

    /** Action performed at local minimum detection. */
    private final Action actionMIN;

    /** Action performed at local maximum detection. */
    private final Action actionMAX;

    /** True if detector should be removed at minimum detection. */
    private final boolean removeMIN;

    /** True if detector should be removed at maximum detection. */
    private final boolean removeMAX;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor for a ExtremaDotProductDetector instance.
     *
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation
     * when the expected extrema is reached.
     * </p>
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param extremumType
     *        {@link ExtremaDotProductDetector#MIN} for lowest dotproduct detection,
     *        {@link ExtremaDotProductDetector#MAX} for highest dotproduct detection or
     *        {@link ExtremaDotProductDetector#MIN_MAX} for both lowest and highest dotproduct
     *        detection
     */
    public ExtremaDotProductDetector(final PVCoordinatesProvider target, final boolean normalizePos,
            final boolean normalizeTarget, final Frame projectionFrame, final int extremumType) {
        this(target, normalizePos, normalizeTarget, projectionFrame, extremumType, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a ExtremaDotProductDetector instance with complimentary parameters.
     *
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation
     * when the expected extrema is reached.
     * </p>
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param extremumType
     *        {@link ExtremaDotProductDetector#MIN} for lowest dotproduct detection,
     *        {@link ExtremaDotProductDetector#MAX} for highest dotproduct detection or
     *        {@link ExtremaDotProductDetector#MIN_MAX} for both lowest and highest dotproduct
     *        detection
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     */
    public ExtremaDotProductDetector(final PVCoordinatesProvider target, final boolean normalizePos,
            final boolean normalizeTarget, final Frame projectionFrame, final int extremumType, final double maxCheck,
            final double threshold) {
        this(target, normalizePos, normalizeTarget, projectionFrame, extremumType, maxCheck, threshold, Action.STOP,
                false);
    }

    /**
     * Constructor for a ExtremaDotProductDetector instance with specified action when extrema is
     * detected.
     *
     * @param target
     *        second coordinates provider.
     * @param normalizePos
     *        first coordinates normalize.
     * @param normalizeTarget
     *        second coordinates normalize.
     * @param projectionFrame
     *        frame with respect to which the vectors are projected
     * @param extremumType
     *        {@link ExtremaDotProductDetector#MIN} for lowest dotproduct detection,
     *        {@link ExtremaDotProductDetector#MAX} for highest dotproduct detection or
     *        {@link ExtremaDotProductDetector#MIN_MAX} for both lowest and highest dotproduct
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached
     * @param remove true if detector should be removed
     *
     *        NB : If remove is true, it means detector should be removed at detection, so the value
     *        of attributes removeMIN and removeMAX must be decided according extremumType. Doing
     *        it, we ensure that detector will be removed well at propagation when calling method
     *        eventOccured (in which the value of attribute shouldBeRemoved is decided). In this
     *        case, users should better create an ExtremaDistanceDectector with constructor
     *        {@link ExtremaDotProductDetector#ExtremaDotProductDetector(PVCoordinatesProvider, 
     *        double, double, EventDetector.Action, EventDetector.Action, boolean, boolean)}.
     */
    public ExtremaDotProductDetector(final PVCoordinatesProvider target, final boolean normalizePos,
            final boolean normalizeTarget, final Frame projectionFrame, final int extremumType, final double maxCheck,
            final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);
        // Final fields
        this.target = target;
        this.normalizePos = normalizePos;
        this.normalizeTarget = normalizeTarget;
        this.projectionFrame = projectionFrame;
        this.shouldBeRemovedFlag = remove;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        this.actionMIN = action;
        this.actionMAX = action;
        this.removeMIN = remove;
        this.removeMAX = remove;
    }

    /**
     * Handle an extrema dotproduct event and choose what to do next.
     *
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected extrema is reached.
     * @exception PatriusException if some specific error occurs.
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
        final Action result;
        if (this.getSlopeSelection() == 0) {
            result = this.actionMIN;
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.removeMIN;
        } else if (this.getSlopeSelection() == 1) {
            result = this.actionMAX;
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.removeMAX;
        } else {
            if (forward ^ !increasing) {
                // minimum case
                result = this.actionMIN;
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.removeMIN;
            } else {
                // maximum case
                result = this.actionMAX;
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.removeMAX;
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {

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

        // compute dot product rate
        final double dotProductRate;
        if (this.normalizePos && this.normalizeTarget) {
            // normalization np1 = pos1/pos1 ; np2 = pos2/pos2 ; dot = np1 * np2 => dP1 * np2 + np1
            // * dP2
            // dP1 = vel1/pos1 - np1 (pos1*vel1/pos1*pos1)
            // dP2 = vel2/pos2 - np2 (pos2*vel2/pos2*pos2)
            final double p1sq = pv1.getPosition().getNormSq();
            final Vector3D np1 = pv1.getPosition().normalize();
            final Vector3D dP1 = pv1.getVelocity().scalarMultiply(1. / MathLib.sqrt(p1sq))
                    .subtract(np1.scalarMultiply(pv1.getPosition().dotProduct(pv1.getVelocity()) / p1sq));

            final double p2sq = pv2.getPosition().getNormSq();
            final Vector3D np2 = pv2.getPosition().normalize();
            final Vector3D dP2 = pv2.getVelocity().scalarMultiply(1. / MathLib.sqrt(p2sq))
                    .subtract(np2.scalarMultiply(pv2.getPosition().dotProduct(pv2.getVelocity()) / p2sq));

            dotProductRate = dP1.dotProduct(np2) + np1.dotProduct(dP2);

        } else if (this.normalizePos) {
            // normalization np1 = pos1/pos1 ; dot = np1 * pos2 => dP1 * pos2 + np1 * vel2
            // dP1 = vel1/pos1 - np1 (pos1*vel1/pos1*pos1)
            final double p1sq = pv1.getPosition().getNormSq();
            final Vector3D np1 = pv1.getPosition().normalize();
            final Vector3D dP1 = pv1.getVelocity().scalarMultiply(1. / MathLib.sqrt(p1sq))
                    .subtract(np1.scalarMultiply(pv1.getPosition().dotProduct(pv1.getVelocity()) / p1sq));

            dotProductRate = dP1.dotProduct(pv2.getPosition()) + np1.dotProduct(pv2.getVelocity());

        } else if (this.normalizeTarget) {
            // normalization np2 = pos2/pos2 ; dot = pos1 * np2 => vel1 * np2 + pos1 * dP2
            // dP2 = vel2/pos2 - np2 (pos2*vel2/pos2*pos2)
            final double p2sq = pv2.getPosition().getNormSq();
            final Vector3D np2 = pv2.getPosition().normalize();
            final Vector3D dP2 = pv2.getVelocity().scalarMultiply(1. / MathLib.sqrt(p2sq))
                    .subtract(np2.scalarMultiply(pv2.getPosition().dotProduct(pv2.getVelocity()) / p2sq));

            dotProductRate = pv1.getVelocity().dotProduct(np2) + pv1.getPosition().dotProduct(dP2);

        } else {
            // no normalization dot = pos1 * pos2 => vel1 * pos2 + pos1 * vel2
            dotProductRate = pv1.getVelocity().dotProduct(pv2.getPosition())
                    + pv1.getPosition().dotProduct(pv2.getVelocity());
        }

        return dotProductRate;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * @return the body
     */
    public PVCoordinatesProvider getBody() {
        return this.target;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>body: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final Action action;
        final boolean remove;
        if (getSlopeSelection() == MIN) {
            action = this.actionMIN;
            remove = this.removeMIN;
        } else {
            action = this.actionMAX;
            remove = this.removeMAX;
        }
        return new ExtremaDotProductDetector(this.target, this.normalizePos, this.normalizeTarget,
                this.projectionFrame, getSlopeSelection(), getMaxCheckInterval(), getThreshold(), action, remove);
    }
}
