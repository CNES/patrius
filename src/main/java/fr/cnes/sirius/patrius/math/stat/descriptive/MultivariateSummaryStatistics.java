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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.VectorialCovariance;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Max;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Min;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfLogs;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfSquares;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * <p>
 * Computes summary statistics for a stream of n-tuples added using the {@link #addValue(double[]) addValue} method. The
 * data values are not stored in memory, so this class can be used to compute statistics for very large n-tuple streams.
 * </p>
 * 
 * <p>
 * The {@link StorelessUnivariateStatistic} instances used to maintain summary state and compute statistics are
 * configurable via setters. For example, the default implementation for the mean can be overridden by calling
 * {@link #setMeanImpl(StorelessUnivariateStatistic[])}. Actual parameters to these methods must implement the
 * {@link StorelessUnivariateStatistic} interface and configuration must be completed before <code>addValue</code> is
 * called. No configuration is necessary to use the default, commons-math provided implementations.
 * </p>
 * 
 * <p>
 * To compute statistics for a stream of n-tuples, construct a MultivariateStatistics instance with dimension n and then
 * use {@link #addValue(double[])} to add n-tuples. The <code>getXxx</code> methods where Xxx is a statistic return an
 * array of <code>double</code> values, where for <code>i = 0,...,n-1</code> the i<sup>th</sup> array element is the
 * value of the given statistic for data range consisting of the i<sup>th</sup> element of each of the input n-tuples.
 * For example, if <code>addValue</code> is called with actual parameters {0, 1, 2}, then {3, 4, 5} and finally {6, 7,
 * 8}, <code>getSum</code> will return a three-element array with values {0+3+6, 1+4+7, 2+5+8}
 * </p>
 * 
 * <p>
 * Note: This class is not thread-safe. Use {@link SynchronizedMultivariateSummaryStatistics} if concurrent access from
 * multiple threads is required.
 * </p>
 * 
 * @since 1.2
 * @version $Id: MultivariateSummaryStatistics.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class MultivariateSummaryStatistics
    implements StatisticalMultivariateSummary, Serializable {

    /** Serialization UID */
    private static final long serialVersionUID = 2271900808994826718L;

    /** Dimension of the data. */
    private final int k;

    /** Count of values that have been added */
    private long n = 0;

    /** Sum statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] sumImpl;

    /** Sum of squares statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] sumSqImpl;

    /** Minimum statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] minImpl;

    /** Maximum statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] maxImpl;

    /** Sum of log statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] sumLogImpl;

    /** Geometric mean statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] geoMeanImpl;

    /** Mean statistic implementation - can be reset by setter. */
    private final StorelessUnivariateStatistic[] meanImpl;

    /** Covariance statistic implementation - cannot be reset. */
    private final VectorialCovariance covarianceImpl;

    /**
     * Construct a MultivariateSummaryStatistics instance
     * 
     * @param kIn
     *        dimension of the data
     * @param isCovarianceBiasCorrected
     *        if true, the unbiased sample
     *        covariance is computed, otherwise the biased population covariance
     *        is computed
     */
    public MultivariateSummaryStatistics(final int kIn, final boolean isCovarianceBiasCorrected) {
        this.k = kIn;

        this.sumImpl = new StorelessUnivariateStatistic[kIn];
        this.sumSqImpl = new StorelessUnivariateStatistic[kIn];
        this.minImpl = new StorelessUnivariateStatistic[kIn];
        this.maxImpl = new StorelessUnivariateStatistic[kIn];
        this.sumLogImpl = new StorelessUnivariateStatistic[kIn];
        this.geoMeanImpl = new StorelessUnivariateStatistic[kIn];
        this.meanImpl = new StorelessUnivariateStatistic[kIn];

        for (int i = 0; i < kIn; ++i) {
            this.sumImpl[i] = new Sum();
            this.sumSqImpl[i] = new SumOfSquares();
            this.minImpl[i] = new Min();
            this.maxImpl[i] = new Max();
            this.sumLogImpl[i] = new SumOfLogs();
            this.geoMeanImpl[i] = new GeometricMean();
            this.meanImpl[i] = new Mean();
        }

        this.covarianceImpl =
            new VectorialCovariance(kIn, isCovarianceBiasCorrected);

    }

    /**
     * Add an n-tuple to the data
     * 
     * @param value
     *        the n-tuple to add
     * @throws DimensionMismatchException
     *         if the length of the array
     *         does not match the one used at construction
     */
    public void addValue(final double[] value) {
        this.checkDimension(value.length);
        for (int i = 0; i < this.k; ++i) {
            final double v = value[i];
            this.sumImpl[i].increment(v);
            this.sumSqImpl[i].increment(v);
            this.minImpl[i].increment(v);
            this.maxImpl[i].increment(v);
            this.sumLogImpl[i].increment(v);
            this.geoMeanImpl[i].increment(v);
            this.meanImpl[i].increment(v);
        }
        this.covarianceImpl.increment(value);
        this.n++;
    }

    /**
     * Returns the dimension of the data
     * 
     * @return The dimension of the data
     */
    @Override
    public int getDimension() {
        return this.k;
    }

    /**
     * Returns the number of available values
     * 
     * @return The number of available values
     */
    @Override
    public long getN() {
        return this.n;
    }

    /**
     * Returns an array of the results of a statistic.
     * 
     * @param stats
     *        univariate statistic array
     * @return results array
     */
    private double[] getResults(final StorelessUnivariateStatistic[] stats) {
        final double[] results = new double[stats.length];
        for (int i = 0; i < results.length; ++i) {
            results[i] = stats[i].getResult();
        }
        return results;
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the sum of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component sums
     */
    @Override
    public double[] getSum() {
        return this.getResults(this.sumImpl);
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the sum of squares of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component sums of squares
     */
    @Override
    public double[] getSumSq() {
        return this.getResults(this.sumSqImpl);
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the sum of logs of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component log sums
     */
    @Override
    public double[] getSumLog() {
        return this.getResults(this.sumLogImpl);
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the mean of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component means
     */
    @Override
    public double[] getMean() {
        return this.getResults(this.meanImpl);
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the standard deviation of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component standard deviations
     */
    @Override
    public double[] getStandardDeviation() {
        final double[] stdDev = new double[this.k];
        if (this.getN() < 1) {
            Arrays.fill(stdDev, Double.NaN);
        } else if (this.getN() < 2) {
            Arrays.fill(stdDev, 0.0);
        } else {
            final RealMatrix matrix = this.covarianceImpl.getResult();
            for (int i = 0; i < this.k; ++i) {
                stdDev[i] = MathLib.sqrt(matrix.getEntry(i, i));
            }
        }
        return stdDev;
    }

    /**
     * Returns the covariance matrix of the values that have been added.
     * 
     * @return the covariance matrix
     */
    @Override
    public RealMatrix getCovariance() {
        return this.covarianceImpl.getResult();
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the maximum of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component maxima
     */
    @Override
    public double[] getMax() {
        return this.getResults(this.maxImpl);
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the minimum of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component minima
     */
    @Override
    public double[] getMin() {
        return this.getResults(this.minImpl);
    }

    /**
     * Returns an array whose i<sup>th</sup> entry is the geometric mean of the
     * i<sup>th</sup> entries of the arrays that have been added using {@link #addValue(double[])}
     * 
     * @return the array of component geometric means
     */
    @Override
    public double[] getGeometricMean() {
        return this.getResults(this.geoMeanImpl);
    }

    /**
     * Generates a text report displaying
     * summary statistics from values that
     * have been added.
     * 
     * @return String with line feeds displaying statistics
     */
    @Override
    public String toString() {
        final String separator = ", ";
        final String suffix = System.getProperty("line.separator");
        final StringBuilder outBuffer = new StringBuilder();
        outBuffer.append("MultivariateSummaryStatistics:" + suffix);
        outBuffer.append("n: " + this.getN() + suffix);
        this.append(outBuffer, this.getMin(), "min: ", separator, suffix);
        this.append(outBuffer, this.getMax(), "max: ", separator, suffix);
        this.append(outBuffer, this.getMean(), "mean: ", separator, suffix);
        this.append(outBuffer, this.getGeometricMean(), "geometric mean: ", separator, suffix);
        this.append(outBuffer, this.getSumSq(), "sum of squares: ", separator, suffix);
        this.append(outBuffer, this.getSumLog(), "sum of logarithms: ", separator, suffix);
        this.append(outBuffer, this.getStandardDeviation(), "standard deviation: ", separator, suffix);
        outBuffer.append("covariance: " + this.getCovariance().toString(MatrixUtils.JAVA_FORMAT) + suffix);
        return outBuffer.toString();
    }

    /**
     * Append a text representation of an array to a buffer.
     * 
     * @param buffer
     *        buffer to fill
     * @param data
     *        data array
     * @param prefix
     *        text prefix
     * @param separator
     *        elements separator
     * @param suffix
     *        text suffix
     */
    private void append(final StringBuilder buffer, final double[] data,
                        final String prefix, final String separator, final String suffix) {
        buffer.append(prefix);
        for (int i = 0; i < data.length; ++i) {
            if (i > 0) {
                buffer.append(separator);
            }
            buffer.append(data[i]);
        }
        buffer.append(suffix);
    }

    /**
     * Resets all statistics and storage
     */
    public void clear() {
        this.n = 0;
        for (int i = 0; i < this.k; ++i) {
            this.minImpl[i].clear();
            this.maxImpl[i].clear();
            this.sumImpl[i].clear();
            this.sumLogImpl[i].clear();
            this.sumSqImpl[i].clear();
            this.geoMeanImpl[i].clear();
            this.meanImpl[i].clear();
        }
        this.covarianceImpl.clear();
    }

    /**
     * Returns true iff <code>object</code> is a <code>MultivariateSummaryStatistics</code> instance and all statistics
     * have the same values as this.
     * 
     * @param object
     *        the object to test equality against.
     * @return true if object equals this
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public boolean equals(final Object object) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (object == this) {
            return true;
        }
        if (!(object instanceof MultivariateSummaryStatistics)) {
            return false;
        }
        final MultivariateSummaryStatistics stat = (MultivariateSummaryStatistics) object;
        return MathArrays.equalsIncludingNaN(stat.getGeometricMean(), this.getGeometricMean()) &&
            MathArrays.equalsIncludingNaN(stat.getMax(), this.getMax()) &&
            MathArrays.equalsIncludingNaN(stat.getMean(), this.getMean()) &&
            MathArrays.equalsIncludingNaN(stat.getMin(), this.getMin()) &&
            Precision.equalsIncludingNaN(stat.getN(), this.getN()) &&
            MathArrays.equalsIncludingNaN(stat.getSum(), this.getSum()) &&
            MathArrays.equalsIncludingNaN(stat.getSumSq(), this.getSumSq()) &&
            MathArrays.equalsIncludingNaN(stat.getSumLog(), this.getSumLog()) &&
            stat.getCovariance().equals(this.getCovariance());
    }

    /**
     * Returns hash code based on values of statistics
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        final int number = 31;
        int result = number + MathUtils.hash(this.getGeometricMean());
        result = result * number + MathUtils.hash(this.getGeometricMean());
        result = result * number + MathUtils.hash(this.getMax());
        result = result * number + MathUtils.hash(this.getMean());
        result = result * number + MathUtils.hash(this.getMin());
        result = result * number + MathUtils.hash(this.getN());
        result = result * number + MathUtils.hash(this.getSum());
        result = result * number + MathUtils.hash(this.getSumSq());
        result = result * number + MathUtils.hash(this.getSumLog());
        result = result * number + this.getCovariance().hashCode();
        return result;
    }

    // Getters and setters for statistics implementations
    /**
     * Sets statistics implementations.
     * 
     * @param newImpl
     *        new implementations for statistics
     * @param oldImpl
     *        old implementations for statistics
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e. if n > 0)
     */
    private void setImpl(final StorelessUnivariateStatistic[] newImpl,
                         final StorelessUnivariateStatistic[] oldImpl) {
        this.checkEmpty();
        this.checkDimension(newImpl.length);
        System.arraycopy(newImpl, 0, oldImpl, 0, newImpl.length);
    }

    /**
     * Returns the currently configured Sum implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the sum
     */
    public StorelessUnivariateStatistic[] getSumImpl() {
        return this.sumImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the Sum.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param sumImplIn
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the Sum
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setSumImpl(final StorelessUnivariateStatistic[] sumImplIn) {
        this.setImpl(sumImplIn, this.sumImpl);
    }

    /**
     * Returns the currently configured sum of squares implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the sum of squares
     */
    public StorelessUnivariateStatistic[] getSumsqImpl() {
        return this.sumSqImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the sum of squares.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param sumsqImpl
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the sum of squares
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setSumsqImpl(final StorelessUnivariateStatistic[] sumsqImpl) {
        this.setImpl(sumsqImpl, this.sumSqImpl);
    }

    /**
     * Returns the currently configured minimum implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the minimum
     */
    public StorelessUnivariateStatistic[] getMinImpl() {
        return this.minImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the minimum.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param minImplIn
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the minimum
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setMinImpl(final StorelessUnivariateStatistic[] minImplIn) {
        this.setImpl(minImplIn, this.minImpl);
    }

    /**
     * Returns the currently configured maximum implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the maximum
     */
    public StorelessUnivariateStatistic[] getMaxImpl() {
        return this.maxImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the maximum.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param maxImplIn
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the maximum
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setMaxImpl(final StorelessUnivariateStatistic[] maxImplIn) {
        this.setImpl(maxImplIn, this.maxImpl);
    }

    /**
     * Returns the currently configured sum of logs implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the log sum
     */
    public StorelessUnivariateStatistic[] getSumLogImpl() {
        return this.sumLogImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the sum of logs.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param sumLogImplIn
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the log sum
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setSumLogImpl(final StorelessUnivariateStatistic[] sumLogImplIn) {
        this.setImpl(sumLogImplIn, this.sumLogImpl);
    }

    /**
     * Returns the currently configured geometric mean implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the geometric mean
     */
    public StorelessUnivariateStatistic[] getGeoMeanImpl() {
        return this.geoMeanImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the geometric mean.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param geoMeanImplIn
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the geometric mean
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setGeoMeanImpl(final StorelessUnivariateStatistic[] geoMeanImplIn) {
        this.setImpl(geoMeanImplIn, this.geoMeanImpl);
    }

    /**
     * Returns the currently configured mean implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the mean
     */
    public StorelessUnivariateStatistic[] getMeanImpl() {
        return this.meanImpl.clone();
    }

    /**
     * <p>
     * Sets the implementation for the mean.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double[]) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param meanImplIn
     *        the StorelessUnivariateStatistic instance to use
     *        for computing the mean
     * @throws DimensionMismatchException
     *         if the array dimension
     *         does not match the one used at construction
     * @throws MathIllegalStateException
     *         if data has already been added
     *         (i.e if n > 0)
     */
    public void setMeanImpl(final StorelessUnivariateStatistic[] meanImplIn) {
        this.setImpl(meanImplIn, this.meanImpl);
    }

    /**
     * Throws MathIllegalStateException if the statistic is not empty.
     * 
     * @throws MathIllegalStateException
     *         if n > 0.
     */
    private void checkEmpty() {
        if (this.n > 0) {
            throw new MathIllegalStateException(
                PatriusMessages.VALUES_ADDED_BEFORE_CONFIGURING_STATISTIC, this.n);
        }
    }

    /**
     * Throws DimensionMismatchException if dimension != k.
     * 
     * @param dimension
     *        dimension to check
     * @throws DimensionMismatchException
     *         if dimension != k
     */
    private void checkDimension(final int dimension) {
        if (dimension != this.k) {
            throw new DimensionMismatchException(dimension, this.k);
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
