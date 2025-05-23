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
package fr.cnes.sirius.patrius.math.stat.inference;

import fr.cnes.sirius.patrius.math.distribution.ChiSquaredDistribution;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements Chi-Square test statistics.
 * 
 * <p>
 * This implementation handles both known and unknown distributions.
 * </p>
 * 
 * <p>
 * Two samples tests can be used when the distribution is unknown <i>a priori</i> but provided by one sample, or when
 * the hypothesis under test is that the two samples come from the same underlying distribution.
 * </p>
 * 
 * @version $Id: ChiSquareTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ChiSquareTest {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Threshold. */
    private static final double THRESHOLD = 10E-6;

    /**
     * Computes the <a href="http://www.itl.nist.gov/div898/handbook/eda/section3/eda35f.htm">
     * Chi-Square statistic</a> comparing <code>observed</code> and <code>expected</code> frequency counts.
     * <p>
     * This statistic can be used to perform a Chi-Square test evaluating the null hypothesis that the observed counts
     * follow the expected distribution.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Expected counts must all be positive.</li>
     * <li>Observed counts must all be &ge; 0.</li>
     * <li>The observed and expected arrays must have the same length and their common length must be at least 2.</li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * <p>
     * <strong>Note: </strong>This implementation rescales the <code>expected</code> array if necessary to ensure that
     * the sum of the expected and observed counts are equal.
     * </p>
     * 
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return chiSquare test statistic
     * @throws NotPositiveException
     *         if <code>observed</code> has negative entries
     * @throws NotStrictlyPositiveException
     *         if <code>expected</code> has entries that are
     *         not strictly positive
     * @throws DimensionMismatchException
     *         if the arrays length is less than 2
     */
    public double chiSquare(final double[] expected, final long[] observed) {

        // Check there is enough data
        if (expected.length < 2) {
            // Exception
            throw new DimensionMismatchException(expected.length, 2);
        }
        // Check data size is consistent
        if (expected.length != observed.length) {
            // Exception
            throw new DimensionMismatchException(expected.length, observed.length);
        }
        // Other checks
        MathArrays.checkPositive(expected);
        MathArrays.checkNonNegative(observed);

        // initialize expected and observed sums
        double sumExpected = 0d;
        double sumObserved = 0d;
        for (int i = 0; i < observed.length; i++) {
            sumExpected += expected[i];
            sumObserved += observed[i];
        }
        double ratio = 1.0d;
        boolean rescale = false;
        if (MathLib.abs(sumExpected - sumObserved) > THRESHOLD) {
            ratio = sumObserved / sumExpected;
            rescale = true;
        }
        double sumSq = 0.0d;
        for (int i = 0; i < observed.length; i++) {
            if (rescale) {
                final double dev = observed[i] - ratio * expected[i];
                sumSq += dev * dev / (ratio * expected[i]);
            } else {
                final double dev = observed[i] - expected[i];
                sumSq += dev * dev / expected[i];
            }
        }
        // Return result
        //
        return sumSq;
    }

    /**
     * Returns the <i>observed significance level</i>, or <a href=
     * "http://www.cas.lancs.ac.uk/glossary_v1.1/hyptest.html#pvalue">
     * p-value</a>, associated with a
     * <a href="http://www.itl.nist.gov/div898/handbook/eda/section3/eda35f.htm">
     * Chi-square goodness of fit test</a> comparing the <code>observed</code> frequency counts to those in the
     * <code>expected</code> array.
     * <p>
     * The number returned is the smallest significance level at which one can reject the null hypothesis that the
     * observed counts conform to the frequency distribution described by the expected counts.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Expected counts must all be positive.</li>
     * <li>Observed counts must all be &ge; 0.</li>
     * <li>The observed and expected arrays must have the same length and their common length must be at least 2.</li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * <p>
     * <strong>Note: </strong>This implementation rescales the <code>expected</code> array if necessary to ensure that
     * the sum of the expected and observed counts are equal.
     * </p>
     * 
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return p-value
     * @throws NotPositiveException
     *         if <code>observed</code> has negative entries
     * @throws NotStrictlyPositiveException
     *         if <code>expected</code> has entries that are
     *         not strictly positive
     * @throws DimensionMismatchException
     *         if the arrays length is less than 2
     * @throws MaxCountExceededException
     *         if an error occurs computing the p-value
     */
    public double chiSquareTest(final double[] expected, final long[] observed) {

        final ChiSquaredDistribution distribution =
            new ChiSquaredDistribution(expected.length - 1.0);
        return 1.0 - distribution.cumulativeProbability(this.chiSquare(expected, observed));
    }

    /**
     * Performs a <a href="http://www.itl.nist.gov/div898/handbook/eda/section3/eda35f.htm">
     * Chi-square goodness of fit test</a> evaluating the null hypothesis that the
     * observed counts conform to the frequency distribution described by the expected
     * counts, with significance level <code>alpha</code>. Returns true iff the null
     * hypothesis can be rejected with 100 * (1 - alpha) percent confidence.
     * <p>
     * <strong>Example:</strong><br>
     * To test the hypothesis that <code>observed</code> follows <code>expected</code> at the 99% level, use
     * </p>
     * <p>
     * <code>chiSquareTest(expected, observed, 0.01) </code>
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Expected counts must all be positive.</li>
     * <li>Observed counts must all be &ge; 0.</li>
     * <li>The observed and expected arrays must have the same length and their common length must be at least 2.
     * <li> <code> 0 &lt; alpha &lt; 0.5 </code></li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * <p>
     * <strong>Note: </strong>This implementation rescales the <code>expected</code> array if necessary to ensure that
     * the sum of the expected and observed counts are equal.
     * </p>
     * 
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     *         1 - alpha
     * @throws NotPositiveException
     *         if <code>observed</code> has negative entries
     * @throws NotStrictlyPositiveException
     *         if <code>expected</code> has entries that are
     *         not strictly positive
     * @throws DimensionMismatchException
     *         if the arrays length is less than 2
     * @throws OutOfRangeException
     *         if <code>alpha</code> is not in the range (0, 0.5]
     * @throws MaxCountExceededException
     *         if an error occurs computing the p-value
     */
    public boolean chiSquareTest(final double[] expected, final long[] observed,
                                 final double alpha) {

        if ((alpha <= 0) || (alpha > HALF)) {
            throw new OutOfRangeException(PatriusMessages.OUT_OF_BOUND_SIGNIFICANCE_LEVEL,
                alpha, 0, HALF);
        }
        return this.chiSquareTest(expected, observed) < alpha;
    }

    /**
     * Computes the Chi-Square statistic associated with a
     * <a href="http://www.itl.nist.gov/div898/handbook/prc/section4/prc45.htm">
     * chi-square test of independence</a> based on the input <code>counts</code> array, viewed as a two-way table.
     * <p>
     * The rows of the 2-way table are <code>count[0], ... , count[count.length - 1] </code>
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>All counts must be &ge; 0.</li>
     * <li>The count array must be rectangular (i.e. all count[i] subarrays must have the same length).</li>
     * <li>The 2-way table represented by <code>counts</code> must have at least 2 columns and at least 2 rows.</li>
     * </li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * @param counts
     *        array representation of 2-way table
     * @return chiSquare test statistic
     * @throws NullArgumentException
     *         if the array is null
     * @throws DimensionMismatchException
     *         if the array is not rectangular
     * @throws NotPositiveException
     *         if {@code counts} has negative entries
     */
    public double chiSquare(final long[][] counts) {

        // Check data
        checkArray(counts);

        // Define rows and columns sizes
        final int nRows = counts.length;
        final int nCols = counts[0].length;

        // compute row, column and total sums
        final double[] rowSum = new double[nRows];
        final double[] colSum = new double[nCols];
        double total = 0.0d;
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                // Sums data
                rowSum[row] += counts[row][col];
                colSum[col] += counts[row][col];
                total += counts[row][col];
            }
        }

        // compute expected counts and chi-square
        double sumSq = 0.0d;
        double expected = 0.0d;
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                expected = (rowSum[row] * colSum[col]) / total;
                sumSq += ((counts[row][col] - expected) *
                    (counts[row][col] - expected)) / expected;
            }
        }
        // Return result
        return sumSq;
    }

    /**
     * Returns the <i>observed significance level</i>, or <a href=
     * "http://www.cas.lancs.ac.uk/glossary_v1.1/hyptest.html#pvalue">
     * p-value</a>, associated with a
     * <a href="http://www.itl.nist.gov/div898/handbook/prc/section4/prc45.htm">
     * chi-square test of independence</a> based on the input <code>counts</code> array, viewed as a two-way table.
     * <p>
     * The rows of the 2-way table are <code>count[0], ... , count[count.length - 1] </code>
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>All counts must be &ge; 0.</li>
     * <li>The count array must be rectangular (i.e. all count[i] subarrays must have the same length).</li>
     * <li>The 2-way table represented by <code>counts</code> must have at least 2 columns and at least 2 rows.</li>
     * </li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * @param counts
     *        array representation of 2-way table
     * @return p-value
     * @throws NullArgumentException
     *         if the array is null
     * @throws DimensionMismatchException
     *         if the array is not rectangular
     * @throws NotPositiveException
     *         if {@code counts} has negative entries
     * @throws MaxCountExceededException
     *         if an error occurs computing the p-value
     */
    public double chiSquareTest(final long[][] counts) {

        checkArray(counts);
        final double df = ((double) counts.length - 1) * ((double) counts[0].length - 1);
        final ChiSquaredDistribution distribution;
        distribution = new ChiSquaredDistribution(df);
        return 1 - distribution.cumulativeProbability(this.chiSquare(counts));
    }

    /**
     * Performs a <a href="http://www.itl.nist.gov/div898/handbook/prc/section4/prc45.htm">
     * chi-square test of independence</a> evaluating the null hypothesis that the
     * classifications represented by the counts in the columns of the input 2-way table
     * are independent of the rows, with significance level <code>alpha</code>.
     * Returns true iff the null hypothesis can be rejected with 100 * (1 - alpha) percent
     * confidence.
     * <p>
     * The rows of the 2-way table are <code>count[0], ... , count[count.length - 1] </code>
     * </p>
     * <p>
     * <strong>Example:</strong><br>
     * To test the null hypothesis that the counts in <code>count[0], ... , count[count.length - 1] </code> all
     * correspond to the same underlying probability distribution at the 99% level, use
     * </p>
     * <p>
     * <code>chiSquareTest(counts, 0.01)</code>
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>All counts must be &ge; 0.</li>
     * <li>The count array must be rectangular (i.e. all count[i] subarrays must have the same length).</li>
     * <li>The 2-way table represented by <code>counts</code> must have at least 2 columns and at least 2 rows.</li>
     * </li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * @param counts
     *        array representation of 2-way table
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     *         1 - alpha
     * @throws NullArgumentException
     *         if the array is null
     * @throws DimensionMismatchException
     *         if the array is not rectangular
     * @throws NotPositiveException
     *         if {@code counts} has any negative entries
     * @throws OutOfRangeException
     *         if <code>alpha</code> is not in the range (0, 0.5]
     * @throws MaxCountExceededException
     *         if an error occurs computing the p-value
     */
    public boolean chiSquareTest(final long[][] counts, final double alpha) {

        if ((alpha <= 0) || (alpha > HALF)) {
            throw new OutOfRangeException(PatriusMessages.OUT_OF_BOUND_SIGNIFICANCE_LEVEL,
                alpha, 0, HALF);
        }
        return this.chiSquareTest(counts) < alpha;
    }

    /**
     * <p>
     * Computes a <a href="http://www.itl.nist.gov/div898/software/dataplot/refman1/auxillar/chi2samp.htm"> Chi-Square
     * two sample test statistic</a> comparing bin frequency counts in <code>observed1</code> and <code>observed2</code>
     * . The sums of frequency counts in the two samples are not required to be the same. The formula used to compute
     * the test statistic is
     * </p>
     * <code>
     * &sum;[(K * observed1[i] - observed2[i]/K)<sup>2</sup> / (observed1[i] + observed2[i])]
     * </code> where <br/>
     * <code>K = &sqrt;[&sum(observed2 / &sum;(observed1)]</code> </p>
     * <p>
     * This statistic can be used to perform a Chi-Square test evaluating the null hypothesis that both observed counts
     * follow the same distribution.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Observed counts must be non-negative.</li>
     * <li>Observed counts for a specific bin must not both be zero.</li>
     * <li>Observed counts for a specific sample must not all be 0.</li>
     * <li>The arrays <code>observed1</code> and <code>observed2</code> must have the same length and their common
     * length must be at least 2.</li>
     * </ul>
     * </p>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data set
     * @return chiSquare test statistic
     * @throws DimensionMismatchException
     *         the the length of the arrays does not match
     * @throws NotPositiveException
     *         if any entries in <code>observed1</code> or <code>observed2</code> are negative
     * @throws ZeroException
     *         if either all counts of <code>observed1</code> or <code>observed2</code> are zero, or if the count at
     *         some index is zero
     *         for both arrays
     * @since 1.2
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public double chiSquareDataSetsComparison(final long[] observed1, final long[] observed2) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Make sure lengths are same
        if (observed1.length < 2) {
            throw new DimensionMismatchException(observed1.length, 2);
        }
        if (observed1.length != observed2.length) {
            throw new DimensionMismatchException(observed1.length, observed2.length);
        }

        // Ensure non-negative counts
        MathArrays.checkNonNegative(observed1);
        MathArrays.checkNonNegative(observed2);

        // Compute and compare count sums
        long countSum1 = 0;
        long countSum2 = 0;
        for (int i = 0; i < observed1.length; i++) {
            countSum1 += observed1[i];
            countSum2 += observed2[i];
        }
        // Ensure neither sample is uniformly 0
        if (countSum1 == 0 || countSum2 == 0) {
            throw new ZeroException();
        }
        // Compare and compute weight only if different
        double weight = 0.0;
        final boolean unequalCounts = countSum1 != countSum2;
        if (unequalCounts) {
            weight = MathLib.sqrt((double) countSum1 / (double) countSum2);
        }
        // Compute ChiSquare statistic
        double sumSq = 0.0d;
        double dev = 0.0d;
        double obs1 = 0.0d;
        double obs2 = 0.0d;
        for (int i = 0; i < observed1.length; i++) {
            if (observed1[i] == 0 && observed2[i] == 0) {
                throw new ZeroException(PatriusMessages.OBSERVED_COUNTS_BOTTH_ZERO_FOR_ENTRY, i);
            }

            obs1 = observed1[i];
            obs2 = observed2[i];
            if (unequalCounts) {
                // apply weights
                dev = obs1 / weight - obs2 * weight;
            } else {
                dev = obs1 - obs2;
            }
            sumSq += (dev * dev) / (obs1 + obs2);
        }
        return sumSq;
    }

    /**
     * <p>
     * Returns the <i>observed significance level</i>, or <a href=
     * "http://www.cas.lancs.ac.uk/glossary_v1.1/hyptest.html#pvalue"> p-value</a>, associated with a Chi-Square two
     * sample test comparing bin frequency counts in <code>observed1</code> and <code>observed2</code>.
     * </p>
     * <p>
     * The number returned is the smallest significance level at which one can reject the null hypothesis that the
     * observed counts conform to the same distribution.
     * </p>
     * <p>
     * See {@link #chiSquareDataSetsComparison(long[], long[])} for details on the formula used to compute the test
     * statistic. The degrees of of freedom used to perform the test is one less than the common length of the input
     * observed count arrays.
     * </p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Observed counts must be non-negative.</li>
     * <li>Observed counts for a specific bin must not both be zero.</li>
     * <li>Observed counts for a specific sample must not all be 0.</li>
     * <li>The arrays <code>observed1</code> and <code>observed2</code> must have the same length and their common
     * length must be at least 2.</li>
     * </ul>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data set
     * @return p-value
     * @throws DimensionMismatchException
     *         the the length of the arrays does not match
     * @throws NotPositiveException
     *         if any entries in <code>observed1</code> or <code>observed2</code> are negative
     * @throws ZeroException
     *         if either all counts of <code>observed1</code> or <code>observed2</code> are zero, or if the count at
     *         the same index is zero
     *         for both arrays
     * @throws MaxCountExceededException
     *         if an error occurs computing the p-value
     * @since 1.2
     */
    public double chiSquareTestDataSetsComparison(final long[] observed1, final long[] observed2) {

        final ChiSquaredDistribution distribution;
        distribution = new ChiSquaredDistribution((double) observed1.length - 1);
        return 1 - distribution.cumulativeProbability(
            this.chiSquareDataSetsComparison(observed1, observed2));
    }

    /**
     * <p>
     * Performs a Chi-Square two sample test comparing two binned data sets. The test evaluates the null hypothesis that
     * the two lists of observed counts conform to the same frequency distribution, with significance level
     * <code>alpha</code>. Returns true iff the null hypothesis can be rejected with 100 * (1 - alpha) percent
     * confidence.
     * </p>
     * <p>
     * See {@link #chiSquareDataSetsComparison(long[], long[])} for details on the formula used to compute the Chisquare
     * statistic used in the test. The degrees of of freedom used to perform the test is one less than the common length
     * of the input observed count arrays.
     * </p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>Observed counts must be non-negative.</li>
     * <li>Observed counts for a specific bin must not both be zero.</li>
     * <li>Observed counts for a specific sample must not all be 0.</li>
     * <li>The arrays <code>observed1</code> and <code>observed2</code> must have the same length and their common
     * length must be at least 2.</li>
     * <li> <code> 0 < alpha < 0.5 </code></li>
     * </ul>
     * <p>
     * If any of the preconditions are not met, an <code>IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data set
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     *         1 - alpha
     * @throws DimensionMismatchException
     *         the the length of the arrays does not match
     * @throws NotPositiveException
     *         if any entries in <code>observed1</code> or <code>observed2</code> are negative
     * @throws ZeroException
     *         if either all counts of <code>observed1</code> or <code>observed2</code> are zero, or if the count at
     *         the same index is zero
     *         for both arrays
     * @throws OutOfRangeException
     *         if <code>alpha</code> is not in the range (0, 0.5]
     * @throws MaxCountExceededException
     *         if an error occurs performing the test
     * @since 1.2
     */
    public boolean chiSquareTestDataSetsComparison(final long[] observed1,
                                                   final long[] observed2, final double alpha) {

        if (alpha <= 0 ||
            alpha > HALF) {
            throw new OutOfRangeException(PatriusMessages.OUT_OF_BOUND_SIGNIFICANCE_LEVEL,
                alpha, 0, HALF);
        }
        return this.chiSquareTestDataSetsComparison(observed1, observed2) < alpha;
    }

    /**
     * Checks to make sure that the input long[][] array is rectangular,
     * has at least 2 rows and 2 columns, and has all non-negative entries.
     * 
     * @param array
     *        input 2-way table to check
     * @throws NullArgumentException
     *         if the array is null
     * @throws DimensionMismatchException
     *         if the array is not valid
     * @throws NotPositiveException
     *         if the array contains any negative entries
     */
    private static void checkArray(final long[][] array) {

        if (array.length < 2) {
            throw new DimensionMismatchException(array.length, 2);
        }
        if (array[0].length < 2) {
            throw new DimensionMismatchException(array[0].length, 2);
        }

        MathArrays.checkRectangular(array);
        MathArrays.checkNonNegative(array);
    }
}
