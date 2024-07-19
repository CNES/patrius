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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fraction;

import java.math.BigInteger;

import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * Representation of a rational number.
 * 
 * implements Serializable since 2.0
 * 
 * @since 1.1
 * @version $Id: Fraction.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Fraction extends Number implements FieldElement<Fraction>, Comparable<Fraction> {

    /** A fraction representing "2 / 1". */
    public static final Fraction TWO = new Fraction(2, 1);

    /** A fraction representing "1". */
    public static final Fraction ONE = new Fraction(1, 1);

    /** A fraction representing "0". */
    public static final Fraction ZERO = new Fraction(0, 1);

    /** A fraction representing "4/5". */
    public static final Fraction FOUR_FIFTHS = new Fraction(4, 5);

    /** A fraction representing "1/5". */
    public static final Fraction ONE_FIFTH = new Fraction(1, 5);

    /** A fraction representing "1/2". */
    public static final Fraction ONE_HALF = new Fraction(1, 2);

    /** A fraction representing "1/4". */
    public static final Fraction ONE_QUARTER = new Fraction(1, 4);

    /** A fraction representing "1/3". */
    public static final Fraction ONE_THIRD = new Fraction(1, 3);

    /** A fraction representing "3/5". */
    public static final Fraction THREE_FIFTHS = new Fraction(3, 5);

    /** A fraction representing "3/4". */
    public static final Fraction THREE_QUARTERS = new Fraction(3, 4);

    /** A fraction representing "2/5". */
    public static final Fraction TWO_FIFTHS = new Fraction(2, 5);

    /** A fraction representing "2/4". */
    public static final Fraction TWO_QUARTERS = new Fraction(2, 4);

    /** A fraction representing "2/3". */
    public static final Fraction TWO_THIRDS = new Fraction(2, 3);

    /** A fraction representing "-1 / 1". */
    public static final Fraction MINUS_ONE = new Fraction(-1, 1);

     /** Serializable UID. */
    private static final long serialVersionUID = 3698073679419233275L;

    /** The denominator. */
    private final int denominator;

    /** The numerator. */
    private final int numerator;

    /**
     * Create a fraction given the double value.
     * 
     * @param value
     *        the double value to convert to a fraction.
     * @throws FractionConversionException
     *         if the continued fraction failed to
     *         converge.
     */
    public Fraction(final double value) {
        this(value, 1.0e-5, 100);
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
     *        maximum error allowed. The resulting fraction is within {@code epsilon} of {@code value}, in absolute
     *        terms.
     * @param maxIterations
     *        maximum number of convergents
     * @throws FractionConversionException
     *         if the continued fraction failed to
     *         converge.
     */
    public Fraction(final double value, final double epsilon, final int maxIterations) {
        this(value, epsilon, Integer.MAX_VALUE, maxIterations);
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
     *        The maximum allowed value for denominator
     * @throws FractionConversionException
     *         if the continued fraction failed to
     *         converge
     */
    public Fraction(final double value, final int maxDenominator) {
        this(value, 0, maxDenominator, 100);
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
     *        maximum error allowed. The resulting fraction is within {@code epsilon} of {@code value}, in absolute
     *        terms.
     * @param maxDenominator
     *        maximum denominator value allowed.
     * @param maxIterations
     *        maximum number of convergents
     * @throws FractionConversionException
     *         if the continued fraction failed to
     *         converge.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private Fraction(final double value, final double epsilon, final int maxDenominator, final int maxIterations) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        super();
        final long overflow = Integer.MAX_VALUE;
        double r0 = value;
        long a0 = (long) MathLib.floor(r0);
        if (MathLib.abs(a0) > overflow) {
            throw new FractionConversionException(value, a0, 1L);
        }

        // check for (almost) integer arguments, which should not go
        // to iterations.
        if (MathLib.abs(a0 - value) < epsilon) {
            this.numerator = (int) a0;
            this.denominator = 1;
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
            if ((MathLib.abs(p2) > overflow) || (MathLib.abs(q2) > overflow)) {
                throw new FractionConversionException(value, p2, q2);
            }

            final double convergent = (double) p2 / (double) q2;
            if (n < maxIterations && MathLib.abs(convergent - value) > epsilon && q2 < maxDenominator) {
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
            this.numerator = (int) p2;
            this.denominator = (int) q2;
        } else {
            this.numerator = (int) p1;
            this.denominator = (int) q1;
        }

    }

    /**
     * Create a fraction from an int.
     * The fraction is num / 1.
     * 
     * @param num
     *        the numerator.
     */
    public Fraction(final int num) {
        this(num, 1);
    }

    /**
     * Create a fraction given the numerator and denominator. The fraction is
     * reduced to lowest terms.
     * 
     * @param numIn
     *        the numerator.
     * @param denIn
     *        the denominator.
     * @throws MathArithmeticException
     *         if the denominator is {@code zero}
     */
    public Fraction(final int numIn, final int denIn) {
        super();

        int num = numIn;
        int den = denIn;

        if (den == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_DENOMINATOR_IN_FRACTION,
                num, den);
        }
        if (den < 0) {
            if (num == Integer.MIN_VALUE ||
                den == Integer.MIN_VALUE) {
                throw new MathArithmeticException(PatriusMessages.OVERFLOW_IN_FRACTION,
                    num, den);
            }
            num = -num;
            den = -den;
        }
        // reduce numerator and denominator by greatest common denominator.
        final int d = ArithmeticUtils.gcd(num, den);
        if (d > 1) {
            num /= d;
            den /= d;
        }

        // move sign to numerator.
        if (den < 0) {
            num = -num;
            den = -den;
        }
        this.numerator = num;
        this.denominator = den;
    }

    /**
     * Returns the absolute value of this fraction.
     * 
     * @return the absolute value.
     */
    public Fraction abs() {
        final Fraction ret;
        if (this.numerator >= 0) {
            ret = this;
        } else {
            ret = this.negate();
        }
        return ret;
    }

    /**
     * Compares this object to another based on size.
     * 
     * @param object
     *        the object to compare to
     * @return -1 if this is less than <tt>object</tt>, +1 if this is greater
     *         than <tt>object</tt>, 0 if they are equal.
     */
    @Override
    public int compareTo(final Fraction object) {
        final long nOd = ((long) this.numerator) * object.denominator;
        final long dOn = ((long) this.denominator) * object.numerator;
        return (nOd < dOn) ? -1 : ((nOd > dOn) ? +1 : 0);
    }

    /**
     * Gets the fraction as a <tt>double</tt>. This calculates the fraction as
     * the numerator divided by denominator.
     * 
     * @return the fraction as a <tt>double</tt>
     */
    @Override
    public double doubleValue() {
        return (double) this.numerator / (double) this.denominator;
    }

    /**
     * Test for the equality of two fractions. If the lowest term
     * numerator and denominators are the same for both fractions, the two
     * fractions are considered to be equal.
     * 
     * @param other
     *        fraction to test for equality to this fraction
     * @return true if two fractions are equal, false if object is <tt>null</tt>, not an instance of {@link Fraction},
     *         or not equal
     *         to this fraction instance.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Fraction) {
            // since fractions are always in lowest terms, numerators and
            // denominators can be compared directly for equality.
            final Fraction rhs = (Fraction) other;
            return (this.numerator == rhs.numerator) &&
                (this.denominator == rhs.denominator);
        }
        return false;
    }

    /**
     * Gets the fraction as a <tt>float</tt>. This calculates the fraction as
     * the numerator divided by denominator.
     * 
     * @return the fraction as a <tt>float</tt>
     */
    @Override
    public float floatValue() {
        return (float) this.doubleValue();
    }

    /**
     * Access the denominator.
     * 
     * @return the denominator.
     */
    public int getDenominator() {
        return this.denominator;
    }

    /**
     * Access the numerator.
     * 
     * @return the numerator.
     */
    public int getNumerator() {
        return this.numerator;
    }

    /**
     * Gets a hashCode for the fraction.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return 37 * (37 * 17 + this.numerator) + this.denominator;
    }

    /**
     * Gets the fraction as an <tt>int</tt>. This returns the whole number part
     * of the fraction.
     * 
     * @return the whole number fraction part
     */
    @Override
    public int intValue() {
        return (int) this.doubleValue();
    }

    /**
     * Gets the fraction as a <tt>long</tt>. This returns the whole number part
     * of the fraction.
     * 
     * @return the whole number fraction part
     */
    @Override
    public long longValue() {
        return (long) this.doubleValue();
    }

    /**
     * Return the additive inverse of this fraction.
     * 
     * @return the negation of this fraction.
     */
    @Override
    public Fraction negate() {
        if (this.numerator == Integer.MIN_VALUE) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW_IN_FRACTION, this.numerator, this.denominator);
        }
        return new Fraction(-this.numerator, this.denominator);
    }

    /**
     * Return the multiplicative inverse of this fraction.
     * 
     * @return the reciprocal fraction
     */
    @Override
    public Fraction reciprocal() {
        return new Fraction(this.denominator, this.numerator);
    }

    /**
     * <p>
     * Adds the value of this fraction to another, returning the result in reduced form. The algorithm follows Knuth,
     * 4.5.1.
     * </p>
     * 
     * @param fraction
     *        the fraction to add, must not be {@code null}
     * @return a {@code Fraction} instance with the resulting values
     * @throws NullArgumentException
     *         if the fraction is {@code null}
     * @throws MathArithmeticException
     *         if the resulting numerator or denominator exceeds {@code Integer.MAX_VALUE}
     */
    @Override
    public Fraction add(final Fraction fraction) {
        return this.addSub(fraction, true /* add */);
    }

    /**
     * Add an integer to the fraction.
     * 
     * @param i
     *        the <tt>integer</tt> to add.
     * @return this + i
     */
    public Fraction add(final int i) {
        return new Fraction(this.numerator + i * this.denominator, this.denominator);
    }

    /**
     * <p>
     * Subtracts the value of another fraction from the value of this one, returning the result in reduced form.
     * </p>
     * 
     * @param fraction
     *        the fraction to subtract, must not be {@code null}
     * @return a {@code Fraction} instance with the resulting values
     * @throws NullArgumentException
     *         if the fraction is {@code null}
     * @throws MathArithmeticException
     *         if the resulting numerator or denominator
     *         cannot be represented in an {@code int}.
     */
    @Override
    public Fraction subtract(final Fraction fraction) {
        return this.addSub(fraction, false /* subtract */);
    }

    /**
     * Subtract an integer from the fraction.
     * 
     * @param i
     *        the <tt>integer</tt> to subtract.
     * @return this - i
     */
    public Fraction subtract(final int i) {
        return new Fraction(this.numerator - i * this.denominator, this.denominator);
    }

    /**
     * Implement add and subtract using algorithm described in Knuth 4.5.1.
     * 
     * @param fraction
     *        the fraction to subtract, must not be {@code null}
     * @param isAdd
     *        true to add, false to subtract
     * @return a {@code Fraction} instance with the resulting values
     * @throws NullArgumentException
     *         if the fraction is {@code null}
     * @throws MathArithmeticException
     *         if the resulting numerator or denominator
     *         cannot be represented in an {@code int}.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private Fraction addSub(final Fraction fraction, final boolean isAdd) {
        // CHECKSTYLE: resume ReturnCount check
        if (fraction == null) {
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        // zero is identity for addition.
        if (this.numerator == 0) {
            return isAdd ? fraction : fraction.negate();
        }
        if (fraction.numerator == 0) {
            return this;
        }
        // if denominators are randomly distributed, d1 will be 1 about 61%
        // of the time.
        final int d1 = ArithmeticUtils.gcd(this.denominator, fraction.denominator);
        if (d1 == 1) {
            // result is ( (u*v' +/- u'v) / u'v')
            final int uvp = ArithmeticUtils.mulAndCheck(this.numerator, fraction.denominator);
            final int upv = ArithmeticUtils.mulAndCheck(fraction.numerator, this.denominator);
            return new Fraction(isAdd ? ArithmeticUtils.addAndCheck(uvp, upv) :
                ArithmeticUtils.subAndCheck(uvp, upv),
                ArithmeticUtils.mulAndCheck(this.denominator, fraction.denominator));
        }
        // the quantity 't' requires 65 bits of precision; see knuth 4.5.1
        // exercise 7. we're going to use a BigInteger.
        // t = u(v'/d1) +/- v(u'/d1)
        final BigInteger uvp = BigInteger.valueOf(this.numerator)
            .multiply(BigInteger.valueOf(fraction.denominator / d1));
        final BigInteger upv = BigInteger.valueOf(fraction.numerator)
            .multiply(BigInteger.valueOf(this.denominator / d1));
        final BigInteger t = isAdd ? uvp.add(upv) : uvp.subtract(upv);
        // but d2 doesn't need extra precision because
        // d2 = gcd(t,d1) = gcd(t mod d1, d1)
        final int tmodd1 = t.mod(BigInteger.valueOf(d1)).intValue();
        final int d2 = (tmodd1 == 0) ? d1 : ArithmeticUtils.gcd(tmodd1, d1);

        // result is (t/d2) / (u'/d1)(v'/d2)
        final BigInteger w = t.divide(BigInteger.valueOf(d2));
        if (w.bitLength() > 31) {
            throw new MathArithmeticException(PatriusMessages.NUMERATOR_OVERFLOW_AFTER_MULTIPLY,
                w);
        }
        return new Fraction(w.intValue(),
            ArithmeticUtils.mulAndCheck(this.denominator / d1,
                fraction.denominator / d2));
    }

    /**
     * <p>
     * Multiplies the value of this fraction by another, returning the result in reduced form.
     * </p>
     * 
     * @param fraction
     *        the fraction to multiply by, must not be {@code null}
     * @return a {@code Fraction} instance with the resulting values
     * @throws NullArgumentException
     *         if the fraction is {@code null}
     * @throws MathArithmeticException
     *         if the resulting numerator or denominator exceeds {@code Integer.MAX_VALUE}
     */
    @Override
    public Fraction multiply(final Fraction fraction) {
        if (fraction == null) {
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        if (this.numerator == 0 || fraction.numerator == 0) {
            return ZERO;
        }
        // knuth 4.5.1
        // make sure we don't overflow unless the result *must* overflow.
        final int d1 = ArithmeticUtils.gcd(this.numerator, fraction.denominator);
        final int d2 = ArithmeticUtils.gcd(fraction.numerator, this.denominator);
        return getReducedFraction(ArithmeticUtils.mulAndCheck(this.numerator / d1, fraction.numerator / d2),
            ArithmeticUtils.mulAndCheck(this.denominator / d2, fraction.denominator / d1));
    }

    /**
     * Multiply the fraction by an integer.
     * 
     * @param i
     *        the <tt>integer</tt> to multiply by.
     * @return this * i
     */
    @Override
    public Fraction multiply(final int i) {
        return new Fraction(this.numerator * i, this.denominator);
    }

    /**
     * <p>
     * Divide the value of this fraction by another.
     * </p>
     * 
     * @param fraction
     *        the fraction to divide by, must not be {@code null}
     * @return a {@code Fraction} instance with the resulting values
     * @throws IllegalArgumentException
     *         if the fraction is {@code null}
     * @throws MathArithmeticException
     *         if the fraction to divide by is zero
     * @throws MathArithmeticException
     *         if the resulting numerator or denominator exceeds {@code Integer.MAX_VALUE}
     */
    @Override
    public Fraction divide(final Fraction fraction) {
        if (fraction == null) {
            throw new NullArgumentException(PatriusMessages.FRACTION);
        }
        if (fraction.numerator == 0) {
            throw new MathArithmeticException(PatriusMessages.ZERO_FRACTION_TO_DIVIDE_BY,
                fraction.numerator, fraction.denominator);
        }
        return this.multiply(fraction.reciprocal());
    }

    /**
     * Divide the fraction by an integer.
     * 
     * @param i
     *        the <tt>integer</tt> to divide by.
     * @return this * i
     */
    public Fraction divide(final int i) {
        return new Fraction(this.numerator, this.denominator * i);
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
        return 100 * this.doubleValue();
    }

    /**
     * <p>
     * Creates a {@code Fraction} instance with the 2 parts of a fraction Y/Z.
     * </p>
     * 
     * <p>
     * Any negative signs are resolved to be on the numerator.
     * </p>
     * 
     * @param numeratorIn
     *        the numerator, for example the three in 'three sevenths'
     * @param denominatorIn
     *        the denominator, for example the seven in 'three sevenths'
     * @return a new fraction instance, with the numerator and denominator reduced
     * @throws MathArithmeticException
     *         if the denominator is {@code zero}
     */
    public static Fraction getReducedFraction(final int numeratorIn, final int denominatorIn) {

        // Initialization
        int numerator = numeratorIn;
        int denominator = denominatorIn;

        if (denominator == 0) {
            // Exception
            throw new MathArithmeticException(PatriusMessages.ZERO_DENOMINATOR_IN_FRACTION,
                numerator, denominator);
        }
        if (numerator == 0) {
            // normalize zero.
            return ZERO;
        }
        // allow 2^k/-2^31 as a valid fraction (where k>0)
        if (denominator == Integer.MIN_VALUE && (numerator & 1) == 0) {
            numerator /= 2;
            denominator /= 2;
        }
        if (denominator < 0) {
            if (numerator == Integer.MIN_VALUE ||
                denominator == Integer.MIN_VALUE) {
                throw new MathArithmeticException(PatriusMessages.OVERFLOW_IN_FRACTION,
                    numerator, denominator);
            }
            numerator = -numerator;
            denominator = -denominator;
        }
        // simplify fraction.
        final int gcd = ArithmeticUtils.gcd(numerator, denominator);
        numerator /= gcd;
        denominator /= gcd;

        // Return result
        return new Fraction(numerator, denominator);
    }

    /**
     * <p>
     * Returns the {@code String} representing this fraction, ie "num / dem" or just "num" if the denominator is one.
     * </p>
     * 
     * @return a string representation of the fraction.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String str = null;
        if (this.denominator == 1) {
            str = Integer.toString(this.numerator);
        } else if (this.numerator == 0) {
            str = "0";
        } else {
            str = this.numerator + " / " + this.denominator;
        }
        return str;
    }

    /** {@inheritDoc} */
    @Override
    public FractionField getField() {
        return FractionField.getInstance();
    }

    // CHECKSTYLE: resume MagicNumber check
}
