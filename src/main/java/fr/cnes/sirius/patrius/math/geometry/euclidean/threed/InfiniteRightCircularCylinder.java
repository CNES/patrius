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
 * @history creation 12/10/2011
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D infinite right circular cylinder, with some algorithm to compute intersections
 * and distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with a radius and two Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0); Vector3D
 *            direction = new Vector3D(6.0, -3.0, -1.0); double radius = 2.0; InfiniteRightCircularCylinder cylinder =
 *            new InfiniteRightCircularCylinder(origin, direction, radius); Creation with a line and a radius :
 *            InfiniteRightCircularCylinder cylinder = new InfiniteRightCircularCylinder(line, radius); Intersection
 *            with a line : boolean intersects = cylinder(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRightCircularCylinder.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class InfiniteRightCircularCylinder implements InfiniteCylinder, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -5163863575805965884L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Double comparison */
    private static final double EPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Position of the origin of the axis */
    private final Vector3D origin;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final Vector3D direction;

    /** Position of the origin of the axis */
    private final double[] originDoubles;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final double[] directionDoubles;

    /** Radius of the cylinder */
    private final double radius;

    /** U vector of the local frame */
    private final Vector3D u;

    /** rotation matrix to the local frame */
    private final Matrix3D rotationMatrix;

    /**
     * Build an infinite right circular cylinder from its radius and its axis as a line
     * 
     * @param inDirection
     *        the direction : axis of the cylinder
     * @param inRadius
     *        the radius of the cylinder
     * @exception IllegalArgumentException
     *            if the radius is negative or null, or if the direction vector has a null norm.
     * */
    public InfiniteRightCircularCylinder(final Line inDirection, final double inRadius) {
        this(inDirection.getOrigin(), inDirection.getDirection(), inRadius);
    }

    /**
     * Build an infinite right circular cylinder from its radius and the origin and direction of its axis
     * 
     * @param inOrigin
     *        the origin of the axis
     * @param inDirection
     *        the direction of the axis
     * @param inRadius
     *        the radius of the cylinder
     * @exception IllegalArgumentException
     *            if the radius is negative or null, or if the direction vector has a null norm.
     * */
    public InfiniteRightCircularCylinder(final Vector3D inOrigin, final Vector3D inDirection, final double inRadius) {

        // test of the radius
        final String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());

        if (inRadius < 0.0 || Precision.equals(inRadius, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        // The direction's norm must be positive
        // The e-10 epsilon is chosen to be coherent with the Plane class
        if (inDirection.getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }

        this.origin = new Vector3D(1.0, inOrigin);
        this.direction = inDirection.normalize();
        this.radius = inRadius;

        // creation of the vectors in doubles
        this.originDoubles = new double[3];
        this.originDoubles[0] = this.origin.getX();
        this.originDoubles[1] = this.origin.getY();
        this.originDoubles[2] = this.origin.getZ();

        this.directionDoubles = new double[3];
        this.directionDoubles[0] = this.direction.getX();
        this.directionDoubles[1] = this.direction.getY();
        this.directionDoubles[2] = this.direction.getZ();

        // creation of the local frame
        this.u = this.direction.orthogonal();
        final Vector3D v = Vector3D.crossProduct(this.direction, this.u);

        final double[][] matrixData =
        { { this.u.getX(), this.u.getY(), this.u.getZ() }, { v.getX(), v.getY(), v.getZ() },
            { this.direction.getX(), this.direction.getY(), this.direction.getZ() } };
        this.rotationMatrix = new Matrix3D(matrixData);
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the direction
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return this.radius;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        // there is intersection if the distance to the axis is lower than the radius
        final double[] distanceToAxis = this.distanceToAxis(line);
        // radius cannot be 0
        return (distanceToAxis[0] - this.radius) / this.radius < EPS && distanceToAxis[1] > 0.0;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        final Vector3D[] intersections;

        if (this.intersects(line)) {
            // computation of the line's parameters in the local frame
            final Vector3D lineDirectionVect = this.rotationMatrix.multiply(line.getDirection()).normalize();
            final Vector3D lineOriginVect = this.rotationMatrix.multiply(line.getOrigin().subtract(this.origin));

            final double oX = lineOriginVect.getX();
            final double oY = lineOriginVect.getY();
            final double dX = lineDirectionVect.getX();
            final double dY = lineDirectionVect.getY();

            // Terms of the second order equation
            // A can't be null because the line is not parallel to the axis
            final double a = dX * dX + dY * dY;
            final double b = 2 * (dX * oX + dY * oY);
            final double c = oX * oX + oY * oY - this.radius * this.radius;

            // solving
            final double det = b * b - 4 * a * c;

            // only one intersection point (the line is tangent to the surface)
            if (MathLib.abs(det) < EPS) {

                // the result is the position of the intersection point
                final double alpha = -b / (2 * a);
                final Vector3D point = lineOriginVect.add(alpha, lineDirectionVect);
                intersections = new Vector3D[1];
                intersections[0] = this.rotationMatrix.transposeAndMultiply(point).add(this.origin);

                // two intersection points (common case)
            } else {
                // the results are the positions of the intersection points
                final double alpha1 = (-b - MathLib.sqrt(det)) / (2 * a);
                final double alpha2 = (-b + MathLib.sqrt(det)) / (2 * a);

                final Vector3D point1 = lineOriginVect.add(alpha1, lineDirectionVect);
                final Vector3D point2 = lineOriginVect.add(alpha2, lineDirectionVect);

                intersections = new Vector3D[2];
                intersections[0] = this.rotationMatrix.transposeAndMultiply(point1).add(this.origin);
                intersections[1] = this.rotationMatrix.transposeAndMultiply(point2).add(this.origin);
            }
            // The determinant can't be negative, the intersection has already be tested

        } else {
            intersections = new Vector3D[0];
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {

        final double[] distanceToAxis = this.distanceToAxis(line);
        final double distance;

        // if there is intersection, the distance is zero
        if (distanceToAxis[0] < this.radius && distanceToAxis[1] > 0.0) {
            distance = 0.0;
        } else {
            // if no intersection is found, the distance is the one
            // to the axis minus the radius : negative if the line
            // is inside the cylinder
            distance = distanceToAxis[0] - this.radius;
        }

        return distance;
    }

    /**
     * Computes the shortest distance between a line and the axis.
     * 
     * @param line
     *        the line
     * @return the distance and a double negative if the line is parallel to the axis, positive otherwise
     */
    private double[] distanceToAxis(final Line line) {

        // Initialisations
        final Vector3D origLineVect = line.getOrigin();
        final Vector3D dirLineVect = line.getDirection();
        final double[] originLine = { origLineVect.getX(), origLineVect.getY(), origLineVect.getZ() };
        final double[] directionLine = { dirLineVect.getX(), dirLineVect.getY(), dirLineVect.getZ() };
        final double distance;
        double isParallel = 1.0;

        // normalised normal to both directions (line and cylinder's axis)
        final double[] normal =
        { this.directionDoubles[1] * directionLine[2] - this.directionDoubles[2] * directionLine[1],
            this.directionDoubles[2] * directionLine[0] - this.directionDoubles[0] * directionLine[2],
            this.directionDoubles[0] * directionLine[1] - this.directionDoubles[1] * directionLine[0] };

        final double normalNorm = MathLib.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);

        // computation of the vector between the two origins
        final double[] originToOrigin = { originLine[0] - this.originDoubles[0], originLine[1] - this.originDoubles[1],
            originLine[2] - this.originDoubles[2] };

        // the line is parallel to the axis...
        if (normalNorm < VECTORSCOMPARISONEPS) {

            // ... computation of the distance with the algorithm of the
            // distance(Vector3D) method of the Line class

            // dot product origin to origin * direction of the axis
            final double dotProd =
                originToOrigin[0] * this.directionDoubles[0] + originToOrigin[1] * this.directionDoubles[1]
                    + originToOrigin[2] * this.directionDoubles[2];

            // computation of the normal vector with another method
            final double[] n = { originToOrigin[0] - dotProd * this.directionDoubles[0],
                originToOrigin[1] - dotProd * this.directionDoubles[1],
                originToOrigin[2] - dotProd * this.directionDoubles[2] };

            // so the distance is the norm of it
            distance = MathLib.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);

            isParallel = -1.0;

        } else {
            // normalised normal vector
            // normalNorm cannot be 0
            normal[0] = normal[0] / normalNorm;
            normal[1] = normal[1] / normalNorm;
            normal[2] = normal[2] / normalNorm;

            // the distance is the projection of the origin to origin vector
            // on the normal vector
            distance = MathLib.abs(originToOrigin[0] * normal[0] + originToOrigin[1] * normal[1] + originToOrigin[2]
                * normal[2]);

        }

        return new double[] { distance, isParallel };
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        final Vector3D[] points = new Vector3D[2];

        // if no intersection, computation of the two points
        if (this.intersects(line)) {
            // if an intersection is found, the points are identical
            points[1] = this.getIntersectionPoints(line)[0];
            points[0] = points[1];
        } else {
            // ..test of the closest point of the line to the axis
            final Line axis = new Line(this.origin, this.origin.add(this.direction));
            final Vector3D[] closestPointsToAxis = axis.closestPointTo(line);

            // the line's closest point is the same as the one found for the axis
            points[0] = closestPointsToAxis[0];

            // closest point of the axis to the line
            final Vector3D projectionOnAxis = closestPointsToAxis[1];

            // the direction from the line's closest point to the cylinder's closest point
            // is the one to the axis' point.
            Vector3D pointToPointDirection = projectionOnAxis.subtract(points[0]);

            final Vector3D pointToPoint;

            // if the line IS the axis
            if (MathLib.abs(pointToPointDirection.getNorm()) < VECTORSCOMPARISONEPS) {
                pointToPoint = new Vector3D(this.radius, this.u);
            } else {

                // vector between the two points
                pointToPointDirection = pointToPointDirection.normalize();

                // computation of the distance between the two points
                final double distanceBetweenPoints = line.distance(axis) - this.radius;

                pointToPoint = new Vector3D(distanceBetweenPoints, pointToPointDirection);
            }

            // second point
            points[1] = points[0].add(pointToPoint);
        }

        return points;
    }

    /**
     * Get a representation for this infinite right circular cylinder.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this infinite right circular cylinder
     */
    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);
        // "Origin":
        res.append("Origin");
        res.append(this.origin.toString()).append(comma);
        // "Direction":
        res.append("Direction");
        res.append(this.direction.toString()).append(comma);
        // "Radius":
        res.append("Radius").append(open);
        res.append(this.radius).append(close);
        res.append(close);

        return res.toString();
    }
}
