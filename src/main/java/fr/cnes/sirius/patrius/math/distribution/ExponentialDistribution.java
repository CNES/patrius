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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.ResizableDoubleArray;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the exponential distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Exponential_distribution">Exponential distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/ExponentialDistribution.html">Exponential distribution (MathWorld)</a>
 * @version $Id: ExponentialDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ExponentialDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Serializable version identifier */
    private static final long serialVersionUID = 2401296428283614780L;
    /**
     * Used when generating Exponential samples.
     * Table containing the constants
     * q_i = sum_{j=1}^i (ln 2)^j/j! = ln 2 + (ln 2)^2/2 + ... + (ln 2)^i/i!
     * until the largest representable fraction below 1 is exceeded.
     * 
     * Note that
     * 1 = 2 - 1 = exp(ln 2) - 1 = sum_{n=1}^infty (ln 2)^n / n!
     * thus q_i -> 1 as i -> +inf,
     * so the higher i, the closer to one we get (the series is not alternating).
     * 
     * By trying, n = 16 in Java is enough to reach 1.0.
     */
    private static final double[] EXPONENTIAL_SA_QI;
    /** The mean of this distribution. */
    private final double mean;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Initialize tables.
     */
    static {
        /**
         * Filling EXPONENTIAL_SA_QI table.
         * Note that we don't want qi = 0 in the table.
         */
        final double ln2 = MathLib.log(2);
        double qi = 0;
        int i = 1;

        /**
         * ArithmeticUtils provides factorials up to 20, so let's use that
         * limit together with Precision.EPSILON to generate the following
         * code (a priori, we know that there will be 16 elements, but it is
         * better to not hardcode it).
         */
        final ResizableDoubleArray ra = new ResizableDoubleArray(20);

        while (qi < 1) {
            qi += MathLib.pow(ln2, i) / ArithmeticUtils.factorial(i);
            ra.addElement(qi);
            ++i;
        }

        EXPONENTIAL_SA_QI = ra.getElements();
    }

    /**
     * Create an exponential distribution with the given mean.
     * 
     * @param meanIn
     *        mean of this distribution.
     */
    public ExponentialDistribution(final double meanIn) {
        this(meanIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create an exponential distribution with the given mean.
     * 
     * @param meanIn
     *        Mean of this distribution.
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code mean <= 0}.
     * @since 2.1
     */
    public ExponentialDistribution(final double meanIn, final double inverseCumAccuracy) {
        this(new Well19937c(), meanIn, inverseCumAccuracy);
    }

    /**
     * Creates an exponential distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param meanIn
     *        Mean of this distribution.
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code mean <= 0}.
     * @since 3.1
     */
    public ExponentialDistribution(final RandomGenerator rng,
        final double meanIn,
        final double inverseCumAccuracy) {
        super(rng);

        if (meanIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.MEAN, meanIn);
        }
        this.mean = meanIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Access the mean.
     * 
     * @return the mean.
     */
    public double getMean() {
        return this.mean;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        if (x < 0) {
            return 0;
        }
        return MathLib.exp(-x / this.mean) / this.mean;
    }

    /**
     * {@inheritDoc}
     * 
     * The implementation of this method is based on:
     * <ul>
     * <li>
     * <a href="http://mathworld.wolfram.com/ExponentialDistribution.html"> 
     * Exponential Distribution</a>, equation (1).</li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(final double x) {
        final double ret;
        if (x <= 0.0) {
            ret = 0.0;
        } else {
            ret = 1.0 - MathLib.exp(-x / this.mean);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * Returns {@code 0} when {@code p= = 0} and {@code Double.POSITIVE_INFINITY} when {@code p == 1}.
     */
    @Override
    public double inverseCumulativeProbability(final double p) {
        final double ret;

        if (p < 0.0 || p > 1.0) {
            throw new OutOfRangeException(p, 0.0, 1.0);
        } else if (p == 1.0) {
            ret = Double.POSITIVE_INFINITY;
        } else {
            ret = -this.mean * MathLib.log(1.0 - p);
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <strong>Algorithm Description</strong>: this implementation uses the <a
     * href="http://www.jesus.ox.ac.uk/~clifford/a5/chap1/node5.html"> Inversion Method</a> to generate exponentially
     * distributed random values from uniform deviates.
     * </p>
     * 
     * @return a random value.
     * @since 2.2
     */
    @Override
    public double sample() {
        // Step 1:
        double a = 0;
        double u = this.random.nextDouble();

        // Step 2 and 3:
        while (u < HALF) {
            a += EXPONENTIAL_SA_QI[0];
            u *= 2;
        }

        // Step 4 (now u >= 0.5):
        u += u - 1;

        // Step 5:
        if (u <= EXPONENTIAL_SA_QI[0]) {
            return this.mean * (a + u);
        }

        // Step 6:
        // Should be 1, be we iterate before it in while using 0
        int i = 0;
        double u2 = this.random.nextDouble();
        double umin = u2;

        // Step 7 and 8:
        do {
            ++i;
            u2 = this.random.nextDouble();

            if (u2 < umin) {
                umin = u2;
            }

            // Step 8:
            // Ensured to exit since EXPONENTIAL_SA_QI[MAX] = 1
        } while (u > EXPONENTIAL_SA_QI[i]);

        return this.mean * (a + umin * EXPONENTIAL_SA_QI[0]);
    }

    /** {@inheritDoc} */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For mean parameter {@code k}, the mean is {@code k}.
     */
    @Override
    public double getNumericalMean() {
        return this.getMean();
    }

    /**
     * {@inheritDoc}
     * 
     * For mean parameter {@code k}, the variance is {@code k^2}.
     */
    @Override
    public double getNumericalVariance() {
        final double m = this.getMean();
        return m * m;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 0 no matter the mean parameter.
     * 
     * @return lower bound of the support (always 0)
     */
    @Override
    public double getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is always positive infinity
     * no matter the mean parameter.
     * 
     * @return upper bound of the support (always Double.POSITIVE_INFINITY)
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns true if support contains lower bound.
     * 
     * @return true
     */
    public boolean isSupportLowerBoundInclusive() {
        return true;
    }

    /**
     * Returns true if support contains lower bound.
     * 
     * @return false
     */
    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * The support of this distribution is connected.
     * 
     * @return {@code true}
     */
    @Override
    public boolean isSupportConnected() {
        return true;
    }
}
