/**
 *
 * Copyright 2011-2022 CNES
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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-126:08/12/2023:[PATRIUS] Distinction increasing/decreasing dans LongitudeDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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

     /** Serializable UID. */
    private static final long serialVersionUID = -8263650183133893663L;
    
    /** Delta-time for longitude derivative computation (by centered finite differences). */
    private static final double DT = 1E-3;

    /** Longitude to detect. */
    private final double longToDetect;

    /** Frame of the central body. */
    private final Frame centralBodyFrame;

    /** Action performed. */
    private final Action actionLong;

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached.
     * </p>
     * 
     * @param longitudeToDetect
     *        the longitude to detect
     * @param bodyFrame
     *        the frame of the central body (take care to use a rotative frame to get the right longitude)
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame) {
        this(longitudeToDetect, bodyFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected
     * longitude is reached in {@link EventDetector.slopeSelection INCREASING_DECREASING} mode.
     * </p>
     * 
     * @param longitudeToDetect
     *        the longitude to detect
     * @param bodyFrame
     *        the frame of the central body (take care to use a rotative frame to get the right longitude)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame,
        final double maxCheck, final double threshold) {
        this(longitudeToDetect, bodyFrame, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is set to {@link EventDetector.slopeSelection INCREASING_DECREASING} mode.
     * </p>
     * 
     * @param longitudeToDetect
     *        the longitude to detect
     * @param bodyFrame
     *        the frame of the central body (take care to use a rotative frame to get the right longitude)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at longitude detection
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action) {
        this(longitudeToDetect, bodyFrame, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the longitude detector.
     * 
     * <p>
     * The default implementation behavior is set to {@link EventDetector.slopeSelection INCREASING_DECREASING} mode.
     * </p>
     * 
     * @param longitudeToDetect
     *        the longitude to detect
     * @param bodyFrame
     *        the frame of the central body (take care to use a rotative frame to get the right longitude)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at longitude detection
     * @param remove
     *        true if detector should be removed
     * @since 3.1
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        this(longitudeToDetect, bodyFrame, maxCheck, threshold, action, remove, EventDetector.INCREASING_DECREASING);
    }
    
    /**
     * Complete constructor for the longitude detector.
     * 
     * @param longitudeToDetect
     *        the longitude to detect
     * @param bodyFrame
     *        the frame of the central body (take care to use a rotative frame to get the right longitude)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at longitude detection
     * @param remove
     *        true if detector should be removed
     * @param slopeSelection
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @since 4.13
     */
    public LongitudeDetector(final double longitudeToDetect, final Frame bodyFrame, final double maxCheck, 
                             final double threshold, final Action action, final boolean remove, 
                             final int slopeSelection) {
        super(slopeSelection, maxCheck, threshold);
        this.longToDetect = MathUtils.normalizeAngle(longitudeToDetect, 0.0);
        this.centralBodyFrame = bodyFrame;

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
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, the value of the switching function increases when times increases around event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected longitude is reached
     * @throws PatriusException
     *         if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionLong;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {

        // The g function is the difference between longitude and longitude to detect
        return MathUtils.normalizeAngle((computeLongitude(state) - this.longToDetect), 0.0);

    }
    
    /** {@inheritDoc} */
    @Override
    public boolean filterEvent(final SpacecraftState state,
                               final boolean increasing,
                               final boolean forward) throws PatriusException {
        // Event is filtered if g function has not the right slope (being longitude velocity)
        // Longitude derivative
        final double lonSlope = getLongitudeDerivative(state);
        if (forward) {
            // Slope must be in the direction of increasing flag
            return increasing ^ lonSlope > 0;
        } else {
            // Backward case
            return !increasing ^ lonSlope > 0;
        }
    }
    
    /**
     * Compute the longitude at the state given in input.
     * 
     * @param state
     *        current state
     * @return longitude
     * @throws PatriusException
     *         if computation failed
     */
    private double computeLongitude(final SpacecraftState state) throws PatriusException {
        // spacecraft position in central body frame
        final Vector3D pInCBodyFrame = state.getPVCoordinates(this.centralBodyFrame).getPosition();

        // Spacecraft longitude
        return MathLib.atan2(pInCBodyFrame.getY(), pInCBodyFrame.getX());
    }

    /**
     * Compute the longitude derivative with respect to time (by centered finite differences).
     * 
     * @param state
     *        current state
     * @return longitude derivative with respect to time
     * @throws PatriusException
     *         if computation failed
     */
    private double getLongitudeDerivative(final SpacecraftState state) throws PatriusException {
        // Local time at t- and t+ in [0, 2Pi[
        double gMinus = MathUtils.normalizeAngle(computeLongitude(state.shiftedBy(-DT)), MathLib.PI);
        double gPlus = MathUtils.normalizeAngle(computeLongitude(state.shiftedBy(DT)), MathLib.PI);

        // Remove potential modulo 2.Pi
        if (gPlus > gMinus + MathLib.PI) {
            gMinus += 2. * MathLib.PI;
        }
        if (gMinus > gPlus + MathLib.PI) {
            gPlus += 2. * MathLib.PI;
        }

        return (gPlus - gMinus) / (2. * DT);
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
