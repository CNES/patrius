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
 * @history creation 10/10/2011
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;
import java.util.Locale;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This is a describing class for a 3D infinite cone, with some algorithm to compute intersections and distances to some
 * other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with an angle and two Vector3D : Vector3D origin = new Vector3D(1.0, 6.0, -2.0); Vector3D axis =
 *            new Vector3D(6.0, -3.0, -1.0); InfiniteEllipticCone cone = new InfiniteEllipticCone(origin, axis;
 *            FastMath.PI / 4.0); Intersection with a line : boolean intersects = cone(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: InfiniteRightCircularCone.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class InfiniteRightCircularCone implements InfiniteCone, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -6525651616917874512L;

    /** Vectors comparisons epsilon */
    private static final double VECTORSCOMPARISONEPS = UtilsPatrius.GEOMETRY_EPSILON;

    /** Double comparison */
    private static final double EPS = UtilsPatrius.DOUBLE_COMPARISON_EPSILON;

    /** Position of the origin of the cone */
    private final Vector3D origin;

    /** Axis of the cone, defines its orientation : Z of the local frame */
    private final Vector3D axis;

    /** Half angle of the cone */
    private final double angle;

    /** tangent of the angle */
    private final double tan;

    /** sine of the angle */
    private final double sin;

    /** rotation matrix to the local frame */
    private final Matrix3D rotationMatrix;

    /**
     * Enum for z intersects.
     */
    private enum ZIntersects {
        /** z1 and z2 intersect. */
        BOTH,
        /** z1 intersects. */
        Z1,
        /** z2 intersects. */
        Z2,
        /** None. */
        NONE;
    }

    /**
     * Build an infinite elliptic cone from its angle, the position of its origin and its axis
     * 
     * @param inOrigin
     *        the origin of the cone
     * @param inDirection
     *        the direction : axis of the cone
     * @param inAngle
     *        the angle of the cone
     * @exception IllegalArgumentException
     *            if the angle is not between 0.0 and PI/4 or if the axis has a null norm
     */
    public InfiniteRightCircularCone(final Vector3D inOrigin, final Vector3D inDirection, final double inAngle) {

        // The axis' norm must be positive
        // The e-10 epsilon is chosen to be coherent with the Plane class
        String message = PatriusMessages.ZERO_NOT_ALLOWED.getLocalizedString(Locale.getDefault());
        if (inDirection.getNorm() < VECTORSCOMPARISONEPS) {
            throw new IllegalArgumentException(message);
        }
        // The angle must be between 0.0 and PI/2
        message = PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault());
        if (inAngle <= 0.0 || inAngle >= MathUtils.HALF_PI) {
            throw new IllegalArgumentException(message);
        }

        this.origin = new Vector3D(1.0, inOrigin);
        this.axis = inDirection.normalize();
        this.angle = inAngle;
        this.tan = MathLib.tan(this.angle);
        this.sin = MathLib.sin(this.angle);

        // creation of the local frame
        final Vector3D u = this.axis.orthogonal();
        final Vector3D v = Vector3D.crossProduct(this.axis, u);

        final double[][] matrixData =
        { { u.getX(), u.getY(), u.getZ() }, { v.getX(), v.getY(), v.getZ() },
            { this.axis.getX(), this.axis.getY(), this.axis.getZ() } };
        this.rotationMatrix = new Matrix3D(matrixData);

    }

    /**
     * @return the origin
     */
    @Override
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the axis
     */
    public Vector3D getAxis() {
        return this.axis;
    }

    /**
     * @return the angle
     */
    public double getAngle() {
        return this.angle;
    }

    /** {@inheritDoc} */
    @Override
    public boolean intersects(final Line line) {

        // Computation of the origin and direction of the line expressed on the local frame
        // of the cone
        final Vector3D lineDirectionVect = this.rotationMatrix.multiply(line.getDirection());
        final double[] lineDirection = { lineDirectionVect.getX(), lineDirectionVect.getY(), lineDirectionVect.getZ() };
        final Vector3D lineOriginVect = this.rotationMatrix.multiply(line.getOrigin().subtract(this.origin));
        final double[] lineOrigin = { lineOriginVect.getX(), lineOriginVect.getY(), lineOriginVect.getZ() };

        // Computation of the solutions of the intersection equation
        final double[] alphaSolutions = this.alphaIntersections(lineOrigin, lineDirection);

        // the return is TRUE if there is at least one solution
        return alphaSolutions.length > 0;

    }

    /**
     * Computes the solution of the alpha parameter such as lineOrigin + alpha * lineDirection belongs to the surface.
     * 
     * @param lineOrigin
     *        the coordinates of the origin of the line
     * @param lineDirection
     *        the coordinates of the direction vector of the line
     * @return the solutions of the intersection equation in alpha parameter
     */
    private double[] alphaIntersections(final double[] lineOrigin, final double[] lineDirection) {
        // Computation of the coefficients of the equation
        final double a =
            this.tan * this.tan * lineDirection[2] * lineDirection[2] - lineDirection[0] * lineDirection[0]
                - lineDirection[1] * lineDirection[1];
        final double b = 2.0 * (this.tan * this.tan * lineDirection[2] *
            lineOrigin[2] - lineDirection[1] * lineOrigin[1] - lineDirection[0]
            * lineOrigin[0]);
        final double c =
            this.tan * this.tan * lineOrigin[2] * lineOrigin[2] - lineOrigin[1] * lineOrigin[1] - lineOrigin[0]
                * lineOrigin[0];

        double[] solution = new double[0];

        // case when the line's angle with the axis equals the angle of the cone
        if ((MathLib.abs(a) < EPS)
            && !(MathLib.abs(b) < EPS)) {

            // the solution's Z coordinate must be positive
            final double alpha = -MathLib.divide(c, b);
            final double zIntersect = lineOrigin[2] + alpha * lineDirection[2];
            if (zIntersect >= 0.0) {
                solution = new double[1];
                solution[0] = alpha;
            }
        } else {
            // general case
            // call to the private solver
            solution = this.alphaSolver(a, b, c, lineOrigin, lineDirection);
        }

        return solution;
    }

    /**
     * Computes the alpha parameter(s) such as an intersection point is lineOrigin + alpha * lineDirection.
     * 
     * @param a
     *        The first term of the equation
     * @param b
     *        The second term of the equation
     * @param c
     *        The third term of the equation
     * @param lineOrigin
     *        the line's origin vector
     * @param lineDirection
     *        the line's direction vector
     * @return the solution found in alpha parameter
     */
    private double[] alphaSolver(final double a, final double b, final double c, final double[] lineOrigin,
                                 final double[] lineDirection) {

        double[] solution = new double[0];

        // determinant computation
        final double det = b * b - 4 * a * c;

        // One or two intersections
        if (MathLib.abs(det) < EPS) {
            final double alpha = -b / (2 * a);

            // the solution's Z coordinate must be positive
            final double zIntersect = lineOrigin[2] + alpha * lineDirection[2];
            if (zIntersect >= 0.0) {
                solution = new double[1];
                solution[0] = alpha;
            }
        } else if (det > 0) {

            // the solution's Z coordinate must be positive
            final double alpha1 = (-b - MathLib.sqrt(det)) / (2 * a);
            final double alpha2 = (-b + MathLib.sqrt(det)) / (2 * a);
            final double zIntersect1 = lineOrigin[2] + alpha1 * lineDirection[2];
            final double zIntersect2 = lineOrigin[2] + alpha2 * lineDirection[2];

            // see if one or another intersects
            final boolean zIntersect1b = zIntersect1 >= 0.0;
            final boolean zIntersect2b = zIntersect2 >= 0.0;

            // enum type
            final ZIntersects intersects = this.zIntersects(zIntersect1b, zIntersect2b);

            switch (intersects) {
                case BOTH:
                    solution = new double[2];
                    solution[0] = alpha1;
                    solution[1] = alpha2;
                    break;
                case Z1:
                    solution = new double[1];
                    solution[0] = alpha1;
                    break;
                case Z2:
                    solution = new double[1];
                    solution[0] = alpha2;
                    break;
                default:
                    // nothing happens here
            }
        }

        return solution;
    }

    /**
     * Internal method to evaluate z intersects
     * 
     * @param z1
     *        if z1 intersects
     * @param z2
     *        if z1 intersects
     * @return which z intersects
     */
    private ZIntersects zIntersects(final boolean z1, final boolean z2) {
        ZIntersects result = ZIntersects.NONE;
        // both
        if (z1 && z2) {
            result = ZIntersects.BOTH;
        } else if (z1) {
            // first
            result = ZIntersects.Z1;
        } else if (z2) {
            // second
            result = ZIntersects.Z2;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] getIntersectionPoints(final Line line) {
        // Computation of the origin and direction of the line expressed on the local frame
        // of the cone
        final Vector3D lineDirectionVect = this.rotationMatrix.multiply(line.getDirection());
        final double[] lineDirection = { lineDirectionVect.getX(), lineDirectionVect.getY(), lineDirectionVect.getZ() };
        final Vector3D lineOriginVect = this.rotationMatrix.multiply(line.getOrigin().subtract(this.origin));
        final double[] lineOrigin = { lineOriginVect.getX(), lineOriginVect.getY(), lineOriginVect.getZ() };

        // Computation of the solutions of the intersection equation
        final double[] alphaSolutions = this.alphaIntersections(lineOrigin, lineDirection);

        // Initialisations
        final int lenght = alphaSolutions.length;
        final Vector3D[] intersections = new Vector3D[lenght];

        for (int i = 0; i < lenght; i++) {
            intersections[i] = new Vector3D(lineOrigin[0] + alphaSolutions[i] * lineDirection[0], lineOrigin[1]
                + alphaSolutions[i] * lineDirection[1], lineOrigin[2] + alphaSolutions[i] * lineDirection[2]);
            intersections[i] = this.rotationMatrix.transposeAndMultiply(intersections[i]).add(this.origin);
        }

        return intersections;
    }

    /** {@inheritDoc} */
    @Override
    public double distanceTo(final Line line) {
        if (this.intersects(line)) {
            return 0.0;
        } else {
            return line.distance(this.noIntersectClosestPoint(line));
        }
    }

    /**
     * If there is no intersection with the cone, this method computes the point realizing the shortest distance to a
     * line. This point is found computing the (point of the line) - cone distance derivative.
     * 
     * @param line
     *        the line
     * @return the distance to the cone
     */
    private Vector3D noIntersectClosestPoint(final Line line) {

        Vector3D point = null;

        // Computation of the origin and direction of the line expressed on the local frame
        // of the cone
        final Vector3D lineDirectionVect = this.rotationMatrix.multiply(line.getDirection());
        final double[] lineDirection = { lineDirectionVect.getX(), lineDirectionVect.getY(), lineDirectionVect.getZ() };
        final Vector3D lineOriginVect = this.rotationMatrix.multiply(line.getOrigin().subtract(this.origin));
        final double[] lineOrigin = { lineOriginVect.getX(), lineOriginVect.getY(), lineOriginVect.getZ() };

        // Computation of the terms of the distance derivative
        final double coeff1 = lineDirection[0] * lineOrigin[0] + lineDirection[1] * lineOrigin[1];
        final double coeff3 = lineDirection[0] * lineDirection[0] + lineDirection[1] * lineDirection[1];
        final double coeff4 = lineDirection[2] * lineDirection[2] * this.tan * this.tan;

        // computation of the coefficient of the null derivative equation solving
        final double a = coeff3 * (coeff4 - coeff3);
        final double b = 2 * coeff1 * (coeff4 - coeff3);

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

            // the solution must be a point close to the superior part of the cone
            final double solNorm = MathLib.sqrt(sol1[0] * sol1[0] + sol1[1] * sol1[1] + sol1[2] * sol1[2]);
            final double angleToZ1 = MathLib.divide(sol1[2], solNorm);
            // computation of the projected point on the cone
            if (angleToZ1 > -this.sin) {
                // creation of the direction vector to the solution point
                // in the local frame
                // tan cannot be 0
                final Vector3D directionPoint = new Vector3D(sol1[0], sol1[1], MathLib.sqrt(sol1[0] * sol1[0]
                    + sol1[1] * sol1[1])
                    / this.tan);

                // solution in the local frame
                point = new Vector3D(solNorm * MathLib.cos(angleToZ1 - this.angle), directionPoint.normalize());

                // frame changing
                point = this.rotationMatrix.transposeAndMultiply(point).add(this.origin);
            }

        }

        if (point == null) {
            // if no point is a zero of the derivative of the distance on the superior
            // part of the cone, the closest point from the cone to the line is the origin of the cone
            point = this.origin;
        }

        return point;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D[] closestPointTo(final Line line) {
        final Vector3D[] points = new Vector3D[2];

        // if an intersection id found, the two points are identical
        if (this.intersects(line)) {
            points[0] = this.getIntersectionPoints(line)[0];
            points[1] = points[0];
        } else {
            // if no intersection is found, use of the private method
            // to find the point of the cone and then projection on the line
            points[1] = this.noIntersectClosestPoint(line);
            points[0] = line.toSpace(line.toSubSpace(points[1]));
        }

        return points;
    }

    /**
     * Get a representation for this infinite elliptic cone.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a representation for this infinite elliptic cone
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
        res.append(this.axis.toString()).append(comma);
        // "Angle":
        res.append("Angle").append(open);
        res.append(this.angle).append(close);
        res.append(close);

        return res.toString();
    }

}
