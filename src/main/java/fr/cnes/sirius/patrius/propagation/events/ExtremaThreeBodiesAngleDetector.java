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
 * @history created 11/07/2012
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:300:22/04/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.util.Map;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Detects the maximal or minimal angle between three bodies is reached, the spacecraft eventually
 * being one of the bodies. If body<sub>1</sub>, body<sub>2</sub> and body<sub>3</sub> are the three
 * bodies, the detector computes the angle between the two vectors v<sub>21</sub> (vector from
 * body<sub>2</sub> to body<sub>1</sub>) and v<sub>23</sub> (vector from body<sub>2</sub> to
 * body<sub>3</sub>). The local minimum or maximum is chosen through a constructor parameter, with
 * values {@link ExtremaLatitudeDetector#MIN}, {@link ExtremaLatitudeDetector#MAX} and
 * {@link ExtremaLatitudeDetector#MIN_MAX} for both.
 * 
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation at minimum or/and
 * maximum angle between three bodies depending on extremum type defined. This can be changed by overriding one of the
 * following constructors :
 * </p>
 * <ul>
 * <li>
 * {@link #ExtremaThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, BodyOrder, int, double, 
 * double, fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaThreeBodiesAngleDetector} or
 * {@link #ExtremaThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, PVCoordinatesProvider, 
 * int, double, double, fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaThreeBodiesAngleDetector}: the defined action is performed at maximal OR/AND minimal angle depending on slope
 * selection defined.
 * <li>
 * {@link #ExtremaThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, BodyOrder, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaThreeBodiesAngleDetector} or
 * {@link #ExtremaThreeBodiesAngleDetector(PVCoordinatesProvider, PVCoordinatesProvider, BodyOrder, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaThreeBodiesAngleDetector} : the defined actions are performed at minimal AND maximal angle.
 * </ul>
 * <p>
 * A multi spacecraft propagation could be performed using the following constructors :
 * <ul>
 * <li>
 * {@link #ExtremaThreeBodiesAngleDetector(String, String, String, int, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaThreeBodiesAngleDetector}: the defined action is performed at maximal OR/AND minimal angle depending on slope
 * selection defined.
 * <li>
 * {@link #ExtremaThreeBodiesAngleDetector(String, String, String, double, double, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action, 
 * fr.cnes.sirius.patrius.propagation.events.EventDetector.Action)
 * ExtremaThreeBodiesAngleDetector} : the defined actions are performed at minimal AND maximal angle.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ExtremaThreeBodiesAngleDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class ExtremaThreeBodiesAngleDetector extends AbstractDetector implements MultiEventDetector {
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

    /** Flag for local minimum angle detection (g increasing). */
    public static final int MIN = 0;

    /** Flag for local maximum angle detection (g decreasing). */
    public static final int MAX = 1;

    /** Flag for both local minimum and maximum angle detection. */
    public static final int MIN_MAX = 2;

    /** Serializable UID. */
    private static final long serialVersionUID = 8713027255126860524L;

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

    /** Action performed at local minimum detection. */
    private final Action actionMIN;

    /** Action performed at local maximum detection. */
    private final Action actionMAX;

    /** True if detector should be removed at local minimum detection. */
    private final boolean removeMIN;

    /** True if detector should be removed at local maximum detection. */
    private final boolean removeMAX;

    /** True if detector should be removed (updated by eventOccured). */
    private boolean shouldBeRemovedFlag = false;

    /** True if g() method is called for the first time. */
    private boolean firstCall;

    /** Type of the propagation (mono or multi). */
    private final PropagationType type;

    /**
     * Constructor for the min and max three bodies angle detector.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extrema is reached.
     * </p>
     * 
     * @param firstBody the body "1"
     * @param secondBody the body "2"
     * @param thirdBody the body "3"
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final int extremumType) {
        this(firstBody, secondBody, thirdBody, extremumType, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the min and max three bodies angle detector.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extrema is reached.
     * </p>
     * 
     * @param firstBody the body "1"
     * @param secondBody the body "2"
     * @param thirdBody the body "3"
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final int extremumType, final double maxCheck, final double threshold) {
        this(firstBody, secondBody, thirdBody, extremumType, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action for both
     * minimum and maximum.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1"
     * @param secondBody the body "2"
     * @param thirdBody the body "3"
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final double maxCheck, final double threshold, final Action actionMin,
        final Action actionMax) {
        this(firstBody, secondBody, thirdBody, maxCheck, threshold, actionMin, actionMax, false,
            false);
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action for both
     * minimum and maximum.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1"
     * @param secondBody the body "2"
     * @param thirdBody the body "3"
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     * @param removeMin true if detector should be removed when the expected local minimum is
     *        reached
     * @param removeMax true if detector should be removed when the expected local maximum is
     *        reached
     * @since 3.1
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final double maxCheck, final double threshold, final Action actionMin,
        final Action actionMax, final boolean removeMin, final boolean removeMax) {
        super(MIN_MAX, maxCheck, threshold);

        this.body1 = firstBody;
        this.body2 = secondBody;
        this.body3 = thirdBody;

        this.actionMIN = actionMin;
        this.actionMAX = actionMax;
        this.removeMIN = removeMin;
        this.removeMAX = removeMax;
        this.firstCall = true;
        this.type = PropagationType.MONO;
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action when
     * extrema is detected.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1"
     * @param secondBody the body "2"
     * @param thirdBody the body "3"
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final int extremumType, final double maxCheck, final double threshold,
        final Action action) {
        this(firstBody, secondBody, thirdBody, extremumType, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action when
     * extrema is detected.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1"
     * @param secondBody the body "2"
     * @param thirdBody the body "3"
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     * @param remove true if detector should be removed when the expected extrema is reached.
     * 
     *        NB : If remove is true, it means detector should be removed at detection, so the value
     *        of attributes removeMIN and removeMAX must be decided according extremumType. Doing
     *        it, we ensure that detector will be removed well at propagation when calling method
     *        eventOccured (in which the value of attribute shouldBeRemoved is decided). In this
     *        case, users should better create an ExtremaThreeBodiesAngleDetector with constructor
     *        {@link ExtremaThreeBodiesAngleDetector#ExtremaThreeBodiesAngleDetector(PVCoordinatesProvider,
     *        PVCoordinatesProvider, PVCoordinatesProvider, double, double, EventDetector.Action,
     *        EventDetector.Action, boolean, boolean)}
     * @since 3.1
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider firstBody,
        final PVCoordinatesProvider secondBody, final PVCoordinatesProvider thirdBody,
        final int extremumType, final double maxCheck, final double threshold,
        final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);

        this.body1 = firstBody;
        this.body2 = secondBody;
        this.body3 = thirdBody;
        this.shouldBeRemovedFlag = remove;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (extremumType == MIN) {
            this.removeMIN = remove;
            this.removeMAX = false;
            this.actionMIN = action;
            this.actionMAX = null;
        } else if (extremumType == MAX) {
            this.removeMIN = false;
            this.removeMAX = remove;
            this.actionMIN = null;
            this.actionMAX = action;
        } else {
            this.removeMIN = remove;
            this.removeMAX = remove;
            this.actionMIN = action;
            this.actionMAX = action;
        }
        this.firstCall = true;
        this.type = PropagationType.MONO;
    }

    /**
     * <p>
     * Simple constructor for the min and max three bodies angle detector. The orbit of one of the three bodies is
     * implicitly the one which is propagated by the propagator associated to this event detector.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extrema is reached.
     * </p>
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise.
     * @param bodyB third body (if the body C is the first or second body), second body otherwise.
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector.
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection.
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final int extremumType) {
        this(bodyA, bodyB, bodyC, extremumType, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * <p>
     * Constructor for the min and max three bodies angle detector with defined maximum check and convergence threshold.
     * The orbit of one of the three bodies is implicitly the one which is propagated by the propagator associated to
     * this event detector.
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * extrema is reached.
     * </p>
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
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final int extremumType,
        final double maxCheck, final double threshold) {
        this(bodyA, bodyB, bodyC, extremumType, maxCheck, threshold, Action.STOP);
    }

    /**
     * <p>
     * Constructor for the min and max three bodies angle detector with specified action for both minimum and maximum.
     * The orbit of one of the three bodies is implicitly the one which is propagated by the propagator associated to
     * this event detector.
     * </p>
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise.
     * @param bodyB third body (if the body C is the first or second body), second body otherwise.
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param actionMin action to be perform when the expected local minimum is reached.
     * @param actionMax action to be perform when the expected local maximum is reached.
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final double maxCheck,
        final double threshold, final Action actionMin, final Action actionMax) {
        this(bodyA, bodyB, bodyC, maxCheck, threshold, actionMin, actionMax, false, false);
    }

    /**
     * <p>
     * Constructor for the min and max three bodies angle detector with specified action for both minimum and maximum.
     * The orbit of one of the three bodies is implicitly the one which is propagated by the propagator associated to
     * this event detector.
     * </p>
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise.
     * @param bodyB third body (if the body C is the first or second body), second body otherwise.
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param actionMin action to be perform when the expected local minimum is reached
     * @param actionMax action to be perform when the expected local maximum is reached
     * @param removeMin true if detector should be removed when the expected local minimum is
     *        reached
     * @param removeMax true if detector should be removed when the expected local maximum is
     *        reached
     * @since 3.1
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final double maxCheck,
        final double threshold, final Action actionMin, final Action actionMax,
        final boolean removeMin, final boolean removeMax) {
        super(MIN_MAX, maxCheck, threshold);

        this.body1 = this.body(bodyA, bodyB, bodyC, 1);
        this.body2 = this.body(bodyA, bodyB, bodyC, 2);
        this.body3 = this.body(bodyA, bodyB, bodyC, 3);

        this.actionMIN = actionMin;
        this.actionMAX = actionMax;
        this.removeMIN = removeMin;
        this.removeMAX = removeMax;
        this.firstCall = true;
        this.type = PropagationType.MONO;
    }

    /**
     * <p>
     * Constructor for the min and max three bodies angle detector with specified action when extrema is detected. The
     * orbit of one of the three bodies is implicitly the one which is propagated by the propagator associated to this
     * event detector.
     * </p>
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise.
     * @param bodyB third body (if the body C is the first or second body), second body otherwise.
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector.
     * @param extremumType the extremum type : {@link ExtremaThreeBodiesAngleDetector#MIN} for
     *        minimal angle detection, {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle
     *        detection or {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and
     *        maximal angle detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action to be perform when the expected local maximum is reached
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final int extremumType,
        final double maxCheck, final double threshold, final Action action) {
        this(bodyA, bodyB, bodyC, extremumType, maxCheck, threshold, action, false);
    }

    /**
     * <p>
     * Constructor for the min and max three bodies angle detector with specified action when extrema is detected. The
     * orbit of one of the three bodies is implicitly the one which is propagated by the propagator associated to this
     * event detector.
     * </p>
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
     * @param extremumType the extremum type : {@link ExtremaThreeBodiesAngleDetector#MIN} for
     *        minimal angle detection, {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle
     *        detection or {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and
     *        maximal angle detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action to be perform when the expected local maximum is reached
     * @param remove true if detector should be removed when the expected extrema is reached.
     * 
     *        NB : If remove is true, it means detector should be removed at detection, so the value
     *        of attributes removeMIN and removeMAX must be decided according extremumType. Doing
     *        it, we ensure that detector will be removed well at propagation when calling method
     *        eventOccured (in which the value of attribute shouldBeRemoved is decided). In this
     *        case, users should better create an ExtremaThreeBodiesAngleDetector with constructor
     *        {@link ExtremaThreeBodiesAngleDetector#ExtremaThreeBodiesAngleDetector(PVCoordinatesProvider,
     *        PVCoordinatesProvider, BodyOrder, double, double, EventDetector.Action, EventDetector.Action,
     *        boolean, boolean)}
     *        .
     */
    public ExtremaThreeBodiesAngleDetector(final PVCoordinatesProvider bodyA,
        final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final int extremumType,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);

        this.body1 = this.body(bodyA, bodyB, bodyC, 1);
        this.body2 = this.body(bodyA, bodyB, bodyC, 2);
        this.body3 = this.body(bodyA, bodyB, bodyC, 3);
        this.shouldBeRemovedFlag = remove;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (extremumType == MIN) {
            this.actionMIN = action;
            this.actionMAX = null;
            this.removeMIN = remove;
            this.removeMAX = false;
        } else if (extremumType == MAX) {
            this.actionMIN = null;
            this.actionMAX = action;
            this.removeMIN = false;
            this.removeMAX = remove;
        } else {
            this.actionMIN = action;
            this.actionMAX = action;
            this.removeMIN = remove;
            this.removeMAX = remove;
        }
        this.firstCall = true;
        this.type = PropagationType.MONO;
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action for both
     * minimum and maximum. Constructor to be used for multi spacecraft propagation only.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1" id
     * @param secondBody the body "2" id
     * @param thirdBody the body "3" id
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     */
    public ExtremaThreeBodiesAngleDetector(final String firstBody, final String secondBody,
        final String thirdBody, final double maxCheck, final double threshold,
        final Action actionMin, final Action actionMax) {
        this(firstBody, secondBody, thirdBody, maxCheck, threshold, actionMin, actionMax, false,
            false);
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action for both
     * minimum and maximum. Constructor to be used for multi-spacecraft propagation only.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1" id
     * @param secondBody the body "2" id
     * @param thirdBody the body "3" id
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param actionMin action to be performed when the expected local minimum is reached
     * @param actionMax action to be performed when the expected local maximum is reached
     * @param removeMin true if detector should be removed when the expected local minimum is
     *        reached.
     * @param removeMax true if detector should be removed when the expected local maximum is
     *        reached.
     */
    public ExtremaThreeBodiesAngleDetector(final String firstBody, final String secondBody,
        final String thirdBody, final double maxCheck, final double threshold,
        final Action actionMin, final Action actionMax, final boolean removeMin,
        final boolean removeMax) {
        super(MIN_MAX, maxCheck, threshold);

        this.inSpacecraftId1 = firstBody;
        this.inSpacecraftId2 = secondBody;
        this.inSpacecraftId3 = thirdBody;

        this.body1 = null;
        this.body2 = null;
        this.body3 = null;

        this.actionMIN = actionMin;
        this.actionMAX = actionMax;
        this.removeMIN = removeMin;
        this.removeMAX = removeMax;
        this.firstCall = true;
        this.type = PropagationType.MULTI;
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action when
     * extrema is detected.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1" id
     * @param secondBody the body "2" id
     * @param thirdBody the body "3" id
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     */
    public ExtremaThreeBodiesAngleDetector(final String firstBody, final String secondBody,
        final String thirdBody, final int extremumType, final double maxCheck,
        final double threshold, final Action action) {
        this(firstBody, secondBody, thirdBody, extremumType, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the min and max three bodies angle detector with specified action when
     * extrema is detected.
     * <p>
     * If v<sub>21</sub> is the vector from the second body to the first body and v<sub>23</sub> is the vector from the
     * second body to the third body, the detected angle will be the angle (in the range 0-&Pi;) between v<sub>21</sub>
     * and v<sub>23</sub>.
     * </p>
     * 
     * @param firstBody the body "1" id
     * @param secondBody the body "2" id
     * @param thirdBody the body "3" id
     * @param extremumType {@link ExtremaThreeBodiesAngleDetector#MIN} for minimal angle detection,
     *        {@link ExtremaThreeBodiesAngleDetector#MAX} for maximal angle detection or
     *        {@link ExtremaThreeBodiesAngleDetector#MIN_MAX} for both minimal and maximal angle
     *        detection
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold convergence threshold (see {@link AbstractDetector})
     * @param action action to be performed when the expected extrema is reached.
     * @param remove true if detector should be removed when the expected extrema is reached.
     * 
     *        NB : If remove is true, it means detector should be removed at detection, so the value
     *        of attributes removeMIN and removeMAX must be decided according extremumType. Doing
     *        it, we ensure that detector will be removed well at propagation when calling method
     *        eventOccured (in which the value of attribute shouldBeRemoved is decided). In this
     *        case, users should better create an ExtremaThreeBodiesAngleDetector with constructor
     *        {@link ExtremaThreeBodiesAngleDetector#ExtremaThreeBodiesAngleDetector(String, String,
     *        String, double, double, EventDetector.Action, EventDetector.Action, boolean, boolean)}
     */
    public ExtremaThreeBodiesAngleDetector(final String firstBody, final String secondBody,
        final String thirdBody, final int extremumType, final double maxCheck,
        final double threshold, final Action action, final boolean remove) {
        super(extremumType, maxCheck, threshold);

        this.inSpacecraftId1 = firstBody;
        this.inSpacecraftId2 = secondBody;
        this.inSpacecraftId3 = thirdBody;
        this.shouldBeRemovedFlag = remove;

        this.body1 = null;
        this.body2 = null;
        this.body3 = null;

        // If slopeSelection is different from 0, 1 or 2, an error has already been raised is
        // superclass.
        if (extremumType == MIN) {
            this.actionMIN = action;
            this.actionMAX = null;
            this.removeMIN = remove;
            this.removeMAX = false;
        } else if (extremumType == MAX) {
            this.actionMIN = null;
            this.actionMAX = action;
            this.removeMIN = false;
            this.removeMAX = remove;
        } else {
            this.actionMIN = action;
            this.actionMAX = action;
            this.removeMIN = remove;
            this.removeMAX = remove;
        }
        this.firstCall = true;
        this.type = PropagationType.MULTI;
    }

    /**
     * Private constructor to avoid code duplication
     * 
     * @param bodyA first body (if the body C is the second or third body), second body otherwise
     * @param bodyB third body (if the body C is the first or second body), second body otherwise
     * @param bodyC position within the constellation of the body whose orbit will be propagated by
     *        the propagator associated to this event detector
     * @param bodyNb first, second or thrid body
     * @return the expected body
     * 
     * @since 3.0
     */
    private PVCoordinatesProvider body(final PVCoordinatesProvider bodyA,
                                       final PVCoordinatesProvider bodyB, final BodyOrder bodyC, final int bodyNb) {
        // Initialize output body
        PVCoordinatesProvider body = null;
        switch (bodyC) {
        // first body
            case FIRST:
                if (bodyNb == 2) {
                    // Second body is bodyA
                    body = bodyA;
                } else if (bodyNb == 3) {
                    body = bodyB;
                } else {
                    body = null;
                }
                break;
            // second body
            case SECOND:
                if (bodyNb == 1) {
                    body = bodyA;
                } else if (bodyNb == 3) {
                    body = bodyB;
                } else {
                    body = null;
                }
                break;
            // third body
            case THIRD:
                if (bodyNb == 1) {
                    body = bodyA;
                } else if (bodyNb == 2) {
                    body = bodyB;
                } else {
                    body = null;
                }
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        // Return expected body
        return body;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle a min or max angle event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the angle is
     * reached.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected angle is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.eventOccurred(increasing, forward);
    }

    /** {@inheritDoc} */
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
        final PVCoordinates p1 =
            (this.body1 == null) ? s.getPVCoordinates() : this.body1.getPVCoordinates(s.getDate(),
                s.getFrame());
        final PVCoordinates p2 =
            (this.body2 == null) ? s.getPVCoordinates() : this.body2.getPVCoordinates(s.getDate(),
                s.getFrame());
        final PVCoordinates p3 =
            (this.body3 == null) ? s.getPVCoordinates() : this.body3.getPVCoordinates(s.getDate(),
                s.getFrame());

        return this.g(p1, p2, p3);
    }

    /**
     * Get 1st body
     * 
     * @return the 1st body
     */
    public PVCoordinatesProvider getFirstBody() {
        return this.body1;
    }

    /**
     * Get 2nd body
     * 
     * @return the 2nd body
     */
    public PVCoordinatesProvider getSecondBody() {
        return this.body2;
    }

    /**
     * Get 3rd body
     * 
     * @return the 3rd body
     */
    public PVCoordinatesProvider getThirdBody() {
        return this.body3;
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

    /** {@inheritDoc} */
    @Override
    public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
        // Nothing to do
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
        // Get PVCoordinates from map of states
        final PVCoordinates p1 = s.get(this.inSpacecraftId1).getPVCoordinates();
        final PVCoordinates p2 = s.get(this.inSpacecraftId2).getPVCoordinates();
        final PVCoordinates p3 = s.get(this.inSpacecraftId3).getPVCoordinates();
        return this.g(p1, p2, p3);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.eventOccurred(increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState>
            resetStates(final Map<String, SpacecraftState> oldStates)
                                                                     throws PatriusException {
        return oldStates;
    }

    /**
     * Private eventOccurred method both multi and mono event detection.
     * 
     * @precondition
     * 
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected angle is reached.
     * @since 3.0
     */
    private Action eventOccurred(final boolean increasing, final boolean forward) {
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
                // minimum distance case
                result = this.actionMIN;
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.removeMIN;
            } else {
                // maximum distance case
                result = this.actionMAX;
                // remove (or not) detector
                this.shouldBeRemovedFlag = this.removeMAX;
            }
        }
        return result;
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
        // computes the relative positions and velocities between points
        final Vector3D relativeVel1 = p1.getVelocity().subtract(p2.getVelocity());
        final Vector3D relativeVel3 = p3.getVelocity().subtract(p2.getVelocity());
        final Vector3D relativePos1 = p1.getPosition().subtract(p2.getPosition());
        final Vector3D relativePos3 = p3.getPosition().subtract(p2.getPosition());

        // constants for the derivative computation
        final double norm1 = relativePos1.getNorm();
        final double norm3 = relativePos3.getNorm();
        final double pos1Pos3 = Vector3D.dotProduct(relativePos1, relativePos3);
        final double pos1Pos1 = relativePos1.getNormSq();
        final double pos3Pos3 = relativePos3.getNormSq();
        final double pos1Vel1 = Vector3D.dotProduct(relativePos1, relativeVel1);
        final double pos3Vel3 = Vector3D.dotProduct(relativePos3, relativeVel3);
        final double pos1Vel3 = Vector3D.dotProduct(relativePos1, relativeVel3);
        final double pos3Vel1 = Vector3D.dotProduct(relativePos3, relativeVel1);

        // this double has the same sign as the angle derivative :
        // (this part of the derivative expression, the ignored factor part
        // having a constant negative sign)
        return -(MathLib.divide(pos1Vel3 + pos3Vel1, norm1 * norm3) - MathLib.divide(pos1Pos3,
            pos1Pos1 * pos3Pos3)
            * (pos1Vel1 * MathLib.divide(norm3, norm1) + pos3Vel3
                * MathLib.divide(norm1, norm3)));
    }

    /**
     * Get the first spacecraft id
     * 
     * @return the first spacecraft id
     */
    public String getInSpacecraftId1() {
        return this.inSpacecraftId1;
    }

    /**
     * Get the second spacecraft id
     * 
     * @return the second spacecraft id
     */
    public String getInSpacecraftId2() {
        return this.inSpacecraftId2;
    }

    /**
     * Get the third spacecraft id
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
        final EventDetector result;
        if (this.getSlopeSelection() == MIN_MAX) {
            result =
                new ExtremaThreeBodiesAngleDetector(this.body1, this.body2, this.body3, this.getMaxCheckInterval(),
                    this.getThreshold(), this.actionMIN, this.actionMAX, this.removeMIN, this.removeMAX);
        } else if (this.getSlopeSelection() == MIN) {
            result =
                new ExtremaThreeBodiesAngleDetector(this.body1, this.body2, this.body3, this.getSlopeSelection(),
                    this.getMaxCheckInterval(), this.getThreshold(), this.actionMIN, this.removeMIN);
        } else {
            result =
                new ExtremaThreeBodiesAngleDetector(this.body1, this.body2, this.body3, this.getSlopeSelection(),
                    this.getMaxCheckInterval(), this.getThreshold(), this.actionMAX, this.removeMAX);
        }
        return result;
    }
}
