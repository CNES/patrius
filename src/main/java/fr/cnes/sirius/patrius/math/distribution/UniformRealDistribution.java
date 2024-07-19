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
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the uniform real distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Uniform_distribution_(continuous)" >Uniform distribution (continuous), at
 *      Wikipedia</a>
 * 
 * @version $Id: UniformRealDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class UniformRealDistribution extends AbstractRealDistribution {

    /** Default inverse cumulative probability accuracy. */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 12. */
    private static final double TWELVE = 12;

    /** Serializable UID. */
    private static final long serialVersionUID = 20120109L;
    /** Lower bound of this distribution (inclusive). */
    private final double lower;
    /** Upper bound of this distribution (exclusive). */
    private final double upper;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Create a standard uniform real distribution with lower bound (inclusive)
     * equal to zero and upper bound (exclusive) equal to one.
     */
    public UniformRealDistribution() {
        this(0, 1);
    }

    /**
     * Create a uniform real distribution using the given lower and upper
     * bounds.
     * 
     * @param lowerIn
     *        Lower bound of this distribution (inclusive).
     * @param upperIn
     *        Upper bound of this distribution (exclusive).
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}.
     */
    public UniformRealDistribution(final double lowerIn, final double upperIn) {
        this(lowerIn, upperIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a uniform distribution.
     * 
     * @param lowerIn
     *        Lower bound of this distribution (inclusive).
     * @param upperIn
     *        Upper bound of this distribution (exclusive).
     * @param inverseCumAccuracyIn
     *        Inverse cumulative probability accuracy.
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}.
     */
    public UniformRealDistribution(final double lowerIn, final double upperIn, final double inverseCumAccuracyIn) {
        this(new Well19937c(), lowerIn, upperIn, inverseCumAccuracyIn);
    }

    /**
     * Creates a uniform distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param lowerIn
     *        Lower bound of this distribution (inclusive).
     * @param upperIn
     *        Upper bound of this distribution (exclusive).
     * @param inverseCumAccuracy
     *        Inverse cumulative probability accuracy.
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}.
     * @since 3.1
     */
    public UniformRealDistribution(final RandomGenerator rng,
        final double lowerIn,
        final double upperIn,
        final double inverseCumAccuracy) {
        super(rng);
        if (lowerIn >= upperIn) {
            throw new NumberIsTooLargeException(
                PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lowerIn, upperIn, false);
        }

        this.lower = lowerIn;
        this.upper = upperIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        if (x < this.lower || x > this.upper) {
            return 0.0;
        }
        return 1 / (this.upper - this.lower);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final double x) {
        final double res;
        if (x <= this.lower) {
            res = 0;
        } else if (x >= this.upper) {
            res = 1;
        } else {
            res = (x - this.lower) / (this.upper - this.lower);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For lower bound {@code lower} and upper bound {@code upper}, the mean is {@code 0.5 * (lower + upper)}.
     */
    @Override
    public double getNumericalMean() {
        return HALF * (this.lower + this.upper);
    }

    /**
     * {@inheritDoc}
     * 
     * For lower bound {@code lower} and upper bound {@code upper}, the
     * variance is {@code (upper - lower)^2 / 12}.
     */
    @Override
    public double getNumericalVariance() {
        final double ul = this.upper - this.lower;
        return ul * ul / TWELVE;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is equal to the lower bound parameter
     * of the distribution.
     * 
     * @return lower bound of the support
     */
    @Override
    public double getSupportLowerBound() {
        return this.lower;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is equal to the upper bound parameter
     * of the distribution.
     * 
     * @return upper bound of the support
     */
    @Override
    public double getSupportUpperBound() {
        return this.upper;
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
     * Returns true if support contains upper bound.
     * 
     * @return true
     */
    public boolean isSupportUpperBoundInclusive() {
        return true;
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

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final double u = this.random.nextDouble();
        return u * this.upper + (1 - u) * this.lower;
    }
}
