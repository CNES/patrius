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
 * @history created 11/06/12
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-80:30/06/2023:[PATRIUS] Discriminer le "increasing" et "decreasing" dans
 * NadirSolarIncidenceDetector (suite)
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11:DM:DM-3291:22/05/2023:[PATRIUS] Discriminer "increasing" et "decreasing"
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2414:27/05/2020:Choix des ephemeris solaires dans certains detecteurs 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:215:08/04/2014:modification of the nadir solar incidence definition
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Spacecraft's nadir point solar incidence detector.
 * <p>
 * The solar incidence is the angle between the nadir-satellite vector and the nadir-sun vector.<br>
 * This detector discriminates among increasing g events and decreasing g events.<br>
 * The default implementation is to {@link EventDetector.Action#STOP stop} propagation when the reference solar
 * incidence is reached. This can be changed by using provided constructors.
 * <p>
 * This detector can take into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, Frame)} (default is signal being instantaneous).
 * </p>
 * 
 * @concurrency thread-hostile
 * 
 * @concurrency.comment the celestial body is thread hostile itself.
 * 
 * @see EventDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: NadirSolarIncidenceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class NadirSolarIncidenceDetector extends AbstractSignalPropagationDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 3009120047664790195L;

    /** Sun. */
    private PVCoordinatesProvider sun;

    /** Body shape. */
    private final BodyShape bodyShape;

    /** Incidence to detect. */
    private final double incidenceRef;

    /**
     * Constructor for the nadir point solar incidence detector.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the local time
     * is reached.
     * </p>
     * 
     * @param incidence
     *        the incidence to detect (set 0. to detect the terminator)
     * @param bodyShape
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @throws PatriusException
     *         if there is an error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape bodyShape,
                                       final double maxCheck, final double threshold) throws PatriusException {
        this(incidence, bodyShape, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for the nadir point solar incidence detector.
     * 
     * @param incidence
     *        the incidence to detect (set 0. to detect the terminator)
     * @param bodyShape
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at nadir point solar incidence detection
     * @throws PatriusException
     *         if there is an error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape bodyShape,
                                       final double maxCheck, final double threshold, final Action action)
        throws PatriusException {
        this(incidence, bodyShape, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the nadir point solar incidence detector.
     * 
     * @param incidence
     *        the incidence to detect (set 0. to detect the terminator)
     * @param bodyShape
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at nadir point solar incidence detection
     * @param remove
     *        true if detector should be removed at nadir point solar incidence detection
     * @since 3.1
     * @throws PatriusException
     *         if there is an error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape bodyShape, final double maxCheck,
                                       final double threshold, final Action action, final boolean remove)
        throws PatriusException {
        this(incidence, bodyShape, maxCheck, threshold, action, remove, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor for the nadir point solar incidence detector with Sun choice.
     * 
     * @param incidence
     *        the incidence to detect (set 0. to detect the terminator)
     * @param bodyShape
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param action
     *        action performed at nadir point solar incidence detection
     * @param remove
     *        true if detector should be removed at nadir point solar incidence detection
     * @param sun
     *        Sun
     * @since 4.5
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape bodyShape, final double maxCheck,
                                       final double threshold, final Action action, final boolean remove,
                                       final CelestialPoint sun) {
        super(maxCheck, threshold, action, action, remove, remove);
        this.bodyShape = bodyShape;
        this.incidenceRef = incidence;
        this.sun = sun;
    }

    /**
     * Constructor for the nadir point solar incidence detector with Sun choice.
     * 
     * @param slopeSelectionIn
     *        the g-function slope selection (0, 1 or 2)
     * @param incidence
     *        the incidence to detect (set 0. to detect the terminator)
     * @param bodyShape
     *        the body shape
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param actionAtIncreasing
     *        action performed at nadir point solar incidence detection at increasing g event
     * @param actionAtDecreasing
     *        action performed at nadir point solar incidence detection at decreasing g event
     * @param removeAtIncreasing
     *        true if detector should be removed at nadir point solar incidence detection at increasing g event
     * @param removeAtDecreasing
     *        true if detector should be removed at nadir point solar incidence detection at decreasing g event
     * @param sun
     *        Sun
     * @since 4.11.1
     */
    public NadirSolarIncidenceDetector(final int slopeSelectionIn, final double incidence, final BodyShape bodyShape,
                                       final double maxCheck, final double threshold, final Action actionAtIncreasing,
                                       final Action actionAtDecreasing, final boolean removeAtIncreasing,
                                       final boolean removeAtDecreasing, final CelestialPoint sun) {
        super(slopeSelectionIn, maxCheck, threshold, actionAtIncreasing, actionAtDecreasing, removeAtIncreasing,
                removeAtDecreasing);
        this.bodyShape = bodyShape;
        this.incidenceRef = incidence;
        this.sun = sun;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {

        // spacecraft position
        final Vector3D satPos = state.getPVCoordinates(this.bodyShape.getBodyFrame()).getPosition();

        // nadir point
        final BodyPoint satPoint = this.bodyShape.buildPoint(satPos, this.bodyShape.getBodyFrame(),
            state.getDate(), BodyPointName.DEFAULT);
        final BodyPoint nadirPoint = satPoint.getClosestPointOnShape();
        final Vector3D nadirPos = nadirPoint.getPosition();

        // zenith vector from this nadir point
        final Vector3D zenithVect = satPos.subtract(nadirPos);

        // vector : nadir point to sun
        final AbsoluteDate sunDate = getSignalEmissionDate(state);
        final Vector3D sunPos = this.sun.getPVCoordinates(sunDate, this.bodyShape.getBodyFrame())
            .getPosition();
        final Vector3D nadirToSun = sunPos.subtract(nadirPos);

        // incidence angle
        final double sunElev = Vector3D.angle(nadirToSun, zenithVect);

        return sunElev - this.incidenceRef;
    }

    /**
     * Handle a solar incidence event and choose what to do next.
     * 
     * @param s
     *        the current state information : date, kinematics, attitude
     * @param increasing
     *        if true, the value of the switching function increases when time increases around event
     * @param forward
     *        if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected nadir point solar incidence is detected
     * @throws PatriusException
     *         if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
        throws PatriusException {
        final Action result;
        if (forward ^ !increasing) {
            // increasing case
            result = this.getActionAtEntry();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtEntry();
        } else {
            // decreasing case
            result = this.getActionAtExit();
            // remove (or not) detector
            this.shouldBeRemovedFlag = this.isRemoveAtExit();
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Setter for the sun {@link PVCoordinatesProvider}
     * 
     * @param sun the sun
     */
    public void setSun(final PVCoordinatesProvider sun) {
        this.sun = sun;
    }

    /**
     * Getter for the Sun.
     * 
     * @return the Sun
     */
    public PVCoordinatesProvider getSun() {
        return this.sun;
    }

    /**
     * Getter for the body shape.
     * 
     * @return the body shape
     */
    public BodyShape getBodyShape() {
        return this.bodyShape;
    }

    /**
     * Getter for the incidence.
     * 
     * @return incidence to detect
     */
    public double getIncidence() {
        return this.incidenceRef;
    }

    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
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

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inbodyShape: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final NadirSolarIncidenceDetector res = new NadirSolarIncidenceDetector(this.getSlopeSelection(),
            this.incidenceRef, this.bodyShape, this.getMaxCheckInterval(), this.getThreshold(), this.actionAtEntry,
            this.actionAtExit, this.removeAtEntry, this.removeAtExit, null);
        res.setSun(this.sun);
        res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return res;
    }
}
