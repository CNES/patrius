/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
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
package fr.cnes.sirius.patrius.math.stat.inference;

import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.stat.ranking.NaNStrategy;
import fr.cnes.sirius.patrius.math.stat.ranking.NaturalRanking;
import fr.cnes.sirius.patrius.math.stat.ranking.TiesStrategy;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * An implementation of the Wilcoxon signed-rank test.
 * 
 * @version $Id: WilcoxonSignedRankTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class WilcoxonSignedRankTest {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 30. */
    private static final double THIRTY = 30;

    /** Ranking algorithm. */
    private final NaturalRanking naturalRanking;

    /**
     * Create a test instance where NaN's are left in place and ties get
     * the average of applicable ranks. Use this unless you are very sure
     * of what you are doing.
     */
    public WilcoxonSignedRankTest() {
        this.naturalRanking = new NaturalRanking(NaNStrategy.FIXED,
            TiesStrategy.AVERAGE);
    }

    /**
     * Create a test instance using the given strategies for NaN's and ties.
     * Only use this if you are sure of what you are doing.
     * 
     * @param nanStrategy
     *        specifies the strategy that should be used for Double.NaN's
     * @param tiesStrategy
     *        specifies the strategy that should be used for ties
     */
    public WilcoxonSignedRankTest(final NaNStrategy nanStrategy,
        final TiesStrategy tiesStrategy) {
        this.naturalRanking = new NaturalRanking(nanStrategy, tiesStrategy);
    }

    /**
     * Ensures that the provided arrays fulfills the assumptions.
     * 
     * @param x
     *        first sample
     * @param y
     *        second sample
     * @throws NullArgumentException
     *         if {@code x} or {@code y} are {@code null}.
     * @throws NoDataException
     *         if {@code x} or {@code y} are zero-length.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} do not
     *         have the same length.
     */
    private void ensureDataConformance(final double[] x, final double[] y) {

        if (x == null ||
            y == null) {
            throw new NullArgumentException();
        }
        if (x.length == 0 ||
            y.length == 0) {
            throw new NoDataException();
        }
        if (y.length != x.length) {
            throw new DimensionMismatchException(y.length, x.length);
        }
    }

    /**
     * Calculates y[i] - x[i] for all i
     * 
     * @param x
     *        first sample
     * @param y
     *        second sample
     * @return z = y - x
     */
    private double[] calculateDifferences(final double[] x, final double[] y) {

        final double[] z = new double[x.length];

        for (int i = 0; i < x.length; ++i) {
            z[i] = y[i] - x[i];
        }

        return z;
    }

    /**
     * Calculates |z[i]| for all i
     * 
     * @param z
     *        sample
     * @return |z|
     * @throws NullArgumentException
     *         if {@code z} is {@code null}
     * @throws NoDataException
     *         if {@code z} is zero-length.
     */
    private double[] calculateAbsoluteDifferences(final double[] z) {

        if (z == null) {
            throw new NullArgumentException();
        }

        if (z.length == 0) {
            throw new NoDataException();
        }

        final double[] zAbs = new double[z.length];

        for (int i = 0; i < z.length; ++i) {
            zAbs[i] = MathLib.abs(z[i]);
        }

        return zAbs;
    }

    /**
     * Computes the <a
     * href="http://en.wikipedia.org/wiki/Wilcoxon_signed-rank_test">
     * Wilcoxon signed ranked statistic</a> comparing mean for two related
     * samples or repeated measurements on a single sample.
     * <p>
     * This statistic can be used to perform a Wilcoxon signed ranked test evaluating the null hypothesis that the two
     * related samples or repeated measurements on a single sample has equal mean.
     * </p>
     * <p>
     * Let X<sub>i</sub> denote the i'th individual of the first sample and Y<sub>i</sub> the related i'th individual in
     * the second sample. Let Z<sub>i</sub> = Y<sub>i</sub> - X<sub>i</sub>.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>The differences Z<sub>i</sub> must be independent.</li>
     * <li>Each Z<sub>i</sub> comes from a continuous population (they must be identical) and is symmetric about a
     * common median.</li>
     * <li>The values that X<sub>i</sub> and Y<sub>i</sub> represent are ordered, so the comparisons greater than, less
     * than, and equal to are meaningful.</li>
     * </ul>
     * </p>
     * 
     * @param x
     *        the first sample
     * @param y
     *        the second sample
     * @return wilcoxonSignedRank statistic (the larger of W+ and W-)
     * @throws NullArgumentException
     *         if {@code x} or {@code y} are {@code null}.
     * @throws NoDataException
     *         if {@code x} or {@code y} are zero-length.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} do not
     *         have the same length.
     */
    public double wilcoxonSignedRank(final double[] x, final double[] y) {

        // check input samples
        this.ensureDataConformance(x, y);

        // throws IllegalArgumentException if x and y are not correctly
        // specified
        final double[] z = this.calculateDifferences(x, y);
        final double[] zAbs = this.calculateAbsoluteDifferences(z);

        final double[] ranks = this.naturalRanking.rank(zAbs);

        double wplus = 0;

        // sum the non zero absolute differences 
        for (int i = 0; i < z.length; ++i) {
            if (z[i] > 0) {
                wplus += ranks[i];
            }
        }

        final int n = x.length;
        final double wminus = ((n * (n + 1)) / 2.0) - wplus;

        return MathLib.max(wplus, wminus);
    }

    /**
     * Algorithm inspired by
     * http://www.fon.hum.uva.nl/Service/Statistics/Signed_Rank_Algorihms.html#C
     * by Rob van Son, Institute of Phonetic Sciences & IFOTT,
     * University of Amsterdam
     * 
     * @param wMax
     *        largest Wilcoxon signed rank value
     * @param n
     *        number of subjects (corresponding to x.length)
     * @return two-sided exact p-value
     */
    private double calculateExactPValue(final double wMax, final int n) {

        // Total number of outcomes (equal to 2^N but a lot faster)
        final int m = 1 << n;

        int largerRankSums = 0;

        for (int i = 0; i < m; ++i) {
            int rankSum = 0;

            // Generate all possible rank sums
            for (int j = 0; j < n; ++j) {

                // (i >> j) & 1 extract i's j-th bit from the right
                if (((i >> j) & 1) == 1) {
                    rankSum += j + 1;
                }
            }

            if (rankSum >= wMax) {
                ++largerRankSums;
            }
        }

        /*
         * largerRankSums / m gives the one-sided p-value, so it's multiplied
         * with 2 to get the two-sided p-value
         */
        return 2 * ((double) largerRankSums) / (m);
    }

    /**
     * @param wMin
     *        smallest Wilcoxon signed rank value
     * @param n
     *        number of subjects (corresponding to x.length)
     * @return two-sided asymptotic p-value
     */
    private double calculateAsymptoticPValue(final double wMin, final int n) {

        final double es = n * (n + 1) / 4.0;

        /*
         * Same as (but saves computations):
         * final double VarW = ((double) (N * (N + 1) * (2*N + 1))) / 24;
         */
        final double varS = es * ((2 * n + 1) / 6.0);

        // - 0.5 is a continuity correction
        final double z = (wMin - es - HALF) / MathLib.sqrt(varS);

        // No try-catch or advertised exception because args are valid
        final NormalDistribution standardNormal = new NormalDistribution(0, 1);

        return 2 * standardNormal.cumulativeProbability(z);
    }

    /**
     * Returns the <i>observed significance level</i>, or <a href=
     * "http://www.cas.lancs.ac.uk/glossary_v1.1/hyptest.html#pvalue">
     * p-value</a>, associated with a <a
     * href="http://en.wikipedia.org/wiki/Wilcoxon_signed-rank_test">
     * Wilcoxon signed ranked statistic</a> comparing mean for two related
     * samples or repeated measurements on a single sample.
     * <p>
     * Let X<sub>i</sub> denote the i'th individual of the first sample and Y<sub>i</sub> the related i'th individual in
     * the second sample. Let Z<sub>i</sub> = Y<sub>i</sub> - X<sub>i</sub>.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>The differences Z<sub>i</sub> must be independent.</li>
     * <li>Each Z<sub>i</sub> comes from a continuous population (they must be identical) and is symmetric about a
     * common median.</li>
     * <li>The values that X<sub>i</sub> and Y<sub>i</sub> represent are ordered, so the comparisons greater than, less
     * than, and equal to are meaningful.</li>
     * </ul>
     * </p>
     * 
     * @param x
     *        the first sample
     * @param y
     *        the second sample
     * @param exactPValue
     *        if the exact p-value is wanted (only works for x.length <= 30,
     *        if true and x.length > 30, this is ignored because
     *        calculations may take too long)
     * @return p-value
     * @throws NullArgumentException
     *         if {@code x} or {@code y} are {@code null}.
     * @throws NoDataException
     *         if {@code x} or {@code y} are zero-length.
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} do not
     *         have the same length.
     * @throws NumberIsTooLargeException
     *         if {@code exactPValue} is {@code true} and {@code x.length} > 30
     * @throws ConvergenceException
     *         if the p-value can not be computed due to
     *         a convergence error
     * @throws MaxCountExceededException
     *         if the maximum number of iterations
     *         is exceeded
     */
    public double wilcoxonSignedRankTest(final double[] x, final double[] y,
                                         final boolean exactPValue) {

        this.ensureDataConformance(x, y);

        final int n = x.length;

        if (exactPValue && n > THIRTY) {
            throw new NumberIsTooLargeException(n, THIRTY, true);
        }

        final double wmax = this.wilcoxonSignedRank(x, y);

        if (exactPValue) {
            return this.calculateExactPValue(wmax, n);
        } else {
            final double wmin = (n * (n + 1) / 2.0) - wmax;
            return this.calculateAsymptoticPValue(wmin, n);
        }
    }
}
