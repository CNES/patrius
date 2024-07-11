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
 * @history creation 05/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
 * This is a describing class for a 3D rectangle plate shape, with some algorithm to compute intersections and distances
 * to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with three Vector3D and two doubles : Plate plate = new plate(center, vectorU, pseudoVectorV,
 *            length, width); Intersection with a line : boolean intersects = plate.intersects(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: Plate.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class Plate implements SolidShape, CrossSectionProvider, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -3489518699224406794L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Position of the center of the plate */
    private final Vector3D center;

    /** Position of the center of the plate as doubles */
    private final double[] centerTab;

    /** U vector of the Frame */
    private final Vector3D u;

    /** U vector of the Frame as doubles */
    private final double[] uTab;

    /** V vector of the Frame */
    private final Vector3D v;

    /** V vector of the Frame as doubles */
    private final double[] vTab;

    /** W vector of the Frame */
    private final Vector3D w;

    /** W vector of the Frame as doubles */
    private final double[] wTab;

    /** Dimension in the u direction */
    private final double length;

    /** Dimension in the v direction */
    private final double width;

    /** Corners of the rectangle */
    private final Vector3D[] corners;

    /** Edges of the rectangle */
    private final LineSegment[] edges;

    /**
     * Build a plate from the position of its center and two vectors to describe its local frame and dimensions.
     * 
     * @param inCenter
     *        the center of the plate
     * @param inU
     *        a non-normalised vector parallel to u
     * @param inV
     *        the second vector defining the plate's plane : corrected to be orthogonal to U
     * @param inLength
     *        the rectangle's length along U
     * @param inWidth
     *        the rectangle's width along V (once corrected)
     * @exception IllegalArgumentException
     *            if the vectors do not define a plane, or if the dimensions are negative or null
     * */
    public Plate(final Vector3D inCenter, final Vector3D inU, final Vector3D inV, final double inLength,
        final double inWidth) {
        final String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());

        // The vectors u and v must not be parallel
        // The e-10 epsilon is chosen to be coherent with the Plane class
        if (Vector3D.crossProduct(inV.normalize(), inU.normalize()).getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }
        if (inLength < 0.0 || Precision.equals(inLength, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        if (inWidth < 0.0 || Precision.equals(inWidth, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        this.center = new Vector3D(1.0, inCenter);
        this.length = inLength;
        this.width = inWidth;
        // inU.getNorm() cannot be 0
        this.u = new Vector3D(1.0 / inU.getNorm(), inU);

        // the v vector is obtained by removing to inV its projection on u to itself,
        // and then by normalising it.
        // In the end, v is normal to u, normalised, and "in the direction" of inV
        this.v = inV.subtract(new Vector3D(Vector3D.dotProduct(this.u, inV), this.u)).normalize();

        // The W vector is a cross product of u and v.
        this.w = Vector3D.crossProduct(this.u, this.v);

        // The attributes "as doubles" are filled
        this.uTab = new double[3];
        this.uTab[0] = this.u.getX();
        this.uTab[1] = this.u.getY();
        this.uTab[2] = this.u.getZ();
        this.vTab = new double[3];
        this.vTab[0] = this.v.getX();
        this.vTab[1] = this.v.getY();
        this.vTab[2] = this.v.getZ();
        this.centerTab = new double[3];
        this.centerTab[0] = this.center.getX();
        this.centerTab[1] = this.center.getY();
        this.centerTab[2] = this.center.getZ();
        this.wTab = new double[3];
        this.wTab[0] = this.w.getX();
        this.wTab[1] = this.w.getY();
        this.wTab[2] = this.w.getZ();

        // Corners computation
        this.corners = new Vector3D[5];
        this.corners[0] = new Vector3D(1.0, this.center, this.length / 2.0, this.u, this.width / 2.0, this.v);
        this.corners[1] = new Vector3D(1.0, this.center, -this.length / 2.0, this.u, this.width / 2.0, this.v);
        this.corners[2] = new Vector3D(1.0, this.center, -this.length / 2.0, this.u, -this.width / 2.0, this.v);
        this.corners[3] = new Vector3D(1.0, this.center, this.length / 2.0, this.u, -this.width / 2.0, this.v);

        // the first one is repeated for the distanceTo(point) algorithm
        this.corners[4] = this.corners[0];

        // the segments
        this.edges = new LineSegment[4];
        this.edges[0] = new LineSegment(this.corners[0], this.u.negate(), this.length);
        this.edges[1] = new LineSegment(this.corners[1], this.v.negate(), this.width);
        this.edges[2] = new LineSegment(this.corners[2], this.u, this.length);
        this.edges[3] = new LineSegment(this.corners[3], this.v, this.width);
    }

    /**
     * @return the U vector of the local frame
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the V vector of the local frame
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * @return the first corner
     */
    public Vector3D getC1() {
        return this.corners[0];
    }

    /**
     * @return the second corner
     */
    public Vector3D getC2() {
        return this.corners[1];
    }

    /**
     * @return the third corner
     */
    public Vector3D getC3() {
        return this.corners[2];
    }

    /**
     * @return the fourth corner
     */
    public Vector3D getC4() {
        return this.corners[3];
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

    /**
     * @return the center point
     */
    public Vector3D getCenter() {
        return this.center;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {
        // This algorithm uses vectors expressed as doubles to avoid using
        // Vector3D objects for performances issues

        // Initialisations
        final Vector3D origin = line.getOrigin();
        final Vector3D direction = line.getDirection();
        final double[] originTab = { origin.getX(), origin.getY(), origin.getZ() };
        final double[] directionTab = { direction.getX(), direction.getY(), direction.getZ() };
        boolean intersects = false;

        // Dot product of the line's direction and the normal vector of the plate.
        // There is no intersection if the result is zero.
        final double dotProdDirW =
            directionTab[0] * this.wTab[0] + directionTab[1] * this.wTab[1] + directionTab[2] * this.wTab[2];

        if (MathLib.abs(dotProdDirW) > VECTORSCOMPARISONEPS) {
            final double[] intersectionWithPlane = this.intersectionsWithDoubles(originTab, directionTab, dotProdDirW);

            // Computation of the coordinates of this point in the local frame of the plate.
            final double uCoord =
                (intersectionWithPlane[0] - this.centerTab[0]) * this.uTab[0]
                    + (intersectionWithPlane[1] - this.centerTab[1]) * this.uTab[1]
                    + (intersectionWithPlane[2] - this.centerTab[2])
                    * this.uTab[2];
            final double vCoord =
                (intersectionWithPlane[0] - this.centerTab[0]) * this.vTab[0]
                    + (intersectionWithPlane[1] - this.centerTab[1]) * this.vTab[1]
                    + (intersectionWithPlane[2] - this.centerTab[2])
                    * this.vTab[2];

            // There is intersection with the plate if the coordinates are
            // between -L/2 and L/2 for the length, and between -W/2 and W/2 for the width.
            // The "<=" operator is there used for performances issues
            final boolean uIsOK = uCoord <= this.length / 2.0 && uCoord >= -this.length / 2.0;
            final boolean vIsOK = vCoord <= this.width / 2.0 && vCoord >= -this.width / 2.0;
            intersects = uIsOK && vIsOK;
        }
        return intersects;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        // Initialisations
        final Vector3D[] intersection;
        final Vector3D origin = line.getOrigin();
        final Vector3D direction = line.getDirection();
        final double[] originTab = { origin.getX(), origin.getY(), origin.getZ() };
        final double[] directionTab = { direction.getX(), direction.getY(), direction.getZ() };

        // Dot product of the line's direction and the normal vector of the plate.
        // There is no intersection if the result is zero.
        final double dotProdDirW =
            directionTab[0] * this.wTab[0] + directionTab[1] * this.wTab[1] + directionTab[2] * this.wTab[2];

        if (MathLib.abs(dotProdDirW) > VECTORSCOMPARISONEPS && this.intersects(line)) {
            // computation of the solution
            final double[] intersectionWithPlane = this.intersectionsWithDoubles(originTab, directionTab, dotProdDirW);
            intersection = new Vector3D[1];
            intersection[0] = new Vector3D(intersectionWithPlane[0],
                intersectionWithPlane[1], intersectionWithPlane[2]);
        } else {
            // no solution
            intersection = new Vector3D[0];
        }

        return intersection;
    }

    /**
     * Computes the intersection point between the the plane containing the plate and a line, using only doubles (and no
     * Vector3D) to describe them. We supposed here that the parallelism of the line and the plane and the line have
     * already been tested.
     * 
     * @param originTab
     *        the line's origin point
     * @param directionTab
     *        the line's direction
     * @param dotProdDirW
     *        the dot product of the w vector and the direction of the line. Must not be zero ! !
     * @return the intersection point coordinates
     */
    private double[] intersectionsWithDoubles(final double[] originTab, final double[] directionTab,
                                              final double dotProdDirW) {

        final double distOriginToPlane =
            (originTab[0] - this.centerTab[0]) * this.wTab[0] + (originTab[1] - this.centerTab[1])
                * this.wTab[1] + (originTab[2] - this.centerTab[2]) * this.wTab[2];

        // Computation of the intersection point of the line and the plane
        final double alpha = -MathLib.divide(distOriginToPlane, dotProdDirW);
        return new double[] { originTab[0] + alpha * directionTab[0], originTab[1] + alpha * directionTab[1],
            originTab[2] + alpha * directionTab[2] };
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {
        // Distance initialisation
        double distance = 0.0;

        // If there is no intersection, the closest point to the line
        // is on the edges
        if (!this.intersects(line)) {

            // For each edge of the plate :

            // Initialisations
            double edgeToLineDist;
            distance = Double.POSITIVE_INFINITY;

            for (int i = 0; i < 4; i++) {

                // computation of the distance to the segment
                edgeToLineDist = this.edges[i].distanceTo(line);

                // the final distance is the smallest !
                if (edgeToLineDist < distance) {
                    distance = edgeToLineDist;
                }
            }
        }

        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        // If there is no intersection, the closest point to the line
        // is on the edges
        if (this.intersects(line)) {
            // if an intersection is found, the closest point is the intersection point.
            points[0] = this.getIntersectionPoints(line)[0];
            points[1] = points[0];
        } else {
            // For each edge of the plate :

            // Initialisations
            double edgeToLineDist;
            double distance = Double.POSITIVE_INFINITY;

            for (int i = 0; i < 4; i++) {

                // computation of the distance to the segment
                edgeToLineDist = this.edges[i].distanceTo(line);

                // the edge considered is the one realizing the shortest distance
                if (edgeToLineDist < distance) {
                    distance = edgeToLineDist;
                    points = this.edges[i].closestPointTo(line);
                }
            }
        }

        return points;
    }

    /**
     * Get a representation for this plate.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this plate
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
        // "Center":
        res.append("Center").append(this.center.toString());
        res.append(comma);
        // "U vector":
        res.append("U vector");
        res.append(this.u.toString());
        res.append(comma);
        // "V vector":
        res.append("V vector");
        res.append(this.v.toString());
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

    /** {@inheritDoc} */
    @Override
    public double getCrossSection(final Vector3D direction) {

        // dot product of the ray direction and of the normal to the plate
        final double cosAngle = MathLib.abs(Vector3D.dotProduct(direction.normalize(), this.w));

        // surface computation
        return cosAngle * this.length * this.width;
    }

    /**
     * @return the edges of the plate
     */
    public LineSegment[] getEdges() {
        return this.edges.clone();
    }
}
