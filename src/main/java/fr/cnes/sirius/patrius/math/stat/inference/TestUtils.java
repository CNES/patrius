/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
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

import fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary;

/**
 * A collection of static methods to create inference test instances or to
 * perform inference tests.
 * 
 * @since 1.1
 * @version $Id: TestUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ShortMethodName")
public final class TestUtils {

    /** Singleton TTest instance. */
    private static final TTest T_TEST = new TTest();

    /** Singleton ChiSquareTest instance. */
    private static final ChiSquareTest CHI_SQUARE_TEST = new ChiSquareTest();

    /** Singleton OneWayAnova instance. */
    private static final OneWayAnova ONE_WAY_ANANOVA = new OneWayAnova();

    /** Singleton G-Test instance. */
    private static final GTest G_TEST = new GTest();

    /**
     * Prevent instantiation.
     */
    private TestUtils() {
        super();
    }

    // CHECKSTYLE: stop JavadocMethodCheck

    /**
     * @see TTest#homoscedasticT(double[], double[])
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @return t statistic
     */
    public static double homoscedasticT(final double[] sample1, final double[] sample2) {
        return T_TEST.homoscedasticT(sample1, sample2);
    }

    /**
     * See TTest#
     * homoscedasticT(fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary,
     * fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary)
     * 
     * @param sampleStats1 StatisticalSummary describing data from the first sample
     * @param sampleStats2 StatisticalSummary describing data from the second sample
     * @return t statistic
     */
    public static double homoscedasticT(final StatisticalSummary sampleStats1,
                                        final StatisticalSummary sampleStats2) {
        return T_TEST.homoscedasticT(sampleStats1, sampleStats2);
    }

    /**
     * @see TTest#homoscedasticTTest(double[], double[], double)
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @param alpha significance level of the test
     * @return true if the null hypothesis can be rejected with confidence 1 - alpha
     */
    public static boolean homoscedasticTTest(final double[] sample1, final double[] sample2,
                                             final double alpha) {
        return T_TEST.homoscedasticTTest(sample1, sample2, alpha);
    }

    /**
     * @see TTest#homoscedasticTTest(double[], double[])
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @return t statistic
     */
    public static double homoscedasticTTest(final double[] sample1, final double[] sample2) {
        return T_TEST.homoscedasticTTest(sample1, sample2);
    }

    /**
     * See TTest#
     * homoscedasticTTest(fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary,
     * fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary)
     * 
     * @param sampleStats1 StatisticalSummary describing data from the first sample
     * @param sampleStats2 StatisticalSummary describing data from the second sample
     * @return t statistic
     */
    public static double homoscedasticTTest(final StatisticalSummary sampleStats1,
                                            final StatisticalSummary sampleStats2) {
        return T_TEST.homoscedasticTTest(sampleStats1, sampleStats2);
    }

    /**
     * @see TTest#pairedT(double[], double[])
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @return t statistic
     */
    public static double pairedT(final double[] sample1, final double[] sample2) {
        return T_TEST.pairedT(sample1, sample2);
    }

    /**
     * @see TTest#pairedTTest(double[], double[], double)
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @param alpha significance level of the test
     * @return true if the null hypothesis can be rejected with confidence 1 - alpha
     */
    public static boolean pairedTTest(final double[] sample1, final double[] sample2,
                                      final double alpha) {
        return T_TEST.pairedTTest(sample1, sample2, alpha);
    }

    /**
     * @see TTest#pairedTTest(double[], double[])
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @return t statistic
     */
    public static double pairedTTest(final double[] sample1, final double[] sample2) {
        return T_TEST.pairedTTest(sample1, sample2);
    }

    /**
     * @see TTest#t(double, double[])
     * @param mu
     *        comparison constant
     * @param observed
     *        array of values
     * @return t statistic
     */
    public static double t(final double mu, final double[] observed) {
        return T_TEST.t(mu, observed);
    }

    /**
     * @see TTest#t(double, fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary)
     * @param mu
     *        comparison constant
     * @param sampleStats
     *        DescriptiveStatistics holding sample summary statitstics
     * @return t statistic
     */
    public static double t(final double mu, final StatisticalSummary sampleStats) {
        return T_TEST.t(mu, sampleStats);
    }

    /**
     * @see TTest#t(double[], double[])
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @return t statistic
     */
    public static double t(final double[] sample1, final double[] sample2) {
        return T_TEST.t(sample1, sample2);
    }

    /**
     * See TTest#
     * t(fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary,
     * fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary)
     * 
     * @param sampleStats1 StatisticalSummary describing data from the first sample
     * @param sampleStats2 StatisticalSummary describing data from the second sample
     * @return t statistic
     */
    public static double t(final StatisticalSummary sampleStats1,
                           final StatisticalSummary sampleStats2) {
        return T_TEST.t(sampleStats1, sampleStats2);
    }

    /**
     * @see TTest#tTest(double, double[], double)
     * @param mu
     *        constant value to compare sample mean against
     * @param sample
     *        array of sample data values
     * @param alpha
     *        significance level of the test
     * @return p-value
     */
    public static boolean tTest(final double mu, final double[] sample, final double alpha) {
        return T_TEST.tTest(mu, sample, alpha);
    }

    /**
     * @see TTest#tTest(double, double[])
     * @param mu
     *        constant value to compare sample mean against
     * @param sample
     *        array of sample data values
     * @return p-value
     */
    public static double tTest(final double mu, final double[] sample) {
        return T_TEST.tTest(mu, sample);
    }

    /**
     * @see TTest#tTest(double, fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary, double)
     * @param mu
     *        constant value to compare sample mean against
     * @param sampleStats
     *        StatisticalSummary describing sample data values
     * @param alpha
     *        significance level of the test
     * @return p-value
     */
    public static boolean tTest(final double mu, final StatisticalSummary sampleStats,
                                final double alpha) {
        return T_TEST.tTest(mu, sampleStats, alpha);
    }

    /**
     * @see TTest#tTest(double, fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary)
     * @param mu
     *        constant value to compare sample mean against
     * @param sampleStats
     *        StatisticalSummary describing sample data
     * @return p-value
     */
    public static double tTest(final double mu, final StatisticalSummary sampleStats) {
        return T_TEST.tTest(mu, sampleStats);
    }

    /**
     * @see TTest#tTest(double[], double[], double)
     * @param sample1
     *        array of sample data values
     * @param sample2
     *        array of sample data values
     * @param alpha
     *        significance level of the test
     * @return true if the null hypothesis can be rejected with
     *         confidence 1 - alpha
     */
    public static boolean tTest(final double[] sample1, final double[] sample2,
                                final double alpha) {
        return T_TEST.tTest(sample1, sample2, alpha);
    }

    /**
     * @see TTest#tTest(double[], double[])
     * @param sample1 array of sample data values
     * @param sample2 array of sample data values
     * @return t statistic
     */
    public static double tTest(final double[] sample1, final double[] sample2) {
        return T_TEST.tTest(sample1, sample2);
    }

    /**
     * See fr.cnes.sirius.patrius.math.stat.inference.TTest#
     * tTest(fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary,
     * fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary, double)
     * 
     * @param sampleStats1 StatisticalSummary describing data from the first sample
     * @param sampleStats2 StatisticalSummary describing data from the second sample
     * @param alpha significance level of the test
     * @return true if the null hypothesis can be rejected with
     *         confidence 1 - alpha
     */
    public static boolean tTest(final StatisticalSummary sampleStats1,
                                final StatisticalSummary sampleStats2,
                                final double alpha) {
        return T_TEST.tTest(sampleStats1, sampleStats2, alpha);
    }

    /**
     * See fr.cnes.sirius.patrius.math.stat.inference.TTest#
     * tTest(fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary,
     * fr.cnes.sirius.patrius.math.stat.descriptive.StatisticalSummary)
     * 
     * @param sampleStats1 StatisticalSummary describing data from the first sample
     * @param sampleStats2 StatisticalSummary describing data from the second sample
     * @return t statistic
     */
    public static double tTest(final StatisticalSummary sampleStats1,
                               final StatisticalSummary sampleStats2) {
        return T_TEST.tTest(sampleStats1, sampleStats2);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquare(double[], long[])
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return chiSquare test statistic
     */
    public static double chiSquare(final double[] expected, final long[] observed) {
        return CHI_SQUARE_TEST.chiSquare(expected, observed);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquare(long[][])
     * @param counts
     *        array representation of 2-way table
     * @return chiSquare test statistic
     */
    public static double chiSquare(final long[][] counts) {
        return CHI_SQUARE_TEST.chiSquare(counts);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareTest(double[], long[], double)
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     *         1 - alpha
     */
    public static boolean chiSquareTest(final double[] expected, final long[] observed,
                                        final double alpha) {
        return CHI_SQUARE_TEST.chiSquareTest(expected, observed, alpha);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareTest(double[], long[])
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return p-value
     */
    public static double chiSquareTest(final double[] expected, final long[] observed) {
        return CHI_SQUARE_TEST.chiSquareTest(expected, observed);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareTest(long[][], double)
     * @param counts
     *        array representation of 2-way table
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     *         1 - alpha
     */
    public static boolean chiSquareTest(final long[][] counts, final double alpha) {
        return CHI_SQUARE_TEST.chiSquareTest(counts, alpha);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareTest(long[][])
     * @param counts
     *        array representation of 2-way table
     * @return p-value
     */
    public static double chiSquareTest(final long[][] counts) {
        return CHI_SQUARE_TEST.chiSquareTest(counts);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareDataSetsComparison(long[], long[])
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data set
     * @return chiSquare test statistic
     * 
     * @since 1.2
     */
    public static double chiSquareDataSetsComparison(final long[] observed1,
                                                     final long[] observed2) {
        return CHI_SQUARE_TEST.chiSquareDataSetsComparison(observed1, observed2);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareTestDataSetsComparison(long[], long[])
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data set
     * @return p-value
     * 
     * @since 1.2
     */
    public static double chiSquareTestDataSetsComparison(final long[] observed1,
                                                         final long[] observed2) {
        return CHI_SQUARE_TEST.chiSquareTestDataSetsComparison(observed1, observed2);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.ChiSquareTest#chiSquareTestDataSetsComparison(long[], long[],
     *      double)
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data set
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence
     *         1 - alpha
     * 
     * @since 1.2
     */
    public static boolean chiSquareTestDataSetsComparison(final long[] observed1,
                                                          final long[] observed2,
                                                          final double alpha) {
        return CHI_SQUARE_TEST.chiSquareTestDataSetsComparison(observed1, observed2, alpha);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.OneWayAnova#anovaFValue(Collection)
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @return Fvalue
     * 
     * @since 1.2
     */
    public static double oneWayAnovaFValue(final Collection<double[]> categoryData) {
        return ONE_WAY_ANANOVA.anovaFValue(categoryData);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.OneWayAnova#anovaPValue(Collection)
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @return Pvalue
     * 
     * @since 1.2
     */
    public static double oneWayAnovaPValue(final Collection<double[]> categoryData) {
        return ONE_WAY_ANANOVA.anovaPValue(categoryData);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.OneWayAnova#anovaTest(Collection,double)
     * @param categoryData
     *        <code>Collection</code> of <code>double[]</code> arrays each containing data for one category
     * @param alpha
     *        significance level of the test
     * @return true if the null hypothesis can be rejected with
     *         confidence 1 - alpha
     * 
     * @since 1.2
     */
    public static boolean oneWayAnovaTest(final Collection<double[]> categoryData,
                                          final double alpha) {
        return ONE_WAY_ANANOVA.anovaTest(categoryData, alpha);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#g(double[], long[])
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return G-Test statistic
     * @since 3.1
     */
    public static double g(final double[] expected, final long[] observed) {
        return G_TEST.g(expected, observed);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#gTest(double[], long[] )
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return p-value
     * @since 3.1
     */
    public static double gTest(final double[] expected, final long[] observed) {
        return G_TEST.gTest(expected, observed);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#gTestIntrinsic(double[], long[] )
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @return p-value
     * @since 3.1
     */
    public static double gTestIntrinsic(final double[] expected, final long[] observed) {
        return G_TEST.gTestIntrinsic(expected, observed);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#gTest(double[],long[],double)
     * @param observed
     *        array of observed frequency counts
     * @param expected
     *        array of expected frequency counts
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence 1 -
     *         alpha
     * @since 3.1
     */
    public static boolean gTest(final double[] expected, final long[] observed,
                                final double alpha) {
        return G_TEST.gTest(expected, observed, alpha);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#gDataSetsComparison(long[], long[])
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data
     *        set
     * @return G-Test statistic
     * @since 3.1
     */
    public static double gDataSetsComparison(final long[] observed1,
                                             final long[] observed2) {
        return G_TEST.gDataSetsComparison(observed1, observed2);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#rootLogLikelihoodRatio(long, long, long, long)
     * @param k11
     *        number of times the two events occurred together (AB)
     * @param k12
     *        number of times the second event occurred WITHOUT the
     *        first event (notA,B)
     * @param k21
     *        number of times the first event occurred WITHOUT the
     *        second event (A, notB)
     * @param k22
     *        number of times something else occurred (i.e. was neither
     *        of these events (notA, notB)
     * @return root log-likelihood ratio
     * @since 3.1
     */
    public static double rootLogLikelihoodRatio(final long k11, final long k12, final long k21, final long k22) {
        return G_TEST.rootLogLikelihoodRatio(k11, k12, k21, k22);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#gTestDataSetsComparison(long[], long[])
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data
     *        set
     * @return p-value
     * @since 3.1
     */
    public static double gTestDataSetsComparison(final long[] observed1,
                                                 final long[] observed2) {
        return G_TEST.gTestDataSetsComparison(observed1, observed2);
    }

    /**
     * @see fr.cnes.sirius.patrius.math.stat.inference.GTest#gTestDataSetsComparison(long[],long[],double)
     * @param observed1
     *        array of observed frequency counts of the first data set
     * @param observed2
     *        array of observed frequency counts of the second data
     *        set
     * @param alpha
     *        significance level of the test
     * @return true iff null hypothesis can be rejected with confidence 1 -
     *         alpha
     * @since 3.1
     */
    public static boolean gTestDataSetsComparison(final long[] observed1,
                                                  final long[] observed2, final double alpha) {
        return G_TEST.gTestDataSetsComparison(observed1, observed2, alpha);
    }

    // CHECKSTYLE: resume JavadocMethodCheck

}
