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
 * @history creation 18/10/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
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
 * This is a describing class for a 3D right circular cone ended by a plane normal to its axis, with some algorithm to
 * compute intersections and distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with an angle, a height and two Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0);
 *            Vector3D direction = new Vector3D(6.0, -3.0, -1.0); double angle = 2.0; double height = 5.0; EllipticCone
 *            cone = new EllipticCone(origin, direction, angle, height); Intersection with a line : boolean intersects =
 *            cone(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: RightCircularCone.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class RightCircularCone implements Cone, Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = -6204155105548567293L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** height of the cone on its axis */
    private final double height;

    /** The associated infinite cone */
    private final InfiniteRightCircularCone infiniteCone;

    /** the ending disk */
    private final Disk upDisk;

    /** Position of the origin of the axis */
    private final Vector3D origin;

    /** Direction of the axis, defines the orientation : Z of the local frame */
    private final Vector3D direction;

    /** Angle of the cone */
    private final double angle;

    /** tangent of the angle */
    private final double tan;

    /** cosine of the angle */
    private final double cos;

    /** sine of the angle */
    private final double sin;

    /** rotation matrix to the local frame */
    private final Matrix3D rotationMatrix;

    /**
     * Build a elliptic cone from its radius, the height, the origin (apex) and direction of its axis
     * 
     * @param inOrigin
     *        the origin of the axis
     * @param inDirection
     *        the direction of the axis
     * @param inAngle
     *        the angle of the cone
     * @param inLength
     *        the height of the cone on its axis
     * @exception IllegalArgumentException
     *            if the angle or the height is negative or null, or if the direction vector has a null norm.
     * */
    public RightCircularCone(final Vector3D inOrigin, final Vector3D inDirection, final double inAngle,
                             final double inLength) {

        final String message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());

        // test of the height
        if (inLength < 0.0 || Precision.equals(inLength, 0.0)) {
            throw new IllegalArgumentException(message);
        }

        // Initialisations
        this.infiniteCone = new InfiniteRightCircularCone(inOrigin, inDirection, inAngle);
        this.height = inLength;
        this.direction = this.infiniteCone.getAxis();
        this.angle = inAngle;
        this.origin = new Vector3D(1.0, inOrigin);

        // disks creation
        final double diskRadius = this.height * MathLib.tan(this.angle);
        this.upDisk = new Disk(this.origin.add(this.height, this.direction), this.direction, diskRadius);

        this.tan = MathLib.tan(this.angle);
        final double[] sincosTheta = MathLib.sinAndCos(this.angle);
        this.sin = sincosTheta[0];
        this.cos = sincosTheta[1];

        // creation of the local frame
        final Vector3D u = this.direction.orthogonal();
        final Vector3D v = Vector3D.crossProduct(this.direction, u);

        final double[][] matrixData =
        { { u.getX(), u.getY(), u.getZ() }, { v.getX(), v.getY(), v.getZ() },
            { this.direction.getX(), this.direction.getY(), this.direction.getZ() } };
        this.rotationMatrix = new Matrix3D(matrixData);
    }

    /**
     * @return the height
     */
    public double getLength() {
        return this.height;
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the angle
     */
    public double getAngle() {
        return this.angle;
    }

    /**
     * @return the direction
     */
    public Vector3D getDirection() {
        return this.direction;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {
        // pre-computation of the intersection points with the infinite cone
        final Vector3D[] infiniteConeInterPoints = this.infiniteCone.getIntersectionPoints(line);

        // computation of the intersection with the finite cone : search of intersection
        // A line can't intersect the disk but not the finite cone, so it is
        // not necessary to be tested
        return this.intersectionsWithCone(infiniteConeInterPoints).length > 0;
    }

    /**
     * Computes the intersection points with the finite cone amongst the ones with the infinite cone
     * 
     * @param infiniteConeInterPoints
     *        the intersection points with the infinite cone
     * @return the intersection points
     */
    private Vector3D[] intersectionsWithCone(final Vector3D[] infiniteConeInterPoints) {

        // Initialisations
        final int nbPoints = infiniteConeInterPoints.length;
        final double[] originDbl = { this.origin.getX(), this.origin.getY(), this.origin.getZ() };
        final double[] dirDbl = { this.direction.getX(), this.direction.getY(), this.direction.getZ() };
        Vector3D[] points = new Vector3D[0];
        Vector3D[] temp;
        int nbPointsOK = 0;

        // search of intersections on the finite cone
        for (int i = 0; i < nbPoints; i++) {

            // Initialisation of the current intersection point in doubles
            final double[] currentPoint = { infiniteConeInterPoints[i].getX(), infiniteConeInterPoints[i].getY(),
                infiniteConeInterPoints[i].getZ() };

            // computation of the third coordinate (on the axis)
            // of this point on the local frame of the cone
            final double projOnAxis = (currentPoint[0] - originDbl[0]) * dirDbl[0]
                    + (currentPoint[1] - originDbl[1]) * dirDbl[1] + (currentPoint[2] - originDbl[2]) * dirDbl[2];

            // the point is on the finite cone...
            if (projOnAxis < this.height) {

                // ..it is added to the array of results
                temp = points;
                nbPointsOK++;
                points = new Vector3D[nbPointsOK];
                System.arraycopy(temp, 0, points, 0, nbPointsOK - 1);
                points[nbPointsOK - 1] = infiniteConeInterPoints[i];
            }
        }

        return points;

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        // pre-computation of the intersection points with the infinite cone
        final Vector3D[] infiniteConeInterPoints = this.infiniteCone.getIntersectionPoints(line);

        // search of intersections on the disks
        final Vector3D[] intersectionsWithDisk1 = this.upDisk.getIntersectionPoints(line);
        final int nbIntWithDisk1 = intersectionsWithDisk1.length;

        // Test of the intersections on the finite cone
        final Vector3D[] intersectionsWithCone = this.intersectionsWithCone(infiniteConeInterPoints);
        final int nbIntWithCyl = intersectionsWithCone.length;

        // Initialisation of the result array
        final Vector3D[] intersections = new Vector3D[nbIntWithCyl + nbIntWithDisk1];

        // concatenation of the found points
        System.arraycopy(intersectionsWithCone, 0, intersections, 0, nbIntWithCyl);
        if (nbIntWithDisk1 > 0) {
            intersections[nbIntWithCyl] = intersectionsWithDisk1[0];
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {

        final double distance;

        // computation of the intersection points with the infinite cone
        final Vector3D[] intersectWithInfiniteCone = this.infiniteCone.getIntersectionPoints(line);
        final int nbIntersect = intersectWithInfiniteCone.length;

        // if no intersection is found, the distance is computed
        // by the private following method
        if (nbIntersect == 0) {
            distance = this.noIntersectDistanceTo(line);
        } else {
            // if an intersection is found, two solutions are possible :
            // * an intersection belongs to the finite cone : distance = 0.0
            // * no intersection belongs to the cone, the distance must
            // be computed from the disk

            // test of each intersection point
            boolean intersect = false;
            for (int i = 0; i < nbIntersect; i++) {

                final Vector3D intersectionToOrig = intersectWithInfiniteCone[i].subtract(this.origin);
                final double projectionOnAxis = Vector3D.dotProduct(this.direction, intersectionToOrig);

                if (projectionOnAxis < this.height) {
                    intersect = true;
                }
            }

            // no intersection belongs to the finite cone :
            // distance to the disk
            if (intersect) {
                // intersection with the finite cone exists : null distance
                distance = 0.0;
            } else {
                distance = this.upDisk.distanceTo(line);
            }
        }

        return distance;
    }

    /**
     * If there is no intersection with the infinite cone, this method computes the shortest distance between the cone
     * and the line by finding the point of the line realising it. This point is found computing the (point of the line)
     * - cone distance derivative. If the infinite cone's closest point belongs not to the finite cone, the distance is
     * computed from the disk.
     * 
     * @param line
     *        the line
     * @return the distance to the cone
     */
    private double noIntersectDistanceTo(final Line line) {

        // Computation of the origin and direction of the line expressed on the local frame
        // of the cone
        final Vector3D lineOriginVect = this.rotationMatrix.multiply(line.getOrigin().subtract(this.origin));
        final double[] lineOrigin = { lineOriginVect.getX(), lineOriginVect.getY(), lineOriginVect.getZ() };
        final Vector3D lineDirectionVect = this.rotationMatrix.multiply(line.getDirection());
        final double[] lineDirection = { lineDirectionVect.getX(), lineDirectionVect.getY(), lineDirectionVect.getZ() };

        // Computation of the terms of the distance derivative
        final double coeff1 = lineDirection[0] * lineOrigin[0] + lineDirection[1] * lineOrigin[1];
        final double coeff3 = lineDirection[0] * lineDirection[0] + lineDirection[1] * lineDirection[1];
        final double coeff4 = lineDirection[2] * lineDirection[2] * this.tan * this.tan;

        // distance default value is computed from the origin
        double distance = line.distance(this.origin);

        // computation of the coefficient of the null derivative equation solving
        final double b = 2 * coeff1 * (coeff4 - coeff3);
        final double a = coeff3 * (coeff4 - coeff3);

        // computation of the closest point to cone cone, on the superior part of the cone

        // If A = 0, the line angle with the local vertical is the same as the angle of
        // the cone, the closest point is the origin

        if (MathLib.abs(a) > VECTORSCOMPARISONEPS) {

            // No need to test the determinant : there is at least one solution,
            // and two solution would mean an intersection with the shape,
            // what has already been tested.

            // computation of the solution
            final double alpha = -b / (2.0 * a);
            final double[] sol1 = { lineOrigin[0] + alpha * lineDirection[0], lineOrigin[1] + alpha * lineDirection[1],
                lineOrigin[2] + alpha * lineDirection[2] };

            // test of the solution : is closest to a point of the finite cone ?
            // If not, the distance to the disk must be computed
            final double mirorOriginZ = this.height * (1 + this.tan * this.tan);

            final double[] mirorOriginToSol = { sol1[0], sol1[1], sol1[2] - mirorOriginZ };

            final double cosFromMirorOrigin = MathLib.divide(mirorOriginToSol[2],
                MathLib.sqrt(mirorOriginToSol[0] * mirorOriginToSol[0] + mirorOriginToSol[1]
                        * mirorOriginToSol[1] + mirorOriginToSol[2] * mirorOriginToSol[2]));

            if (cosFromMirorOrigin > -this.cos) {
                // distance from the disk computation
                distance = this.upDisk.distanceTo(line);
            } else {
                // To be closest to the finite cone, the solution
                // must be a point close to the superior part of the cone
                final double angleToZ1 = MathLib.divide(sol1[2],
                    MathLib.sqrt(sol1[0] * sol1[0] + sol1[1] * sol1[1] + sol1[2] * sol1[2]));

                if (angleToZ1 > -this.sin) {
                    distance = (MathLib.sqrt(sol1[0] * sol1[0] + sol1[1] * sol1[1]) - this.tan * sol1[2]) * this.cos;
                }
                // else, the distance is computed from the origin : default value.
            }
        }

        return distance;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        Vector3D[] points = new Vector3D[2];

        if (this.intersects(line)) {
            // As an intersection is found the points are identical
            // Intersections
            final Vector3D[] iscs = this.getIntersectionPoints(line);

            // Return intersection with lowest abscissa on line
            points[0] = line.pointOfMinAbscissa(iscs);
            points[1] = points[0];
        } else {
            // point of the infinite cone that is the closest to the line
            final Vector3D[] infConeClosestPointToLine = this.infiniteCone.closestPointTo(line);

            // Z coordinate of this point in the local frame computation
            final double projOnAxis = Vector3D.dotProduct(infConeClosestPointToLine[0], this.direction);

            // test of the Z coordinate
            if (projOnAxis > this.height) {
                // if the coordinate is greater than H, the point is searched on the
                // ending disk
                points = this.upDisk.closestPointTo(line);
            } else {
                // if it belongs to the finite cone
                points = infConeClosestPointToLine;
            }
        }

        return points;
    }

    /**
     * Get a representation for this elliptic cone.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this elliptic cone
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
        // "Angle":
        res.append("Angle").append(open);
        res.append(this.angle).append(close);
        res.append(comma);
        // "Height":
        res.append("Height").append(open);
        res.append(this.height).append(close);
        res.append(close);

        return res.toString();
    }
}
