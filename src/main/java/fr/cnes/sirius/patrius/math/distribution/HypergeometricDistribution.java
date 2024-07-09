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
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the hypergeometric distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Hypergeometric_distribution">Hypergeometric distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/HypergeometricDistribution.html">Hypergeometric distribution
 *      (MathWorld)</a>
 * @version $Id: HypergeometricDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class HypergeometricDistribution extends AbstractIntegerDistribution {
    /** Serializable version identifier. */
    private static final long serialVersionUID = -436928820673516179L;
    /** The number of successes in the population. */
    private final int numberOfSuccesses;
    /** The population size. */
    private final int populationSize;
    /** The sample size. */
    private final int sampleSize;
    /** Cached numerical variance */
    private double numericalVariance = Double.NaN;
    /** Whether or not the numerical variance has been calculated */
    private boolean numericalVarianceIsCalculated = false;

    /**
     * Construct a new hypergeometric distribution with the specified population
     * size, number of successes in the population, and sample size.
     * 
     * @param populationSizeIn
     *        Population size.
     * @param numberOfSuccessesIn
     *        Number of successes in the population.
     * @param sampleSizeIn
     *        Sample size.
     * @throws NotPositiveException
     *         if {@code numberOfSuccesses < 0}.
     * @throws NotStrictlyPositiveException
     *         if {@code populationSize <= 0}.
     * @throws NumberIsTooLargeException
     *         if {@code numberOfSuccesses > populationSize},
     *         or {@code sampleSize > populationSize}.
     */
    public HypergeometricDistribution(final int populationSizeIn,
        final int numberOfSuccessesIn, final int sampleSizeIn) {
        this(new Well19937c(), populationSizeIn, numberOfSuccessesIn, sampleSizeIn);
    }

    /**
     * Creates a new hypergeometric distribution.
     * 
     * @param rngIn
     *        Random number generator.
     * @param populationSizeIn
     *        Population size.
     * @param numberOfSuccessesIn
     *        Number of successes in the population.
     * @param sampleSizeIn
     *        Sample size.
     * @throws NotPositiveException
     *         if {@code numberOfSuccesses < 0}.
     * @throws NotStrictlyPositiveException
     *         if {@code populationSize <= 0}.
     * @throws NumberIsTooLargeException
     *         if {@code numberOfSuccesses > populationSize},
     *         or {@code sampleSize > populationSize}.
     * @since 3.1
     */
    public HypergeometricDistribution(final RandomGenerator rngIn,
        final int populationSizeIn,
        final int numberOfSuccessesIn,
        final int sampleSizeIn) {
        super(rngIn);

        if (populationSizeIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.POPULATION_SIZE,
                populationSizeIn);
        }
        if (numberOfSuccessesIn < 0) {
            throw new NotPositiveException(PatriusMessages.NUMBER_OF_SUCCESSES,
                numberOfSuccessesIn);
        }
        if (sampleSizeIn < 0) {
            throw new NotPositiveException(PatriusMessages.NUMBER_OF_SAMPLES,
                sampleSizeIn);
        }

        if (numberOfSuccessesIn > populationSizeIn) {
            throw new NumberIsTooLargeException(PatriusMessages.NUMBER_OF_SUCCESS_LARGER_THAN_POPULATION_SIZE,
                numberOfSuccessesIn, populationSizeIn, true);
        }
        if (sampleSizeIn > populationSizeIn) {
            throw new NumberIsTooLargeException(PatriusMessages.SAMPLE_SIZE_LARGER_THAN_POPULATION_SIZE,
                sampleSizeIn, populationSizeIn, true);
        }

        this.numberOfSuccesses = numberOfSuccessesIn;
        this.populationSize = populationSizeIn;
        this.sampleSize = sampleSizeIn;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        final double ret;

        final int[] domain = this.getDomain(this.populationSize, this.numberOfSuccesses, this.sampleSize);
        if (x < domain[0]) {
            ret = 0.0;
        } else if (x >= domain[1]) {
            ret = 1.0;
        } else {
            ret = this.innerCumulativeProbability(domain[0], x, 1);
        }

        return ret;
    }

    /**
     * Return the domain for the given hypergeometric distribution parameters.
     * 
     * @param n
     *        Population size.
     * @param m
     *        Number of successes in the population.
     * @param k
     *        Sample size.
     * @return a two element array containing the lower and upper bounds of the
     *         hypergeometric distribution.
     */
    private int[] getDomain(final int n, final int m, final int k) {
        return new int[] { this.getLowerDomain(n, m, k), this.getUpperDomain(m, k) };
    }

    /**
     * Return the lowest domain value for the given hypergeometric distribution
     * parameters.
     * 
     * @param n
     *        Population size.
     * @param m
     *        Number of successes in the population.
     * @param k
     *        Sample size.
     * @return the lowest domain value of the hypergeometric distribution.
     */
    private int getLowerDomain(final int n, final int m, final int k) {
        return MathLib.max(0, m - (n - k));
    }

    /**
     * Access the number of successes.
     * 
     * @return the number of successes.
     */
    public int getNumberOfSuccesses() {
        return this.numberOfSuccesses;
    }

    /**
     * Access the population size.
     * 
     * @return the population size.
     */
    public int getPopulationSize() {
        return this.populationSize;
    }

    /**
     * Access the sample size.
     * 
     * @return the sample size.
     */
    public int getSampleSize() {
        return this.sampleSize;
    }

    /**
     * Return the highest domain value for the given hypergeometric distribution
     * parameters.
     * 
     * @param m
     *        Number of successes in the population.
     * @param k
     *        Sample size.
     * @return the highest domain value of the hypergeometric distribution.
     */
    private int getUpperDomain(final int m, final int k) {
        return MathLib.min(k, m);
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final int x) {
        final double ret;

        // Domain
        final int[] domain = this.getDomain(this.populationSize, this.numberOfSuccesses, this.sampleSize);
        if (x < domain[0] || x > domain[1]) {
            // Null probability
            ret = 0.0;
        } else {
            // Compute probability
            final double p = (double) this.sampleSize / (double) this.populationSize;
            final double q = (double) (this.populationSize - this.sampleSize) / (double) this.populationSize;
            final double p1 = SaddlePointExpansion.logBinomialProbability(x,
                this.numberOfSuccesses, p, q);
            final double p2 =
                SaddlePointExpansion.logBinomialProbability(this.sampleSize - x,
                    this.populationSize - this.numberOfSuccesses, p, q);
            final double p3 =
                SaddlePointExpansion.logBinomialProbability(this.sampleSize, this.populationSize, p, q);
            ret = MathLib.exp(p1 + p2 - p3);
        }

        return ret;
    }

    /**
     * For this distribution, {@code X}, this method returns {@code P(X >= x)}.
     * 
     * @param x
     *        Value at which the CDF is evaluated.
     * @return the upper tail CDF for this distribution.
     * @since 1.1
     */
    public double upperCumulativeProbability(final int x) {
        final double ret;

        final int[] domain = this.getDomain(this.populationSize, this.numberOfSuccesses, this.sampleSize);
        if (x <= domain[0]) {
            ret = 1.0;
        } else if (x > domain[1]) {
            ret = 0.0;
        } else {
            ret = this.innerCumulativeProbability(domain[1], x, -1);
        }

        return ret;
    }

    /**
     * For this distribution, {@code X}, this method returns {@code P(x0 <= X <= x1)}.
     * This probability is computed by summing the point probabilities for the
     * values {@code x0, x0 + 1, x0 + 2, ..., x1}, in the order directed by {@code dx}.
     * 
     * @param x0In
     *        Inclusive lower bound.
     * @param x1
     *        Inclusive upper bound.
     * @param dx
     *        Direction of summation (1 indicates summing from x0 to x1, and
     *        0 indicates summing from x1 to x0).
     * @return {@code P(x0 <= X <= x1)}.
     */
    private double innerCumulativeProbability(final int x0In, final int x1, final int dx) {
        int x0 = x0In;
        double ret = this.probability(x0);
        while (x0 != x1) {
            x0 += dx;
            ret += this.probability(x0);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the mean is {@code n * m / N}.
     */
    @Override
    public double getNumericalMean() {
        return (double) (this.getSampleSize() * this.getNumberOfSuccesses()) / (double) this.getPopulationSize();
    }

    /**
     * {@inheritDoc}
     * 
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the variance is {@code [n * m * (N - n) * (N - m)] / [N^2 * (N - 1)]}.
     */
    @Override
    public double getNumericalVariance() {
        if (!this.numericalVarianceIsCalculated) {
            this.numericalVariance = this.calculateNumericalVariance();
            this.numericalVarianceIsCalculated = true;
        }
        return this.numericalVariance;
    }

    /**
     * Used by {@link #getNumericalVariance()}.
     * 
     * @return the variance of this distribution
     */
    protected double calculateNumericalVariance() {
        final double s = this.getPopulationSize();
        final double m = this.getNumberOfSuccesses();
        final double n = this.getSampleSize();
        return (n * m * (s - n) * (s - m)) / (s * s * (s - 1));
    }

    /**
     * {@inheritDoc}
     * 
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the lower bound of the support is {@code max(0, n + m - N)}.
     * 
     * @return lower bound of the support
     */
    @Override
    public int getSupportLowerBound() {
        return MathLib.max(0,
            this.getSampleSize() + this.getNumberOfSuccesses() - this.getPopulationSize());
    }

    /**
     * {@inheritDoc}
     * 
     * For number of successes {@code m} and sample size {@code n}, the upper
     * bound of the support is {@code min(m, n)}.
     * 
     * @return upper bound of the support
     */
    @Override
    public int getSupportUpperBound() {
        return MathLib.min(this.getNumberOfSuccesses(), this.getSampleSize());
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
