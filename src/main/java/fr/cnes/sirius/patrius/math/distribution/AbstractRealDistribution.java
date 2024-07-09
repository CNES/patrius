/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
* VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour if/else "nominal" dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolverUtils;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Base class for probability distributions on the reals.
 * Default implementations are provided for some of the methods
 * that do not vary from distribution to distribution.
 * 
 * @version $Id: AbstractRealDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public abstract class AbstractRealDistribution
    implements RealDistribution, Serializable {
    /** Default accuracy. */
    public static final double SOLVER_DEFAULT_ABSOLUTE_ACCURACY = 1e-6;
    /** Serializable version identifier */
    private static final long serialVersionUID = -38038050983108802L;

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
    protected AbstractRealDistribution(final RandomGenerator rng) {
        this.random = rng;
    }

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(x0 < X <= x1)}.
     * 
     * @param x0
     *        Lower bound (excluded).
     * @param x1
     *        Upper bound (included).
     * @return the probability that a random variable with this distribution
     *         takes a value between {@code x0} and {@code x1}, excluding the lower
     *         and including the upper endpoint.
     * @throws NumberIsTooLargeException
     *         if {@code x0 > x1}.
     * 
     *         The default implementation uses the identity {@code P(x0 < X <= x1) = P(X <= x1) - P(X <= x0)}
     * 
     * @since 3.1
     */
    @Override
    public double probability(final double x0,
                              final double x1) {
        if (x0 > x1) {
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
     * <li>{@link #getSupportUpperBound()} for {@code p = 1}.</li>
     * </ul>
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Commons-Math code kept as such
    @Override
    public double inverseCumulativeProbability(final double p) {
        // CHECKSTYLE: resume CommentRatio check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        
        // IMPLEMENTATION NOTES
        // --------------------
        // Where applicable, use is made of the one-sided Chebyshev inequality
        // to bracket the root. This inequality states that
        // P(X - mu >= k * sig) <= 1 / (1 + k^2),
        // mu: mean, sig: standard deviation. Equivalently
        // 1 - P(X < mu + k * sig) <= 1 / (1 + k^2),
        // F(mu + k * sig) >= k^2 / (1 + k^2).
        // For k = sqrt(p / (1 - p)), we find
        // F(mu + k * sig) >= p,
        // and (mu + k * sig) is an upper-bound for the root.
        // Then, introducing Y = -X, mean(Y) = -mu, sd(Y) = sig, and
        // P(Y >= -mu + k * sig) <= 1 / (1 + k^2),
        // P(-X >= -mu + k * sig) <= 1 / (1 + k^2),
        // P(X <= mu - k * sig) <= 1 / (1 + k^2),
        // F(mu - k * sig) <= 1 / (1 + k^2).
        // For k = sqrt((1 - p) / p), we find
        // F(mu - k * sig) <= p,
        // and (mu - k * sig) is a lower-bound for the root.
        // In cases where the Chebyshev inequality does not apply, geometric
        // progressions 1, 2, 4, ... and -1, -2, -4, ... are used to bracket
        // the root.
        // --------------------
        if (p < 0.0 || p > 1.0) {
            // Out of range exception
            throw new OutOfRangeException(p, 0, 1);
        }

        // Get lower boud
        double lowerBound = this.getSupportLowerBound();
        if (p == 0.0) {
            return lowerBound;
        }

        // Get upper bound
        double upperBound = this.getSupportUpperBound();
        if (p == 1.0) {
            return upperBound;
        }

        // Initialization
        final double mu = this.getNumericalMean();
        final double sig;
        final double numVariance = this.getNumericalVariance();
        if (numVariance >= 0. && !Double.isNaN(numVariance)) {
            sig = MathLib.sqrt(numVariance);
        } else {
            sig = Double.NaN;
        }
        final boolean chebyshevApplies;
        chebyshevApplies = !(Double.isInfinite(mu) || Double.isNaN(mu) ||
            Double.isInfinite(sig) || Double.isNaN(sig));

        if (lowerBound == Double.NEGATIVE_INFINITY) {
            if (chebyshevApplies) {
                lowerBound = mu - sig * MathLib.sqrt((1. - p) / p);
            } else {
                lowerBound = -1.0;
                while (this.cumulativeProbability(lowerBound) >= p) {
                    lowerBound *= 2.0;
                }
            }
        }

        if (upperBound == Double.POSITIVE_INFINITY) {
            if (chebyshevApplies) {
                upperBound = mu + sig * MathLib.sqrt(p / (1. - p));
            } else {
                upperBound = 1.0;
                while (this.cumulativeProbability(upperBound) < p) {
                    upperBound *= 2.0;
                }
            }
        }

        final UnivariateFunction toSolve = new UnivariateFunction(){
            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return AbstractRealDistribution.this.cumulativeProbability(x) - p;
            }
        };

        final double x = UnivariateSolverUtils.solve(toSolve,
            lowerBound,
            upperBound,
            this.getSolverAbsoluteAccuracy());

        if (!this.isSupportConnected()) {
            /* Test for plateau. */
            final double dx = this.getSolverAbsoluteAccuracy();
            if (x - dx >= this.getSupportLowerBound()) {
                final double px = this.cumulativeProbability(x);
                if (this.cumulativeProbability(x - dx) == px) {
                    upperBound = x;
                    while (upperBound - lowerBound > dx) {
                        final double midPoint = 0.5 * (lowerBound + upperBound);
                        if (this.cumulativeProbability(midPoint) < px) {
                            lowerBound = midPoint;
                        } else {
                            upperBound = midPoint;
                        }
                    }
                    return upperBound;
                }
            }
        }
        return x;
    }

    /**
     * Returns the solver absolute accuracy for inverse cumulative computation.
     * You can override this method in order to use a Brent solver with an
     * absolute accuracy different from the default.
     * 
     * @return the maximum absolute error in inverse cumulative probability estimates
     */
    protected double getSolverAbsoluteAccuracy() {
        return SOLVER_DEFAULT_ABSOLUTE_ACCURACY;
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
     * inversion method.
     * </a>
     */
    @Override
    public double sample() {
        return this.inverseCumulativeProbability(this.random.nextDouble());
    }

    /**
     * {@inheritDoc}
     * 
     * The default implementation generates the sample by calling {@link #sample()} in a loop.
     */
    @Override
    public double[] sample(final int sampleSize) {
        if (sampleSize <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.NUMBER_OF_SAMPLES,
                sampleSize);
        }
        final double[] out = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            out[i] = this.sample();
        }
        return out;
    }

    /**
     * {@inheritDoc}
     * 
     * @return zero.
     * @since 3.1
     */
    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public double probability(final double x) {
        return 0d;
    }
}
