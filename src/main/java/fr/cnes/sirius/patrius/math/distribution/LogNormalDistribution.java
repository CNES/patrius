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

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Erf;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the log-normal (gaussian) distribution.
 * 
 * <p>
 * <strong>Parameters:</strong> {@code X} is log-normally distributed if its natural logarithm {@code log(X)} is
 * normally distributed. The probability distribution function of {@code X} is given by (for {@code x > 0})
 * </p>
 * <p>
 * {@code exp(-0.5 * ((ln(x) - m) / s)^2) / (s * sqrt(2 * pi) * x)}
 * </p>
 * <ul>
 * <li>{@code m} is the <em>scale</em> parameter: this is the mean of the normally distributed natural logarithm of this
 * distribution,</li>
 * <li>{@code s} is the <em>shape</em> parameter: this is the standard deviation of the normally distributed natural
 * logarithm of this distribution.
 * </ul>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Log-normal_distribution"> Log-normal distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/LogNormalDistribution.html"> Log Normal distribution (MathWorld)</a>
 * 
 * @version $Id: LogNormalDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class LogNormalDistribution extends AbstractRealDistribution {
    /** Default inverse cumulative probability accuracy. */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 40. */
    private static final double FOURTY = 40;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20120112;

    /** &radic;(2 &pi;) */
    private static final double SQRT2PI = MathLib.sqrt(2 * FastMath.PI);

    /** &radic;(2) */
    private static final double SQRT2 = MathLib.sqrt(2.0);

    /** The scale parameter of this distribution. */
    private final double scale;

    /** The shape parameter of this distribution. */
    private final double shape;

    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Create a log-normal distribution, where the mean and standard deviation
     * of the {@link NormalDistribution normally distributed} natural
     * logarithm of the log-normal distribution are equal to zero and one
     * respectively. In other words, the scale of the returned distribution is {@code 0}, while its shape is {@code 1}.
     */
    public LogNormalDistribution() {
        this(0, 1);
    }

    /**
     * Create a log-normal distribution using the specified scale and shape.
     * 
     * @param scaleIn
     *        the scale parameter of this distribution
     * @param shapeIn
     *        the shape parameter of this distribution
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0}.
     */
    public LogNormalDistribution(final double scaleIn, final double shapeIn) {
        this(scaleIn, shapeIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a log-normal distribution using the specified scale, shape and
     * inverse cumulative distribution accuracy.
     * 
     * @param scaleIn
     *        the scale parameter of this distribution
     * @param shapeIn
     *        the shape parameter of this distribution
     * @param inverseCumAccuracy
     *        Inverse cumulative probability accuracy.
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0}.
     */
    public LogNormalDistribution(final double scaleIn, final double shapeIn, final double inverseCumAccuracy) {
        this(new Well19937c(), scaleIn, shapeIn, inverseCumAccuracy);
    }

    /**
     * Creates a log-normal distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param scaleIn
     *        Scale parameter of this distribution.
     * @param shapeIn
     *        Shape parameter of this distribution.
     * @param inverseCumAccuracy
     *        Inverse cumulative probability accuracy.
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0}.
     * @since 3.1
     */
    public LogNormalDistribution(final RandomGenerator rng,
        final double scaleIn,
        final double shapeIn,
        final double inverseCumAccuracy) {
        super(rng);

        if (shapeIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SHAPE, shapeIn);
        }

        this.scale = scaleIn;
        this.shape = shapeIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Returns the scale parameter of this distribution.
     * 
     * @return the scale parameter
     */
    public double getScale() {
        return this.scale;
    }

    /**
     * Returns the shape parameter of this distribution.
     * 
     * @return the shape parameter
     */
    public double getShape() {
        return this.shape;
    }

    /**
     * {@inheritDoc}
     * 
     * For scale {@code m}, and shape {@code s} of this distribution, the PDF
     * is given by
     * <ul>
     * <li>{@code 0} if {@code x <= 0},</li>
     * <li>{@code exp(-0.5 * ((ln(x) - m) / s)^2) / (s * sqrt(2 * pi) * x)} otherwise.</li>
     * </ul>
     */
    @Override
    public double density(final double x) {
        if (x <= 0) {
            return 0;
        }
        final double x0 = MathLib.log(x) - this.scale;
        final double x1 = x0 / this.shape;
        return MathLib.exp(-HALF * x1 * x1) / (this.shape * SQRT2PI * x);
    }

    /**
     * {@inheritDoc}
     * 
     * For scale {@code m}, and shape {@code s} of this distribution, the CDF
     * is given by
     * <ul>
     * <li>{@code 0} if {@code x <= 0},</li>
     * <li>{@code 0} if {@code ln(x) - m < 0} and {@code m - ln(x) > 40 * s}, as in these cases the actual value is
     * within {@code Double.MIN_VALUE} of 0,
     * <li>{@code 1} if {@code ln(x) - m >= 0} and {@code ln(x) - m > 40 * s}, as in these cases the actual value is
     * within {@code Double.MIN_VALUE} of 1,</li>
     * <li>{@code 0.5 + 0.5 * erf((ln(x) - m) / (s * sqrt(2))} otherwise.</li>
     * </ul>
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double cumulativeProbability(final double x) {
        // CHECKSTYLE: resume ReturnCount check
        if (x <= 0) {
            return 0;
        }
        final double dev = MathLib.log(x) - this.scale;
        if (MathLib.abs(dev) > FOURTY * this.shape) {
            return dev < 0 ? 0.0d : 1.0d;
        }
        return HALF + HALF * Erf.erf(dev / (this.shape * SQRT2));
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final double x0,
                              final double x1) {
        if (x0 > x1) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_ENDPOINT_ABOVE_UPPER_ENDPOINT,
                x0, x1, true);
        }
        if (x0 <= 0 || x1 <= 0) {
            return super.probability(x0, x1);
        }
        final double denom = this.shape * SQRT2;
        final double v0 = (MathLib.log(x0) - this.scale) / denom;
        final double v1 = (MathLib.log(x1) - this.scale) / denom;
        return HALF * Erf.erf(v0, v1);
    }

    /** {@inheritDoc} */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For scale {@code m} and shape {@code s}, the mean is {@code exp(m + s^2 / 2)}.
     */
    @Override
    public double getNumericalMean() {
        final double s = this.shape;
        return MathLib.exp(this.scale + (s * s / 2));
    }

    /**
     * {@inheritDoc}
     * 
     * For scale {@code m} and shape {@code s}, the variance is {@code (exp(s^2) - 1) * exp(2 * m + s^2)}.
     */
    @Override
    public double getNumericalVariance() {
        final double s = this.shape;
        final double ss = s * s;
        return (MathLib.exp(ss) - 1) * MathLib.exp(2 * this.scale + ss);
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

    /** {@inheritDoc} */
    @Override
    public double sample() {
        final double n = this.random.nextGaussian();
        return MathLib.exp(this.scale + this.shape * n);
    }
}
