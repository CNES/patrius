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
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

/**
 * Computes the Kurtosis of the available values.
 * <p>
 * We use the following (unbiased) formula to define kurtosis:
 * </p>
 * <p>
 * kurtosis = { [n(n+1) / (n -1)(n - 2)(n-3)] sum[(x_i - mean)^4] / std^4 } - [3(n-1)^2 / (n-2)(n-3)]
 * </p>
 * <p>
 * where n is the number of values, mean is the {@link Mean} and std is the {@link StandardDeviation}
 * </p>
 * <p>
 * Note that this statistic is undefined for n < 4. <code>Double.Nan</code> is returned when there is not sufficient
 * data to compute the statistic.
 * </p>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If multiple threads access an instance of this
 * class concurrently, and at least one of the threads invokes the <code>increment()</code> or <code>clear()</code>
 * method, it must be synchronized externally.
 * </p>
 * 
 * @version $Id: Kurtosis.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Kurtosis extends AbstractStorelessUnivariateStatistic implements Serializable {

    /** Threshold 1E-20. */
    private static final double THRESHOLD20 = 10E-20;

     /** Serializable UID. */
    private static final long serialVersionUID = 2784465764798260919L;

    /** Fourth Moment on which this statistic is based */
    protected FourthMoment moment;

    /**
     * Determines whether or not this statistic can be incremented or cleared.
     * <p>
     * Statistics based on (constructed from) external moments cannot be incremented or cleared.
     * </p>
     */
    protected boolean incMoment;

    /**
     * Construct a Kurtosis
     */
    public Kurtosis() {
        super();
        this.incMoment = true;
        this.moment = new FourthMoment();
    }

    /**
     * Construct a Kurtosis from an external moment
     * 
     * @param m4
     *        external Moment
     */
    public Kurtosis(final FourthMoment m4) {
        super();
        this.incMoment = false;
        this.moment = m4;
    }

    /**
     * Copy constructor, creates a new {@code Kurtosis} identical
     * to the {@code original}
     * 
     * @param original
     *        the {@code Kurtosis} instance to copy
     * @throws NullArgumentException
     *         if original is null
     */
    public Kurtosis(final Kurtosis original) {
        super();
        copy(original, this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that when {@link #Kurtosis(FourthMoment)} is used to create a Variance, this method does nothing. In that
     * case, the FourthMoment should be incremented directly.
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
    public double getResult() {
        double kurtosis = Double.NaN;
        if (this.moment.getN() > 3) {
            final double variance = this.moment.m2 / (this.moment.n - 1);
            if (this.moment.n <= 3 || variance < THRESHOLD20) {
                kurtosis = 0.0;
            } else {
                final double n = this.moment.n;
                kurtosis =
                    (n * (n + 1) * this.moment.getResult() -
                        3 * this.moment.m2 * this.moment.m2 * (n - 1)) /
                        ((n - 1) * (n - 2) * (n - 3) * variance * variance);
            }
        }
        return kurtosis;
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
    public long getN() {
        return this.moment.getN();
    }

    /* UnvariateStatistic Approach */

    /**
     * Returns the kurtosis of the entries in the specified portion of the
     * input array.
     * <p>
     * See {@link Kurtosis} for details on the computing algorithm.
     * </p>
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
     * @return the kurtosis of the values or Double.NaN if length is less than 4
     * @throws MathIllegalArgumentException
     *         if the input array is null or the array
     *         index parameters are not valid
     */
    @Override
    public double evaluate(final double[] values, final int begin, final int length) {
        // Initialize the kurtosis
        double kurt = Double.NaN;

        if (test(values, begin, length) && length > 3) {

            // Compute the mean and standard deviation
            final Variance variance = new Variance();
            variance.incrementAll(values, begin, length);
            final double mean = variance.moment.m1;
            final double stdDev = MathLib.sqrt(variance.getResult());

            // Sum the ^4 of the distance from the mean divided by the
            // standard deviation
            double accum3 = 0.0;
            for (int i = begin; i < begin + length; i++) {
                accum3 += MathLib.pow(values[i] - mean, 4.0);
            }
            accum3 /= MathLib.pow(stdDev, 4.0d);

            // Get N
            final double n0 = length;

            final double coefficientOne =
                (n0 * (n0 + 1)) / ((n0 - 1) * (n0 - 2) * (n0 - 3));
            final double termTwo =
                (3 * MathLib.pow(n0 - 1, 2.0)) / ((n0 - 2) * (n0 - 3));

            // Calculate kurtosis
            kurt = (coefficientOne * accum3) - termTwo;
        }
        return kurt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Kurtosis copy() {
        final Kurtosis result = new Kurtosis();
        // No try-catch because args are guaranteed non-null
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
     *        Kurtosis to copy
     * @param dest
     *        Kurtosis to copy to
     * @throws NullArgumentException
     *         if either source or dest is null
     */
    public static void copy(final Kurtosis source, final Kurtosis dest) {
        MathUtils.checkNotNull(source);
        MathUtils.checkNotNull(dest);
        dest.setData(source.getDataRef());
        dest.moment = source.moment.copy();
        dest.incMoment = source.incMoment;
    }

}
