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
 * @history created 27/02/12
 *
 *
 * HISTORY
* VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-17:22/05/2023:[PATRIUS] Detecteur de distance a la surface d'un corps celeste
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Detects when the distance from the spacecraft to the surface of a given body reaches a predetermined value.
 * <p>
 * The default implementation behaviour is to {@link EventDetector.Action#STOP stop} propagation when the distance is
 * reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment attributes are mutable and related to propagation.
 * 
 * @see {@link DistanceDetector}
 * 
 * @author Florian Teilhard
 * 
 * @since 4.11
 */
public class SurfaceDistanceDetector extends DistanceDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 2565133119918956535L;

    /** Body distance type. */
    public enum BodyDistanceType {
        /** Distance to the closest point of the body surface. */
        CLOSEST,

        /** Distance from the closest point of the body surface aligned with the spacecraft and the body centrer. */
        RADIAL;
    }

    /** The chosen type of body distance. */
    private final BodyDistanceType bodyDistanceType;

    /** The chosen type of body shape. */
    private final BodyShape bodyShape;

    /** The chosen type of body fixed frame. */
    private final Frame bodyFixedFrame;

    /**
     * Constructor for a DistanceDetector instance.
     * <p>
     * This simple constructor takes default values for maximal checking interval ({@link #DEFAULT_MAXCHECK}) and
     * convergence threshold ({@link #DEFAULT_THRESHOLD}).
     * </p>
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the distance is
     * reached.
     * </p>
     * <p>
     * The surfaceDistanceIn parameter must be positive.
     * </p>
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param surfaceDistanceIn
     *        Distance triggering the event (m)
     * @param bodyDistanceTypeIn
     *        Body distance type
     * @throws IllegalArgumentException
     *         if the distance is negative
     */
    public SurfaceDistanceDetector(final CelestialBody bodyIn, final double surfaceDistanceIn,
                                   final BodyDistanceType bodyDistanceTypeIn) {
        this(bodyIn, surfaceDistanceIn, bodyDistanceTypeIn, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
    }

    /**
     * Constructor for a DistanceDetector instance with additional maxCheck and threshold parameters
     * 
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when the distance is
     * reached.
     * </p>
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param surfaceDistanceIn
     *        Distance triggering the event (m)
     * @param bodyDistanceTypeIn
     *        Body distance type
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @throws IllegalArgumentException
     *         if the distance is negative
     */
    public SurfaceDistanceDetector(final CelestialBody bodyIn, final double surfaceDistanceIn,
                                   final BodyDistanceType bodyDistanceTypeIn, final double maxCheck,
                                   final double threshold) {
        this(bodyIn, surfaceDistanceIn, bodyDistanceTypeIn, maxCheck, threshold, Action.STOP, Action.STOP);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param surfaceDistanceIn
     *        Distance triggering the event (m)
     * @param bodyDistanceTypeIn
     *        Body distance type
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @param actionIncreasing
     *        Action performed at distance detection when distance is increasing
     * @param actionDecreasing
     *        Action performed at distance detection when distance is decreasing
     * @throws IllegalArgumentException
     *         if the distance is negative
     */
    public SurfaceDistanceDetector(final CelestialBody bodyIn, final double surfaceDistanceIn,
                                   final BodyDistanceType bodyDistanceTypeIn, final double maxCheck,
                                   final double threshold, final Action actionIncreasing,
                                   final Action actionDecreasing) {
        this(bodyIn, surfaceDistanceIn, bodyDistanceTypeIn, maxCheck, threshold, actionIncreasing, actionDecreasing,
                false, false);
    }

    /**
     * Constructor for a DistanceDetector instance with complimentary parameters.
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param surfaceDistanceIn
     *        Distance triggering the event (m)
     * @param bodyDistanceTypeIn
     *        Body distance type
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @param actionIncreasing
     *        Action performed at distance detection when distance is increasing
     * @param actionDecreasing
     *        Action performed at distance detection when distance is decreasing
     * @param removeIncreasing
     *        True if detector should be removed at increasing distance detection
     * @param removeDecreasing
     *        True if detector should be removed at decreasing distance detection
     * @throws IllegalArgumentException
     *         if the distance is negative
     */
    public SurfaceDistanceDetector(final CelestialBody bodyIn, final double surfaceDistanceIn,
                                   final BodyDistanceType bodyDistanceTypeIn, final double maxCheck,
                                   final double threshold, final Action actionIncreasing,
                                   final Action actionDecreasing, final boolean removeIncreasing,
                                   final boolean removeDecreasing) {
        super(bodyIn, surfaceDistanceIn, maxCheck, threshold, actionIncreasing, actionDecreasing, removeIncreasing,
                removeDecreasing);

        // Set body distance type
        this.bodyDistanceType = bodyDistanceTypeIn;
        // Set body shape
        this.bodyShape = bodyIn.getShape();
        // Set body fixed frame
        this.bodyFixedFrame = this.bodyShape.getBodyFrame();
    }

    /**
     * Build a new altitude detector with slope selection.
     * <p>
     * The maximal interval between altitude checks should be smaller than the half duration of the minimal pass to
     * handle, otherwise some short passes could be missed.
     * </p>
     * 
     * @param bodyIn
     *        The body whose distance is watched
     * @param surfaceDistanceIn
     *        Distance triggering the event (m)
     * @param bodyDistanceTypeIn
     *        Body distance type
     * @param slopeSelection
     *        The g-function slope selection (0,1 or 2)
     * @param maxCheck
     *        Maximal checking interval (s)
     * @param threshold
     *        Convergence threshold (s)
     * @param action
     *        Action performed at distance detection
     * @param remove
     *        True if detector should be removed at distance detection
     */
    public SurfaceDistanceDetector(final CelestialBody bodyIn, final double surfaceDistanceIn,
                                   final BodyDistanceType bodyDistanceTypeIn, final int slopeSelection,
                                   final double maxCheck, final double threshold, final Action action,
                                   final boolean remove) {
        super(bodyIn, surfaceDistanceIn, slopeSelection, maxCheck, threshold, action, remove);

        // Set body distance type
        this.bodyDistanceType = bodyDistanceTypeIn;
        // Set body shape
        this.bodyShape = bodyIn.getShape();
        // Set body fixed frame
        this.bodyFixedFrame = this.bodyShape.getBodyFrame();

    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * WARNING: at the moment, the g value is correctly computed only for EllipsoidBodyShape, not for FacetBodyShape.
     * </p>
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {
        // Get the position of the satellite (in the body frame)
        final Vector3D satPos = state.getPVCoordinates(this.bodyFixedFrame).getPosition();
        // Retrieve the date
        final AbsoluteDate date = state.getDate();
        // Define the result
        double result = 0.;
        // Compute the value depending on the body distance type
        if (this.bodyDistanceType.equals(BodyDistanceType.CLOSEST)) {
            // Compute the body point corresponding to the satellite position
            final BodyPoint satPosBodyPoint = this.bodyShape.buildPoint(satPos, this.bodyFixedFrame, date,
                BodyPointName.DEFAULT);
            // Compute the closest distance as the altitude (which can be negative) of the point point
            // corresponding to the satellite position
            final double closestDistance = satPosBodyPoint.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
                .getHeight();
            // Compute the result as the difference between the closest distance and the threshold distance
            result = closestDistance - this.distance;
        } else {
            // Create the line connecting the center of the body to the position of the satellite (in the body
            // frame)
            final Line line = new Line(Vector3D.ZERO, satPos);
            // Find the intersection point between the body surface (altitude equal to 0) and the line connecting
            // the center of the body to the position of the satellite (in the body frame)
            final BodyPoint intersectionBodyPoint = this.bodyShape.getIntersectionPoint(line, satPos,
                this.bodyFixedFrame, date);
            final Vector3D intersectionPoint = intersectionBodyPoint.getPosition();
            // Compute the distance between the satellite position and the intersection point (in the body frame)
            double radialDistance = intersectionPoint.distance(satPos);
            // If the satellite at the intern of the body shape, then the radial distance is negative
            if (satPos.getNorm() < intersectionPoint.getNorm()) {
                radialDistance = -radialDistance;
            }
            // Compute the result as the difference between the radial distance and the threshold distance
            result = radialDistance - this.distance;
        }

        // Return value
        return result;
    }

    /**
     * Getter for the body.
     * 
     * @return the body
     */
    @Override
    public CelestialBody getBody() {
        return (CelestialBody) this.body;
    }

    /**
     * Getter for the chosen body distance type for the detector.
     * 
     * @return the chosen body distance type for the detector
     */
    public BodyDistanceType getBodyDistanceType() {
        return this.bodyDistanceType;
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
     * Getter for the body fixed frame.
     * 
     * @return the body fixed frame
     */
    public Frame getBodyFixedFrame() {
        return this.bodyFixedFrame;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>body: {@link CelestialBody}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final EventDetector detector;
        if (this.getSlopeSelection() == INCREASING) {
            detector = new SurfaceDistanceDetector((CelestialBody) this.body, this.distance, this.bodyDistanceType,
                this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(),
                this.isRemoveAtEntry());
        } else if (this.getSlopeSelection() == DECREASING) {
            detector = new SurfaceDistanceDetector((CelestialBody) this.body, this.distance, this.bodyDistanceType,
                this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtExit(),
                this.isRemoveAtExit());
        } else {
            detector = new SurfaceDistanceDetector((CelestialBody) this.body, this.distance, this.bodyDistanceType,
                this.getMaxCheckInterval(), this.getThreshold(), this.getActionAtEntry(), this.getActionAtExit(),
                this.isRemoveAtEntry(), this.isRemoveAtExit());
        }
        return detector;
    }
}
