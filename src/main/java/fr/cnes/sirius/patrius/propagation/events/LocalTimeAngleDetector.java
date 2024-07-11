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
 * @history created 07/03/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:346:23/04/2015:creation of a local time class
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::FA:902:13/12/2016:corrected anomaly on local time computation
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the local time angle of a spacecraft is equal to a predetermined value.<br>
 * The local time is represented by the angle between the projections of the Sun and the satellite
 * in the equatorial plane; therefore this angle is equal to zero when the local time is 12.00h and
 * &Pi; when the local time is 0.00h (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;).
 * <p>
 * The default implementation is to {@link EventDetector.Action#STOP stop} propagation when the local time is reached.
 * This can be changed by using provided constructors.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
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
 * @version $Id: LocalTimeAngleDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class LocalTimeAngleDetector extends AbstractDetector {

     /** Serializable UID. */
    private static final long serialVersionUID = -8185366674138568798L;

    /** Local time angle triggering the event. */
    private final double time;

    /** Sun. */
    private final CelestialBody sun;

    /** Last g() call local time difference to the reference to detect. */
    private double lastLocalTimeDiff;

    /** Evolution way of the local time to the reference to detect. */
    private int way;

    /** Action performed. */
    private final Action actionLocalTime;

    /**
     * Constructor for a LocalTimeDetector instance.
     * 
     * @param localTimeAngle satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public LocalTimeAngleDetector(final double localTimeAngle) throws PatriusException {
        this(localTimeAngle, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the local time
     * is reached.
     * </p>
     * 
     * @param localTimeAngle satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
        final double threshold) throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters.
     * 
     * @param localTimeAngle satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at local time detection
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
        final double threshold, final Action action) throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters.
     * 
     * @param localTimeAngle satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at local time detection
     * @param remove true if detector should be removed
     * @since 3.1
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
        final double threshold, final Action action, final boolean remove) throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, action, remove, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters including Sun choice.
     * 
     * @param localTimeAngle satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at local time detection
     * @param remove true if detector should be removed
     * @param sun Sun
     * @since 4.5
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
        final double threshold, final Action action, final boolean remove, final CelestialBody sun)
        throws PatriusException {
        // the local time event is triggered when the g-function slope is positive at its zero:
        super(EventDetector.INCREASING_DECREASING, maxCheck, threshold);
        this.time = localTimeAngle;
        if (this.time < -FastMath.PI || this.time >= FastMath.PI) {
            throw new PatriusException(PatriusMessages.LOCAL_SOLAR_TIME_OUT_OF_RANGE, "Local");
        }
        this.sun = sun;
        this.lastLocalTimeDiff = 0.0;
        this.way = 1;

        // action
        this.actionLocalTime = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle a local time angle event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when time increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected local time is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionLocalTime;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Getting the position of the satellite:
        final Vector3D sPV = state.getPVCoordinates().getPosition();

        // Compute local time angle
        Vector3D posTIRF = sPV;
        if (!state.getFrame().equals(FramesFactory.getTIRF())) {
            posTIRF = state.getFrame().getTransformTo(FramesFactory.getTIRF(), state.getDate()).transformVector(sPV);
        }

        // Compute satellite position
        final Vector3D posProj = new Vector3D(posTIRF.getX(), posTIRF.getY(), .0);
        // Compute Sun position
        final AbsoluteDate sunDate = getSignalEmissionDate(this.sun, state.getOrbit(), state.getDate());
        final Vector3D sunPV = this.sun.getPVCoordinates(sunDate, FramesFactory.getTIRF()).getPosition();
        final Vector3D sunPVproj = new Vector3D(sunPV.getX(), sunPV.getY(), .0);
        // Compute the angle between the sun and satellite projections over the equatorial plane
        double angle = Vector3D.angle(sunPVproj, posProj);
        if (sunPVproj.getX() * posProj.getY() - sunPVproj.getY() * posProj.getX() < 0) {
            // The "angle" function returns a value between 0 and PI, while we are working with angle between -PI and PI
            // when z-component of the cross product between the two vectors is negative, -angle is returned
            angle = -angle;
        }
        // Set angle in [-PI, PI[ if necessary
        angle = (angle >= FastMath.PI) ? angle - 2 * FastMath.PI : angle;

        // The g function is the difference between local time angle and local time angle to detect
        final double localTimeDiff = MathUtils.normalizeAngle((angle - this.time), 0.0);

        // If a discontinuity is detected in the g method, we change the sign of the method
        if (MathLib.abs(localTimeDiff - this.lastLocalTimeDiff) > FastMath.PI) {
            this.way = -this.way;
        }

        // save the current longitude diff
        this.lastLocalTimeDiff = localTimeDiff;

        // Computing the g function:
        return this.way * localTimeDiff;

    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Get local time angle to detect.
     * 
     * @return the local time angle triggering the event (in the range [-&Pi, &Pi[).
     * 
     * @since 1.2
     */
    public double getTime() {
        return this.time;
    }

    /**
     * Return the action at detection.
     * 
     * @return action at detection
     */
    public Action getAction() {
        return this.actionLocalTime;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        try {
            final LocalTimeAngleDetector res = new LocalTimeAngleDetector(this.time, this.getMaxCheckInterval(),
                    this.getThreshold(), this.actionLocalTime, this.shouldBeRemovedFlag);
            res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
            return res;
        } catch (final PatriusException e) {
            // It cannot happen, since initial detector has been properly created
            throw new PatriusExceptionWrapper(e);
        }
    }
}
