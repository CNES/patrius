/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 * HISTORY
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-442:25/04/2025:[PATRIUS] Calcul des eclipses d'un corps celeste
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.events.detectors;

import fr.cnes.sirius.patrius.bodies.ApparentRadiusProvider;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.ConstantRadiusProvider;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.forces.radiation.LightingRatio;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for events related to a celestial body in eclipse.
 * <p>
 * This class finds eclipse events, i.e. celestial body within umbra (total eclipse) or penumbra
 * (partial eclipse).
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#CONTINUE} continue
 * propagation when entering the eclipse and to {@link EventDetector.Action#STOP} stop
 * propagation when exiting the eclipse. This can be changed by using some constructors.
 * <p>
 * This detector takes into account signal propagation duration through
 * {@link #setPropagationDelayType(PropagationDelayType, Frame)} (default is signal being
 * instantaneous).
 * </p>
 *
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 */
public class BodyInEclipseDetector extends AbstractEclipseDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -2148810509334096295L;

    /** Target body. */
    private final PVCoordinatesProvider targetBody;

    /** Radius of the target body */
    private final double targetRadius;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private final boolean totalEclipse;

    /**
     * Flag to indicate whether the detected events correspond to the instants when the
     * target body begins or stops to be fully in eclipse (true), or partially in eclipse (false)
     */
    private final boolean bodyFullyInEclipse;

    /** Model to use */
    private final BodyInEclipseModelEnum model;

    /**
     * Enumerate for the types of available models for the BodyInEclipse calculations
     */
    public enum BodyInEclipseModelEnum {
        /** Exact model key */
        EXACT_MODEL,
        /** Approximative model key */
        APPROX_MODEL
    }

    /**
     * Build a new eclipse detector based on full shape for the occulting body.
     * The occulted body and the target body are spherical.
     * Action at entry CONTINUE by default and action at exit STOP by default.
     * RemoveEntry and removeExit are false by default.
     *
     * @param targetBodyIn
     *        the target body ephemeris
     * @param targetRadiusIn
     *        the target body radius (m)
     * @param occulted
     *        the occulted body ephemeris
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occultingBodyIn
     *        the occulting body
     * @param totalEclipseIn
     *        true for total eclipse (umbra), false for partial eclipse (penumbra)
     * @param bodyFullyInEclipseIn
     *        true for body fully in eclipse, false for body partially in
     *        eclipse
     * @param modelIn
     *        the model to use (exact or approx)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     *
     */
    public BodyInEclipseDetector(final PVCoordinatesProvider targetBodyIn, final double targetRadiusIn,
                                 final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                 final BodyShape occultingBodyIn, final boolean totalEclipseIn,
                                 final boolean bodyFullyInEclipseIn, final BodyInEclipseModelEnum modelIn,
                                 final int slopeSelection, final double maxCheck, final double threshold) {

        this(targetBodyIn, targetRadiusIn, occulted, occultedRadiusIn, occultingBodyIn, totalEclipseIn,
            bodyFullyInEclipseIn, modelIn, slopeSelection, maxCheck, threshold, false, false);
    }

    /**
     * Build a new eclipse detector based on full shape for the occulting body.
     * The occulted body and the target body are spherical.
     * Action at entry CONTINUE by default and action at exit STOP by default.
     *
     * @param targetBodyIn
     *        the target body ephemeris
     * @param targetRadiusIn
     *        the target body radius (m)
     * @param occulted
     *        the occulted body ephemeris
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occultingBodyIn
     *        the occulting body
     * @param totalEclipseIn
     *        true for total eclipse (umbra), false for partial eclipse (penumbra)
     * @param bodyFullyInEclipseIn
     *        true for body fully in eclipse, false for body partially in
     *        eclipse
     * @param modelIn
     *        the model to use (exact or approx)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     *
     */
    public BodyInEclipseDetector(final PVCoordinatesProvider targetBodyIn, final double targetRadiusIn,
                                 final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                 final BodyShape occultingBodyIn, final boolean totalEclipseIn,
                                 final boolean bodyFullyInEclipseIn, final BodyInEclipseModelEnum modelIn,
                                 final int slopeSelection, final double maxCheck, final double threshold,
                                 final boolean removeEntry, final boolean removeExit) {

        this(targetBodyIn, targetRadiusIn, occulted, occultedRadiusIn, occultingBodyIn, totalEclipseIn,
            bodyFullyInEclipseIn, modelIn, slopeSelection, maxCheck, threshold, Action.CONTINUE,
            Action.STOP, removeEntry, removeExit);
    }

    /**
     * Build a new eclipse detector.
     * All 3 bodies are spherical.
     * Action at entry CONTINUE by default and action at exit STOP by default.
     * RemoveEntry and removeExit are false by default.
     *
     * @param targetBodyIn
     *        the target body ephemeris
     * @param targetRadiusIn
     *        the target body radius (m)
     * @param occulted
     *        the occulted body ephemeris
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occulting
     *        the occulting body ephemeris
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param totalEclipseIn
     *        true for total eclipse (umbra), false for partial eclipse (penumbra)
     * @param bodyFullyInEclipseIn
     *        true for body fully in eclipse, false for body partially in
     *        eclipse
     * @param modelIn
     *        the model to use (exact or approx)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     */
    public BodyInEclipseDetector(final PVCoordinatesProvider targetBodyIn, final double targetRadiusIn,
                                 final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                 final PVCoordinatesProvider occulting, final double occultingRadius,
                                 final boolean totalEclipseIn, final boolean bodyFullyInEclipseIn,
                                 final BodyInEclipseModelEnum modelIn, final int slopeSelection,
                                 final double maxCheck, final double threshold) {

        this(targetBodyIn, targetRadiusIn, occulted, occultedRadiusIn, occulting, occultingRadius, totalEclipseIn,
            bodyFullyInEclipseIn, modelIn, slopeSelection, maxCheck, threshold, false, false);
    }

    /**
     * Build a new eclipse detector.
     * All 3 bodies are spherical.
     * Action at entry CONTINUE by default and action at exit STOP by default.
     *
     * @param targetBodyIn
     *        the target body ephemeris
     * @param targetRadiusIn
     *        the target body radius (m)
     * @param occulted
     *        the occulted body ephemeris
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occulting
     *        the occulting body ephemeris
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param totalEclipseIn
     *        true for total eclipse (umbra), false for partial eclipse (penumbra)
     * @param bodyFullyInEclipseIn
     *        true for body fully in eclipse, false for body partially in
     *        eclipse
     * @param modelIn
     *        the model to use (exact or approx)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     */
    public BodyInEclipseDetector(final PVCoordinatesProvider targetBodyIn, final double targetRadiusIn,
                                 final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                 final PVCoordinatesProvider occulting, final double occultingRadius,
                                 final boolean totalEclipseIn, final boolean bodyFullyInEclipseIn,
                                 final BodyInEclipseModelEnum modelIn, final int slopeSelection,
                                 final double maxCheck, final double threshold,
                                 final boolean removeEntry, final boolean removeExit) {

        this(targetBodyIn, targetRadiusIn, occulted, occultedRadiusIn, occulting, occultingRadius, totalEclipseIn,
            bodyFullyInEclipseIn, modelIn, slopeSelection, maxCheck, threshold, Action.CONTINUE, Action.STOP,
            removeEntry, removeExit);
    }

    /**
     * Build a new eclipse detector based on full shape for the occulting body.
     * The occulted body and the target body are spherical.
     *
     * @param targetBodyIn
     *        the target body ephemeris
     * @param targetRadiusIn
     *        the target body radius (m)
     * @param occulted
     *        the occulted body ephemeris
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occultingBodyIn
     *        the occulting body
     * @param totalEclipseIn
     *        true for total eclipse (umbra), false for partial eclipse (penumbra)
     * @param bodyFullyInEclipseIn
     *        true for body fully in eclipse, false for body partially in
     *        eclipse
     * @param modelIn
     *        the model to use (exact or approx)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     *
     */
    public BodyInEclipseDetector(final PVCoordinatesProvider targetBodyIn, final double targetRadiusIn,
                                 final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                 final BodyShape occultingBodyIn, final boolean totalEclipseIn,
                                 final boolean bodyFullyInEclipseIn, final BodyInEclipseModelEnum modelIn,
                                 final int slopeSelection, final double maxCheck, final double threshold,
                                 final Action entry, final Action exit, final boolean removeEntry,
                                 final boolean removeExit) {

        super(occulted, occultedRadiusIn, occultingBodyIn, maxCheck, threshold,
            entry, exit, removeEntry, removeExit, slopeSelection);

        this.targetBody = targetBodyIn;
        this.targetRadius = targetRadiusIn;
        this.totalEclipse = totalEclipseIn;
        this.bodyFullyInEclipse = bodyFullyInEclipseIn;
        this.model = modelIn;
    }

    /**
     * Build a new eclipse detector.
     * All 3 bodies are spherical.
     *
     * @param targetBodyIn
     *        the target body ephemeris
     * @param targetRadiusIn
     *        the target body radius (m)
     * @param occulted
     *        the occulted body ephemeris
     * @param occultedRadiusIn
     *        the occulted body radius (m)
     * @param occulting
     *        the occulting body ephemeris
     * @param occultingRadius
     *        the occulting body radius (m)
     * @param totalEclipseIn
     *        true for total eclipse (umbra), false for partial eclipse (penumbra)
     * @param bodyFullyInEclipseIn
     *        true for body fully in eclipse, false for body partially in
     *        eclipse
     * @param modelIn
     *        the model to use (exact or approx)
     * @param slopeSelection
     *        slope selection
     * @param maxCheck
     *        maximal checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     * @param entry
     *        action performed when entering the eclipse
     * @param exit
     *        action performed when exiting the eclipse
     * @param removeEntry
     *        when the spacecraft point enters the zone.
     * @param removeExit
     *        when the spacecraft point leaves the zone.
     */
    public BodyInEclipseDetector(final PVCoordinatesProvider targetBodyIn, final double targetRadiusIn,
                                 final PVCoordinatesProvider occulted, final double occultedRadiusIn,
                                 final PVCoordinatesProvider occulting, final double occultingRadius,
                                 final boolean totalEclipseIn, final boolean bodyFullyInEclipseIn,
                                 final BodyInEclipseModelEnum modelIn, final int slopeSelection,
                                 final double maxCheck, final double threshold,
                                 final Action entry, final Action exit, final boolean removeEntry,
                                 final boolean removeExit) {

        super(occulted, occultedRadiusIn, occulting, occultingRadius, slopeSelection, maxCheck, threshold,
            entry, exit, removeEntry, removeExit);

        this.targetBody = targetBodyIn;
        this.targetRadius = targetRadiusIn;
        this.totalEclipse = totalEclipseIn;
        this.bodyFullyInEclipse = bodyFullyInEclipseIn;
        this.model = modelIn;
    }

    /**
     * Compute the value of the switching function. This function becomes negative when entering the
     * region of shadow and positive when exiting.
     *
     * @param state
     *        state
     * @return value of the switching function
     * @exception PatriusException
     *            if some specific error occurs
     */
    @Override
    public double g(final SpacecraftState state) throws PatriusException {
        return this.g(state.getDate());
    }

    /**
     * Compute the value of the switching function at a given date.
     *
     * @param date
     *        date of computation
     * @return value of the switching function
     * @throws PatriusException
     */
    public double g(final AbsoluteDate date) throws PatriusException {

        // Compute lighting ratio
        final LightingRatio lightingRatioComputer = buildLightingRatioComputer(getOcculted());
        final double lightingRatio =
                lightingRatioComputer.computeExtended(getInterestPoint(), date);

        // Compute g = current LR - target LR
        return lightingRatio - getTargetLightingRatio();
    }

    /**
     * Compute the interest point.
     * This point is moving at the surface of the target body.
     * It is the closest point to the cone (penumbra or umbra) for the detection of body partially
     * in eclipse.
     * It is the farthest point to the cone (penumbra or umbra) for the detection of body totally in
     * eclipse.
     *
     * @return the PVCoordinatesProvider of the interest point.
     */
    private PVCoordinatesProvider getInterestPoint() {

        final PVCoordinatesProvider occultedBody = getOcculted();
        final PVCoordinatesProvider occultingBody = getOcculting();
        final double occultedRadius = getOccultedRadius();

        return new PVCoordinatesProvider(){

            private static final long serialVersionUID = 1L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws PatriusException {

                // Initialize return object
                PVCoordinates pvCoordOut;

                final PVCoordinates targetPV = BodyInEclipseDetector.this.targetBody.getPVCoordinates(date, frame);
                final Vector3D posTarget = targetPV.getPosition();
                final Vector3D posOcculted =
                        occultedBody.getPVCoordinates(date, frame).getPosition();
                final Vector3D posOcculting =
                        occultingBody.getPVCoordinates(date, frame).getPosition();

                // Special case: the line occulted-occulting intersects the target body
                final Line occultedOccultingLine = new Line(posOcculted, posOcculting);
                final Sphere targetSphere = new Sphere(posTarget, BodyInEclipseDetector.this.targetRadius);
                final Vector3D[] intersectionPoints =
                        targetSphere.getIntersectionPoints(occultedOccultingLine);
                if (intersectionPoints.length > 0 && !BodyInEclipseDetector.this.bodyFullyInEclipse) {
                    // For body partially in eclipse, we return the intersection point
                    // because the nominal algorithm fails if the target body is much larger than
                    // the occulting body
                    pvCoordOut = new PVCoordinates(intersectionPoints[0], targetPV.getVelocity());
                } else {

                    // Compute alpha and beta angles
                    double alpha;
                    double beta;

                    final int sign;
                    if (BodyInEclipseDetector.this.bodyFullyInEclipse) {
                        sign = -1;
                    } else {
                        sign = 1;
                    }

                    final Vector3D targetToOcculted = posOcculted.subtract(posTarget);
                    final double distanceToOcculted = targetToOcculted.getNorm();

                    if (BodyInEclipseDetector.this.model == BodyInEclipseModelEnum.EXACT_MODEL) {
                        if ((!BodyInEclipseDetector.this.bodyFullyInEclipse
                                && BodyInEclipseDetector.this.isTotalEclipse())
                                || (BodyInEclipseDetector.this.bodyFullyInEclipse
                                        && !BodyInEclipseDetector.this.isTotalEclipse())) {
                            beta = MathLib
                                    .asin((occultedRadius + BodyInEclipseDetector.this.targetRadius) / distanceToOcculted);
                            alpha = sign * (0.5 * MathLib.PI - beta);
                        } else {
                            beta = MathLib
                                    .asin((occultedRadius - BodyInEclipseDetector.this.targetRadius) / distanceToOcculted);
                            alpha = sign * (0.5 * MathLib.PI + beta);
                        }
                    } else if (BodyInEclipseDetector.this.model == BodyInEclipseModelEnum.APPROX_MODEL) {
                        beta = MathLib.asin(BodyInEclipseDetector.this.targetRadius / distanceToOcculted);
                        alpha = sign * (0.5 * MathLib.PI - beta);
                    } else {
                        throw new IllegalArgumentException(UNSUPPORTED_MODE_EXCEPTION
                            + BodyInEclipseDetector.this.model);
                    }

                    // Build the plane that contains the 3 bodies centers
                    final Plane plane = new Plane(posTarget, posOcculted, posOcculting);
                    final Vector3D normalToPlane = plane.getNormal();

                    // Apply rotation of angle alpha
                    final Rotation rot = new Rotation(normalToPlane, alpha);
                    final Vector3D delta =
                            rot.applyTo(targetToOcculted.normalize()).scalarMultiply(
                                BodyInEclipseDetector.this.targetRadius);

                    // Create the interest point
                    final Vector3D interestPos = posTarget.add(delta);

                    pvCoordOut = new PVCoordinates(interestPos, targetPV.getVelocity());
                }

                return pvCoordOut;
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return BodyInEclipseDetector.this.targetBody.getNativeFrame(date);
            }
        };

    }

    /** {@inheritDoc} */
    @Override
    public boolean isTotalEclipse() {
        return this.totalEclipse;
    }

    /**
     * Get the eclipse flag.
     *
     * @param date
     *        the current date
     * @return an eclipse flag indicating whether the object is in eclipse: true if it is in eclipse,
     *         false if it is not
     * @throws PatriusException
     *         if some specific error occurs while retrieving the value of the switching function g(s)
     */
    public boolean isInEclipse(final AbsoluteDate date) throws PatriusException {
        return this.g(date) < 0;
    }

    /**
     * Get the target lighting ratio according to the eclipse type (total or partial)
     *
     * @return
     */
    private double getTargetLightingRatio() {

        double targetLightingRatio;

        if (isTotalEclipse()) {
            targetLightingRatio = 0.;
        } else {
            targetLightingRatio = 1.;
        }

        return targetLightingRatio;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>occultingBody: {@link PVCoordinatesProvider}</li>
     * <li>occultedBody: {@link PVCoordinatesProvider}</li>
     * <li>occultingRadiusProvider: {@link ApparentRadiusProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public BodyInEclipseDetector copy() {
        final BodyInEclipseDetector result;
        // Check if occulting radius provider is an instance of ConstantRadiusProvider
        if (getOccultingRadiusProvider() instanceof ConstantRadiusProvider) {
            // Occulting radius provider is an instance of ConstantRadiusProvider
            result = new BodyInEclipseDetector(this.targetBody, this.targetRadius,
                getOcculted(), getOccultedRadius(), getOcculting(),
                getOccultingRadiusProvider().getApparentRadius(null, null, null, null),
                isTotalEclipse(), this.bodyFullyInEclipse, this.model, getSlopeSelection(), getMaxCheckInterval(),
                getThreshold(), getActionAtEntry(), getActionAtExit(), isRemoveAtEntry(), isRemoveAtExit());

        } else {
            // Occulting radius provider is not an instance of ConstantRadiusProvider (but of
            // VariableRadiusProvider)
            result = new BodyInEclipseDetector(this.targetBody, this.targetRadius,
                getOcculted(), getOccultedRadius(), getOccultingBodyShape(), isTotalEclipse(),
                this.bodyFullyInEclipse, this.model, getSlopeSelection(), getMaxCheckInterval(),
                getThreshold(), getActionAtEntry(), getActionAtExit(), isRemoveAtEntry(), isRemoveAtExit());
        }

        result.setPropagationDelayType(getPropagationDelayType(), getInertialFrame());
        return result;
    }

}
