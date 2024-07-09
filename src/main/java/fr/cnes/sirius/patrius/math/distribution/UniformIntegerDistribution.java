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

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the uniform integer distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Uniform_distribution_(discrete)" >Uniform distribution (discrete), at
 *      Wikipedia</a>
 * 
 * @version $Id: UniformIntegerDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class UniformIntegerDistribution extends AbstractIntegerDistribution {

    /** 12. */
    private static final double TWELVE = 12;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20120109L;
    /** Lower bound (inclusive) of this distribution. */
    private final int lower;
    /** Upper bound (inclusive) of this distribution. */
    private final int upper;

    /**
     * Creates a new uniform integer distribution using the given lower and
     * upper bounds (both inclusive).
     * 
     * @param lowerIn
     *        Lower bound (inclusive) of this distribution.
     * @param upperIn
     *        Upper bound (inclusive) of this distribution.
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}.
     */
    public UniformIntegerDistribution(final int lowerIn, final int upperIn) {
        this(new Well19937c(), lowerIn, upperIn);
    }

    /**
     * Creates a new uniform integer distribution using the given lower and
     * upper bounds (both inclusive).
     * 
     * @param rng
     *        Random number generator.
     * @param lowerIn
     *        Lower bound (inclusive) of this distribution.
     * @param upperIn
     *        Upper bound (inclusive) of this distribution.
     * @throws NumberIsTooLargeException
     *         if {@code lower >= upper}.
     * @since 3.1
     */
    public UniformIntegerDistribution(final RandomGenerator rng,
        final int lowerIn,
        final int upperIn) {
        super(rng);

        if (lowerIn >= upperIn) {
            throw new NumberIsTooLargeException(
                PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                lowerIn, upperIn, false);
        }
        this.lower = lowerIn;
        this.upper = upperIn;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final int x) {
        if (x < this.lower || x > this.upper) {
            return 0;
        }
        return 1.0 / (this.upper - this.lower + 1);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        final double res;
        if (x < this.lower) {
            res = 0;
        } else if (x > this.upper) {
            res = 1;
        } else {
            res = (x - this.lower + 1.0) / (this.upper - this.lower + 1.0);
        }
        return res;
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
     * For lower bound {@code lower} and upper bound {@code upper}, and {@code n = upper - lower + 1}, the variance is
     * {@code (n^2 - 1) / 12}.
     */
    @Override
    public double getNumericalVariance() {
        final double n = this.upper - this.lower + 1;
        return (n * n - 1) / TWELVE;
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
    public int getSupportLowerBound() {
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
    public int getSupportUpperBound() {
        return this.upper;
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
    public int sample() {
        final double r = this.random.nextDouble();
        final double scaled = r * this.upper + (1 - r) * this.lower + r;
        return (int) MathLib.floor(scaled);
    }
}
