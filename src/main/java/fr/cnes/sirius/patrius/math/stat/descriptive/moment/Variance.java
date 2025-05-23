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
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.WeightedEvaluation;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computes the variance of the available values. By default, the unbiased
 * "sample variance" definitional formula is used:
 * <p>
 * variance = sum((x_i - mean)^2) / (n - 1)
 * </p>
 * <p>
 * where mean is the {@link Mean} and <code>n</code> is the number of sample observations.
 * </p>
 * <p>
 * The definitional formula does not have good numerical properties, so this implementation does not compute the
 * statistic using the definitional formula.
 * <ul>
 * <li>The <code>getResult</code> method computes the variance using updating formulas based on West's algorithm, as
 * described in <a href="http://doi.acm.org/10.1145/359146.359152"> Chan, T. F. and J. G. Lewis 1979, <i>Communications
 * of the ACM</i>, vol. 22 no. 9, pp. 526-531.</a></li>
 * <li>The <code>evaluate</code> methods leverage the fact that they have the full array of values in memory to execute
 * a two-pass algorithm. Specifically, these methods use the "corrected two-pass algorithm" from Chan, Golub, Levesque,
 * <i>Algorithms for Computing the Sample Variance</i>, American Statistician, vol. 37, no. 3 (1983) pp. 242-247.</li>
 * </ul>
 * Note that adding values using <code>increment</code> or <code>incrementAll</code> and then executing
 * <code>getResult</code> will sometimes give a different, less accurate, result than executing <code>evaluate</code>
 * with the full array of values. The former approach should only be used when the full array of values is not
 * available.
 * </p>
 * <p>
 * The "population variance" ( sum((x_i - mean)^2) / n ) can also be computed using this statistic. The
 * <code>isBiasCorrected</code> property determines whether the "population" or "sample" value is returned by the
 * <code>evaluate</code> and <code>getResult</code> methods. To compute population variances, set this property to
 * <code>false.</code>
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Variance.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Variance extends AbstractStorelessUnivariateStatistic implements Serializable, WeightedEvaluation {

     /** Serializable UID. */
    private static final long serialVersionUID = -9111962718267217978L;

    /** SecondMoment is used in incremental calculation of Variance */
    protected SecondMoment moment = null;

    /**
     * Whether or not {@link #increment(double)} should increment
     * the internal second moment. When a Variance is constructed with an
     * external SecondMoment as a constructor parameter, this property is
     * set to false and increments must be applied to the second moment
     * directly.
     */
    protected boolean incMoment = true;

    /**
     * Whether or not bias correction is applied when computing the
     * value of the statistic. True means that bias is corrected. See {@link Variance} for details on the formula.
     */
    private boolean biasCorrectedFlag = true;

    /**
     * Constructs a Variance with default (true) <code>isBiasCorrected</code> property.
     */
    public Variance() {
        super();
        this.moment = new SecondMoment();
    }

    /**
     * Constructs a Variance based on an external second moment.
     * When this constructor is used, the statistic may only be
     * incremented via the moment, i.e., {@link #increment(double)} does nothing; whereas {@code m2.increment(value)}
     * increments
     * both {@code m2} and the Variance instance constructed from it.
     * 
     * @param m2
     *        the SecondMoment (Third or Fourth moments work
     *        here as well.)
     */
    public Variance(final SecondMoment m2) {
        super();
        this.incMoment = false;
        this.moment = m2;
    }

    /**
     * Constructs a Variance with the specified <code>isBiasCorrected</code> property
     * 
     * @param isBiasCorrectedIn
     *        setting for bias correction - true means
     *        bias will be corrected and is equivalent to using the argumentless
     *        constructor
     */
    public Variance(final boolean isBiasCorrectedIn) {
        super();
        this.moment = new SecondMoment();
        this.biasCorrectedFlag = isBiasCorrectedIn;
    }

    /**
     * Constructs a Variance with the specified <code>isBiasCorrected</code> property and the supplied external second
     * moment.
     * 
     * @param isBiasCorrectedIn
     *        setting for bias correction - true means
     *        bias will be corrected
     * @param m2
     *        the SecondMoment (Third or Fourth moments work
     *        here as well.)
     */
    public Variance(final boolean isBiasCorrectedIn, final SecondMoment m2) {
        super();
        this.incMoment = false;
        this.moment = m2;
        this.biasCorrectedFlag = isBiasCorrectedIn;
    }

    /**
     * Copy constructor, creates a new {@code Variance} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Variance} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Variance(final Variance original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If all values are available, it is more accurate to use {@link #evaluate(double[])} rather than adding values one
     * at a time using this method and then executing {@link #getResult}, since <code>evaluate</code> leverages the fact
     * that is has the full list of values together to execute a two-pass algorithm. See {@link Variance}.
     * </p>
     * 
     * <p>
     * Note also that when {@link #Variance(SecondMoment)} is used to create a Variance, this method does nothing. In
     * that case, the SecondMoment should be incremented directly.
     * </p>
     */
    @Override
    public void increment(final double d) {
        if (this.incMoment) {
            this.moment.increment(d);
        }
    }

    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double getResult() {
        // CHECKSTYLE: resume ReturnCount check
        if (this.moment.n == 0) {
            return Double.NaN;
        } else if (this.moment.n == 1) {
            return 0d;
        } else {
            if (this.biasCorrectedFlag) {
                return this.moment.m2 / (this.moment.n - 1d);
            }
            return this.moment.m2 / (this.moment.n);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getN() {
        return this.moment.getN();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        if (this.incMoment) {
            this.moment.clear();
        }
    }

    /**
     * Returns the variance of the entries in the input array, or <code>Double.NaN</code> if the array is empty.
     * <p>
     * See {@link Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
     * </p>
     * 
     * @param values
     *        the input array
     * @return the variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the array is null
     */
    @Override
    public double evaluate(final double[] values) {
        if (values == null) {
            throw new NullArgumentException(PatriusMessages.INPUT_ARRAY);
        }
        return this.evaluate(values, 0, values.length);
    }

    /**
     * Returns the variance of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * See {@link Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
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
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {

        double var = Double.NaN;

        if (test(values, begin, length)) {
            this.clear();
            if (length == 1) {
                var = 0.0;
            } else if (length > 1) {
                final Mean mean = new Mean();
                final double m = mean.evaluate(values, begin, length);
                var = this.evaluate(values, m, begin, length);
            }
        }
        return var;
    }

    /**
     * <p>
     * Returns the weighted variance of the entries in the specified portion of the input array, or
     * <code>Double.NaN</code> if the designated subarray is empty.
     * </p>
     * <p>
     * Uses the formula
     * 
     * <pre>
     *   &Sigma;(weights[i]*(values[i] - weightedMean)<sup>2</sup>)/(&Sigma;(weights[i]) - 1)
     * </pre>
     * 
     * where weightedMean is the weighted mean
     * </p>
     * <p>
     * This formula will not return the same result as the unweighted variance when all weights are equal, unless all
     * weights are equal to 1. The formula assumes that weights are to be treated as "expansion values," as will be the
     * case if for example the weights represent frequency counts. To normalize weights so that the denominator in the
     * variance computation equals the length of the input vector minus one, use
     * 
     * <pre>
     *   <code>evaluate(values, MathArrays.normalizeArray(weights, values.length)); </code>
     * </pre>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>IllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * <li>the start and length arguments do not determine a valid array</li>
     * </ul>
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if either array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the weighted variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    @Override
    public double evaluate(final double[] values, final double[] weights,
                           final int begin, final int length) {

        double var = Double.NaN;

        if (this.test(values, weights, begin, length)) {
            this.clear();
            if (length == 1) {
                var = 0.0;
            } else if (length > 1) {
                final Mean mean = new Mean();
                final double m = mean.evaluate(values, weights, begin, length);
                var = this.evaluate(values, weights, m, begin, length);
            }
        }
        return var;
    }

    /**
     * <p>
     * Returns the weighted variance of the entries in the the input array.
     * </p>
     * <p>
     * Uses the formula
     * 
     * <pre>
     *   &Sigma;(weights[i]*(values[i] - weightedMean)<sup>2</sup>)/(&Sigma;(weights[i]) - 1)
     * </pre>
     * 
     * where weightedMean is the weighted mean
     * </p>
     * <p>
     * This formula will not return the same result as the unweighted variance when all weights are equal, unless all
     * weights are equal to 1. The formula assumes that weights are to be treated as "expansion values," as will be the
     * case if for example the weights represent frequency counts. To normalize weights so that the denominator in the
     * variance computation equals the length of the input vector minus one, use
     * 
     * <pre>
     *   <code>evaluate(values, MathArrays.normalizeArray(weights, values.length)); </code>
     * </pre>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * </ul>
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if either array is null.
     * </p>
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @return the weighted variance of the values
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    @Override
    public double evaluate(final double[] values, final double[] weights) {
        return this.evaluate(values, weights, 0, values.length);
    }

    /**
     * Returns the variance of the entries in the specified portion of
     * the input array, using the precomputed mean value. Returns <code>Double.NaN</code> if the designated subarray is
     * empty.
     * <p>
     * See {@link Variance} for details on the computing algorithm.
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
     * <p>
     * Does not change the internal state of the statistic.
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
    public double evaluate(final double[] values, final double mean,
                           final int begin, final int length) {

        // Initialization
        double var = Double.NaN;

        if (test(values, begin, length)) {
            if (length == 1) {
                var = 0.0;
            } else if (length > 1) {
                double accum = 0.0;
                double dev = 0.0;
                double accum2 = 0.0;
                for (int i = begin; i < begin + length; i++) {
                    dev = values[i] - mean;
                    accum += dev * dev;
                    accum2 += dev;
                }
                final double len = length;
                if (this.biasCorrectedFlag) {
                    // Bias correction
                    var = (accum - (accum2 * accum2 / len)) / (len - 1.0);
                } else {
                    // No bias correction
                    var = (accum - (accum2 * accum2 / len)) / len;
                }
            }
        }
        // Return result
        return var;
    }

    /**
     * Returns the variance of the entries in the input array, using the
     * precomputed mean value. Returns <code>Double.NaN</code> if the array
     * is empty.
     * <p>
     * See {@link Variance} for details on the computing algorithm.
     * </p>
     * <p>
     * If <code>isBiasCorrected</code> is <code>true</code> the formula used assumes that the supplied mean value is the
     * arithmetic mean of the sample data, not a known population parameter. If the mean is a known population
     * parameter, or if the "population" version of the variance is desired, set <code>isBiasCorrected</code> to
     * <code>false</code> before invoking this method.
     * </p>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
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
    public double evaluate(final double[] values, final double mean) {
        return this.evaluate(values, mean, 0, values.length);
    }

    /**
     * Returns the weighted variance of the entries in the specified portion of
     * the input array, using the precomputed weighted mean value. Returns <code>Double.NaN</code> if the designated
     * subarray is empty.
     * <p>
     * Uses the formula
     * 
     * <pre>
     *   &Sigma;(weights[i]*(values[i] - mean)<sup>2</sup>)/(&Sigma;(weights[i]) - 1)
     * </pre>
     * 
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the weighted arithmetic mean of the sample data, not a
     * known population parameter. This method is supplied only to save computation when the mean has already been
     * computed.
     * </p>
     * <p>
     * This formula will not return the same result as the unweighted variance when all weights are equal, unless all
     * weights are equal to 1. The formula assumes that weights are to be treated as "expansion values," as will be the
     * case if for example the weights represent frequency counts. To normalize weights so that the denominator in the
     * variance computation equals the length of the input vector minus one, use
     * 
     * <pre>
     *   <code>evaluate(values, MathArrays.normalizeArray(weights, values.length), mean); </code>
     * </pre>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * <li>the start and length arguments do not determine a valid array</li>
     * </ul>
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
     * </p>
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @param mean
     *        the precomputed weighted mean value
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    public double evaluate(final double[] values, final double[] weights,
                           final double mean, final int begin, final int length) {

        // Initialization
        double var = Double.NaN;

        // check input then compute variance
        if (this.test(values, weights, begin, length)) {
            if (length == 1) {
                // zero if only one element
                var = 0.0;
            } else if (length > 1) {
                double accum = 0.0;
                double dev = 0.0;
                double accum2 = 0.0;
                for (int i = begin; i < begin + length; i++) {
                    dev = values[i] - mean;
                    accum += weights[i] * (dev * dev);
                    accum2 += weights[i] * dev;
                }

                double sumWts = 0;
                for (int i = begin; i < begin + length; i++) {
                    sumWts += weights[i];
                }

                if (this.biasCorrectedFlag) {
                    // Bias correction
                    var = (accum - (accum2 * accum2 / sumWts)) / (sumWts - 1.0);
                } else {
                    // No bias correction
                    var = (accum - (accum2 * accum2 / sumWts)) / sumWts;
                }
            }
        }
        // Return result
        return var;
    }

    /**
     * <p>
     * Returns the weighted variance of the values in the input array, using the precomputed weighted mean value.
     * </p>
     * <p>
     * Uses the formula
     * 
     * <pre>
     *   &Sigma;(weights[i]*(values[i] - mean)<sup>2</sup>)/(&Sigma;(weights[i]) - 1)
     * </pre>
     * 
     * </p>
     * <p>
     * The formula used assumes that the supplied mean value is the weighted arithmetic mean of the sample data, not a
     * known population parameter. This method is supplied only to save computation when the mean has already been
     * computed.
     * </p>
     * <p>
     * This formula will not return the same result as the unweighted variance when all weights are equal, unless all
     * weights are equal to 1. The formula assumes that weights are to be treated as "expansion values," as will be the
     * case if for example the weights represent frequency counts. To normalize weights so that the denominator in the
     * variance computation equals the length of the input vector minus one, use
     * 
     * <pre>
     *   <code>evaluate(values, MathArrays.normalizeArray(weights, values.length), mean); </code>
     * </pre>
     * <p>
     * Returns 0 for a single-value (i.e. length = 1) sample.
     * </p>
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if any of the following are true:
     * <ul>
     * <li>the values array is null</li>
     * <li>the weights array is null</li>
     * <li>the weights array does not have the same length as the values array</li>
     * <li>the weights array contains one or more infinite values</li>
     * <li>the weights array contains one or more NaN values</li>
     * <li>the weights array contains negative values</li>
     * </ul>
     * </p>
     * <p>
     * Does not change the internal state of the statistic.
     * </p>
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @param mean
     *        the precomputed weighted mean value
     * @return the variance of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    public double evaluate(final double[] values, final double[] weights, final double mean) {
        return this.evaluate(values, weights, mean, 0, values.length);
    }

    /**
     * @return Returns the isBiasCorrected.
     */
    public boolean isBiasCorrected() {
        return this.biasCorrectedFlag;
    }

    /**
     * @param biasCorrected
     *        The isBiasCorrected to set.
     */
    public void setBiasCorrected(final boolean biasCorrected) {
        this.biasCorrectedFlag = biasCorrected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Variance copy() {
        final Variance result = new Variance();
        // No try-catch or advertised exception because parameters are guaranteed non-null
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
     *        Variance to copy
     * @param dest
     *        Variance to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Variance source, final Variance dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.moment = source.moment.copy();
        dest.biasCorrectedFlag = source.biasCorrectedFlag;
        dest.incMoment = source.incMoment;
    }
}
