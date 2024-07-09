/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
* VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.text.NumberFormat;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.geometry.Space;
import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This class represents a 2D vector.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @version $Id: Vector2D.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Vector2D implements Vector<Euclidean2D> {

    /** Origin (coordinates: 0, 0). */
    public static final Vector2D ZERO = new Vector2D(0, 0);

    // CHECKSTYLE: stop ConstantName
    /** A vector with all coordinates set to NaN. */
    @SuppressWarnings("PMD.VariableNamingConventions")
    public static final Vector2D NaN = new Vector2D(Double.NaN, Double.NaN);
    // CHECKSTYLE: resume ConstantName

    /** A vector with all coordinates set to positive infinity. */
    public static final Vector2D POSITIVE_INFINITY =
        new Vector2D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /** A vector with all coordinates set to negative infinity. */
    public static final Vector2D NEGATIVE_INFINITY =
        new Vector2D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 266938651998679754L;

    /** Abscissa. */
    private final double x;

    /** Ordinate. */
    private final double y;

    /**
     * Simple constructor.
     * Build a vector from its coordinates
     * 
     * @param xIn
     *        abscissa
     * @param yIn
     *        ordinate
     * @see #getX()
     * @see #getY()
     */
    public Vector2D(final double xIn, final double yIn) {
        this.x = xIn;
        this.y = yIn;
    }

    /**
     * Simple constructor.
     * Build a vector from its coordinates
     * 
     * @param v
     *        coordinates array
     * @exception DimensionMismatchException
     *            if array does not have 2 elements
     * @see #toArray()
     */
    public Vector2D(final double[] v) {
        if (v.length != 2) {
            throw new DimensionMismatchException(v.length, 2);
        }
        this.x = v[0];
        this.y = v[1];
    }

    /**
     * Multiplicative constructor
     * Build a vector from another one and a scale factor.
     * The vector built will be a * u
     * 
     * @param a
     *        scale factor
     * @param u
     *        base (unscaled) vector
     */
    public Vector2D(final double a, final Vector2D u) {
        this.x = a * u.x;
        this.y = a * u.y;
    }

    /**
     * Linear constructor
     * Build a vector from two other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     */
    public Vector2D(final double a1, final Vector2D u1, final double a2, final Vector2D u2) {
        this.x = a1 * u1.x + a2 * u2.x;
        this.y = a1 * u1.y + a2 * u2.y;
    }

    /**
     * Linear constructor
     * Build a vector from three other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     */
    public Vector2D(final double a1, final Vector2D u1, final double a2, final Vector2D u2,
        final double a3, final Vector2D u3) {
        this.x = a1 * u1.x + a2 * u2.x + a3 * u3.x;
        this.y = a1 * u1.y + a2 * u2.y + a3 * u3.y;
    }

    /**
     * Linear constructor
     * Build a vector from four other ones and corresponding scale factors.
     * The vector built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4
     * 
     * @param a1
     *        first scale factor
     * @param u1
     *        first base (unscaled) vector
     * @param a2
     *        second scale factor
     * @param u2
     *        second base (unscaled) vector
     * @param a3
     *        third scale factor
     * @param u3
     *        third base (unscaled) vector
     * @param a4
     *        fourth scale factor
     * @param u4
     *        fourth base (unscaled) vector
     */
    public Vector2D(final double a1, final Vector2D u1, final double a2, final Vector2D u2,
        final double a3, final Vector2D u3, final double a4, final Vector2D u4) {
        this.x = a1 * u1.x + a2 * u2.x + a3 * u3.x + a4 * u4.x;
        this.y = a1 * u1.y + a2 * u2.y + a3 * u3.y + a4 * u4.y;
    }

    /**
     * Get the abscissa of the vector.
     * 
     * @return abscissa of the vector
     * @see #Vector2D(double, double)
     */
    public double getX() {
        return this.x;
    }

    /**
     * Get the ordinate of the vector.
     * 
     * @return ordinate of the vector
     * @see #Vector2D(double, double)
     */
    public double getY() {
        return this.y;
    }

    /**
     * Get the vector coordinates as a dimension 2 array.
     * 
     * @return vector coordinates
     * @see #Vector2D(double[])
     */
    public double[] toArray() {
        return new double[] { this.x, this.y };
    }

    /** {@inheritDoc} */
    @Override
    public Space getSpace() {
        return Euclidean2D.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D getZero() {
        return ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm1() {
        return MathLib.abs(this.x) + MathLib.abs(this.y);
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm() {
        return MathLib.sqrt(this.x * this.x + this.y * this.y);
    }

    /** {@inheritDoc} */
    @Override
    public double getNormSq() {
        return this.x * this.x + this.y * this.y;
    }

    /** {@inheritDoc} */
    @Override
    public double getNormInf() {
        return MathLib.max(MathLib.abs(this.x), MathLib.abs(this.y));
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D add(final Vector<Euclidean2D> v) {
        final Vector2D v2 = (Vector2D) v;
        return new Vector2D(this.x + v2.getX(), this.y + v2.getY());
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D add(final double factor, final Vector<Euclidean2D> v) {
        final Vector2D v2 = (Vector2D) v;
        return new Vector2D(this.x + factor * v2.getX(), this.y + factor * v2.getY());
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D subtract(final Vector<Euclidean2D> p) {
        final Vector2D p3 = (Vector2D) p;
        return new Vector2D(this.x - p3.x, this.y - p3.y);
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D subtract(final double factor, final Vector<Euclidean2D> v) {
        final Vector2D v2 = (Vector2D) v;
        return new Vector2D(this.x - factor * v2.getX(), this.y - factor * v2.getY());
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D normalize() {
        final double s = this.getNorm();
        if (s == 0) {
            throw new MathArithmeticException(PatriusMessages.CANNOT_NORMALIZE_A_ZERO_NORM_VECTOR);
        }
        return this.scalarMultiply(1 / s);
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D negate() {
        return new Vector2D(-this.x, -this.y);
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D scalarMultiply(final double a) {
        return new Vector2D(a * this.x, a * this.y);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNaN() {
        return Double.isNaN(this.x) || Double.isNaN(this.y);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInfinite() {
        return !this.isNaN() && (Double.isInfinite(this.x) || Double.isInfinite(this.y));
    }

    /** {@inheritDoc} */
    @Override
    public double distance1(final Vector<Euclidean2D> p) {
        final Vector2D p3 = (Vector2D) p;
        final double dx = MathLib.abs(p3.x - this.x);
        final double dy = MathLib.abs(p3.y - this.y);
        return dx + dy;
    }

    /** {@inheritDoc} */
    @Override
    public double distance(final Vector<Euclidean2D> p) {
        final Vector2D p3 = (Vector2D) p;
        final double dx = p3.x - this.x;
        final double dy = p3.y - this.y;
        return MathLib.sqrt(dx * dx + dy * dy);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceInf(final Vector<Euclidean2D> p) {
        final Vector2D p3 = (Vector2D) p;
        final double dx = MathLib.abs(p3.x - this.x);
        final double dy = MathLib.abs(p3.y - this.y);
        return MathLib.max(dx, dy);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceSq(final Vector<Euclidean2D> p) {
        final Vector2D p3 = (Vector2D) p;
        final double dx = p3.x - this.x;
        final double dy = p3.y - this.y;
        return dx * dx + dy * dy;
    }

    /** {@inheritDoc} */
    @Override
    public double dotProduct(final Vector<Euclidean2D> v) {
        final Vector2D v2 = (Vector2D) v;
        return this.x * v2.x + this.y * v2.y;
    }

    /**
     * Compute the distance between two vectors according to the L<sub>2</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>p1.subtract(p2).getNorm()</code> except that no intermediate
     * vector is built
     * </p>
     * 
     * @param p1
     *        first vector
     * @param p2
     *        second vector
     * @return the distance between p1 and p2 according to the L<sub>2</sub> norm
     */
    public static double distance(final Vector2D p1, final Vector2D p2) {
        return p1.distance(p2);
    }

    /**
     * Compute the distance between two vectors according to the L<sub>&infin;</sub> norm.
     * <p>
     * Calling this method is equivalent to calling: <code>p1.subtract(p2).getNormInf()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param p1
     *        first vector
     * @param p2
     *        second vector
     * @return the distance between p1 and p2 according to the L<sub>&infin;</sub> norm
     */
    public static double distanceInf(final Vector2D p1, final Vector2D p2) {
        return p1.distanceInf(p2);
    }

    /**
     * Compute the square of the distance between two vectors.
     * <p>
     * Calling this method is equivalent to calling: <code>p1.subtract(p2).getNormSq()</code> except that no
     * intermediate vector is built
     * </p>
     * 
     * @param p1
     *        first vector
     * @param p2
     *        second vector
     * @return the square of the distance between p1 and p2
     */
    public static double distanceSq(final Vector2D p1, final Vector2D p2) {
        return p1.distanceSq(p2);
    }

    /**
     * Test for the equality of two 2D vectors.
     * <p>
     * If all coordinates of two 2D vectors are exactly the same, and none are <code>Double.NaN</code>, the two 2D
     * vectors are considered to be equal.
     * </p>
     * <p>
     * <code>NaN</code> coordinates are considered to affect globally the vector and be equals to each other - i.e, if
     * either (or all) coordinates of the 2D vector are equal to <code>Double.NaN</code>, the 2D vector is equal to
     * {@link #NaN}.
     * </p>
     * 
     * @param other
     *        Object to test for equality to this
     * @return true if two 2D vector objects are equal, false if
     *         object is null, not an instance of Vector2D, or
     *         not equal to this Vector2D instance
     * 
     */
    @Override
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof Vector2D) {
            final Vector2D rhs = (Vector2D) other;
            if (rhs.isNaN()) {
                return this.isNaN();
            }

            return (this.x == rhs.x) && (this.y == rhs.y);
        }
        return false;
    }

    // CHECKSTYLE: stop MagicNumber check
    // Reason: model - Commons-Math code

    /**
     * Get a hashCode for the 2D vector.
     * <p>
     * All NaN values have the same hash code.
     * </p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (this.isNaN()) {
            return 542;
        }
        return 122 * (76 * MathUtils.hash(this.x) + MathUtils.hash(this.y));
    }

    // CHECKSTYLE: resume MagicNumber check

    /**
     * Get a string representation of this vector.
     * 
     * @return a string representation of this vector
     */
    @Override
    public String toString() {
        return Vector2DFormat.getInstance().format(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final NumberFormat format) {
        return new Vector2DFormat(format).format(this);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getRealVector() {
        final double[] data = { this.x, this.y };
        return new ArrayRealVector(data);
    }

    /**
     * For a given vector, get the angle between vector and X-axis counted in counter-clockwise direction: 0
     * corresponds to Vector2D(1, 0), and increasing values are counter-clockwise.
     * 
     * @return the angle between vector and X-axis counted in counter-clockwise direction (&alpha;)
     *         (between -PI and +PI)
     */ 
    public double getAlpha() {
        return MathLib.atan2(y, x);
    }

    // CHECKSTYLE: resume IllegalType check
}
