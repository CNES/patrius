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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.AbstractBodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinates;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Point location relative to a 2D body surface of type {@link FacetBodyShape}.
 * <p>
 * Instance of this class are guaranteed to be immutable.
 * </p>
 *
 * @see AbstractBodyPoint
 *
 * @author Emmanuel Bignon
 *
 * @since 4.12
 */
public class FacetPoint extends AbstractBodyPoint {

    /** Serializable UID. */
    private static final long serialVersionUID = -3637953954378864186L;

    /** List of shape triangles closest to this. */
    private transient List<Triangle> closestTrianglesList;

    // CHECKSTYLE: stop LineLength check
    // Reason: Javaoc formatting

    /**
     * Constructor from lat/long/height coordinates.<br>
     * <b><u>Warning</u></b>: Be aware of following limitations when using an LLH coordinates system using the <i>normal
     * height</i> convention (typically the {@link LLHCoordinatesSystem#BODYCENTRIC_NORMAL} system):
     * <ul>
     * <li>Some regions of the space are not accessible with such a system: indeed, the accessible regions are limited
     * to the vertical lines from any point at the shape surface (lines intersecting the shape surface, with a direction
     * aligned with the local normal direction on the intersection point).</li>
     * <li>When the closest point on shape is either a vertex or placed at the boundary between two facets (on a shape
     * segment), an infinity of points share the same lat/long/normalHeight coordinates (see illustration below in 2D,
     * to be transposed in 3D).</i>
     * <li>Points on some regions of space can be built from several different lat/long/normal height coordinates.</li>
     * </ul>
     *
     * <pre>
     *       o v0 = vertex0                                  . &lt;-- normale to s01
     *     %% \                                         .
     *   %%%%% \                                   .
     * %%%%%%%% \ s01= segment between        .
     * %%%%%%%%% \     v0 and v1         .
     * %%%%%%%%%% \                 .     \
     * %%%%%%%%%%% \ _         .           | same {lat, long, normalHeight} coordinates on the portion of any sphere centered on v1
     * %%%%%%%%%%%% \ \   .                |
     * %%%%%%%%%%%%% o v1  .    .    .    / .     .      .      .      .      &lt;-- normale to s12
     * %%%%%%%%%%%%% |_|
     * %%%%%%%%%%%%% |
     * %%%%%%%%%%%%% |
     * %% INSIDE %%% | s12
     * %%  BODY  %%% |
     * %%%%%%%%%%%%% |
     * %%%%%%%%%%%%% |_
     * %%%%%%%%%%%%% | |
     * %%%%%%%%%%%%% o v2   .     .    .    .    .    .    .    .    .    .    &lt;-- normale to s12
     * %%%%%%%%%%%% /_/   .    .
     * %%%%%%%%%%% /           .           .
     * %%%%%%%%%% /                  .               .
     * %%%%%%%%% / s23                    .                    .  &lt;-- only the bisector is accessible to construction from {lat, long, normal height} coordinates
     * %%%%%%%% /                              .
     * %%%%%%% /                                    .
     *  %%%%% /                                         .
     *   %%% o v3                                            &lt;-- normale to s23
     *
     * </pre>
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param coordSystem
     *        LLH coordinates system in which are expressed the entered lat/long/height (must not be
     *        {@link LLHCoordinatesSystem#ELLIPSODETIC} otherwise an exception is raised)
     * @param latitude
     *        input latitude
     * @param longitude
     *        input longitude
     * @param height
     *        input height
     * @param name
     *        name of the point
     * @throws IllegalArgumentException
     *         if the input coordinates use the {@link LLHCoordinatesSystem#ELLIPSODETIC} system
     */
    public FacetPoint(final FacetBodyShape bodyShape, final LLHCoordinatesSystem coordSystem, final double latitude,
                      final double longitude, final double height, final String name) {
        this(bodyShape, new LLHCoordinates(coordSystem, latitude, longitude, height), name);
    }

    /**
     * Constructor from lat/long/height coordinates.<br>
     * <b><u>Warning</u></b>: Be aware of following limitations when using an LLH coordinates system using the <i>normal
     * height</i> convention (typically the {@link LLHCoordinatesSystem#BODYCENTRIC_NORMAL} system):
     * <ul>
     * <li>Some regions of the space are not accessible with such a system: indeed, the accessible regions are limited
     * to the vertical lines from any point at the shape surface (lines intersecting the shape surface, with a direction
     * aligned with the local normal direction on the intersection point).</li>
     * <li>When the closest point on shape is either a vertex or placed at the boundary between two facets (on a shape
     * segment), an infinity of points share the same lat/long/normalHeight coordinates (see illustration below in 2D,
     * to be transposed in 3D).</i>
     * <li>Points on some regions of space can be built from several different lat/long/normal height coordinates.</li>
     * </ul>
     *
     * <pre>
     *       o v0 = vertex0                                  . &lt;-- normale to s01
     *     %% \                                         .
     *   %%%%% \                                   .
     * %%%%%%%% \ s01= segment between        .
     * %%%%%%%%% \     v0 and v1         .
     * %%%%%%%%%% \                 .     \
     * %%%%%%%%%%% \ _         .           | same {lat, long, normalHeight} coordinates on the portion of any sphere centered on v1
     * %%%%%%%%%%%% \ \   .                |
     * %%%%%%%%%%%%% o v1  .    .    .    / .     .      .      .      .      &lt;-- normale to s12
     * %%%%%%%%%%%%% |_|
     * %%%%%%%%%%%%% |
     * %%%%%%%%%%%%% |
     * %% INSIDE %%% | s12
     * %%  BODY  %%% |
     * %%%%%%%%%%%%% |
     * %%%%%%%%%%%%% |_
     * %%%%%%%%%%%%% | |
     * %%%%%%%%%%%%% o v2   .     .    .    .    .    .    .    .    .    .    &lt;-- normale to s12
     * %%%%%%%%%%%% /_/   .    .
     * %%%%%%%%%%% /           .           .
     * %%%%%%%%%% /                  .               .
     * %%%%%%%%% / s23                    .                    .  &lt;-- only the bisector is accessible to construction from {lat, long, normal height} coordinates
     * %%%%%%%% /                              .
     * %%%%%%% /                                    .
     *  %%%%% /                                         .
     *   %%% o v3                                            &lt;-- normale to s23
     *
     * </pre>
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param coordIn
     *        lat/long/height coordinates associated with the coordinates system in which they are expressed
     * @param name
     *        name of the point
     * @throws IllegalArgumentException
     *         if the input coordinates use the {@link LLHCoordinatesSystem#ELLIPSODETIC} system
     */
    public FacetPoint(final FacetBodyShape bodyShape, final LLHCoordinates coordIn, final String name) {
        super(bodyShape, coordIn, name);
    }

    // CHECKSTYLE: resume LineLength check

    /**
     * Constructor from a position.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        point position in body frame
     * @param name
     *        name of the point
     */
    public FacetPoint(final FacetBodyShape bodyShape, final Vector3D position, final String name) {
        super(bodyShape, position, false, name);
    }

    /**
     * Constructor from a position expressed in any given frame.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        position in provided frame at provided date
     * @param frame
     *        frame
     * @param date
     *        date
     * @param name
     *        name of the point
     * @throws PatriusException
     *         if frame transformation cannot be computed at provided date
     */
    public FacetPoint(final FacetBodyShape bodyShape, final Vector3D position, final Frame frame,
                      final AbsoluteDate date, final String name)
        throws PatriusException {
        this(bodyShape, frame.getTransformTo(bodyShape.getBodyFrame(), date).transformPosition(position), name);
    }

    /**
     * Protected constructor from a position, a list of closest triangles and a flag stating if the point is known to be
     * on the shape: if the latter is set true, the normal height is set to 0 and the closest and radial projections on
     * the shape are set equal to this.
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param position
     *        point position in body frame
     * @param closestTriangles
     *        list of closest triangles
     * @param onTheShape
     *        flag stating if the point is known to be on the shape or not: if true, both <i>closestPointOnShape</i> and
     *        <i>radialPointOnShape</i> attribute are set equal to this, and <i>normalHeight</i> is set to 0
     * @param name
     *        name of the point
     */
    protected FacetPoint(final FacetBodyShape bodyShape, final Vector3D position,
                         final List<Triangle> closestTriangles, final boolean onTheShape, final String name) {
        super(bodyShape, position, onTheShape, name);
        this.closestTrianglesList = closestTriangles;
    }

    /**
     * Protected constructor from an {@link Intersection} object.
     * <p>
     * The built facet point is associated with one single triangle.
     * </p>
     *
     * @param bodyShape
     *        body shape on which the point is defined
     * @param intersection
     *        intersection defining both the position and the associated shape triangle
     * @param name
     *        name of the point
     */
    protected FacetPoint(final FacetBodyShape bodyShape, final Intersection intersection, final String name) {
        super(bodyShape, intersection.getPoint(), true, name);
        this.closestTrianglesList = Arrays.asList(new Triangle[] { intersection.getTriangle() });
    }

    /**
     * Get the list of shape triangles closest to this.
     *
     * @return the list of shape triangles closest to this
     */
    public List<Triangle> getClosestTriangles() {

        if (this.closestTrianglesList == null) {
            // Lazy initialization
            if (isOnShapeSurface()) {
                this.closestTrianglesList = computeClosestTriangles();
            } else {
                this.closestTrianglesList = getClosestPointOnShape().getClosestTriangles();
            }
        }
        return this.closestTrianglesList;
    }

    /**
     * Compute the triangles closest to this.
     *
     * @return the list of closest triangles
     */
    private List<Triangle> computeClosestTriangles() {

        final FacetBodyShape shape = getBodyShape();
        final List<Triangle> closestTriangles = new ArrayList<>();
        double minDist = Double.POSITIVE_INFINITY;
        // Loop on all triangles
        for (final Triangle t : shape.getTriangles()) {
            // Distance to triangle
            final double distance = t.distanceTo(getPosition());
            if (distance < minDist) {
                // New min distance found: becomes reference for min distance
                minDist = distance;
                closestTriangles.clear();
                closestTriangles.add(t);
            } else if (Double.compare(distance, minDist) == 0) {
                // Same min distance: store triangle
                closestTriangles.add(t);
            }
        }
        return closestTriangles;
    }

    /** {@inheritDoc} */
    @Override
    public FacetBodyShape getBodyShape() {
        // Body shape is necessarily a FacetBodyShape
        return (FacetBodyShape) super.getBodyShape();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned facet point is associated with one single triangle.
     * </p>
     */
    @Override
    public FacetPoint getRadialProjectionOnShape() {
        return (FacetPoint) super.getRadialProjectionOnShape();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned facet point is associated with one or several triangles following its position on the shape:
     * <ul>
     * <li>3 triangles or more if it corresponds to one shape vertex,</li>
     * <li>2 triangles if it belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     */
    @Override
    public FacetPoint getClosestPointOnShape() {
        return (FacetPoint) super.getClosestPointOnShape();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *         if the entered body point is not a {@link FacetPoint} instance
     */
    @Override
    protected void setClosestPointOnShape(final BodyPoint closestOnShape) {
        // Check if the entered body point is a FacetPoint instance
        if (!(closestOnShape instanceof FacetPoint)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.EXPECT_FACETPOINT_INSTANCE);
        }
        super.setClosestPointOnShape(closestOnShape);
    }

    /** {@inheritDoc} */
    @Override
    protected final Vector3D computeNormal() {

        // closest position on the shape surface: computed from the first closest triangle (no incidence on result)
        final Vector3D closestPositionOnShape = getClosestPointOnShape().getPosition();

        // compute the normal at shape surface which is equal to averaged normal to closest triangles
        Vector3D normalOnShape = Vector3D.ZERO;
        for (final Triangle triangle : getClosestTriangles()) {
            normalOnShape = normalOnShape.add(triangle.getNormal());
        }

        Vector3D normal;
        if (Vector3D.distance(getPosition(), closestPositionOnShape) < getBodyShape().getDistanceEpsilon()) {
            // Point on shape surface: normal is equal to averaged normal to closest triangles
            normal = normalOnShape;
        } else {
            // Point not on shape, normal is in the direction of position, oriented towards space
            normal = getPosition().subtract(closestPositionOnShape);

            if (normal.dotProduct(normalOnShape) < 0) {
                normal = normal.negate();
            }
        }

        // normal voluntarily not normalized as the normalization is performed in the method getNormal()
        return normal;
    }

    /**
     * Getter for a string representing the object using the entered coordinates system.<br>
     * <u>Note</u>: the method may induce some computation if the coordinates and/or the closest triangles have not been
     * computed yet.
     *
     * @param coordSystem
     *        coordinates system to be used
     * @return a string representing the object
     */
    @Override
    public String toString(final LLHCoordinatesSystem coordSystem) {

        // initialize a string builder
        final StringBuilder builder = new StringBuilder();

        // append the first closest triangle ID
        builder.append(getClosestTriangles().get(0).getID());

        // append all further closest triangles IDs
        for (int i = 1; i < getClosestTriangles().size(); i++) {
            builder.append(", ").append(getClosestTriangles().get(i).getID());
        }

        return String.format("%s: name='%s', %s, closest facets={%s}, body='%s'", getClass().getSimpleName(),
            getName(), getLLHCoordinates(coordSystem).toString(), builder.toString(), getBodyShape().getName());
    }
}
