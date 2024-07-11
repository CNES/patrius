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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.complex;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implements <a href="http://mathworld.wolfram.com/Quaternion.html">
 * quaternions</a> (Hamilton's hypercomplex numbers). <br/>
 * Instance of this class are guaranteed to be immutable.
 * 
 * @since 3.1
 * @version $Id: Quaternion.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class Quaternion implements Serializable {
    /** Identity quaternion. */
    public static final Quaternion IDENTITY = new Quaternion(1, 0, 0, 0);
    /** Zero quaternion. */
    public static final Quaternion ZERO = new Quaternion(0, 0, 0, 0);
    /** i */
    public static final Quaternion I = new Quaternion(0, 1, 0, 0);
    /** j */
    public static final Quaternion J = new Quaternion(0, 0, 1, 0);
    /** k */
    public static final Quaternion K = new Quaternion(0, 0, 0, 1);

    /** Serializable UID. */
    private static final long serialVersionUID = 20092012L;

    /** First component (scalar part). */
    private final double q0;
    /** Second component (first vector part). */
    private final double q1;
    /** Third component (second vector part). */
    private final double q2;
    /** Fourth component (third vector part). */
    private final double q3;

    /**
     * Builds a quaternion from its components.
     * 
     * @param a
     *        Scalar component.
     * @param b
     *        First vector component.
     * @param c
     *        Second vector component.
     * @param d
     *        Third vector component.
     */
    public Quaternion(final double a,
        final double b,
        final double c,
        final double d) {
        this.q0 = a;
        this.q1 = b;
        this.q2 = c;
        this.q3 = d;
    }

    /**
     * Builds a quaternion from scalar and vector parts.
     * 
     * @param scalar
     *        Scalar part of the quaternion.
     * @param v
     *        Components of the vector part of the quaternion.
     * 
     * @throws DimensionMismatchException
     *         if the array length is not 3.
     */
    public Quaternion(final double scalar,
        final double[] v) {
        if (v.length != 3) {
            throw new DimensionMismatchException(v.length, 3);
        }
        this.q0 = scalar;
        this.q1 = v[0];
        this.q2 = v[1];
        this.q3 = v[2];
    }

    /**
     * Builds a pure quaternion from a vector (assuming that the scalar
     * part is zero).
     * 
     * @param v
     *        Components of the vector part of the pure quaternion.
     */
    public Quaternion(final double[] v) {
        this(0, v);
    }

    /**
     * Returns the conjugate quaternion of the instance.
     * 
     * @return the conjugate quaternion
     */
    public Quaternion getConjugate() {
        return new Quaternion(this.q0, -this.q1, -this.q2, -this.q3);
    }

    /**
     * Returns the Hamilton product of two quaternions.
     * 
     * @param q1
     *        First quaternion.
     * @param q2
     *        Second quaternion.
     * @return the product {@code q1} and {@code q2}, in that order.
     */
    public static Quaternion multiply(final Quaternion q1, final Quaternion q2) {
        // Components of the first quaternion.
        final double q1a = q1.getQ0();
        final double q1b = q1.getQ1();
        final double q1c = q1.getQ2();
        final double q1d = q1.getQ3();

        // Components of the second quaternion.
        final double q2a = q2.getQ0();
        final double q2b = q2.getQ1();
        final double q2c = q2.getQ2();
        final double q2d = q2.getQ3();

        // Components of the product.
        final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
        final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
        final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
        final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

        return new Quaternion(w, x, y, z);
    }

    /**
     * Returns the Hamilton product of the instance by a quaternion.
     * 
     * @param q
     *        Quaternion.
     * @return the product of this instance with {@code q}, in that order.
     */
    public Quaternion multiply(final Quaternion q) {
        return multiply(this, q);
    }

    /**
     * Computes the sum of two quaternions.
     * 
     * @param q1
     *        Quaternion.
     * @param q2
     *        Quaternion.
     * @return the sum of {@code q1} and {@code q2}.
     */
    public static Quaternion add(final Quaternion q1,
                                 final Quaternion q2) {
        return new Quaternion(q1.getQ0() + q2.getQ0(),
            q1.getQ1() + q2.getQ1(),
            q1.getQ2() + q2.getQ2(),
            q1.getQ3() + q2.getQ3());
    }

    /**
     * Computes the sum of the instance and another quaternion.
     * 
     * @param q
     *        Quaternion.
     * @return the sum of this instance and {@code q}
     */
    public Quaternion add(final Quaternion q) {
        return add(this, q);
    }

    /**
     * Subtracts two quaternions.
     * 
     * @param q1
     *        First Quaternion.
     * @param q2
     *        Second quaternion.
     * @return the difference between {@code q1} and {@code q2}.
     */
    public static Quaternion subtract(final Quaternion q1,
                                      final Quaternion q2) {
        return new Quaternion(q1.getQ0() - q2.getQ0(),
            q1.getQ1() - q2.getQ1(),
            q1.getQ2() - q2.getQ2(),
            q1.getQ3() - q2.getQ3());
    }

    /**
     * Subtracts a quaternion from the instance.
     * 
     * @param q
     *        Quaternion.
     * @return the difference between this instance and {@code q}.
     */
    public Quaternion subtract(final Quaternion q) {
        return subtract(this, q);
    }

    /**
     * Computes the dot-product of two quaternions.
     * 
     * @param q1
     *        Quaternion.
     * @param q2
     *        Quaternion.
     * @return the dot product of {@code q1} and {@code q2}.
     */
    public static double dotProduct(final Quaternion q1,
                                    final Quaternion q2) {
        return q1.getQ0() * q2.getQ0() +
            q1.getQ1() * q2.getQ1() +
            q1.getQ2() * q2.getQ2() +
            q1.getQ3() * q2.getQ3();
    }

    /**
     * Computes the dot-product of the instance by a quaternion.
     * 
     * @param q
     *        Quaternion.
     * @return the dot product of this instance and {@code q}.
     */
    public double dotProduct(final Quaternion q) {
        return dotProduct(this, q);
    }

    /**
     * Computes the norm of the quaternion.
     * 
     * @return the norm.
     */
    public double getNorm() {
        return MathLib.sqrt(this.q0 * this.q0 +
            this.q1 * this.q1 +
            this.q2 * this.q2 +
            this.q3 * this.q3);
    }

    /**
     * Computes the normalized quaternion (the versor of the instance).
     * The norm of the quaternion must not be zero.
     * 
     * @return a normalized quaternion.
     * @throws ZeroException
     *         if the norm of the quaternion is zero.
     */
    public Quaternion normalize() {
        final double norm = this.getNorm();

        if (norm < Precision.SAFE_MIN) {
            throw new ZeroException(PatriusMessages.NORM, norm);
        }

        return new Quaternion(this.q0 / norm,
            this.q1 / norm,
            this.q2 / norm,
            this.q3 / norm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Quaternion) {
            final Quaternion q = (Quaternion) other;
            return this.q0 == q.getQ0() &&
                this.q1 == q.getQ1() &&
                this.q2 == q.getQ2() &&
                this.q3 == q.getQ3();
        }

        return false;
    }

    // CHECKSTYLE: stop MagicNumber check
    // Reason: model - Commons-Math code

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // "Effective Java" (second edition, p. 47).
        int result = 17;
        for (final double comp : new double[] { this.q0, this.q1, this.q2, this.q3 }) {
            final int c = MathUtils.hash(comp);
            result = 31 * result + c;
        }
        return result;
    }

    // CHECKSTYLE: resume MagicNumber check

    /**
     * Checks whether this instance is equal to another quaternion
     * within a given tolerance.
     * 
     * @param q
     *        Quaternion with which to compare the current quaternion.
     * @param eps
     *        Tolerance.
     * @return {@code true} if the each of the components are equal
     *         within the allowed absolute error.
     */
    public boolean equals(final Quaternion q,
                          final double eps) {
        return Precision.equals(this.q0, q.getQ0(), eps) &&
            Precision.equals(this.q1, q.getQ1(), eps) &&
            Precision.equals(this.q2, q.getQ2(), eps) &&
            Precision.equals(this.q3, q.getQ3(), eps);
    }

    /**
     * Checks whether the instance is a unit quaternion within a given
     * tolerance.
     * 
     * @param eps
     *        Tolerance (absolute error).
     * @return {@code true} if the norm is 1 within the given tolerance, {@code false} otherwise
     */
    public boolean isUnitQuaternion(final double eps) {
        return Precision.equals(this.getNorm(), 1d, eps);
    }

    /**
     * Checks whether the instance is a pure quaternion within a given
     * tolerance.
     * 
     * @param eps
     *        Tolerance (absolute error).
     * @return {@code true} if the scalar part of the quaternion is zero.
     */
    public boolean isPureQuaternion(final double eps) {
        return MathLib.abs(this.getQ0()) <= eps;
    }

    /**
     * Returns the polar form of the quaternion.
     * 
     * @return the unit quaternion with positive scalar part.
     */
    public Quaternion getPositivePolarForm() {
        if (this.getQ0() < 0) {
            final Quaternion unitQ = this.normalize();
            // The quaternion of rotation (normalized quaternion) q and -q
            // are equivalent (i.e. represent the same rotation).
            return new Quaternion(-unitQ.getQ0(),
                -unitQ.getQ1(),
                -unitQ.getQ2(),
                -unitQ.getQ3());
        } else {
            return this.normalize();
        }
    }

    /**
     * Returns the inverse of this instance.
     * The norm of the quaternion must not be zero.
     * 
     * @return the inverse.
     * @throws ZeroException
     *         if the norm (squared) of the quaternion is zero.
     */
    public Quaternion getInverse() {
        final double squareNorm = this.q0 * this.q0 + this.q1 * this.q1 + this.q2 * this.q2 + this.q3 * this.q3;
        if (squareNorm < Precision.SAFE_MIN) {
            throw new ZeroException(PatriusMessages.NORM, squareNorm);
        }

        return new Quaternion(this.q0 / squareNorm,
            -this.q1 / squareNorm,
            -this.q2 / squareNorm,
            -this.q3 / squareNorm);
    }

    /**
     * Gets the first component of the quaternion (scalar part).
     * 
     * @return the scalar part.
     */
    public double getQ0() {
        return this.q0;
    }

    /**
     * Gets the second component of the quaternion (first component
     * of the vector part).
     * 
     * @return the first component of the vector part.
     */
    public double getQ1() {
        return this.q1;
    }

    /**
     * Gets the third component of the quaternion (second component
     * of the vector part).
     * 
     * @return the second component of the vector part.
     */
    public double getQ2() {
        return this.q2;
    }

    /**
     * Gets the fourth component of the quaternion (third component
     * of the vector part).
     * 
     * @return the third component of the vector part.
     */
    public double getQ3() {
        return this.q3;
    }

    /**
     * Gets the scalar part of the quaternion.
     * 
     * @return the scalar part.
     * @see #getQ0()
     */
    public double getScalarPart() {
        return this.getQ0();
    }

    /**
     * Gets the three components of the vector part of the quaternion.
     * 
     * @return the vector part.
     * @see #getQ1()
     * @see #getQ2()
     * @see #getQ3()
     */
    public double[] getVectorPart() {
        return new double[] { this.getQ1(), this.getQ2(), this.getQ3() };
    }

    /**
     * Multiplies the instance by a scalar.
     * 
     * @param alpha
     *        Scalar factor.
     * @return a scaled quaternion.
     */
    public Quaternion multiply(final double alpha) {
        return new Quaternion(alpha * this.q0,
            alpha * this.q1,
            alpha * this.q2,
            alpha * this.q3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String sp = " ";
        final StringBuilder s = new StringBuilder();
        s.append("[")
            .append(this.q0).append(sp)
            .append(this.q1).append(sp)
            .append(this.q2).append(sp)
            .append(this.q3)
            .append("]");

        return s.toString();
    }
}
