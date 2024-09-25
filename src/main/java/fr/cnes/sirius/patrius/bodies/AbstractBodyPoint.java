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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * VERSION:4.12:DM:DM-7:17/08/2023:[PATRIUS] Symétriser les méthodes closestPointTo de BodyShape
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.EnumMap;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Abstract class for a point linked to a body. This point does not make any assumption about the shape of the body.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 *
 * @see BodyPoint
 *
 * @author Emmanuel Bignon
 *
 * @since 4.12
 */
public abstract class AbstractBodyPoint implements BodyPoint {

    /** Serializable UID. */
    private static final long serialVersionUID = -3424485348634647971L;

    /** Associated body shape. */
    private final BodyShape bodyShape;

    /** Name of the point. */
    private final String name;

    /** Flag stating if the point is known to be on the shape or not (used for serialization). */
    private final boolean onShapeSurface;

    /** Point cartesian position in body frame. */
    private Vector3D position;

    /** Normal direction. */
    private transient Vector3D normal;

    /** Normal height, which corresponds to the signed distance to shape. */
    private double normalHeight = Double.NaN;

    /** Closest point on shape surface. */
    private transient BodyPoint closestPointOnShape;

    /** Radial projection point on shape surface. */
    private transient BodyPoint radialProjectionOnShape;

    /** Input LLH coordinates (may be <code>null</code> if this is defined from its position) */
    private LLHCoordinates inputCoord;

    /** Map of LLH coordinates for all coordinates systems. */
    private final EnumMap<LLHCoordinatesSystem, LLHCoordinates> coordinatesMap;

    /**
     * Constructor from lat/long/height coordinates.
     *
     * @param bodyShape
     *        body shape on which the point is defined, expected ellipsoidal if the {@link LatLongSystem#ELLIPSODETIC}
     *        system is used
     * @param coordIn
     *        lat/long/height coordinates associated with the coordinates system in which they are expressed
     * @param name
     *        name of the point
     * @throws IllegalArgumentException
     *         if the input coordinates use the {@link LLHCoordinatesSystem#ELLIPSODETIC} system and bodyShape is not an
     *         ellipsoid
     */
    protected AbstractBodyPoint(final BodyShape bodyShape, final LLHCoordinates coordIn, final String name) {
        // Raise an exception if the body shape is not an ellipsoid and input LLH coordinates use the PLANETODETIC
        // system
        if (coordIn.getLLHCoordinatesSystem() == LLHCoordinatesSystem.ELLIPSODETIC
                && !(bodyShape instanceof EllipsoidBodyShape)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.ELLIPSODETIC_ONLY_ON_ELLIPSOIDS);
        }

        // Body shape
        this.bodyShape = bodyShape;

        // Name of the point
        this.name = name;

        // LLH coordinates
        this.inputCoord = coordIn; // Will be used to compute the position (in getPosition())
        this.coordinatesMap = new EnumMap<>(LLHCoordinatesSystem.class);

        // if (MathLib.abs(coordIn.getHeight()) < getBodyShape().getAltitudeEpsilon()) {
        if (Double.compare(coordIn.getHeight(), 0.) == 0) {
            // this is on the shape surface

            // set normal height equal to 0 and closest and radial projections on shape equal to this
            setPointOnShapeSurface();
            this.onShapeSurface = true;

            // put the input coordinates in the coordinates map
            this.coordinatesMap.put(coordIn.getLLHCoordinatesSystem(), coordIn);

        } else {
            // Else input coordinates are not put in the map as they may be "not optimal"
            this.onShapeSurface = false;
        }
    }

    /**
     * Constructor.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        position expressed in the body frame
     * @param onShapeSurface
     *        flag stating if the point is known to be on the shape or not: if true, both <i>closestPointOnShape</i> and
     *        <i>radialPointOnShape</i> attribute are set equal to this, and <i>normalHeight</i> is set to 0
     * @param name
     *        name of the point
     */
    protected AbstractBodyPoint(final BodyShape bodyShape, final Vector3D position, final boolean onShapeSurface,
                                final String name) {
        // Body shape
        this.bodyShape = bodyShape;

        // position in body frame
        this.position = position;
        // Note: in this constructor, inputCoord isn't initialized (null), but it's not an issue as the position is
        // already "initialized" so it won't be called in the getPosition() method

        // Map of LLH coordinates
        this.coordinatesMap = new EnumMap<>(LLHCoordinatesSystem.class);

        // Name of the point
        this.name = name;

        this.onShapeSurface = onShapeSurface;
        if (onShapeSurface) {
            // This is known to be on the shape surface

            // Set normal height equal to 0 and closest and radial projections on shape equal to this
            setPointOnShapeSurface();
        }
    }

    /**
     * Set the normal height to 0 and the closest and radial projections equal to this.
     */
    private final void setPointOnShapeSurface() {
        // Set the closest point on shape equal to this
        this.closestPointOnShape = this;

        // Set the radial point on shape also equal to this
        this.radialProjectionOnShape = this;

        // Set the normal height to 0
        this.normalHeight = 0.;
    }

    /** {@inheritDoc} */
    @Override
    public BodyShape getBodyShape() {
        return this.bodyShape;
    }

    /** {@inheritDoc} */
    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *         if input coordinates use the {@link LLHCoordinatesSystem#ELLIPSODETIC} system and the body is not an
     *         ellipsoid
     */
    @Override
    public final Vector3D getPosition() {
        if (this.position == null) {
            // Position is null means that inputCoord cannot be null: compute the position from input coordinates
            this.position = computePosition(this.inputCoord);
        }
        return this.position;
    }

    /**
     * Method to compute position in body frame, to be implemented in each specific child class of
     * {@link AbstractBodyPoint}. Associated getter {@link #getPosition()} is implemented in this class in order to
     * handle lazy initialization of this attribute.
     *
     * @param coordIn
     *        coordinates used to instantiate this
     * @return position in body frame as {@link Vector3D}
     */
    private final Vector3D computePosition(final LLHCoordinates coordIn) {

        switch (coordIn.getLLHCoordinatesSystem()) {
            case ELLIPSODETIC:

                // Cast the body shape to retrieve the ellipsoid body
                final EllipsoidBodyShape ellispoidShape = (EllipsoidBodyShape) getBodyShape();

                // Compute the ellipsodetic position through the ellipsoid
                return ellispoidShape.computePositionFromEllipsodeticCoordinates(coordIn.getLatitude(),
                    coordIn.getLongitude(), coordIn.getHeight());

            case BODYCENTRIC_NORMAL:

                /* Compute pivot point on surface (at altitude 0) */

                // Pivot point is the radial point on the surface with entered bodycentric lat/long coordinates
                BodyPoint pivotOnSurface = getBodyShape().buildRadialPointOnShapeSurface(coordIn.getLatitude(),
                    coordIn.getLongitude());
                // This point is voluntarily not set as closest point on body surface as there may be several LLH
                // coordinates corresponding to this, with possibly different pivot points

                /* Shift along normal direction */

                // Height vector from pivotOnSurface
                Vector3D heightVector = pivotOnSurface.getNormal().scalarMultiply(coordIn.getHeight());

                // Expected position
                return pivotOnSurface.getPosition().add(heightVector);

            case BODYCENTRIC_RADIAL:

                /* Compute pivot point on surface (at altitude 0) */

                // Pivot point is the radial point on the surface with entered bodycentric lat/long coordinates
                pivotOnSurface = getBodyShape().buildRadialPointOnShapeSurface(coordIn.getLatitude(),
                    coordIn.getLongitude());
                // This point is voluntarily not set as radial point on body surface as there may be several LLH
                // coordinates corresponding to this, with possibly different pivot points

                /* Shift along radial direction */

                final Vector3D radialPositionOnSurface = pivotOnSurface.getPosition();

                // Height vector from pivotOnSurface
                heightVector = radialPositionOnSurface.normalize().scalarMultiply(coordIn.getHeight());

                // Expected position
                return radialPositionOnSurface.add(heightVector);

            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final Vector3D getNormal() {
        if (this.normal == null) {
            // Compute the normal direction once if not initialized
            this.normal = computeNormal().normalize();
        }
        return this.normal;
    }

    /**
     * Method to compute normal direction in body frame, to be implemented in each specific child class of
     * {@link AbstractBodyPoint}.
     * Associated getter {@link #getNormal()} is implemented in this class in order to handle lazy initialization of
     * this attribute. <br>
     * The returned vector does not need to be normalized (normalization managed by method {@link #getNormal()}).
     *
     * @return normal direction in body frame
     */
    protected abstract Vector3D computeNormal();

    /** {@inheritDoc} */
    @Override
    public final double getNormalHeight() {

        if (Double.isNaN(this.normalHeight)) {
            // Normal height not computed yet

            // Closest point on the shape surface
            final BodyPoint closestOnShape = getClosestPointOnShape();

            // Height signum: +1 if position outside the shape, -1 if inside the shape
            final double heightSignum = computePositionHeightCoordinateSignum(getPosition(), closestOnShape);

            // Signed height
            this.normalHeight = getPosition().distance(closestOnShape.getPosition()) * heightSignum;
        }
        return this.normalHeight;
    }

    /** {@inheritDoc} */
    @Override
    public BodyPoint getClosestPointOnShape() {

        if (this.closestPointOnShape == null) {

            // Initialize variable
            BodyPoint normalProjectionOnShape = null;

            // Closest point name
            final String closestPointName = BodyPointName.join(getName(), BodyPointName.CLOSEST_ON_SHAPE);

            normalProjectionOnShape = getBodyShape().closestPointTo(getPosition(), closestPointName);
            this.closestPointOnShape = normalProjectionOnShape;
        }
        return this.closestPointOnShape;
    }

    /**
     * Setter for the entered body point as closest point on shape.
     *
     * @param closestOnShape
     *        body point to be set
     */
    protected void setClosestPointOnShape(final BodyPoint closestOnShape) {
        this.closestPointOnShape = closestOnShape;
        if (this.closestPointOnShape == this) {
            // Point on shape: also set he radial point on shape equal to this and the normal height equal to 0
            setPointOnShapeSurface();
        }
    }

    /** {@inheritDoc} */
    @Override
    public BodyPoint getRadialProjectionOnShape() {

        if (this.radialProjectionOnShape == null) {
            // Radial projection name
            final String radialProjectionName = BodyPointName.join(getName(), BodyPointName.RADIAL_ON_SHAPE);

            // Line from body center to position of this
            final Line line = new Line(Vector3D.ZERO, getPosition(), Vector3D.ZERO);

            // Define the point used for intersection selection = position of this multiplied by a given scalar factor
            // in order to be outside the shape
            final Vector3D close = getPosition().normalize().scalarMultiply(
                2 * getBodyShape().getEncompassingSphereRadius());

            try {
                this.radialProjectionOnShape = getBodyShape().getIntersectionPoint(line, close,
                    getBodyShape().getBodyFrame(), null, radialProjectionName);
            } catch (final PatriusException e) {
                // Cannot happen, no frame conversion is performed
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
            }
        }
        return this.radialProjectionOnShape;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Also sets the normal height if the requested coordinates system has the {@link HeightSystem#NORMAL} system.
     * </p>
     *
     * @throws IllegalArgumentException
     *         if {@link LLHCoordinatesSystem#ELLIPSODETIC} with a not ellipsoidal body shape
     */
    @Override
    public final LLHCoordinates getLLHCoordinates(final LLHCoordinatesSystem coordSystem) {

        // Compute the coordinates if not computed yet
        if (this.coordinatesMap.get(coordSystem) == null) {

            // initialize output
            LLHCoordinates coord = null;
            switch (coordSystem) {

                case ELLIPSODETIC:
                    coord = computeEllipsodeticNormalLLHCoordinates();
                    break;

                case BODYCENTRIC_NORMAL:
                    coord = computeBodycentricNormalLLHCoordinates();
                    break;

                case BODYCENTRIC_RADIAL:
                    coord = computeBodycentricRadialLLHCoordinates();
                    break;

                default:
                    // NOTEST
            }
            this.coordinatesMap.put(coordSystem, coord);
        }
        return this.coordinatesMap.get(coordSystem);
    }

    /**
     * Getter for the the ellipsodetic/normal LLH coordinates.
     *
     * @return the ellipsodetic/normal LLH coordinates
     * @throws IllegalArgumentException
     *         if body shape is not an {@link EllipsoidBodyShape}
     */
    private LLHCoordinates computeEllipsodeticNormalLLHCoordinates() throws IllegalArgumentException {

        // Check the body shape is an ellipsoid, otherwise raise an exception
        if (!(getBodyShape() instanceof EllipsoidBodyShape)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.ELLIPSODETIC_ONLY_ON_ELLIPSOIDS);
        }

        // Cast the body shape to retrieve the ellipsoid parameters
        final EllipsoidBodyShape ellispoidShape = (EllipsoidBodyShape) getBodyShape();
        final IEllipsoid ellispoid = ellispoidShape.getEllipsoid();

        // Closest point on shape
        final Vector3D positionOnSurface = getClosestPointOnShape().getPosition();

        // Longitude
        final double longitude = positionOnSurface.getAlpha();

        // Latitude
        final Vector3D verticalVect = ellispoid.getNormal(positionOnSurface);
        final double latitude = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, verticalVect.getZ())));

        return new LLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC, latitude, longitude, getNormalHeight());
    }

    /**
     * Getter for the bodycentric/normal coordinates.
     *
     * @return the bodycentric/normal coordinates
     */
    private LLHCoordinates computeBodycentricNormalLLHCoordinates() {

        // Closest point on shape
        final BodyPoint closestOnShape = getClosestPointOnShape();
        final Vector3D closestPosition = closestOnShape.getPosition();

        // Bodycentric latitude and longitude
        return new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_NORMAL, closestPosition.getDelta(),
            closestPosition.getAlpha(), getNormalHeight());
    }

    /**
     * Getter for the bodycentric/radial coordinates.
     *
     * @return the bodycentric/radial coordinates
     */
    private LLHCoordinates computeBodycentricRadialLLHCoordinates() {

        // radial point on shape
        final BodyPoint radialOnShape = getRadialProjectionOnShape();
        final Vector3D radialPosition = radialOnShape.getPosition();

        // height sign: +1 if position outside the shape, -1 if inside the shape
        final double heightSign = computePositionHeightCoordinateSignum(getPosition(), getClosestPointOnShape());

        // signed height
        final double height = getPosition().distance(radialPosition) * heightSign;

        // bodycentric latitude and longitude
        return new LLHCoordinates(LLHCoordinatesSystem.BODYCENTRIC_RADIAL, radialPosition.getDelta(),
            radialPosition.getAlpha(), height);
    }

    /**
     * Return the expected signum for the position height coordinate.
     *
     * @param position
     *        The point cartesian position expressed in body frame
     * @param closestPointOnShape
     *        The closest point to this on the shape surface
     * @return the signum for the position height coordinate
     */
    protected static final double computePositionHeightCoordinateSignum(final Vector3D position,
                                                                        final BodyPoint closestPointOnShape) {
        // Normal height vector from closest point on shape to position
        final Vector3D normalHeightVector = position.subtract(closestPointOnShape.getPosition());
        return MathLib.signum(normalHeightVector.dotProduct(closestPointOnShape.getNormal()));
    }

    /**
     * {@inheritDoc}
     *
     * <u>Note</u>: the method may induce some computation if the coordinates have not been computed yet in the
     * associated body shape preferred coordinates system.
     */
    @Override
    public String toString() {
        return toString(this.bodyShape.getLLHCoordinatesSystem());
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final LLHCoordinatesSystem coordSystem) {
        return String.format("%s: name='%s', %s, body='%s'", getClass().getSimpleName(), getName(),
            getLLHCoordinates(coordSystem).toString(), getBodyShape().getName());
    }

    /**
     * Custom deserialization is needed.
     *
     * @param stream
     *        Object stream
     * @throws IOException
     *         if an I/O error occurs
     * @throws ClassNotFoundException
     *         if the class of a serialized object cannot be found
     */
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
        // manually set the point on shape surface if needed
        if (this.onShapeSurface) {
            setPointOnShapeSurface();
        }
    }
}
