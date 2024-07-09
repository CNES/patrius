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
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.GeometricMean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.SecondMoment;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Max;
import fr.cnes.sirius.patrius.math.stat.descriptive.rank.Min;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfLogs;
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.SumOfSquares;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * <p>
 * Computes summary statistics for a stream of data values added using the {@link #addValue(double) addValue} method.
 * The data values are not stored in memory, so this class can be used to compute statistics for very large data
 * streams.
 * </p>
 * <p>
 * The {@link StorelessUnivariateStatistic} instances used to maintain summary state and compute statistics are
 * configurable via setters. For example, the default implementation for the variance can be overridden by calling
 * {@link #setVarianceImpl(StorelessUnivariateStatistic)}. Actual parameters to these methods must implement the
 * {@link StorelessUnivariateStatistic} interface and configuration must be completed before <code>addValue</code> is
 * called. No configuration is necessary to use the default, commons-math provided implementations.
 * </p>
 * <p>
 * Note: This class is not thread-safe. Use {@link SynchronizedSummaryStatistics} if concurrent access from multiple
 * threads is required.
 * </p>
 * 
 * @version $Id: SummaryStatistics.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SummaryStatistics implements StatisticalSummary, Serializable {

    /** Serialization UID */
    private static final long serialVersionUID = -2021321786743555871L;

    /** count of values that have been added */
    private long n = 0;

    /** SecondMoment is used to compute the mean and variance */
    private SecondMoment secondMoment = new SecondMoment();

    /** sum of values that have been added */
    private Sum sum = new Sum();

    /** sum of the square of each value that has been added */
    private SumOfSquares sumsq = new SumOfSquares();

    /** min of values that have been added */
    private Min min = new Min();

    /** max of values that have been added */
    private Max max = new Max();

    /** sumLog of values that have been added */
    private SumOfLogs sumLog = new SumOfLogs();

    /** geoMean of values that have been added */
    private GeometricMean geoMean = new GeometricMean(this.sumLog);

    /** mean of values that have been added */
    private Mean mean = new Mean(this.secondMoment);

    /** variance of values that have been added */
    private Variance variance = new Variance(this.secondMoment);

    /** Sum statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic sumImpl = this.sum;

    /** Sum of squares statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic sumsqImpl = this.sumsq;

    /** Minimum statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic minImpl = this.min;

    /** Maximum statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic maxImpl = this.max;

    /** Sum of log statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic sumLogImpl = this.sumLog;

    /** Geometric mean statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic geoMeanImpl = this.geoMean;

    /** Mean statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic meanImpl = this.mean;

    /** Variance statistic implementation - can be reset by setter. */
    private StorelessUnivariateStatistic varianceImpl = this.variance;

    /**
     * Construct a SummaryStatistics instance
     */
    public SummaryStatistics() {
        // Nothing to do
    }

    /**
     * A copy constructor. Creates a deep-copy of the {@code original}.
     * 
     * @param original
     *        the {@code SummaryStatistics} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public SummaryStatistics(final SummaryStatistics original) {
        copy(original, this);
    }

    /**
     * Return a {@link StatisticalSummaryValues} instance reporting current
     * statistics.
     * 
     * @return Current values of statistics
     */
    public StatisticalSummary getSummary() {
        return new StatisticalSummaryValues(this.getMean(), this.getVariance(), this.getN(),
            this.getMax(), this.getMin(), this.getSum());
    }

    /**
     * Add a value to the data
     * 
     * @param value
     *        the value to add
     */
    public void addValue(final double value) {
        this.sumImpl.increment(value);
        this.sumsqImpl.increment(value);
        this.minImpl.increment(value);
        this.maxImpl.increment(value);
        this.sumLogImpl.increment(value);
        this.secondMoment.increment(value);
        // If mean, variance or geomean have been overridden,
        // need to increment these
        if (this.meanImpl != this.mean) {
            this.meanImpl.increment(value);
        }
        if (this.varianceImpl != this.variance) {
            this.varianceImpl.increment(value);
        }
        if (this.geoMeanImpl != this.geoMean) {
            this.geoMeanImpl.increment(value);
        }
        this.n++;
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
     * Returns the sum of the values that have been added
     * 
     * @return The sum or <code>Double.NaN</code> if no values have been added
     */
    @Override
    public double getSum() {
        return this.sumImpl.getResult();
    }

    /**
     * Returns the sum of the squares of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return The sum of squares
     */
    public double getSumsq() {
        return this.sumsqImpl.getResult();
    }

    /**
     * Returns the mean of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the mean
     */
    @Override
    public double getMean() {
        return this.meanImpl.getResult();
    }

    /**
     * Returns the standard deviation of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the standard deviation
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
     * Returns the (sample) variance of the available values.
     * 
     * <p>
     * This method returns the bias-corrected sample variance (using {@code n - 1} in the denominator). Use
     * {@link #getPopulationVariance()} for the non-bias-corrected population variance.
     * </p>
     * 
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the variance
     */
    @Override
    public double getVariance() {
        return this.varianceImpl.getResult();
    }

    /**
     * Returns the <a href="http://en.wikibooks.org/wiki/Statistics/Summary/Variance">
     * population variance</a> of the values that have been added.
     * 
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the population variance
     */
    public double getPopulationVariance() {
        final Variance populationVariance = new Variance(this.secondMoment);
        populationVariance.setBiasCorrected(false);
        return populationVariance.getResult();
    }

    /**
     * Returns the maximum of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the maximum
     */
    @Override
    public double getMax() {
        return this.maxImpl.getResult();
    }

    /**
     * Returns the minimum of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the minimum
     */
    @Override
    public double getMin() {
        return this.minImpl.getResult();
    }

    /**
     * Returns the geometric mean of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the geometric mean
     */
    public double getGeometricMean() {
        return this.geoMeanImpl.getResult();
    }

    /**
     * Returns the sum of the logs of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * 
     * @return the sum of logs
     * @since 1.2
     */
    public double getSumOfLogs() {
        return this.sumLogImpl.getResult();
    }

    /**
     * Returns a statistic related to the Second Central Moment. Specifically,
     * what is returned is the sum of squared deviations from the sample mean
     * among the values that have been added.
     * <p>
     * Returns <code>Double.NaN</code> if no data values have been added and returns <code>0</code> if there is just one
     * value in the data set.
     * </p>
     * <p>
     * 
     * @return second central moment statistic
     * @since 2.0
     */
    public double getSecondMoment() {
        return this.secondMoment.getResult();
    }

    /**
     * Generates a text report displaying summary statistics from values that
     * have been added.
     * 
     * @return String with line feeds displaying statistics
     * @since 1.2
     */
    @Override
    public String toString() {
        final StringBuilder outBuffer = new StringBuilder();
        final String endl = "\n";
        outBuffer.append("SummaryStatistics:").append(endl);
        outBuffer.append("n: ").append(this.getN()).append(endl);
        outBuffer.append("min: ").append(this.getMin()).append(endl);
        outBuffer.append("max: ").append(this.getMax()).append(endl);
        outBuffer.append("mean: ").append(this.getMean()).append(endl);
        outBuffer.append("geometric mean: ").append(this.getGeometricMean())
            .append(endl);
        outBuffer.append("variance: ").append(this.getVariance()).append(endl);
        outBuffer.append("sum of squares: ").append(this.getSumsq()).append(endl);
        outBuffer.append("standard deviation: ").append(this.getStandardDeviation())
            .append(endl);
        outBuffer.append("sum of logs: ").append(this.getSumOfLogs()).append(endl);
        return outBuffer.toString();
    }

    /**
     * Resets all statistics and storage
     */
    public void clear() {
        this.n = 0;
        this.minImpl.clear();
        this.maxImpl.clear();
        this.sumImpl.clear();
        this.sumLogImpl.clear();
        this.sumsqImpl.clear();
        this.geoMeanImpl.clear();
        this.secondMoment.clear();
        if (this.meanImpl != this.mean) {
            this.meanImpl.clear();
        }
        if (this.varianceImpl != this.variance) {
            this.varianceImpl.clear();
        }
    }

    /**
     * Returns true iff <code>object</code> is a <code>SummaryStatistics</code> instance and all statistics have the
     * same values as this.
     * 
     * @param object
     *        the object to test equality against.
     * @return true if object equals this
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof SummaryStatistics)) {
            return false;
        }
        final SummaryStatistics stat = (SummaryStatistics) object;
        return Precision.equalsIncludingNaN(stat.getGeometricMean(), this.getGeometricMean()) &&
            Precision.equalsIncludingNaN(stat.getMax(), this.getMax()) &&
            Precision.equalsIncludingNaN(stat.getMean(), this.getMean()) &&
            Precision.equalsIncludingNaN(stat.getMin(), this.getMin()) &&
            Precision.equalsIncludingNaN(stat.getN(), this.getN()) &&
            Precision.equalsIncludingNaN(stat.getSum(), this.getSum()) &&
            Precision.equalsIncludingNaN(stat.getSumsq(), this.getSumsq()) &&
            Precision.equalsIncludingNaN(stat.getVariance(), this.getVariance());
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
        result = result * number + MathUtils.hash(this.getSumsq());
        result = result * number + MathUtils.hash(this.getVariance());
        return result;
    }

    // Getters and setters for statistics implementations
    /**
     * Returns the currently configured Sum implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the sum
     * @since 1.2
     */
    public StorelessUnivariateStatistic getSumImpl() {
        return this.sumImpl;
    }

    /**
     * <p>
     * Sets the implementation for the Sum.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param sumImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the Sum
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n >0)
     * @since 1.2
     */
    public void setSumImpl(final StorelessUnivariateStatistic sumImplIn) {
        this.checkEmpty();
        this.sumImpl = sumImplIn;
    }

    /**
     * Returns the currently configured sum of squares implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the sum of squares
     * @since 1.2
     */
    public StorelessUnivariateStatistic getSumsqImpl() {
        return this.sumsqImpl;
    }

    /**
     * <p>
     * Sets the implementation for the sum of squares.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param sumsqImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the sum of squares
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setSumsqImpl(final StorelessUnivariateStatistic sumsqImplIn) {
        this.checkEmpty();
        this.sumsqImpl = sumsqImplIn;
    }

    /**
     * Returns the currently configured minimum implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the minimum
     * @since 1.2
     */
    public StorelessUnivariateStatistic getMinImpl() {
        return this.minImpl;
    }

    /**
     * <p>
     * Sets the implementation for the minimum.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param minImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the minimum
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setMinImpl(final StorelessUnivariateStatistic minImplIn) {
        this.checkEmpty();
        this.minImpl = minImplIn;
    }

    /**
     * Returns the currently configured maximum implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the maximum
     * @since 1.2
     */
    public StorelessUnivariateStatistic getMaxImpl() {
        return this.maxImpl;
    }

    /**
     * <p>
     * Sets the implementation for the maximum.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param maxImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the maximum
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setMaxImpl(final StorelessUnivariateStatistic maxImplIn) {
        this.checkEmpty();
        this.maxImpl = maxImplIn;
    }

    /**
     * Returns the currently configured sum of logs implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the log sum
     * @since 1.2
     */
    public StorelessUnivariateStatistic getSumLogImpl() {
        return this.sumLogImpl;
    }

    /**
     * <p>
     * Sets the implementation for the sum of logs.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param sumLogImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the log sum
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setSumLogImpl(final StorelessUnivariateStatistic sumLogImplIn) {
        this.checkEmpty();
        this.sumLogImpl = sumLogImplIn;
        this.geoMean.setSumLogImpl(sumLogImplIn);
    }

    /**
     * Returns the currently configured geometric mean implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the geometric mean
     * @since 1.2
     */
    public StorelessUnivariateStatistic getGeoMeanImpl() {
        return this.geoMeanImpl;
    }

    /**
     * <p>
     * Sets the implementation for the geometric mean.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param geoMeanImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the geometric mean
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setGeoMeanImpl(final StorelessUnivariateStatistic geoMeanImplIn) {
        this.checkEmpty();
        this.geoMeanImpl = geoMeanImplIn;
    }

    /**
     * Returns the currently configured mean implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the mean
     * @since 1.2
     */
    public StorelessUnivariateStatistic getMeanImpl() {
        return this.meanImpl;
    }

    /**
     * <p>
     * Sets the implementation for the mean.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param meanImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the mean
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setMeanImpl(final StorelessUnivariateStatistic meanImplIn) {
        this.checkEmpty();
        this.meanImpl = meanImplIn;
    }

    /**
     * Returns the currently configured variance implementation
     * 
     * @return the StorelessUnivariateStatistic implementing the variance
     * @since 1.2
     */
    public StorelessUnivariateStatistic getVarianceImpl() {
        return this.varianceImpl;
    }

    /**
     * <p>
     * Sets the implementation for the variance.
     * </p>
     * <p>
     * This method must be activated before any data has been added - i.e., before {@link #addValue(double) addValue}
     * has been used to add data; otherwise an IllegalStateException will be thrown.
     * </p>
     * 
     * @param varianceImplIn
     *        the StorelessUnivariateStatistic instance to use for
     *        computing the variance
     * @throws MathIllegalStateException
     *         if data has already been added (i.e if n > 0)
     * @since 1.2
     */
    public void setVarianceImpl(final StorelessUnivariateStatistic varianceImplIn) {
        this.checkEmpty();
        this.varianceImpl = varianceImplIn;
    }

    /**
     * Throws IllegalStateException if n > 0.
     * 
     * @throws MathIllegalStateException
     *         if data has been added
     */
    private void checkEmpty() {
        if (this.n > 0) {
            throw new MathIllegalStateException(
                PatriusMessages.VALUES_ADDED_BEFORE_CONFIGURING_STATISTIC, this.n);
        }
    }

    /**
     * Returns a copy of this SummaryStatistics instance with the same internal state.
     * 
     * @return a copy of this
     */
    public SummaryStatistics copy() {
        final SummaryStatistics result = new SummaryStatistics();
        // No try-catch or advertised exception because arguments are guaranteed non-null
        copy(this, result);
        return result;
    }

    /**
     * Copies source to dest.
     * <p>
     * Neither source nor dest can be null.
     * </p>
     * 
     * @param sourceIn
     *        SummaryStatistics to copy
     * @param destIn
     *        SummaryStatistics to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public static void copy(final SummaryStatistics sourceIn, final SummaryStatistics destIn) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        MathUtils.checkNotNull(sourceIn);
        MathUtils.checkNotNull(destIn);
        destIn.maxImpl = sourceIn.maxImpl.copy();
        destIn.minImpl = sourceIn.minImpl.copy();
        destIn.sumImpl = sourceIn.sumImpl.copy();
        destIn.sumLogImpl = sourceIn.sumLogImpl.copy();
        destIn.sumsqImpl = sourceIn.sumsqImpl.copy();
        destIn.secondMoment = sourceIn.secondMoment.copy();
        destIn.n = sourceIn.n;

        // Keep commons-math supplied statistics with embedded moments in synch
        if (sourceIn.getVarianceImpl() instanceof Variance) {
            destIn.varianceImpl = new Variance(destIn.secondMoment);
        } else {
            destIn.varianceImpl = sourceIn.varianceImpl.copy();
        }
        if (sourceIn.meanImpl instanceof Mean) {
            destIn.meanImpl = new Mean(destIn.secondMoment);
        } else {
            destIn.meanImpl = sourceIn.meanImpl.copy();
        }
        if (sourceIn.getGeoMeanImpl() instanceof GeometricMean) {
            destIn.geoMeanImpl = new GeometricMean((SumOfLogs) destIn.sumLogImpl);
        } else {
            destIn.geoMeanImpl = sourceIn.geoMeanImpl.copy();
        }

        // Make sure that if stat == statImpl in source, same
        // holds in dest; otherwise copy stat
        if (sourceIn.geoMean == sourceIn.geoMeanImpl) {
            destIn.geoMean = (GeometricMean) destIn.geoMeanImpl;
        } else {
            GeometricMean.copy(sourceIn.geoMean, destIn.geoMean);
        }
        if (sourceIn.max == sourceIn.maxImpl) {
            destIn.max = (Max) destIn.maxImpl;
        } else {
            Max.copy(sourceIn.max, destIn.max);
        }
        if (sourceIn.mean == sourceIn.meanImpl) {
            destIn.mean = (Mean) destIn.meanImpl;
        } else {
            Mean.copy(sourceIn.mean, destIn.mean);
        }
        if (sourceIn.min == sourceIn.minImpl) {
            destIn.min = (Min) destIn.minImpl;
        } else {
            Min.copy(sourceIn.min, destIn.min);
        }
        if (sourceIn.sum == sourceIn.sumImpl) {
            destIn.sum = (Sum) destIn.sumImpl;
        } else {
            Sum.copy(sourceIn.sum, destIn.sum);
        }
        if (sourceIn.variance == sourceIn.varianceImpl) {
            destIn.variance = (Variance) destIn.varianceImpl;
        } else {
            Variance.copy(sourceIn.variance, destIn.variance);
        }
        if (sourceIn.sumLog == sourceIn.sumLogImpl) {
            destIn.sumLog = (SumOfLogs) destIn.sumLogImpl;
        } else {
            SumOfLogs.copy(sourceIn.sumLog, destIn.sumLog);
        }
        if (sourceIn.sumsq == sourceIn.sumsqImpl) {
            destIn.sumsq = (SumOfSquares) destIn.sumsqImpl;
        } else {
            SumOfSquares.copy(sourceIn.sumsq, destIn.sumsq);
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
