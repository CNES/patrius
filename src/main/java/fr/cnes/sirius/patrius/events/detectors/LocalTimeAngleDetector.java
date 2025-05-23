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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-89:30/06/2023:[PATRIUS] Problème dans la fonction g de LocalTimeAngleDetector - Retour en
 * arrière
 * VERSION:4.11:DM:DM-3258:22/05/2023:[PATRIUS] Adaptation des detecteurs SolarTime et LocalTime pour l'interplanetaire
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
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
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
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
 * {@link #setPropagationDelayType(PropagationDelayType, Frame)} (default is signal being instantaneous).
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
public class LocalTimeAngleDetector extends AbstractSignalPropagationDetector {

     /** Serializable UID. */
    private static final long serialVersionUID = -8185366674138568798L;

    /** Delta-time for local time derivative computation (by centered finite differences). */
    private static final double DT = 1E-3;
    
    /** Local time angle triggering the event. */
    private final double time;

    /** Sun. */
    private final CelestialPoint sun;

    /** Action performed. */
    private final Action actionLocalTime;
    
    /** Frame in relation to which the calculations are made */
    private final CelestialBodyFrame frame;

    /**
     * Constructor for a LocalTimeDetector instance.
     * <p>Local time will be computed in satellite orbit frame.</p>
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
     * <p>Local time will be computed in satellite orbit frame.</p>
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
     * @param frame in relation to which the calculations are made
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[ or frame not null and not inertial
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
                                  final double threshold, final CelestialBodyFrame frame) throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, frame, Action.STOP);
    }

    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters.
     * <p>Local time will be computed in satellite orbit frame.</p>
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
     * @param frame in relation to which the calculations are made
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[ or frame not null and not inertial
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
                                  final double threshold, final CelestialBodyFrame frame, final Action action)
        throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, frame, action, false);
    }
    
    
    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters.
     * <p>Local time will be computed in satellite orbit frame.</p>
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
     * Constructor for a LocalTimeDetector instance with complimentary parameters.
     * 
     * @param localTimeAngle satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at local time detection
     * @param remove true if detector should be removed
     * @param frame in relation to which the calculations are made
     * @since 3.1
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[ or frame not null and not inertial
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
                                  final double threshold, final CelestialBodyFrame frame, final Action action,
                                  final boolean remove) throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, frame, action, remove, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor for a LocalTimeDetector instance with complimentary parameters including Sun choice.
     * <p>Local time will be computed in satellite orbit frame.</p>
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
        final double threshold, final Action action, final boolean remove, final CelestialPoint sun)
        throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, null, action, remove, sun);
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
     * @param frame in relation to which the calculations are made
     * @since 4.5
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[ or frame not null and not inertial
     */
    public LocalTimeAngleDetector(final double localTimeAngle, final double maxCheck,
                                  final double threshold, final CelestialBodyFrame frame, final Action action,
                                  final boolean remove, final CelestialPoint sun)
        throws PatriusException {
        this(localTimeAngle, maxCheck, threshold, frame, action, remove, sun, EventDetector.INCREASING_DECREASING);
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
     * @param frame in relation to which the calculations are made
     * @param slopeSelection
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @since 4.5
     * @throws PatriusException error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[ or frame not null and not inertial
     */
    public LocalTimeAngleDetector(final double localTimeAngle,
            final double maxCheck,
            final double threshold,
            final CelestialBodyFrame frame,
            final Action action,
            final boolean remove,
            final CelestialPoint sun,
            final int slopeSelection) throws PatriusException {
        // the local time event is triggered when the g-function slope is positive at its zero:
        super(slopeSelection, maxCheck, threshold);
        this.time = localTimeAngle;
        if (this.time < -FastMath.PI || this.time >= FastMath.PI) {
            throw new PatriusException(PatriusMessages.LOCAL_SOLAR_TIME_OUT_OF_RANGE, "Local");
        }
        this.sun = sun;

        // action
        this.actionLocalTime = action;
        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
        if (frame != null && !frame.isPseudoInertial()) {
            throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
        }
        this.frame = frame;
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
        // The g function is the difference between local time angle and local time angle to detect
        return MathUtils.normalizeAngle((computeLocalTime(state) - this.time), 0.0);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /** {@inheritDoc} */
    @Override
    public boolean filterEvent(final SpacecraftState state,
            final boolean increasing,
            final boolean forward) throws PatriusException {
        // Event is filtered if g function has not the right slope (being local time velocity)
        // Local time derivative
        final double ltSlope = getLocalTimeDerivative(state);
        if (forward) {
            // Slope must be in the direction of increasing flag
            return increasing ^ ltSlope > 0;
        } else {
            // Backward case
            return !increasing ^ ltSlope > 0;
        }
    }

    /**
     * Compute the local time.
     * @param state current state
     * @return local time
     * @throws PatriusException thrown if computation failed
     */
    private double computeLocalTime(final SpacecraftState state) throws PatriusException {
        final AbsoluteDate sunDate = getSignalEmissionDate(state);
        final Vector3D satPos;
        final Vector3D sunPos;
        if (this.frame == null) {
            // Getting the position of the satellite in satellite orbit frame:
            satPos = state.getPVCoordinates().getPosition();
            // Compute Sun position:
            sunPos = this.sun.getPVCoordinates(sunDate, state.getFrame()).getPosition();
        } else {
            // Getting the position of the satellite in the right frame:
            satPos = state.getPVCoordinates(this.frame).getPosition();
            // Compute Sun position in the right frame:
            sunPos = this.sun.getPVCoordinates(sunDate, this.frame).getPosition();
        }
        // Compute satellite position projection
        final Vector3D satPosProj = new Vector3D(satPos.getX(), satPos.getY(), .0);
        // Compute sun position projection
        final Vector3D sunPosProj = new Vector3D(sunPos.getX(), sunPos.getY(), .0);
        // Compute the angle between the sun and satellite projections over the equatorial plane
        double angle = Vector3D.angle(sunPosProj, satPosProj);
        if (sunPosProj.getX() * satPosProj.getY() - sunPosProj.getY() * satPosProj.getX() < 0) {
            // The "angle" function returns a value between 0 and PI, while we are working with angle between -PI and PI
            // when z-component of the cross product between the two vectors is negative, -angle is returned
            angle = -angle;
        }
        return angle;
    }

    /**
     * Compute the local time derivative with respect to time (by centered finite differences).
     * @param state current state
     * @return local time derivative with respect to time
     * @throws PatriusException thrown if computation failed
     */
    private double getLocalTimeDerivative(final SpacecraftState state) throws PatriusException {
        // Local time at t- and t+ in [0, 2Pi[
        double gMinus = MathUtils.normalizeAngle(computeLocalTime(state.shiftedBy(-DT)), MathLib.PI);
        double gPlus = MathUtils.normalizeAngle(computeLocalTime(state.shiftedBy(DT)), MathLib.PI);

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

    /**
     * Returns the frame used for solar time computation.
     * @return the frame used for solar time computation
     */
    public CelestialBodyFrame getFrame() {
        return frame;
    }

    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frameIn) {
        super.setPropagationDelayType(propagationDelayType, frameIn);
    }
    
    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getEmitter(final SpacecraftState s) {
        return this.sun;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinatesProvider getReceiver(final SpacecraftState s) {
        return s.getOrbit();
    }

    /** {@inheritDoc} */
    @Override
    public DatationChoice getDatationChoice() {
        return DatationChoice.RECEIVER;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector copy() {
        try {
            final LocalTimeAngleDetector res = new LocalTimeAngleDetector(this.time, this.getMaxCheckInterval(),
                    this.getThreshold(), getFrame(), this.actionLocalTime, this.shouldBeRemovedFlag, sun);
            res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
            return res;
        } catch (final PatriusException e) {
            // It cannot happen, since initial detector has been properly created
            throw new PatriusExceptionWrapper(e);
        }
    }
}
