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
 * @history created 13/02/25
 *
 * HISTORY
 * VERSION:4.16:OPENFD-489:25/04/2025:[PATRIUS] Adaptation de l'evenement LocalTime pour une direction zenithale
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * Detects when the local time angle of point on a spacecraft is equal to a predetermined value.<br>
 * The local time is represented by the angle between the projections of the Sun and the zenithal direction of the
 * point; therefore this angle is equal to zero when the local time is 12.00h and
 * &Pi; when the local time is 0.00h (Local Time In Hours = 12.00h + local time angle * 12 / &Pi;).
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment attributes are mutable and related to propagation.
 *
 * @see LocalTimeAngleDetector
 *
 * @author Thales Services Numeriques
 *
 * @version $Id: BodyPointLocalTimeAngleDetector.java $
 *
 * @since 4.16
 */
public class BodyPointLocalTimeAngleDetector extends LocalTimeAngleDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -8185366674138568798L;

    /** Body point which zenith direction is used instead of satellite position (optional) */
    private final BodyPoint bodyPoint;

    /**
     * Constructor
     *
     * @param localTimeAngle
     *        satellite local time angle triggering the event (in the range [-&Pi;,
     *        &Pi;[). Angle between the projections of the Sun and the satellite in the equatorial
     *        plane (Local Time In Hours = 12.00h + localTimeAngle * 12 / &Pi;)
     * @param bodyPoint
     *        Body point which zenith direction is used instead of satellite position (optional)
     * @param maxCheck
     *        maximum check (see {@link AbstractDetector})
     * @param threshold
     *        threshold (see {@link AbstractDetector})
     * @param frame
     *        in relation to which the calculations are made
     * @param action
     *        action performed at local time detection
     * @param remove
     *        true if detector should be removed
     * @param sun
     *        Sun
     * @param slopeSelection
     *        {@link NodeDetector#ASCENDING} for ascending node detection,<br>
     *        {@link NodeDetector#DESCENDING} for descending node detection,<br>
     *        {@link NodeDetector#ASCENDING_DESCENDING} for both ascending and descending node
     *        detection.
     * @since 4.16
     * @throws PatriusException
     *         error when loading the ephemeris files or local time angle not in
     *         the range [-&Pi;, &Pi;[ or frame not null and not inertial
     */
    public BodyPointLocalTimeAngleDetector(final double localTimeAngle, final BodyPoint bodyPoint,
                                           final double maxCheck, final double threshold,
                                           final CelestialBodyFrame frame, final Action action,
                                           final boolean remove, final PVCoordinatesProvider sun,
                                           final int slopeSelection)
        throws PatriusException {
        super(localTimeAngle, maxCheck, threshold, frame, action, remove, sun, slopeSelection);
        this.bodyPoint = bodyPoint;
    }

    /**
     * Compute the local time.
     *
     * @param state
     *        current state
     * @return local time
     * @throws PatriusException
     *         thrown if computation failed
     */
    @Override
    protected double computeLocalTime(final SpacecraftState state) throws PatriusException {

        // Check frame is not null
        if (getFrame() == null) {
            throw new PatriusException(new DummyLocalizable("Frame is mandatory"));
        }

        final AbsoluteDate sunDate = getSignalEmissionDate(state);

        // Get the normal direction of the body point
        final Vector3D satPos = this.bodyPoint.getNormal(sunDate, getFrame());
        // Compute Sun position:
        final Vector3D sunPos =
            getSun().getPVCoordinates(sunDate, getFrame()).getPosition();

        return computeSunSatAngle(satPos, sunPos);
    }

    /**
     * Getter for the body point
     *
     * @return the body point
     */
    public BodyPoint getBodyPoint() {
        return this.bodyPoint;
    }

    /** {@inheritDoc} */
    @Override
    public BodyPointLocalTimeAngleDetector copy() {
        try {
            final BodyPointLocalTimeAngleDetector res = new BodyPointLocalTimeAngleDetector(
                getTime(), getBodyPoint(), getMaxCheckInterval(), getThreshold(),
                getFrame(), getAction(), this.shouldBeRemovedFlag, getSun(),
                getSlopeSelection());

            res.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
            return res;
        } catch (final PatriusException e) {
            // It cannot happen, since initial detector has been properly created
            throw new PatriusExceptionWrapper(e);
        }
    }

}