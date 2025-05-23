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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.complex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop ReturnCount check
//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code

/**
 * Representation of a Complex number, i.e. a number which has both a
 * real and imaginary part. <br/>
 * Implementations of arithmetic operations handle {@code NaN} and
 * infinite values according to the rules for {@link java.lang.Double}, i.e. {@link #equals} is an equivalence relation
 * for all instances that have
 * a {@code NaN} in either real or imaginary part, e.g. the following are
 * considered equal:
 * <ul>
 * <li>{@code 1 + NaNi}</li>
 * <li>{@code NaN + i}</li>
 * <li>{@code NaN + NaNi}</li>
 * </ul>
 * Note that this is in contradiction with the IEEE-754 standard for floating
 * point numbers (according to which the test {@code x == x} must fail if {@code x} is {@code NaN}). The method
 * {@link fr.cnes.sirius.patrius.math.util.Precision#equals(double,double,int)
 * equals for primitive double} in {@link fr.cnes.sirius.patrius.math.util.Precision} conforms with IEEE-754 while this
 * class conforms with the standard behavior
 * for Java object types. <br/>
 * Implements Serializable since 2.0
 * 
 * @version $Id: Complex.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Complex implements FieldElement<Complex>, Serializable {
    /** The square root of -1. A number representing "0.0 + 1.0i" */
    public static final Complex I = new Complex(0.0, 1.0);
    // CHECKSTYLE: stop ConstantName
    /** A complex number representing "NaN + NaNi" */
    @SuppressWarnings("PMD.VariableNamingConventions")
    public static final Complex NaN = new Complex(Double.NaN, Double.NaN);
    // CHECKSTYLE: resume ConstantName
    /** A complex number representing "+INF + INFi" */
    public static final Complex INF = new Complex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    /** A complex number representing "1.0 + 0.0i" */
    public static final Complex ONE = new Complex(1.0, 0.0);
    /** A complex number representing "0.0 + 0.0i" */
    public static final Complex ZERO = new Complex(0.0, 0.0);

    /** Serializable UID. */
    private static final long serialVersionUID = -6195664516687396620L;

    /** The imaginary part. */
    private final double imaginary;
    /** The real part. */
    private final double real;
    /** Record whether this complex number is equal to NaN. */
    private final transient boolean isNaNFlag;
    /** Record whether this complex number is infinite. */
    private final transient boolean isInfiniteFlag;

    /**
     * Create a complex number given only the real part.
     * 
     * @param realIn
     *        Real part.
     */
    public Complex(final double realIn) {
        this(realIn, 0.0);
    }

    /**
     * Create a complex number given the real and imaginary parts.
     * 
     * @param realIn
     *        Real part.
     * @param imaginaryIn
     *        Imaginary part.
     */
    public Complex(final double realIn, final double imaginaryIn) {
        this.real = realIn;
        this.imaginary = imaginaryIn;

        this.isNaNFlag = Double.isNaN(realIn) || Double.isNaN(imaginaryIn);
        this.isInfiniteFlag = !this.isNaNFlag &&
            (Double.isInfinite(realIn) || Double.isInfinite(imaginaryIn));
    }

    /**
     * Return the absolute value of this complex number.
     * Returns {@code NaN} if either real or imaginary part is {@code NaN} and {@code Double.POSITIVE_INFINITY} if
     * neither part is {@code NaN},
     * but at least one part is infinite.
     * 
     * @return the absolute value.
     */
    public double abs() {
        if (this.isNaNFlag) {
            return Double.NaN;
        }
        if (this.isInfinite()) {
            return Double.POSITIVE_INFINITY;
        }
        if (MathLib.abs(this.real) < MathLib.abs(this.imaginary)) {
            if (this.imaginary == 0.0) {
                return MathLib.abs(this.real);
            }
            final double q = this.real / this.imaginary;
            return MathLib.abs(this.imaginary) * MathLib.sqrt(1 + q * q);
        }

        if (this.real == 0.0) {
            return MathLib.abs(this.imaginary);
        }
        final double q = this.imaginary / this.real;
        return MathLib.abs(this.real) * MathLib.sqrt(1 + q * q);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this + addend)}.
     * Uses the definitional formula
     * 
     * <pre>
     *  <code>
     *   (a + bi) + (c + di) = (a+c) + (b+d)i
     *  </code>
     * </pre>
     * 
     * <br/>
     * If either {@code this} or {@code addend} has a {@code NaN} value in
     * either part, {@link #NaN} is returned; otherwise {@code Infinite} and {@code NaN} values are returned in the
     * parts of the result
     * according to the rules for {@link java.lang.Double} arithmetic.
     * 
     * @param addend
     *        Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @throws NullArgumentException
     *         if {@code addend} is {@code null}.
     */
    @Override
    public Complex add(final Complex addend) {
        MathUtils.checkNotNull(addend);
        if (this.isNaNFlag || addend.isNaNFlag) {
            return NaN;
        }

        return createComplex(this.real + addend.getReal(),
            this.imaginary + addend.getImaginary());
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this + addend)},
     * with {@code addend} interpreted as a real number.
     * 
     * @param addend
     *        Value to be added to this {@code Complex}.
     * @return {@code this + addend}.
     * @see #add(Complex)
     */
    public Complex add(final double addend) {
        if (this.isNaNFlag || Double.isNaN(addend)) {
            return NaN;
        }

        return createComplex(this.real + addend, this.imaginary);
    }

    /**
     * Return the conjugate of this complex number.
     * The conjugate of {@code a + bi} is {@code a - bi}. <br/>
     * {@link #NaN} is returned if either the real or imaginary
     * part of this Complex number equals {@code Double.NaN}. <br/>
     * If the imaginary part is infinite, and the real part is not {@code NaN}, the returned value has infinite
     * imaginary part
     * of the opposite sign, e.g. the conjugate of {@code 1 + POSITIVE_INFINITY i} is {@code 1 - NEGATIVE_INFINITY i}.
     * 
     * @return the conjugate of this Complex object.
     */
    public Complex conjugate() {
        if (this.isNaNFlag) {
            return NaN;
        }

        return createComplex(this.real, -this.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)}.
     * Implements the definitional formula
     * 
     * <pre>
     *  <code>
     *    a + bi          ac + bd + (bc - ad)i
     *    ----------- = -------------------------
     *    c + di         c<sup>2</sup> + d<sup>2</sup>
     *  </code>
     * </pre>
     * 
     * but uses
     * <a href="http://doi.acm.org/10.1145/1039813.1039814">
     * prescaling of operands</a> to limit the effects of overflows and
     * underflows in the computation. <br/>
     * {@code Infinite} and {@code NaN} values are handled according to the
     * following rules, applied in the order presented:
     * <ul>
     * <li>If either {@code this} or {@code divisor} has a {@code NaN} value in either part,
     *  {@link #NaN} is returned.</li>
     * <li>If {@code divisor} equals {@link #ZERO}, {@link #NaN} is returned.</li>
     * <li>If {@code this} and {@code divisor} are both infinite, {@link #NaN} is returned.</li>
     * <li>If {@code this} is finite (i.e., has no {@code Infinite} or {@code NaN} parts) and {@code divisor} is
     * infinite (one or both parts infinite), {@link #ZERO} is returned.</li>
     * <li>If {@code this} is infinite and {@code divisor} is finite, {@code NaN} values are returned in the parts of
     * the result if the {@link java.lang.Double} rules applied to the definitional formula force {@code NaN}
     *  results.</li>
     * </ul>
     * 
     * @param divisor
     *        Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     * @throws NullArgumentException
     *         if {@code divisor} is {@code null}.
     */
    @Override
    public Complex divide(final Complex divisor) {
        MathUtils.checkNotNull(divisor);
        if (this.isNaNFlag || divisor.isNaNFlag) {
            return NaN;
        }

        final double c = divisor.getReal();
        final double d = divisor.getImaginary();
        if (c == 0.0 && d == 0.0) {
            return NaN;
        }

        if (divisor.isInfinite() && !this.isInfinite()) {
            return ZERO;
        }

        if (MathLib.abs(c) < MathLib.abs(d)) {
            final double q = c / d;
            final double denominator = c * q + d;
            return createComplex((this.real * q + this.imaginary) / denominator,
                (this.imaginary * q - this.real) / denominator);
        }
        final double q = d / c;
        final double denominator = d * q + c;
        return createComplex((this.imaginary * q + this.real) / denominator,
            (this.imaginary - this.real * q) / denominator);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this / divisor)},
     * with {@code divisor} interpreted as a real number.
     * 
     * @param divisor
     *        Value by which this {@code Complex} is to be divided.
     * @return {@code this / divisor}.
     * @see #divide(Complex)
     */
    public Complex divide(final double divisor) {
        if (this.isNaNFlag || Double.isNaN(divisor)) {
            return NaN;
        }
        if (divisor == 0d) {
            return NaN;
        }
        if (Double.isInfinite(divisor)) {
            return this.isInfinite() ? NaN : ZERO;
        }
        return createComplex(this.real / divisor,
            this.imaginary / divisor);
    }

    /** {@inheritDoc} */
    @Override
    public Complex reciprocal() {
        if (this.isNaNFlag) {
            return NaN;
        }

        if (this.real == 0.0 && this.imaginary == 0.0) {
            return NaN;
        }

        if (this.isInfiniteFlag) {
            return ZERO;
        }

        if (MathLib.abs(this.real) < MathLib.abs(this.imaginary)) {
            final double q = this.real / this.imaginary;
            final double scale = 1. / (this.real * q + this.imaginary);
            return createComplex(scale * q, -scale);
        }
        final double q = this.imaginary / this.real;
        final double scale = 1. / (this.imaginary * q + this.real);
        return createComplex(scale, -scale * q);
    }

    /**
     * Test for the equality of two Complex objects.
     * If both the real and imaginary parts of two complex numbers
     * are exactly the same, and neither is {@code Double.NaN}, the two
     * Complex objects are considered to be equal.
     * All {@code NaN} values are considered to be equal - i.e, if either
     * (or both) real and imaginary parts of the complex number are equal
     * to {@code Double.NaN}, the complex number is equal to {@code NaN}.
     * 
     * @param other
     *        Object to test for equality to this
     * @return true if two Complex objects are equal, false if object is {@code null}, not an instance of Complex, or
     *         not equal to this Complex
     *         instance.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Complex) {
            final Complex c = (Complex) other;
            if (c.isNaNFlag) {
                return this.isNaNFlag;
            }
            return (this.real == c.real) && (this.imaginary == c.imaginary);
        }
        return false;
    }

    /**
     * Get a hashCode for the complex number.
     * Any {@code Double.NaN} value in real or imaginary part produces
     * the same hash code {@code 7}.
     * 
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        if (this.isNaNFlag) {
            return 7;
        }
        return 37 * (17 * MathUtils.hash(this.imaginary) + MathUtils.hash(this.real));
    }

    /**
     * Access the imaginary part.
     * 
     * @return the imaginary part.
     */
    public double getImaginary() {
        return this.imaginary;
    }

    /**
     * Access the real part.
     * 
     * @return the real part.
     */
    public double getReal() {
        return this.real;
    }

    /**
     * Checks whether either or both parts of this complex number is {@code NaN}.
     * 
     * @return true if either or both parts of this complex number is {@code NaN}; false otherwise.
     */
    public boolean isNaN() {
        return this.isNaNFlag;
    }

    /**
     * Checks whether either the real or imaginary part of this complex number
     * takes an infinite value (either {@code Double.POSITIVE_INFINITY} or {@code Double.NEGATIVE_INFINITY}) and neither
     * part
     * is {@code NaN}.
     * 
     * @return true if one or both parts of this complex number are infinite
     *         and neither part is {@code NaN}.
     */
    public boolean isInfinite() {
        return this.isInfiniteFlag;
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}.
     * Implements preliminary checks for {@code NaN} and infinity followed by
     * the definitional formula:
     * 
     * <pre>
     *  <code>
     *   (a + bi)(c + di) = (ac - bd) + (ad + bc)i
     *  </code>
     * </pre>
     * 
     * Returns {@link #NaN} if either {@code this} or {@code factor} has one or
     * more {@code NaN} parts. <br/>
     * Returns {@link #INF} if neither {@code this} nor {@code factor} has one
     * or more {@code NaN} parts and if either {@code this} or {@code factor} has one or more infinite parts (same
     * result is returned regardless of
     * the sign of the components). <br/>
     * Returns finite values in components of the result per the definitional
     * formula in all remaining cases.
     * 
     * @param factor
     *        value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @throws NullArgumentException
     *         if {@code factor} is {@code null}.
     */
    @Override
    public Complex multiply(final Complex factor) {

        final Complex res;

        MathUtils.checkNotNull(factor);
        if (this.isNaNFlag || factor.isNaNFlag) {
            res = NaN;
        } else if (Double.isInfinite(this.real) ||
            Double.isInfinite(this.imaginary) ||
            Double.isInfinite(factor.real) ||
            Double.isInfinite(factor.imaginary)) {
            // we don't use isInfinite() to avoid testing for NaN again
            res = INF;
        } else {
            res = createComplex(this.real * factor.real - this.imaginary * factor.imaginary,
                this.real * factor.imaginary + this.imaginary * factor.real);
        }
        return res;
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor} interpreted as a integer
     * number.
     * 
     * @param factor
     *        value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    @Override
    public Complex multiply(final int factor) {
        final Complex res;
        if (this.isNaNFlag) {
            res = NaN;
        } else if (Double.isInfinite(this.real) ||
            Double.isInfinite(this.imaginary)) {
            res = INF;
        } else {
            res = createComplex(this.real * factor, this.imaginary * factor);
        }
        return res;
    }

    /**
     * Returns a {@code Complex} whose value is {@code this * factor}, with {@code factor} interpreted as a real number.
     * 
     * @param factor
     *        value to be multiplied by this {@code Complex}.
     * @return {@code this * factor}.
     * @see #multiply(Complex)
     */
    public Complex multiply(final double factor) {
        final Complex res;
        if (this.isNaNFlag || Double.isNaN(factor)) {
            res = NaN;
        } else if (Double.isInfinite(this.real) ||
            Double.isInfinite(this.imaginary) ||
            Double.isInfinite(factor)) {
            // we don't use isInfinite() to avoid testing for NaN again
            res = INF;
        } else {
            res = createComplex(this.real * factor, this.imaginary * factor);
        }
        return res;
    }

    /**
     * Returns a {@code Complex} whose value is {@code (-this)}.
     * Returns {@code NaN} if either real or imaginary
     * part of this Complex number equals {@code Double.NaN}.
     * 
     * @return {@code -this}.
     */
    @Override
    public Complex negate() {
        if (this.isNaNFlag) {
            return NaN;
        }

        return createComplex(-this.real, -this.imaginary);
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)}.
     * Uses the definitional formula
     * 
     * <pre>
     *  <code>
     *   (a + bi) - (c + di) = (a-c) + (b-d)i
     *  </code>
     * </pre>
     * 
     * If either {@code this} or {@code subtrahend} has a {@code NaN]} value in either part, {@link #NaN} is returned;
     * otherwise infinite and {@code NaN} values are
     * returned in the parts of the result according to the rules for {@link java.lang.Double} arithmetic.
     * 
     * @param subtrahend
     *        value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     * @throws NullArgumentException
     *         if {@code subtrahend} is {@code null}.
     */
    @Override
    public Complex subtract(final Complex subtrahend) {
        MathUtils.checkNotNull(subtrahend);
        if (this.isNaNFlag || subtrahend.isNaNFlag) {
            return NaN;
        }

        return createComplex(this.real - subtrahend.getReal(),
            this.imaginary - subtrahend.getImaginary());
    }

    /**
     * Returns a {@code Complex} whose value is {@code (this - subtrahend)}.
     * 
     * @param subtrahend
     *        value to be subtracted from this {@code Complex}.
     * @return {@code this - subtrahend}.
     * @see #subtract(Complex)
     */
    public Complex subtract(final double subtrahend) {
        if (this.isNaNFlag || Double.isNaN(subtrahend)) {
            return NaN;
        }
        return createComplex(this.real - subtrahend, this.imaginary);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseCosine.html" TARGET="_top">
     * inverse cosine</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   acos(z) = -i (log(z + i (sqrt(1 - z<sup>2</sup>))))
     *  </code>
     * </pre>
     * 
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN} or infinite.
     * 
     * @return the inverse cosine of this complex number.
     * @since 1.2
     */
    public Complex acos() {
        if (this.isNaNFlag) {
            return NaN;
        }

        return this.add(this.sqrt1z().multiply(I)).log().multiply(I.negate());
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseSine.html" TARGET="_top">
     * inverse sine</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   asin(z) = -i (log(sqrt(1 - z<sup>2</sup>) + iz))
     *  </code>
     * </pre>
     * 
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN} or infinite.
     * 
     * @return the inverse sine of this complex number.
     * @since 1.2
     */
    public Complex asin() {
        if (this.isNaNFlag) {
            return NaN;
        }

        return this.sqrt1z().add(this.multiply(I)).log().multiply(I.negate());
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/InverseTangent.html" TARGET="_top">
     * inverse tangent</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   atan(z) = (i/2) log((i + z)/(i - z))
     *  </code>
     * </pre>
     * 
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN} or infinite.
     * 
     * @return the inverse tangent of this complex number
     * @since 1.2
     */
    public Complex atan() {
        if (this.isNaNFlag) {
            return NaN;
        }

        return this.add(I).divide(I.subtract(this)).log()
            .multiply(I.divide(createComplex(2.0, 0.0)));
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Cosine.html" TARGET="_top">
     * cosine</a>
     * of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   cos(a + bi) = cos(a)cosh(b) - sin(a)sinh(b)i
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link java.lang.Math#sin}, {@link java.lang.Math#cos},
     * {@link MathLib#cosh} and {@link MathLib#sinh}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   cos(1 &plusmn; INFINITY i) = 1 &#x2213; INFINITY i
     *   cos(&plusmn;INFINITY + i) = NaN + NaN i
     *   cos(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *  </code>
     * </pre>
     * 
     * @return the cosine of this complex number.
     * @since 1.2
     */
    public Complex cos() {
        if (this.isNaNFlag) {
            return NaN;
        }

        final double[] sincos = MathLib.sinAndCos(this.real);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double[] sinhcosh = MathLib.sinhAndCosh(this.imaginary);
        final double sinh = sinhcosh[0];
        final double cosh = sinhcosh[1];
        return createComplex(cos * cosh, -sin * sinh);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicCosine.html" TARGET="_top">
     * hyperbolic cosine</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   cosh(a + bi) = cosh(a)cos(b) + sinh(a)sin(b)i}
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link java.lang.Math#sin}, {@link java.lang.Math#cos},
     * {@link MathLib#cosh} and {@link MathLib#sinh}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   cosh(1 &plusmn; INFINITY i) = NaN + NaN i
     *   cosh(&plusmn;INFINITY + i) = INFINITY &plusmn; INFINITY i
     *   cosh(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *  </code>
     * </pre>
     * 
     * @return the hyperbolic cosine of this complex number.
     * @since 1.2
     */
    public Complex cosh() {
        if (this.isNaNFlag) {
            return NaN;
        }

        final double[] sincos = MathLib.sinAndCos(this.imaginary);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double[] sinhcosh = MathLib.sinhAndCosh(this.real);
        final double sinh = sinhcosh[0];
        final double cosh = sinhcosh[1];
        return createComplex(cosh * cos, sinh * sin);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/ExponentialFunction.html" TARGET="_top">
     * exponential function</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   exp(a + bi) = exp(a)cos(b) + exp(a)sin(b)i
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link java.lang.Math#exp}, {@link java.lang.Math#cos}, and
     * {@link java.lang.Math#sin}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   exp(1 &plusmn; INFINITY i) = NaN + NaN i
     *   exp(INFINITY + i) = INFINITY + INFINITY i
     *   exp(-INFINITY + i) = 0 + 0i
     *   exp(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *  </code>
     * </pre>
     * 
     * @return <code><i>e</i><sup>this</sup></code>.
     * @since 1.2
     */
    public Complex exp() {
        if (this.isNaNFlag) {
            return NaN;
        }

        final double[] sincos = MathLib.sinAndCos(this.imaginary);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double expReal = MathLib.exp(this.real);
        return createComplex(expReal * cos, expReal * sin);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/NaturalLogarithm.html" TARGET="_top">
     * natural logarithm</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   log(a + bi) = ln(|a + bi|) + arg(a + bi)i
     *  </code>
     * </pre>
     * 
     * where ln on the right hand side is {@link java.lang.Math#log}, {@code |a + bi|} is the modulus,
     * {@link Complex#abs}, and {@code arg(a + bi) = }{@link java.lang.Math#atan2}(b, a). <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite (or critical) values in real or imaginary parts of the input may
     * result in infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   log(1 &plusmn; INFINITY i) = INFINITY &plusmn; (&pi;/2)i
     *   log(INFINITY + i) = INFINITY + 0i
     *   log(-INFINITY + i) = INFINITY + &pi;i
     *   log(INFINITY &plusmn; INFINITY i) = INFINITY &plusmn; (&pi;/4)i
     *   log(-INFINITY &plusmn; INFINITY i) = INFINITY &plusmn; (3&pi;/4)i
     *   log(0 + 0i) = -INFINITY + 0i
     *  </code>
     * </pre>
     * 
     * @return the value <code>ln &nbsp; this</code>, the natural logarithm
     *         of {@code this}.
     * @since 1.2
     */
    public Complex log() {
        if (this.isNaNFlag) {
            return NaN;
        }

        return createComplex(MathLib.log(this.abs()),
            MathLib.atan2(this.imaginary, this.real));
    }

    /**
     * Returns of value of this complex number raised to the power of {@code x}.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   y<sup>x</sup> = exp(x&middot;log(y))
     *  </code>
     * </pre>
     * 
     * where {@code exp} and {@code log} are {@link #exp} and {@link #log}, respectively. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN} or infinite, or if {@code y} equals {@link Complex#ZERO}.
     * 
     * @param x
     *        exponent to which this {@code Complex} is to be raised.
     * @return <code> this<sup>x</sup></code>.
     * @throws NullArgumentException
     *         if x is {@code null}.
     * @since 1.2
     */
    public Complex pow(final Complex x) {
        MathUtils.checkNotNull(x);
        return this.log().multiply(x).exp();
    }

    /**
     * Returns of value of this complex number raised to the power of {@code x}.
     * 
     * @param x
     *        exponent to which this {@code Complex} is to be raised.
     * @return <code>this<sup>x</sup></code>.
     * @see #pow(Complex)
     */
    public Complex pow(final double x) {
        return this.log().multiply(x).exp();
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Sine.html" TARGET="_top">
     * sine</a>
     * of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   sin(a + bi) = sin(a)cosh(b) - cos(a)sinh(b)i
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link java.lang.Math#sin}, {@link java.lang.Math#cos},
     * {@link MathLib#cosh} and {@link MathLib#sinh}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or {@code NaN} values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   sin(1 &plusmn; INFINITY i) = 1 &plusmn; INFINITY i
     *   sin(&plusmn;INFINITY + i) = NaN + NaN i
     *   sin(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *  </code>
     * </pre>
     * 
     * @return the sine of this complex number.
     * @since 1.2
     */
    public Complex sin() {
        if (this.isNaNFlag) {
            return NaN;
        }

        final double[] sincos = MathLib.sinAndCos(this.real);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double[] sinhcosh = MathLib.sinhAndCosh(this.imaginary);
        final double sinh = sinhcosh[0];
        final double cosh = sinhcosh[1];
        return createComplex(sin * cosh, cos * sinh);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicSine.html" TARGET="_top">
     * hyperbolic sine</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   sinh(a + bi) = sinh(a)cos(b)) + cosh(a)sin(b)i
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link java.lang.Math#sin}, {@link java.lang.Math#cos},
     * {@link MathLib#cosh} and {@link MathLib#sinh}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   sinh(1 &plusmn; INFINITY i) = NaN + NaN i
     *   sinh(&plusmn;INFINITY + i) = &plusmn; INFINITY + INFINITY i
     *   sinh(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *  </code>
     * </pre>
     * 
     * @return the hyperbolic sine of {@code this}.
     * @since 1.2
     */
    public Complex sinh() {
        if (this.isNaNFlag) {
            return NaN;
        }

        final double[] sincos = MathLib.sinAndCos(this.imaginary);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double[] sinhcosh = MathLib.sinhAndCosh(this.real);
        final double sinh = sinhcosh[0];
        final double cosh = sinhcosh[1];
        return createComplex(sinh * cos, cosh * sin);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/SquareRoot.html" TARGET="_top">
     * square root</a> of this complex number.
     * Implements the following algorithm to compute {@code sqrt(a + bi)}:
     * <ol>
     * <li>Let {@code t = sqrt((|a| + |a + bi|) / 2)}</li>
     * <li>
     * 
     * <pre>
     * if {@code  a &#8805; 0} return {@code t + (b/2t)i}
     *  else return {@code |b|/2t + sign(b)t i }
     * </pre>
     * 
     * </li>
     * </ol>
     * where
     * <ul>
     * <li>{@code |a| = }{@link Math#abs}(a)</li>
     * <li>{@code |a + bi| = }{@link Complex#abs}(a + bi)</li>
     * <li>{@code sign(b) =  }{@link MathLib#copySign(double,double) copySign(1d, b)}
     * </ul>
     * <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   sqrt(1 &plusmn; INFINITY i) = INFINITY + NaN i
     *   sqrt(INFINITY + i) = INFINITY + 0i
     *   sqrt(-INFINITY + i) = 0 + INFINITY i
     *   sqrt(INFINITY &plusmn; INFINITY i) = INFINITY + NaN i
     *   sqrt(-INFINITY &plusmn; INFINITY i) = NaN &plusmn; INFINITY i
     *  </code>
     * </pre>
     * 
     * @return the square root of {@code this}.
     * @since 1.2
     */
    public Complex sqrt() {
        if (this.isNaNFlag) {
            return NaN;
        }

        if (this.real == 0.0 && this.imaginary == 0.0) {
            return createComplex(0.0, 0.0);
        }

        final double t = MathLib.sqrt((MathLib.abs(this.real) + this.abs()) / 2.0);
        if (this.real >= 0.0) {
            return createComplex(t, this.imaginary / (2.0 * t));
        }
        return createComplex(MathLib.abs(this.imaginary) / (2.0 * t),
            MathLib.copySign(1d, this.imaginary) * t);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/SquareRoot.html" TARGET="_top">
     * square root</a> of <code>1 - this<sup>2</sup></code> for this complex
     * number.
     * Computes the result directly as {@code sqrt(ONE.subtract(z.multiply(z)))}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * @return the square root of <code>1 - this<sup>2</sup></code>.
     * @since 1.2
     */
    public Complex sqrt1z() {
        return createComplex(1.0, 0.0).subtract(this.multiply(this)).sqrt();
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/Tangent.html" TARGET="_top">
     * tangent</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   tan(a + bi) = sin(2a)/(cos(2a)+cosh(2b)) + [sinh(2b)/(cos(2a)+cosh(2b))]i
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link MathLib#sin}, {@link MathLib#cos},
     * {@link MathLib#cosh} and {@link MathLib#sinh}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite (or critical) values in real or imaginary parts of the input may
     * result in infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   tan(a &plusmn; INFINITY i) = 0 &plusmn; i
     *   tan(&plusmn;INFINITY + bi) = NaN + NaN i
     *   tan(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *   tan(&plusmn;&pi;/2 + 0 i) = &plusmn;INFINITY + NaN i
     *  </code>
     * </pre>
     * 
     * @return the tangent of {@code this}.
     * @since 1.2
     */
    public Complex tan() {
        if (this.isNaNFlag || Double.isInfinite(this.real)) {
            return NaN;
        }
        if (this.imaginary > 20.0) {
            return createComplex(0.0, 1.0);
        }
        if (this.imaginary < -20.0) {
            return createComplex(0.0, -1.0);
        }

        final double real2 = 2.0 * this.real;
        final double imaginary2 = 2.0 * this.imaginary;
        final double[] sincos = MathLib.sinAndCos(real2);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double[] sinhcosh = MathLib.sinhAndCosh(imaginary2);
        final double sinh = sinhcosh[0];
        final double cosh = sinhcosh[1];
        final double d = cos + cosh;

        return createComplex(sin / d, sinh / d);
    }

    /**
     * Compute the
     * <a href="http://mathworld.wolfram.com/HyperbolicTangent.html" TARGET="_top">
     * hyperbolic tangent</a> of this complex number.
     * Implements the formula:
     * 
     * <pre>
     *  <code>
     *   tan(a + bi) = sinh(2a)/(cosh(2a)+cos(2b)) + [sin(2b)/(cosh(2a)+cos(2b))]i
     *  </code>
     * </pre>
     * 
     * where the (real) functions on the right-hand side are {@link MathLib#sin}, {@link MathLib#cos},
     * {@link MathLib#cosh} and {@link MathLib#sinh}. <br/>
     * Returns {@link Complex#NaN} if either real or imaginary part of the
     * input argument is {@code NaN}. <br/>
     * Infinite values in real or imaginary parts of the input may result in
     * infinite or NaN values returned in parts of the result.
     * 
     * <pre>
     *  Examples:
     *  <code>
     *   tanh(a &plusmn; INFINITY i) = NaN + NaN i
     *   tanh(&plusmn;INFINITY + bi) = &plusmn;1 + 0 i
     *   tanh(&plusmn;INFINITY &plusmn; INFINITY i) = NaN + NaN i
     *   tanh(0 + (&pi;/2)i) = NaN + INFINITY i
     *  </code>
     * </pre>
     * 
     * @return the hyperbolic tangent of {@code this}.
     * @since 1.2
     */
    public Complex tanh() {
        if (this.isNaNFlag || Double.isInfinite(this.imaginary)) {
            return NaN;
        }
        if (this.real > 20.0) {
            return createComplex(1.0, 0.0);
        }
        if (this.real < -20.0) {
            return createComplex(-1.0, 0.0);
        }
        final double real2 = 2.0 * this.real;
        final double imaginary2 = 2.0 * this.imaginary;
        final double[] sincos = MathLib.sinAndCos(imaginary2);
        final double sin = sincos[0];
        final double cos = sincos[1];
        final double[] sinhcosh = MathLib.sinhAndCosh(real2);
        final double sinh = sinhcosh[0];
        final double cosh = sinhcosh[1];
        final double d = cosh + cos;

        return createComplex(sinh / d, sin / d);
    }

    /**
     * Compute the argument of this complex number.
     * The argument is the angle phi between the positive real axis and
     * the point representing this number in the complex plane.
     * The value returned is between -PI (not inclusive)
     * and PI (inclusive), with negative values returned for numbers with
     * negative imaginary parts. <br/>
     * If either real or imaginary part (or both) is NaN, NaN is returned.
     * Infinite parts are handled as {@code Math.atan2} handles them,
     * essentially treating finite parts as zero in the presence of an
     * infinite coordinate and returning a multiple of pi/4 depending on
     * the signs of the infinite parts.
     * See the javadoc for {@code Math.atan2} for full details.
     * 
     * @return the argument of {@code this}.
     */
    public double getArgument() {
        return MathLib.atan2(this.getImaginary(), this.getReal());
    }

    /**
     * Computes the n-th roots of this complex number.
     * The nth roots are defined by the formula:
     * 
     * <pre>
     *  <code>
     *   z<sub>k</sub> = abs<sup>1/n</sup> (cos(phi + 2&pi;k/n) + i (sin(phi + 2&pi;k/n))
     *  </code>
     * </pre>
     * 
     * for <i>{@code k=0, 1, ..., n-1}</i>, where {@code abs} and {@code phi} are respectively the {@link #abs()
     * modulus} and {@link #getArgument() argument} of this complex number. <br/>
     * If one or both parts of this complex number is NaN, a list with just
     * one element, {@link #NaN} is returned.
     * if neither part is NaN, but at least one part is infinite, the result
     * is a one-element list containing {@link #INF}.
     * 
     * @param n
     *        Degree of root.
     * @return a List<Complex> of all {@code n}-th roots of {@code this}.
     * @throws NotPositiveException
     *         if {@code n <= 0}.
     * @since 2.0
     */
    public List<Complex> nthRoot(final int n) {

        if (n <= 0) {
            throw new NotPositiveException(PatriusMessages.CANNOT_COMPUTE_NTH_ROOT_FOR_NEGATIVE_N,
                n);
        }

        final List<Complex> result = new ArrayList<>();

        if (this.isNaNFlag) {
            result.add(NaN);
            return result;
        }
        if (this.isInfinite()) {
            result.add(INF);
            return result;
        }

        // nth root of abs -- faster / more accurate to use a solver here?
        final double nthRootOfAbs = MathLib.pow(this.abs(), 1.0 / n);

        // Compute nth roots of complex number with k = 0, 1, ... n-1
        final double nthPhi = this.getArgument() / n;
        final double slice = 2 * FastMath.PI / n;
        double innerPart = nthPhi;
        for (int k = 0; k < n; k++) {
            // inner part
            final double[] sincos = MathLib.sinAndCos(innerPart);
            final double sin = sincos[0];
            final double cos = sincos[1];
            final double realPart = nthRootOfAbs * cos;
            final double imaginaryPart = nthRootOfAbs * sin;
            result.add(createComplex(realPart, imaginaryPart));
            innerPart += slice;
        }

        return result;
    }

    /**
     * Create a complex number given the real and imaginary parts.
     * 
     * @param realPart
     *        Real part.
     * @param imaginaryPart
     *        Imaginary part.
     * @return a new complex number instance.
     * @since 1.2
     * @see #valueOf(double, double)
     */
    protected Complex createComplex(final double realPart,
                                    final double imaginaryPart) {
        return new Complex(realPart, imaginaryPart);
    }

    /**
     * Create a complex number given the real and imaginary parts.
     * 
     * @param realPart
     *        Real part.
     * @param imaginaryPart
     *        Imaginary part.
     * @return a Complex instance.
     */
    public static Complex valueOf(final double realPart,
                                  final double imaginaryPart) {
        if (Double.isNaN(realPart) ||
            Double.isNaN(imaginaryPart)) {
            return NaN;
        }
        return new Complex(realPart, imaginaryPart);
    }

    /**
     * Create a complex number given only the real part.
     * 
     * @param realPart
     *        Real part.
     * @return a Complex instance.
     */
    public static Complex valueOf(final double realPart) {
        if (Double.isNaN(realPart)) {
            return NaN;
        }
        return new Complex(realPart);
    }

    /**
     * Resolve the transient fields in a deserialized Complex Object.
     * Subclasses will need to override {@link #createComplex} to
     * deserialize properly.
     * 
     * @return A Complex instance with all fields resolved.
     * @since 2.0
     */
    protected final Object readResolve() {
        return createComplex(this.real, this.imaginary);
    }

    /** {@inheritDoc} */
    @Override
    public ComplexField getField() {
        return ComplexField.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "(" + this.real + ", " + this.imaginary + ")";
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check
    // CHECKSTYLE: resume ReturnCount check
}
