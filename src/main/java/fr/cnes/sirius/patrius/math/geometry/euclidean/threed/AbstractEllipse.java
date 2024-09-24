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
 * @history creation 23/11/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is an abstract describing class for an ellipse in 3D space, with some algorithm to compute intersections and
 * distances to some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with two radiuses, and two Vector3D : Vector3D center = new Vector3D(1.0, 6.0, -2.0); Vector3D
 *            normal = new Vector3D(6.0, -3.0, -1.0); double radiusA = 2.0; double radiusB = 5.0; Ellipse ellipse = new
 *            Ellipse(center, normal, radiusA, radiusB); Intersection with a line : boolean intersects = ellipse(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: AbstractEllipse.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public abstract class AbstractEllipse implements SolidShape, Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 2542698102987576947L;

    /** Angular convergence threshold in the distance computation algorithm. */
    private static final double CONVTHRESHOLD = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Angular non convergence stopper in the distance computation algorithm. */
    private static final int MAXITERATIONS = 100;

    /** Dimension for distance array */
    private static final int DIM = 8;

    /** position of the center */
    private final Vector3D center;

    /** position of the center in doubles */
    private final double[] centerTab;

    /** normal to the plane containing the ellipse */
    private final Vector3D normal;

    /** normal to the plane containing the ellipse in doubles */
    private final double[] normalTab;

    /** radius on the U axis of the local frame */
    private final double radiusA;

    /** radius on the V axis of the local frame */
    private final double radiusB;

    /** U vector of the local frame */
    private final Vector3D u;

    /** U vector of the local frame in doubles */
    private final double[] uTab;

    /** V vector of the local frame */
    private final Vector3D v;

    /** V vector of the local frame in doubles */
    private final double[] vTab;

    /** first four points to test in iterative algorithms */
    private final Vector3D[] pointsToTest;

    /**
     * Build an ellipse in the 3D space from its center, normal vector, approximative U vector of the local frame, and
     * two radiuses.
     * 
     * @param inCenter
     *        position of the center
     * @param inNormal
     *        normal to the plane containing the ellipse
     * @param inUvector
     *        approximative U vector of the local frame : corrected to be orthogonal to the normal
     * @param inRadiusA
     *        radius on the U axis of the local frame
     * @param inRadiusB
     *        radius on the V axis of the local frame
     * @throws IllegalArgumentException
     *         if one radius is'nt strictly positive, if the normal or the uVector has a not strictly positive norm,
     *         or if they are parallel.
     */
    public AbstractEllipse(final Vector3D inCenter, final Vector3D inNormal, final Vector3D inUvector,
                           final double inRadiusA, final double inRadiusB) {

        // The normal's and u vector's norm must be positive, they must not be parallel
        // The e-10 epsilon is chosen to be coherent with the Plane class
        String message = PatriusMessages.CANNOT_NORMALIZE_A_ZERO_NORM_VECTOR.getLocalizedString(Locale.getDefault());
        if (Vector3D.crossProduct(inNormal, inUvector).getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }

        // The radiuses must be positive
        message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());
        if (inRadiusA < 0.0 || Precision.equals(0.0, inRadiusA)) {
            throw new IllegalArgumentException(message);
        }
        if (inRadiusB < 0.0 || Precision.equals(0.0, inRadiusB)) {
            throw new IllegalArgumentException(message);
        }

        // Initialisations
        this.radiusA = inRadiusA;
        this.radiusB = inRadiusB;
        this.center = new Vector3D(1.0, inCenter);
        this.normal = inNormal.normalize();

        // creation of the right U and V vectors
        this.u = inUvector.subtract(new Vector3D(Vector3D.dotProduct(this.normal, inUvector), this.normal)).normalize();
        this.v = Vector3D.crossProduct(this.normal, this.u);

        // normal vector in doubles
        this.normalTab = new double[3];
        this.normalTab[0] = this.normal.getX();
        this.normalTab[1] = this.normal.getY();
        this.normalTab[2] = this.normal.getZ();

        // center vector in doubles
        this.centerTab = new double[3];
        this.centerTab[0] = this.center.getX();
        this.centerTab[1] = this.center.getY();
        this.centerTab[2] = this.center.getZ();

        // U vector in doubles
        this.uTab = new double[3];
        this.uTab[0] = this.u.getX();
        this.uTab[1] = this.u.getY();
        this.uTab[2] = this.u.getZ();

        // V vector in doubles
        this.vTab = new double[3];
        this.vTab[0] = this.v.getX();
        this.vTab[1] = this.v.getY();
        this.vTab[2] = this.v.getZ();

        // test of the distance to the line of four points
        // of the ellipse : intersections with the U and V axis
        this.pointsToTest = new Vector3D[6];
        this.pointsToTest[0] = this.center.add(this.radiusA, this.u);
        this.pointsToTest[1] = this.center.add(new Vector3D(this.radiusA / 2.0, this.u,
            this.radiusB * MathLib.sqrt(3.0) / 2.0, this.v));
        this.pointsToTest[2] = this.center.add(new Vector3D(-this.radiusA / 2.0, this.u,
            this.radiusB * MathLib.sqrt(3.0) / 2.0, this.v));
        this.pointsToTest[3] = this.center.add(-this.radiusA, this.u);
        this.pointsToTest[4] = this.center.add(new Vector3D(-this.radiusA / 2.0, this.u,
            -this.radiusB * MathLib.sqrt(3.0) / 2.0, this.v));
        this.pointsToTest[5] = this.center.add(new Vector3D(this.radiusA / 2.0, this.u,
            -this.radiusB * MathLib.sqrt(3.0) / 2.0, this.v));
    }

    /**
     * @return the center
     */
    public final Vector3D getCenter() {
        return this.center;
    }

    /**
     * @return the normal
     */
    public final Vector3D getNormal() {
        return this.normal;
    }

    /**
     * @return the radius A
     */
    public final double getRadiusA() {
        return this.radiusA;
    }

    /**
     * @return the radius B
     */
    public final double getRadiusB() {
        return this.radiusB;
    }

    /**
     * @return the u vector for the local frame
     */
    public final Vector3D getU() {
        return this.u;
    }

    /**
     * @return the v vector for the local frame
     */
    public final Vector3D getV() {
        return this.v;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean intersects(final Line line) {

        return this.getIntersectionPoints(line).length > 0;
    }

    /** {@inheritDoc} */
    @Override
    public final Vector3D[] getIntersectionPoints(final Line line) {

        // Initialisations
        final double uCoord;
        final double vCoord;
        final double[] intersectionWithPlane;
        Vector3D[] intersections = new Vector3D[0];
        final Vector3D origin = line.getOrigin();
        final Vector3D direction = line.getDirection();
        final double[] originTab = { origin.getX(), origin.getY(), origin.getZ() };
        final double[] directionTab = { direction.getX(), direction.getY(), direction.getZ() };

        // Dot product of the line's direction and the normal vector of the plate.
        // There is no intersection if the result is zero.
        final double dotProdDirW =
            directionTab[0] * this.normalTab[0] + directionTab[1] * this.normalTab[1] + directionTab[2]
                    * this.normalTab[2];

        if (MathLib.abs(dotProdDirW) > VECTORSCOMPARISONEPS) {
            // computation of the solutions
            // computation of the intersections with the associated plane
            intersectionWithPlane = this.intersectionsWithDoubles(originTab, directionTab, dotProdDirW);

            // Computation of the coordinates of this point in the local frame of the ellipse.
            uCoord =
                (intersectionWithPlane[0] - this.centerTab[0]) * this.uTab[0]
                        + (intersectionWithPlane[1] - this.centerTab[1])
                        * this.uTab[1] + (intersectionWithPlane[2] - this.centerTab[2]) * this.uTab[2];
            vCoord =
                (intersectionWithPlane[0] - this.centerTab[0]) * this.vTab[0]
                        + (intersectionWithPlane[1] - this.centerTab[1])
                        * this.vTab[1] + (intersectionWithPlane[2] - this.centerTab[2]) * this.vTab[2];

            // There is intersection with the ellipse if the coordinates are
            // such as X^2 + Y^2 <= a^2*cos^2 + b^2sin^2, cos and sin beeing the sine
            // and cosine of the angle between the intersection position vector and u
            // in the local frame
            final double coordNorm = MathLib.sqrt(uCoord * uCoord + vCoord * vCoord);
            if (coordNorm > VECTORSCOMPARISONEPS) {
                final double absCos = MathLib.divide(MathLib.abs(uCoord), coordNorm);
                final double absSin = MathLib.divide(MathLib.abs(vCoord), coordNorm);
                final double minCriteria =
                    this.radiusA * this.radiusA * absCos * absCos + this.radiusB * this.radiusB * absSin * absSin;
                final double testedValue = uCoord * uCoord + vCoord * vCoord;
                if (testedValue <= minCriteria) {
                    intersections = new Vector3D[1];
                    intersections[0] = new Vector3D(intersectionWithPlane[0], intersectionWithPlane[1],
                        intersectionWithPlane[2]);
                }
            } else {
                intersections = new Vector3D[1];
                intersections[0] = this.center;
            }
        }

        return intersections;
    }

    /**
     * Computes the intersection point between the the plane containing the ellipse and a line, using only doubles (and
     * no Vector3D) to describe them. We supposed here that the parallelism of the line and the plane and the line have
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
            (originTab[0] - this.centerTab[0]) * this.normalTab[0] + (originTab[1] - this.centerTab[1])
                    * this.normalTab[1] + (originTab[2] - this.centerTab[2]) * this.normalTab[2];

        // Computation of the intersection point of the line and the plane
        final double alpha = -MathLib.divide(distOriginToPlane, dotProdDirW);
        return new double[] { originTab[0] + alpha * directionTab[0], originTab[1] + alpha * directionTab[1],
            originTab[2] + alpha * directionTab[2] };
    }

    /** {@inheritDoc} */
    @Override
    public final double distanceTo(final Line line) {
        final double rez;
        if (this.intersects(line)) {
            // the line intersects the ellipse
            rez = 0.0;
        } else {
            // the line does not intersect the ellipse
            rez = this.ellipseDistance(line)[0];
        }
        return rez;
    }

    /**
     * This method computes the distance from the Line to the ellipse and the angle from the u vector of the point
     * realizing this distance.
     * 
     * @precondition we assume the Line does not intersect the ellipse
     * 
     * @param line
     *        the Line instance
     * @return the distance from the line to the ellipse and the corresponding angle
     */
    private double[] ellipseDistance(final Line line) {

        final double[] distances = new double[DIM];

        // Initialisations for the loops
        double closestPointAngle1 = 0.0;
        double closestPointAngle2 = 0.0;
        double distanceMinToPoint1 = Double.POSITIVE_INFINITY;
        double distanceMinToPoint2;
        int closestPoint1 = 0;

        // computation of the distances to each of those four points
        for (int i = 0; i < 6; i++) {
            distances[i + 1] = line.distance(this.pointsToTest[i]);

            // search of the point realizing the shortest distance in those four
            if (distances[i + 1] < distanceMinToPoint1) {
                distanceMinToPoint1 = distances[i + 1];
                closestPoint1 = i + 1;
                closestPointAngle1 = i * (FastMath.PI / 3.0);
            }
        }
        distances[DIM - 1] = distances[1];
        distances[0] = distances[6];

        // choice of the second point amongst the two next to the closest
        if (distances[closestPoint1 + 1] > distances[closestPoint1 - 1]) {
            closestPointAngle2 = ((double) closestPoint1 - 2) * (FastMath.PI / 3.0);
            distanceMinToPoint2 = distances[closestPoint1 - 1];
        } else {
            closestPointAngle2 = closestPoint1 * (FastMath.PI / 3.0);
            distanceMinToPoint2 = distances[closestPoint1 + 1];
        }

        // loop : search for the point of the ellipse
        // realising the shortest distance to the line.
        // The angle segment of search is split in two at each step
        Vector3D currentTestedPoint;
        int stopper = 0;
        final double[] newAngles = new double[6];

        while (MathLib.abs(closestPointAngle1 - closestPointAngle2) > CONVTHRESHOLD
                || MathLib.abs(distanceMinToPoint1 - distanceMinToPoint2) > CONVTHRESHOLD) {

            // computation of the new tested points
            // existing points
            distances[1] = distanceMinToPoint1;
            newAngles[1] = closestPointAngle1;
            distances[4] = distanceMinToPoint2;
            newAngles[4] = closestPointAngle2;

            // first new point
            newAngles[2] = (2.0 * closestPointAngle1 + closestPointAngle2) / 3.0;
            currentTestedPoint = this.center.add(new Vector3D(MathLib.cos(newAngles[2]) * this.radiusA, this.u, MathLib
                .sin(newAngles[2]) * this.radiusB, this.v));
            distances[2] = line.distance(currentTestedPoint);

            // second new point
            newAngles[3] = (closestPointAngle1 + 2.0 * closestPointAngle2) / 3.0;
            currentTestedPoint = this.center.add(new Vector3D(MathLib.cos(newAngles[3]) * this.radiusA, this.u, MathLib
                .sin(newAngles[3]) * this.radiusB, this.v));
            distances[3] = line.distance(currentTestedPoint);

            // the two central points are duplicated
            distances[0] = distances[2];
            distances[5] = distances[3];
            newAngles[0] = newAngles[2];
            newAngles[5] = newAngles[3];

            distanceMinToPoint1 = Double.POSITIVE_INFINITY;
            for (int i = 1; i < 5; i++) {
                if (distances[i] < distanceMinToPoint1) {
                    closestPoint1 = i;
                    closestPointAngle1 = newAngles[i];
                    distanceMinToPoint1 = distances[i];
                }
            }

            // choice of the second point amongst the two next to the closest
            if (distances[closestPoint1 + 1] > distances[closestPoint1 - 1]) {
                closestPointAngle2 = newAngles[closestPoint1 - 1];
                distanceMinToPoint2 = distances[closestPoint1 - 1];
            } else {
                closestPointAngle2 = newAngles[closestPoint1 + 1];
                distanceMinToPoint2 = distances[closestPoint1 + 1];
            }

            stopper++;
            if (stopper == MAXITERATIONS) {
                throw new ConvergenceException();
            }
        }
        final double distance = distanceMinToPoint1;

        // return of the minus of the distances and the corresponding angle
        return new double[] { distance, closestPointAngle1 };
    }

    /**
     * Computes the shortest distance from a point to the ellipse.
     * 
     * @param point
     *        the point
     * @return the shortest distance from the point to the ellipse
     */
    public final double distanceTo(final Vector3D point) {
        // distance to the closest point
        return this.closestPointTo(point).subtract(point).getNorm();
    }

    /**
     * Computes the point on the ellipse closest to a point.
     * 
     * @param point
     *        the point
     * @return the closest point on the ellipse
     */
    public final Vector3D closestPointTo(final Vector3D point) {
        final Vector3D closestPoint;

        // Coordinates of the point in the ellipse's local frame
        final Vector3D centerToPoint = point.subtract(this.center);
        final double projOnU = MathLib.abs(Vector3D.dotProduct(centerToPoint, this.u));
        final double projOnV = MathLib.abs(Vector3D.dotProduct(centerToPoint, this.v));

        // compared values computation for the test of the projection on the U, V plane
        final double absTheta = MathLib.atan2(projOnV, projOnU);
        final double[] sincos = MathLib.sinAndCos(absTheta);
        final double sinTheta = sincos[0];
        final double cosTheta = sincos[1];
        final double normOnUV = projOnU * projOnU + projOnV * projOnV;
        final double maxNormOnUV =
            this.radiusA * this.radiusA * cosTheta * cosTheta + this.radiusB * this.radiusB * sinTheta * sinTheta;

        // if the projection of the point in the ellipse's plane is out of the shape
        if (normOnUV > maxNormOnUV) {

            final double[] distances = new double[DIM];

            // Initialisations for the loops
            double distanceMinToPoint1 = Double.POSITIVE_INFINITY;
            double distanceMinToPoint2;
            double closestPtAngle1 = 0.0;
            double closestPointAngle2 = 0.0;
            int closestPoint1 = 0;

            // computation of the distances to each of those four points
            for (int i = 0; i < 6; i++) {
                distances[i + 1] = point.subtract(this.pointsToTest[i]).getNorm();

                // search of the point realizing the shortest distance in those four
                if (distances[i + 1] < distanceMinToPoint1) {
                    distanceMinToPoint1 = distances[i + 1];
                    closestPoint1 = i + 1;
                    closestPtAngle1 = i * (FastMath.PI / 3.0);
                }
            }
            distances[0] = distances[6];
            distances[DIM - 1] = distances[1];

            // choice of the second point amongst the two next to the closest
            if (distances[closestPoint1 + 1] < distances[closestPoint1 - 1]) {
                closestPointAngle2 = closestPoint1 * (FastMath.PI / 3.0);
                distanceMinToPoint2 = distances[closestPoint1 + 1];
            } else {
                closestPointAngle2 = ((double) closestPoint1 - 2) * (FastMath.PI / 3.0);
                distanceMinToPoint2 = distances[closestPoint1 - 1];
            }

            // loop : search for the point of the ellipse
            // realising the shortest distance to the point.
            // The angle segment of search is split in two at each step
            Vector3D newPointTested;
            int stopper = 0;
            final double[] newAngles = new double[6];
            newPointTested = this.pointsToTest[closestPoint1 - 1];

            while (MathLib.abs(closestPtAngle1 - closestPointAngle2) > CONVTHRESHOLD
                    || MathLib.abs(distanceMinToPoint1 - distanceMinToPoint2) > CONVTHRESHOLD) {

                // computation of the new tested points
                // existing points
                distances[1] = distanceMinToPoint1;
                newAngles[1] = closestPtAngle1;
                distances[4] = distanceMinToPoint2;
                newAngles[4] = closestPointAngle2;

                // first new point
                newAngles[2] = (2.0 * closestPtAngle1 + closestPointAngle2) / 3.0;
                newPointTested = this.center.add(new Vector3D(MathLib.cos(newAngles[2]) * this.radiusA, this.u, MathLib
                    .sin(newAngles[2]) * this.radiusB, this.v));
                distances[2] = point.subtract(newPointTested).getNorm();

                // second new point
                newAngles[3] = (closestPtAngle1 + 2.0 * closestPointAngle2) / 3.0;
                newPointTested = this.center.add(new Vector3D(MathLib.cos(newAngles[3]) * this.radiusA, this.u, MathLib
                    .sin(newAngles[3]) * this.radiusB, this.v));
                distances[3] = point.subtract(newPointTested).getNorm();

                // the two central points are duplicated
                distances[0] = distances[2];
                distances[5] = distances[3];
                newAngles[0] = newAngles[2];
                newAngles[5] = newAngles[3];

                final double[] closestPoints = closestPoints(newAngles, distances);
                closestPtAngle1 = closestPoints[0];
                closestPointAngle2 = closestPoints[1];
                distanceMinToPoint1 = closestPoints[2];
                distanceMinToPoint2 = closestPoints[3];

                stopper++;
                if (stopper == MAXITERATIONS) {
                    throw new ConvergenceException();
                }
            }
            // the return is the last point created
            closestPoint = newPointTested;

        } else {
            // if the projection of the point in the ellipse's plane belongs to the shape
            closestPoint = this.center.add(projOnU, this.u).add(projOnV, this.v);
        }
        // return of the minus of the distances
        return closestPoint;
    }

    /**
     * Computes the two next closest points and their associated angles for the iterative closest point computation
     * method
     * 
     * @param newAngles
     *        the input angles
     * @param distances
     *        the input distances
     * @see AbstractEllipse#closestPointTo
     * @return the two selected angles and distances
     */
    private static double[] closestPoints(final double[] newAngles, final double[] distances) {

        // Initialisations
        final double[] anglesDistances = new double[4];
        anglesDistances[2] = Double.POSITIVE_INFINITY;
        int closestPoint1 = 1;

        // choice of the closest point
        // Set angle and distance for closest point
        for (int i = 1; i < 5; i++) {
            if (distances[i] < anglesDistances[2]) {
                closestPoint1 = i;
                anglesDistances[0] = newAngles[i];
                anglesDistances[2] = distances[i];
            }
        }

        // choice of the second point amongst the two next to the closest
        // Set angle and distance for second point
        if (distances[closestPoint1 + 1] > distances[closestPoint1 - 1]) {
            anglesDistances[1] = newAngles[closestPoint1 - 1];
            anglesDistances[3] = distances[closestPoint1 - 1];
        } else {
            anglesDistances[1] = newAngles[closestPoint1 + 1];
            anglesDistances[3] = distances[closestPoint1 + 1];
        }

        return anglesDistances;
    }

    /** {@inheritDoc} */
    @Override
    public final Vector3D[] closestPointTo(final Line line) {
        // Initialization
        final Vector3D[] points = new Vector3D[2];

        if (this.intersects(line)) {
            // the line intersects the ellipse

            // Intersections
            final Vector3D[] iscs = this.getIntersectionPoints(line);

            // Return intersection with lowest abscissa on line
            points[0] = line.pointOfMinAbscissa(iscs);
            points[1] = points[0];
        } else {
            // the line does not intersect the ellipse
            final double angle = this.ellipseDistance(line)[1];
            final double[] sincos = MathLib.sinAndCos(angle);
            final double sin = sincos[0];
            final double cos = sincos[1];
            points[1] = this.center.add(new Vector3D(cos * this.radiusA, this.u, sin * this.radiusB, this.v));
            points[0] = line.toSpace(line.toSubSpace(points[1]));
        }

        return points;
    }
}
