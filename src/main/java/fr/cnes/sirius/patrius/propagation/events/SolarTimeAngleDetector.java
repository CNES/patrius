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
 * @history created 12/04/12
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
* VERSION:4.3:DM:DM-2001:15/05/2019:[Patrius] Donner le Soleil en entree de SolarTimeAngleDetector
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:710:22/03/2016:local time angle computation in [-PI, PI[
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the solar time angle of a spacecraft is equal to a predetermined value.<br>
 * The solar time is represented by the angle between the projections of the Sun in the osculator
 * orbital plane and the satellite position; therefore this angle is equal to zero when the solar
 * time is 12.00h and &Pi; when the solar time is 0.00h (Solar Time In Hours = 12.00h + solar time
 * angle * 12 / &Pi;).
 * <p>
 * The default implementation is to {@link EventDetector.Action#STOP stop} propagation when the solar time is reached.
 * This can be changed by using provided constructors.
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType)} (default is signal being instantaneous).
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
 * @version $Id: SolarTimeAngleDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 */
public class SolarTimeAngleDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = -9173356074954765598L;

    /** Solar time angle triggering the event. */
    private final double time;

    /** The Sun. */
    private final CelestialBody sun;

    /** Action performed */
    private final Action actionSolarTime;

    /** True if detector should be removed */
    private boolean shouldBeRemovedFlag = false;

    /**
     * Constructor for a SolarTimeDetector instance.
     * 
     * @param solarTimeAngle satellite solar time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projection of the Sun in the osculator orbital plane and the
     *        satellite position (Solar Time In Hours = 12.00h + solar time angle * 12 / &Pi;).
     * @throws PatriusException error when loading the ephemeris files or solar time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public SolarTimeAngleDetector(final double solarTimeAngle) throws PatriusException {
        this(solarTimeAngle, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a SolarTimeDetector instance with complimentary parameters.
     * 
     * @param solarTimeAngle satellite solar time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projection of the Sun in the osculator orbital plane and the
     *        satellite position (Solar Time In Hours = 12.00h + solar time angle * 12 / &Pi;).
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @throws PatriusException error when loading the ephemeris files or solar time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public SolarTimeAngleDetector(final double solarTimeAngle, final double maxCheck,
        final double threshold) throws PatriusException {
        this(solarTimeAngle, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a SolarTimeDetector instance with complimentary parameters.
     * 
     * @param solarTimeAngle satellite solar time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projection of the Sun in the osculator orbital plane and the
     *        satellite position (Solar Time In Hours = 12.00h + solar time angle * 12 / &Pi;).
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at solar time detection
     * @throws PatriusException error when loading the ephemeris files or solar time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public SolarTimeAngleDetector(final double solarTimeAngle, final double maxCheck,
        final double threshold, final Action action) throws PatriusException {
        this(solarTimeAngle, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a SolarTimeDetector instance with complimentary parameters.
     * 
     * @param solarTimeAngle satellite solar time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projection of the Sun in the osculator orbital plane and the
     *        satellite position (Solar Time In Hours = 12.00h + solar time angle * 12 / &Pi;).
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at solar time detection
     * @param remove true if detector should be removed
     * @since 3.1
     * @throws PatriusException error when loading the ephemeris files or solar time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public SolarTimeAngleDetector(final double solarTimeAngle, final double maxCheck,
        final double threshold, final Action action, final boolean remove)
        throws PatriusException {
        this(solarTimeAngle, CelestialBodyFactory.getSun(), maxCheck, threshold, action, remove);
    }

    /**
     * Constructor for a SolarTimeDetector instance with complimentary parameters.
     * 
     * @param solarTimeAngle satellite solar time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projection of the Sun in the osculator orbital plane and the
     *        satellite position (Solar Time In Hours = 12.00h + solar time angle * 12 / &Pi;).
     * @param sunModel Sun model
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at solar time detection
     * @param remove true if detector should be removed
     * @since 4.3
     * @throws PatriusException error when loading the ephemeris files or solar time angle not in
     *         the range [-&Pi;, &Pi;[
     */
    public SolarTimeAngleDetector(final double solarTimeAngle, final CelestialBody sunModel, final double maxCheck,
        final double threshold, final Action action, final boolean remove)
        throws PatriusException {
        // the solar time event is triggered when the g-function slope is positive at its zero
        super(EventDetector.INCREASING, maxCheck, threshold);
        this.time = solarTimeAngle;
        if (this.time < -FastMath.PI || this.time >= FastMath.PI) {
            throw new PatriusException(PatriusMessages.LOCAL_SOLAR_TIME_OUT_OF_RANGE, "Solar");
        }
        this.sun = sunModel;
        this.actionSolarTime = action;
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handle a solar time angle event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the solar time
     * is reached.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude.
     * @param increasing if true, the value of the switching function increases when time increases
     *        around event.
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected solar time is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionSolarTime;
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
        // Getting the position of the satellite:
        final Vector3D sat = s.getPVCoordinates(s.getFrame()).getPosition();
        // Getting the position of the sun:
        final AbsoluteDate sunDate = getSignalEmissionDate(sun, s, getThreshold(), getPropagationDelayType());
        final Vector3D sunP = this.sun.getPVCoordinates(sunDate, s.getFrame()).getPosition();
        final Vector3D n = s.getPVCoordinates().getMomentum().normalize();
        final Vector3D sunPj = sunP.subtract(n.scalarMultiply(Vector3D.dotProduct(sunP, n)));
        // Computing the angle between the satellite and the sun projection over the orbital plane:
        double angle = Vector3D.angle(sunPj, sat);
        if (Vector3D.dotProduct(n, (Vector3D.crossProduct(sunPj, sat))) < 0) {
            // The "angle" function returns a value between 0 and PI, while the solar time must be
            // between -PI and
            // PI: when n*(sunPj X sat) is negative, - angle:
            angle = -angle;
        }
        // Computing the g function:
        return MathLib.sin(angle - this.time);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Get solar time angle to detect.
     * 
     * @return the solar time angle triggering the event.
     */
    public double getTime() {
        return this.time;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        try {
            final SolarTimeAngleDetector res = new SolarTimeAngleDetector(this.time, this.sun,
                    this.getMaxCheckInterval(), this.getThreshold(), this.actionSolarTime, this.shouldBeRemovedFlag);
            res.setPropagationDelayType(getPropagationDelayType());
            return res;
        } catch (final PatriusException e) {
            // It cannot happen
            throw new PatriusExceptionWrapper(e);
        }
    }
}