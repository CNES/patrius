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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.util.Localizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * Some useful, arithmetics related, additions to the built-in functions in {@link Math}.
 *
 * @version $Id: ArithmeticUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class ArithmeticUtils {

    /** All long-representable factorials */
    private static final long[] FACTORIALS = new long[] {
        1L, 1L, 2L,
        6L, 24L, 120L,
        720L, 5040L, 40320L,
        362880L, 3628800L, 39916800L,
        479001600L, 6227020800L, 87178291200L,
        1307674368000L, 20922789888000L, 355687428096000L,
        6402373705728000L, 121645100408832000L, 2432902008176640000L };

    /** Stirling numbers of the second kind. */
    private static final AtomicReference<long[][]> STIRLING_S2 = new AtomicReference<>(null);

    /** Private constructor. */
    private ArithmeticUtils() {
        super();
    }

    /**
     * Add two integers, checking for overflow.
     *
     * @param x
     *        an addend
     * @param y
     *        an addend
     * @return the sum {@code x+y}
     * @throws MathArithmeticException
     *         if the result can not be represented
     *         as an {@code int}.
     * @since 1.1
     */
    public static int addAndCheck(final int x, final int y) {
        final long s = (long) x + (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW_IN_ADDITION, x, y);
        }
        return (int) s;
    }

    /**
     * Add two long integers, checking for overflow.
     *
     * @param a
     *        an addend
     * @param b
     *        an addend
     * @return the sum {@code a+b}
     * @throws MathArithmeticException
     *         if the result can not be represented as an
     *         long
     * @since 1.2
     */
    public static long addAndCheck(final long a, final long b) {
        return ArithmeticUtils.addAndCheck(a, b, PatriusMessages.OVERFLOW_IN_ADDITION);
    }

    /**
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "{@code n choose k}", the number of {@code k}-element subsets that can be selected from an
     * {@code n}-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code 0 <= k <= n } (otherwise {@code IllegalArgumentException} is thrown)</li>
     * <li>The result is small enough to fit into a {@code long}. The largest value of {@code n} for which all
     * coefficients are {@code  < Long.MAX_VALUE} is 66. If the computed value exceeds {@code Long.MAX_VALUE} an
     * {@code ArithMeticException} is thrown.</li>
     * </ul>
     * </p>
     *
     * @param n
     *        the size of the set
     * @param k
     *        the size of the subsets to be counted
     * @return {@code n choose k}
     * @throws NotPositiveException
     *         if {@code n < 0}.
     * @throws NumberIsTooLargeException
     *         if {@code k > n}.
     * @throws MathArithmeticException
     *         if the result is too large to be
     *         represented by a long integer.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static long binomialCoefficient(final int n, final int k) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        ArithmeticUtils.checkBinomial(n, k);
        if ((n == k) || (k == 0)) {
            return 1;
        }
        if ((k == 1) || (k == n - 1)) {
            return n;
        }
        // Use symmetry for large k
        if (k > n / 2) {
            return binomialCoefficient(n, n - k);
        }

        // We use the formula
        // (n choose k) = n! / (n-k)! / k!
        // (n choose k) == ((n-k+1)*...*n) / (1*...*k)
        // which could be written
        // (n choose k) == (n-1 choose k-1) * n / k
        long result = 1;
        if (n <= 61) {
            // For n <= 61, the naive implementation cannot overflow.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                result = result * i / j;
                i++;
            }
        } else if (n <= 66) {
            // For n > 61 but n <= 66, the result cannot overflow,
            // but we must take care not to overflow intermediate values.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                // We know that (result * i) is divisible by j,
                // but (result * i) may overflow, so we split j:
                // Filter out the gcd, d, so j/d and i/d are integer.
                // result is divisible by (j/d) because (j/d)
                // is relative prime to (i/d) and is a divisor of
                // result * (i/d).
                final long d = gcd(i, j);
                result = (result / (j / d)) * (i / d);
                i++;
            }
        } else {
            // For n > 66, a result overflow might occur, so we check
            // the multiplication, taking care to not overflow
            // unnecessary.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                final long d = gcd(i, j);
                result = mulAndCheck(result / (j / d), i / d);
                i++;
            }
        }
        return result;
    }

    /**
     * Returns a {@code double} representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "{@code n choose k}", the number of {@code k}-element subsets that can be selected from an
     * {@code n}-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code 0 <= k <= n } (otherwise {@code IllegalArgumentException} is thrown)</li>
     * <li>The result is small enough to fit into a {@code double}. The largest value of {@code n} for which all
     * coefficients are < Double.MAX_VALUE is 1029. If the computed value exceeds Double.MAX_VALUE,
     * Double.POSITIVE_INFINITY is returned</li>
     * </ul>
     * </p>
     *
     * @param n
     *        the size of the set
     * @param k
     *        the size of the subsets to be counted
     * @return {@code n choose k}
     * @throws NotPositiveException
     *         if {@code n < 0}.
     * @throws NumberIsTooLargeException
     *         if {@code k > n}.
     * @throws MathArithmeticException
     *         if the result is too large to be
     *         represented by a long integer.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static double binomialCoefficientDouble(final int n, final int k) {
        // CHECKSTYLE: resume ReturnCount check
        ArithmeticUtils.checkBinomial(n, k);
        if ((n == k) || (k == 0)) {
            return 1d;
        }
        if ((k == 1) || (k == n - 1)) {
            return n;
        }
        if (k > n / 2) {
            return binomialCoefficientDouble(n, n - k);
        }
        if (n < 67) {
            return binomialCoefficient(n, k);
        }

        double result = 1d;
        for (int i = 1; i <= k; i++) {
            result *= (double) (n - k + i) / (double) i;
        }

        return MathLib.floor(result + 0.5);
    }

    /**
     * Returns the natural {@code log} of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "{@code n choose k}", the number of {@code k}-element subsets that can be selected from an
     * {@code n}-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code 0 <= k <= n } (otherwise {@code IllegalArgumentException} is thrown)</li>
     * </ul>
     * </p>
     *
     * @param n
     *        the size of the set
     * @param k
     *        the size of the subsets to be counted
     * @return {@code n choose k}
     * @throws NotPositiveException
     *         if {@code n < 0}.
     * @throws NumberIsTooLargeException
     *         if {@code k > n}.
     * @throws MathArithmeticException
     *         if the result is too large to be
     *         represented by a long integer.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static double binomialCoefficientLog(final int n, final int k) {
        // CHECKSTYLE: resume ReturnCount check
        ArithmeticUtils.checkBinomial(n, k);
        if ((n == k) || (k == 0)) {
            return 0;
        }
        if ((k == 1) || (k == n - 1)) {
            return MathLib.log(n);
        }

        /*
         * For values small enough to do exact integer computation,
         * return the log of the exact value
         */
        if (n < 67) {
            return MathLib.log(binomialCoefficient(n, k));
        }

        /*
         * Return the log of binomialCoefficientDouble for values that will not
         * overflow binomialCoefficientDouble
         */
        if (n < 1030) {
            return MathLib.log(binomialCoefficientDouble(n, k));
        }

        if (k > n / 2) {
            return binomialCoefficientLog(n, n - k);
        }

        /*
         * Sum logs for values that could overflow
         */
        double logSum = 0;

        // n!/(n-k)!
        for (int i = n - k + 1; i <= n; i++) {
            logSum += MathLib.log(i);
        }

        // divide by k!
        for (int i = 2; i <= k; i++) {
            logSum -= MathLib.log(i);
        }

        return logSum;
    }

    /**
     * Returns n!. Shorthand for {@code n} <a
     * href="http://mathworld.wolfram.com/Factorial.html"> Factorial</a>, the
     * product of the numbers {@code 1,...,n}.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code n >= 0} (otherwise {@code IllegalArgumentException} is thrown)</li>
     * <li>The result is small enough to fit into a {@code long}. The largest value of {@code n} for which {@code n!} <
     * Long.MAX_VALUE} is 20. If the computed value exceeds {@code Long.MAX_VALUE} an {@code ArithMeticException } is
     * thrown.</li>
     * </ul>
     * </p>
     *
     * @param n
     *        argument
     * @return {@code n!}
     * @throws MathArithmeticException
     *         if the result is too large to be represented
     *         by a {@code long}.
     * @throws NotPositiveException
     *         if {@code n < 0}.
     * @throws MathArithmeticException
     *         if {@code n > 20}: The factorial value is too
     *         large to fit in a {@code long}.
     */
    public static long factorial(final int n) {
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.FACTORIAL_NEGATIVE_PARAMETER,
                n);
        }
        if (n > 20) {
            throw new MathArithmeticException();
        }
        return FACTORIALS[n];
    }

    /**
     * Compute n!, the<a href="http://mathworld.wolfram.com/Factorial.html">
     * factorial</a> of {@code n} (the product of the numbers 1 to n), as a {@code double}.
     * The result should be small enough to fit into a {@code double}: The
     * largest {@code n} for which {@code n! < Double.MAX_VALUE} is 170.
     * If the computed value exceeds {@code Double.MAX_VALUE}, {@code Double.POSITIVE_INFINITY} is returned.
     *
     * @param n
     *        Argument.
     * @return {@code n!}
     * @throws NotPositiveException
     *         if {@code n < 0}.
     */
    public static double factorialDouble(final int n) {
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.FACTORIAL_NEGATIVE_PARAMETER,
                n);
        }
        if (n < 21) {
            return FACTORIALS[n];
        }
        return MathLib.floor(MathLib.exp(ArithmeticUtils.factorialLog(n)) + 0.5);
    }

    /**
     * Compute the natural logarithm of the factorial of {@code n}.
     *
     * @param n
     *        Argument.
     * @return {@code n!}
     * @throws NotPositiveException
     *         if {@code n < 0}.
     */
    public static double factorialLog(final int n) {
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.FACTORIAL_NEGATIVE_PARAMETER,
                n);
        }
        if (n < 21) {
            return MathLib.log(FACTORIALS[n]);
        }
        double logSum = 0;
        for (int i = 2; i <= n; i++) {
            logSum += MathLib.log(i);
        }
        return logSum;
    }

    /**
     * Computes the greatest common divisor of the absolute value of two
     * numbers, using a modified version of the "binary gcd" method.
     * See Knuth 4.5.2 algorithm B.
     * The algorithm is due to Josef Stein (1961). <br/>
     * Special cases:
     * <ul>
     * <li>The invocations {@code gcd(Integer.MIN_VALUE, Integer.MIN_VALUE)}, {@code gcd(Integer.MIN_VALUE, 0)} and
     * {@code gcd(0, Integer.MIN_VALUE)} throw an {@code ArithmeticException}, because the result would be 2^31, which
     * is too large for an int value.</li>
     * <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and {@code gcd(x, 0)} is the absolute value of {@code x},
     * except for the special cases above.</li>
     * <li>The invocation {@code gcd(0, 0)} is the only one which returns {@code 0}.</li>
     * </ul>
     *
     * @param p
     *        Number.
     * @param q
     *        Number.
     * @return the greatest common divisor (never negative).
     * @throws MathArithmeticException
     *         if the result cannot be represented as
     *         a non-negative {@code int} value.
     * @since 1.1
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    public static int gcd(final int p, final int q) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        int a = p;
        int b = q;
        if (a == 0 || b == 0) {
            if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE) {
                throw new MathArithmeticException(PatriusMessages.GCD_OVERFLOW_32_BITS,
                    p, q);
            }
            return MathLib.abs(a + b);
        }

        long al = a;
        long bl = b;
        boolean useLong = false;
        if (a < 0) {
            if (Integer.MIN_VALUE == a) {
                useLong = true;
            } else {
                a = -a;
            }
            al = -al;
        }
        if (b < 0) {
            if (Integer.MIN_VALUE == b) {
                useLong = true;
            } else {
                b = -b;
            }
            bl = -bl;
        }
        if (useLong) {
            if (al == bl) {
                throw new MathArithmeticException(PatriusMessages.GCD_OVERFLOW_32_BITS,
                    p, q);
            }
            long blbu = bl;
            bl = al;
            al = blbu % al;
            if (al == 0) {
                if (bl > Integer.MAX_VALUE) {
                    throw new MathArithmeticException(PatriusMessages.GCD_OVERFLOW_32_BITS,
                        p, q);
                }
                return (int) bl;
            }
            blbu = bl;

            // Now "al" and "bl" fit in an "int".
            b = (int) al;
            a = (int) (blbu % al);
        }

        return gcdPositive(a, b);
    }

    /**
     * Computes the greatest common divisor of two <em>positive</em> numbers
     * (this precondition is <em>not</em> checked and the result is undefined
     * if not fulfilled) using the "binary gcd" method which avoids division
     * and modulo operations.
     * See Knuth 4.5.2 algorithm B.
     * The algorithm is due to Josef Stein (1961). <br/>
     * Special cases:
     * <ul>
     * <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and {@code gcd(x, 0)} is the value of {@code x}.</li>
     * <li>The invocation {@code gcd(0, 0)} is the only one which returns {@code 0}.</li>
     * </ul>
     *
     * @param aIn
     *        Positive number.
     * @param bIn
     *        Positive number.
     * @return the greatest common divisor.
     */
    private static int gcdPositive(final int aIn, final int bIn) {

        int a = aIn;
        int b = bIn;

        final int res;
        if (a == 0) {
            res = b;
        } else if (b == 0) {
            res = a;
        } else {

            // Make "a" and "b" odd, keeping track of common power of 2.
            final int aTwos = Integer.numberOfTrailingZeros(a);
            a >>= aTwos;
            final int bTwos = Integer.numberOfTrailingZeros(b);
            b >>= bTwos;
            final int shift = Math.min(aTwos, bTwos);

            // "a" and "b" are positive.
            // If a > b then "gdc(a, b)" is equal to "gcd(a - b, b)".
            // If a < b then "gcd(a, b)" is equal to "gcd(b - a, a)".
            // Hence, in the successive iterations:
            // "a" becomes the absolute difference of the current values,
            // "b" becomes the minimum of the current values.
            while (a != b) {
                final int delta = a - b;
                b = Math.min(a, b);
                a = Math.abs(delta);

                // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
                a >>= Integer.numberOfTrailingZeros(a);
            }

            // Recover the common power of 2.
            res = a << shift;
        }
        return res;
    }

    /**
     * <p>
     * Gets the greatest common divisor of the absolute value of two numbers, using the "binary gcd" method which avoids
     * division and modulo operations. See Knuth 4.5.2 algorithm B. This algorithm is due to Josef Stein (1961).
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code gcd(Long.MIN_VALUE, Long.MIN_VALUE)}, {@code gcd(Long.MIN_VALUE, 0L)} and
     * {@code gcd(0L, Long.MIN_VALUE)} throw an {@code ArithmeticException}, because the result would be 2^63, which is
     * too large for a long value.</li>
     * <li>The result of {@code gcd(x, x)}, {@code gcd(0L, x)} and {@code gcd(x, 0L)} is the absolute value of {@code x}
     * , except for the special cases above.
     * <li>The invocation {@code gcd(0L, 0L)} is the only one which returns {@code 0L}.</li>
     * </ul>
     *
     * @param p
     *        Number.
     * @param q
     *        Number.
     * @return the greatest common divisor, never negative.
     * @throws MathArithmeticException
     *         if the result cannot be represented as
     *         a non-negative {@code long} value.
     * @since 2.1
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static long gcd(final long p, final long q) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        long u = p;
        long v = q;
        if ((u == 0) || (v == 0)) {
            if ((u == Long.MIN_VALUE) || (v == Long.MIN_VALUE)) {
                throw new MathArithmeticException(PatriusMessages.GCD_OVERFLOW_64_BITS,
                    p, q);
            }
            return MathLib.abs(u) + MathLib.abs(v);
        }
        // keep u and v negative, as negative integers range down to
        // -2^63, while positive numbers can only be as large as 2^63-1
        // (i.e. we can't necessarily negate a negative number without
        // overflow)
        /* assert u!=0 && v!=0; */
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
          // B1. [Find power of 2]
        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0) {
            // while u and v are both even
            u /= 2;
            v /= 2;
            // cast out twos.
            k++;
            if (k == 63) {
                break;
            }
        }
        if (k == 63) {
            throw new MathArithmeticException(PatriusMessages.GCD_OVERFLOW_64_BITS,
                p, q);
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        // one is odd.
        long t = ((u & 1) == 1) ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            while ((t & 1) == 0) {
                // while t is even..
                // cast out twos
                t /= 2;
            }
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = (v - u) / 2;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        // gcd is u*2^k
        return -u * (1L << k);
    }

    /**
     * <p>
     * Returns the least common multiple of the absolute value of two numbers, using the formula
     * {@code lcm(a,b) = (a / gcd(a,b)) * b}.
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Integer.MIN_VALUE, n)} and {@code lcm(n, Integer.MIN_VALUE)}, where {@code abs(n)}
     * is a power of 2, throw an {@code ArithmeticException}, because the result would be 2^31, which is too large for
     * an int value.</li>
     * <li>The result of {@code lcm(0, x)} and {@code lcm(x, 0)} is {@code 0} for any {@code x}.
     * </ul>
     *
     * @param a
     *        Number.
     * @param b
     *        Number.
     * @return the least common multiple, never negative.
     * @throws MathArithmeticException
     *         if the result cannot be represented as
     *         a non-negative {@code int} value.
     * @since 1.1
     */
    public static int lcm(final int a, final int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        final int lcm = MathLib.abs(ArithmeticUtils.mulAndCheck(a / gcd(a, b), b));
        if (lcm == Integer.MIN_VALUE) {
            throw new MathArithmeticException(PatriusMessages.LCM_OVERFLOW_32_BITS,
                a, b);
        }
        return lcm;
    }

    /**
     * <p>
     * Returns the least common multiple of the absolute value of two numbers, using the formula
     * {@code lcm(a,b) = (a / gcd(a,b)) * b}.
     * </p>
     * Special cases:
     * <ul>
     * <li>The invocations {@code lcm(Long.MIN_VALUE, n)} and {@code lcm(n, Long.MIN_VALUE)}, where {@code abs(n)} is a
     * power of 2, throw an {@code ArithmeticException}, because the result would be 2^63, which is too large for an int
     * value.</li>
     * <li>The result of {@code lcm(0L, x)} and {@code lcm(x, 0L)} is {@code 0L} for any {@code x}.
     * </ul>
     *
     * @param a
     *        Number.
     * @param b
     *        Number.
     * @return the least common multiple, never negative.
     * @throws MathArithmeticException
     *         if the result cannot be represented
     *         as a non-negative {@code long} value.
     * @since 2.1
     */
    public static long lcm(final long a, final long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        final long lcm = MathLib.abs(ArithmeticUtils.mulAndCheck(a / gcd(a, b), b));
        if (lcm == Long.MIN_VALUE) {
            throw new MathArithmeticException(PatriusMessages.LCM_OVERFLOW_64_BITS,
                a, b);
        }
        return lcm;
    }

    /**
     * Multiply two integers, checking for overflow.
     *
     * @param x
     *        Factor.
     * @param y
     *        Factor.
     * @return the product {@code x * y}.
     * @throws MathArithmeticException
     *         if the result can not be
     *         represented as an {@code int}.
     * @since 1.1
     */
    public static int mulAndCheck(final int x, final int y) {
        final long m = ((long) x) * ((long) y);
        if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
            throw new MathArithmeticException();
        }
        return (int) m;
    }

    /**
     * Multiply two long integers, checking for overflow.
     *
     * @param a
     *        Factor.
     * @param b
     *        Factor.
     * @return the product {@code a * b}.
     * @throws MathArithmeticException
     *         if the result can not be represented
     *         as a {@code long}.
     * @since 1.2
     */
    public static long mulAndCheck(final long a, final long b) {
        final long ret;
        if (a > b) {
            // use symmetry to reduce boundary cases
            ret = mulAndCheck(b, a);
        } else {
            if (a < 0) {
                if (b < 0) {
                    // check for positive overflow with negative a, negative b
                    if (a >= Long.MAX_VALUE / b) {
                        ret = a * b;
                    } else {
                        throw new MathArithmeticException();
                    }
                } else if (b > 0) {
                    // check for negative overflow with negative a, positive b
                    if (Long.MIN_VALUE / b <= a) {
                        ret = a * b;
                    } else {
                        throw new MathArithmeticException();

                    }
                } else {
                    // assert b == 0
                    ret = 0;
                }
            } else if (a > 0) {
                // assert a > 0
                // assert b > 0

                // check for positive overflow with positive a, positive b
                if (a <= Long.MAX_VALUE / b) {
                    ret = a * b;
                } else {
                    throw new MathArithmeticException();
                }
            } else {
                // assert a == 0
                ret = 0;
            }
        }
        return ret;
    }

    /**
     * Subtract two integers, checking for overflow.
     *
     * @param x
     *        Minuend.
     * @param y
     *        Subtrahend.
     * @return the difference {@code x - y}.
     * @throws MathArithmeticException
     *         if the result can not be represented
     *         as an {@code int}.
     * @since 1.1
     */
    public static int subAndCheck(final int x, final int y) {
        final long s = (long) x - (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW_IN_SUBTRACTION, x, y);
        }
        return (int) s;
    }

    /**
     * Subtract two long integers, checking for overflow.
     *
     * @param a
     *        Value.
     * @param b
     *        Value.
     * @return the difference {@code a - b}.
     * @throws MathArithmeticException
     *         if the result can not be represented as a {@code long}.
     * @since 1.2
     */
    public static long subAndCheck(final long a, final long b) {
        final long ret;
        if (b == Long.MIN_VALUE) {
            if (a < 0) {
                ret = a - b;
            } else {
                throw new MathArithmeticException(PatriusMessages.OVERFLOW_IN_ADDITION, a, -b);
            }
        } else {
            // use additive inverse
            ret = addAndCheck(a, -b, PatriusMessages.OVERFLOW_IN_ADDITION);
        }
        return ret;
    }

    /**
     * Raise an int to an int power.
     *
     * @param k
     *        Number to raise.
     * @param eIn
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static int pow(final int k, final int eIn) {

        int e = eIn;
        if (e < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        int result = 1;
        int k2p = k;
        while (e != 0) {
            if ((e & 0x1) != 0) {
                result *= k2p;
            }
            k2p *= k2p;
            e = e >> 1;
        }

        return result;
    }

    /**
     * Raise an int to a long power.
     *
     * @param k
     *        Number to raise.
     * @param eIn
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static int pow(final int k, final long eIn) {

        long e = eIn;
        if (e < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        int result = 1;
        int k2p = k;
        while (e != 0) {
            if ((e & 0x1) != 0) {
                result *= k2p;
            }
            k2p *= k2p;
            e = e >> 1;
        }

        return result;
    }

    /**
     * Raise a long to an int power.
     *
     * @param k
     *        Number to raise.
     * @param eIn
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static long pow(final long k, final int eIn) {
        int e = eIn;
        if (e < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        long result = 1L;
        long k2p = k;
        while (e != 0) {
            if ((e & 0x1) != 0) {
                result *= k2p;
            }
            k2p *= k2p;
            e = e >> 1;
        }

        return result;
    }

    /**
     * Raise a long to a long power.
     *
     * @param k
     *        Number to raise.
     * @param eIn
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static long pow(final long k, final long eIn) {
        long e = eIn;
        if (e < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        long result = 1L;
        long k2p = k;
        while (e != 0) {
            if ((e & 0x1) != 0) {
                result *= k2p;
            }
            k2p *= k2p;
            e = e >> 1;
        }

        return result;
    }

    /**
     * Raise a BigInteger to an int power.
     *
     * @param k
     *        Number to raise.
     * @param e
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static BigInteger pow(final BigInteger k, final int e) {
        if (e < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        return k.pow(e);
    }

    /**
     * Raise a BigInteger to a long power.
     *
     * @param k
     *        Number to raise.
     * @param eIn
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static BigInteger pow(final BigInteger k, final long eIn) {
        long e = eIn;
        if (e < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        BigInteger result = BigInteger.ONE;
        BigInteger k2p = k;
        while (e != 0) {
            if ((e & 0x1) != 0) {
                result = result.multiply(k2p);
            }
            k2p = k2p.multiply(k2p);
            e = e >> 1;
        }

        return result;

    }

    /**
     * Raise a BigInteger to a BigInteger power.
     *
     * @param k
     *        Number to raise.
     * @param eIn
     *        Exponent (must be positive or zero).
     * @return k<sup>e</sup>
     * @throws NotPositiveException
     *         if {@code e < 0}.
     */
    public static BigInteger pow(final BigInteger k, final BigInteger eIn) {
        BigInteger e = eIn;
        if (e.compareTo(BigInteger.ZERO) < 0) {
            throw new NotPositiveException(PatriusMessages.EXPONENT, e);
        }

        BigInteger result = BigInteger.ONE;
        BigInteger k2p = k;
        while (!BigInteger.ZERO.equals(e)) {
            if (e.testBit(0)) {
                result = result.multiply(k2p);
            }
            k2p = k2p.multiply(k2p);
            e = e.shiftRight(1);
        }

        return result;
    }

    /**
     * Returns the <a
     * href="http://mathworld.wolfram.com/StirlingNumberoftheSecondKind.html">
     * Stirling number of the second kind</a>, "{@code S(n,k)}", the number of
     * ways of partitioning an {@code n}-element set into {@code k} non-empty
     * subsets.
     * <p>
     * The preconditions are {@code 0 <= k <= n } (otherwise {@code NotPositiveException} is thrown)
     * </p>
     *
     * @param n
     *        the size of the set
     * @param k
     *        the number of non-empty subsets
     * @return {@code S(n,k)}
     * @throws NotPositiveException
     *         if {@code k < 0}.
     * @throws NumberIsTooLargeException
     *         if {@code k > n}.
     * @throws MathArithmeticException
     *         if some overflow happens, typically for n exceeding 25 and
     *         k between 20 and n-2 (S(n,n-1) is handled specifically and does not overflow)
     * @since 3.1
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop NestedBlockDepth check
    // Reason: Commons-Math code kept as such
    public static long stirlingS2(final int n, final int k) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        if (k < 0) {
            throw new NotPositiveException(k);
        }
        if (k > n) {
            throw new NumberIsTooLargeException(k, n, true);
        }

        long[][] stirlingS2 = STIRLING_S2.get();

        if (stirlingS2 == null) {
            // the cache has never been initialized, compute the first numbers
            // by direct recurrence relation

            // as S(26,9) = 11201516780955125625 is larger than Long.MAX_VALUE
            // we must stop computation at row 26
            final int maxIndex = 26;
            stirlingS2 = new long[maxIndex][];
            stirlingS2[0] = new long[] { 1L };
            for (int i = 1; i < stirlingS2.length; ++i) {
                stirlingS2[i] = new long[i + 1];
                stirlingS2[i][0] = 0;
                stirlingS2[i][1] = 1;
                stirlingS2[i][i] = 1;
                for (int j = 2; j < i; ++j) {
                    stirlingS2[i][j] = j * stirlingS2[i - 1][j] + stirlingS2[i - 1][j - 1];
                }
            }

            // atomically save the cache
            STIRLING_S2.compareAndSet(null, stirlingS2);

        }

        if (n < stirlingS2.length) {
            // the number is in the small cache
            return stirlingS2[n][k];
        }

        // use explicit formula to compute the number without caching it
        if (k == 0) {
            return 0;
        } else if (k == 1 || k == n) {
            return 1;
        } else if (k == 2) {
            return (1L << (n - 1)) - 1L;
        } else if (k == n - 1) {
            return binomialCoefficient(n, 2);
        } else {
            // definition formula: note that this may trigger some overflow
            long sum = 0;
            long sign = ((k & 0x1) == 0) ? 1 : -1;
            for (int j = 1; j <= k; ++j) {
                sign = -sign;
                sum += sign * binomialCoefficient(k, j) * pow(j, n);
                if (sum < 0) {
                    // there was an overflow somewhere
                    throw new MathArithmeticException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN,
                        n, 0, stirlingS2.length - 1);
                }
            }
            return sum / factorial(k);
        }
    }

    // CHECKSTYLE: resume NestedBlockDepth check

    /**
     * Add two long integers, checking for overflow.
     *
     * @param a
     *        Addend.
     * @param b
     *        Addend.
     * @param pattern
     *        Pattern to use for any thrown exception.
     * @return the sum {@code a + b}.
     * @throws MathArithmeticException
     *         if the result cannot be represented
     *         as a {@code long}.
     * @since 1.2
     */
    private static long addAndCheck(final long a, final long b,
                                    final Localizable pattern) {
        final long ret;
        if (a > b) {
            // use symmetry to reduce boundary cases
            ret = addAndCheck(b, a, pattern);
        } else {
            // assert a <= b

            if (a < 0) {
                if (b < 0) {
                    // check for negative overflow
                    if (Long.MIN_VALUE - b <= a) {
                        ret = a + b;
                    } else {
                        throw new MathArithmeticException(pattern, a, b);
                    }
                } else {
                    // opposite sign addition is always safe
                    ret = a + b;
                }
            } else {
                // assert a >= 0
                // assert b >= 0

                // check for positive overflow
                if (a <= Long.MAX_VALUE - b) {
                    ret = a + b;
                } else {
                    throw new MathArithmeticException(pattern, a, b);
                }
            }
        }
        return ret;
    }

    /**
     * Check binomial preconditions.
     *
     * @param n
     *        Size of the set.
     * @param k
     *        Size of the subsets to be counted.
     * @throws NotPositiveException
     *         if {@code n < 0}.
     * @throws NumberIsTooLargeException
     *         if {@code k > n}.
     */
    private static void checkBinomial(final int n, final int k) {
        if (n < k) {
            throw new NumberIsTooLargeException(PatriusMessages.BINOMIAL_INVALID_PARAMETERS_ORDER,
                k, n, true);
        }
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.BINOMIAL_NEGATIVE_PARAMETER, n);
        }
    }

    /**
     * Provide all the groups of groupSize elements among the provided elements.
     *
     * @param <T>
     *        The type of the elements
     * @param elements
     *        The elements
     * @param groupsSize
     *        The size of the group
     * @return all the combinations possible
     * @throws IllegalArgumentException
     *         if the groupSize exceeds the elements size
     */
    public static <T> List<List<T>> binomialCombinations(final List<T> elements, final int groupsSize) {
        if (groupsSize > elements.size()) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.BINOMIAL_COMBINATIONS_INVALID_GROUPS_SIZE);
        }
        final List<List<T>> output = new ArrayList<>();
        @SuppressWarnings("unchecked")
        final T[] array = (T[]) java.lang.reflect.Array.newInstance(elements.get(0).getClass(), groupsSize);
        recurtion(elements, groupsSize, 0, array, output);
        return output;
    }

    /**
     * Recursion method for the {@link #binomialCombinations} computation.
     *
     * @param <T>
     *        The type of the elements
     * @param elements
     *        The elements
     * @param groupsSize
     *        The size of the group
     * @param startPosition
     *        Working variable for the recursion
     * @param result
     *        Working variable for the recursion
     * @param results
     *        List of the combinations
     */
    private static <T> void recurtion(final List<T> elements, final int groupsSize, final int startPosition,
                                      final T[] result, final List<List<T>> results) {
        if (groupsSize == 0) {
            results.add(Arrays.asList(result.clone()));
            return;
        }
        for (int i = startPosition; i <= elements.size() - groupsSize; i++) {
            result[result.length - groupsSize] = elements.get(i);
            recurtion(elements, groupsSize - 1, i + 1, result, results);
        }
    }

    /**
     * Returns true if the argument is a power of two.
     *
     * @param n
     *        the number to test
     * @return true if the argument is a power of two
     */
    public static boolean isPowerOfTwo(final long n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check
}
