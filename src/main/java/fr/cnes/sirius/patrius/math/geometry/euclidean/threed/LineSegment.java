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
 * @history creation 19/10/2011
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a line segment in 3D space, with a method to compute the shortest distance to a line.
 * </p>
 * 
 * @useSample <p>
 *            Creation with two dimensions, a length and three Vector3D : Vector3D origin = new Vector3D(1.0, 6.0,
 *            -2.0); Vector3D direction = new Vector3D(6.0, -3.0, -1.0); double length = 2.0; LineSegment segment = new
 *            LineSegment(origin, direction, length); Distance to a line : double distance = segment.distanceTo(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: LineSegment.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class LineSegment implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 9192006834483139397L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Position of the origin of the segment */
    private final Vector3D origin;

    /** Position of the end of the segment */
    private final Vector3D end;

    /** Length of the segment */
    private final double length;

    /** Direction of the cone, defines its orientation : Z of the local frame */
    private final Vector3D direction;

    /**
     * Build a line segment from its origin, direction and length.
     * 
     * @param inOrigin
     *        the segment's origin
     * @param inDirection
     *        the segment's direction
     * @param inLength
     *        the segment's length
     */
    public LineSegment(final Vector3D inOrigin, final Vector3D inDirection, final double inLength) {

        // test of the dimensions
        final String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());

        if (inLength < 0.0 || Precision.equals(inLength, 0.0)) {
            throw new IllegalArgumentException(message);
        }
        // The direction's norm must be positive
        // The e-10 epsilon is chosen to be coherent with the Plane class
        if (inDirection.getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }

        this.length = inLength;
        this.origin = new Vector3D(1.0, inOrigin);
        this.direction = inDirection.normalize();
        this.end = this.origin.add(this.length, this.direction);
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the end
     */
    public Vector3D getEnd() {
        return this.end;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return this.length;
    }

    /**
     * @return the direction
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /**
     * Computes the shortest distance to a line of space.
     * 
     * @param line
     *        the line
     * @return the distance
     */
    public double distanceTo(final Line line) {

        // closest points computation
        final Vector3D[] closestPoints = this.closestPointTo(line);

        // distance between those points
        return closestPoints[0].subtract(closestPoints[1]).getNorm();
    }

    /**
     * Computation of the closest point to a line, and the associated point of the line;
     * 
     * @param line
     *        the line
     * @return the point of the line and the point of the segment.
     */
    public Vector3D[] closestPointTo(final Line line) {
        final Vector3D[] points = new Vector3D[2];

        // computation of the normal to both directions
        final Vector3D originLine = line.getOrigin();
        final Vector3D directionLine = line.getDirection().normalize();
        Vector3D normal = Vector3D.crossProduct(directionLine, this.direction);

        // if the line is parallel to the segment, the distance is known
        if (normal.getNorm() < VECTORSCOMPARISONEPS) {
            points[1] = this.origin;
            points[0] = line.toSpace(line.toSubSpace(points[1]));
        } else {

            // Vector from the origin of the segment to the origin of the line
            final Vector3D origLineToOrigSegment = originLine.subtract(this.origin);

            // projection of this vector on the normed normal vector
            normal = normal.normalize();
            final double projectionOnNormal = Vector3D.dotProduct(origLineToOrigSegment, normal);

            // creation of the shortest vector from the segment to the line
            normal = normal.scalarMultiply(projectionOnNormal);

            // computation of the points origin and origin + direction
            // translated into the plane parallel to the separating middle plane
            // and containing the input line.
            final Vector3D translatedOrig = this.origin.add(normal);
            final Vector3D translatedOrigPlusDir = this.origin.add(this.direction).add(normal);

            // Computation of the distances of these points to the input line
            final double distOrig = line.distance(translatedOrig);
            final double distOrigPLusDir = line.distance(translatedOrigPlusDir);

            final Vector3D signumVectorOrig = line.toSpace(line.toSubSpace(translatedOrig)).subtract(translatedOrig);
            final Vector3D signumVectorOrigplusDir = line.toSpace(line.toSubSpace(translatedOrigPlusDir)).subtract(
                translatedOrigPlusDir);

            final double dotProd = Vector3D.dotProduct(signumVectorOrig, signumVectorOrigplusDir);

            // Thales theorem : computation of the point of the segment
            // witch is the closest to the input line
            final double alpha;
            if (dotProd > 0.0) {
                alpha = distOrig / (distOrig - distOrigPLusDir);
            } else {
                // if the lines are crossing between the translated origin and the
                // translated origin + the direction
                alpha = distOrig / (distOrig + distOrigPLusDir);
            }

            if (alpha < 0.0) {
                // the closest point is the origin corner
                points[1] = this.origin;
                points[0] = line.toSpace(line.toSubSpace(points[1]));
            } else if (alpha > this.length) {
                // the closest point is the next corner
                points[1] = this.end;
                points[0] = line.toSpace(line.toSubSpace(points[1]));
            } else {
                // The closest point belongs to the side
                points[1] = this.origin.add(alpha, this.direction);
                points[0] = points[1].add(normal);
            }
        }

        return points;
    }

    /**
     * Get a representation for this line segment.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this line segment
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
        // "Length":
        res.append("Length").append(open);
        res.append(this.length).append(close);
        res.append(close);

        return res.toString();
    }

}
