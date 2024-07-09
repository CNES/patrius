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
 * @history creation 01/03/2013
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for target entry/exit events with respect to a satellite sensor FOV defined by a vector3D
 * giving the direction in satellite frame and taking into account masking from the central body
 * <p>
 * This class handles fields of view with a circular boundary.
 * </p>
 * <p>
 * This class handles central body as a {@link GeometricBodyShape ellipsoid}.
 * </p>
 * <p>
 * The default implementation behaviour is to
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when the target is in
 * the field of view outside of eclipse. This can be changed by using one of the provided constructors.
 * </p>
 * <p>
 * This detector can takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType)} (default is signal being instantaneous).
 * </p>
 * 
 * @see Propagator#addEventDetector(fr.cnes.sirius.patrius.propagation.events.EventDetector)
 * @see GeometricBodyShape
 * @see IEllipsoid
 * @see EclipseDetector
 * @see CircularFieldOfViewDetector
 * 
 * @concurrency not thread-safe
 * @concurrency.comment one attribute not thread-safe and one conditionally thread-safe
 * 
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class CentralBodyMaskCircularFOVDetector extends AbstractDetector {

    /** Serial UID */
    private static final long serialVersionUID = 4676339505975043489L;

    /** The eclipse detector taking into account ellipsoid body shape */
    private final EclipseDetector eclipseDetector;

    /** The circular field of view detector */
    private final CircularFieldOfViewDetector circularFOVDetector;

    /** Action performed */
    private final Action actionCentralBodyMask;

    /** True if detector should be removed. */
    private boolean shouldBeRemovedFlag = false;

    /**
     * 
     * Constructor with user maxcheck and threshold creating a circularFOVDetector and an
     * EllipsoidEclipseDetector
     * <p>
     * The default implementation behaviour is to
     * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP stop} propagation when the target is
     * visible.
     * </p>
     * 
     * @param occultedBody the target to detect in field of view outside of eclipse
     * @param occultedBodyRadius the radius of this target
     * @param occultingBody the ellipsoid body shape of the central body potentially occulting the
     *        target
     * @param totalEclipseFlag true to detect only when the target is fully visible, false to detect
     *        also when the target is partially visible
     * @param center the sight axis direction of the field of view in satellite frame
     * @param halfAperture the half aperture of the field of view
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @see EclipseDetector
     * @see CircularFieldOfViewDetector
     * 
     * @since 1.3
     */
    public CentralBodyMaskCircularFOVDetector(final PVCoordinatesProvider occultedBody,
        final double occultedBodyRadius, final GeometricBodyShape occultingBody,
        final boolean totalEclipseFlag, final Vector3D center, final double halfAperture,
        final double maxCheck, final double threshold) {
        this(occultedBody, occultedBodyRadius, occultingBody, totalEclipseFlag, center,
            halfAperture, maxCheck, threshold, Action.STOP);
    }

    /**
     * 
     * Constructor with user maxcheck and threshold creating a circularFOVDetector and an
     * EllipsoidEclipseDetector
     * 
     * 
     * @param occultedBody the target to detect in field of view outside of eclipse
     * @param occultedBodyRadius the radius of this target
     * @param occultingBody the ellipsoid body shape of the central body potentially occulting the
     *        target
     * @param totalEclipseFlag true to detect only when the target is fully visible, false to detect
     *        also when the target is partially visible
     * @param center the sight axis direction of the field of view in satellite frame
     * @param halfAperture the half aperture of the field of view
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at circularFOV detection
     * @see EclipseDetector
     * @see CircularFieldOfViewDetector
     * 
     * @since 1.3
     */
    public CentralBodyMaskCircularFOVDetector(final PVCoordinatesProvider occultedBody,
        final double occultedBodyRadius, final GeometricBodyShape occultingBody,
        final boolean totalEclipseFlag, final Vector3D center, final double halfAperture,
        final double maxCheck, final double threshold, final Action action) {
        this(occultedBody, occultedBodyRadius, occultingBody, totalEclipseFlag, center,
            halfAperture, maxCheck, threshold, action, false);
    }

    /**
     * 
     * Constructor with user maxcheck and threshold creating a circularFOVDetector and an
     * EllipsoidEclipseDetector
     * 
     * @param occultedBody the target to detect in field of view outside of eclipse
     * @param occultedBodyRadius the radius of this target
     * @param occultingBody the ellipsoid body shape of the central body potentially occulting the
     *        target
     * @param totalEclipseFlag true to detect only when the target is fully visible, false to detect
     *        also when the target is partially visible
     * @param center the sight axis direction of the field of view in satellite frame
     * @param halfAperture the half aperture of the field of view
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at circularFOV detection
     * @param remove if detector should be removed
     * 
     * @see EclipseDetector
     * @see CircularFieldOfViewDetector
     * 
     * @since 3.1
     */
    public CentralBodyMaskCircularFOVDetector(final PVCoordinatesProvider occultedBody,
        final double occultedBodyRadius, final GeometricBodyShape occultingBody,
        final boolean totalEclipseFlag, final Vector3D center, final double halfAperture,
        final double maxCheck, final double threshold, final Action action, final boolean remove) {
        super(maxCheck, threshold);
        this.eclipseDetector = new EclipseDetector(occultedBody, occultedBodyRadius, occultingBody,
            totalEclipseFlag ? 0 : 1, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD);
        this.circularFOVDetector = new CircularFieldOfViewDetector(occultedBody, center,
            halfAperture, maxCheck);

        // action
        this.actionCentralBodyMask = action;

        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * 
     * Constructor with EclipseDetector and CircularFieldOfViewDetector.
     * 
     * @param eclipseDetectorIn Eclipse detector
     * @param circularFOVDetectorIn circular field of view
     * @param maxCheck maximum check (see {@link AbstractDetector})
     * @param threshold threshold (see {@link AbstractDetector})
     * @param action action performed at circularFOV detection
     * @param remove if detector should be removed
     * 
     * @see EclipseDetector
     * @see CircularFieldOfViewDetector
     * 
     * @since 3.1
     */
    public CentralBodyMaskCircularFOVDetector(final EclipseDetector eclipseDetectorIn,
        final CircularFieldOfViewDetector circularFOVDetectorIn, final double maxCheck,
        final double threshold, final Action action, final boolean remove) {
        super(maxCheck, threshold);
        this.eclipseDetector = eclipseDetectorIn;
        this.circularFOVDetector = circularFOVDetectorIn;

        // action
        this.actionCentralBodyMask = action;

        // remove (or not) detector
        this.shouldBeRemovedFlag = remove;
    }

    /**
     * Constructor with default maxcheck and default threshold, creating a circularFOVDetector and
     * an EllipsoidEclipseDetector
     * 
     * 
     * @param occultedBody the target to detect in field of view outside of eclipse
     * @param occultedBodyRadius the radius of this target
     * @param occultingBody the ellipsoid body shape of the central body potentially occulting the
     *        target
     * @param totalEclipseFlag true to detect only when the target is fully visible, false to detect
     *        also when the target is partially visible
     * @param center the sight axis direction of the field of view in satellite frame
     * @param halfAperture the half aperture of the field of view
     * 
     * @see EclipseDetector
     * @see CircularFieldOfViewDetector
     * 
     * @since 1.3
     */
    public CentralBodyMaskCircularFOVDetector(final PVCoordinatesProvider occultedBody,
        final double occultedBodyRadius, final GeometricBodyShape occultingBody,
        final boolean totalEclipseFlag, final Vector3D center, final double halfAperture) {

        this(occultedBody, occultedBodyRadius, occultingBody, totalEclipseFlag, center,
            halfAperture, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);

    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Handle propagation time for underlying detectors
        eclipseDetector.setPropagationDelayType(getPropagationDelayType());
        circularFOVDetector.setPropagationDelayType(getPropagationDelayType());
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.shouldBeRemovedFlag;
    }

    /**
     * Handle a target in field of view outside eclipse reaching event and choose what to do next.
     * 
     * @param s the current state of propagation
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when the expected circularFOV is reached
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return this.actionCentralBodyMask;
    }

    /**
     * The switching function is the minimum value between the eclipse detector g function and the
     * circularFOVDetector
     * 
     * @param s the current state of propagation
     * @throws PatriusException if some specific error occurs
     * @return value of the switching function
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        final double eclipseG = this.eclipseDetector.g(s);
        final double circularFOVG = this.circularFOVDetector.g(s);
        return MathLib.min(eclipseG, circularFOVG);
    }

    /**
     * Get the eclipse detector.
     * 
     * @return the eclipse detector taking into account ellipsoid body shape
     */
    public EclipseDetector getEclipseDetector() {
        return this.eclipseDetector;
    }

    /**
     * Get the circular FOV detector.
     * 
     * @return The circular field of view detector
     */
    public CircularFieldOfViewDetector getCircularFOVDetector() {
        return this.circularFOVDetector;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>circularFOVDetector: {@link CircularFieldOfViewDetector}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final CentralBodyMaskCircularFOVDetector res = new CentralBodyMaskCircularFOVDetector(
                (EclipseDetector) this.eclipseDetector.copy(), this.circularFOVDetector, this.getMaxCheckInterval(),
                this.getThreshold(), this.actionCentralBodyMask, this.shouldBeRemovedFlag);
        res.setPropagationDelayType(getPropagationDelayType());
        return res;
    }
}
