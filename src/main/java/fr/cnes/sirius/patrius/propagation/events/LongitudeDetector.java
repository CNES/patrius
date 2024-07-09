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
 * @history created 10/07/12
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the spacecraft reaches a given local longitude.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the longitude is
 * reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see EventDetector
 * 
 * @author chabaudp
 * 
 * @version $Id: LongitudeDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 */
public class LongitudeDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = -8263650183133893663L;

    /** longitude to detect */
    private final double longToDetect;

    /** the frame of the central body */
    private final Frame centralBodyFrame;

    /** last g() call longitude difference to the reference to detect */
    private double lastLongitudeDiff;

    /** evolution way of the longitude difference to the reference to detect */
    private int way;

    /** Action performed */
    private final Action actionLong;

    /** True if detector should be removed */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached.
     * </p>
     * 
     * @param longitudeToDetect the longitude to detect
     * @param bodyFrame the frame of the central body (take care to use a rotative frame to get the
     *        right longitude)
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame) {
        this(longitudeToDetect, bodyFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached.
     * </p>
     * 
     * @param longitudeToDetect the longitude to detect
     * @param bodyFrame the frame of the central body (take care to use a rotative frame to get the
     *        right longitude)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame,
        final double maxCheck, final double threshold) {
        this(longitudeToDetect, bodyFrame, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached.
     * </p>
     * 
     * @param longitudeToDetect the longitude to detect
     * @param bodyFrame the frame of the central body (take care to use a rotative frame to get the
     *        right longitude)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at longitude detection
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action) {
        this(longitudeToDetect, bodyFrame, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached.
     * </p>
     * 
     * @param longitudeToDetect the longitude to detect
     * @param bodyFrame the frame of the central body (take care to use a rotative frame to get the
     *        right longitude)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at longitude detection
     * @param remove true if detector should be removed
     * @since 3.1
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(EventDetector.INCREASING_DECREASING, maxCheck, threshold);
        this.longToDetect = MathUtils.normalizeAngle(longitudeToDetect, 0.0);
        this.centralBodyFrame = bodyFrame;
        this.way = 1;
        this.lastLongitudeDiff = 0.0;

        // action
        this.actionLong = action;

        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /**
     * Handle a longitude reaching event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude.
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected longitude is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionLong;
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

        // spacecraft position in central body frame
        final Vector3D pInCBodyFrame = s.getPVCoordinates(this.centralBodyFrame).getPosition();

        // Spacecraft longitude
        final double longitude = MathLib.atan2(pInCBodyFrame.getY(), pInCBodyFrame.getX());

        // The g function is the difference between longitude and longitude to detect
        final double longitudeDiff = MathUtils.normalizeAngle((longitude - this.longToDetect), 0.0);

        // If a discontinuity is detected in the g method, we change the sign of the method
        if (MathLib.abs(longitudeDiff - this.lastLongitudeDiff) > FastMath.PI) {
            this.way = -this.way;
        }

        // save the current longitude diff
        this.lastLongitudeDiff = longitudeDiff;

        return this.way * longitudeDiff;

    }

    /**
     * Returns longitude to detect.
     * 
     * @return the longitude triggering the event.
     */
    public double getLongitudeToDetect() {
        return this.longToDetect;
    }

    /**
     * Returns central body frame.
     * 
     * @return the frame of the central body
     */
    public Frame getBodyFrame() {
        return this.centralBodyFrame;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>earthcentralBoyFrame: {@link Frame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new LongitudeDetector(this.longToDetect, this.centralBodyFrame, this.getMaxCheckInterval(),
            this.getThreshold(), this.actionLong, this.shouldBeRemovedFlag);
    }
}
