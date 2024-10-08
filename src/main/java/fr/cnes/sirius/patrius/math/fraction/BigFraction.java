/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fraction;

import java.math.BigDecimal;
import java.math.BigInteger;

import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Commons-Math code

/**
 * Representation of a rational number without any overflow. This class is
 * immutable.
 * 
 * @version $Id: BigFraction.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class BigFraction extends Number implements FieldElement<BigFraction>, Comparable<BigFraction> {

    /** A fraction representing "2 / 1". */
    public static final BigFraction TWO = new BigFraction(2);

    /** A fraction representing "1". */
    public static final BigFraction ONE = new BigFraction(1);

    /** A fraction representing "0". */
    public static final BigFraction ZERO = new BigFraction(0);

    /** A fraction representing "-1 / 1". */
    public static final BigFraction MINUS_ONE = new BigFraction(-1);

    /** A fraction representing "4/5". */
    public static final BigFraction FOUR_FIFTHS = new BigFraction(4, 5);

    /** A fraction representing "1/5". */
    public static final BigFraction ONE_FIFTH = new BigFraction(1, 5);

    /** A fraction representing "1/2". */
    public static final BigFraction ONE_HALF = new BigFraction(1, 2);

    /** A fraction representing "1/4". */
    public static final BigFraction ONE_QUARTER = new BigFraction(1, 4);

    /** A fraction representing "1/3". */
    public static final BigFraction ONE_THIRD = new BigFraction(1, 3);

    /** A fraction representing "3/5". */
    public static final BigFraction THREE_FIFTHS = new BigFraction(3, 5);

    /** A fraction representing "3/4". */
    public static final BigFraction THREE_QUARTERS = new BigFraction(3, 4);

    /** A fraction representing "2/5". */
    public static final BigFraction TWO_FIFTHS = new BigFraction(2, 5);

    /** A fraction representing "2/4". */
    public static final BigFraction TWO_QUARTERS = new BigFraction(2, 4);

    /** A fraction representing "2/3". */
    public static final BigFraction TWO_THIRDS = new BigFraction(2, 3);

     /** Serializable UID. */
    private static final long serialVersionUID = -5630213147331578515L;

    /** <code>BigInteger</code> representation of 100. */
    private static final BigInteger ONE_HUNDRED = BigInteger.valueOf(100);

    /** The numerator. */
    private final BigInteger numerator;

    /** The denominator. */
    private final BigInteger denominator;

    /**
     * <p>
     * Create a {@link BigFraction} equivalent to the passed <tt>BigInteger</tt>, ie "num / 1".
     * </p>
     * 
     * @param num
     *        the numerator.
     */
    public BigFraction(final BigInteger num) {
        this(num, BigInteger.ONE);
    }

    /**
     * Create a {@link BigFraction} given the numerator and denominator as {@code BigInteger}. The {@link BigFraction}
     * is reduced to lowest terms.
     * 
     * @param numIn
     *        the numerator, must not be {@code null}.
     * @param denIn
     *        the denominator, must not be {@code null}.
     * @throws ZeroException
     *         if the denominator is zero.
     * @throws NullArgumentException
     *         if either of the arguments is null
     */
    public BigFraction(final BigInteger numIn, final BigInteger denIn) {
        super();
        BigInteger num = numIn;
        BigInteger den = denIn;
        MathUtils.checkNotNull(num, PatriusMessages.NUMERATOR);
        MathUtils.checkNotNull(den, PatriusMessages.DENOMINATOR);
        if (BigInteger.ZERO.equals(den)) {
            throw new ZeroException(PatriusMessages.ZERO_DENOMINATOR);
        }
        if (BigInteger.ZERO.equals(num)) {
            this.numerator = BigInteger.ZERO;
            this.denominator = BigInteger.ONE;
        } else {

            // reduce numerator and denominator by greatest common denominator
            final BigInteger gcd = num.gcd(den);
            if (BigInteger.ONE.compareTo(gcd) < 0) {
                num = num.divide(gcd);
                den = den.divide(gcd);
            }

            // move sign to numerator
            if (BigInteger.ZERO.compareTo(den) > 0) {
                num = num.negate();
                den = den.negate();
            }

            // store the values in the final fields
            this.numerator = num;
            this.denominator = den;

        }
    }

    /**
     * Create a fraction given the double value.
     * <p>
     * This constructor behaves <em>differently</em> from {@link #BigFraction(double, double, int)}. It converts the
     * double value exactly, considering its internal bits representation. This works for all values except NaN and
     * infinities and does not requires any loop or convergence threshold.
     * </p>
     * <p>
     * Since this conversion is exact and since double numbers are sometimes approximated, the fraction created may seem
     * strange in some cases. For example, calling <code>new BigFraction(1.0 / 3.0)</code> does <em>not</em> create the
     * fraction 1/3, but the fraction 6004799503160661 / 18014398509481984 because the double number passed to the
     * constructor is not exactly 1/3 (this number cannot be stored exactly in IEEE754).
     * </p>
     * 
     * @see #BigFraction(double, double, int)
     * @param value
     *        the double value to convert to a fraction.
     * @exception MathIllegalArgumentException
     *            if value is NaN or infinite
     */
    public BigFraction(final double value) {
        super();
        if (Double.isNaN(value)) {
            throw new MathIllegalArgumentException(PatriusMessages.NAN_VALUE_CONVERSION);
        }
        if (Double.isInfinite(value)) {
            throw new MathIllegalArgumentException(PatriusMessages.INFINITE_VALUE_CONVERSION);
        }

        // compute m and k such that value = m * 2^k
        final long bits = Double.doubleToLongBits(value);
        final long sign = bits & 0x8000000000000000L;
        final long exponent = bits & 0x7ff0000000000000L;
        long m = bits & 0x000fffffffffffffL;
        if (exponent != 0) {
            // this was a normalized number, add the implicit most significant bit
            m |= 0x0010000000000000L;
        }
        if (sign != 0) {
            m = -m;
        }
        int k = ((int) (exponent >> 52)) - 1075;
        while (((m & 0x001ffffffffffffeL) != 0) && ((m & 0x1) == 0)) {
            m = m >> 1;
            ++k;
        }

        if (k < 0) {
            this.numerator = BigInteger.valueOf(m);
            this.denominator = BigInteger.ZERO.flipBit(-k);
        } else {
            this.numerator = BigInteger.valueOf(m).multiply(BigInteger.ZERO.flipBit(k));
            this.denominator = BigInteger.ONE;
        }

    }

    /**
     * Create a fraction given the double value and maximum error allowed.
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html"> Continued Fraction</a> equations (11) and
     * (22)-(26)</li>
     * </ul>
     * </p>
     * 
     * @param value
     *        the double value to convert to a fraction.
     * @param epsilon
     *        maximum error allowed. The resulting fraction is within <code>epsilon</code> of <code>value</code>, in
     *        absolute terms.
     * @param maxIterations
     *        maximum number of convergents.
     * @throws FractionConversionException
     *         if the continued fraction failed to converge.
     * @see #BigFraction(double)
     */
    public BigFraction(final double value, final double epsilon,
                       final int maxIterations) {
        this(value, epsilon, Integer.MAX_VALUE, maxIterations);
    }

    /**
     * Create a fraction given the double value and either the maximum error
     * allowed or the maximum number of denominator digits.
     * <p>
     * 
     * NOTE: This constructor is called with EITHER - a valid epsilon value and the maxDenominator set to
     * Integer.MAX_VALUE (that way the maxDenominator has no effect). OR - a valid maxDenominator value and the epsilon
     * value set to zero (that way epsilon only has effect if there is an exact match before the maxDenominator value is
     * reached).
     * </p>
     * <p>
     * 
     * It has been done this way so that the same code can be (re)used for both scenarios. However this could be
     * confusing to users if it were part of the public API and this constructor should therefore remain PRIVATE.
     * </p>
     * 
     * See JIRA issue ticket MATH-181 for more details:
     * 
     * https://issues.apache.org/jira/browse/MATH-181
     * 
     * @param value
     *        the double value to convert to a fraction.
     * @param epsilon
     *        maximum error allowed. The resulting fraction is within <code>epsilon</code> of <code>value</code>, in
     *        absolute terms.
     * @param maxDenominator
     *        maximum denominator value allowed.
     * @param maxIterations
     *        maximum number of convergents.
     * @throws FractionConversionException
     *         if the continued fraction failed to converge.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private BigFraction(final double value, final double epsilon,
                        final int maxDenominator, final int maxIterations) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        super();
        final long overflow = Integer.MAX_VALUE;
        double r0 = value;
        long a0 = (long) MathLib.floor(r0);
        if (a0 > overflow) {
            throw new FractionConversionException(value, a0, 1L);
        }

        // check for (almost) integer arguments, which should not go
        // to iterations.
        if (MathLib.abs(a0 - value) < epsilon) {
            this.numerator = BigInteger.valueOf(a0);
            this.denominator = BigInteger.ONE;
            return;
        }

        long p0 = 1;
        long q0 = 0;
        long p1 = a0;
        long q1 = 1;

        long p2 = 0;
        long q2 = 1;

        int n = 0;
        boolean stop = false;
        do {
            ++n;
            final double r1 = 1.0 / (r0 - a0);
            final long a1 = (long) MathLib.floor(r1);
            p2 = (a1 * p1) + p0;
            q2 = (a1 * q1) + q0;
            if ((p2 > overflow) || (q2 > overflow)) {
                throw new FractionConversionException(value, p2, q2);
            }

            final double convergent = (double) p2 / (double) q2;
            if ((n < maxIterations) &&
                    (MathLib.abs(convergent - value) > epsilon) &&
                    (q2 < maxDenominator)) {
                p0 = p1;
                p1 = p2;
                q0 = q1;
                q1 = q2;
                a0 = a1;
                r0 = r1;
            } else {
                stop = true;
            }
        } while (!stop);

        if (n >= maxIterations) {
            throw new FractionConversionException(value, maxIterations);
        }

        if (q2 < maxDenominator) {
            this.numerator = BigInteger.valueOf(p2);
            this.denominator = BigInteger.valueOf(q2);
        } else {
            this.numerator = BigInteger.valueOf(p1);
            this.denominator = BigInteger.valueOf(q1);
        }
    }

    /**
     * Create a fraction given the double value and maximum denominator.
     * <p>
     * References:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/ContinuedFraction.html"> Continued Fraction</a> equations (11) and
     * (22)-(26)</li>
     * </ul>
     * </p>
     * 
     * @param value
     *        the double value to convert to a fraction.
     * @param maxDenominator
     *        The maximum allowed value for denominator.
     * @throws FractionConversionException
     *         if the continued fraction failed to converge.
     */
    public BigFraction(final double value, final int maxDenominator) {
        this(value, 0, maxDenominator, 100);
    }

    /**
     * <p>
     * Create a {@link BigFraction} equivalent to the passed <tt>int</tt>, ie "num / 1".
     * </p>
     * 
     * @param num
     *        the numerator.
     */
    public BigFraction(final int num) {
        this(BigInteger.valueOf(num), BigInteger.ONE);
    }

    /**
     * <p>
     * Create a {@link BigFraction} given the numerator and denominator as simple <tt>int</tt>. The {@link BigFraction}
     * is reduced to lowest terms.
     * </p>
     * 
     * @param num
     *        the numerator.
     * @param den
     *        the denominator.
     */
    public BigFraction(final int num, final int den) {
        this(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    /**
     * <p>
     * Create a {@link BigFraction} equivalent to the passed long, ie "num / 1".
     * </p>
     * 
     * @param num
     *        the numerator.
     */
    public BigFraction(final long num) {
        this(BigInteger.valueOf(num), BigInteger.ONE);
    }

    /**
     * <p>
     * Create a {@link BigFraction} given the numerator and denominator as simple <tt>long</tt>. The {@link BigFraction}
     * is reduced to lowest terms.
     * </p>
     * 
     * @param num
     *        the numerator.
     * @param den
     *        the denominator.
     */
    public BigFraction(final long num, final long den) {
        this(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    /**
     * <p>
     * Creates a <code>BigFraction</code> instance with the 2 parts of a fraction Y/Z.
     * </p>
     * 
     * <p>
     * Any negative signs are resolved to be on the numerator.
     * </p>
     * 
     * @param numerator
     *        the numerator, for example the three in 'three sevenths'.
     * @param denominator
     *        the denominator, for example the seven in 'three sevenths'.
     * @return a new fraction instance, with the numerator and denominator
     *         reduced.
     * @throws ArithmeticException
     *         if the denominator is <code>zero</code>.
     */
    public static BigFraction getReducedFraction(final int numerator,
                                                 final int denominator) {
        if (numerator == 0) {
            // normalize zero.
            return ZERO;
        }

        return new BigFraction(numerator, denominator);
    }

    /**
     * <p>
     * Returns the absolute value of this {@link BigFraction}.
     * </p>
     * 
     * @return the absolute value as a {@link BigFraction}.
     */
    public BigFraction abs() {
        return (BigInteger.ZERO.compareTo(this.numerator) <= 0) ? this : this.negate();
    }

    /**
     * <p>
     * Adds the value of this fraction to the passed {@link BigInteger}, returning the result in reduced form.
     * </p>
     * 
     * @param bg
     *        the {@link BigInteger} to add, must'nt be <code>null</code>.
     * @return a <code>BigFraction</code> instance with the resulting values.
     * @throws NullArgumentException
     *         if the {@link BigInteger} is <code>null</code>.
     */
    public BigFraction add(final BigInteger bg) {
        MathUtils.checkNotNull(bg);
        return new BigFraction(this.numerator.add(this.denominator.multiply(bg)), this.denominator);
    }

    /**
     * <p>
     * Adds the value of this fraction to the passed <tt>integer</tt>, returning the result in reduced form.
     * </p>
     * 
     * @param i
     *        the <tt>integer</tt> to add.
     * @return a <code>BigFraction</code> instance with the resulting values.
     */
    public BigFraction add(final int i) {
        return this.add(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Adds the value of this fraction to the passed <tt>long</tt>, returning the result in reduced form.
     * </p>
     * 
     * @param l
     *        the <tt>long</tt> to add.
     * @return a <code>BigFraction</code> instance with the resulting values.
     */
    public BigFraction add(final long l) {
        return this.add(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Adds the value of this fraction to another, returning the result in reduced form.
     * </p>
     * 
     * @param fraction
     *        the {@link BigFraction} to add, must not be <code>null</code>.
     * @return a {@link BigFraction} instance with the resulting values.
     * @throws NullArgumentException
     *         if the {@link BigFraction} is {@code null}.
     */
    @Override
    public BigFraction add(final BigFraction fraction) {
        if (fraction == null) {
            // raise an exception if the fraction is null
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        // no changes to do if the fraction is 0
        if (ZERO.equals(fraction)) {
            return this;
        }

        BigInteger num = null;
        BigInteger den = null;

        if (this.denominator.equals(fraction.denominator)) {
            // if the detonimators are the same, the numerator is the sum of both
            num = this.numerator.add(fraction.numerator);
            den = this.denominator;
        } else {
            // convert the both fraction at the same denominator to add the numerators
            num = (this.numerator.multiply(fraction.denominator)).add((fraction.numerator).multiply(this.denominator));
            den = this.denominator.multiply(fraction.denominator);
        }
        return new BigFraction(num, den);

    }

    /**
     * <p>
     * Gets the fraction as a <code>BigDecimal</code>. This calculates the fraction as the numerator divided by
     * denominator.
     * </p>
     * 
     * @return the fraction as a <code>BigDecimal</code>.
     * @throws ArithmeticException
     *         if the exact quotient does not have a terminating decimal
     *         expansion.
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(this.numerator).divide(new BigDecimal(this.denominator));
    }

    /**
     * <p>
     * Gets the fraction as a <code>BigDecimal</code> following the passed rounding mode. This calculates the fraction
     * as the numerator divided by denominator.
     * </p>
     * 
     * @param roundingMode
     *        rounding mode to apply. see {@link BigDecimal} constants.
     * @return the fraction as a <code>BigDecimal</code>.
     * @throws IllegalArgumentException
     *         if <tt>roundingMode</tt> does not represent a valid rounding
     *         mode.
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue(final int roundingMode) {
        return new BigDecimal(this.numerator).divide(new BigDecimal(this.denominator), roundingMode);
    }

    /**
     * <p>
     * Gets the fraction as a <code>BigDecimal</code> following the passed scale and rounding mode. This calculates the
     * fraction as the numerator divided by denominator.
     * </p>
     * 
     * @param scale
     *        scale of the <code>BigDecimal</code> quotient to be returned.
     *        see {@link BigDecimal} for more information.
     * @param roundingMode
     *        rounding mode to apply. see {@link BigDecimal} constants.
     * @return the fraction as a <code>BigDecimal</code>.
     * @see BigDecimal
     */
    public BigDecimal bigDecimalValue(final int scale, final int roundingMode) {
        return new BigDecimal(this.numerator).divide(new BigDecimal(this.denominator), scale, roundingMode);
    }

    /**
     * <p>
     * Compares this object to another based on size.
     * </p>
     * 
     * @param object
     *        the object to compare to, must not be <code>null</code>.
     * @return -1 if this is less than <tt>object</tt>, +1 if this is greater
     *         than <tt>object</tt>, 0 if they are equal.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final BigFraction object) {
        final BigInteger nOd = this.numerator.multiply(object.denominator);
        final BigInteger dOn = this.denominator.multiply(object.numerator);
        return nOd.compareTo(dOn);
    }

    /**
     * <p>
     * Divide the value of this fraction by the passed {@code BigInteger}, ie {@code this * 1 / bg}, returning the
     * result in reduced form.
     * </p>
     * 
     * @param bg
     *        the {@code BigInteger} to divide by, must not be {@code null}
     * @return a {@link BigFraction} instance with the resulting values
     * @throws NullArgumentException
     *         if the {@code BigInteger} is {@code null}
     * @throws MathArithmeticException
     *         if the fraction to divide by is zero
     */
    public BigFraction divide(final BigInteger bg) {
        if (bg == null) {
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        if (BigInteger.ZERO.equals(bg)) {
            throw new MathArithmeticException(PatriusMessages.ZERO_DENOMINATOR);
        }
        return new BigFraction(this.numerator, this.denominator.multiply(bg));
    }

    /**
     * <p>
     * Divide the value of this fraction by the passed {@code int}, ie {@code this * 1 / i}, returning the result in
     * reduced form.
     * </p>
     * 
     * @param i
     *        the {@code int} to divide by
     * @return a {@link BigFraction} instance with the resulting values
     * @throws MathArithmeticException
     *         if the fraction to divide by is zero
     */
    public BigFraction divide(final int i) {
        return this.divide(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Divide the value of this fraction by the passed {@code long}, ie {@code this * 1 / l}, returning the result in
     * reduced form.
     * </p>
     * 
     * @param l
     *        the {@code long} to divide by
     * @return a {@link BigFraction} instance with the resulting values
     * @throws MathArithmeticException
     *         if the fraction to divide by is zero
     */
    public BigFraction divide(final long l) {
        return this.divide(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Divide the value of this fraction by another, returning the result in reduced form.
     * </p>
     * 
     * @param fraction
     *        Fraction to divide by, must not be {@code null}.
     * @return a {@link BigFraction} instance with the resulting values.
     * @throws NullArgumentException
     *         if the {@code fraction} is {@code null}.
     * @throws MathArithmeticException
     *         if the fraction to divide by is zero
     */
    @Override
    public BigFraction divide(final BigFraction fraction) {
        if (fraction == null) {
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        if (BigInteger.ZERO.equals(fraction.numerator)) {
            throw new MathArithmeticException(PatriusMessages.ZERO_DENOMINATOR);
        }

        return this.multiply(fraction.reciprocal());
    }

    /**
     * <p>
     * Gets the fraction as a <tt>double</tt>. This calculates the fraction as the numerator divided by denominator.
     * </p>
     * 
     * @return the fraction as a <tt>double</tt>
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue() {
        double result = this.numerator.doubleValue() / this.denominator.doubleValue();
        if (Double.isNaN(result)) {
            // Numerator and/or denominator must be out of range:
            // Calculate how far to shift them to put them in range.
            final int shift = Math.max(this.numerator.bitLength(),
                this.denominator.bitLength()) - MathLib.getExponent(Double.MAX_VALUE);
            result = this.numerator.shiftRight(shift).doubleValue() /
                    this.denominator.shiftRight(shift).doubleValue();
        }
        return result;
    }

    /**
     * <p>
     * Test for the equality of two fractions. If the lowest term numerator and denominators are the same for both
     * fractions, the two fractions are considered to be equal.
     * </p>
     * 
     * @param other
     *        fraction to test for equality to this fraction, can be <code>null</code>.
     * @return true if two fractions are equal, false if object is <code>null</code>, not an instance of
     *         {@link BigFraction}, or not
     *         equal to this fraction instance.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        boolean ret = false;

        if (this == other) {
            ret = true;
        } else if (other instanceof BigFraction) {
            final BigFraction rhs = ((BigFraction) other).reduce();
            final BigFraction thisOne = this.reduce();
            ret = thisOne.numerator.equals(rhs.numerator) && thisOne.denominator.equals(rhs.denominator);
        }

        return ret;
    }

    /**
     * <p>
     * Gets the fraction as a <tt>float</tt>. This calculates the fraction as the numerator divided by denominator.
     * </p>
     * 
     * @return the fraction as a <tt>float</tt>.
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue() {
        float result = this.numerator.floatValue() / this.denominator.floatValue();
        if (Double.isNaN(result)) {
            // Numerator and/or denominator must be out of range:
            // Calculate how far to shift them to put them in range.
            final int shift = Math.max(this.numerator.bitLength(),
                this.denominator.bitLength()) - MathLib.getExponent(Float.MAX_VALUE);
            result = this.numerator.shiftRight(shift).floatValue() /
                    this.denominator.shiftRight(shift).floatValue();
        }
        return result;
    }

    /**
     * <p>
     * Access the denominator as a <code>BigInteger</code>.
     * </p>
     * 
     * @return the denominator as a <code>BigInteger</code>.
     */
    public BigInteger getDenominator() {
        return this.denominator;
    }

    /**
     * <p>
     * Access the denominator as a <tt>int</tt>.
     * </p>
     * 
     * @return the denominator as a <tt>int</tt>.
     */
    public int getDenominatorAsInt() {
        return this.denominator.intValue();
    }

    /**
     * <p>
     * Access the denominator as a <tt>long</tt>.
     * </p>
     * 
     * @return the denominator as a <tt>long</tt>.
     */
    public long getDenominatorAsLong() {
        return this.denominator.longValue();
    }

    /**
     * <p>
     * Access the numerator as a <code>BigInteger</code>.
     * </p>
     * 
     * @return the numerator as a <code>BigInteger</code>.
     */
    public BigInteger getNumerator() {
        return this.numerator;
    }

    /**
     * <p>
     * Access the numerator as a <tt>int</tt>.
     * </p>
     * 
     * @return the numerator as a <tt>int</tt>.
     */
    public int getNumeratorAsInt() {
        return this.numerator.intValue();
    }

    /**
     * <p>
     * Access the numerator as a <tt>long</tt>.
     * </p>
     * 
     * @return the numerator as a <tt>long</tt>.
     */
    public long getNumeratorAsLong() {
        return this.numerator.longValue();
    }

    /**
     * <p>
     * Gets a hashCode for the fraction.
     * </p>
     * 
     * @return a hash code value for this object.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 37 * (37 * 17 + this.numerator.hashCode()) + this.denominator.hashCode();
    }

    /**
     * <p>
     * Gets the fraction as an <tt>int</tt>. This returns the whole number part of the fraction.
     * </p>
     * 
     * @return the whole number fraction part.
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue() {
        return this.numerator.divide(this.denominator).intValue();
    }

    /**
     * <p>
     * Gets the fraction as a <tt>long</tt>. This returns the whole number part of the fraction.
     * </p>
     * 
     * @return the whole number fraction part.
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue() {
        return this.numerator.divide(this.denominator).longValue();
    }

    /**
     * <p>
     * Multiplies the value of this fraction by the passed <code>BigInteger</code>, returning the result in reduced
     * form.
     * </p>
     * 
     * @param bg
     *        the {@code BigInteger} to multiply by.
     * @return a {@code BigFraction} instance with the resulting values.
     * @throws NullArgumentException
     *         if {@code bg} is {@code null}.
     */
    public BigFraction multiply(final BigInteger bg) {
        if (bg == null) {
            throw new NullArgumentException();
        }
        return new BigFraction(bg.multiply(this.numerator), this.denominator);
    }

    /**
     * <p>
     * Multiply the value of this fraction by the passed <tt>int</tt>, returning the result in reduced form.
     * </p>
     * 
     * @param i
     *        the <tt>int</tt> to multiply by.
     * @return a {@link BigFraction} instance with the resulting values.
     */
    @Override
    public BigFraction multiply(final int i) {
        return this.multiply(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Multiply the value of this fraction by the passed <tt>long</tt>, returning the result in reduced form.
     * </p>
     * 
     * @param l
     *        the <tt>long</tt> to multiply by.
     * @return a {@link BigFraction} instance with the resulting values.
     */
    public BigFraction multiply(final long l) {
        return this.multiply(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Multiplies the value of this fraction by another, returning the result in reduced form.
     * </p>
     * 
     * @param fraction
     *        Fraction to multiply by, must not be {@code null}.
     * @return a {@link BigFraction} instance with the resulting values.
     * @throws NullArgumentException
     *         if {@code fraction} is {@code null}.
     */
    @Override
    public BigFraction multiply(final BigFraction fraction) {
        if (fraction == null) {
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        if (this.numerator.equals(BigInteger.ZERO) ||
                fraction.numerator.equals(BigInteger.ZERO)) {
            return ZERO;
        }
        return new BigFraction(this.numerator.multiply(fraction.numerator),
            this.denominator.multiply(fraction.denominator));
    }

    /**
     * <p>
     * Return the additive inverse of this fraction, returning the result in reduced form.
     * </p>
     * 
     * @return the negation of this fraction.
     */
    @Override
    public BigFraction negate() {
        return new BigFraction(this.numerator.negate(), this.denominator);
    }

    /**
     * <p>
     * Gets the fraction percentage as a <tt>double</tt>. This calculates the fraction as the numerator divided by
     * denominator multiplied by 100.
     * </p>
     * 
     * @return the fraction percentage as a <tt>double</tt>.
     */
    public double percentageValue() {
        return this.multiply(ONE_HUNDRED).doubleValue();
    }

    /**
     * <p>
     * Returns a {@code BigFraction} whose value is {@code (this<sup>exponent</sup>)}, returning the result in reduced
     * form.
     * </p>
     * 
     * @param exponent
     *        exponent to which this {@code BigFraction} is to be
     *        raised.
     * @return <tt>this<sup>exponent</sup></tt>.
     */
    public BigFraction pow(final int exponent) {
        if (exponent < 0) {
            return new BigFraction(this.denominator.pow(-exponent), this.numerator.pow(-exponent));
        }
        return new BigFraction(this.numerator.pow(exponent), this.denominator.pow(exponent));
    }

    /**
     * <p>
     * Returns a <code>BigFraction</code> whose value is <tt>(this<sup>exponent</sup>)</tt>, returning the result in
     * reduced form.
     * </p>
     * 
     * @param exponent
     *        exponent to which this <code>BigFraction</code> is to be raised.
     * @return <tt>this<sup>exponent</sup></tt> as a <code>BigFraction</code>.
     */
    public BigFraction pow(final long exponent) {
        if (exponent < 0) {
            return new BigFraction(ArithmeticUtils.pow(this.denominator, -exponent),
                ArithmeticUtils.pow(this.numerator, -exponent));
        }
        return new BigFraction(ArithmeticUtils.pow(this.numerator, exponent),
            ArithmeticUtils.pow(this.denominator, exponent));
    }

    /**
     * <p>
     * Returns a <code>BigFraction</code> whose value is <tt>(this<sup>exponent</sup>)</tt>, returning the result in
     * reduced form.
     * </p>
     * 
     * @param exponent
     *        exponent to which this <code>BigFraction</code> is to be raised.
     * @return <tt>this<sup>exponent</sup></tt> as a <code>BigFraction</code>.
     */
    public BigFraction pow(final BigInteger exponent) {
        if (exponent.compareTo(BigInteger.ZERO) < 0) {
            final BigInteger eNeg = exponent.negate();
            return new BigFraction(ArithmeticUtils.pow(this.denominator, eNeg),
                ArithmeticUtils.pow(this.numerator, eNeg));
        }
        return new BigFraction(ArithmeticUtils.pow(this.numerator, exponent),
            ArithmeticUtils.pow(this.denominator, exponent));
    }

    /**
     * <p>
     * Returns a <code>double</code> whose value is <tt>(this<sup>exponent</sup>)</tt>, returning the result in reduced
     * form.
     * </p>
     * 
     * @param exponent
     *        exponent to which this <code>BigFraction</code> is to be raised.
     * @return <tt>this<sup>exponent</sup></tt>.
     */
    public double pow(final double exponent) {
        return MathLib.pow(this.numerator.doubleValue(), exponent) /
                MathLib.pow(this.denominator.doubleValue(), exponent);
    }

    /**
     * <p>
     * Return the multiplicative inverse of this fraction.
     * </p>
     * 
     * @return the reciprocal fraction.
     */
    @Override
    public BigFraction reciprocal() {
        return new BigFraction(this.denominator, this.numerator);
    }

    /**
     * <p>
     * Reduce this <code>BigFraction</code> to its lowest terms.
     * </p>
     * 
     * @return the reduced <code>BigFraction</code>. It doesn't change anything if
     *         the fraction can be reduced.
     */
    public BigFraction reduce() {
        final BigInteger gcd = this.numerator.gcd(this.denominator);
        return new BigFraction(this.numerator.divide(gcd), this.denominator.divide(gcd));
    }

    /**
     * <p>
     * Subtracts the value of an {@link BigInteger} from the value of this {@code BigFraction}, returning the result in
     * reduced form.
     * </p>
     * 
     * @param bg
     *        the {@link BigInteger} to subtract, cannot be {@code null}.
     * @return a {@code BigFraction} instance with the resulting values.
     * @throws NullArgumentException
     *         if the {@link BigInteger} is {@code null}.
     */
    public BigFraction subtract(final BigInteger bg) {
        if (bg == null) {
            throw new NullArgumentException();
        }
        return new BigFraction(this.numerator.subtract(this.denominator.multiply(bg)), this.denominator);
    }

    /**
     * <p>
     * Subtracts the value of an {@code integer} from the value of this {@code BigFraction}, returning the result in
     * reduced form.
     * </p>
     * 
     * @param i
     *        the {@code integer} to subtract.
     * @return a {@code BigFraction} instance with the resulting values.
     */
    public BigFraction subtract(final int i) {
        return this.subtract(BigInteger.valueOf(i));
    }

    /**
     * <p>
     * Subtracts the value of a {@code long} from the value of this {@code BigFraction}, returning the result in reduced
     * form.
     * </p>
     * 
     * @param l
     *        the {@code long} to subtract.
     * @return a {@code BigFraction} instance with the resulting values.
     */
    public BigFraction subtract(final long l) {
        return this.subtract(BigInteger.valueOf(l));
    }

    /**
     * <p>
     * Subtracts the value of another fraction from the value of this one, returning the result in reduced form.
     * </p>
     * 
     * @param fraction
     *        {@link BigFraction} to subtract, must not be {@code null}.
     * @return a {@link BigFraction} instance with the resulting values
     * @throws NullArgumentException
     *         if the {@code fraction} is {@code null}.
     */
    @Override
    public BigFraction subtract(final BigFraction fraction) {
        if (fraction == null) {
            // raise an exception if the fraction is null
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        // no changes to do if the fraction value is ZERO
        if (ZERO.equals(fraction)) {
            return this;
        }

        BigInteger num = null;
        BigInteger den = null;
        if (this.denominator.equals(fraction.denominator)) {
            // if the detonimators are the same, the numerator is the sum of both
            num = this.numerator.subtract(fraction.numerator);
            den = this.denominator;
        } else {
            // convert the both fraction at the same denominator to add the numerators
            num =
                (this.numerator.multiply(fraction.denominator)).subtract((fraction.numerator)
                    .multiply(this.denominator));
            den = this.denominator.multiply(fraction.denominator);
        }
        return new BigFraction(num, den);

    }

    /**
     * <p>
     * Returns the <code>String</code> representing this fraction, ie "num / dem" or just "num" if the denominator is
     * one.
     * </p>
     * 
     * @return a string representation of the fraction.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String str = null;
        if (BigInteger.ONE.equals(this.denominator)) {
            str = this.numerator.toString();
        } else if (BigInteger.ZERO.equals(this.numerator)) {
            str = "0";
        } else {
            str = this.numerator + " / " + this.denominator;
        }
        return str;
    }

    /** {@inheritDoc} */
    @Override
    public BigFractionField getField() {
        return BigFractionField.getInstance();
    }

    // CHECKSTYLE: resume MagicNumber check
}
