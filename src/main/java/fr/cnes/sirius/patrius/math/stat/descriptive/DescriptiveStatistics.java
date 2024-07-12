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
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Kurtosis;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Skewness;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Max;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Min;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Percentile;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfSquares;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.ResizableDoubleArray;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Maintains a dataset of values of a single variable and computes descriptive
 * statistics based on stored data. The {@link #getWindowSize() windowSize} property sets a limit on the number of
 * values that can be stored in the
 * dataset. The default value, INFINITE_WINDOW, puts no limit on the size of
 * the dataset. This value should be used with caution, as the backing store
 * will grow without bound in this case. For very large datasets, {@link SummaryStatistics}, which does not store the
 * dataset, should be used
 * instead of this class. If <code>windowSize</code> is not INFINITE_WINDOW and
 * more values are added than can be stored in the dataset, new values are
 * added in a "rolling" manner, with new values replacing the "oldest" values
 * in the dataset.
 * 
 * <p>
 * Note: this class is not threadsafe. Use {@link SynchronizedDescriptiveStatistics} if concurrent access from multiple
 * threads is required.
 * </p>
 * 
 * @version $Id: DescriptiveStatistics.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class DescriptiveStatistics implements StatisticalSummary, Serializable {

    /**
     * Represents an infinite window size. When the {@link #getWindowSize()} returns this value, there is no limit to
     * the number of data values
     * that can be stored in the dataset.
     */
    public static final int INFINITE_WINDOW = -1;

    /** 50. */
    private static final double HALF_PERCENT = 50.;

     /** Serializable UID. */
    private static final long serialVersionUID = 4133067267405273064L;

    /** Name of the setQuantile method. */
    private static final String SET_QUANTILE_METHOD_NAME = "setQuantile";

    /** hold the window size **/
    protected int windowSize = INFINITE_WINDOW;

    /**
     * Stored data values
     */
    private ResizableDoubleArray eDA = new ResizableDoubleArray();

    /** Mean statistic implementation - can be reset by setter. */
    private UnivariateStatistic meanImpl = new Mean();

    /** Geometric mean statistic implementation - can be reset by setter. */
    private UnivariateStatistic geometricMeanImpl = new GeometricMean();

    /** Kurtosis statistic implementation - can be reset by setter. */
    private UnivariateStatistic kurtosisImpl = new Kurtosis();

    /** Maximum statistic implementation - can be reset by setter. */
    private UnivariateStatistic maxImpl = new Max();

    /** Minimum statistic implementation - can be reset by setter. */
    private UnivariateStatistic minImpl = new Min();

    /** Percentile statistic implementation - can be reset by setter. */
    private UnivariateStatistic percentileImpl = new Percentile();

    /** Skewness statistic implementation - can be reset by setter. */
    private UnivariateStatistic skewnessImpl = new Skewness();

    /** Variance statistic implementation - can be reset by setter. */
    private UnivariateStatistic varianceImpl = new Variance();

    /** Sum of squares statistic implementation - can be reset by setter. */
    private UnivariateStatistic sumsqImpl = new SumOfSquares();

    /** Sum statistic implementation - can be reset by setter. */
    private UnivariateStatistic sumImpl = new Sum();

    /**
     * Construct a DescriptiveStatistics instance with an infinite window
     */
    public DescriptiveStatistics() {
        // Nothing to do
    }

    /**
     * Construct a DescriptiveStatistics instance with the specified window
     * 
     * @param window
     *        the window size.
     * @throws MathIllegalArgumentException
     *         if window size is less than 1 but
     *         not equal to {@link #INFINITE_WINDOW}
     */
    public DescriptiveStatistics(final int window) {
        this.setWindowSize(window);
    }

    /**
     * Construct a DescriptiveStatistics instance with an infinite window
     * and the initial data values in double[] initialDoubleArray.
     * If initialDoubleArray is null, then this constructor corresponds to
     * DescriptiveStatistics()
     * 
     * @param initialDoubleArray
     *        the initial double[].
     */
    public DescriptiveStatistics(final double[] initialDoubleArray) {
        if (initialDoubleArray != null) {
            this.eDA = new ResizableDoubleArray(initialDoubleArray);
        }
    }

    /**
     * Copy constructor. Construct a new DescriptiveStatistics instance that
     * is a copy of original.
     * 
     * @param original
     *        DescriptiveStatistics instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public DescriptiveStatistics(final DescriptiveStatistics original) {
        copy(original, this);
    }

    /**
     * Adds the value to the dataset. If the dataset is at the maximum size
     * (i.e., the number of stored elements equals the currently configured
     * windowSize), the first (oldest) element in the dataset is discarded
     * to make room for the new value.
     * 
     * @param v
     *        the value to be added
     */
    public void addValue(final double v) {
        if (this.windowSize == INFINITE_WINDOW) {
            this.eDA.addElement(v);
        } else {
            if (this.getN() == this.windowSize) {
                this.eDA.addElementRolling(v);
            } else if (this.getN() < this.windowSize) {
                this.eDA.addElement(v);
            }
        }
    }

    /**
     * Removes the most recent value from the dataset.
     * 
     * @throws MathIllegalStateException
     *         if there are no elements stored
     */
    public void removeMostRecentValue() {
        try {
            this.eDA.discardMostRecentElements(1);
        } catch (final MathIllegalArgumentException ex) {
            throw new MathIllegalStateException(ex, PatriusMessages.NO_DATA);
        }
    }

    /**
     * Replaces the most recently stored value with the given value.
     * There must be at least one element stored to call this method.
     * 
     * @param v
     *        the value to replace the most recent stored value
     * @return replaced value
     * @throws MathIllegalStateException
     *         if there are no elements stored
     */
    public double replaceMostRecentValue(final double v) {
        return this.eDA.substituteMostRecentElement(v);
    }

    /**
     * Returns the <a href="http://www.xycoon.com/arithmetic_mean.htm">
     * arithmetic mean </a> of the available values
     * 
     * @return The mean or Double.NaN if no values have been added.
     */
    @Override
    public double getMean() {
        return this.apply(this.meanImpl);
    }

    /**
     * Returns the <a href="http://www.xycoon.com/geometric_mean.htm">
     * geometric mean </a> of the available values
     * 
     * @return The geometricMean, Double.NaN if no values have been added,
     *         or if the product of the available values is less than or equal to 0.
     */
    public double getGeometricMean() {
        return this.apply(this.geometricMeanImpl);
    }

    /**
     * Returns the (sample) variance of the available values.
     * 
     * <p>
     * This method returns the bias-corrected sample variance (using {@code n - 1} in the denominator). Use
     * {@link #getPopulationVariance()} for the non-bias-corrected population variance.
     * </p>
     * 
     * @return The variance, Double.NaN if no values have been added
     *         or 0.0 for a single value set.
     */
    @Override
    public double getVariance() {
        return this.apply(this.varianceImpl);
    }

    /**
     * Returns the <a href="http://en.wikibooks.org/wiki/Statistics/Summary/Variance">
     * population variance</a> of the available values.
     * 
     * @return The population variance, Double.NaN if no values have been added,
     *         or 0.0 for a single value set.
     */
    public double getPopulationVariance() {
        return this.apply(new Variance(false));
    }

    /**
     * Returns the standard deviation of the available values.
     * 
     * @return The standard deviation, Double.NaN if no values have been added
     *         or 0.0 for a single value set.
     */
    @Override
    public double getStandardDeviation() {
        double stdDev = Double.NaN;
        if (this.getN() > 0) {
            if (this.getN() > 1) {
                stdDev = MathLib.sqrt(this.getVariance());
            } else {
                stdDev = 0.0;
            }
        }
        return stdDev;
    }

    /**
     * Returns the skewness of the available values. Skewness is a
     * measure of the asymmetry of a given distribution.
     * 
     * @return The skewness, Double.NaN if no values have been added
     *         or 0.0 for a value set &lt;=2.
     */
    public double getSkewness() {
        return this.apply(this.skewnessImpl);
    }

    /**
     * Returns the Kurtosis of the available values. Kurtosis is a
     * measure of the "peakedness" of a distribution
     * 
     * @return The kurtosis, Double.NaN if no values have been added, or 0.0
     *         for a value set &lt;=3.
     */
    public double getKurtosis() {
        return this.apply(this.kurtosisImpl);
    }

    /**
     * Returns the maximum of the available values
     * 
     * @return The max or Double.NaN if no values have been added.
     */
    @Override
    public double getMax() {
        return this.apply(this.maxImpl);
    }

    /**
     * Returns the minimum of the available values
     * 
     * @return The min or Double.NaN if no values have been added.
     */
    @Override
    public double getMin() {
        return this.apply(this.minImpl);
    }

    /**
     * Returns the number of available values
     * 
     * @return The number of available values
     */
    @Override
    public long getN() {
        return this.eDA.getNumElements();
    }

    /**
     * Returns the sum of the values that have been added to Univariate.
     * 
     * @return The sum or Double.NaN if no values have been added
     */
    @Override
    public double getSum() {
        return this.apply(this.sumImpl);
    }

    /**
     * Returns the sum of the squares of the available values.
     * 
     * @return The sum of the squares or Double.NaN if no
     *         values have been added.
     */
    public double getSumsq() {
        return this.apply(this.sumsqImpl);
    }

    /**
     * Resets all statistics and storage
     */
    public void clear() {
        this.eDA.clear();
    }

    /**
     * Returns the maximum number of values that can be stored in the
     * dataset, or INFINITE_WINDOW (-1) if there is no limit.
     * 
     * @return The current window size or -1 if its Infinite.
     */
    public int getWindowSize() {
        return this.windowSize;
    }

    /**
     * WindowSize controls the number of values that contribute to the
     * reported statistics. For example, if windowSize is set to 3 and the
     * values {1,2,3,4,5} have been added <strong> in that order</strong> then
     * the <i>available values</i> are {3,4,5} and all reported statistics will
     * be based on these values. If {@code windowSize} is decreased as a result
     * of this call and there are more than the new value of elements in the
     * current dataset, values from the front of the array are discarded to
     * reduce the dataset to {@code windowSize} elements.
     * 
     * @param windowSizeIn
     *        sets the size of the window.
     * @throws MathIllegalArgumentException
     *         if window size is less than 1 but
     *         not equal to {@link #INFINITE_WINDOW}
     */
    public void setWindowSize(final int windowSizeIn) {
        if (windowSizeIn < 1 && windowSizeIn != INFINITE_WINDOW) {
            throw new MathIllegalArgumentException(
                PatriusMessages.NOT_POSITIVE_WINDOW_SIZE, windowSizeIn);
        }

        this.windowSize = windowSizeIn;

        // We need to check to see if we need to discard elements
        // from the front of the array. If the windowSize is less than
        // the current number of elements.
        if (windowSizeIn != INFINITE_WINDOW && windowSizeIn < this.eDA.getNumElements()) {
            this.eDA.discardFrontElements(this.eDA.getNumElements() - windowSizeIn);
        }
    }

    /**
     * Returns the current set of values in an array of double primitives.
     * The order of addition is preserved. The returned array is a fresh
     * copy of the underlying data -- i.e., it is not a reference to the
     * stored data.
     * 
     * @return returns the current set of numbers in the order in which they
     *         were added to this set
     */
    public double[] getValues() {
        return this.eDA.getElements();
    }

    /**
     * Returns the current set of values in an array of double primitives,
     * sorted in ascending order. The returned array is a fresh
     * copy of the underlying data -- i.e., it is not a reference to the
     * stored data.
     * 
     * @return returns the current set of
     *         numbers sorted in ascending order
     */
    public double[] getSortedValues() {
        final double[] sort = this.getValues();
        Arrays.sort(sort);
        return sort;
    }

    /**
     * Returns the element at the specified index
     * 
     * @param index
     *        The Index of the element
     * @return return the element at the specified index
     */
    public double getElement(final int index) {
        return this.eDA.getElement(index);
    }

    /**
     * Returns an estimate for the pth percentile of the stored values.
     * <p>
     * The implementation provided here follows the first estimation procedure presented <a
     * href="http://www.itl.nist.gov/div898/handbook/prc/section2/prc252.htm">here.</a>
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li><code>0 &lt; p &le; 100</code> (otherwise an <code>MathIllegalArgumentException</code> is thrown)</li>
     * <li>at least one value must be stored (returns <code>Double.NaN
     *     </code> otherwise)</li>
     * </ul>
     * </p>
     * 
     * @param p
     *        the requested percentile (scaled from 0 - 100)
     * @return An estimate for the pth percentile of the stored data
     * @throws MathIllegalStateException
     *         if percentile implementation has been
     *         overridden and the supplied implementation does not support setQuantile
     * @throws MathIllegalArgumentException
     *         if p is not a valid quantile
     */
    public double getPercentile(final double p) {
        if (this.percentileImpl instanceof Percentile) {
            // Percentile statistic implementation is an instance of Percentile, set value for quantile field
            ((Percentile) this.percentileImpl).setQuantile(p);
        } else {
            throw new MathIllegalStateException(PatriusMessages.PERCENTILE_IMPLEMENTATION_UNSUPPORTED_METHOD,
                    this.percentileImpl.getClass().getName(), SET_QUANTILE_METHOD_NAME);
        }
        return this.apply(this.percentileImpl);
    }

    /**
     * Generates a text report displaying univariate statistics from values
     * that have been added. Each statistic is displayed on a separate
     * line.
     * 
     * @return String with line feeds displaying statistics
     */
    @Override
    public String toString() {
        // Initialization
        final StringBuilder outBuffer = new StringBuilder();
        final String endl = "\n";
        outBuffer.append("DescriptiveStatistics:").append(endl);
        outBuffer.append("n: ").append(this.getN()).append(endl);
        outBuffer.append("min: ").append(this.getMin()).append(endl);
        outBuffer.append("max: ").append(this.getMax()).append(endl);
        outBuffer.append("mean: ").append(this.getMean()).append(endl);
        outBuffer.append("std dev: ").append(this.getStandardDeviation())
            .append(endl);
        try {
            // No catch for MIAE because actual parameter is valid below
            outBuffer.append("median: ").append(this.getPercentile(HALF_PERCENT)).append(endl);
        } catch (final MathIllegalStateException ex) {
            outBuffer.append("median: unavailable").append(endl);
        }
        outBuffer.append("skewness: ").append(this.getSkewness()).append(endl);
        outBuffer.append("kurtosis: ").append(this.getKurtosis()).append(endl);
        // Return result
        //
        return outBuffer.toString();
    }

    /**
     * Apply the given statistic to the data associated with this set of statistics.
     * 
     * @param stat
     *        the statistic to apply
     * @return the computed value of the statistic.
     */
    public double apply(final UnivariateStatistic stat) {
        // No try-catch or advertised exception here because arguments are guaranteed valid
        return this.eDA.compute(stat);
    }

    // Implementation getters and setter

    /**
     * Returns the currently configured mean implementation.
     * 
     * @return the UnivariateStatistic implementing the mean
     * @since 1.2
     */
    public synchronized UnivariateStatistic getMeanImpl() {
        return this.meanImpl;
    }

    /**
     * <p>
     * Sets the implementation for the mean.
     * </p>
     * 
     * @param meanImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the mean
     * @since 1.2
     */
    public synchronized void setMeanImpl(final UnivariateStatistic meanImplIn) {
        this.meanImpl = meanImplIn;
    }

    /**
     * Returns the currently configured geometric mean implementation.
     * 
     * @return the UnivariateStatistic implementing the geometric mean
     * @since 1.2
     */
    public synchronized UnivariateStatistic getGeometricMeanImpl() {
        return this.geometricMeanImpl;
    }

    /**
     * <p>
     * Sets the implementation for the gemoetric mean.
     * </p>
     * 
     * @param geometricMeanImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the geometric mean
     * @since 1.2
     */
    public synchronized void setGeometricMeanImpl(
                                                  final UnivariateStatistic geometricMeanImplIn) {
        this.geometricMeanImpl = geometricMeanImplIn;
    }

    /**
     * Returns the currently configured kurtosis implementation.
     * 
     * @return the UnivariateStatistic implementing the kurtosis
     * @since 1.2
     */
    public synchronized UnivariateStatistic getKurtosisImpl() {
        return this.kurtosisImpl;
    }

    /**
     * <p>
     * Sets the implementation for the kurtosis.
     * </p>
     * 
     * @param kurtosisImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the kurtosis
     * @since 1.2
     */
    public synchronized void setKurtosisImpl(final UnivariateStatistic kurtosisImplIn) {
        this.kurtosisImpl = kurtosisImplIn;
    }

    /**
     * Returns the currently configured maximum implementation.
     * 
     * @return the UnivariateStatistic implementing the maximum
     * @since 1.2
     */
    public synchronized UnivariateStatistic getMaxImpl() {
        return this.maxImpl;
    }

    /**
     * <p>
     * Sets the implementation for the maximum.
     * </p>
     * 
     * @param maxImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the maximum
     * @since 1.2
     */
    public synchronized void setMaxImpl(final UnivariateStatistic maxImplIn) {
        this.maxImpl = maxImplIn;
    }

    /**
     * Returns the currently configured minimum implementation.
     * 
     * @return the UnivariateStatistic implementing the minimum
     * @since 1.2
     */
    public synchronized UnivariateStatistic getMinImpl() {
        return this.minImpl;
    }

    /**
     * <p>
     * Sets the implementation for the minimum.
     * </p>
     * 
     * @param minImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the minimum
     * @since 1.2
     */
    public synchronized void setMinImpl(final UnivariateStatistic minImplIn) {
        this.minImpl = minImplIn;
    }

    /**
     * Returns the currently configured percentile implementation.
     * 
     * @return the UnivariateStatistic implementing the percentile
     * @since 1.2
     */
    public synchronized UnivariateStatistic getPercentileImpl() {
        return this.percentileImpl;
    }

    /**
     * Sets the implementation to be used by {@link #getPercentile(double)}.
     * The supplied <code>UnivariateStatistic</code> must provide a <code>setQuantile(double)</code> method; otherwise
     * <code>IllegalArgumentException</code> is thrown.
     * 
     * @param percentileImplIn
     *        the percentileImpl to set
     * @throws MathIllegalArgumentException
     *         if the supplied implementation does not
     *         provide a <code>setQuantile</code> method
     * @since 1.2
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public synchronized void setPercentileImpl(final UnivariateStatistic percentileImplIn) {
        if (percentileImplIn instanceof Percentile) {
            // Percentile statistic implementation is an instance of Percentile, set value for quantile field
            ((Percentile) percentileImplIn).setQuantile(Double.valueOf(HALF_PERCENT));
        } else {
            throw new MathIllegalArgumentException(
                    PatriusMessages.PERCENTILE_IMPLEMENTATION_UNSUPPORTED_METHOD,
                    percentileImplIn.getClass().getName(), SET_QUANTILE_METHOD_NAME);
        }
        this.percentileImpl = percentileImplIn;
    }

    /**
     * Returns the currently configured skewness implementation.
     * 
     * @return the UnivariateStatistic implementing the skewness
     * @since 1.2
     */
    public synchronized UnivariateStatistic getSkewnessImpl() {
        return this.skewnessImpl;
    }

    /**
     * <p>
     * Sets the implementation for the skewness.
     * </p>
     * 
     * @param skewnessImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the skewness
     * @since 1.2
     */
    public synchronized void setSkewnessImpl(
                                             final UnivariateStatistic skewnessImplIn) {
        this.skewnessImpl = skewnessImplIn;
    }

    /**
     * Returns the currently configured variance implementation.
     * 
     * @return the UnivariateStatistic implementing the variance
     * @since 1.2
     */
    public synchronized UnivariateStatistic getVarianceImpl() {
        return this.varianceImpl;
    }

    /**
     * <p>
     * Sets the implementation for the variance.
     * </p>
     * 
     * @param varianceImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the variance
     * @since 1.2
     */
    public synchronized void setVarianceImpl(
                                             final UnivariateStatistic varianceImplIn) {
        this.varianceImpl = varianceImplIn;
    }

    /**
     * Returns the currently configured sum of squares implementation.
     * 
     * @return the UnivariateStatistic implementing the sum of squares
     * @since 1.2
     */
    public synchronized UnivariateStatistic getSumsqImpl() {
        return this.sumsqImpl;
    }

    /**
     * <p>
     * Sets the implementation for the sum of squares.
     * </p>
     * 
     * @param sumsqImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the sum of squares
     * @since 1.2
     */
    public synchronized void setSumsqImpl(final UnivariateStatistic sumsqImplIn) {
        this.sumsqImpl = sumsqImplIn;
    }

    /**
     * Returns the currently configured sum implementation.
     * 
     * @return the UnivariateStatistic implementing the sum
     * @since 1.2
     */
    public synchronized UnivariateStatistic getSumImpl() {
        return this.sumImpl;
    }

    /**
     * <p>
     * Sets the implementation for the sum.
     * </p>
     * 
     * @param sumImplIn
     *        the UnivariateStatistic instance to use
     *        for computing the sum
     * @since 1.2
     */
    public synchronized void setSumImpl(final UnivariateStatistic sumImplIn) {
        this.sumImpl = sumImplIn;
    }

    /**
     * Returns a copy of this DescriptiveStatistics instance with the same internal state.
     * 
     * @return a copy of this
     */
    public DescriptiveStatistics copy() {
        final DescriptiveStatistics result = new DescriptiveStatistics();
        // No try-catch or advertised exception because parms are guaranteed valid
        copy(this, result);
        return result;
    }

    /**
     * Copies source to dest.
     * <p>
     * Neither source nor dest can be null.
     * </p>
     * 
     * @param source
     *        DescriptiveStatistics to copy
     * @param dest
     *        DescriptiveStatistics to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final DescriptiveStatistics source, final DescriptiveStatistics dest) {
        // Checks
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        // Copy data and window size
        dest.eDA = source.eDA.copy();
        dest.windowSize = source.windowSize;

        // Copy implementations
        //
        dest.maxImpl = source.maxImpl.copy();
        dest.meanImpl = source.meanImpl.copy();
        dest.minImpl = source.minImpl.copy();
        dest.sumImpl = source.sumImpl.copy();
        dest.varianceImpl = source.varianceImpl.copy();
        dest.sumsqImpl = source.sumsqImpl.copy();
        dest.geometricMeanImpl = source.geometricMeanImpl.copy();
        dest.kurtosisImpl = source.kurtosisImpl;
        dest.skewnessImpl = source.skewnessImpl;
        dest.percentileImpl = source.percentileImpl;
    }
}
