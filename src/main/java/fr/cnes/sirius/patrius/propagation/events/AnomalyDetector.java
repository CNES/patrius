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
 * @history created 05/03/12
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the anomaly of the spacecraft reaches a predetermined value, &theta;.<br>
 * Anomaly is not defined for all kinds of orbits: this detector will detect anomaly events only if
 * the corresponding orbit is not a circular orbit, otherwise it may trigger events randomly.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the anomaly
 * &theta; is reached. This can be changed by using provided constructors.
 * </p>
 * This detector is unusable on a circular orbit where the perigee always moves very fast and in any
 * way. This detector detects only anomaly going in a growing way.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AnomalyDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class AnomalyDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = 1950385578566948721L;

    /** Anomaly triggering the event. */
    private final double anomaly;

    /** Anomaly type. */
    private final PositionAngle type;

    /** Action performed */
    private final Action actionAnomaly;

    /** True if detector should be removed. */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor for an AnomalyDetector instance.
     * 
     * @param angleType true, eccentric or mean anomaly.
     * @param angle anomaly value triggering the event.
     */
    public AnomalyDetector(final PositionAngle angleType, final double angle) {
        this(angleType, angle, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for an AnomalyDetector instance with complementary parameters.
     * <p>
     * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the anomaly is
     * reached.
     * </p>
     * 
     * @param angleType true, eccentric or mean anomaly.
     * @param angle anomaly value triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     */
    public AnomalyDetector(final PositionAngle angleType, final double angle,
        final double maxCheck, final double threshold) {
        this(angleType, angle, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for an AnomalyDetector instance with complementary parameters.
     * 
     * @param angleType true, eccentric or mean anomaly.
     * @param angle anomaly value triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at anomaly detection
     */
    public AnomalyDetector(final PositionAngle angleType, final double angle,
        final double maxCheck, final double threshold, final Action action) {
        this(angleType, angle, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for an AnomalyDetector instance with complementary parameters.
     * 
     * @param angleType true, eccentric or mean anomaly.
     * @param angle anomaly value triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at anomaly detection
     * @param remove if detector should be removed
     * @since 3.1
     */
    public AnomalyDetector(final PositionAngle angleType, final double angle,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        // the anomaly event is triggered when the g-function slope is positive at its zero:
        super(EventDetector.INCREASING, maxCheck, threshold);
        this.anomaly = angle;
        this.type = angleType;
        // action
        this.actionAnomaly = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle an anomaly event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the anomaly is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionAnomaly;
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
        final KeplerianOrbit orbit;
        if (s.getOrbit().getType().equals(OrbitType.KEPLERIAN)) {
            // Keplerian orbit:
            orbit = (KeplerianOrbit) s.getOrbit();

        } else {
            // non-Keplerian orbit, orbit conversion:
            orbit = new KeplerianOrbit(s.getOrbit());
        }
        if (orbit.getE() < Precision.DOUBLE_COMPARISON_EPSILON) {
            // circular orbit: anomaly is undefined
            return 0.;
        } else {
            // non-circular orbit:
            double current = 0;
            switch (this.type) {
                case MEAN:
                    current = orbit.getMeanAnomaly();
                    break;
                case ECCENTRIC:
                    current = orbit.getEccentricAnomaly();
                    break;
                case TRUE:
                    current = orbit.getTrueAnomaly();
                    break;
                default:
                    break;
            }
            // computes the sinus of the difference between the actual spacecraft anomaly and the
            // threshold value:
            return MathLib.sin(current - this.anomaly);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Get the anomaly to detect.
     * 
     * @return the anomaly triggering the event.
     */
    public double getAnomaly() {
        return this.anomaly;
    }

    /**
     * Get the type of anomaly to detect.
     * 
     * @return the anomaly type
     */
    public PositionAngle getAnomalyType() {
        return this.type;
    }

    /**
     * Return the action at detection.
     * 
     * @return action at detection
     */
    public Action getAction() {
        return this.actionAnomaly;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        return new AnomalyDetector(this.type, this.anomaly, this.getMaxCheckInterval(), this.getThreshold(),
            this.actionAnomaly, this.shouldBeRemovedFlag);
    }
}
