/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Base class for integer-valued discrete distributions. Default
 * implementations are provided for some of the methods that do not vary
 * from distribution to distribution.
 * 
 * @version $Id: AbstractIntegerDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class AbstractIntegerDistribution
    implements IntegerDistribution, Serializable {
    /** Serializable version identifier */
    private static final long serialVersionUID = -1146319659338487221L;

    /**
     * RNG instance used to generate samples from the distribution.
     * 
     * @since 3.1
     */
    protected final RandomGenerator random;

    /**
     * @param rng
     *        Random number generator.
     * @since 3.1
     */
    protected AbstractIntegerDistribution(final RandomGenerator rng) {
        this.random = rng;
    }

    /**
     * {@inheritDoc}
     * 
     * The default implementation uses the identity
     * <p>
     * {@code P(x0 < X <= x1) = P(X <= x1) - P(X <= x0)}
     * </p>
     */
    @Override
    public double cumulativeProbability(final int x0, final int x1) {
        if (x1 < x0) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_ENDPOINT_ABOVE_UPPER_ENDPOINT,
                x0, x1, true);
        }
        return this.cumulativeProbability(x1) - this.cumulativeProbability(x0);
    }

    /**
     * {@inheritDoc}
     * 
     * The default implementation returns
     * <ul>
     * <li>{@link #getSupportLowerBound()} for {@code p = 0},</li>
     * <li>{@link #getSupportUpperBound()} for {@code p = 1}, and</li>
     * <li>{@link #solveInverseCumulativeProbability(double, int, int)} for {@code 0 < p < 1}.</li>
     * </ul>
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: model - Commons-Math code kept as such
    @Override
    public int inverseCumulativeProbability(final double p) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        if (p < 0.0 || p > 1.0) {
            // Out of range exception
            throw new OutOfRangeException(p, 0, 1);
        }

        // Get lower bound
        int lower = this.getSupportLowerBound();
        if (p == 0.0) {
            return lower;
        }
        if (lower == Integer.MIN_VALUE) {
            if (this.checkedCumulativeProbability(lower) >= p) {
                return lower;
            }
        } else {
            // this ensures cumulativeProbability(lower) < p, which is important for the solving step
            lower -= 1;
        }

        // Get upper bound
        int upper = this.getSupportUpperBound();
        if (p == 1.0) {
            return upper;
        }

        // use the one-sided Chebyshev inequality to narrow the bracket
        // cf. AbstractRealDistribution.inverseCumulativeProbability(double)
        final double mu = this.getNumericalMean();
        final double sigma = MathLib.sqrt(this.getNumericalVariance());
        final boolean isUndefined = Double.isInfinite(mu) || Double.isNaN(mu) ||
            Double.isInfinite(sigma) || Double.isNaN(sigma);
        final boolean chebyshevApplies = !(isUndefined || sigma == 0.0);
        if (chebyshevApplies) {
            double k = MathLib.sqrt((1.0 - p) / p);
            double tmp = mu - k * sigma;
            if (tmp > lower) {
                lower = ((int) Math.ceil(tmp)) - 1;
            }
            k = 1.0 / k;
            tmp = mu + k * sigma;
            if (tmp < upper) {
                upper = ((int) Math.ceil(tmp)) - 1;
            }
        }

        return this.solveInverseCumulativeProbability(p, lower, upper);
    }

    /**
     * This is a utility function used by {@link #inverseCumulativeProbability(double)}. It assumes {@code 0 < p < 1}
     * and
     * that the inverse cumulative probability lies in the bracket {@code (lower, upper]}. The implementation does
     * simple bisection to find the
     * smallest {@code p}-quantile <code>inf{x in Z | P(X<=x) >= p}</code>.
     * 
     * @param p
     *        the cumulative probability
     * @param lowerIn
     *        a value satisfying {@code cumulativeProbability(lower) < p}
     * @param upperIn
     *        a value satisfying {@code p <= cumulativeProbability(upper)}
     * @return the smallest {@code p}-quantile of this distribution
     */
    protected int solveInverseCumulativeProbability(final double p, final int lowerIn, final int upperIn) {
        int lower = lowerIn;
        int upper = upperIn;
        while (lower + 1 < upper) {
            int xm = (lower + upper) / 2;
            if (xm < lower || xm > upper) {
                /*
                 * Overflow.
                 * There will never be an overflow in both calculation methods
                 * for xm at the same time
                 */
                xm = lower + (upper - lower) / 2;
            }

            final double pm = this.checkedCumulativeProbability(xm);
            if (pm >= p) {
                upper = xm;
            } else {
                lower = xm;
            }
        }
        return upper;
    }

    /** {@inheritDoc} */
    @Override
    public void reseedRandomGenerator(final long seed) {
        this.random.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     * 
     * The default implementation uses the
     * <a href="http://en.wikipedia.org/wiki/Inverse_transform_sampling">
     * inversion method</a>.
     */
    @Override
    public int sample() {
        return this.inverseCumulativeProbability(this.random.nextDouble());
    }

    /**
     * {@inheritDoc}
     * 
     * The default implementation generates the sample by calling {@link #sample()} in a loop.
     */
    @Override
    public int[] sample(final int sampleSize) {
        if (sampleSize <= 0) {
            throw new NotStrictlyPositiveException(
                PatriusMessages.NUMBER_OF_SAMPLES, sampleSize);
        }
        final int[] out = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            out[i] = this.sample();
        }
        return out;
    }

    /**
     * Computes the cumulative probability function and checks for {@code NaN} values returned. Throws
     * {@code MathInternalError} if the value is {@code NaN}. Rethrows any exception encountered evaluating the
     * cumulative
     * probability function. Throws {@code MathInternalError} if the cumulative
     * probability function returns {@code NaN}.
     * 
     * @param argument
     *        input value
     * @return the cumulative probability
     * @throws MathInternalError
     *         if the cumulative probability is {@code NaN}
     */
    private double checkedCumulativeProbability(final int argument) {
        double result = Double.NaN;
        result = this.cumulativeProbability(argument);
        if (Double.isNaN(result)) {
            throw new MathInternalError(PatriusMessages
                .DISCRETE_CUMULATIVE_PROBABILITY_RETURNED_NAN, argument);
        }
        return result;
    }
}
