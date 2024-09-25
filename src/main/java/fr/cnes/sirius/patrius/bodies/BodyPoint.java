/**
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
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for a point linked to a body. This point does not make any assumption about the shape of the body.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.12
 */
public interface BodyPoint extends PVCoordinatesProvider {

    /**
     * Get the {@link BodyShape} associated to this body point.
     *
     * @return {@link BodyShape} associated to this body point
     */
    public BodyShape getBodyShape();

    /**
     * Getter for the name of the point.
     *
     * @return the name of the point.
     */
    public String getName();

    /**
     * Get the point cartesian position expressed in body frame.
     *
     * @return point cartesian position in body frame
     */
    public Vector3D getPosition();

    /**
     * Get the normal direction, expressed in body frame, to the local horizontal plane at the point position, oriented
     * towards the outside.<br>
     * <br>
     * It corresponds to the direction (apart from orientation) from this towards the <i>closestPointOnShape</i>.
     *
     * @return unit vector in the normal direction
     */
    public Vector3D getNormal();

    /**
     * Returns the normal height, computed as the signed distance between the <i>closestPointOnShape</i> and this:
     * <ul>
     * <li>Positive distance if this is outside the shape,</li>
     * <li>Negative distance if this is inside the shape,</li>
     * <li>Null distance if this is on the shape.</li>
     * </ul>
     *
     * @return the normal height
     */
    public double getNormalHeight();

    /**
     * Returns the closest point to this on the shape surface.
     *
     * @return the closest point to this on the shape surface
     */
    public BodyPoint getClosestPointOnShape();

    /**
     * Returns the body point, on the associated shape surface, in the radial direction corresponding to the position of
     * this: if several of them (may happen for not star-convex shapes), the method considers the one farthest to the
     * body frame origin (having the largest norm).<br>
     * <b><u>Warnings</u></b>: the returned point is not necessary the closest point belonging to the shape in the
     * radial direction.
     *
     * @return the projection of this on the associated shape surface along the radial direction
     */
    public BodyPoint getRadialProjectionOnShape();

    /**
     * Returns true if the point is located inside the shape, false otherwise. A point lying on the surface of the shape
     * is considered to be in the shape.
     *
     * @return {@code true} if the point is located inside or on the shape, {@code false} otherwise
     */
    public default boolean isInsideShape() {
        return getNormalHeight() < getBodyShape().getDistanceEpsilon();
    }

    /**
     * Returns true if point is on body shape surface (i.e. its normal height is smaller than the body shape altitude
     * epsilon), false otherwise.
     *
     * @return {@code true} if point is on body shape surface, {@code false otherwise
     */
    public default boolean isOnShapeSurface() {
        return MathLib.abs(getNormalHeight()) < getBodyShape().getDistanceEpsilon();
    }

    /**
     * Returns (after computation if not computed yet) the latitude/longitude/height coordinates of this in the
     * associated body shape preferred coordinates system.
     *
     * @return the lat/long/height coordinates as a {@link LLHCoordinates}
     */
    public default LLHCoordinates getLLHCoordinates() {
        return getLLHCoordinates(getBodyShape().getLLHCoordinatesSystem());
    }

    /**
     * Returns (after computation if not computed yet) the latitude/longitude/height coordinates of this in the
     * requested coordinates system.
     *
     * @param coordSystem
     *        requested coordinates system
     * @return the lat/long/height coordinates as a {@link LLHCoordinates}
     * @throws IllegalArgumentException
     *         if {@link LLHCoordinatesSystem#ELLIPSODETIC} with a not ellipsoidal body shape
     */
    public LLHCoordinates getLLHCoordinates(final LLHCoordinatesSystem coordSystem);

    /**
     * Compute the cartesian distance, in meters, between the position of this and this of provided point.
     *
     * @param otherPoint
     *        other body point
     * @return the angular distance
     */
    public default double distance(final BodyPoint otherPoint) {
        return Vector3D.distance(getPosition(), otherPoint.getPosition());
    }

    /**
     * Compute the angular separation, in radians, between the position of this and this of provided point, from the
     * body frame origin.
     *
     * @param otherPoint
     *        other body point
     * @return the angular distance
     */
    public default double angularSeparation(final BodyPoint otherPoint) {
        return Vector3D.angle(getPosition(), otherPoint.getPosition());
    }

    /** {@inheritDoc} */
    @Override
    public default PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        final PVCoordinates pvInBodyFrame = new PVCoordinates(getPosition(), Vector3D.ZERO, Vector3D.ZERO);
        return getNativeFrame(date).getTransformTo(frame, date).transformPVCoordinates(pvInBodyFrame);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unused")
    @Override
    public default Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return getBodyShape().getBodyFrame();
    }

    /**
     * Returns the normal direction expressed in the provided frame.
     *
     * @param date
     *        date
     * @param frame
     *        frame in which the normal direction must be expressed
     * @return the normal direction expressed in the provided frame
     * @throws PatriusException
     *         if frame transformation cannot be computed
     */
    public default Vector3D getNormal(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return getBodyShape().getBodyFrame().getTransformTo(frame, date).transformVector(getNormal());
    }

    /**
     * Getter for a string representing the object using the entered coordinates system.<br>
     * <u>Note</u>: the method may induce some computation if the coordinates have not been computed yet.
     *
     * @param coordSystem
     *        coordinates system to be used
     * @return a string representing the object
     */
    public String toString(final LLHCoordinatesSystem coordSystem);

    /** Class allowing to manage the default body point names. */
    public static class BodyPointName {

        /** Default name. */
        public static final String DEFAULT = "point";

        /** Name for an intersection point at null altitude. */
        public static final String INTERSECTION = "intersection";

        /** Name for an intersection point at not null altitude. */
        public static final String INTERSECTION_AT_ALTITUDE = "intersectionAtAltitude";

        /** Name for closest point on shape. */
        public static final String CLOSEST_ON_SHAPE = "closestOnShape";

        /** Name for radial point on shape. */
        public static final String RADIAL_ON_SHAPE = "radialProjection";

        /** Name for closest point on a line. */
        public static final String CLOSEST_ON_LINE = "closestOnLine";

        /** List of body point names. */
        public static final List<String> NAMES_LIST = Collections.unmodifiableList(Arrays.asList(new String[] {
            DEFAULT, INTERSECTION, INTERSECTION_AT_ALTITUDE, CLOSEST_ON_SHAPE, RADIAL_ON_SHAPE }));

        /**
         * Private constructor
         */
        private BodyPointName() {
            // Nothing to initialize
        }

        /**
         * Build a string with the following rule:
         * <ul>
         * <li>if the origin name is contained in the {@link BodyPointName#NAMES_LIST names list}, return the final name
         * </li>
         * <li>otherwise, concatenate the two names with this format: {@code originName_finalName}</li>
         * </ul>
         * 
         * @param originName
         *        Origin name
         * @param finalName
         *        Final name
         * @return the output name
         */
        public static String join(final String originName, final String finalName) {
            final String outputName;
            if (NAMES_LIST.contains(originName)) {
                outputName = finalName;
            } else {
                outputName = originName + "_" + finalName;
            }
            return outputName;
        }
    }
}
