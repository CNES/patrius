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
 * @history created 06/03/12
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Detects when the beta angle of the spacecraft reaches a predetermined value.<br>
 * The beta angle is the angle between the orbit plane and the vector from the central body to the
 * sun. The angle is positive on the side of the plane containing the spacecraft's momentum vector.
 * The bodies considered are :
 * <ul>
 * <li>the spacecraft</li>
 * <li>the central body for the spacecraft</li>
 * <li>the Sun</li>
 * </ul>
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the beta angle is
 * reached. This can be changed by using provided constructors.
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
 * @author cardosop
 * 
 * @version $Id: BetaAngleDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class BetaAngleDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = -523367166332164051L;

    /** Angle triggering the event. */
    private final double angle;

    /** Action performed */
    private final Action actionBetaAngle;

    /** Celestial body. */
    private final CelestialBody sun;

    /**
     * Constructor for a BetaAngleDetector instance.
     * 
     * @param ang angle in radians triggering the event.
     * @throws PatriusException thrown if Sun could not be retrieved
     */
    public BetaAngleDetector(final double ang) throws PatriusException {
        this(ang, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a BetaAngleDetector instance with complimentary parameters.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the angle is
     * reached.
     * </p>
     * 
     * @param ang angle in radians triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @throws PatriusException thrown if Sun could not be retrieved
     * @throws IllegalArgumentException if angle is out of range [-Pi / 2 , Pi / 2]
     */
    public BetaAngleDetector(final double ang, final double maxCheck, final double threshold) throws PatriusException {
        this(ang, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for a BetaAngleDetector instance with complimentary parameters.
     * 
     * @param ang angle in radians triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at beta angle detection
     * @throws PatriusException thrown if Sun could not be retrieved
     * @throws IllegalArgumentException if angle is out of range [-Pi / 2 , Pi / 2]
     */
    public BetaAngleDetector(final double ang, final double maxCheck, final double threshold,
        final Action action) throws PatriusException {
        this(ang, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for a BetaAngleDetector instance with complimentary parameters.
     * 
     * @param ang angle in radians triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at beta angle detection
     * @param remove if the detector should be removed
     * @throws PatriusException thrown if Sun could not be retrieved
     * @since 3.1
     * @throws IllegalArgumentException if angle is out of range [-Pi / 2 , Pi / 2]
     */
    public BetaAngleDetector(final double ang, final double maxCheck, final double threshold,
        final Action action, final boolean remove) throws PatriusException {
        this(ang, maxCheck, threshold, action, remove, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor for a BetaAngleDetector instance with complimentary parameters including Sun choice.
     * 
     * @param ang angle in radians triggering the event.
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at beta angle detection
     * @param remove if the detector should be removed
     * @param sun sun
     * @since 4.5
     * @throws IllegalArgumentException if angle is out of range [-Pi / 2 , Pi / 2]
     */
    public BetaAngleDetector(final double ang, final double maxCheck, final double threshold,
        final Action action, final boolean remove, final CelestialBody sun) {
        super(maxCheck, threshold);
        // Validate input
        if (ang < -MathUtils.HALF_PI || ang > MathUtils.HALF_PI) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.OUT_OF_RANGE);
        }
        // Final fields
        this.angle = ang;
        this.sun = sun;

        // action
        this.actionBetaAngle = action;

        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Handles a beta angle event and chooses what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude.
     * @param increasing if true, the value of the switching function increases when time increases
     *        around event.
     * @param forward if true, the integration variable (time) increases during integration.
     * @return performed the action when the angle is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionBetaAngle;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Vector : from central body to the spacecraft
        final PVCoordinates sPV = state.getPVCoordinates();
        // Vector : from central body to the sun
        final AbsoluteDate sunDate = getSignalEmissionDate(sun, state.getOrbit(), state.getDate(), getThreshold());
        final PVCoordinates sunPV = sun.getPVCoordinates(sunDate, state.getFrame());
        final Vector3D sunVect = sunPV.getPosition();
        // Vector : spacecraft momentum
        final Vector3D momentum = sPV.getMomentum();

        // Beta angle (-Pi/2, Pi/2)
        final double betaAngle = MathUtils.HALF_PI - Vector3D.angle(momentum, sunVect);

        return (betaAngle - this.angle);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Returns beta angle triggering the event.
     * 
     * @return the Angle triggering the event.
     */
    public double getAngle() {
        return this.angle;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }

    /** {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>sun: {@link CelestialBody}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final BetaAngleDetector res = new BetaAngleDetector(this.angle, this.getMaxCheckInterval(), this.getThreshold(),
                this.actionBetaAngle, this.shouldBeRemovedFlag, sun);
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
