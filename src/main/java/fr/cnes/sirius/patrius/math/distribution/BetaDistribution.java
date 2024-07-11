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

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Beta;
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the Beta distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Beta_distribution">Beta distribution</a>
 * @version $Id: BetaDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0 (changed to concrete class in 3.0)
 */
public class BetaDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    /** Serializable version identifier. */
    private static final long serialVersionUID = -1221965979403477668L;
    /** First shape parameter. */
    private final double alpha;
    /** Second shape parameter. */
    private final double beta;
    /**
     * Normalizing factor used in density computations.
     * updated whenever alpha or beta are changed.
     */
    private double z;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Build a new instance.
     * 
     * @param alphaIn
     *        First shape parameter (must be positive).
     * @param betaIn
     *        Second shape parameter (must be positive).
     */
    public BetaDistribution(final double alphaIn, final double betaIn) {
        this(alphaIn, betaIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Build a new instance.
     * 
     * @param alphaIn
     *        First shape parameter (must be positive).
     * @param betaIn
     *        Second shape parameter (must be positive).
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @since 2.1
     */
    public BetaDistribution(final double alphaIn, final double betaIn, final double inverseCumAccuracy) {
        this(new Well19937c(), alphaIn, betaIn, inverseCumAccuracy);
    }

    /**
     * Creates a &beta; distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param alphaIn
     *        First shape parameter (must be positive).
     * @param betaIn
     *        Second shape parameter (must be positive).
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @since 3.1
     */
    public BetaDistribution(final RandomGenerator rng,
        final double alphaIn,
        final double betaIn,
        final double inverseCumAccuracy) {
        super(rng);

        this.alpha = alphaIn;
        this.beta = betaIn;
        this.z = Double.NaN;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Access the first shape parameter, {@code alpha}.
     * 
     * @return the first shape parameter.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * Access the second shape parameter, {@code beta}.
     * 
     * @return the second shape parameter.
     */
    public double getBeta() {
        return this.beta;
    }

    /** Recompute the normalization factor. */
    private void recomputeZ() {
        if (Double.isNaN(this.z)) {
            this.z = Gamma.logGamma(this.alpha) + Gamma.logGamma(this.beta) - Gamma.logGamma(this.alpha + this.beta);
        }
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double density(final double x) {
        // CHECKSTYLE: resume ReturnCount check
        this.recomputeZ();
        if (x < 0 || x > 1) {
            return 0;
        } else if (x == 0) {
            if (this.alpha < 1) {
                // Exception
                throw new NumberIsTooSmallException(PatriusMessages.CANNOT_COMPUTE_BETA_DENSITY_AT_0_FOR_SOME_ALPHA,
                    this.alpha, 1, false);
            }
            return 0;
        } else if (x == 1) {
            if (this.beta < 1) {
                // Exception
                throw new NumberIsTooSmallException(PatriusMessages.CANNOT_COMPUTE_BETA_DENSITY_AT_1_FOR_SOME_BETA,
                    this.beta, 1, false);
            }
            return 0;
        } else {
            // Compute density
            final double logX = MathLib.log(x);
            final double log1mX = MathLib.log1p(-x);
            return MathLib.exp((this.alpha - 1) * logX + (this.beta - 1) * log1mX - this.z);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final double x) {
        final double res;
        if (x <= 0) {
            res = 0;
        } else if (x >= 1) {
            res = 1;
        } else {
            res = Beta.regularizedBeta(x, this.alpha, this.beta);
        }
        return res;
    }

    /**
     * Return the absolute accuracy setting of the solver used to estimate
     * inverse cumulative probabilities.
     * 
     * @return the solver absolute accuracy.
     * @since 2.1
     */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For first shape parameter {@code alpha} and second shape parameter {@code beta}, the mean is
     * {@code alpha / (alpha + beta)}.
     */
    @Override
    public double getNumericalMean() {
        final double a = this.getAlpha();
        return a / (a + this.getBeta());
    }

    /**
     * {@inheritDoc}
     * 
     * For first shape parameter {@code alpha} and second shape parameter {@code beta}, the variance is
     * {@code (alpha * beta) / [(alpha + beta)^2 * (alpha + beta + 1)]}.
     */
    @Override
    public double getNumericalVariance() {
        final double a = this.getAlpha();
        final double b = this.getBeta();
        final double alphabetasum = a + b;
        return (a * b) / ((alphabetasum * alphabetasum) * (alphabetasum + 1));
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 0 no matter the parameters.
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
     * The upper bound of the support is always 1 no matter the parameters.
     * 
     * @return upper bound of the support (always 1)
     */
    @Override
    public double getSupportUpperBound() {
        return 1;
    }

    /**
     * Returns true if support contains lower bound.
     * 
     * @return false
     */
    public boolean isSupportLowerBoundInclusive() {
        return false;
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
