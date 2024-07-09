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

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Beta;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the binomial distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Binomial_distribution">Binomial distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/BinomialDistribution.html">Binomial Distribution (MathWorld)</a>
 * @version $Id: BinomialDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BinomialDistribution extends AbstractIntegerDistribution {
    /** Serializable version identifier. */
    private static final long serialVersionUID = 6751309484392813623L;
    /** The number of trials. */
    private final int numberOfTrials;
    /** The probability of success. */
    private final double probabilityOfSuccess;

    /**
     * Create a binomial distribution with the given number of trials and
     * probability of success.
     * 
     * @param trials
     *        Number of trials.
     * @param p
     *        Probability of success.
     * @throws NotPositiveException
     *         if {@code trials < 0}.
     * @throws OutOfRangeException
     *         if {@code p < 0} or {@code p > 1}.
     */
    public BinomialDistribution(final int trials, final double p) {
        this(new Well19937c(), trials, p);
    }

    /**
     * Creates a binomial distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param trials
     *        Number of trials.
     * @param p
     *        Probability of success.
     * @throws NotPositiveException
     *         if {@code trials < 0}.
     * @throws OutOfRangeException
     *         if {@code p < 0} or {@code p > 1}.
     * @since 3.1
     */
    public BinomialDistribution(final RandomGenerator rng,
        final int trials,
        final double p) {
        super(rng);

        if (trials < 0) {
            throw new NotPositiveException(PatriusMessages.NUMBER_OF_TRIALS,
                trials);
        }
        if (p < 0 || p > 1) {
            throw new OutOfRangeException(p, 0, 1);
        }

        this.probabilityOfSuccess = p;
        this.numberOfTrials = trials;
    }

    /**
     * Access the number of trials for this distribution.
     * 
     * @return the number of trials.
     */
    public int getNumberOfTrials() {
        return this.numberOfTrials;
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
        if (x < 0 || x > this.numberOfTrials) {
            ret = 0.0;
        } else {
            ret = MathLib.exp(SaddlePointExpansion.logBinomialProbability(x,
                this.numberOfTrials, this.probabilityOfSuccess,
                1.0 - this.probabilityOfSuccess));
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        final double ret;
        if (x < 0) {
            ret = 0.0;
        } else if (x >= this.numberOfTrials) {
            ret = 1.0;
        } else {
            ret = 1.0 - Beta.regularizedBeta(this.probabilityOfSuccess,
                x + 1.0, this.numberOfTrials - x);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * For {@code n} trials and probability parameter {@code p}, the mean is {@code n * p}.
     */
    @Override
    public double getNumericalMean() {
        return this.numberOfTrials * this.probabilityOfSuccess;
    }

    /**
     * {@inheritDoc}
     * 
     * For {@code n} trials and probability parameter {@code p}, the variance is {@code n * p * (1 - p)}.
     */
    @Override
    public double getNumericalVariance() {
        final double p = this.probabilityOfSuccess;
        return this.numberOfTrials * p * (1 - p);
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 0 except for the probability
     * parameter {@code p = 1}.
     * 
     * @return lower bound of the support (0 or the number of trials)
     */
    @Override
    public int getSupportLowerBound() {
        return this.probabilityOfSuccess < 1.0 ? 0 : this.numberOfTrials;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is the number of trials except for the
     * probability parameter {@code p = 0}.
     * 
     * @return upper bound of the support (number of trials or 0)
     */
    @Override
    public int getSupportUpperBound() {
        return this.probabilityOfSuccess > 0.0 ? this.numberOfTrials : 0;
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
