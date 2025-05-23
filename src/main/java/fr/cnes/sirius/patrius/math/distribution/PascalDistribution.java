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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Beta;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Implementation of the Pascal distribution. The Pascal distribution is a special case of the Negative Binomial
 * distribution where the number of successes parameter is an integer.
 * </p>
 * <p>
 * There are various ways to express the probability mass and distribution functions for the Pascal distribution. The
 * present implementation represents the distribution of the number of failures before {@code r} successes occur. This
 * is the convention adopted in e.g. <a
 * href="http://mathworld.wolfram.com/NegativeBinomialDistribution.html">MathWorld</a>, but <em>not</em> in <a
 * href="http://en.wikipedia.org/wiki/Negative_binomial_distribution">Wikipedia</a>.
 * </p>
 * <p>
 * For a random variable {@code X} whose values are distributed according to this distribution, the probability mass
 * function is given by<br/>
 * {@code P(X = k) = C(k + r - 1, r - 1) * p^r * (1 - p)^k,}<br/>
 * where {@code r} is the number of successes, {@code p} is the probability of success, and {@code X} is the total
 * number of failures. {@code C(n, k)} is the binomial coefficient ({@code n} choose {@code k}). The mean and variance
 * of {@code X} are<br/>
 * {@code E(X) = (1 - p) * r / p, var(X) = (1 - p) * r / p^2.}<br/>
 * Finally, the cumulative distribution function is given by<br/>
 * {@code P(X <= k) = I(p, r, k + 1)}, where I is the regularized incomplete Beta function.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Negative_binomial_distribution"> Negative binomial distribution
 *      (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/NegativeBinomialDistribution.html"> Negative binomial distribution
 *      (MathWorld)</a>
 * @version $Id: PascalDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2 (changed to concrete class in 3.0)
 */
public class PascalDistribution extends AbstractIntegerDistribution {
    /** Serializable UID. */
    private static final long serialVersionUID = 6751309484392813623L;
    /** The number of successes. */
    private final int numberOfSuccesses;
    /** The probability of success. */
    private final double probabilityOfSuccess;

    /**
     * Create a Pascal distribution with the given number of successes and
     * probability of success.
     * 
     * @param r
     *        Number of successes.
     * @param p
     *        Probability of success.
     * @throws NotStrictlyPositiveException
     *         if the number of successes is not positive
     * @throws OutOfRangeException
     *         if the probability of success is not in the
     *         range {@code [0, 1]}.
     */
    public PascalDistribution(final int r, final double p) {
        this(new Well19937c(), r, p);
    }

    /**
     * Create a Pascal distribution with the given number of successes and
     * probability of success.
     * 
     * @param rng
     *        Random number generator.
     * @param r
     *        Number of successes.
     * @param p
     *        Probability of success.
     * @throws NotStrictlyPositiveException
     *         if the number of successes is not positive
     * @throws OutOfRangeException
     *         if the probability of success is not in the
     *         range {@code [0, 1]}.
     * @since 3.1
     */
    public PascalDistribution(final RandomGenerator rng,
        final int r,
        final double p) {
        super(rng);

        if (r <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.NUMBER_OF_SUCCESSES,
                r);
        }
        if (p < 0 || p > 1) {
            throw new OutOfRangeException(p, 0, 1);
        }

        this.numberOfSuccesses = r;
        this.probabilityOfSuccess = p;
    }

    /**
     * Access the number of successes for this distribution.
     * 
     * @return the number of successes.
     */
    public int getNumberOfSuccesses() {
        return this.numberOfSuccesses;
    }

    /**
     * Access the probability of success for this distribution.
     * 
     * @return the probability of success.
     */
    public double getProbabilityOfSuccess() {
        return this.probabilityOfSuccess;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final int x) {
        final double ret;
        if (x < 0) {
            ret = 0.0;
        } else {
            ret = ArithmeticUtils.binomialCoefficientDouble(x +
                this.numberOfSuccesses - 1, this.numberOfSuccesses - 1) *
                MathLib.pow(this.probabilityOfSuccess, this.numberOfSuccesses) *
                MathLib.pow(1.0 - this.probabilityOfSuccess, x);
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        final double ret;
        if (x < 0) {
            ret = 0.0;
        } else {
            ret = Beta.regularizedBeta(this.probabilityOfSuccess,
                this.numberOfSuccesses, x + 1.0);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * For number of successes {@code r} and probability of success {@code p},
     * the mean is {@code r * (1 - p) / p}.
     */
    @Override
    public double getNumericalMean() {
        final double p = this.getProbabilityOfSuccess();
        final double r = this.getNumberOfSuccesses();
        return (r * (1 - p)) / p;
    }

    /**
     * {@inheritDoc}
     * 
     * For number of successes {@code r} and probability of success {@code p},
     * the variance is {@code r * (1 - p) / p^2}.
     */
    @Override
    public double getNumericalVariance() {
        final double p = this.getProbabilityOfSuccess();
        final double r = this.getNumberOfSuccesses();
        return r * (1 - p) / (p * p);
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 0 no matter the parameters.
     * 
     * @return lower bound of the support (always 0)
     */
    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is always positive infinity no matter the
     * parameters. Positive infinity is symbolized by {@code Integer.MAX_VALUE}.
     * 
     * @return upper bound of the support (always {@code Integer.MAX_VALUE} for positive infinity)
     */
    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
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
