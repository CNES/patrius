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
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the Weibull distribution. This implementation uses the
 * two parameter form of the distribution defined by
 * <a href="http://mathworld.wolfram.com/WeibullDistribution.html">
 * Weibull Distribution</a>, equations (1) and (2).
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Weibull_distribution">Weibull distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/WeibullDistribution.html">Weibull distribution (MathWorld)</a>
 * @since 1.1 (changed to concrete class in 3.0)
 * @version $Id: WeibullDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class WeibullDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    /** Serializable version identifier. */
    private static final long serialVersionUID = 8589540077390120676L;
    /** The shape parameter. */
    private final double shape;
    /** The scale parameter. */
    private final double scale;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;
    /** Cached numerical mean */
    private double numericalMean = Double.NaN;
    /** Whether or not the numerical mean has been calculated */
    private boolean numericalMeanIsCalculated = false;
    /** Cached numerical variance */
    private double numericalVariance = Double.NaN;
    /** Whether or not the numerical variance has been calculated */
    private boolean numericalVarianceIsCalculated = false;

    /**
     * Create a Weibull distribution with the given shape and scale and a
     * location equal to zero.
     * 
     * @param alpha
     *        Shape parameter.
     * @param beta
     *        Scale parameter.
     * @throws NotStrictlyPositiveException
     *         if {@code alpha <= 0} or {@code beta <= 0}.
     */
    public WeibullDistribution(final double alpha, final double beta) {
        this(alpha, beta, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a Weibull distribution with the given shape, scale and inverse
     * cumulative probability accuracy and a location equal to zero.
     * 
     * @param alpha
     *        Shape parameter.
     * @param beta
     *        Scale parameter.
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates
     *        (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code alpha <= 0} or {@code beta <= 0}.
     * @since 2.1
     */
    public WeibullDistribution(final double alpha, final double beta,
        final double inverseCumAccuracy) {
        this(new Well19937c(), alpha, beta, inverseCumAccuracy);
    }

    /**
     * Creates a Weibull distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param alpha
     *        Shape parameter.
     * @param beta
     *        Scale parameter.
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates
     *        (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code alpha <= 0} or {@code beta <= 0}.
     * @since 3.1
     */
    public WeibullDistribution(final RandomGenerator rng,
        final double alpha,
        final double beta,
        final double inverseCumAccuracy) {
        super(rng);

        if (alpha <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SHAPE,
                alpha);
        }
        if (beta <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SCALE,
                beta);
        }
        this.scale = beta;
        this.shape = alpha;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Access the shape parameter, {@code alpha}.
     * 
     * @return the shape parameter, {@code alpha}.
     */
    public double getShape() {
        return this.shape;
    }

    /**
     * Access the scale parameter, {@code beta}.
     * 
     * @return the scale parameter, {@code beta}.
     */
    public double getScale() {
        return this.scale;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        if (x < 0) {
            return 0;
        }

        final double xscale = x / this.scale;
        final double xscalepow = MathLib.pow(xscale, this.shape - 1);

        /*
         * FastMath.pow(x / scale, shape) =
         * FastMath.pow(xscale, shape) =
         * FastMath.pow(xscale, shape - 1) * xscale
         */
        final double xscalepowshape = xscalepow * xscale;

        return (this.shape / this.scale) * xscalepow * MathLib.exp(-xscalepowshape);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final double x) {
        final double ret;
        if (x <= 0.0) {
            ret = 0.0;
        } else {
            ret = 1.0 - MathLib.exp(-MathLib.pow(x / this.scale, this.shape));
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * Returns {@code 0} when {@code p == 0} and {@code Double.POSITIVE_INFINITY} when {@code p == 1}.
     */
    @Override
    public double inverseCumulativeProbability(final double p) {
        final double ret;
        if (p < 0.0 || p > 1.0) {
            throw new OutOfRangeException(p, 0.0, 1.0);
        } else if (p == 0) {
            ret = 0.0;
        } else if (p == 1) {
            ret = Double.POSITIVE_INFINITY;
        } else {
            ret = this.scale * MathLib.pow(-MathLib.log(1.0 - p), 1.0 / this.shape);
        }
        return ret;
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
     * The mean is {@code scale * Gamma(1 + (1 / shape))}, where {@code Gamma()} is the Gamma-function.
     */
    @Override
    public double getNumericalMean() {
        if (!this.numericalMeanIsCalculated) {
            this.numericalMean = this.calculateNumericalMean();
            this.numericalMeanIsCalculated = true;
        }
        return this.numericalMean;
    }

    /**
     * used by {@link #getNumericalMean()}
     * 
     * @return the mean of this distribution
     */
    protected double calculateNumericalMean() {
        final double sh = this.getShape();
        final double sc = this.getScale();

        return sc * MathLib.exp(Gamma.logGamma(1 + (1 / sh)));
    }

    /**
     * {@inheritDoc}
     * 
     * The variance is {@code scale^2 * Gamma(1 + (2 / shape)) - mean^2} where {@code Gamma()} is the Gamma-function.
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
     * used by {@link #getNumericalVariance()}
     * 
     * @return the variance of this distribution
     */
    protected double calculateNumericalVariance() {
        final double sh = this.getShape();
        final double sc = this.getScale();
        final double mn = this.getNumericalMean();

        return (sc * sc) * MathLib.exp(Gamma.logGamma(1 + (2 / sh))) -
            (mn * mn);
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
     * The upper bound of the support is always positive infinity
     * no matter the parameters.
     * 
     * @return upper bound of the support (always {@code Double.POSITIVE_INFINITY})
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
