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
 * @history creation 12/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D infinite rectangle cylinder, with some algorithm to compute intersections and
 * distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with its dimensions and three Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0); Vector3D
 *            direction = new Vector3D(6.0, -3.0, -1.0); Vector3D uVector = new Vector3D(-5.0, 3.0, 0.0); double length
 *            = 2.0; double width = 3.0; InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(origin,
 *            direction, uVector, length, width); Creation with a line, a Vector3D (u vector) and the dimensions :
 *            InfiniteRectangleCylinder cylinder = new InfiniteRectangleCylinder(line, uVector, length, width);
 *            Intersection with a line : boolean intersects = cylinder(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRectangleCylinder.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public final class InfiniteRectangleCylinder implements InfiniteCylinder {

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Position of the origin of the axis */
    private final Vector3D origin;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final Vector3D direction;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final double[] directionDbl;

    /** U vector of the local frame */
    private final Vector3D u;

    /** V vector of the local frame */
    private final Vector3D v;

    /** dimension on the U axis */
    private final double length;

    /** dimension on the V axis */
    private final double width;

    /** Side dimension */
    private final double[] sideDimension;

    /** Side edges */
    private final Line[] edges;

    /** Position of the origin of each side */
    private final double[][] sideOriginDbl;

    /** Normal vector of each side */
    private final double[][] sideNormalDbl;

    /**
     * Build an infinite rectangle cylinder from its dimensions, orientation and the origin and direction of its axis
     * 
     * @param inDirection
     *        the direction (middle axis of the cylinder)
     * @param inUVector
     *        the approximative U vector of the frame : corrected to be orthogonal to the direction
     * @param inLength
     *        dimension on the U axis
     * @param inWidth
     *        dimension on the V axis
     * @exception IllegalArgumentException
     *            if one of the dimensions is negative or null, or if the direction vector has a null norm.
     * */
    public InfiniteRectangleCylinder(final Line inDirection, final Vector3D inUVector, final double inLength,
        final double inWidth) {
        this(inDirection.getOrigin(), inDirection.getDirection(), inUVector, inLength, inWidth);
    }

    /**
     * Build an infinite rectangle cylinder from its dimensions, orientation and the origin and direction of its axis
     * 
     * @param inOrigin
     *        the origin of the axis
     * @param inDirection
     *        the direction of the axis
     * @param inUvector
     *        the approximative U vector of the frame
     * @param inLength
     *        dimension on the U axis
     * @param inWidth
     *        dimension on the V axis
     * @exception IllegalArgumentException
     *            if one of the dimensions is negative or null, or if the direction vector has a null norm.
     * */
    public InfiniteRectangleCylinder(final Vector3D inOrigin, final Vector3D inDirection, final Vector3D inUvector,
        final double inLength, final double inWidth) {

        // test of the dimensions
        final String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());
        if (inLength < 0.0 || Precision.equals(inLength, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        if (inWidth < 0.0 || Precision.equals(inWidth, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        // The direction's norm must be positive and the U vector can't be parallel to the axis
        // The e-10 epsilon is chosen to be coherent with the Plane class
        if (Vector3D.crossProduct(inUvector, inDirection).getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }

        this.origin = new Vector3D(1.0, inOrigin);
        this.direction = inDirection.normalize();

        this.length = inLength;
        this.width = inWidth;

        this.directionDbl = new double[3];
        this.directionDbl[0] = this.direction.getX();
        this.directionDbl[1] = this.direction.getY();
        this.directionDbl[2] = this.direction.getZ();

        // creation of the right U and V vectors
        this.u =
            inUvector.subtract(new Vector3D(Vector3D.dotProduct(this.direction, inUvector), this.direction))
                .normalize();
        this.v = Vector3D.crossProduct(this.direction, this.u);

        // Normal vector of each side
        this.sideNormalDbl = new double[5][3];
        this.sideNormalDbl[0][0] = this.u.getX();
        this.sideNormalDbl[0][1] = this.u.getY();
        this.sideNormalDbl[0][2] = this.u.getZ();
        this.sideNormalDbl[1][0] = this.v.getX();
        this.sideNormalDbl[1][1] = this.v.getY();
        this.sideNormalDbl[1][2] = this.v.getZ();
        this.sideNormalDbl[2][0] = -this.u.getX();
        this.sideNormalDbl[2][1] = -this.u.getY();
        this.sideNormalDbl[2][2] = -this.u.getZ();
        this.sideNormalDbl[3][0] = -this.v.getX();
        this.sideNormalDbl[3][1] = -this.v.getY();
        this.sideNormalDbl[3][2] = -this.v.getZ();
        this.sideNormalDbl[4][0] = this.u.getX();
        this.sideNormalDbl[4][1] = this.u.getY();
        this.sideNormalDbl[4][2] = this.u.getZ();

        // Origin of each side
        this.sideOriginDbl = new double[4][3];
        Vector3D currentOrigin = this.origin.add(this.length / 2.0, this.u);
        this.sideOriginDbl[0][0] = currentOrigin.getX();
        this.sideOriginDbl[0][1] = currentOrigin.getY();
        this.sideOriginDbl[0][2] = currentOrigin.getZ();
        currentOrigin = this.origin.add(-this.length / 2.0, this.u);
        this.sideOriginDbl[1][0] = currentOrigin.getX();
        this.sideOriginDbl[1][1] = currentOrigin.getY();
        this.sideOriginDbl[1][2] = currentOrigin.getZ();
        currentOrigin = this.origin.add(this.width / 2.0, this.v);
        this.sideOriginDbl[2][0] = currentOrigin.getX();
        this.sideOriginDbl[2][1] = currentOrigin.getY();
        this.sideOriginDbl[2][2] = currentOrigin.getZ();
        currentOrigin = this.origin.add(-this.width / 2.0, this.v);
        this.sideOriginDbl[3][0] = currentOrigin.getX();
        this.sideOriginDbl[3][1] = currentOrigin.getY();
        this.sideOriginDbl[3][2] = currentOrigin.getZ();

        // creation of the side edges
        this.edges = new Line[4];
        Vector3D origmod = this.origin.add(new Vector3D(this.length / 2.0, this.u, this.width / 2.0, this.v));
        this.edges[0] = new Line(origmod, origmod.add(this.direction));
        origmod = this.origin.add(new Vector3D(-this.length / 2.0, this.u, this.width / 2.0, this.v));
        this.edges[1] = new Line(origmod, origmod.add(this.direction));
        origmod = this.origin.add(new Vector3D(-this.length / 2.0, this.u, -this.width / 2.0, this.v));
        this.edges[2] = new Line(origmod, origmod.add(this.direction));
        origmod = this.origin.add(new Vector3D(this.length / 2.0, this.u, -this.width / 2.0, this.v));
        this.edges[3] = new Line(origmod, origmod.add(this.direction));

        // side dimensions
        this.sideDimension = new double[4];
        this.sideDimension[0] = this.width;
        this.sideDimension[1] = this.length;
        this.sideDimension[2] = this.width;
        this.sideDimension[3] = this.length;

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
     * @return the u vector of the frame
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the v vector of the frame
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return this.length;
    }

    /**
     * @return the width
     */
    public double getWidth() {
        return this.width;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        return this.getIntersectionPoints(line).length > 0;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // Initialisations
        Vector3D[] intersections = new Vector3D[0];
        final Vector3D originLine = line.getOrigin();
        final Vector3D directionLine = line.getDirection();
        final double[] originLineDbl = { originLine.getX(), originLine.getY(), originLine.getZ() };
        final double[] directionLineDbl = { directionLine.getX(), directionLine.getY(), directionLine.getZ() };

        // test of the parallelism :
        // Cross product of the line's direction and the direction of the cylinder.
        // There is no intersection if the result is of a zero norm.
        final double[] crossProdDir =
        { directionLineDbl[1] * this.directionDbl[2] - directionLineDbl[2] * this.directionDbl[1],
            directionLineDbl[2] * this.directionDbl[0] - directionLineDbl[0] * this.directionDbl[2],
            directionLineDbl[0] * this.directionDbl[1] - directionLineDbl[1] * this.directionDbl[0] };

        final double crossProdNorm = crossProdDir[0] * crossProdDir[0] + crossProdDir[1] * crossProdDir[1]
            + crossProdDir[2] * crossProdDir[2];

        // direction not parallel to the axis :
        // projection of the line on each plane defined by a side
        // and test of the coordinates of the result
        if (!(MathLib.abs(crossProdNorm) < Precision.DOUBLE_COMPARISON_EPSILON)) {
            intersections = new Vector3D[0];

            int currentLength = 0;
            Vector3D[] temp;
            Vector3D intersectionWithSide;

            for (int i = 0; i < 4; i++) {

                intersectionWithSide = null;

                // search of an intersection on the current side :
                // Dot product of the line's direction and the normal vector of the side.
                // There is no intersection if the result is zero.
                final double dotProdDirW = directionLineDbl[0] * this.sideNormalDbl[i][0] + directionLineDbl[1]
                    * this.sideNormalDbl[i][1] + directionLineDbl[2] * this.sideNormalDbl[i][2];

                if (MathLib.abs(dotProdDirW) > VECTORSCOMPARISONEPS) {

                    final double distOriginToPlane =
                        (originLineDbl[0] - this.sideOriginDbl[i][0]) * this.sideNormalDbl[i][0]
                            + (originLineDbl[1] - this.sideOriginDbl[i][1]) * this.sideNormalDbl[i][1]
                            + (originLineDbl[2] - this.sideOriginDbl[i][2]) * this.sideNormalDbl[i][2];

                    // Computation of the intersection point of the line and the plane
                    final double alpha = -MathLib.divide(distOriginToPlane, dotProdDirW);
                    final double[] intersectionWithPlane = { originLineDbl[0] + alpha * directionLineDbl[0],
                        originLineDbl[1] + alpha * directionLineDbl[1],
                        originLineDbl[2] + alpha * directionLineDbl[2] };

                    // Computation of the width coordinate of this point in the local frame of the side.
                    final double coordToTest = MathLib.abs((intersectionWithPlane[0] - this.sideOriginDbl[i][0])
                        * this.sideNormalDbl[i + 1][0] + (intersectionWithPlane[1] - this.sideOriginDbl[i][1])
                        * this.sideNormalDbl[i + 1][1] + (intersectionWithPlane[2] - this.sideOriginDbl[i][2])
                        * this.sideNormalDbl[i + 1][2]);

                    // Test of this coordinate : if the intersection point belongs to the side,
                    // it will be added to the list
                    if (coordToTest < this.sideDimension[i] / 2.0) {
                        intersectionWithSide = new Vector3D(intersectionWithPlane[0], intersectionWithPlane[1],
                            intersectionWithPlane[2]);
                    }
                }

                // if an intersection is found, the point is added
                // to the list.
                if (intersectionWithSide != null) {
                    currentLength = currentLength + 1;
                    temp = intersections;
                    intersections = new Vector3D[currentLength];
                    System.arraycopy(temp, 0, intersections, 0, currentLength - 1);
                    intersections[currentLength - 1] = intersectionWithSide;
                }

            }
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {

        double distance;

        // if there is no intersection with the shape
        if (this.intersects(line)) {
            // if an intersection is found, the distance is 0.0
            distance = 0.0;
        } else {
            // if the line is parallel to the axis
            if (Vector3D.crossProduct(line.getDirection(), this.direction).getNorm() < VECTORSCOMPARISONEPS) {

                // call to the right private method
                distance = this.distanceToParallelLine(line);
            } else {
                // if the line is not parallel to the axis, the shortest distance
                // is to a point from a side axis
                distance = Double.POSITIVE_INFINITY;
                for (int i = 0; i < 4; i++) {
                    distance = MathLib.min(distance, this.edges[i].distance(line));
                }
            }
        }

        return distance;
    }

    /**
     * Computes the distance from the cylinder to a line supposed to be parallel to its axis.
     * 
     * @param line
     *        line parallel to this
     * @return the distance to the line
     */
    private double distanceToParallelLine(final Line line) {
        double distance;

        final Vector3D originToOrigin = line.getOrigin().subtract(this.origin);

        final double testedLength = MathLib.abs(Vector3D.dotProduct(originToOrigin, this.u));
        final double testedWidth = MathLib.abs(Vector3D.dotProduct(originToOrigin, this.v));

        // the point must be the closest to side
        final double lengthDiff = testedLength - this.length / 2.0;
        final double widthDiff = testedWidth - this.width / 2.0;

        // if the origin is closest to an edge
        if (0 < widthDiff && 0 < lengthDiff) {
            distance = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                distance = MathLib.min(distance, this.edges[i].distance(line));
            }
        } else if (0 < widthDiff && 0 > lengthDiff) {
            // if the origin is closest to a side
            distance = widthDiff;
        } else if (0 > widthDiff && 0 < lengthDiff) {
            distance = lengthDiff;
        } else {
            // if the line is inside the cylinder, the distance is negative
            distance = MathLib.max(widthDiff, lengthDiff);
        }

        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        // if there is no intersection with the shape
        if (this.intersects(line)) {
            points[0] = this.getIntersectionPoints(line)[0];
            points[1] = points[0];
        } else {
            // if the line is parallel to the axis
            if (Vector3D.crossProduct(line.getDirection(), this.direction).getNorm() < VECTORSCOMPARISONEPS) {

                points = this.closestPointToSide(line);
            } else {
                // if the line is not parallel to the axis, the shortest distance
                // is to a point from an edge
                double distance = Double.POSITIVE_INFINITY;
                double currentDist;
                for (int i = 0; i < 4; i++) {
                    currentDist = this.edges[i].distance(line);
                    if (currentDist < distance) {
                        points = this.edges[i].closestPointTo(line);
                        distance = currentDist;
                    }
                }
            }
        }

        return points;
    }

    /**
     * Computes the closest point to a line to the axis
     * 
     * @param line
     *        the line
     * @return the closest point of the cylinder
     */
    private Vector3D[] closestPointToSide(final Line line) {
        final Vector3D[] points = new Vector3D[2];
        final Vector3D originToOrigin = line.getOrigin().subtract(this.origin);

        final double testedLength = Vector3D.dotProduct(originToOrigin, this.u);
        final double testedWidth = Vector3D.dotProduct(originToOrigin, this.v);

        // the point must be the closest to side
        final double lengthDiff = MathLib.abs(testedLength) - this.length / 2.0;
        final double widthDiff = MathLib.abs(testedWidth) - this.width / 2.0;

        // if the origin is closest to an edge
        if (0 < widthDiff && 0 < lengthDiff) {
            double distance = Double.POSITIVE_INFINITY;
            double currentDist;

            // the considered edge is the one realizing the shortest distance
            for (int i = 0; i < 4; i++) {
                currentDist = this.edges[i].distance(line);
                if (currentDist < distance) {
                    // the closest point from the line is its origin
                    points[0] = line.getOrigin();
                    points[1] = this.edges[i].toSpace(line.toSubSpace(points[0]));
                    distance = currentDist;
                }
            }
        } else if (lengthDiff > widthDiff) {
            // if the origin is closest to a side
            // if the closest point belongs to the a side in U direction
            final Vector3D sidePoint =
                new Vector3D(MathLib.signum(testedLength) * this.length / 2.0, this.u, testedWidth, this.v);
            points[1] = this.origin.add(sidePoint);
            points[0] = line.toSpace(line.toSubSpace(points[1]));
        } else {
            // if the closest point belongs to the a side in V direction
            final Vector3D sidePoint =
                new Vector3D(testedLength, this.u, MathLib.signum(testedWidth) * this.width / 2.0, this.v);
            points[1] = this.origin.add(sidePoint);
            points[0] = line.toSpace(line.toSubSpace(points[1]));
        }

        return points;
    }

    /**
     * Get a representation for this infinite rectangle cylinder.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this infinite rectangle cylinder
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
        res.append(this.origin.toString());
        res.append(comma);
        // "Direction":
        res.append("Direction");
        res.append(this.direction.toString());
        res.append(comma);
        // "U vector":
        res.append("U vector");
        res.append(this.u.toString());
        res.append(comma);
        // "Length":
        res.append("Length").append(open);
        res.append(this.length).append(close);
        res.append(comma);
        // "Width":
        res.append("Width").append(open);
        res.append(this.width).append(close);
        res.append(close);

        return res.toString();
    }

}
