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
 * @history Created on 12/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class is the Infinite Oblique Circular Cone class.
 * </p>
 * <p>
 * It represents the mathematical object by the same name.
 * </p>
 * 
 * @useSample
 *            With the following parameters : <br>
 *            {@code   Vector3D origin = new Vector3D(1,2,3);}<br>
 *            {@code   Vector3D axis = new Vector3D(0,0,1);}<br>
 *            {@code   Vector3D axisU = new Vector3D(1,0,0);}<br>
 *            {@code   double alpha = FastMath.PI / 4;}<br>
 *            {@code   double beta = FastMath.PI / 5;}<br>
 *            The user may instanciate an infinite oblique circular cone :<br>
 *            {@code   InfiniteObliqueCircularCone myCone =
 *            new InfiniteObliqueCircularCone(origin, axis, axisU, alpha, beta);}
 * 
 * @concurrency immutable
 * 
 * @see InfiniteCone
 * @see Shape
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: InfiniteEllipticCone.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class InfiniteEllipticCone implements InfiniteCone, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -7542518748306042613L;

    /** Vector norm comparison (constructor) */
    private static final double CSTEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Double comparison */
    private static final double EPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Newton loop limit */
    private static final int NEWTONLIMIT = 100;

    /** Incrementation rate limitation is pi/rate for θ */
    private static final double TRATE = 25;

    /** Incrementation rate limitation is 1/HRATE for h */
    private static final double HRATE = 2;

    /** Newton step limit */
    private static final int STEPLIMIT = 1;

    /** Newton Z starting value */
    private static final double ZSTART = .5;

    /** Position of origin */
    private final Vector3D origin;

    /** local X-axis */
    private final Vector3D localX;

    /** local Z-axis */
    private final Vector3D localZ;

    /** angle along X axis */
    private final double alpha;

    /** angle along Y axis */
    private final double beta;

    /** tangeant of alpha */
    private final double ta;

    /** tangeant of beta */
    private final double tb;

    /** Transformation Matrix to standard basis */
    private final Matrix3D standardBasisTransform;

    /** Transformation Matrix to local cone basis */
    private final Matrix3D localBasisTransform;

    /**
     * This is the constructor for the class InfiniteObliqueCircularCone. It allow the
     * user to instantiate an infinite oblique circular cone. An infinite oblique circular
     * cone is a cone with the particularity that its axis is not perpendicular to the
     * center of the base. As such, this cone does not have the rotational symmetry property
     * about its axis.
     * 
     * @param aorigin
     *        Apex, or vertex, or summit, of the cone
     * @param aDirection
     *        Direction of the axis of the cone
     * @param aaxisU
     *        Axis of the plane perpendicular to the cones' axis : will be recomputed to be orthogonal and
     *        and normalised. Along this direction
     *        (in the {@code (origin, aaxisU, aaxis)} plane), the cone has an angle of {@code alpha}. Along the
     *        cross product
     *        of {@code axis} and the recomputed {@code axisU}, the cone has an angle of {@code beta}
     * @param aalpha
     *        Angle along {@code axisU} (in radians)
     * @param bbeta
     *        Angle along {@code axis.crossProduct(axisU)} (in radians)
     * @exception IllegalArgumentException
     *            if semi-axis or norm of revolution axis is null
     */
    public InfiniteEllipticCone(final Vector3D aorigin, final Vector3D aDirection, final Vector3D aaxisU,
        final double aalpha, final double bbeta) {

        /**
         * Input arguments check
         */
        // The axis' norm must be positive
        // The e-10 EPSilon is chosen to be coherent with the Plane class
        String message = PatriusMessages.NUMBER_TOO_SMALL.getLocalizedString(Locale.getDefault());

        if (aDirection.getNorm() < CSTEPS) {
            throw new IllegalArgumentException(message);
        }
        if (aaxisU.getNorm() < CSTEPS) {
            throw new IllegalArgumentException(message);
        }
        if (aDirection.crossProduct(aaxisU).getNorm() < CSTEPS) {
            throw new IllegalArgumentException(message);
        }
        // The input angles must be between 0.0 and PI/2
        message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());
        if (aalpha <= 0.0 || aalpha >= FastMath.PI / 2) {
            throw new IllegalArgumentException(message);
        }
        if (bbeta <= 0.0 || bbeta >= FastMath.PI / 2) {
            throw new IllegalArgumentException(message);
        }

        /**
         * Input tests passed. Create Object
         */
        // Assign origin
        this.origin = aorigin;

        // Assign angle
        this.alpha = aalpha;
        this.ta = MathLib.tan(this.alpha);
        this.beta = bbeta;
        this.tb = MathLib.tan(this.beta);

        // Assign axes
        this.localZ = aDirection.normalize();
        this.localX = aaxisU.subtract(this.localZ.scalarMultiply(aaxisU.dotProduct(this.localZ))).normalize();
        final Vector3D localY = this.localZ.crossProduct(this.localX);

        // Assign Matrices
        final double[][] matrixData = {
            { this.localX.getX(), localY.getX(), this.localZ.getX() },
            { this.localX.getY(), localY.getY(), this.localZ.getY() },
            { this.localX.getZ(), localY.getZ(), this.localZ.getZ() } };
        this.standardBasisTransform = new Matrix3D(matrixData);
        this.localBasisTransform = this.standardBasisTransform.transpose();

    }

    /**
     * This method returns the origin of the cone
     * 
     * @return A vector containing the coordinates of the cones origin
     */
    @Override
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * This method returns the angle of the cone along X axis
     * 
     * @return A double representing the angle along X axis
     */
    public double getAngleX() {
        return this.alpha;
    }

    /**
     * This method returns the aperture of the cone along X axis
     * 
     * @return A double representing the aperture along X axis
     */
    public double getApertureX() {
        return 2 * this.alpha;
    }

    /**
     * This method returns the semi axis of the cone along X axis
     * 
     * @return A double representing the semi axis along X axis (at height {@code h=1})
     */
    public double getSemiAxisX() {
        return this.ta;
    }

    /**
     * This method returns the angle of the cone along Y axis
     * 
     * @return A double representing the angle along Y axis
     */
    public double getAngleY() {
        return this.beta;
    }

    /**
     * This method returns the aperture of the cone along Y axis
     * 
     * @return A double representing the aperture along Y axis
     */
    public double getApertureY() {
        return 2 * this.beta;
    }

    /**
     * This method returns the semi axis of the cone along Y axis
     * 
     * @return A double representing the semi axis along Y axis (at height {@code h=1})
     */
    public double getSemiAxisY() {
        return this.tb;
    }

    /**
     * This method returns the matrix of the transformation to the local basis
     * 
     * @return the matrix
     */
    public Matrix3D getLocalBasisTransform() {
        return this.localBasisTransform;
    }

    /**
     * Express a Vector3D in spheroid local frame. Warning : Affine transformation
     * 
     * @param myVector
     *        Vector expressed in standard basis
     * @return vectorRef Same vector expressed in spheroid local frame
     */
    public Vector3D getAffineLocalExpression(final Vector3D myVector) {
        // Transformation is V_local = M^(-1) * (V_global - Origin)
        return this.localBasisTransform.multiply(myVector.subtract(this.origin));
    }

    /**
     * Express a Vector3D in spheroid local frame. Warning : Vectorial transformation
     * 
     * @param myVector
     *        Vector expressed in standard basis
     * @return vectorRef Same vector expressed in spheroid local frame
     */
    public Vector3D getVectorialLocalExpression(final Vector3D myVector) {
        // Transformation is V_local = M^(-1) * V_global
        return this.localBasisTransform.multiply(myVector);
    }

    /**
     * Express a Vector3D in standard basis. Warning : Vectorial transformation
     * 
     * @param vector
     *        Vector expressed in spheroid local frame
     * @return vectorRef Same vector expressed in standard basis
     */
    public Vector3D getVectorialStandardExpression(final Vector3D vector) {
        // Transformation is V_global = M * V_local
        return this.standardBasisTransform.multiply(vector);
    }

    /**
     * Express a Vector3D in standard basis. Warning : Affine transformation
     * 
     * @param vector
     *        Vector expressed in spheroid local frame
     * @return vectorRef Same vector expressed in standard basis
     */
    public Vector3D getAffineStandardExpression(final Vector3D vector) {
        // Transformation is V_global = M * V_local + Origin
        return this.standardBasisTransform.multiply(vector).add(this.origin);
    }

    /**
     * This method returns the matrix of the transformation to the standard basis
     * 
     * @return A vector containing the coordinates of the cones origin
     */
    public Matrix3D getStandardBasisTransform() {
        return this.standardBasisTransform;
    }

    /**
     * Convert from Cartesian to Conic coordinates
     * 
     * @param x
     *        coordinate
     * @param y
     *        coordinate
     * @param z
     *        coordinate
     * @return Conic coordinates (theta, h)
     */
    private double[] getConicCoordinates(final double x, final double y, final double z) {

        // Container
        final double[] coords = new double[2];

        // Transformation
        coords[1] = z;
        coords[0] = MathLib.atan2(MathLib.divide(y, this.tb), MathLib.divide(x, this.ta));

        return coords;
    }

    /**
     * Get polynomial data
     * 
     * @param line
     *        User specified line
     * @return {delta, a<sub>2</sub>, a<sub>1</sub>, a<sub>0</sub>}
     */
    private double[] getDeterminantAndCoeffs(final Line line) {

        // Get line equations in spheroid frame
        final Vector3D lineOrg = this.getAffineLocalExpression(line.getOrigin());
        final Vector3D lineDir = this.getVectorialLocalExpression(line.getDirection());

        /**
         * Compute intersection with a line
         * 
         * The line is expressed in the following parametric equation system
         * (considered in the cone local basis) :
         * 
         * For each coordinate q_i,
         * q_i = alpha_i * t + beta_i
         * with alpha_i and beta_i constant and t in R
         * 
         * Thus, the line intersects the basis if some value of t is found to
         * satisfy the cones equation. This equation, in the cone local
         * basis, is given by :
         * 
         * x²/ta² + y²/tb² - z² = 0
         * 
         * By reinjecting the triplet (x,y,z) corresponding to a point the line,
         * we obtain a second degree polynomial that is given by :
         * 
         * t2coeff x t² + t1coeff * t + t0coeff = 0
         * 
         * Solutions of this polynomial indicated the point corresponding to the
         * t_sol value satisfy the cones equation, hereby belonging to the intersection
         * of the line and the cone (that extends in R³). Further filtering is required
         * to provide solutions of the intersection of the same line with the cone that
         * extends in R²xR+.
         */
        final double t2coeff = lineDir.getX() * lineDir.getX() / (this.ta * this.ta)
            + lineDir.getY() * lineDir.getY() / (this.tb * this.tb) - lineDir.getZ() * lineDir.getZ();
        final double t1coeff = 2 * (lineDir.getX() * lineOrg.getX()) / (this.ta * this.ta)
            + 2 * (lineDir.getY() * lineOrg.getY()) / (this.tb * this.tb) - 2 * lineDir.getZ() * lineOrg.getZ();
        final double t0coeff = lineOrg.getX() * lineOrg.getX() / (this.ta * this.ta)
            + lineOrg.getY() * lineOrg.getY() / (this.tb * this.tb) - lineOrg.getZ() * lineOrg.getZ();

        final double delta = t1coeff * t1coeff - 4 * t2coeff * t0coeff;

        return new double[] { delta, t2coeff, t1coeff, t0coeff };
    }

    /**
     * This method returns true if the user specified line intersects the cone.
     * 
     * @param line
     *        User specified line
     * @return a boolean set to true if the line intersects, false otherwise
     */
    @Override
    public boolean intersects(final Line line) {
        // Get determinant
        final Vector3D[] data = this.getIntersectionPoints(line);
        return (data.length > 0);
    }

    /**
     * This methods computes and returns the intersection points between a line and the cone.
     * 
     * @param line
     *        The line with which the intersections are to be computed
     * @return A Vector3D array containing the intersections coordinates.
     *         If no intersections have been found, the array is empty.
     */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // Initialise intersections container
        Vector3D[] result = null;

        // Get line equations in spheroid frame
        final Vector3D lineOrg = this.getAffineLocalExpression(line.getOrigin());
        final Vector3D lineDir = this.getVectorialLocalExpression(line.getDirection());

        // Coefficients for intersection points computation
        final double[] equationData = this.getDeterminantAndCoeffs(line);
        final double delta = equationData[0];
        final double t2coeff = equationData[1];
        final double t1coeff = equationData[2];

        if (delta > 0) {

            // Compute the corresponding t parameter
            final double t1 = (-t1coeff + MathLib.sqrt(delta)) / (2 * t2coeff);
            final double t2 = (-t1coeff - MathLib.sqrt(delta)) / (2 * t2coeff);

            // Compute the Positions in spheroid frame
            final Vector3D p1 = new Vector3D(lineDir.getX() * t1 + lineOrg.getX(),
                lineDir.getY() * t1 + lineOrg.getY(),
                lineDir.getZ() * t1 + lineOrg.getZ());
            final Vector3D p2 = new Vector3D(lineDir.getX() * t2 + lineOrg.getX(),
                lineDir.getY() * t2 + lineOrg.getY(),
                lineDir.getZ() * t2 + lineOrg.getZ());

            // Check if the points are valid. The cone is indeed limited by
            // the origin and extends in R²xR+. Valid solutions have z > 0
            final int[] k = { 0, 0 };
            if (p1.getZ() >= 0) {
                k[0] = 1;
            }
            if (p2.getZ() >= 0) {
                k[1] = 1;
            }

            // Initialise result container
            result = new Vector3D[k[0] + k[1]];

            // Return points expressed in standard frame.
            // This is an affine transformation.
            int kk = 0;
            if (k[0] == 1) {
                result[0] = this.getAffineStandardExpression(p1);
                kk++;
            }
            if (k[1] == 1) {
                result[kk] = this.getAffineStandardExpression(p2);
            }

        } else if (Precision.equals(delta, 0)) {

            // Compute the corresponding t parameter
            final double t1 = (-t1coeff) / (2 * t2coeff);

            // Compute the Positions in spheroid frame
            Vector3D p1 = new Vector3D(lineDir.getX() * t1 + lineOrg.getX(),
                lineDir.getY() * t1 + lineOrg.getY(),
                lineDir.getZ() * t1 + lineOrg.getZ());

            // Check if in the correct domain of space
            result = new Vector3D[0];

            if (p1.getZ() >= 0) {

                result = new Vector3D[1];
                // Transform
                p1 = this.getAffineStandardExpression(p1);
                // Return result
                result[0] = p1;

            }
        } else {
            // No intersections
            result = new Vector3D[0];
        }

        return result;
    }

    /**
     * Convert from Cartesian to Conic coordinates
     * 
     * @param theta
     *        angle
     * @param h
     *        height
     * @return Cartesian coordinates (x, y, z)
     */
    private double[] getCartesianCoordinates(final double theta, final double h) {

        // Container
        final double[] coords = new double[3];

        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double sin = sincosTheta[0];
        final double cos = sincosTheta[1];

        // Transformation
        coords[0] = h * this.ta * cos;
        coords[1] = h * this.tb * sin;
        coords[2] = h;

        return coords;
    }

    /**
     * Return true if point is inside cone
     * 
     * @param point
     *        in standard basis
     * @return boolean if is inside
     * 
     * @since 1.0
     */
    public boolean isInside(final Vector3D point) {

        // local transform
        final Vector3D localPoint = this.getAffineLocalExpression(point);

        // cone equ
        final double x = localPoint.getX();
        final double y = localPoint.getY();
        final double zCone = MathLib.sqrt(x * x / (this.ta * this.ta) + y * y / (this.tb * this.tb));

        // return
        return localPoint.getZ() >= zCone;

    }

    /**
     * Return true if point is inside cone
     * 
     * @param point
     *        in standard basis
     * @return boolean if is inside
     * 
     * @since 1.0
     */
    public boolean isStrictlyInside(final Vector3D point) {

        // local transform
        final Vector3D localPoint = this.getAffineLocalExpression(point);

        // cone equ
        final double x = localPoint.getX();
        final double y = localPoint.getY();
        final double zCone = MathLib.sqrt(x * x / (this.ta * this.ta) + y * y / (this.tb * this.tb));

        // return
        return localPoint.getZ() > zCone;

    }

    /**
     * Computes the transformation from any point in R³ its equivalent in the first octant.
     * This function returns a 1x3 array containing +1 if the coordinate is in the first octant,
     * -1 otherwise.
     * 
     * @param point
     *        user point expressed in spheroid local frame
     * @return transformation a triplet of ±1
     */
    private double[] getOctantTransformation(final Vector3D point) {

        // Get user point coordinates in cone basis
        final double x = point.getX();
        final double y = point.getY();

        // Get transformation data
        final double xT = x < 0 ? -1 : 1;
        final double yT = y < 0 ? -1 : 1;
        final double zT = 1;

        return new double[] { xT, yT, zT };
    }

    /**
     * Return angular coordinates of closest intersection point of line (center - myPoint) and spheroid.
     * These values will serve as a starting point for the Newton algorithm. Performance increase is about
     * 20 to 30% (in iterations) in case it is a spheroid. In case the instantiated object is a sphere,
     * the angular values are the exact solution of the closest point problem.
     * 
     * @param x
     *        coordinate
     * @param y
     *        coordinate
     * @param z
     *        coordinate
     * @return conic coordinates of the intersection point (center - point) / (spheroid)
     * 
     * @since 1.0
     */
    private double[] getOptimizedStartingLocation(final double x, final double y, final double z) {

        double[] coords = new double[2];

        if (Precision.equals(x, 0, EPS) && Precision.equals(x, y, EPS)) {

            // is on cone axis

            if (z <= 0) {

                // on negative part thus closest point is origin
                coords[0] = 0;
                coords[1] = 0;

            } else {

                // on positive part, thus closest to whichever side is smaller
                if (this.ta > this.tb) {

                    final double[] pointOnCone = this.getCartesianCoordinates(FastMath.PI / 2, z);

                    // tranverse is closest thus theta is at quadrant bound pi/2
                    coords[0] = FastMath.PI / 2;
                    coords[1] = z - pointOnCone[1] * MathLib.cos(this.beta) * MathLib.sin(this.beta);

                } else {

                    final double[] pointOnCone = this.getCartesianCoordinates(0, z);

                    // tranverse is closest thus theta is at quadrant bound 0
                    coords[0] = 0;
                    coords[1] = z - pointOnCone[0] * MathLib.cos(this.alpha) * MathLib.sin(this.alpha);

                }
            }
        } else {

            final double isOnCone = x * x / (this.ta * this.ta) + y * y / (this.tb * this.tb) - z * z;

            if (z > 0 && Precision.equals(isOnCone, 0, CSTEPS)) {

                // Is on cone
                coords = this.getConicCoordinates(x, y, z);

            } else {

                // get local theta
                final double localTheta = MathLib.atan2(MathLib.divide(y, this.tb), MathLib.divide(x, this.ta));
                coords[0] = localTheta;
                coords[1] = MathLib.max(ZSTART, z);

            }

        }

        return coords;
    }

    /**
     * Computes the function value {@code F(θ, h) = 0} where θ and h represent the
     * conic coordinates and {@code F} the function defined by the equations expressing the
     * condition that the point {@code x} on the cone achieving the minimal distance to the user point {@code p} is such
     * as {@code (p-x)} is perpendicular to the cone surface :<br>
     * <ul>
     *     {@code (1)     F_θ(θ, h) = (p - x)•dx/dθ}<br>
     *     {@code (2)     F_h(θ, h) = (p - x)•dx/dh}
     * </ul>
     * {@code p} being the user specified point and<br>
     * {@code x} being the point of the cone<br>
     * <br>
     * 
     * @param theta
     *        θ (in radians)
     * @param h
     *        height
     * @param x
     *        coordinate of point
     * @param y
     *        coordinate of point
     * @param z
     *        coordinate of point
     * 
     * @return the function calculated with given data in the form of a 2x1 matrix
     * 
     * @see InfiniteEllipticCone#getFp(double, double, double, double)
     * @see InfiniteEllipticCone#runNewtonAlgorithm(double, double, double)
     * 
     */
    private Array2DRowRealMatrix getF(final double theta, final double h,
                                      final double x, final double y, final double z) {

        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];

        // Angles
        final double ct2 = ct * ct;
        final double st2 = st * st;
        final double h2 = h * h;
        final double ta2 = this.ta * this.ta;
        final double tb2 = this.tb * this.tb;

        // Function values
        final double a1 = -x * h * this.ta * st + h2 * ta2 * ct * st + y * h * this.tb * ct - h2 * tb2 * st * ct;
        final double a2 = x * this.ta * ct - h * ta2 * ct2 + y * this.tb * st - h * tb2 * st2 + z - h;

        // Result in Matrix3D
        return new Array2DRowRealMatrix(new double[][] { { a1 }, { a2 } });
    }

    /**
     * Computes the jacobian matrix of the function {@code F(θ, φ)} where θ and h represent the
     * conic coordinates and {@code F} the function defined by the equations expressing the
     * condition that the point {@code x} on the cone achieving the minimal distance to the user point {@code p} is such
     * as {@code (p-x)} is perpendicular to the cones surface (detailed above). The Jacobian matrix is
     * given by the partial derivatives of {@code F} with respect to parameters {@code θ} and {@code h} :<br>
     * 
     * <ul>
     * {@code J_F(θ, h) =   ( dF_θ/dθ (θ, h)    dF_θ/dh (θ, h) )}<br>
     * {@code               ( dF_h/dθ (θ, h)    dF_h/dh (θ, h) )}
     * </ul>
     * 
     * @param theta
     *        θ (in radians)
     * @param h
     *        height
     * @param x
     *        coordinate of point
     * @param y
     *        coordinate of point
     * 
     * @return the jacobian matrix calculated with given data contained in a 2x2 matrix
     * 
     * @see InfiniteEllipticCone#getF(double, double, double, double, double)
     * @see InfiniteEllipticCone#runNewtonAlgorithm(double, double, double)
     * 
     */
    private Array2DRowRealMatrix getFp(final double theta, final double h,
                                       final double x, final double y) {

        // Angles
        // Theta
        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double st = sincosTheta[0];
        final double ct = sincosTheta[1];
        final double ct2 = ct * ct;
        final double st2 = st * st;
        final double h2 = h * h;
        final double ta2 = this.ta * this.ta;
        final double tb2 = this.tb * this.tb;

        // Derivatives
        final double a11 = -h * (x * this.ta * ct + y * this.tb * st) + h2 * (ct2 - st2) * (ta2 - tb2);
        final double a12 = y * this.tb * ct - x * this.ta * st + 2 * h * ct * st * (ta2 - tb2);
        final double a21 = a12;
        final double a22 = -ta2 * ct2 - tb2 * st2 - 1;

        // Return Matrix3D
        return new Array2DRowRealMatrix(new double[][] { { a11, a12 }, { a21, a22 } });

    }

    /**
     * Newton Algorithm
     * 
     * @param x
     *        coordinate of target point
     * @param y
     *        coordinate of target point
     * @param z
     *        coordinate of target point
     * @return double[] with coordinates of point on cone
     */
    private double[] runNewtonAlgorithm(final double x, final double y, final double z) {

        // Incrementation thresholds
        final double thetaRate = FastMath.PI / TRATE;
        final double heighTRATE = 1 / HRATE;

        // Initialisation of computed point coordinates
        final double[] startingLocation = this.getOptimizedStartingLocation(x, y, z);
        double theta = startingLocation[0];
        double h = startingLocation[1];

        // Initialisation of containers
        double det;
        double[][] invData;
        Array2DRowRealMatrix state = new Array2DRowRealMatrix(new double[][] { { theta }, { h } });
        Array2DRowRealMatrix step;
        Array2DRowRealMatrix myF;
        Array2DRowRealMatrix myFp;
        Array2DRowRealMatrix myFpInv;
        double stepNorm = 1;

        int i = 0;

        while (i < NEWTONLIMIT && stepNorm > EPS) {

            // Get new step values
            myF = this.getF(theta, h, x, y, z);
            myFp = this.getFp(theta, h, x, y);

            det = myFp.getEntry(0, 0) * myFp.getEntry(1, 1) - myFp.getEntry(1, 0) * myFp.getEntry(0, 1);

            if (det == 0) {
                // reached origin
                stepNorm = 0;

            } else {
                // Invert Jacobian Matrix
                invData = new double[][] {
                    { MathLib.divide(myFp.getEntry(1, 1), det),
                        -MathLib.divide(myFp.getEntry(0, 1), det) },
                    { -MathLib.divide(myFp.getEntry(1, 0), det),
                        MathLib.divide(myFp.getEntry(0, 0), det) } };
                myFpInv = new Array2DRowRealMatrix(invData);

                /*
                 * Calculate step
                 * This is done according to the Newton method directives :
                 * x(n+1) = x(n) - J(n)^-1 * F(n)
                 */
                step = myFpInv.multiply(myF);

                // Saturate incrementations in order to prevent divergence of algorithm
                // theta saturation
                final double sign00 = step.getEntry(0, 0) >= 0 ? 1 : -1;
                step.setEntry(0, 0, sign00 * MathLib.min(MathLib.abs(step.getEntry(0, 0)), thetaRate));
                final double sign10 = step.getEntry(1, 0) >= 0 ? 1 : -1;
                step.setEntry(1, 0, sign10 * MathLib.min(MathLib.abs(step.getEntry(1, 0)), heighTRATE));

                // new Values
                stepNorm = step.getNorm();
                state = state.subtract(step);

                // store theta and phi values
                theta = state.getEntry(0, 0);
                h = state.getEntry(1, 0);

                // h = 0 is a loop breaker
                if (Precision.equals(h, 0, EPS)) {
                    // reached origin
                    stepNorm = 0;
                }

                // keep track of stEPS
                i++;
            }
        }
        if (i >= NEWTONLIMIT) {
            throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, NEWTONLIMIT);
        }

        return this.getCartesianCoordinates(theta, h);
    }

    /**
     * Computes the closest point on the cone to a user specified point
     * 
     * @param point
     *        specified by user in standard basis
     * @return A vector3D representing the coordinates of the closest point
     */
    public Vector3D closestPointTo(final Vector3D point) {

        // Point in spheroid local basis
        final Vector3D localPoint = this.getAffineLocalExpression(point);
        double x = localPoint.getX();
        double y = localPoint.getY();
        double z = localPoint.getZ();

        // Get the transformation to put the user point in the first octant
        final double[] t = this.getOctantTransformation(localPoint);
        final double xT = t[0];
        final double yT = t[1];
        final double zT = t[2];

        // If not, move it there. The cone is symmetric (Oxz and Oyz). zT = 1 always.
        x = xT * x;
        y = yT * y;
        z = zT * z;

        // Initialise point coordinates
        final double xL;
        final double yL;
        final double zL;

        // Call newton method
        final double[] result = this.runNewtonAlgorithm(x, y, z);

        // Move the computed point to the correct octant
        xL = xT * result[0];
        yL = yT * result[1];
        zL = zT * result[2];

        // return result
        return this.getAffineStandardExpression(new Vector3D(xL, yL, zL));
    }

    /**
     * @param point
     *        specified by user in standard basis
     * @return distance to point. Negative if point is inside cone
     */
    public double distanceTo(final Vector3D point) {

        // Get closest point on spheroid
        final Vector3D closestPoint = this.closestPointTo(point);

        // Calculate distance between the closest point and the user specified point
        double distanceToPoint = closestPoint.subtract(point).getNorm();

        // Check if the point is inside
        if (this.isInside(point)) {
            distanceToPoint *= -1;
        }

        return distanceToPoint;
    }

    /**
     * Calculate the closest point to a line
     * 
     * @param line
     *        specified by user
     * @return closest point as a Vector3D to line
     */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        /* This method returns the shortest distance from the user specified line to the spheroids surface */

        final Vector3D[] points = new Vector3D[2];

        // intersections
        final Vector3D[] iscs = this.getIntersectionPoints(line);

        if (iscs.length == 0) {

            // A point p on the line is expressed as p = lineDir * t + lineOrg in standard basis
            final Vector3D lineOrg = line.getOrigin();
            final Vector3D lineDir = line.getDirection().normalize();

            // Containers
            double fc;
            double fc1;
            double fc2;
            double fp;
            double fpp;
            Vector3D pc;
            Vector3D pc1;
            Vector3D pc2;
            Vector3D currentPoint;
            Vector3D currentPoint1;
            Vector3D currentPoint2;

            // Newton loop init
            double sign;
            final double dt = 1;
            int i = 0;
            double t = 0;
            double step = 1;
            currentPoint = lineOrg.add(lineDir.scalarMultiply(t));
            currentPoint1 = lineOrg.add(lineDir.scalarMultiply(t + dt));
            currentPoint2 = lineOrg.add(lineDir.scalarMultiply(t - dt));

            pc = this.closestPointTo(currentPoint);
            fc = currentPoint.subtract(pc).getNorm();
            pc1 = this.closestPointTo(currentPoint1);
            fc1 = currentPoint1.subtract(pc1).getNorm();
            pc2 = this.closestPointTo(currentPoint2);
            fc2 = currentPoint2.subtract(pc2).getNorm();

            while (i < NEWTONLIMIT && MathLib.abs(step) > EPS) {
                // distance derivatives
                fp = (fc1 - fc2) / (2 * dt);
                fpp = (fc1 - 2 * fc + fc2) / (dt * dt);

                // step and incrementation
                step = -MathLib.divide(fp, fpp);
                sign = step >= 0 ? 1 : -1;
                t = t + sign * MathLib.min(MathLib.abs(step), STEPLIMIT);

                // computation of new points
                currentPoint1 = lineOrg.add(lineDir.scalarMultiply(t + dt));
                currentPoint2 = lineOrg.add(lineDir.scalarMultiply(t - dt));
                currentPoint = lineOrg.add(lineDir.scalarMultiply(t));

                // distances
                pc1 = this.closestPointTo(currentPoint1);
                fc1 = currentPoint1.subtract(pc1).getNorm();
                pc2 = this.closestPointTo(currentPoint2);
                fc2 = currentPoint2.subtract(pc2).getNorm();
                pc = this.closestPointTo(currentPoint);
                fc = currentPoint.subtract(pc).getNorm();

                // keep track of steps
                i++;

                if (Precision.equals(pc1.getX(), this.origin.getX(), EPS) &&
                    Precision.equals(pc1.getY(), this.origin.getY(), EPS) &&
                    Precision.equals(pc1.getZ(), this.origin.getZ(), EPS)) {
                    /*
                     * If current point is equal to cones' origin, stop algorithm
                     * and return origin
                     */
                    step = 0;
                }

            }

            if (i >= NEWTONLIMIT) {
                throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, NEWTONLIMIT);
            }

            // return result
            points[0] = currentPoint;
            points[1] = pc;

        } else {

            // if line intersects, return first intersection
            points[0] = iscs[0];
            points[1] = points[0];
        }

        return points;
    }

    /**
     * Get the smallest distance from the line to the cone
     * 
     * @param line
     *        the line
     * @return the computed distance
     */
    @Override
    public double distanceTo(final Line line) {

        // Get point on line and point on spheroid
        final Vector3D[] myPts = this.closestPointTo(line);

        return myPts[0].subtract(myPts[1]).getNorm();
    }

    /**
     * Get a representation for this infinite oblique circular cone.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this infinite oblique circular cone
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
        res.append(this.localZ.toString());
        res.append(comma);
        // "U vector":
        res.append("U vector");
        res.append(this.localX.toString());
        res.append(comma);
        // "Angle on U":
        res.append("Angle on U").append(open);
        res.append(this.alpha).append(close);
        res.append(comma);
        // "Angle on V":
        res.append("Angle on V").append(open);
        res.append(this.beta).append(close);
        res.append(close);

        return res.toString();
    }
}
