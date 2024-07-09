/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;

/**
 * Implementation of the chi-squared distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Chi-squared_distribution">Chi-squared distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/Chi-SquaredDistribution.html">Chi-squared Distribution (MathWorld)</a>
 * @version $Id: ChiSquaredDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ChiSquaredDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    /** Serializable version identifier */
    private static final long serialVersionUID = -8352658048349159782L;
    /** Internal Gamma distribution. */
    private final GammaDistribution gamma;
    /** Inverse cumulative probability accuracy */
    private final double solverAbsoluteAccuracy;

    /**
     * Create a Chi-Squared distribution with the given degrees of freedom.
     * 
     * @param degreesOfFreedom
     *        Degrees of freedom.
     */
    public ChiSquaredDistribution(final double degreesOfFreedom) {
        this(degreesOfFreedom, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a Chi-Squared distribution with the given degrees of freedom and
     * inverse cumulative probability accuracy.
     * 
     * @param degreesOfFreedom
     *        Degrees of freedom.
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @since 2.1
     */
    public ChiSquaredDistribution(final double degreesOfFreedom,
        final double inverseCumAccuracy) {
        this(new Well19937c(), degreesOfFreedom, inverseCumAccuracy);
    }

    /**
     * Create a Chi-Squared distribution with the given degrees of freedom and
     * inverse cumulative probability accuracy.
     * 
     * @param rng
     *        Random number generator.
     * @param degreesOfFreedom
     *        Degrees of freedom.
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @since 3.1
     */
    public ChiSquaredDistribution(final RandomGenerator rng,
        final double degreesOfFreedom,
        final double inverseCumAccuracy) {
        super(rng);

        this.gamma = new GammaDistribution(degreesOfFreedom / 2, 2);
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Access the number of degrees of freedom.
     * 
     * @return the degrees of freedom.
     */
    public double getDegreesOfFreedom() {
        return this.gamma.getShape() * 2.0;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        return this.gamma.density(x);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final double x) {
        return this.gamma.cumulativeProbability(x);
    }

    /** {@inheritDoc} */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For {@code k} degrees of freedom, the mean is {@code k}.
     */
    @Override
    public double getNumericalMean() {
        return this.getDegreesOfFreedom();
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code 2 * k}, where {@code k} is the number of degrees of freedom.
     */
    @Override
    public double getNumericalVariance() {
        return 2 * this.getDegreesOfFreedom();
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 0 no matter the
     * degrees of freedom.
     * 
     * @return zero.
     */
    @Override
    public double getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is always positive infinity no matter the
     * degrees of freedom.
     * 
     * @return {@code Double.POSITIVE_INFINITY}.
     */
    @Override
    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns true if support contains lower bound.
     * 
     * @return false
     */
    public boolean isSupportLowerBoundInclusive() {
        return true;
    }

    /**
     * Returns true if support contains upper bound.
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
