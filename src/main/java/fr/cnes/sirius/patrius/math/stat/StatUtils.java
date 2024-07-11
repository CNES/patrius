/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.StandardDeviation;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Max;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Median;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Min;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Product;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfLogs;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfSquares;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * StatUtils provides static methods for computing statistics based on data
 * stored in double[] arrays.
 * 
 * @version $Id: StatUtils.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class StatUtils {

    /** sum */
    private static final UnivariateStatistic SUM_F = new Sum();

    /** sumSq */
    private static final UnivariateStatistic SUM_OF_SQUARES_F = new SumOfSquares();

    /** prod */
    private static final UnivariateStatistic PRODUCT_F = new Product();

    /** sumLog */
    private static final UnivariateStatistic SUM_OF_LOGS_F = new SumOfLogs();

    /** min */
    private static final UnivariateStatistic MIN_F = new Min();

    /** max */
    private static final UnivariateStatistic MAX_F = new Max();

    /** mean */
    private static final UnivariateStatistic MEAN_F = new Mean();

    /** variance */
    private static final Variance VARIANCE_F = new Variance();

    /** standard deviation */
    private static final StandardDeviation STANDARDDEVIATION_F = new StandardDeviation();

    /** percentile */
    private static final Percentile PERCENTILE_F = new Percentile();

    /** median */
    private static final Median MEDIAN_F = new Median();

    /** geometric mean */
    private static final GeometricMean GEOMETRIC_MEAN_F = new GeometricMean();

    /**
     * Private Constructor
     */
    private StatUtils() {
    }

    /**
     * Returns the sum of the values in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the input array is null.
     * </p>
     * 
     * @param values
     *        array of values to sum
     * @return the sum of the values or <code>Double.NaN</code> if the array
     *         is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double sum(final double[] values) {
        return SUM_F.evaluate(values);
    }

    /**
     * Returns the sum of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the sum of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double sum(final double[] values, final int begin,
                             final int length) {
        return SUM_F.evaluate(values, begin, length);
    }

    /**
     * Returns the sum of the squares of the entries in the input array, or <code>Double.NaN</code> if the array is
     * empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        input array
     * @return the sum of the squared values or <code>Double.NaN</code> if the
     *         array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double sumSq(final double[] values) {
        return SUM_OF_SQUARES_F.evaluate(values);
    }

    /**
     * Returns the sum of the squares of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the sum of the squares of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double sumSq(final double[] values, final int begin,
                               final int length) {
        return SUM_OF_SQUARES_F.evaluate(values, begin, length);
    }

    /**
     * Returns the product of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the product of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double product(final double[] values) {
        return PRODUCT_F.evaluate(values);
    }

    /**
     * Returns the product of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the product of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double product(final double[] values, final int begin,
                                 final int length) {
        return PRODUCT_F.evaluate(values, begin, length);
    }

    /**
     * Returns the sum of the natural logs of the entries in the input array, or <code>Double.NaN</code> if the array is
     * empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfLogs}.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the sum of the natural logs of the values or Double.NaN if
     *         the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double sumLog(final double[] values) {
        return SUM_OF_LOGS_F.evaluate(values);
    }

    /**
     * Returns the sum of the natural logs of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfLogs}.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the sum of the natural logs of the values or Double.NaN if
     *         length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double sumLog(final double[] values, final int begin,
                                final int length) {
        return SUM_OF_LOGS_F.evaluate(values, begin, length);
    }

    /**
     * Returns the arithmetic mean of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean} for details on the computing algorithm.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the mean of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double mean(final double[] values) {
        return MEAN_F.evaluate(values);
    }

    /**
     * Returns the arithmetic mean of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean} for details on the computing algorithm.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the mean of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double mean(final double[] values, final int begin,
                              final int length) {
        return MEAN_F.evaluate(values, begin, length);
    }

    /**
     * Returns the quadratic mean of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the quadratic mean of the values or Double.NaN if the array is empty
     * @throws IllegalArgumentException
     *         if the array is null
     */
    public static double quadraticMean(final double[] values) {
        final double res;
        final double sumSq = sumSq(values);
        final double normalizedSum = sumSq / values.length;
        if (normalizedSum >= 0. && !Double.isNaN(normalizedSum)) {
            res = MathLib.sqrt(normalizedSum);
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * Returns the quadratic mean of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null or the array index parameters are not valid
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the quadratic mean of the values or Double.NaN if length = 0
     * @throws IllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double quadraticMean(final double[] values, final int begin,
                                       final int length) {
        final double res;
        final double sumSq = sumSq(values, begin, length);
        final double normalizedSum = sumSq / length;
        if (normalizedSum >= 0. && !Double.isNaN(normalizedSum)) {
            res = MathLib.sqrt(normalizedSum);
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * Returns the geometric mean of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean} for details on the computing
     * algorithm.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the geometric mean of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double geometricMean(final double[] values) {
        return GEOMETRIC_MEAN_F.evaluate(values);
    }

    /**
     * Returns the geometric mean of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean} for details on the computing
     * algorithm.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the geometric mean of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double geometricMean(final double[] values, final int begin,
                                       final int length) {
        return GEOMETRIC_MEAN_F.evaluate(values, begin, length);
    }

    /**
     * Returns the variance of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * 
     * <p>
     * This method returns the bias-corrected sample variance (using {@code n - 1} in the denominator). Use
     * {@link #populationVariance(double[])} for the non-bias-corrected population variance.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the variance of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double variance(final double[] values) {
        return VARIANCE_F.evaluate(values);
    }

    /**
     * Returns the variance of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * 
     * <p>
     * This method returns the bias-corrected sample variance (using {@code n - 1} in the denominator). Use
     * {@link #populationVariance(double[], int, int)} for the non-bias-corrected population variance.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double variance(final double[] values, final int begin,
                                  final int length) {
        return VARIANCE_F.evaluate(values, begin, length);
    }

    /**
     * Returns the variance of the entries in the specified portion of
     * the input array, using the precomputed mean value. Returns <code>Double.NaN</code> if the designated subarray is
     * empty.
     * 
     * <p>
     * This method returns the bias-corrected sample variance (using {@code n - 1} in the denominator). Use
     * {@link #populationVariance(double[], double, int, int)} for the non-bias-corrected population variance.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the arithmetic mean of the sample data, not a known
     * population parameter. This method is supplied only to save computation when the mean has already been computed.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * 
     * @param values
     *        the input array
     * @param mean
     *        the precomputed mean value
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double variance(final double[] values, final double mean,
                                  final int begin, final int length) {
        return VARIANCE_F.evaluate(values, mean, begin, length);
    }

    /**
     * Returns the variance of the entries in the input array, using the
     * precomputed mean value. Returns <code>Double.NaN</code> if the array
     * is empty.
     * 
     * <p>
     * This method returns the bias-corrected sample variance (using {@code n - 1} in the denominator). Use
     * {@link #populationVariance(double[], double)} for the non-bias-corrected population variance.
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the arithmetic mean of the sample data, not a known
     * population parameter. This method is supplied only to save computation when the mean has already been computed.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param mean
     *        the precomputed mean value
     * @return the variance of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double variance(final double[] values, final double mean) {
        return VARIANCE_F.evaluate(values, mean);
    }

    /**
     * Returns the standard deviation of the entries in the input array, or <code>Double.NaN</code> if the array is
     * empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.StandardDeviation} for details on the computing
     * algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the standard deviation of the values or Double.NaN if the array is empty
     * @throws IllegalArgumentException
     *         if the array is null
     */
    public static double standardDeviation(final double[] values) {
        return STANDARDDEVIATION_F.evaluate(values);
    }

    /**
     * Returns the standard deviation of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.StandardDeviation} for details on the computing
     * algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null or the array index parameters are not valid.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the standard deviation of the values or Double.NaN if length = 0
     * @throws IllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double standardDeviation(final double[] values, final int begin,
                                           final int length) {
        return STANDARDDEVIATION_F.evaluate(values, begin, length);
    }

    /**
     * Returns the standard deviation of the entries in the specified portion of
     * the input array, using the precomputed mean value. Returns <code>Double.NaN</code> if the designated subarray is
     * empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.StandardDeviation} for details on the computing
     * algorithm.
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the arithmetic mean of the sample data, not a known
     * population parameter. This method is supplied only to save computation when the mean has already been computed.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null or the array index parameters are not valid.
     * </p>
     * 
     * @param values
     *        the input array
     * @param mean
     *        the precomputed mean value
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the standard deviation of the values or Double.NaN if length = 0
     * @throws IllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double standardDeviation(final double[] values, final double mean,
                                           final int begin, final int length) {
        return STANDARDDEVIATION_F.evaluate(values, mean, begin, length);
    }

    /**
     * Returns the standard deviation of the entries in the input array, using the
     * precomputed mean value. Returns <code>Double.NaN</code> if the array
     * is empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.StandardDeviation} for details on the computing
     * algorithm.
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the arithmetic mean of the sample data, not a known
     * population parameter. This method is supplied only to save computation when the mean has already been computed.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param mean
     *        the precomputed mean value
     * @return the standard deviation of the values or Double.NaN if the array is empty
     * @throws IllegalArgumentException
     *         if the array is null
     */
    public static double standardDeviation(final double[] values, final double mean) {
        return STANDARDDEVIATION_F.evaluate(values, mean);
    }

    /**
     * Returns the <a href="http://en.wikibooks.org/wiki/Statistics/Summary/Variance">
     * population variance</a> of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the formula and computing
     * algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the population variance of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double populationVariance(final double[] values) {
        return new Variance(false).evaluate(values);
    }

    /**
     * Returns the <a href="http://en.wikibooks.org/wiki/Statistics/Summary/Variance">
     * population variance</a> of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the population variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double populationVariance(final double[] values, final int begin,
                                            final int length) {
        return new Variance(false).evaluate(values, begin, length);
    }

    /**
     * Returns the <a href="http://en.wikibooks.org/wiki/Statistics/Summary/Variance">
     * population variance</a> of the entries in the specified portion of
     * the input array, using the precomputed mean value. Returns <code>Double.NaN</code> if the designated subarray is
     * empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the arithmetic mean of the sample data, not a known
     * population parameter. This method is supplied only to save computation when the mean has already been computed.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * 
     * @param values
     *        the input array
     * @param mean
     *        the precomputed mean value
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the population variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double populationVariance(final double[] values, final double mean,
                                            final int begin, final int length) {
        return new Variance(false).evaluate(values, mean, begin, length);
    }

    /**
     * Returns the <a href="http://en.wikibooks.org/wiki/Statistics/Summary/Variance">
     * population variance</a> of the entries in the input array, using the
     * precomputed mean value. Returns <code>Double.NaN</code> if the array
     * is empty.
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the arithmetic mean of the sample data, not a known
     * population parameter. This method is supplied only to save computation when the mean has already been computed.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param mean
     *        the precomputed mean value
     * @return the population variance of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double populationVariance(final double[] values, final double mean) {
        return new Variance(false).evaluate(values, mean);
    }

    /**
     * Returns the maximum of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * <ul>
     * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no
     * impact on the value of the statistic).</li>
     * <li>If any of the values equals <code>Double.POSITIVE_INFINITY</code>, the result is
     * <code>Double.POSITIVE_INFINITY.</code></li>
     * </ul>
     * 
     * @param values
     *        the input array
     * @return the maximum of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double max(final double[] values) {
        return MAX_F.evaluate(values);
    }

    /**
     * Returns the maximum of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * <ul>
     * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no
     * impact on the value of the statistic).</li>
     * <li>If any of the values equals <code>Double.POSITIVE_INFINITY</code>, the result is
     * <code>Double.POSITIVE_INFINITY.</code></li>
     * </ul>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the maximum of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double max(final double[] values, final int begin,
                             final int length) {
        return MAX_F.evaluate(values, begin, length);
    }

    /**
     * Returns the minimum of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * <ul>
     * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no
     * impact on the value of the statistic).</li>
     * <li>If any of the values equals <code>Double.NEGATIVE_INFINITY</code>, the result is
     * <code>Double.NEGATIVE_INFINITY.</code></li>
     * </ul>
     * 
     * @param values
     *        the input array
     * @return the minimum of the values or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    public static double min(final double[] values) {
        return MIN_F.evaluate(values);
    }

    /**
     * Returns the minimum of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null or the array index parameters are not
     * valid.
     * </p>
     * <ul>
     * <li>The result is <code>NaN</code> iff all values are <code>NaN</code> (i.e. <code>NaN</code> values have no
     * impact on the value of the statistic).</li>
     * <li>If any of the values equals <code>Double.NEGATIVE_INFINITY</code>, the result is
     * <code>Double.NEGATIVE_INFINITY.</code></li>
     * </ul>
     * 
     * @param values
     *        the input array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the minimum of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null or the array index
     *         parameters are not valid
     */
    public static double min(final double[] values, final int begin,
                             final int length) {
        return MIN_F.evaluate(values, begin, length);
    }

    /**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array.
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>values</code> has length <code>0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[0]</code> if <code>values</code> has length
     * <code>1</code></li>
     * <li>Throws <code>IllegalArgumentException</code> if <code>values</code> is null or p is not a valid quantile
     * value (p must be greater than 0 and less than or equal to 100)</li>
     * </ul>
     * </p>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile} for a description of the percentile
     * estimation algorithm used.
     * </p>
     * 
     * @param values
     *        input array of values
     * @param p
     *        the percentile value to compute
     * @return the percentile value or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException
     *         if <code>values</code> is null
     *         or p is invalid
     */
    public static double percentile(final double[] values, final double p) {
        return PERCENTILE_F.evaluate(values, p);
    }

    /**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array, starting with the element in (0-based)
     * position <code>begin</code> in the array and including <code>length</code> values.
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>length = 0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[begin]</code> if <code>length = 1 </code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code> is null , <code>begin</code> or
     * <code>length</code> is invalid, or <code>p</code> is not a valid quantile value (p must be greater than 0 and
     * less than or equal to 100)</li>
     * </ul>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile} for a description of the percentile
     * estimation algorithm used.
     * </p>
     * 
     * @param values
     *        array of input values
     * @param p
     *        the percentile to compute
     * @param begin
     *        the first (0-based) element to include in the computation
     * @param length
     *        the number of array elements to include
     * @return the percentile value
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid or the
     *         input array is null
     */
    public static double percentile(final double[] values, final int begin,
                                    final int length, final double p) {
        return PERCENTILE_F.evaluate(values, begin, length, p);
    }

    /**
     * Returns an estimate of the median of the values in the <code>values</code> array.
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>values</code> has length <code>0</code></li>
     * <li>Returns <code>values[0]</code> if <code>values</code> has length <code>1</code></li>
     * <li>Throws <code>IllegalArgumentException</code> if <code>values</code> is null</li>
     * </ul>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile} for a description of the median
     * estimation algorithm used.
     * </p>
     * 
     * @param values
     *        input array of values
     * @return the median value or Double.NaN if the array is empty
     * @throws IllegalArgumentException
     *         if <code>values</code> is null
     *         or p is invalid
     */
    public static double median(final double[] values) {
        return MEDIAN_F.evaluate(values);
    }

    /**
     * Returns an estimate of the median of the values in the <code>values</code> array, starting with the element in
     * (0-based) position <code>begin</code> in the array and including <code>length</code> values
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>length = 0</code></li>
     * <li>Returns <code>values[begin]</code> if <code>length = 1 </code></li>
     * <li>Throws <code>IllegalArgumentException</code> if <code>values</code> is null , <code>begin</code> or
     * <code>length</code> is invalid</li>
     * </ul>
     * <p>
     * See {@link fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile} for a description of the percentile
     * estimation algorithm used.
     * </p>
     * 
     * @param values
     *        array of input values
     * @param begin
     *        the first (0-based) element to include in the computation
     * @param length
     *        the number of array elements to include
     * @return the median value
     * @throws IllegalArgumentException
     *         if the parameters are not valid or the
     *         input array is null
     */
    public static double median(final double[] values, final int begin,
                                final int length) {
        return MEDIAN_F.evaluate(values, begin, length);
    }

    /**
     * Returns the sum of the (signed) differences between corresponding elements of the
     * input arrays -- i.e., sum(sample1[i] - sample2[i]).
     * 
     * @param sample1
     *        the first array
     * @param sample2
     *        the second array
     * @return sum of paired differences
     * @throws DimensionMismatchException
     *         if the arrays do not have the same
     *         (positive) length.
     * @throws NoDataException
     *         if the sample arrays are empty.
     */
    public static double sumDifference(final double[] sample1, final double[] sample2) {
        final int n = sample1.length;
        if (n != sample2.length) {
            // Exception
            throw new DimensionMismatchException(n, sample2.length);
        }
        if (n <= 0) {
            // Exception
            throw new NoDataException(PatriusMessages.INSUFFICIENT_DIMENSION);
        }
        double result = 0;
        // Compute the sum of the difference between elements of the two arrays
        for (int i = 0; i < n; i++) {
            result += sample1[i] - sample2[i];
        }
        return result;
    }

    /**
     * Returns the mean of the (signed) differences between corresponding elements of the
     * input arrays -- i.e., sum(sample1[i] - sample2[i]) / sample1.length.
     * 
     * @param sample1
     *        the first array
     * @param sample2
     *        the second array
     * @return mean of paired differences
     * @throws DimensionMismatchException
     *         if the arrays do not have the same
     *         (positive) length.
     * @throws NoDataException
     *         if the sample arrays are empty.
     */
    public static double meanDifference(final double[] sample1, final double[] sample2) {
        return sumDifference(sample1, sample2) / sample1.length;
    }

    /**
     * Returns the variance of the (signed) differences between corresponding elements of the
     * input arrays -- i.e., var(sample1[i] - sample2[i]).
     * 
     * @param sample1
     *        the first array
     * @param sample2
     *        the second array
     * @param meanDifference
     *        the mean difference between corresponding entries
     * @see #meanDifference(double[],double[])
     * @return variance of paired differences
     * @throws DimensionMismatchException
     *         if the arrays do not have the same
     *         length.
     * @throws NumberIsTooSmallException
     *         if the arrays length is less than 2.
     */
    public static double varianceDifference(final double[] sample1,
                                            final double[] sample2, final double meanDifference) {
        final int n = sample1.length;
        if (n != sample2.length) {
            // Exception
            throw new DimensionMismatchException(n, sample2.length);
        }
        if (n < 2) {
            // Exception
            throw new NumberIsTooSmallException(n, 2, true);
        }

        // Initialization
        double sum1 = 0d;
        double sum2 = 0d;
        double diff = 0d;
        for (int i = 0; i < n; i++) {
            diff = sample1[i] - sample2[i];
            sum1 += (diff - meanDifference) * (diff - meanDifference);
            sum2 += diff - meanDifference;
        }
        // Result
        return (sum1 - (sum2 * sum2 / n)) / (n - 1);
    }

    /**
     * Normalize (standardize) the sample, so it is has a mean of 0 and a standard deviation of 1.
     * 
     * @param sample
     *        Sample to normalize.
     * @return normalized (standardized) sample.
     * @since 2.2
     */
    public static double[] normalize(final double[] sample) {
        final DescriptiveStatistics stats = new DescriptiveStatistics();

        // Add the data from the series to stats
        for (final double element : sample) {
            stats.addValue(element);
        }

        // Compute mean and standard deviation
        final double mean = stats.getMean();
        final double standardDeviation = stats.getStandardDeviation();

        // initialize the standardizedSample, which has the same length as the sample
        final double[] standardizedSample = new double[sample.length];

        for (int i = 0; i < sample.length; i++) {
            // z = (x- mean)/standardDeviation
            standardizedSample[i] = (sample[i] - mean) / standardDeviation;
        }
        return standardizedSample;
    }
}
