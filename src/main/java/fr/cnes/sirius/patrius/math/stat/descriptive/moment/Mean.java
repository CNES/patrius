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
import fr.cnes.sirius.patrius.math.stat.descriptive.summary.Sum;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * <p>
 * Computes the arithmetic mean of a set of values. Uses the definitional formula:
 * </p>
 * <p>
 * mean = sum(x_i) / n
 * </p>
 * <p>
 * where <code>n</code> is the number of observations.
 * </p>
 * <p>
 * When {@link #increment(double)} is used to add data incrementally from a stream of (unstored) values, the value of
 * the statistic that {@link #getResult()} returns is computed using the following recursive updating algorithm:
 * </p>
 * <ol>
 * <li>Initialize <code>m = </code> the first value</li>
 * <li>For each additional value, update using <br>
 * <code>m = m + (new value - m) / (number of observations)</code></li>
 * </ol>
 * <p>
 * If {@link #evaluate(double[])} is used to compute the mean of an array of stored values, a two-pass, corrected
 * algorithm is used, starting with the definitional formula computed using the array of stored values and then
 * correcting this by adding the mean deviation of the data values from the arithmetic mean. See, e.g. "Comparison of
 * Several Algorithms for Computing Sample Means and Variances," Robert F. Ling, Journal of the American Statistical
 * Association, Vol. 69, No. 348 (Dec., 1974), pp. 859-866.
 * </p>
 * <p>
 * Returns <code>Double.NaN</code> if the dataset is empty.
 * </p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access an instance of this class concurrently, and at least
 * one of the threads invokes the <code>increment()</code> or <code>clear()</code> method, it must be synchronized
 * externally.
 * 
 * @version $Id: Mean.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ShortClassName")
public class Mean extends AbstractStorelessUnivariateStatistic
    implements Serializable, WeightedEvaluation {

     /** Serializable UID. */
    private static final long serialVersionUID = -1296043746617791564L;

    /** First moment on which this statistic is based. */
    protected FirstMoment moment;

    /**
     * Determines whether or not this statistic can be incremented or cleared.
     * <p>
     * Statistics based on (constructed from) external moments cannot be incremented or cleared.
     * </p>
     */
    protected boolean incMoment;

    /** Constructs a Mean. */
    public Mean() {
        super();
        this.incMoment = true;
        this.moment = new FirstMoment();
    }

    /**
     * Constructs a Mean with an External Moment.
     * 
     * @param m1
     *        the moment
     */
    public Mean(final FirstMoment m1) {
        super();
        this.moment = m1;
        this.incMoment = false;
    }

    /**
     * Copy constructor, creates a new {@code Mean} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Mean} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Mean(final Mean original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that when {@link #Mean(FirstMoment)} is used to create a Mean, this method does nothing. In that case, the
     * FirstMoment should be incremented directly.
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
    @Override
    public void clear() {
        if (this.incMoment) {
            this.moment.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getResult() {
        return this.moment.m1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getN() {
        return this.moment.getN();
    }

    /**
     * Returns the arithmetic mean of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if the array is null.
     * </p>
     * <p>
     * See {@link Mean} for details on the computing algorithm.
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
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        if (test(values, begin, length)) {
            final Sum sum = new Sum();
            final double sampleSize = length;

            // Compute initial estimate using definitional formula
            final double xbar = sum.evaluate(values, begin, length) / sampleSize;

            // Compute correction factor in second pass
            double correction = 0;
            for (int i = begin; i < begin + length; i++) {
                correction += values[i] - xbar;
            }
            return xbar + (correction / sampleSize);
        }
        // length = 0, return Double.NaN
        return Double.NaN;
    }

    /**
     * Returns the weighted arithmetic mean of the entries in the specified portion of
     * the input array, or <code>Double.NaN</code> if the designated subarray
     * is empty.
     * <p>
     * Throws <code>IllegalArgumentException</code> if either array is null.
     * </p>
     * <p>
     * See {@link Mean} for details on the computing algorithm. The two-pass algorithm described above is used here,
     * with weights applied in computing both the original estimate and the correction factor.
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
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @param begin
     *        index of the first array element to include
     * @param length
     *        the number of elements to include
     * @return the mean of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    @Override
    public double evaluate(final double[] values, final double[] weights,
                           final int begin, final int length) {
        if (!test(values, begin, length, false)) {
            return Double.NaN;
        }

        // check input variables
        this.test(values, weights, begin, length);

        // initialize sum
        final Sum sum = new Sum();

        // Compute initial estimate using definitional formula
        final double sumw = sum.evaluate(weights, begin, length);
        final double xbarw = sum.evaluate(values, weights, begin, length) / sumw;

        // Compute correction factor in second pass
        double correction = 0;
        for (int i = begin; i < begin + length; i++) {
            correction += weights[i] * (values[i] - xbarw);
        }
        return xbarw + (correction / sumw);
    }

    /**
     * Returns the weighted arithmetic mean of the entries in the input array.
     * <p>
     * Throws <code>MathIllegalArgumentException</code> if either array is null.
     * </p>
     * <p>
     * See {@link Mean} for details on the computing algorithm. The two-pass algorithm described above is used here,
     * with weights applied in computing both the original estimate and the correction factor.
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
     * 
     * @param values
     *        the input array
     * @param weights
     *        the weights array
     * @return the mean of the values or Double.NaN if length = 0
     * @throws MathIllegalArgumentException
     *         if the parameters are not valid
     * @since 2.1
     */
    @Override
    public double evaluate(final double[] values, final double[] weights) {
        return this.evaluate(values, weights, 0, values.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mean copy() {
        final Mean result = new Mean();
        // No try-catch or advertised exception because args are guaranteed non-null
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
     *        Mean to copy
     * @param dest
     *        Mean to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Mean source, final Mean dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.incMoment = source.incMoment;
        dest.moment = source.moment.copy();
    }
}
