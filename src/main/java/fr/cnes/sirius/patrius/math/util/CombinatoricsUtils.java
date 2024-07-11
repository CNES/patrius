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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import java.util.concurrent.atomic.AtomicReference;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Commons-Math code

/**
 * Combinatorial utilities.
 * 
 * @version $Id: CombinatoricsUtils.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public final class CombinatoricsUtils {

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
    private static final AtomicReference<long[][]> STIRLING_S2 = new AtomicReference<long[][]>(null);

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Private constructor (class contains only static methods). */
    private CombinatoricsUtils() {
    }

    /**
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "{@code n choose k}", the number of {@code k}-element subsets that can be selected from an
     * {@code n}-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> {@code 0 <= k <= n } (otherwise {@code MathIllegalArgumentException} is thrown)</li>
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
        CombinatoricsUtils.checkBinomial(n, k);
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
                final long d = ArithmeticUtils.gcd(i, j);
                result = (result / (j / d)) * (i / d);
                i++;
            }
        } else {
            // For n > 66, a result overflow might occur, so we check
            // the multiplication, taking care to not overflow
            // unnecessary.
            int i = n - k + 1;
            for (int j = 1; j <= k; j++) {
                final long d = ArithmeticUtils.gcd(i, j);
                result = ArithmeticUtils.mulAndCheck(result / (j / d), i / d);
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
        // safety check
        CombinatoricsUtils.checkBinomial(n, k);
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

        // initialize the output
        double result = 1d;
        for (int i = 1; i <= k; i++) {
            result *= (double) (n - k + i) / (double) i;
        }
        // return the floor of result rounded up
        return MathLib.floor(result + HALF);
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
        CombinatoricsUtils.checkBinomial(n, k);
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
        return MathLib.floor(MathLib.exp(CombinatoricsUtils.factorialLog(n)) + HALF);
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
    public static void checkBinomial(final int n,
                                     final int k) {
        if (n < k) {
            throw new NumberIsTooLargeException(PatriusMessages.BINOMIAL_INVALID_PARAMETERS_ORDER,
                k, n, true);
        }
        if (n < 0) {
            throw new NotPositiveException(PatriusMessages.BINOMIAL_NEGATIVE_PARAMETER, n);
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
