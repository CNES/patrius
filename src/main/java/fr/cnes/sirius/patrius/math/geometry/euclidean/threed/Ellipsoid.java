/**
 * 
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history Created on 05/10/2011
 * 
 *          HISTORY
* VERSION:4.7:FA:FA-2654:18/05/2021:Erreur dans la détection de phénomènes d'éblouissement d'un senseur
* VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:FA:FA-2470:27/05/2020:Optimisation de la methode runNewtonAlgorithm de la classe Ellipsoid
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 *          VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour if/else "nominal" dans
 *          Ellipsoid.getPointLocation()
 *          VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *          VERSION::FA:180:18/03/2014:Improved code by using an enum instead of ints
 *          VERSION::FA:---:11/04/2014:Quality assurance
 *          VERSION::FA:345:30/10/2014:modified comments ratio
 *          VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 *          VERSION::FA:650:22/07/2016: ellipsoid corrections
 *          VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 *          END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.LUDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * This is the Ellipsoid (also called Revolved Ellipsoid) class.This class cannot represent all ellipsoid objects.
 * </p>
 * <p>
 * It creates an ellipsoid object.
 * </p>
 * <p>
 * <u>Usage:</u> With two Vector3D for position and Rev. Axis and three doubles for the three semi axes, call <br>
 * <center>Ellipsoid myEllipsoid = new Ellipsoid(position, axis, a, b, c)</center>
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see IEllipsoid
 * @see SolidShape
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Ellipsoid.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.2
 * 
 */
public class Ellipsoid implements IEllipsoid, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8950995549840576381L;

    /** Incrementation rate limitation is pi/rate */
    private static final double RATE = 25;

    /** Vector norm comparison (constructor) */
    private static final double CSTEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Double comparison */
    private static final double EPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Default epsilon for Newton algorithm. */
    private static final double DEFAULT_NEWTON_EPS = 1E-11;

    /** Singularity Threshold */
    private static final double SINGULARITY_EPS = 1e-15;

    /** Newton loop limit (set high for highly degenerated cases) */
    private static final int NEWTONLIMIT = 1000000;

    /** Epsilon for Newton algorithm. */
    private double epsNewton;

    /** Ellipsoid center position */
    private final Vector3D position;

    /** Ellipsoid X-axis in standard basis */
    private final Vector3D ellipsoidX;

    /** Ellipsoid Y-axis in standard basis */
    private final Vector3D ellipsoidY;

    /** Ellipsoid axis of revolution in standard basis */
    private final Vector3D ellipsoidZ;

    /** Ellipsoid semi-axis a (in plan orthogonal to axis of revolution) */
    private final double a;

    /** Ellipsoid semi-axis b (in plan orthogonal to axis of revolution) */
    private final double b;

    /** Ellipsoid semi-axis c (along ellipsoids axis of revolution) */
    private final double c;

    /** Ellipsoid semi-axis a to the power of 2 */
    private final double a2;

    /** Ellipsoid semi-axis b to the power of 2 */
    private final double b2;

    /** Ellipsoid semi-axis c to the power of 2 */
    private final double c2;

    /** Ellipsoid basis of local */
    private final Matrix3D standardBasisTransform;

    /** Ellipsoid inverted basis of local */
    private final Matrix3D localBasisTransform;

    /** Smallest axis */
    private SmallestAxis smallest;

    /** Normalized ellipsoid semi-axis a (in plan orthogonal to axis of revolution) */
    private final double an;

    /** Normalized ellipsoid semi-axis b (in plan orthogonal to axis of revolution) */
    private final double bn;

    /** Normalized ellipsoid semi-axis c (along ellipsoids axis of revolution) */
    private final double cn;

    /** Normalized ellipsoid semi-axis a to the power of 2 */
    private final double a2n;

    /** Normalized ellipsoid semi-axis b to the power of 2 */
    private final double b2n;

    /** Normalized ellipsoid semi-axis c to the power of 2 */
    private final double c2n;

    /** Normalized ellipsoid norm factor (average of [a, b, c]) */
    private final double normFactor;

    /** Used to pinpoint the smallest axis */
    private enum SmallestAxis {
        /** Semi A */
        A,
        /** Semi B */
        B,
        /** Semi C */
        C
    }

    /** Enums to locate point. Members self explanatory */
    private enum PointLocation {
        /** Outside on X axis. */
        OutsideX,
        /** Outside on Y axis. */
        OutsideY,
        /** Outside on Z axis. */
        OutsideZ,
        /** Outside on no particular axis. */
        Outside,
        /** On the surface of the ellipsoid. */
        OnEllipsoid,
        /** Inside the ellipsoid. */
        Inside,
        /** At the center of the ellipsoid. */
        AtCenter,
        /** Should never happen. */
        Error;
    }

    /**
     * This constructor builds a ellipsoid from its centers position, its revolution axis and its transverse and
     * conjugate radii. A ellipsoid, or ellipsoid of revolution, is a quadric surface obtained by rotating an ellipse
     * about one of its principal axes.
     * 
     * @param myPosition
     *        The position of the ellipsoids center
     * @param myRevAxis
     *        The axis of revolution of the ellipsoid
     * @param myXAxis
     *        The axis of semi major axis a. Will be normalized and taken orthogonal to myRevAxis.
     * @param myA
     *        Transverse radius : semi axis of the ellipsoid along a direction orthogonal to the axis of revolution
     * @param myB
     *        Transverse radius : semi axis of the ellipsoid along a direction orthogonal to the axis of revolution
     *        (orthogonal to myA direction)
     * @param myC
     *        Conjugate radius : semi axis of the ellipsoid along the axis of revolution
     * 
     * @exception IllegalArgumentException
     *            if semi-axis or norm of revolution axis is null
     */
    public Ellipsoid(final Vector3D myPosition, final Vector3D myRevAxis, final Vector3D myXAxis, final double myA,
                     final double myB, final double myC) {

        final String message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());

        // Test for validity of user inputs
        // - The semi axis values must be positive
        // - The norm of the revolution axis must be different than 0
        if (Precision.equals(myA, 0.0) || myA < 0) {
            throw new IllegalArgumentException(message);
        }
        if (Precision.equals(myB, 0.0) || myB < 0) {
            throw new IllegalArgumentException(message);
        }
        if (Precision.equals(myC, 0.0) || myC < 0) {
            throw new IllegalArgumentException(message);
        }
        // The 1e-10 epsilon is chosen to be coherent with the Plane class
        if (myRevAxis.getNorm() < CSTEPS) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }
        // The 1e-10 epsilon is chosen to be coherent with the Plane class
        if (myXAxis.getNorm() < CSTEPS) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        // If tests pass, assign user input to object parameters
        this.position = myPosition;

        this.a = myA;
        this.b = myB;
        this.c = myC;

        this.a2 = this.a * this.a;
        this.b2 = this.b * this.b;
        this.c2 = this.c * this.c;

        this.normFactor = (this.a + this.b + this.c) / 3.;
        this.an = this.a / this.normFactor;
        this.bn = this.b / this.normFactor;
        this.cn = this.c / this.normFactor;
        this.a2n = this.an * this.an;
        this.b2n = this.bn * this.bn;
        this.c2n = this.cn * this.cn;

        this.pinpointSmallestAxis();
        this.setNewtonThreshold(DEFAULT_NEWTON_EPS);

        /**
         * This computes the matrices that allow the transformation from the local basis to the standard basis. Indeed,
         * the matrix containing the local basis vectors expressed in the standard basis is the matrix that allows the
         * transformation from the local basis to the standard basis, and is called the {@code standardBasisTransform}
         * matrix. It's inverse, which allows the transformation from the standard basis to the local local basis, is
         * the transpose of the {@code standardBasisTransform} and is herein designated as {@code localBasisTransform}.
         */
        this.ellipsoidZ = myRevAxis.normalize();
        this.ellipsoidX = myXAxis.subtract(myXAxis.dotProduct(this.ellipsoidZ), this.ellipsoidZ).normalize();
        this.ellipsoidY = this.ellipsoidZ.crossProduct(this.ellipsoidX);

        final double[][] matrixData = {
            { this.ellipsoidX.getX(), this.ellipsoidY.getX(), this.ellipsoidZ.getX() },
            { this.ellipsoidX.getY(), this.ellipsoidY.getY(), this.ellipsoidZ.getY() },
            { this.ellipsoidX.getZ(), this.ellipsoidY.getZ(), this.ellipsoidZ.getZ() } };
        this.standardBasisTransform = new Matrix3D(matrixData);

        // Computes the local basis transform
        this.localBasisTransform = this.standardBasisTransform.transpose();

    }

    /**
     * Check the smallest axis
     * 
     * @see Ellipsoid
     */
    private void pinpointSmallestAxis() {
        if (this.a >= this.b) {
            if (this.b >= this.c) {
                this.smallest = SmallestAxis.C;
            } else {
                this.smallest = SmallestAxis.B;
            }
        } else if (this.b >= this.a) {
            if (this.a >= this.c) {
                this.smallest = SmallestAxis.C;
            } else {
                this.smallest = SmallestAxis.A;
            }
        }
        // The following lines should not happen hence in comment
        // else if (c >= a) {
        // if (a >= b) {
        // this.smallest = SmallestAxis.B;
        // } else {
        // this.smallest = SmallestAxis.A;
        // }
        // }
    }

    /**
     * @return the ellipsoid semi principal X vector
     */
    public Vector3D getSemiPrincipalX() {
        return this.ellipsoidX;
    }

    /**
     * @return the ellipsoid semi principal Y vector
     */
    public Vector3D getSemiPrincipalY() {
        return this.ellipsoidY;
    }

    /**
     * @return the ellipsoid semi principal Z vector
     */
    public Vector3D getSemiPrincipalZ() {
        return this.ellipsoidZ;
    }

    /**
     * Get ellipsoid center
     * 
     * @return the position of the ellipsoids center
     */
    @Override
    public Vector3D getCenter() {
        return this.position;
    }

    /**
     * Get the length of the semi principal axis X
     * 
     * @return semi principal axis X length
     */
    @Override
    public double getSemiA() {
        return this.a;
    }

    /**
     * Get the length of the semi principal axis Y
     * 
     * @return semi principal axis Y length
     */
    @Override
    public double getSemiB() {
        return this.b;
    }

    /**
     * Get the length of the semi principal axis Z
     * 
     * @return semi principal axis Z length
     */
    @Override
    public double getSemiC() {
        return this.c;
    }

    /**
     * Get transformation matrix (from ellipsoid local basis to standard basis)
     * 
     * @return standardBasisTransform the local basis transform matrix
     */
    public Matrix3D getStandardBasisTransform() {
        return this.standardBasisTransform;
    }

    /**
     * Get transformation matrix (from standard basis to ellipsoid local basis)
     * 
     * @return localBasisTransform the local basis transformation matrix
     */
    public Matrix3D getLocalBasisTransform() {
        return this.localBasisTransform;
    }

    /**
     * Express a Vector3D in ellipsoid local basis. Warning : Affine transformation
     * 
     * @param myVector
     *        Vector expressed in standard basis
     * @return vectorRef Same vector expressed in ellipsoid local basis
     */
    public Vector3D getAffineLocalExpression(final Vector3D myVector) {
        // Transformation is V_local = M^(-1) * (V_global - Origin)
        return this.localBasisTransform.multiply(myVector.subtract(this.position));
    }

    /**
     * Express a Vector3D in standard basis. Warning : Affine transformation
     * 
     * @param myVector
     *        Vector expressed in ellipsoid local basis
     * @return vectorRef Same vector expressed in standard basis
     */
    public Vector3D getAffineStandardExpression(final Vector3D myVector) {
        // Transformation is V_global = M * V_local + Origin
        return this.standardBasisTransform.multiply(myVector).add(this.position);
    }

    /**
     * Express a Vector3D in ellipsoid local basis. Warning : Vectorial transformation
     * 
     * @param myVector
     *        Vector expressed in standard basis
     * @return vectorRef Same vector expressed in ellipsoid local basis
     */
    public Vector3D getVectorialLocalExpression(final Vector3D myVector) {
        // Transformation is V_local = M^(-1) * V_global
        return this.localBasisTransform.multiply(myVector);
    }

    /**
     * Express a Vector3D in standard basis. Warning : Vectorial transformation
     * 
     * @param myVector
     *        Vector expressed in ellipsoid local basis
     * @return vectorRef Same vector expressed in standard basis
     */
    public Vector3D getVectorialStandardExpression(final Vector3D myVector) {
        // Transformation is V_global = M * V_local
        return this.standardBasisTransform.multiply(myVector);
    }

    /**
     * Convert from Ellipsoid to Cartesian coordinates
     * 
     * @param theta
     *        theta angle in local basis
     * @param phi
     *        phi angle
     * @return triplet (x,y,z) as a double[] expressed in ellipsoid local basis
     */
    public double[] getCartesianCoordinates(final double theta, final double phi) {

        // Container
        final double[] coords = new double[3];

        // Theta / Phi
        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double sinTheta = sincosTheta[0];
        final double cosTheta = sincosTheta[1];
        final double[] sincosPhi = MathLib.sinAndCos(phi);
        final double sinPhi = sincosPhi[0];
        final double cosPhi = sincosPhi[1];

        // Transformation
        coords[0] = this.a * cosPhi * cosTheta;
        coords[1] = this.b * cosPhi * sinTheta;
        coords[2] = this.c * sinPhi;

        return coords;
    }

    /**
     * Convert from Cartesian to Ellipsoid coordinates
     * 
     * @param point
     *        Point as a Vector3D in local basis
     * @return angle coordinates (theta, phi)
     */
    public double[] getEllipsoidicCoordinates(final Vector3D point) {

        // Container
        final double[] coords = new double[2];

        // Transformation
        coords[0] = MathLib.atan2(point.getY() / this.b, point.getX() / this.a);
        coords[1] = MathLib.atan2(point.getZ() / this.c,
            MathLib.sqrt((point.getX() * point.getX()) / (this.a2) + (point.getY() * point.getY()) / (this.b2)));

        return coords;
    }

    /**
     * Computes the normal vector to the surface in local basis
     * 
     * @param point
     *        Point as a Vector3D in local basis
     * @return the normal vector in local basis
     */
    @Override
    public Vector3D getNormal(final Vector3D point) {
        // ellipsoidic coordinates of closest point
        final double[] cc = this.getEllipsoidicCoordinates(point);
        final double[] sincosTheta = MathLib.sinAndCos(cc[0]);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];
        final double[] sincosPhi = MathLib.sinAndCos(cc[1]);
        final double sp = sincosPhi[0];
        final double cp = sincosPhi[1];

        // tangents to ellipsoid surface
        final Vector3D v1 = new Vector3D(-this.a * st * cp, this.b * ct * cp, 0);
        final Vector3D v2 = new Vector3D(-this.a * ct * sp, -this.b * st * sp, this.c * cp);

        return Vector3D.crossProduct(v1, v2).normalize();
    }

    /**
     * Get polynomial discriminant.
     * 
     * @param ul
     *        User specified line expressed as a {@link LineCoefficients}
     * @return delta
     */
    private double[] getDiscriminantAndCoeffs(final LineCoefficients ul) {

        /**
         * Compute intersection with a line
         * 
         * The line is expressed in the following parametric equation system (considered in the ellipsoid local basis) :
         * 
         * For each coordinate q_i,
         * 
         * q_i = alpha_i * t + beta_i with alpha_i and beta_i constant and t in R
         * 
         * Thus, the line intersects the ellipsoid if some value of t is found to satisfy the ellipsoid equation. This
         * equation, in the ellipsoid local basis, is given by :
         * 
         * x²/a² + y²/b² + z²/c² = 1
         * 
         * By reinjecting the triplet (x,y,z) corresponding to a point the line, we obtain a second degree polynomial
         * that is given by :
         * 
         * t2coeff x t² + t1coeff * t + t0coeff = 0
         * 
         * Solutions of this polynomial indicate that the point corresponding to the t_sol value satisfy the ellipsoid
         * equation, hereby belonging to the intersection of the line and the ellipsoid.
         */
        final double t2coeff = ul.bx * ul.bx / this.a2 + ul.by * ul.by / this.b2 + ul.bz * ul.bz / this.c2;
        final double t1coeff = (ul.bx * ul.ax / this.a2 + ul.by * ul.ay / this.b2 + ul.bz * ul.az / this.c2) * 2;
        final double t0coeff = ul.ax * ul.ax / this.a2 + ul.ay * ul.ay / this.b2 + ul.az * ul.az / this.c2 - 1;

        final double delta = t1coeff * t1coeff - 4 * t2coeff * t0coeff;

        return new double[] { delta, t2coeff, t1coeff, t0coeff };
    }

    /**
     * If more than one intersection points are found, the closest to the line's origin is returned first
     * 
     * @param line
     *        line with which the intersections are calculated
     * @return intersections an array of Vector3D objects. Empty if no intersections
     */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // Initialise intersections container
        final Vector3D[] result;

        // Get line equations in ellipsoid basis
        final LineCoefficients ul = new LineCoefficients(line);

        // Coefficients for intersection points computation
        final double[] equationData = this.getDiscriminantAndCoeffs(ul);
        final double delta = equationData[0];
        final double t2coeff = equationData[1];
        final double t1coeff = equationData[2];

        if (delta > 0) {
            // Two intersections
            result = new Vector3D[2];

            // Compute the corresponding t parameter
            final double t1 = (-t1coeff + MathLib.sqrt(delta)) / (2 * t2coeff);
            final double t2 = (-t1coeff - MathLib.sqrt(delta)) / (2 * t2coeff);

            // Compute the Positions in ellipsoid basis
            final Vector3D p1 = ul.getPoint(t1);
            final Vector3D p2 = ul.getPoint(t2);

            // Return points expressed in standard basis.
            // P1 is the closest of line origin when t1coeff and t2coeff have same sign
            // (negative sum of roots t1 and t2)
            if (t1coeff * t2coeff > 0) {
                result[0] = this.getAffineStandardExpression(p1);
                result[1] = this.getAffineStandardExpression(p2);
            } else {
                result[0] = this.getAffineStandardExpression(p2);
                result[1] = this.getAffineStandardExpression(p1);
            }

        } else if (Precision.equals(delta, 0)) {
            // One intersection
            result = new Vector3D[1];

            // Compute the corresponding t parameter
            final double t1 = (-t1coeff) / (2 * t2coeff);

            // Compute the Positions in ellipsoid basis
            final Vector3D p1 = ul.getPoint(t1);

            // Return point expressed in standard basis.
            // This is an affine transformation.
            result[0] = this.getAffineStandardExpression(p1);

        } else {
            // No intersections
            result = new Vector3D[0];
        }

        return result;
    }

    /**
     * This method returns true if the line intersects the ellipsoid
     * 
     * @param line
     *        line for the computation of the intersection
     * @return a boolean set to true if the line indeed intersects the ellipsoid
     */
    @Override
    public boolean intersects(final Line line) {
        // Get discriminant
        final double[] delta = this.getDiscriminantAndCoeffs(new LineCoefficients(line));
        return (delta[0] > 0 || Precision.equals(delta[0], 0));
    }

    /**
     * This method returns a double that represents the location of the user point on the ellipsoid.
     * 
     * @precondition The point must be expressed in the standard basis
     * 
     * @param point
     *        User point in local frame
     * @return location as a double
     */
    private PointLocation getPointLocation(final Vector3D point) {

        // Result initialization
        PointLocation result = PointLocation.Error;

        // Local coordinates
        final double x = point.getX();
        final double y = point.getY();
        final double z = point.getZ();
        final double absZ = MathLib.abs(z);

        // Ellipsoid equation
        final double zEllipsoidSquare = (1 - (x * x) / (this.a2) - (y * y) / (this.b2)) * this.c2;
        if (zEllipsoidSquare >= 0. && !Double.isNaN(zEllipsoidSquare)) {
            final double zEllipsoid = MathLib.sqrt(zEllipsoidSquare);
            if (Precision.equals(absZ, zEllipsoid, EPS)) {
                // zEllipsoid is defined and z = zEllipsoid
                result = PointLocation.OnEllipsoid;
            } else if (absZ < zEllipsoid) {
                // zEllipsoid is defined and z < zEllipsoid, z is always positive here, user point is inside
                result = PointLocation.Inside;
                if (Precision.equals(x, 0, EPS) && Precision.equals(y, 0, EPS)
                        && Precision.equals(z, 0, EPS)) {
                    // if point is at center of ellipsoid
                    result = PointLocation.AtCenter;
                }
            } else {
                // Either : - zEllipsoid is defined and z > zEllipsoid
                result = this.getPointLocationPart2(x, y, z);
            }
        } else {
            // zEllipsoid is NaN (point outside ellipsoid)
            result = this.getPointLocationPart2(x, y, z);
        }
        return result;
    }

    /**
     * Part 2 of getPointLocation
     * 
     * @param x
     *        coordinate
     * @param y
     *        coordinate
     * @param z
     *        coordinate
     * @return result
     * @see Ellipsoid#getPointLocation(Vector3D)
     */
    private PointLocation getPointLocationPart2(final double x, final double y, final double z) {
        /* In this case, the point is outside the ellipsoid */

        // init result
        final PointLocation result;

        if (Precision.equals(x, 0, EPS) && Precision.equals(y, 0, EPS)) {
            // Point is on Z axis
            result = PointLocation.OutsideZ;
        } else if (Precision.equals(x, 0, EPS) && Precision.equals(z, 0, EPS)) {
            // Point is on Y axis
            result = PointLocation.OutsideY;
        } else if (Precision.equals(y, 0, EPS) && Precision.equals(z, 0, EPS)) {
            // Point is on X axis
            result = PointLocation.OutsideX;
        } else {
            /*
             * Point isnt on any axis and is outside of ellipsoid, because either z > zEllipsoid zPheroid is NaN, i.e.
             * the (x,y) point cant satisfy the ellipsoid equation
             */
            result = PointLocation.Outside;
        }

        return result;
    }

    /**
     * Returns true if the point is inside the ellipsoid
     * 
     * @param point
     *        user point in standard basis
     * @return boolean if is inside
     * 
     * @see Ellipsoid#getPointLocation(Vector3D)
     */
    private boolean isInside(final Vector3D point) {

        final boolean res;
        // local point
        final Vector3D localPoint = this.getAffineLocalExpression(point);

        // ellipsoid equ
        final double x = localPoint.getX();
        final double y = localPoint.getY();
        final double z = MathLib.abs(localPoint.getZ());

        final double zEllipsoidSquare = 1 - (x * x) / (this.a2) - (y * y) / (this.b2);
        if (zEllipsoidSquare >= 0. && !Double.isNaN(zEllipsoidSquare)) {
            final double zEllipsoid = this.c * MathLib.sqrt(zEllipsoidSquare);
            // Return true if the point is inside
            res = z < zEllipsoid;
        } else {
            // zEllipsoid is NaN: point is outside
            res = false;
        }
        return res;
    }

    /**
     * Computes the transformation from any point in R³ its equivalent in the first octant. This function returns a 1x3
     * array containing +1 if the coordinate is in the first octant, -1 otherwise.
     * 
     * @param point
     *        user point expressed in ellipsoid local basis
     * @return triplet of ±1
     */
    private double[] getOctantTransformation(final Vector3D point) {

        // Get user point coordinates in ellipsoid basis
        final double x = point.getX();
        final double y = point.getY();
        final double z = point.getZ();

        // Get transformation data
        final double xT = x < 0 ? -1 : 1;
        final double yT = y < 0 ? -1 : 1;
        final double zT = z < 0 ? -1 : 1;

        return new double[] { xT, yT, zT };
    }

    /**
     * Return angular coordinates of closest intersection point of line (center - myPoint) and ellipsoid. These values
     * will serve as a starting point for the Newton algorithm. Performance increase is about 20 to 30% (in iterations)
     * in case it is a ellipsoid. In case the instantiated object is a sphere, the angular values are the exact solution
     * of the closest point problem.
     * 
     * @param myPoint
     *        point outside the ellipsoid expressed in ellipsoid local basis
     * @return ellipsoidic coordinates of the intersection point (center - point) / (ellipsoid)
     */
    private double[] getOptimizedStartingLocation(final Vector3D myPoint) {

        // Intersection between the ellipsoid and the line going throught the point and the center
        final Vector3D lineOrg = this.getAffineStandardExpression(myPoint);
        final Vector3D[] intersections = this.getIntersectionPoints(new Line(lineOrg, this.position));

        int index = 0;
        if (intersections[0].subtract(lineOrg).getNorm() > intersections[1].subtract(lineOrg).getNorm()) {
            index = 1;
        }

        // get ellipsoidic coordinates!
        return this.getEllipsoidicCoordinates(this.getAffineLocalExpression(intersections[index]));
    }

    /**
     * This method is the Newton Algorithm that calculates the numerical solution of the multivariable problem
     * {@code F(q_i) = 0}.
     * 
     * <center>{@code x(n+1) = x(n) - J(n)^-1 * F(n)}</center><br>
     * 
     * It implements the algorithm described in the below reference with the single difference of the algorithm starting
     * point. Tests show the starting location defined as being the intersection of the line (center to point) and
     * (ellipsoid) is slightly more efficient than the starting point suggested in the paper.
     * 
     * @param x
     *        coordinate of user point
     * @param y
     *        coordinate of user point
     * @param z
     *        coordinate of user point
     * @return Cartesian coordinates (expressed in local basis) of closest computed point
     * 
     * @see Ellipsoid#getFPoint(double, double, double, double, double)
     * @see Ellipsoid#getFpPoint(double, double, double, double, double)
     * @see <a href="http://www2.imperial.ac.uk/~rn/distance2ellipse.pdf"> <i>Distance from a point to an ellipsoid</i>,
     *      Robert Nürnberg, Imperial College London, 2006.</a>
     */
    private double[] runNewtonAlgorithm(final double x, final double y, final double z) {

        // Normalization
        final double xn = x / this.normFactor;
        final double yn = y / this.normFactor;
        final double zn = z / this.normFactor;

        // Incrementation thresholds
        final double thetaRate = FastMath.PI / RATE;
        final double phiRate = FastMath.PI / RATE;

        // Initialisation of computed point coordinates
        final double[] coords = this.getOptimizedStartingLocation(new Vector3D(xn, yn, zn));

        /*
         * The following angluar values are suggested as starting values for planetographic longitude and parametric
         * latitude by the article.
         * θ value : atan2(a * y, a * x) φ value : atan2(z, c * sqrt( (x * x / (a2) + y * y / (b2)) ) )
         */

        // The following starting point has been found to be slightly more efficient
        double theta = coords[0];
        double phi = coords[1];

        // Initialisation of containers
        double det;
        double[][] invData;
        double[] state = new double[] { theta, phi };
        double[] step;
        double[] myF;
        double[][] myFp;
        double[][] myFpInv;
        double stepNorm = 1;

        int i = 0;

        while (i < NEWTONLIMIT && stepNorm > this.epsNewton) {

            // Get new step values
            myF = this.getFPoint(theta, phi, xn, yn, zn);
            myFp = this.getFpPoint(theta, phi, xn, yn, zn);

            // Invert Jacobian Matrix
            det = myFp[0][0] * myFp[1][1] - myFp[1][0] * myFp[0][1];
            if (det == 0) {
                // Specific case
                step = new double[2];
            } else {
                invData = new double[][] {
                        { MathLib.divide(myFp[1][1], det),
                            -MathLib.divide(myFp[0][1], det) },
                        { -MathLib.divide(myFp[1][0], det), MathLib.divide(myFp[0][0], det) } };
                myFpInv = invData;

                // Calculate step
                step = new double[] { myFpInv[0][0] * myF[0] + myFpInv[0][1] * myF[1],
                        myFpInv[1][0] * myF[0] + myFpInv[1][1] * myF[1] };

                // Saturate incrementations in order to prevent divergence of algorithm
                final double sign00 = step[0] >= 0 ? 1 : -1;
                step[0] = sign00 * MathLib.min(MathLib.abs(step[0]), thetaRate);
                final double sign10 = step[1] >= 0 ? 1 : -1;
                step[1] = sign10 * MathLib.min(MathLib.abs(step[1]), phiRate);
            }

            // new Values
            stepNorm = MathLib.max(MathLib.abs(step[0]), MathLib.abs(step[1]));
            state = new double[] { state[0] - step[0], state[1] - step[1] };

            // store theta and phi values
            theta = state[0];
            phi = state[1];

            // keep track of steps
            i++;

        }
        if (i >= NEWTONLIMIT) {
            throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, NEWTONLIMIT);
        }

        return this.getCartesianCoordinates(theta, phi);
    }

    /**
     * This method is the Newton Algorithm that calculates the numerical solution of the multivariable problem
     * {@code F(q_i) = 0}.
     * 
     * <center>{@code x(n+1) = x(n) - J(n)^-1 * F(n)}</center><br>
     * 
     * It implements an augmented version of the algorithm described in the below reference with the single difference
     * of the algorithm starting point. Tests show the starting location defined as being the intersection of the line
     * (center to point) and (ellipsoid) is slightly more efficient than the starting point suggested in the paper.
     * 
     * @param line
     *        user line expressed in local ellipsoid frame
     * @return Cartesian coordinates (expressed in local basis) of closest computed point
     * 
     * @see Ellipsoid#getFLine(double, double, double, LineCoefficients)
     * @see Ellipsoid#getFpLine(double, double, double, LineCoefficients)
     * @see <a href="http://www2.imperial.ac.uk/~rn/distance2ellipse.pdf"> <i>Distance from a point to an ellipsoid</i>,
     *      Robert Nürnberg, Imperial College London, 2006.</a>
     */
    private double[] runNewtonAlgorithmLine(final LineCoefficients line) {

        // Build normalized line
        final Vector3D normOrigin = line.getLineOrigin().scalarMultiply(1. / this.normFactor);
        final LineCoefficients normLine = new LineCoefficients(normOrigin, line.getLineDirection());

        // Incrementation thresholds
        final double thetaRate = FastMath.PI / RATE;
        final double phiRate = FastMath.PI / RATE;
        final double tRate = thetaRate;

        // Initialisation of computed point coordinates
        final double[] coords = this.getOptimizedStartingLocation(normLine.getPoint(0));
        if (Precision.equals(MathLib.abs(coords[1]), FastMath.PI / 2)) {
            final double sign = coords[1] >= 0 ? 1 : -1;
            coords[1] = sign * FastMath.PI / 4;
        }

        /*
         * The following angluar values are suggested as starting values for planetographic longitude and parametric
         * latitude by the article.
         */

        // The following starting point has been found to be slightly more efficient
        double theta = coords[0];
        double phi = coords[1];
        double t = 0;

        // Initialisation of containers
        RealMatrix state = new Array2DRowRealMatrix(new double[][] { { theta }, { phi }, { t } });
        RealMatrix step;
        RealMatrix myF = this.getFLine(theta, phi, t, normLine);
        RealMatrix myFp = this.getFpLine(theta, phi, t, normLine);
        RealMatrix myFpInv;
        DecompositionSolver solver = new LUDecomposition(myFp, SINGULARITY_EPS).getSolver();
        boolean singularityFlag = false;
        double stepNorm = 1;

        // counter
        int i = 0;

        while (i < NEWTONLIMIT && stepNorm > this.epsNewton && !singularityFlag) {

            /*
             * SOLVE CURRENT STEP
             */

            // Invert Jacobian Matrix
            myFpInv = solver.getInverse();

            // Calculate step
            step = myFpInv.multiply(myF);

            // Saturate incrementations in order to prevent divergence of algorithm
            final double sign00 = step.getEntry(0, 0) >= 0 ? 1 : -1;
            step.setEntry(0, 0, sign00 * MathLib.min(MathLib.abs(step.getEntry(0, 0)), thetaRate));

            final double sign10 = step.getEntry(1, 0) >= 0 ? 1 : -1;
            step.setEntry(1, 0, sign10 * MathLib.min(MathLib.abs(step.getEntry(1, 0)), phiRate));

            final double sign20 = step.getEntry(2, 0) >= 0 ? 1 : -1;
            step.setEntry(2, 0, sign20 * MathLib.min(MathLib.abs(step.getEntry(2, 0)), tRate));

            // new Values
            stepNorm = step.getNorm();
            state = state.subtract(step);

            // store theta and phi values
            theta = state.getEntry(0, 0);
            phi = state.getEntry(1, 0);
            t = state.getEntry(2, 0);

            // keep track of steps
            i++;

            /*
             * PREPARE NEXT STEP
             */

            // Get new step values
            myF = this.getFLine(theta, phi, t, normLine);
            myFp = this.getFpLine(theta, phi, t, normLine);

            // get new solver for matrix inversion
            solver = new LUDecomposition(myFp, SINGULARITY_EPS).getSolver();
            singularityFlag = !solver.isNonSingular();
       
        }
        if (i >= NEWTONLIMIT) {
            throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, NEWTONLIMIT);
        }

        final double[] coord = this.getCartesianCoordinates(theta, phi);

        return new double[] { coord[0], coord[1], coord[2], t * this.normFactor };
    }

    /**
     * Computes the function value {@code F(θ, φ) = 0} where θ and φ represent the ellipsoidic coordinates and {@code F}
     * the function defined by the equations expressing the condition that the point {@code x} on the ellipsoid
     * achieving the minimal distance to the user point {@code p} is such as {@code (p-x)} is perpendicular to the
     * ellipsoids surface :<br>
     * <ul>
     *     {@code (1)     F_θ(θ, φ) = (p - x)•dx/dθ}<br>
     *     {@code (2)     F_φ(θ, φ) = (p - x)•dx/dφ}
     * </ul>
     * {@code p} being the user specified point and<br>
     * {@code x} being the point of the ellipsoid<br>
     * <br>
     * 
     * @param theta
     *        planetographic longitude θ (in radians)
     * @param phi
     *        parametric latitude φ (in radians)
     * @param x
     *        coordinate of point
     * @param y
     *        coordinate of point
     * @param z
     *        coordinate of point
     * 
     * @return the function calculated with given data in the form of a 2x1 matrix
     * 
     * @see Ellipsoid#getFpPoint(double, double, double, double, double)
     * @see Ellipsoid#runNewtonAlgorithm(double, double, double)
     * 
     */
    private double[] getFPoint(final double theta, final double phi, final double x, final double y,
                                           final double z) {

        // Angles
        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];
        final double[] sincosPhi = MathLib.sinAndCos(phi);
        final double sp = sincosPhi[0];
        final double cp = sincosPhi[1];

        // Function values
        final double f1 = (this.a2n - this.b2n) * ct * st * cp - x * this.an * st + y * this.bn * ct;
        final double f2 =
            (this.a2n * ct * ct + this.b2n * st * st - this.c2n) * cp * sp - x * this.an * sp * ct - y * this.bn * sp
                    * st
                    + z * this.cn * cp;

        // Result in Matrix3D
        return new double[] { f1, f2 };

    }

    /**
     * Computes the jacobian matrix of the function {@code F(θ, φ)} where theta and phi represent the ellipsoidic
     * coordinates and {@code F} the function defined by the equations expressing the condition that the point {@code x}
     * on the ellipsoid achieving the minimal distance to the user point {@code p} is such as {@code (p-x)} is
     * perpendicular to the ellipsoids surface (detailed above). The Jacobian matrix is given by the partial derivatives
     * of {@code F} with respect to parameters {@code θ} and {@code φ} :<br>
     * 
     * <ul>
     * {@code J_F(θ, φ) =  ( dF_θ/dθ (θ, φ)    dF_θ/dφ (θ, φ) )}<br>
     * {@code              ( dF_φ/dθ (θ, φ)    dF_φ/dφ (θ, φ) )}
     * </ul>
     * 
     * @param theta
     *        planetographic longitude θ (in radians)
     * @param phi
     *        parametric latitude φ (in radians)
     * @param x
     *        coordinate of point
     * @param y
     *        coordinate of point
     * @param z
     *        coordinate of point
     * 
     * @return the jacobian matrix calculated with given data contained in a 2x2 matrix
     * 
     * @see Ellipsoid#getFPoint(double, double, double, double, double)
     * @see Ellipsoid#runNewtonAlgorithm(double, double, double)
     * 
     */
    private double[][] getFpPoint(final double theta, final double phi, final double x, final double y,
                                            final double z) {

        // Angles
        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];
        final double[] sincosPhi = MathLib.sinAndCos(phi);
        final double sp = sincosPhi[0];
        final double cp = sincosPhi[1];
        // Squared angles
        final double ct2 = ct * ct;
        final double st2 = st * st;
        final double cp2 = cp * cp;
        final double sp2 = sp * sp;

        // Derivatives
        final double fp11 = (this.a2n - this.b2n) * (ct2 - st2) * cp - x * this.an * ct - y * this.bn * st;
        final double fp12 = -(this.a2n - this.b2n) * ct * st * sp;
        final double fp21 =
            -2 * (this.a2n - this.b2n) * ct * st * sp * cp + x * this.an * st * sp - y * this.bn * sp * ct;
        final double fp22 =
            (this.a2n * ct2 + this.b2n * st2 - this.c2n) * (cp2 - sp2) - x * this.an * cp * ct - y * this.bn * cp * st
                    - z * this.cn * sp;

        // Return Matrix3D
        return new double[][] { { fp11, fp12 }, { fp21, fp22 } };

    }

    /**
     * Computes the function value {@code F(θ, φ, t) = 0} where θ and φ represent the ellipsoidic coordinates, t
     * represents the location on the line and {@code F} the function defined by the equations expressing the condition
     * that the points {@code p, l} on the ellipsoid and the line achieving the minimal distance are such as
     * {@code (p-l)} is perpendicular to the ellipsoids surface :<br>
     * <ul>
     *     {@code (1)     F_θ(θ, φ, t) = (p - l)•dp/dθ}<br>
     *     {@code (2)     F_φ(θ, φ, t) = (p - l)•dp/dφ}<br>
     *     {@code (3)     F_t(θ, φ, t) = (p - l)•dl/dt}<br>
     * </ul>
     * {@code p} being the point on the ellipsoid<br>
     * {@code l} being the point on the line<br>
     * <br>
     * 
     * @param theta
     *        planetographic longitude θ (in radians)
     * @param phi
     *        parametric latitude φ (in radians)
     * @param t
     *        location on the line (ax + t * bx)
     * @param line
     *        user line in local ellipsoid frame
     * 
     * @return the function calculated with given data in the form of a 2x1 matrix
     * 
     * @see Ellipsoid#getFpLine(double, double, double, LineCoefficients)
     * @see Ellipsoid#runNewtonAlgorithmLine(LineCoefficients)
     * 
     */
    private Array2DRowRealMatrix getFLine(final double theta, final double phi, final double t,
                                          final LineCoefficients line) {

        // Angles

        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];
        final double[] sincosPhi = MathLib.sinAndCos(phi);
        final double sp = sincosPhi[0];
        final double cp = sincosPhi[1];

        // values
        final double x = line.getPointX(t);
        final double y = line.getPointY(t);
        final double z = line.getPointZ(t);

        // Function values
        final double f1 = (this.a2n - this.b2n) * ct * st * cp - x * this.an * st + y * this.bn * ct;
        final double f2 =
            (this.a2n * ct * ct + this.b2n * st * st - this.c2n) * cp * sp - x * this.an * sp * ct - y * this.bn * sp
                    * st
                    + z * this.cn * cp;
        final double f3 =
            (x - this.an * ct * cp) * line.bx + (y - this.bn * st * cp) * line.by + (z - this.cn * sp) * line.bz;

        // Result in Matrix3D
        return new Array2DRowRealMatrix(new double[][] { { f1 }, { f2 }, { f3 } });

    }

    /**
     * Computes the jacobian matrix of the function {@code F(θ, φ, t)} where theta and phi represent the ellipsoidic
     * coordinates, t the location on the line and {@code F} the function defined by the equations expressing the
     * condition that the points {@code p, l} on the ellipsoid and the line achieving the minimal distance are such as
     * {@code (p-l)} is perpendicular to the ellipsoids surface. The Jacobian matrix is given by the partial
     * derivatives of {@code F} with respect to parameters {@code θ}, {@code φ} and {@code t} :<br>
     * 
     * <ul>
     * {@code J_F(θ, φ) =  ( dF_θ/dθ (θ, φ, t)    dF_θ/dφ (θ, φ, t)    dF_θ/dt (θ, φ, t) )}<br>
     * {@code              ( dF_φ/dθ (θ, φ, t)    dF_φ/dφ (θ, φ, t)    dF_φ/dt (θ, φ, t) )}<br>
     * {@code              ( dF_t/dθ (θ, φ, t)    dF_t/dφ (θ, φ, t)    dF_t/dt (θ, φ, t) )}
     * </ul>
     * 
     * @param theta
     *        planetographic longitude θ (in radians)
     * @param phi
     *        parametric latitude φ (in radians)
     * @param t
     *        location on the line
     * @param line
     *        user line
     * 
     * @return the jacobian matrix calculated with given data contained in a 2x2 matrix
     * 
     * @see Ellipsoid#getFLine(double, double, double, LineCoefficients)
     * @see Ellipsoid#runNewtonAlgorithmLine(LineCoefficients)
     * 
     */
    private Array2DRowRealMatrix getFpLine(final double theta, final double phi, final double t,
                                           final LineCoefficients line) {

        // Angles
        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];
        final double[] sincosPhi = MathLib.sinAndCos(phi);
        final double sp = sincosPhi[0];
        final double cp = sincosPhi[1];
        // cos(theta) * cos(theta)
        final double ct2 = ct * ct;
        // sin(theta) * sin(theta)
        final double st2 = st * st;
        // cos(phi) * cos(phi)
        final double cp2 = cp * cp;
        // sin(phi) * sin(phi)
        final double sp2 = sp * sp;

        // values
        final double x = line.getPointX(t);
        final double y = line.getPointY(t);
        final double z = line.getPointZ(t);

        // Derivatives
        final double fp11 = (this.a2n - this.b2n) * (ct2 - st2) * cp - x * this.an * ct - y * this.bn * st;
        final double fp12 = -(this.a2n - this.b2n) * ct * st * sp;
        final double fp13 = -line.bx * this.an * st + line.by * this.bn * ct;
        final double fp21 =
            -2 * (this.a2n - this.b2n) * ct * st * sp * cp + x * this.an * st * sp - y * this.bn * sp * ct;
        final double fp22 =
            (this.a2n * ct2 + this.b2n * st2 - this.c2n) * (cp2 - sp2) - x * this.an * cp * ct - y * this.bn * cp * st
                    - z * this.cn * sp;
        final double fp23 = -line.bx * this.an * sp * ct - line.by * this.bn * sp * st + line.bz * this.cn * cp;
        final double fp31 = this.an * st * cp * line.bx - this.bn * ct * cp * line.by;
        final double fp32 = this.an * ct * sp * line.bx + this.bn * st * sp * line.by - this.cn * cp * line.bz;
        final double fp33 = line.bx * line.bx + line.by * line.by + line.bz * line.bz;

        // Return Matrix3D
        return new Array2DRowRealMatrix(new double[][] { { fp11, fp12, fp13 }, { fp21, fp22, fp23 },
            { fp31, fp32, fp33 } });

    }

    /**
     * Computes the point, on the ellipsoid surface, that is the closest to a point of space.
     * 
     * @param point
     *        the point expressed in standard basis
     * @return the closest point to the user point on the ellipsoid surface
     */
    @Override
    public Vector3D closestPointTo(final Vector3D point) {
        /**
         * This method returns the ellipsoids closest point to the user specified point
         */

        // Point in ellipsoid local basis
        final Vector3D localPoint = this.getAffineLocalExpression(point);
        double x = localPoint.getX();
        double y = localPoint.getY();
        double z = localPoint.getZ();

        // Get the transformation to put the user point in the first octant
        final double[] t = this.getOctantTransformation(localPoint);
        final double xT = t[0];
        final double yT = t[1];
        final double zT = t[2];

        // If not, move it there. The ellipsoid is symmetric.
        x = xT * x;
        y = yT * y;
        z = zT * z;

        // Compute point position relative to ellipsoid
        final PointLocation result = this.getPointLocation(new Vector3D(x, y, z));

        // Initialise point coordinates
        double xL = 0;
        double yL = 0;
        double zL = 0;

        switch (result) {
            case OutsideX:
                // If the point is on X-axis, the closest point on the ellipsoid
                // is located on the poles and is either north or south, depending on the sign
                // of the local z-axis coordinate of the given point
                xL = this.a;
                yL = 0;
                zL = 0;
                break;
            case OutsideY:

                // If the point is on the axis of revolution, the closest point on the ellipsoid
                // is located on the poles and is either north or south, depending on the sign
                // of the local z-axis coordinate of the given point
                xL = 0;
                yL = this.b;
                zL = 0;
                break;
            case OutsideZ:

                // If the point is on the axis of revolution, the closest point on the ellipsoid
                // is located on the poles and is either north or south, depending on the sign
                // of the local z-axis coordinate of the given point
                xL = 0;
                yL = 0;
                zL = this.c;
                break;
            case AtCenter:
                switch (this.smallest) {
                    case A:
                        xL = this.a;
                        yL = 0;
                        zL = 0;
                        break;
                    case B:
                        xL = 0;
                        yL = this.b;
                        zL = 0;
                        break;
                    case C:
                        xL = 0;
                        yL = 0;
                        zL = this.c;
                        break;
                    default:
                        // this should never happen
                        throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
                }
                break;
            case OnEllipsoid:
                // Point is on ellipsoid
                xL = x;
                yL = y;
                zL = z;
                break;
            default:
                /*
                 * Point is either inside or outside ellipsoid and is on no particular axis and isnt at center
                 */
                final double[] myResult = this.runNewtonAlgorithm(x, y, z);
                xL = myResult[0];
                yL = myResult[1];
                zL = myResult[2];
                break;
        }

        // Move the computed point to the correct octant
        xL = xT * xL;
        yL = yT * yL;
        zL = zT * zL;

        // return result
        return this.getAffineStandardExpression(new Vector3D(xL, yL, zL));
    }

    /**
     * Computes the distance to the closest point on the ellipsoid. If the point is inside the ellipsoid, the returned
     * distance is negative. This method calls the method {@link Ellipsoid#closestPointTo(Vector3D)} that computes the
     * coordinates of the closest point on the ellipsoid and returns the distance between the user point and the
     * returned point.
     * 
     * @param point
     *        point coordinates in standard basis
     * @return distance to the closest point on the ellipsoid
     */
    public double distanceTo(final Vector3D point) {

        // Calculate distance between the closest point and the user specified point
        double distance = this.closestPointTo(point).subtract(point).getNorm();

        // Check if the point is inside
        if (this.isInside(point)) {
            distance *= -1;
        }

        return distance;
    }

    /**
     * This method computes the point on the line that is the closest to the ellipsoid.
     * 
     * @param line
     *        line for the shortest distance computation
     * 
     * @return Array of length 2 containing the point of the ellipsoid (slot [0]) and the point of the line (slot [1])
     *         expressed as {@link Vector3D}
     */
    @Override
    public Vector3D[] closestPointTo(final Line line) {

        final Vector3D[] points = new Vector3D[2];

        // intersections
        final Vector3D[] iscs = this.getIntersectionPoints(line);

        if (iscs.length == 0) {

            // line does not intersect
            final LineCoefficients userLine = new LineCoefficients(line);
            final double[] res = this.runNewtonAlgorithmLine(userLine);

            points[0] = this.getAffineStandardExpression(new Vector3D(res[0], res[1], res[2]));
            points[1] = this.getAffineStandardExpression(userLine.getPoint(res[3]));

        } else {

            // if line intersects, return first intersection
            points[0] = iscs[0];
            points[1] = iscs[0];
        }

        return points;
    }

    /**
     * Get the smallest distance from the line to the ellipsoid
     * 
     * @param line
     *        the line
     * @return the computed distance
     */
    @Override
    public double distanceTo(final Line line) {

        // Get point on line and point on ellipsoid
        final Vector3D[] myPoints = this.closestPointTo(line);

        return myPoints[0].subtract(myPoints[1]).getNorm();

    }

    /**
     * Setter for Newton algorithm threshold used to compute closest point and distance to the ellipsoid.
     * Default value for this threshold is 1E-11.
     * 
     * @param newThreshold
     *        new threshold to set
     */
    public void setNewtonThreshold(final double newThreshold) {
        this.epsNewton = newThreshold;
    }

    /**
     * Get a representation for this ellipsoid. The given parameters are in the same order as in the constructor.
     * 
     * @return a representation for this ellipsoid
     */
    @Override
    public String toString() {
        // create a string builder
        final StringBuilder res = new StringBuilder();
        // class name
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        // append class name
        res.append(shortClassName).append(open);
        res.append("Center");
        // append position
        res.append(this.position.toString());
        res.append(comma);
        // append axis
        res.append("Revolution axis");
        res.append(this.ellipsoidZ.toString());
        res.append(comma);
        // append ellipsoid axes
        res.append("Axis a");
        res.append(this.ellipsoidX.toString());
        res.append(comma);
        // append ellipsoid axes
        res.append("Semi axis a").append(open);
        res.append(this.a).append(close);
        res.append(comma);
        // append ellipsoid axes
        res.append("Semi axis b").append(open);
        res.append(this.b).append(close);
        res.append(comma);
        // append ellipsoid axes
        res.append("Semi axis c").append(open);
        res.append(this.c).append(close);
        res.append(close);

        // return result
        return res.toString();
    }

    /**
     * Representation of a line. A {@link Vector3D point} on the line has its three coordinates expressed as :
     * 
     * <ul>
     * {@code P =  ( X )=  ( ax + bx * t )}<br>
     * {@code      ( Y )=  ( ay + by * t )}<br>
     * {@code      ( Z )=  ( az + bz * t )}
     * </ul>
     * 
     * @concurrency immutable
     * 
     * @see Line
     * 
     */
    private final class LineCoefficients {

        /**
         * Constant component of x coordinate
         */
        private final double ax;
        /**
         * Linear component of x coordinate
         */
        private final double bx;
        /**
         * Constant component of y coordinate
         */
        private final double ay;
        /**
         * Linear component of y coordinate
         */
        private final double by;
        /**
         * Constant component of z coordinate
         */
        private final double az;
        /**
         * Linear component of z coordinate
         */
        private final double bz;

        /**
         * Use this constructor to express a user {@link Line line} in the local ellipsoid frame
         * 
         * @param line
         *        user line in standard frame
         * 
         * @see Ellipsoid#closestPointTo(Line)
         * @see Ellipsoid#runNewtonAlgorithmLine(LineCoefficients)
         * 
         */
        private LineCoefficients(final Line line) {

            Vector3D newOrg = Ellipsoid.this.getAffineLocalExpression(line.getOrigin());
            final Vector3D newDir = Ellipsoid.this.getVectorialLocalExpression(line.getDirection());

            final Line localLine = new Line(newOrg, newOrg.add(newDir));

            newOrg = localLine.getOrigin();

            this.ax = newOrg.getX();
            this.ay = newOrg.getY();
            this.az = newOrg.getZ();

            this.bx = newDir.getX();
            this.by = newDir.getY();
            this.bz = newDir.getZ();

        }

        /**
         * Constructor.
         * 
         * @param origin
         *        origin
         * @param direction
         *        direction
         */
        private LineCoefficients(final Vector3D origin, final Vector3D direction) {
            this.ax = origin.getX();
            this.ay = origin.getY();
            this.az = origin.getZ();

            this.bx = direction.getX();
            this.by = direction.getY();
            this.bz = direction.getZ();
        }

        /**
         * Get line origin.
         * 
         * @return line origin
         */
        private Vector3D getLineOrigin() {
            return new Vector3D(this.ax, this.ay, this.az);
        }

        /**
         * Get line direction.
         * 
         * @return line direction
         */
        private Vector3D getLineDirection() {
            return new Vector3D(this.bx, this.by, this.bz);
        }

        /**
         * Get line point at location {@code t}
         * 
         * @param t
         *        location on line
         * @return point as a {@link Vector3D}
         */
        private Vector3D getPoint(final double t) {
            return new Vector3D(this.ax + t * this.bx, this.ay + t * this.by, this.az + t * this.bz);
        }

        /**
         * Get line point X coordinate at location {@code t}
         * 
         * @param t
         *        location on line
         * @return X coordinate in ellipsoid frame
         */
        private double getPointX(final double t) {
            return this.ax + t * this.bx;
        }

        /**
         * Get line point Y coordinate at location {@code t}
         * 
         * @param t
         *        location on line
         * @return Y coordinate in ellipsoid frame
         */
        private double getPointY(final double t) {
            return this.ay + t * this.by;
        }

        /**
         * Get line point Z coordinate at location {@code t}
         * 
         * @param t
         *        location on line
         * @return Z coordinate in ellipsoid frame
         */
        private double getPointZ(final double t) {
            return this.az + t * this.bz;
        }

    }
}
