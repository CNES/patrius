/**
 * 
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history created 06/03/12
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:300:18/03/2015: Creation multi propagator
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.Map;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the angle between three bodies is equal to a predetermined value.
 * <p>
 * If body<sub>1</sub>, body<sub>2</sub> and body<sub>3</sub> are the three bodies, the detector computes the angle
 * between the two vectors v<sub>21</sub> (vector from body<sub>2</sub> to body<sub>1</sub>) and v<sub>23</sub> (vector
 * from body<sub>2</sub> to body<sub>3</sub>), and compare it with the threshold angle value.
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the angle between
 * the three bodies is reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: ThreeBodiesAngleDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
@SuppressWarnings("PMD.NullAssignment")
public class ThreeBodiesAngleDetector extends AbstractDetector implements MultiEventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -5302659256921813170L;

    /** the main spacecraft id */
    private String inSpacecraftId1;

    /** the secondary spacecraft id */
    private String inSpacecraftId2;

    /** the third spacecraft id */
    private String inSpacecraftId3;

    /** First body. */
    private final PVCoordinatesProvider body1;

    /** Second body. */
    private final PVCoordinatesProvider body2;

    /** Third body. */
    private final PVCoordinatesProvider body3;

    /** Alignment angle (rad). */
    private final double alignAngle;

    /** Action performed */
    private final Action actionThreeBodiesAngle;

    /** True if detector should be removed */
    private boolean shouldBeRemovedFlag = false;

    /** True if g() method is called for the first time. */
    private boolean firstCall;

    /** Type of the propagation (mono or multi). */
    private final PropagationType type;

    /**
     * Propagation type.
     * 
     * @since 3.0
     */
    private static enum PropagationType {
        /** Mono sat propagation */
        MONO,
        /** Multi sat propagation */
        MULTI;
    }

    /**
     * Build a new three bodies angle detector. Constructor to be used for single spacecraft
     * propagation only ( {@link Propagator}).
     * 
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the first body
     * @param secondBody the second body
     * @param thirdBody the third body
     * @param angle angle in rad triggering the event.
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final double angle) {
        this(firstBody, secondBody, thirdBody, angle, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new three bodies angle detector with complimentary parameters. Constructor to be used
     * for single spacecraft propagation only ({@link Propagator}).
     * 
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the angle is
     * reached.
     * </p>
     * 
     * @param firstBody the first body
     * @param secondBody the second body
     * @param thirdBody the third body
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final double angle, final double maxCheck, final double threshold) {
        this(firstBody, secondBody, thirdBody, angle, maxCheck, threshold, Action.STOP);
    }

    /**
     * Build a new three bodies angle detector with complimentary parameters. Constructor to be used
     * for single spacecraft propagation only ({@link Propagator}).
     * 
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the first body
     * @param secondBody the second body
     * @param thirdBody the third body
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action performed at angle detection
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final double angle, final double maxCheck, final double threshold, final Action action) {
        this(null, firstBody, null, secondBody, null, thirdBody, angle, maxCheck, threshold,
            action, false, PropagationType.MONO);
    }

    /**
     * Build a new three bodies angle detector with a position among the parameters. Constructor to
     * be used for single spacecraft propagation only ({@link Propagator}). The orbit of one of the
     * three bodies is implicitly the one which is propagated by the propagator associated to this
     * event detector.
     * 
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise
     * @param bodyB third body (if the body C is the first or second body), second body otherwise
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector
     * @param angle angle in rad triggering the event.
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final double angle) {
        this(bodyA, bodyB, bodyC, angle, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Build a new three bodies angle detector with complimentary parameters. Constructor to be used
     * for single spacecraft propagation only ({@link Propagator}). The orbit of one of the three
     * bodies is implicitly the one which is propagated by the propagator associated to this event
     * detector.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the angle is
     * reached.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise
     * @param bodyB third body (if the body C is the first or second body), second body otherwise
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final double angle,
        final double maxCheck, final double threshold) {
        this(bodyA, bodyB, bodyC, angle, maxCheck, threshold, Action.STOP);
    }

    /**
     * Build a new three bodies angle detector with complimentary parameters. Constructor to be used
     * for single spacecraft propagation only ({@link Propagator}). The orbit of one of the three
     * bodies is implicitly the one which is propagated by the propagator associated to this event
     * detector.
     * <p>
     * 
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise
     * @param bodyB third body (if the body C is the first or second body), second body otherwise
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * 
     * @param action action performed at angle detection
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final double angle,
        final double maxCheck, final double threshold, final Action action) {
        this(bodyA, bodyB, bodyC, angle, maxCheck, threshold, action, false);
    }

    /**
     * Build a new three bodies angle detector with complimentary parameters. Constructor to be used
     * for single spacecraft propagation only ({@link Propagator}). The orbit of one of the three
     * bodies is implicitly the one which is propagated by the propagator associated to this event
     * detector.
     * <p>
     * 
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise
     * @param bodyB third body (if the body C is the first or second body), second body otherwise
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * 
     * @param action action performed at angle detection
     * @param remove true if detector should be removed at angle detection
     * @since 3.1
     */
    public ThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final double angle,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(maxCheck, threshold);

        switch (bodyC) {
            case FIRST:
                this.body1 = null;
                this.body2 = bodyA;
                this.body3 = bodyB;
                break;
            case SECOND:
                this.body1 = bodyA;
                this.body2 = null;
                this.body3 = bodyB;
                break;
            default:
                this.body1 = bodyA;
                this.body2 = bodyB;
                this.body3 = null;
                break;
        }
        // set the angle input value in the 0-PI interval:
        this.alignAngle = angle - MathLib.floor(angle / FastMath.PI) * FastMath.PI;
        this.type = PropagationType.MONO;
        this.firstCall = true;
        // action
        this.actionThreeBodiesAngle = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Build a new three bodies angle detector. Constructor to be used for multi spacecraft
     * propagation only.
     * 
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstId first body (if the body C is the second or third body), second body otherwise
     * @param secondId third body (if the body C is the first or second body), second body otherwise
     * @param thirdId position within the constellation of the body whose orbit will be propagated
     *        by the propagator associated to this event detector
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action performed at angle detection
     */
    public ThreeBodiesAngleDetector(final String firstId, final String secondId,
        final String thirdId, final double angle, final double maxCheck,
        final double threshold, final Action action) {
        this(firstId, null, secondId, null, thirdId, null, angle, maxCheck, threshold, action,
            false, PropagationType.MULTI);
    }

    /**
     * Private constructor.
     * 
     * @param firstId the first state id
     * @param secondId the second state id
     * @param thirdId the third state id
     * @param firstBody the first body
     * @param secondBody the second body
     * @param thirdBody the third body
     * @param angle angle in rad triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action performed at angle detection
     * @param remove true if detector should be removed at angle detection
     * @param propagationType propagation type (mono or multi)
     * @since 3.1
     */
    private ThreeBodiesAngleDetector(final String firstId, final PVCoordinatesProvider firstBody,
        final String secondId, final PVCoordinatesProvider secondBody, final String thirdId,
        final PVCoordinatesProvider thirdBody, final double angle, final double maxCheck,
        final double threshold, final Action action, final boolean remove,
        final PropagationType propagationType) {
        super(maxCheck, threshold);
        this.inSpacecraftId1 = firstId;
        this.inSpacecraftId2 = secondId;
        this.inSpacecraftId3 = thirdId;
        this.body1 = firstBody;
        this.body2 = secondBody;
        this.body3 = thirdBody;
        // set the angle input value in the 0-PI interval:
        this.alignAngle = angle - MathLib.floor(angle / FastMath.PI) * FastMath.PI;
        this.type = propagationType;
        this.firstCall = true;
        // action
        this.actionThreeBodiesAngle = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle an angle event and choose what to do next.
     * 
     * @param s the current state information
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration
     * @return the action performed when the three bodies angle is detected
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionThreeBodiesAngle;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /**
     * Compute the value of the switching function.
     * 
     * This function measures the difference between the angle between the vector v<sub>21</sub> and
     * the vector v<sub>23</sub> and the given angle.
     * 
     * @param s the current state information
     * 
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        if (this.firstCall) {
            // Check if the constructor for multi propagation was defined
            if (this.type == PropagationType.MULTI) {
                throw new PatriusException(PatriusMessages.MONO_MULTI_DETECTOR);
            }
            this.firstCall = false;
        }

        // computes the positions of the three bodies is a common frame:
        final PVCoordinates p1 = (this.body1 == null) ? s.getPVCoordinates() : this.body1.getPVCoordinates(
            s.getDate(), s.getFrame());
        final PVCoordinates p2 = (this.body2 == null) ? s.getPVCoordinates() : this.body2.getPVCoordinates(
            s.getDate(), s.getFrame());
        final PVCoordinates p3 = (this.body3 == null) ? s.getPVCoordinates() : this.body3.getPVCoordinates(
            s.getDate(), s.getFrame());
        return this.g(p1, p2, p3);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Bodies order type.
     */
    public enum BodyOrder {
        /**
         * The first body is the body used during propagation.
         */
        FIRST,

        /**
         * The second body is the body used during propagation.
         */
        SECOND,

        /**
         * The third body is the body used during propagation.
         */
        THIRD
    }

    /**
     * Get 1st body.
     * 
     * @return the 1st body
     */
    public PVCoordinatesProvider getFirstBody() {
        return this.body1;
    }

    /**
     * Get 2nd body.
     * 
     * @return the 2nd body
     */
    public PVCoordinatesProvider getSecondBody() {
        return this.body2;
    }

    /**
     * Get 3rd body.
     * 
     * @return the 3rd body
     */
    public PVCoordinatesProvider getThirdBody() {
        return this.body3;
    }

    /**
     * Get the alignment angle.
     * 
     * @return the Alignment angle (rad).
     */
    public double getAlignmentAngle() {
        return this.alignAngle;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
        // Does nothing
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final Map<String, SpacecraftState> s) throws PatriusException {
        if (this.firstCall) {
            // Check if the constructor for single propagation was defined
            if (this.type == PropagationType.MONO) {
                throw new PatriusException(PatriusMessages.MONO_MULTI_DETECTOR);
            }
            this.firstCall = false;
        }

        final PVCoordinates p1 = s.get(this.inSpacecraftId1).getPVCoordinates();
        final PVCoordinates p2 = s.get(this.inSpacecraftId2).getPVCoordinates();
        final PVCoordinates p3 = s.get(this.inSpacecraftId3).getPVCoordinates();
        return this.g(p1, p2, p3);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionThreeBodiesAngle;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState>
            resetStates(final Map<String, SpacecraftState> oldStates)
                                                                     throws PatriusException {
        return oldStates;
    }

    /**
     * Private g() method for both multi and mono event detection.
     * 
     * @param p1 main PVCoordinates
     * @param p2 secondary PVCoordinates
     * @param p3 third PVCoordinates
     * @return value of the switching function
     * @throws PatriusException if some specific error occurs
     * 
     * @since 3.0
     */
    @SuppressWarnings("PMD.ShortMethodName")
    private double g(final PVCoordinates p1, final PVCoordinates p2, final PVCoordinates p3)
                                                                                            throws PatriusException {
        // computes the vectors between the bodies positions:
        final Vector3D p2p1 = p1.getPosition().subtract(p2.getPosition());
        final Vector3D p2p3 = p3.getPosition().subtract(p2.getPosition());
        // return the difference between the two angles:
        return Vector3D.angle(p2p1, p2p3) - this.alignAngle;
    }

    /**
     * Get the first spacecraft id.
     * 
     * @return the first spacecraft id
     */
    public String getInSpacecraftId1() {
        return this.inSpacecraftId1;
    }

    /**
     * Get the second spacecraft id.
     * 
     * @return the second spacecraft id
     */
    public String getInSpacecraftId2() {
        return this.inSpacecraftId2;
    }

    /**
     * Get the third spacecraft id.
     * 
     * @return the third spacecraft id
     */
    public String getInSpacecraftId3() {
        return this.inSpacecraftId3;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>body1: {@link PVCoordinatesProvider}</li>
     * <li>body2: {@link PVCoordinatesProvider}</li>
     * <li>body3: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new ThreeBodiesAngleDetector(this.inSpacecraftId1, this.body1, this.inSpacecraftId2, this.body2,
            this.inSpacecraftId3, this.body3, this.alignAngle, this.getMaxCheckInterval(), this.getThreshold(),
            this.actionThreeBodiesAngle, this.shouldBeRemovedFlag, this.type);
    }
}
