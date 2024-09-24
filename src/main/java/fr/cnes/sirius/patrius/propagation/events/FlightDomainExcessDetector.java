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
 * @history created 21/03/12
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * VERSION::DM:207:27/03/2014:Added type of AOL to detect as well as reference equator
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 *
 */
public final class FlightDomainExcessDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = -4001939786952545101L;

    /** Reference frame to compute flight domain. */
    private final Frame referenceFrame;
    /** Desired represented frame used for the flight domain Euler angles computation. */
    private Frame representedFrame;
    /** Represented frame of PATRIUS attitude (always satellite frame). */
    private Frame satelliteFrame;

    /** Threshold triggering the event. */
    private final RotationOrder order;

    /** Angular mid points */
    private final double[] angMidPoint;
    /** Angular widths */
    private final double[] angWidth;

    /** Action performed at excess detection when ascending. */
    private final Action actionAtAscending;

    /** Action performed at excess detection when descending. */
    private final Action actionAtDescending;

    /**
     * Constructor for an FlightDomainExcessDetector instance.
     *
     * @param order
     *            rotation order for euler sequence.
     * @param angMinMax
     *            angles thresholds [min,max][1,2,3].
     * @param refFrame
     *            reference frame with respect to which the angles are to be computed
     * @see PositionAngle
     */
    public FlightDomainExcessDetector(final RotationOrder order, final double[][] angMinMax, final Frame refFrame) {
        this(order, angMinMax, refFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an FlightDomainExcessDetector instance with complementary parameters.
     *
     * @param order
     *            rotation order for euler sequence.
     * @param angMinMax
     *            angles thresholds [min,max][1,2,3].
     * @param refFrame
     *            reference frame with respect to which the angles are to be computed
     * @param maxCheck
     *            maximum check (see {@link AbstractDetector})
     * @param threshold
     *            threshold (see {@link AbstractDetector})
     */
    public FlightDomainExcessDetector(final RotationOrder order, final double[][] angMinMax, final Frame refFrame,
            final double maxCheck, final double threshold) {
        this(order, angMinMax, refFrame, maxCheck, threshold, Action.CONTINUE, Action.STOP);
    }

    /**
     * Constructor for an FlightDomainExcessDetector instance with complementary parameters.
     *
     * @param order
     *            rotation order for euler sequence.
     * @param angMinMax
     *            angles thresholds [min,max][1,2,3].
     * @param refFrame
     *            reference frame with respect to which the angles are to be computed
     * @param maxCheck
     *            maximum check (see {@link AbstractDetector})
     * @param threshold
     *            threshold (see {@link AbstractDetector})
     * @param ascending
     *            action performed when ascending
     * @param descending
     *            action performed when descending
     */
    public FlightDomainExcessDetector(final RotationOrder order, final double[][] angMinMax, final Frame refFrame,
            final double maxCheck, final double threshold, final Action ascending, final Action descending) {
        super(maxCheck, threshold);

        this.actionAtAscending = ascending;
        this.actionAtDescending = descending;

        this.order = order;
        this.referenceFrame = refFrame;
        this.angMidPoint = new double[3];
        this.angWidth = new double[3];
        for (int i = 0; i < 3; i++) {
            this.angWidth[i] = (angMinMax[1][i] - angMinMax[0][i]) / 2;
            this.angMidPoint[i] = angMinMax[0][i] + this.angWidth[i];
            if (this.angWidth[i] < 0.) {
                if (i == 1) {
                    // user should not give min > max; we should likely throw !!!
                    this.angWidth[i] = -this.angWidth[i];
                } else {
                    // this is actually allowed, means go all the way around
                    this.angWidth[i] += FastMath.PI;
                    this.angMidPoint[i] = angMinMax[0][i] + this.angWidth[i];
                }
            }
        }
    }

    /**
     * Handle a flight domain event and choose what to do next.
     *
     * @param s
     *            the current state information : date, kinematics, attitude
     * @param increasing
     *            if true, the value of the switching function increases when times increases around event
     * @param forward
     *            if true, the integration variable (time) increases during integration.
     * @return the action performed when flight domain excess is reached.
     * @exception PatriusException
     *                if some specific error occurs
     */
    @Override
    public final Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        Action actionAtEvent = this.actionAtDescending;
        if (increasing) {
            actionAtEvent = this.actionAtAscending;
        }
        return actionAtEvent;
    }

    /**
     * Define the frame represented by the attitude law and the desired flight domain represented frame
     *
     * @param desiredRepFrame
     *            the desired represented frame of the flight domain
     * @param satAttFrame
     *            the satellite attitude law frame
     */
    public final void setAttitudeRepresentedFrame(final Frame desiredRepFrame, final Frame satAttFrame) {
        this.representedFrame = desiredRepFrame;
        this.satelliteFrame = satAttFrame;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public final double g(final SpacecraftState s) throws PatriusException {
        // Get attitude with respect to referenceFrame
        final Attitude attitude = s.getAttitude().withReferenceFrame(this.referenceFrame);
        Rotation rotation = attitude.getRotation();

        // Transform Satellite frame to represented frame , if necessary.
        // Note that PATRIUS does not manage explicitly the represented frame, always Satellite attitude frame
        if (this.satelliteFrame != this.representedFrame) {
            // Compute transform from representedFrame to satFrame
            final Transform transform = this.satelliteFrame.getTransformTo(this.representedFrame, s.getDate());

            // Compose rotation to express it in satFrame
            rotation = rotation.applyTo(transform.getRotation());
        }
        final double[] angles = rotation.getAngles(this.order);
        final double[] diff = new double[3];
        // angle 2 is 0,PI (if axis1==axis3) or -PI/2,PI/2 (if axis1!=axis3), angle1;3 are 0-2PI
        for (int i = 0; i < 3; i++) {
            diff[i] = MathLib.abs(angles[i] - this.angMidPoint[i]);
            if (i != 1) {
                diff[i] -= MathLib.floor(diff[i] / (2 * FastMath.PI)) * 2 * FastMath.PI;
                if (diff[i] >= FastMath.PI) {
                    diff[i] = 2 * FastMath.PI - diff[i];
                }
            }
            // positive if within threshold
            diff[i] = this.angWidth[i] - diff[i];
        }
        // if no violations, build positive number as smallest of all margins; else keep strongest violation
        return MathLib.min(diff[1], MathLib.min(diff[0], diff[2]));
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        final double[][] angMinMax = new double[2][3];

        for (int i = 0; i < 3; i++) {
            angMinMax[0][i] = angMidPoint[i] - angWidth[i];
            angMinMax[1][i] = 2 * angWidth[i] + angMinMax[0][i];
        }

        return new FlightDomainExcessDetector(order, angMinMax, referenceFrame, getMaxCheckInterval(), getThreshold(),
                actionAtAscending, actionAtDescending);
    }
}
