/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import java.text.NumberFormat;

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
 * This class represents a 1D vector.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @version $Id: Vector1D.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class Vector1D implements Vector<Euclidean1D> {

    /** Origin (coordinates: 0). */
    public static final Vector1D ZERO = new Vector1D(0.0);

    /** Unit (coordinates: 1). */
    public static final Vector1D ONE = new Vector1D(1.0);

    // CHECKSTYLE: stop ConstantName
    /** A vector with all coordinates set to NaN. */
    @SuppressWarnings("PMD.VariableNamingConventions")
    public static final Vector1D NaN = new Vector1D(Double.NaN);
    // CHECKSTYLE: resume ConstantName

    /** A vector with all coordinates set to positive infinity. */
    public static final Vector1D POSITIVE_INFINITY =
        new Vector1D(Double.POSITIVE_INFINITY);

    /** A vector with all coordinates set to negative infinity. */
    public static final Vector1D NEGATIVE_INFINITY =
        new Vector1D(Double.NEGATIVE_INFINITY);

    /** Serializable UID. */
    private static final long serialVersionUID = 7556674948671647925L;

    /** Abscissa. */
    private final double x;

    /**
     * Simple constructor.
     * Build a vector from its coordinates
     * 
     * @param xIn
     *        abscissa
     * @see #getX()
     */
    public Vector1D(final double xIn) {
        this.x = xIn;
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
    public Vector1D(final double a, final Vector1D u) {
        this.x = a * u.x;
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
    public Vector1D(final double a1, final Vector1D u1, final double a2, final Vector1D u2) {
        this.x = a1 * u1.x + a2 * u2.x;
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
    public Vector1D(final double a1, final Vector1D u1, final double a2, final Vector1D u2,
        final double a3, final Vector1D u3) {
        this.x = a1 * u1.x + a2 * u2.x + a3 * u3.x;
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
    public Vector1D(final double a1, final Vector1D u1, final double a2, final Vector1D u2,
        final double a3, final Vector1D u3, final double a4, final Vector1D u4) {
        this.x = a1 * u1.x + a2 * u2.x + a3 * u3.x + a4 * u4.x;
    }

    /**
     * Get the abscissa of the vector.
     * 
     * @return abscissa of the vector
     * @see #Vector1D(double)
     */
    public double getX() {
        return this.x;
    }

    /** {@inheritDoc} */
    @Override
    public Space getSpace() {
        return Euclidean1D.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D getZero() {
        return ZERO;
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm1() {
        return MathLib.abs(this.x);
    }

    /** {@inheritDoc} */
    @Override
    public double getNorm() {
        return MathLib.abs(this.x);
    }

    /** {@inheritDoc} */
    @Override
    public double getNormSq() {
        return this.x * this.x;
    }

    /** {@inheritDoc} */
    @Override
    public double getNormInf() {
        return MathLib.abs(this.x);
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D add(final Vector<Euclidean1D> v) {
        final Vector1D v1 = (Vector1D) v;
        return new Vector1D(this.x + v1.getX());
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D add(final double factor, final Vector<Euclidean1D> v) {
        final Vector1D v1 = (Vector1D) v;
        return new Vector1D(this.x + factor * v1.getX());
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D subtract(final Vector<Euclidean1D> p) {
        final Vector1D p3 = (Vector1D) p;
        return new Vector1D(this.x - p3.x);
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D subtract(final double factor, final Vector<Euclidean1D> v) {
        final Vector1D v1 = (Vector1D) v;
        return new Vector1D(this.x - factor * v1.getX());
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D normalize() {
        final double s = this.getNorm();
        if (s == 0) {
            throw new MathArithmeticException(PatriusMessages.CANNOT_NORMALIZE_A_ZERO_NORM_VECTOR);
        }
        return this.scalarMultiply(1 / s);
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D negate() {
        return new Vector1D(-this.x);
    }

    /** {@inheritDoc} */
    @Override
    public Vector1D scalarMultiply(final double a) {
        return new Vector1D(a * this.x);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNaN() {
        return Double.isNaN(this.x);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInfinite() {
        return !this.isNaN() && Double.isInfinite(this.x);
    }

    /** {@inheritDoc} */
    @Override
    public double distance1(final Vector<Euclidean1D> p) {
        final Vector1D p3 = (Vector1D) p;
        return MathLib.abs(p3.x - this.x);
    }

    /** {@inheritDoc} */
    @Override
    public double distance(final Vector<Euclidean1D> p) {
        final Vector1D p3 = (Vector1D) p;
        final double dx = p3.x - this.x;
        return MathLib.abs(dx);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceInf(final Vector<Euclidean1D> p) {
        final Vector1D p3 = (Vector1D) p;
        return MathLib.abs(p3.x - this.x);
    }

    /** {@inheritDoc} */
    @Override
    public double distanceSq(final Vector<Euclidean1D> p) {
        final Vector1D p3 = (Vector1D) p;
        final double dx = p3.x - this.x;
        return dx * dx;
    }

    /** {@inheritDoc} */
    @Override
    public double dotProduct(final Vector<Euclidean1D> v) {
        final Vector1D v1 = (Vector1D) v;
        return this.x * v1.x;
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
    public static double distance(final Vector1D p1, final Vector1D p2) {
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
    public static double distanceInf(final Vector1D p1, final Vector1D p2) {
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
    public static double distanceSq(final Vector1D p1, final Vector1D p2) {
        return p1.distanceSq(p2);
    }

    /**
     * Test for the equality of two 1D vectors.
     * <p>
     * If all coordinates of two 1D vectors are exactly the same, and none are <code>Double.NaN</code>, the two 1D
     * vectors are considered to be equal.
     * </p>
     * <p>
     * <code>NaN</code> coordinates are considered to affect globally the vector and be equals to each other - i.e, if
     * either (or all) coordinates of the 1D vector are equal to <code>Double.NaN</code>, the 1D vector is equal to
     * {@link #NaN}.
     * </p>
     * 
     * @param other
     *        Object to test for equality to this
     * @return true if two 1D vector objects are equal, false if
     *         object is null, not an instance of Vector1D, or
     *         not equal to this Vector1D instance
     * 
     */
    @Override
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof Vector1D) {
            final Vector1D rhs = (Vector1D) other;
            if (rhs.isNaN()) {
                return this.isNaN();
            }

            return this.x == rhs.x;
        }
        return false;
    }

    // CHECKSTYLE: stop MagicNumber check
    // Reason: model - Commons-Math code

    /**
     * Get a hashCode for the 1D vector.
     * <p>
     * All NaN values have the same hash code.
     * </p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (this.isNaN()) {
            return 7785;
        }
        return 997 * MathUtils.hash(this.x);
    }

    // CHECKSTYLE: resume MagicNumber check

    /**
     * Get a string representation of this vector.
     * 
     * @return a string representation of this vector
     */
    @Override
    public String toString() {
        return Vector1DFormat.getInstance().format(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final NumberFormat format) {
        return new Vector1DFormat(format).format(this);
    }

    /** {@inheritDoc} */
    @Override
    public RealVector getRealVector() {
        final double[] data = { this.x };
        return new ArrayRealVector(data);
    }

    // CHECKSTYLE: resume IllegalType check
}
