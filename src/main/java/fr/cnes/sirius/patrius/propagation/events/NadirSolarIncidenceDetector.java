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
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Spacecraft's nadir point solar incidence detector.
 * <p>
 * The solar incidence is the angle between the nadir-satellite vector and the nadir-sun vector.<br>
 * This detector does not discriminate among increasing g events and decreasing g events: every detected event is a
 * solar incidence crossing event.<br>
 * The default implementation is to {@link EventDetector.Action#STOP stop} propagation when the reference solar
 * incidence is reached. This can be changed by using provided constructors.
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, fr.cnes.sirius.patrius.frames.Frame)} 
 * (default is signal being instantaneous).
 * </p>
 * 
 * @concurrency thread-hostile
 * 
 * @concurrency.comment the earth celestial body is thread hostile itself.
 * 
 * @see EventDetector
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: NadirSolarIncidenceDetector.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public class NadirSolarIncidenceDetector extends AbstractDetector {

    /** Serial UID. */
    private static final long serialVersionUID = 3009120047664790195L;

    /** The Sun. */
    private PVCoordinatesProvider inSun;

    /** The earth shape. */
    private final BodyShape inEarth;

    /** incidence to detect */
    private final double indenceRef;

    /** Action performed */
    private final Action actionNadir;

    /**
     * Constructor for the nadir point solar incidence detector
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the local time
     * is reached.
     * </p>
     * 
     * @param incidence the incidence to detect (set 0. to detect the terminator)
     * @param earth the earth shape
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @throws PatriusException error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape earth,
        final double maxCheck, final double threshold) throws PatriusException {
        this(incidence, earth, maxCheck, threshold, Action.STOP);
    }

    /**
     * Constructor for the nadir point solar incidence detector
     * 
     * @param incidence the incidence to detect (set 0. to detect the terminator)
     * @param earth the earth shape
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at nadir point solar incidence detection
     * @throws PatriusException error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape earth,
        final double maxCheck, final double threshold, final Action action)
        throws PatriusException {
        this(incidence, earth, maxCheck, threshold, action, false);
    }

    /**
     * Constructor for the nadir point solar incidence detector
     * 
     * @param incidence the incidence to detect (set 0. to detect the terminator)
     * @param earth the earth shape
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at nadir point solar incidence detection
     * @param remove true if detector should be removed at nadir point solar incidence detection
     * @since 3.1
     * @throws PatriusException error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape earth,
        final double maxCheck, final double threshold, final Action action, final boolean remove)
        throws PatriusException {
        this(incidence, earth, maxCheck, threshold, action, remove, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor for the nadir point solar incidence detector with Sun choice.
     * 
     * @param incidence the incidence to detect (set 0. to detect the terminator)
     * @param earth the earth shape
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at nadir point solar incidence detection
     * @param remove true if detector should be removed at nadir point solar incidence detection
     * @param sun Sun
     * @since 4.5
     * @throws PatriusException error when loading the ephemeris files
     */
    public NadirSolarIncidenceDetector(final double incidence, final BodyShape earth,
        final double maxCheck, final double threshold, final Action action, final boolean remove,
        final CelestialBody sun) throws PatriusException {
        super(maxCheck, threshold);
        this.inEarth = earth;
        this.indenceRef = incidence;
        this.inSun = sun;
        this.actionNadir = action;
        this.shouldBeRemovedFlag = remove;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {

        // spacecraft position
        final Vector3D satPos = state.getPVCoordinates(this.inEarth.getBodyFrame()).getPosition();

        // nadir point
        final GeodeticPoint satGeo = this.inEarth.transform(satPos, this.inEarth.getBodyFrame(), state.getDate());
        final GeodeticPoint nadirGeo = new GeodeticPoint(satGeo.getLatitude(),
            satGeo.getLongitude(), 0.0);
        final Vector3D nadirPos = this.inEarth.transform(nadirGeo);

        // zenith vector from this nadir point
        final Vector3D zenithVect = satPos.subtract(nadirPos);

        // vector : nadir point to sun
        final AbsoluteDate sunDate = getSignalEmissionDate(inSun, state.getOrbit(), state.getDate(), getThreshold());
        final Vector3D sunPos = this.inSun.getPVCoordinates(sunDate, this.inEarth.getBodyFrame())
            .getPosition();
        final Vector3D nadirToSun = sunPos.subtract(nadirPos);

        // incidence angle
        final double sunElev = Vector3D.angle(nadirToSun, zenithVect);

        return sunElev - this.indenceRef;
    }

    /**
     * Handle a solar incidence event and choose what to do next.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when time increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected nadir point solar incidence is detected
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionNadir;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Does nothing
    }

    /**
     * Sets the sun {@link PVCoordinatesProvider}
     * 
     * @param sun the sun
     */
    public void setSun(final PVCoordinatesProvider sun) {
        this.inSun = sun;
    }

    /**
     * Returns Sun.
     * 
     * @return the Sun
     */
    public PVCoordinatesProvider getSun() {
        return this.inSun;
    }

    /**
     * Returns Earth shape.
     * 
     * @return the earth shape
     */
    public BodyShape getEarthShape() {
        return this.inEarth;
    }

    /**
     * Returns incidence.
     * 
     * @return incidence to detect
     */
    public double getIncidence() {
        return this.indenceRef;
    }
    
    /** @inheritDoc */
    @Override
    public void setPropagationDelayType(final PropagationDelayType propagationDelayType, final Frame frame) {
        super.setPropagationDelayType(propagationDelayType, frame);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inEarth: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        try {
            final NadirSolarIncidenceDetector res =
                new NadirSolarIncidenceDetector(this.indenceRef, this.inEarth, this.getMaxCheckInterval(),
                    this.getThreshold(), this.actionNadir, this.shouldBeRemovedFlag);
            res.setSun(this.inSun);
            res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
            return res;
        } catch (final PatriusException e) {
            // It cannot happen
            throw new PatriusExceptionWrapper(e);
        }
    }
}
