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
 * @history Created 20/10/2011
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:458:19/10/2015: Use class FastMath instead of class Math
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class is the Infinite Elliptic Cylinder class.
 * </p>
 * <p>
 * It represents the mathematical object by he same name
 * </p>
 * 
 * @useSample With the following parameters : <br>
 *            {@code   Vector3D axis = new Vector3D(0,0,1);}<br>
 *            {@code   Vector3D axisU = new Vector3D(1,0,0);}<br>
 *            {@code   double a = 1;}<br>
 *            {@code   double b = 1.5;}<br>
 *            The user may instanciate an infinite oblique circular cone :<br>
 *            {@code   InfiniteEllipticCylinder myCylinder = new InfiniteEllipticCylinder(axis, axisU, a, b);}
 * 
 * @concurrency immutable
 * 
 * @see InfiniteCylinder
 * @see Shape
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: InfiniteEllipticCylinder.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class InfiniteEllipticCylinder implements InfiniteCylinder, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -4454479846241988517L;

    /** Vector norm comparison (constructor) */
    private static final double CSTEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Double comparison */
    private static final double EPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Newton loop limit */
    private static final int NEWTONLIMIT = 100;

    /** Newton step limit */
    private static final double STEPLIMIT = 1;

    /** Incrementation rate limitation is pi/TRATE for θ */
    private static final double TRATE = 25;
    
    /** Value for default position */
    private static final int DEFAULT_POS = -3;
    
    /** Value for center position */
    private static final int CENTER_POS = -2;

    /** point of cylinders' axis */
    private final Vector3D origin;

    /** local X-axis */
    private final Vector3D localX;

    /** local Z-axis (Cylinder axis) */
    private final Vector3D localZ;

    /** Semi axis along X axis */
    private final double a;

    /** Semi axis along Y axis */
    private final double b;

    /** Transformation Matrix to standard basis */
    private final Matrix3D standardBasisTransform;

    /** Transformation Matrix to local cone basis */
    private final Matrix3D localBasisTransform;

    /**
     * This is the constructor for the class InfiniteEllipticCylinder. It allow the user to instantiate an infinite
     * elliptic cylinder. An infinite elliptic cylinder is a cylinder with the particularity that the result of the
     * intersection of the cylinder with a plane perpendicular to its axis is an ellipse.
     * 
     * @param myLocalOrigin
     *        Origin of cylinder. It is a point that belong to the cylinders' axis.
     * @param myDirection
     *        Cylinder direction
     * @param myXAxis
     *        Transver axis along which semi axis is a : corrected to be orthogonal to the direction
     * @param myA
     *        semi axis of cylinder along myXAxis
     * @param myB
     *        semi axis of cylinder along axis perpendicular to myXAxis and myAxis
     * @exception IllegalArgumentException
     *            if semi-axis or norm of revolution axis is null
     */
    public InfiniteEllipticCylinder(final Vector3D myLocalOrigin, final Vector3D myDirection, final Vector3D myXAxis,
        final double myA, final double myB) {

        /**
         * Input arguments check
         */
        // The axis' norm must be positive
        // The e-10 EPSilon is chosen to be coherent with the Plane class
        final String message = PatriusMessages.NUMBER_TOO_SMALL.getLocalizedString(Locale.getDefault());
        if (myDirection.getNorm() < CSTEPS) {
            throw new IllegalArgumentException(message);
        }
        if (myXAxis.getNorm() < CSTEPS) {
            throw new IllegalArgumentException(message);
        }
        if (myDirection.crossProduct(myXAxis).getNorm() < CSTEPS) {
            throw new IllegalArgumentException(message);
        }
        // The input angles must be between 0.0 and PI/2
        if (myA <= 0.0) {
            throw new IllegalArgumentException(message);
        }
        if (myB <= 0.0) {
            throw new IllegalArgumentException(message);
        }

        /**
         * Input tests passed. Create Object
         */

        // Assign cylinder props
        this.a = myA;
        this.b = myB;

        // Assign origin
        this.origin = myLocalOrigin;

        // Assign axes
        this.localZ = myDirection.normalize();
        this.localX = myXAxis.subtract(this.localZ.scalarMultiply(myXAxis.dotProduct(this.localZ))).normalize();
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
     * This method returns the position of the cylinder on the Oxy plane
     * 
     * @return A vector containing the position
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * This method returns the main axis of the cylinder
     * 
     * @return A vector containing the cylinders' axis
     */
    public Vector3D getDirection() {
        return this.localZ;
    }

    /**
     * This method returns the semi axis a
     * 
     * @return A double representing the semi axis along X axis
     */
    public double getSemiAxisA() {
        return this.a;
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
     * This method returns the semi axis b
     * 
     * @return A double representing the semi axis along b axis
     */
    public double getSemiAxisB() {
        return this.b;
    }

    /**
     * This method returns the matrix of the transformation to the standard basis
     * 
     * @return A vector containing the coordinates of the cylinder origin
     */
    public Matrix3D getStandardBasisTransform() {
        return this.standardBasisTransform;
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
     * Express a Vector3D in standard basis. Warning : Affine transformation
     * 
     * @param myVector
     *        Vector expressed in spheroid local frame
     * @return vectorRef Same vector expressed in standard basis
     */
    public Vector3D getAffineStandardExpression(final Vector3D myVector) {
        // Transformation is V_global = M * V_local + Origin
        return this.standardBasisTransform.multiply(myVector).add(this.origin);
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
     * Express a Vector3D in standard basis. Warning : Vectorial transformation
     * 
     * @param myVector
     *        Vector expressed in spheroid local frame
     * @return vectorRef Same vector expressed in standard basis
     */
    public Vector3D getVectorialStandardExpression(final Vector3D myVector) {
        // Transformation is V_global = M * V_local
        return this.standardBasisTransform.multiply(myVector);
    }

    /**
     * This method returns true if the user specified line intersects the cylinder.
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
     * Cylindrical coordinates
     * 
     * @param x
     *        coordinate
     * @param y
     *        coordinate
     * @param z
     *        coordiante
     * @return Cylindrical coordinate {theta, h}
     */
    private double[] getCylindricalCoordinates(final double x, final double y, final double z) {

        // Container
        final double[] coords = new double[3];

        // Transformation
        coords[0] = MathLib.atan2(y / this.b, x / this.a);
        coords[1] = z;

        return coords;
    }

    /**
     * Cartesian coordinates
     * 
     * @param theta
     *        angle
     * @param h
     *        height
     * @return Cartesian coordinates {x, y, z}
     */
    private double[] getCartesianCoordinates(final double theta, final double h) {
        // Container
        final double[] coords = new double[3];

        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double sin = sincosTheta[0];
        final double cos = sincosTheta[1];

        // Transformation
        coords[0] = this.a * cos;
        coords[1] = this.b * sin;
        coords[2] = h;

        return coords;
    }

    /**
     * Get polynomial data
     * 
     * @param lineOrg
     *        Line origin
     * @param lineDir
     *        Line direction
     * @return {delta, a<sub>2</sub>, a<sub>1</sub>, a<sub>0</sub>}
     */
    private double[] getDeterminantAndCoeffs(final Vector3D lineOrg, final Vector3D lineDir) {

        /**
         * Compute intersection with a line
         * 
         * The line is expressed in the following parametric equation system (considered in the cone local basis) :
         * 
         * For each coordinate q_i, q_i = alpha_i * t + beta_i with alpha_i and beta_i constant and t in R
         * 
         * Thus, the line intersects the cylinder if some value of t is found to satisfy the cylinders' equation. This
         * equation, in the cylinders' local basis, is given by :
         * 
         * x²/a² + y²/b² = 1
         * 
         * By reinjecting the triplet (x,y,z) corresponding to a point the line, we obtain a second degree polynomial
         * that is given by :
         * 
         * t2coeff x t² + t1coeff * t + t0coeff = 0
         * 
         * Solutions of this polynomial indicated the point corresponding to the t_sol value satisfy the cylinders'
         * equation, hereby belonging to the intersection of the line and the cylinder (that extends in R³).
         */
        double t2coeff =
            lineDir.getX() * lineDir.getX() / (this.a * this.a) + lineDir.getY() * lineDir.getY() / (this.b * this.b);
        final double t1coeff =
            2 * (lineDir.getX() * lineOrg.getX()) / (this.a * this.a) + 2 * (lineDir.getY() * lineOrg.getY())
                / (this.b * this.b);
        final double t0coeff =
            lineOrg.getX() * lineOrg.getX() / (this.a * this.a) + lineOrg.getY() * lineOrg.getY() / (this.b * this.b)
                - 1;

        final double delta;

        if (Precision.equals(t2coeff, 0, EPS) && Precision.equals(t1coeff, 0, EPS)) {
            if (Precision.equals(t0coeff, 0, EPS)) {
                delta = 0;
                t2coeff = 1;
            } else {
                delta = -1;
            }
        } else {
            delta = t1coeff * t1coeff - 4 * t2coeff * t0coeff;
        }

        return new double[] { delta, t2coeff, t1coeff, t0coeff };
    }

    /**
     * This methods computes and returns the intersection points between a line and the cylinder.
     * 
     * @param line
     *        The line with which the intersections are to be computed
     * @return A Vector3D array containing the intersections coordinates. If no intersections have been found, the array
     *         is empty.
     */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {

        // Initialise intersections container
        final Vector3D[] result;

        // Get line equations in spheroid frame
        final Vector3D lineOrg = this.getAffineLocalExpression(line.getOrigin());
        final Vector3D lineDir = this.getVectorialLocalExpression(line.getDirection());

        // Coefficients for intersection points computation
        final double[] equationData = this.getDeterminantAndCoeffs(lineOrg, lineDir);
        final double delta = equationData[0];
        final double t2coeff = equationData[1];
        final double t1coeff = equationData[2];

        if (delta > 0) {

            // two points
            result = new Vector3D[2];

            // Compute the corresponding t parameter
            final double t1 = (-t1coeff + MathLib.sqrt(delta)) / (2 * t2coeff);
            final double t2 = (-t1coeff - MathLib.sqrt(delta)) / (2 * t2coeff);

            // Compute the Positions in spheroid frame
            final Vector3D p1 = new Vector3D(lineDir.getX() * t1 + lineOrg.getX(),
                lineDir.getY() * t1 + lineOrg.getY(), lineDir.getZ() * t1 + lineOrg.getZ());
            final Vector3D p2 = new Vector3D(lineDir.getX() * t2 + lineOrg.getX(),
                lineDir.getY() * t2 + lineOrg.getY(), lineDir.getZ() * t2 + lineOrg.getZ());

            // Return points expressed in standard basis.
            // This is an affine transformation.
            result[0] = this.getAffineStandardExpression(p1);
            result[1] = this.getAffineStandardExpression(p2);

        } else if (Precision.equals(delta, 0)) {

            // two points
            result = new Vector3D[1];

            // Compute the corresponding t parameter
            final double t1 = (-t1coeff) / (2 * t2coeff);

            // Compute the Positions in spheroid frame
            final Vector3D p1 = new Vector3D(lineDir.getX() * t1 + lineOrg.getX(),
                lineDir.getY() * t1 + lineOrg.getY(), lineDir.getZ() * t1 + lineOrg.getZ());

            result[0] = this.getAffineStandardExpression(p1);

        } else {
            // No intersections
            result = new Vector3D[0];
        }

        return result;
    }

    /**
     * Return true if point is inside the cylinder
     * 
     * @param point
     *        in standard basis
     * @return boolean if is inside
     * 
     * @since 1.0
     */
    private boolean isInside(final Vector3D point) {

        // local transform
        final Vector3D localPoint = this.getAffineLocalExpression(point);

        // cone equ
        final double x = localPoint.getX();
        final double y = localPoint.getY();
        final double eqn = x * x / (this.a * this.a) + y * y / (this.b * this.b) - 1;

        // return
        return eqn < 0;

    }

    /**
     * Computes the transformation from any point in R³ its equivalent in the first octant. This function returns a 1x3
     * array containing +1 if the coordinate is in the first octant, -1 otherwise.
     * 
     * @param point
     *        user point expressed in the cylinders local frame
     * @return transformation a triplet of ±1
     */
    private double[] getOctantTransformation(final Vector3D point) {

        // Get user point coordinates in cone basis
        final double x = point.getX();
        final double y = point.getY();

        // Get transformation data
        final double xT = x < 0 ? -1 : 1;
        final double yT = y < 0 ? -1 : 1;

        return new double[] { xT, yT };
    }

    /**
     * Point location Returns :<br>
     * {@code    -2    } if point is at center<br>
     * {@code    -1    } if point is on ellipse<br>
     * {@code     0    } if point is inside or outside<br>
     * {@code     1    } if point is outside on x axis<br>
     * {@code     2    } if point is outside on y
     * axis<br>
     * 
     * @param x
     *        coordinate of point
     * @param y
     *        coordinate of point
     * @return position of point in first quadrant
     * */
    private int getPointLocation(final double x, final double y) {

        final double ellipseEquation = x * x / (this.a * this.a) + y * y / (this.b * this.b) - 1;

        int pos = DEFAULT_POS;

        if (ellipseEquation > 0) {
            // outside
            pos = 0;
            if (Precision.equals(y, 0, EPS)) {
                // on x axis
                pos = 1;
            } else if (Precision.equals(x, 0, EPS)) {
                // on y axis
                pos = 2;
            }
        } else {
            if (Precision.equals(ellipseEquation, 0, EPS)) {
                // on ellipse
                pos = -1;
            } else {
                if (Precision.equals(x, 0, EPS) && Precision.equals(y, 0, EPS)) {
                    // center
                    pos = CENTER_POS;
                } else {
                    // inside
                    pos = 0;
                }
            }
        }

        return pos;
    }

    /**
     * Optimized starting location
     * 
     * @param x
     *        coordinate
     * @param y
     *        coordinate
     * @return theta start
     */
    private double getOptimizedStartingLocation(final double x, final double y) {

        // create line
        final Vector3D userPoint = this.getAffineStandardExpression(new Vector3D(x, y, 0));
        final Vector3D centerPoint = this.getAffineStandardExpression(new Vector3D(0, 0, 0));
        final Line myLine = new Line(userPoint, centerPoint);

        // get intersection points
        final Vector3D[] iscs = this.getIntersectionPoints(myLine);

        iscs[0] = this.getAffineLocalExpression(iscs[0]);
        iscs[1] = this.getAffineLocalExpression(iscs[1]);

        final int index = iscs[0].getX() < 0 ? 1 : 0;

        // get cylindrical coords
        final double[] cc = this.getCylindricalCoordinates(iscs[index].getX(), iscs[index].getY(), iscs[index].getZ());

        return cc[0];
    }

    /**
     * Newton
     * 
     * @param x
     *        coordinate of target point
     * @param y
     *        coordinate of target point
     * @return theta finish
     */
    private double runNewtonAlgorithm(final double x, final double y) {

        // init containers
        int i = 0;
        double myF = 0;
        double myFp = 0;
        double step = 0;
        double stepNorm = 1;
        double ct;
        double st;
        double sign;

        // init starting point
        double theta = this.getOptimizedStartingLocation(x, y);
        // Newton loop
        while (i <= NEWTONLIMIT && stepNorm > EPS) {

            final double[] sincosTheta = MathLib.sinAndCos(theta);
            st = sincosTheta[0];
            ct = sincosTheta[1];

            myF = -(x - this.a * ct) * this.a * st + (y - this.b * st) * this.b * ct;
            myFp = -x * this.a * ct - y * this.b * st + (this.a * this.a - this.b * this.b) * (ct * ct - st * st);

            step = -MathLib.divide(myF, myFp);
            sign = step >= 0 ? 1 : -1;
            stepNorm = MathLib.abs(step);
            step = sign * MathLib.min(stepNorm, FastMath.PI / TRATE);

            theta = theta + step;

            i++;

            // System.out.println(theta);
        }

        // non convergence
        if (i >= NEWTONLIMIT) {
            throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, NEWTONLIMIT);
        }

        return theta;
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
        double distance = closestPoint.subtract(point).getNorm();

        // Check if the point is inside
        if (this.isInside(point)) {
            distance *= -1;
        }

        return distance;
    }

    /**
     * Newton method with line
     * 
     * @param line
     *        user Line
     * @return points in a Vector3D array expressed in standard basis
     */
    private Vector3D[] runNewtonV2(final Line line) {
        // A point p on the line is expressed as p = lineDir * t + lineOrg in standard basis
        final Vector3D lineOrg = line.getOrigin();
        final Vector3D lineDir = line.getDirection();

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
        int i = 0;
        double t = 0;
        final double dt = 1;
        double step = 1;
        currentPoint = lineOrg.add(lineDir.scalarMultiply(t));
        currentPoint1 = lineOrg.add(lineDir.scalarMultiply(t + dt));
        currentPoint2 = lineOrg.add(lineDir.scalarMultiply(t - dt));

        // Closest points
        pc = this.closestPointTo(currentPoint);
        fc = currentPoint.subtract(pc).getNorm();
        pc1 = this.closestPointTo(currentPoint1);
        fc1 = currentPoint1.subtract(pc1).getNorm();
        pc2 = this.closestPointTo(currentPoint2);
        fc2 = currentPoint2.subtract(pc2).getNorm();

        // Loop until convergence or max step reached
        while (i < NEWTONLIMIT && MathLib.abs(step) > EPS) {

            // distance derivatives
            fp = (fc1 - fc2) / (2 * dt);
            fpp = (fc1 - 2 * fc + fc2) / (dt * dt);

            // step and incrementation
            step = -MathLib.divide(fp, fpp);
            final double sign00 = step >= 0 ? 1 : -1;
            step = sign00 * MathLib.min(MathLib.abs(step), STEPLIMIT);
            t = t + step;

            // computation of new points
            currentPoint = lineOrg.add(lineDir.scalarMultiply(t));
            currentPoint1 = lineOrg.add(lineDir.scalarMultiply(t + dt));
            currentPoint2 = lineOrg.add(lineDir.scalarMultiply(t - dt));

            // distances
            pc = this.closestPointTo(currentPoint);
            fc = currentPoint.subtract(pc).getNorm();
            pc1 = this.closestPointTo(currentPoint1);
            fc1 = currentPoint1.subtract(pc1).getNorm();
            pc2 = this.closestPointTo(currentPoint2);
            fc2 = currentPoint2.subtract(pc2).getNorm();

            // keep track of steps
            i++;

        }
        if (i >= NEWTONLIMIT) {
            // Max count exception
            throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, NEWTONLIMIT);
        }

        // return result
        return new Vector3D[] { currentPoint, pc };
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
        final double z = localPoint.getZ();

        // Get the transformation to put the user point in the first octant
        final double[] t = this.getOctantTransformation(localPoint);
        final double xT = t[0];
        final double yT = t[1];

        // If not, move it there. The cone is symmetric (Oxz and Oyz). zT = 1 always.
        x = xT * x;
        y = yT * y;

        // Initialise point coordinates
        double xL;
        double yL;

        // Get closest point
        final int pointLocation = this.getPointLocation(x, y);

        if (pointLocation == CENTER_POS) {
            // point at center
            if (this.a <= this.b) {
                xL = this.a;
                yL = 0;
            } else {
                xL = 0;
                yL = this.b;
            }
        } else if (pointLocation == -1) {
            // point on ellipse
            xL = x;
            yL = y;
        } else if (pointLocation == 0) {
            // point inside or outside
            final double thetaF = this.runNewtonAlgorithm(x, y);
            final double[] coords = this.getCartesianCoordinates(thetaF, 0);
            xL = coords[0];
            yL = coords[1];
        } else if (pointLocation == 1) {
            // point outside on X axis
            xL = this.a;
            yL = 0;
        } else {
            // point outside on Y axis
            // pointLocation = 2
            xL = 0;
            yL = this.b;
        }

        // Move the computed point to the correct octant
        xL = xT * xL;
        yL = yT * yL;

        // return result
        return this.getAffineStandardExpression(new Vector3D(xL, yL, z));
    }

    /**
     * Calculate closest point to a line
     * 
     * @param line
     *        user line
     * @return points in a Vector3D array.
     * 
     * @see InfiniteEllipticCylinder#closestPointTo(Vector3D)
     * 
     * @since 1.0
     */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        /* This method returns the shortest distance from the user specified line to the spheroids surface */

        Vector3D[] points = new Vector3D[2];

        Vector3D lineOrg = this.getAffineLocalExpression(line.getOrigin());
        final Vector3D lineDir = this.getVectorialLocalExpression(line.getDirection()).normalize();
        lineOrg = new Vector3D(1.0, lineOrg, -Vector3D.dotProduct(lineOrg, lineDir), lineDir);

        // intersections
        final Vector3D[] iscs = this.getIntersectionPoints(line);

        if (iscs.length == 0) {

            if (line.getDirection().normalize().crossProduct(this.localZ).getNorm() <= CSTEPS) {
                // line along z-axis

                // line not on cylinder (or else iscs.length != 0)
                points[0] = this.getAffineStandardExpression(new Vector3D(lineOrg.getX(), lineOrg.getY(), 0));
                points[1] = this.closestPointTo(points[0]);

            } else {
                points = this.runNewtonV2(line);
            }

        } else {

            // if line intersects, return first intersection
            points[0] = iscs[0];
            points[1] = iscs[0];
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
        final Vector3D[] myPoints = this.closestPointTo(line);

        return myPoints[0].subtract(myPoints[1]).getNorm();

    }

    /**
     * Get a representation for this infinite elliptic cylinder.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this infinite elliptic cylinder
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
        // point of cylinders' axis coordinates:
        res.append("Origin");
        res.append(this.origin.toString());
        res.append(comma);
        // local Z-axis (Cylinder axis):
        res.append("Direction");
        res.append(this.localZ.toString());
        res.append(comma);
        // local X-axis:
        res.append("U vector");
        res.append(this.localX.toString());
        res.append(comma);
        // Semi axis along X axis:
        res.append("Radius A").append(open);
        res.append(this.a).append(close);
        res.append(comma);
        // Semi axis along Y axis:
        res.append("Radius B").append(open);
        res.append(this.b).append(close);

        res.append(close);
        return res.toString();
    }

}
