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
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the Cauchy distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Cauchy_distribution">Cauchy distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/CauchyDistribution.html">Cauchy Distribution (MathWorld)</a>
 * @since 1.1 (changed to concrete class in 3.0)
 * @version $Id: CauchyDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class CauchyDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    /** Serializable version identifier */
    private static final long serialVersionUID = 8589540077390120676L;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** The median of this distribution. */
    private final double median;
    /** The scale of this distribution. */
    private final double scale;
    /** Inverse cumulative probability accuracy */
    private final double solverAbsoluteAccuracy;

    /**
     * Creates a Cauchy distribution with the median equal to zero and scale
     * equal to one.
     */
    public CauchyDistribution() {
        this(0, 1);
    }

    /**
     * Creates a Cauchy distribution using the given median and scale.
     * 
     * @param medianIn
     *        Median for this distribution.
     * @param scaleIn
     *        Scale parameter for this distribution.
     */
    public CauchyDistribution(final double medianIn, final double scaleIn) {
        this(medianIn, scaleIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Creates a Cauchy distribution using the given median and scale.
     * 
     * @param medianIn
     *        Median for this distribution.
     * @param scaleIn
     *        Scale parameter for this distribution.
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates
     *        (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code scale <= 0}.
     * @since 2.1
     */
    public CauchyDistribution(final double medianIn, final double scaleIn,
        final double inverseCumAccuracy) {
        this(new Well19937c(), medianIn, scaleIn, inverseCumAccuracy);
    }

    /**
     * Creates a Cauchy distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param medianIn
     *        Median for this distribution.
     * @param scaleIn
     *        Scale parameter for this distribution.
     * @param inverseCumAccuracy
     *        Maximum absolute error in inverse
     *        cumulative probability estimates
     *        (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code scale <= 0}.
     * @since 3.1
     */
    public CauchyDistribution(final RandomGenerator rng,
        final double medianIn,
        final double scaleIn,
        final double inverseCumAccuracy) {
        super(rng);
        if (scaleIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SCALE, scaleIn);
        }
        this.scale = scaleIn;
        this.median = medianIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final double x) {
        return HALF + (MathLib.atan((x - this.median) / this.scale) / FastMath.PI);
    }

    /**
     * Access the median.
     * 
     * @return the median for this distribution.
     */
    public double getMedian() {
        return this.median;
    }

    /**
     * Access the scale parameter.
     * 
     * @return the scale parameter for this distribution.
     */
    public double getScale() {
        return this.scale;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        final double dev = x - this.median;
        return (1 / FastMath.PI) * (this.scale / (dev * dev + this.scale * this.scale));
    }

    /**
     * {@inheritDoc}
     * 
     * Returns {@code Double.NEGATIVE_INFINITY} when {@code p == 0} and {@code Double.POSITIVE_INFINITY} when
     * {@code p == 1}.
     */
    @Override
    public double inverseCumulativeProbability(final double p) {
        final double ret;
        if (p < 0 || p > 1) {
            throw new OutOfRangeException(p, 0, 1);
        } else if (p == 0) {
            ret = Double.NEGATIVE_INFINITY;
        } else if (p == 1) {
            ret = Double.POSITIVE_INFINITY;
        } else {
            ret = this.median + this.scale * MathLib.tan(FastMath.PI * (p - HALF));
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * The mean is always undefined no matter the parameters.
     * 
     * @return mean (always Double.NaN)
     */
    @Override
    public double getNumericalMean() {
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     * 
     * The variance is always undefined no matter the parameters.
     * 
     * @return variance (always Double.NaN)
     */
    @Override
    public double getNumericalVariance() {
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always negative infinity no matter
     * the parameters.
     * 
     * @return lower bound of the support (always Double.NEGATIVE_INFINITY)
     */
    @Override
    public double getSupportLowerBound() {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is always positive infinity no matter
     * the parameters.
     * 
     * @return upper bound of the support (always Double.POSITIVE_INFINITY)
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
