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
 * @history creation 19/10/2011
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
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D rectangle cone ended by a plane normal to its axis (pyramid), with some algorithm
 * to compute intersections and distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with two dimensions, a length and three Vector3D : Vector3D origin = new Vector3D(1.0, 6.0,
 *            -2.0); Vector3D direction = new Vector3D(6.0, -3.0, -1.0); Vector3D uVector = new Vector3D(-5.0, 3.0,
 *            0.0); double length = 2.0; double width = 4.0; double height = 5.0; RectangleCone cone = new
 *            RectangleCone(origin, direction, uVector, length, width, height); Intersection with a line : boolean
 *            intersects = cone(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: RectangleCone.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class RectangleCone implements Cone, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -617787590801253864L;

    /** Position of the origin of the cone */
    private final Vector3D origin;

    /** Height of the cone on its axis */
    private final double height;

    /** Length of the cone's basis (U axis) */
    private final double length;

    /** Width of the cone's basis (U axis) */
    private final double width;

    /** Direction of the cone, defines its orientation : Z of the local frame */
    private final Vector3D direction;

    /** U vector of the local frame */
    private final Vector3D u;

    /** V vector of the local frame */
    private final Vector3D v;

    /** The associated infinite cone */
    private final InfiniteRectangleCone infiniteCone;

    /** The ending plate */
    private final Plate endingPlate;

    /** Edges of the pyramid */
    private final LineSegment[] edges;

    /**
     * Build a rectangle cone from its apex (origin), axis' direction, approximative U vector of the local frame and
     * dimensions.
     * 
     * @param inOrigin
     *        the cone's origin
     * @param inDirection
     *        the cone's direction
     * @param inUvector
     *        the cone's approximative U vector : corrected to be orthogonal to the direction
     * @param inLength
     *        the basis' length
     * @param inWidth
     *        the basis' width
     * @param inHeight
     *        the cone's height on its axis
     */
    public RectangleCone(final Vector3D inOrigin, final Vector3D inDirection, final Vector3D inUvector,
        final double inLength, final double inWidth, final double inHeight) {

        final String message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());

        // test of the dimensions
        if (inLength < 0.0 || Precision.equals(inLength, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        if (inWidth < 0.0 || Precision.equals(inWidth, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        if (inHeight < 0.0 || Precision.equals(inHeight, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        // infinite cone's angles computation
        final double angleU = MathLib.atan(inLength / (2.0 * inHeight));
        final double angleV = MathLib.atan(inWidth / (2.0 * inHeight));

        // associated infinite cone creation
        this.infiniteCone = new InfiniteRectangleCone(inOrigin, inDirection, inUvector, angleU, angleV);

        // initialisations
        this.length = inLength;
        this.width = inWidth;
        this.height = inHeight;
        this.origin = new Vector3D(1.0, inOrigin);
        this.direction = this.infiniteCone.getAxis();
        this.u = this.infiniteCone.getU();
        this.v = this.infiniteCone.getV();

        // rotation matrix
        final double[][] matrixData =
        { { this.u.getX(), this.u.getY(), this.u.getZ() }, { this.v.getX(), this.v.getY(), this.v.getZ() },
            { this.direction.getX(), this.direction.getY(), this.direction.getZ() } };
        final Matrix3D rotationMatrix = new Matrix3D(matrixData);

        // creation of the edges
        final Vector3D[] edgesDirections = new Vector3D[4];
        final double tanU = MathLib.tan(angleU);
        final double tanV = MathLib.tan(angleV);
        final double norm = 1 + tanU * tanU + tanV * tanV;

        // side edges directions computation
        // norm cannot be 0
        edgesDirections[0] =
            rotationMatrix.transposeAndMultiply(new Vector3D(tanU / norm, tanV / norm, 1 / norm));
        edgesDirections[1] =
            rotationMatrix.transposeAndMultiply(new Vector3D(-tanU / norm, tanV / norm, 1 / norm));
        edgesDirections[2] =
            rotationMatrix.transposeAndMultiply(new Vector3D(-tanU / norm, -tanV / norm, 1 / norm));
        edgesDirections[3] =
            rotationMatrix.transposeAndMultiply(new Vector3D(tanU / norm, -tanV / norm, 1 / norm));

        // side edges computation
        this.edges = new LineSegment[4];
        final double edgesLength =
            MathLib.sqrt(this.height * this.height + MathLib.sqrt(this.length * this.length + this.width * this.width));
        this.edges[0] = new LineSegment(this.origin, edgesDirections[0], edgesLength);
        this.edges[1] = new LineSegment(this.origin, edgesDirections[1], edgesLength);
        this.edges[2] = new LineSegment(this.origin, edgesDirections[2], edgesLength);
        this.edges[3] = new LineSegment(this.origin, edgesDirections[3], edgesLength);

        // ending plate creation
        this.endingPlate =
            new Plate(this.origin.add(this.height, this.direction), this.u, this.v, this.length, this.width);
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the height
     */
    public double getHeight() {
        return this.height;
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
     * @return the direction
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /**
     * @return the u vector of the local frame
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * @return the v vector of the local frame
     */
    public Vector3D getV() {
        return this.v;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {
        // Computation of the intersection points with the finite cone
        // No need to test the ending plate,
        // a line can't intersect it but not the cone.
        return this.getIntersectionsOnFiniteCone(line).length > 0;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // Initialisations
        final Vector3D[] intersections;

        // Computation of the intersection points with the finite cone
        final Vector3D[] intersectOnFiniteCone = this.getIntersectionsOnFiniteCone(line);
        final int nbIntersectOnFiniteCone = intersectOnFiniteCone.length;

        // Computation of the intersection points with the ending plate
        final Vector3D[] intersectOnPlate = this.endingPlate.getIntersectionPoints(line);
        final int nbIntersectOnPlate = intersectOnPlate.length;

        intersections = new Vector3D[nbIntersectOnPlate + nbIntersectOnFiniteCone];

        // all found points are added to the list
        System.arraycopy(intersectOnFiniteCone, 0, intersections, 0, nbIntersectOnFiniteCone);
        if (nbIntersectOnPlate != 0) {
            intersections[nbIntersectOnFiniteCone] = intersectOnPlate[0];
        }

        return intersections;
    }

    /**
     * Computes the intersection points with the finite cone (but not with the ending plate).
     * 
     * @param line
     *        the line to be tested
     * @return the intersection points
     */
    private Vector3D[] getIntersectionsOnFiniteCone(final Line line) {

        // computation if the intersection points with the infinite cone
        final Vector3D[] intersectOnInfiniteCone = this.infiniteCone.getIntersectionPoints(line);
        final int nbIntOnInfiniteCone = intersectOnInfiniteCone.length;

        // Initialisations
        Vector3D[] intersectOnFiniteCone = new Vector3D[0];
        Vector3D[] temp;
        int currentLength = 0;
        double projOnAxis;

        // For each intersection point found on the infinite cone..0.
        for (int i = 0; i < nbIntOnInfiniteCone; i++) {
            // ...test of the Z coordinate in the local frame
            projOnAxis = Vector3D.dotProduct(intersectOnInfiniteCone[i].subtract(this.origin), this.direction);

            // If it found lower than the height, the intersection in on the finite cone too,
            // and the point is added to the list
            if (projOnAxis < this.height) {
                currentLength = currentLength + 1;
                temp = intersectOnFiniteCone;
                intersectOnFiniteCone = new Vector3D[currentLength];
                System.arraycopy(temp, 0, intersectOnFiniteCone, 0, currentLength - 1);
                intersectOnFiniteCone[currentLength - 1] = intersectOnInfiniteCone[i];
            }
        }

        return intersectOnFiniteCone;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {

        // Distance to edges initialisation
        double distance = 0.0;

        // If there is no intersection, the closest point to the line
        // is on the sides
        if (!this.intersects(line)) {

            // For each side of the plate :
            // computation of the closest point to the line and of its
            // distance to the line

            // Initialisations
            double edgeToLineDist;
            double distanceToEdges = Double.POSITIVE_INFINITY;

            for (int i = 0; i < 4; i++) {

                // computation of the distance to the segment
                edgeToLineDist = this.edges[i].distanceTo(line);

                // the final distance is the smallest !
                if (edgeToLineDist < distanceToEdges) {
                    distanceToEdges = edgeToLineDist;
                }
            }

            // computation of the distance to the ending plate
            final double distanceToPlate = this.endingPlate.distanceTo(line);

            distance = MathLib.min(distanceToPlate, distanceToEdges);
        }

        return distance;

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        if (this.intersects(line)) {
            points[0] = this.getIntersectionPoints(line)[0];
            points[1] = points[0];
        } else {
            // point of the infinite cone that is the closest to the line
            final Vector3D[] infConeClosestPointToLine = this.infiniteCone.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infConeClosestPointToLine[0], this.direction);

            // test of the Z coordinate
            if (projOnAxis > this.height) {
                // if the coordinate is greater than H, the point is searched on the
                // ending plate
                points = this.endingPlate.closestPointTo(line);
            } else {
                // if it belongs to the finite cone
                points = infConeClosestPointToLine;
            }
        }

        return points;
    }

    /**
     * Get a representation for this rectangle cone.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this rectangle cone
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
        res.append("Direction").append(this.direction.toString());
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
        res.append(comma);
        // "Height":
        res.append("Height").append(open);
        res.append(this.height).append(close);
        res.append(close);

        return res.toString();
    }
}
