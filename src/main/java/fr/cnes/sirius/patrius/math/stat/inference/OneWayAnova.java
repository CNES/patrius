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

import java.util.Collection;

import fr.cnes.sirius.patrius.math.distribution.FDistribution;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfSquares;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements one-way ANOVA (analysis of variance) statistics.
 * 
 * <p>
 * Tests for differences between two or more categories of univariate data (for example, the body mass index of
 * accountants, lawyers, doctors and computer programmers). When two categories are given, this is equivalent to the
 * {@link fr.cnes.sirius.patrius.math.stat.inference.TTest}.
 * </p>
 * <p>
 * Uses the {@link fr.cnes.sirius.patrius.math.distribution.FDistribution
 * commons-math F Distribution implementation} to estimate exact p-values.
 * </p>
 * <p>
 * This implementation is based on a description at http://faculty.vassar.edu/lowry/ch13pt1.html
 * </p>
 * 
 * <pre>
 * Abbreviations: bg = between groups,
 *                wg = within groups,
 *                ss = sum squared deviations
 * </pre>
 * 
 * @since 1.2
 * @version $Id: OneWayAnova.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class OneWayAnova {

    /** 0.5. */
    private static final double HALF = 0.5;

    /**
     * Computes the ANOVA F-value for a collection of <code>double[]</code> arrays.
     * 
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>The categoryData <code>Collection</code> must contain <code>double[]</code> arrays.</li>
     * <li>There must be at least two <code>double[]</code> arrays in the <code>categoryData</code> collection and each
     * of these arrays must contain at least two values.</li>
     * </ul>
     * </p>
     * <p>
     * This implementation computes the F statistic using the definitional formula
     * 
     * <pre>
     * F = msbg / mswg
     * </pre>
     * 
     * where
     * 
     * <pre>
     *  msbg = between group mean square
     *  mswg = within group mean square
     * </pre>
     * 
     * are as defined <a href="http://faculty.vassar.edu/lowry/ch13pt1.html"> here</a>
     * </p>
     * 
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @return Fvalue
     * @throws NullArgumentException
     *         if <code>categoryData</code> is <code>null</code>
     * @throws DimensionMismatchException
     *         if the length of the <code>categoryData</code> array is less than 2 or a contained <code>double[]</code>
     *         array does not have
     *         at least two values
     */
    public double anovaFValue(final Collection<double[]> categoryData) {
        return this.anovaStats(categoryData).f;

    }

    /**
     * Computes the ANOVA P-value for a collection of <code>double[]</code> arrays.
     * 
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>The categoryData <code>Collection</code> must contain <code>double[]</code> arrays.</li>
     * <li>There must be at least two <code>double[]</code> arrays in the <code>categoryData</code> collection and each
     * of these arrays must contain at least two values.</li>
     * </ul>
     * </p>
     * <p>
     * This implementation uses the {@link fr.cnes.sirius.patrius.math.distribution.FDistribution
     * commons-math F Distribution implementation} to estimate the exact p-value, using the formula
     * 
     * <pre>
     * p = 1 - cumulativeProbability(F)
     * </pre>
     * 
     * where <code>F</code> is the F value and <code>cumulativeProbability</code> is the commons-math implementation of
     * the F distribution.
     * </p>
     * 
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @return Pvalue
     * @throws NullArgumentException
     *         if <code>categoryData</code> is <code>null</code>
     * @throws DimensionMismatchException
     *         if the length of the <code>categoryData</code> array is less than 2 or a contained <code>double[]</code>
     *         array does not have
     *         at least two values
     * @throws ConvergenceException
     *         if the p-value can not be computed due to a convergence error
     * @throws MaxCountExceededException
     *         if the maximum number of iterations is exceeded
     */
    public double anovaPValue(final Collection<double[]> categoryData) {

        final AnovaStats a = this.anovaStats(categoryData);
        // No try-catch or advertised exception because args are valid
        final FDistribution fdist = new FDistribution(a.dfbg, a.dfwg);
        return 1.0 - fdist.cumulativeProbability(a.f);

    }

    /**
     * Performs an ANOVA test, evaluating the null hypothesis that there
     * is no difference among the means of the data categories.
     * 
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>The categoryData <code>Collection</code> must contain <code>double[]</code> arrays.</li>
     * <li>There must be at least two <code>double[]</code> arrays in the <code>categoryData</code> collection and each
     * of these arrays must contain at least two values.</li>
     * <li>alpha must be strictly greater than 0 and less than or equal to 0.5.</li>
     * </ul>
     * </p>
     * <p>
     * This implementation uses the {@link fr.cnes.sirius.patrius.math.distribution.FDistribution
     * commons-math F Distribution implementation} to estimate the exact p-value, using the formula
     * 
     * <pre>
     * p = 1 - cumulativeProbability(F)
     * </pre>
     * 
     * where <code>F</code> is the F value and <code>cumulativeProbability</code> is the commons-math implementation of
     * the F distribution.
     * </p>
     * <p>
     * True is returned iff the estimated p-value is less than alpha.
     * </p>
     * 
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @param alpha
     *        significance level of the test
     * @return true if the null hypothesis can be rejected with
     *         confidence 1 - alpha
     * @throws NullArgumentException
     *         if <code>categoryData</code> is <code>null</code>
     * @throws DimensionMismatchException
     *         if the length of the <code>categoryData</code> array is less than 2 or a contained <code>double[]</code>
     *         array does not have
     *         at least two values
     * @throws OutOfRangeException
     *         if <code>alpha</code> is not in the range (0, 0.5]
     * @throws ConvergenceException
     *         if the p-value can not be computed due to a convergence error
     * @throws MaxCountExceededException
     *         if the maximum number of iterations is exceeded
     */
    public boolean anovaTest(final Collection<double[]> categoryData,
                             final double alpha) {

        if ((alpha <= 0) || (alpha > HALF)) {
            throw new OutOfRangeException(
                PatriusMessages.OUT_OF_BOUND_SIGNIFICANCE_LEVEL,
                alpha, 0, HALF);
        }
        return this.anovaPValue(categoryData) < alpha;

    }

    /**
     * This method actually does the calculations (except P-value).
     * 
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @return computed AnovaStats
     * @throws NullArgumentException
     *         if <code>categoryData</code> is <code>null</code>
     * @throws DimensionMismatchException
     *         if the length of the <code>categoryData</code> array is less than 2 or a contained <code>double[]</code>
     *         array does not contain
     *         at least two values
     */
    private AnovaStats anovaStats(final Collection<double[]> categoryData) {

        if (categoryData == null) {
            // No data
            throw new NullArgumentException();
        }

        // check if we have enough categories
        if (categoryData.size() < 2) {
            throw new DimensionMismatchException(
                PatriusMessages.TWO_OR_MORE_CATEGORIES_REQUIRED,
                categoryData.size(), 2);
        }

        // check if each category has enough data and all is double[]
        for (final double[] array : categoryData) {
            if (array.length <= 1) {
                throw new DimensionMismatchException(
                    PatriusMessages.TWO_OR_MORE_VALUES_IN_CATEGORY_REQUIRED,
                    array.length, 2);
            }
        }

        // Init loop data
        int dfwg = 0;
        double sswg = 0;
        final Sum totsum = new Sum();
        final SumOfSquares totsumsq = new SumOfSquares();
        int totnum = 0;

        for (final double[] data : categoryData) {
            // Loop on data
            final Sum sum = new Sum();
            final SumOfSquares sumsq = new SumOfSquares();
            int num = 0;

            for (final double val : data) {
                // within category
                num++;
                sum.increment(val);
                sumsq.increment(val);

                // for all categories
                totnum++;
                totsum.increment(val);
                totsumsq.increment(val);
            }
            dfwg += num - 1;
            final double ss = sumsq.getResult() - sum.getResult() * sum.getResult() / num;
            sswg += ss;
        }
        // Build results
        final double sst = totsumsq.getResult() - totsum.getResult() *
            totsum.getResult() / totnum;
        final double ssbg = sst - sswg;
        final int dfbg = categoryData.size() - 1;
        final double msbg = ssbg / dfbg;
        final double mswg = sswg / dfwg;
        final double f = msbg / mswg;

        // Return result
        return new AnovaStats(dfbg, dfwg, f);
    }

    /**
     * Convenience class to pass dfbg,dfwg,F values around within OneWayAnova.
     * No get/set methods provided.
     */
    private static final class AnovaStats {

        /** Degrees of freedom in numerator (between groups). */
        private final int dfbg;

        /** Degrees of freedom in denominator (within groups). */
        private final int dfwg;

        /** Statistic. */
        private final double f;

        /**
         * Constructor
         * 
         * @param dfbgIn
         *        degrees of freedom in numerator (between groups)
         * @param dfwgIn
         *        degrees of freedom in denominator (within groups)
         * @param fIn
         *        statistic
         */
        private AnovaStats(final int dfbgIn, final int dfwgIn, final double fIn) {
            this.dfbg = dfbgIn;
            this.dfwg = dfwgIn;
            this.f = fIn;
        }
    }

}
