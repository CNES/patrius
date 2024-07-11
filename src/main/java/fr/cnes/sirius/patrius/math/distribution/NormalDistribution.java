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
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Erf;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the normal (gaussian) distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Normal_distribution">Normal distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/NormalDistribution.html">Normal distribution (MathWorld)</a>
 * @version $Id: NormalDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NormalDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 40. */
    private static final double FOURTY = 40;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8589540077390120676L;
    /** &radic;(2 &pi;) */
    private static final double SQRT2PI = MathLib.sqrt(2 * FastMath.PI);
    /** &radic;(2) */
    private static final double SQRT2 = MathLib.sqrt(2.0);
    /** Mean of this distribution. */
    private final double mean;
    /** Standard deviation of this distribution. */
    private final double standardDeviation;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Create a normal distribution with mean equal to zero and standard
     * deviation equal to one.
     */
    public NormalDistribution() {
        this(0, 1);
    }

    /**
     * Create a normal distribution using the given mean and standard deviation.
     * 
     * @param meanIn
     *        Mean for this distribution.
     * @param sdIn
     *        Standard deviation for this distribution.
     * @throws NotStrictlyPositiveException
     *         if {@code sd <= 0}.
     */
    public NormalDistribution(final double meanIn, final double sdIn) {
        this(meanIn, sdIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a normal distribution using the given mean, standard deviation and
     * inverse cumulative distribution accuracy.
     * 
     * @param meanIn
     *        Mean for this distribution.
     * @param sd
     *        Standard deviation for this distribution.
     * @param inverseCumAccuracy
     *        Inverse cumulative probability accuracy.
     * @throws NotStrictlyPositiveException
     *         if {@code sd <= 0}.
     * @since 2.1
     */
    public NormalDistribution(final double meanIn, final double sd, final double inverseCumAccuracy) {
        this(new Well19937c(), meanIn, sd, inverseCumAccuracy);
    }

    /**
     * Creates a normal distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param meanIn
     *        Mean for this distribution.
     * @param sd
     *        Standard deviation for this distribution.
     * @param inverseCumAccuracy
     *        Inverse cumulative probability accuracy.
     * @throws NotStrictlyPositiveException
     *         if {@code sd <= 0}.
     * @since 3.1
     */
    public NormalDistribution(final RandomGenerator rng,
        final double meanIn,
        final double sd,
        final double inverseCumAccuracy) {
        super(rng);

        if (sd <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.STANDARD_DEVIATION, sd);
        }

        this.mean = meanIn;
        this.standardDeviation = sd;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Access the mean.
     * 
     * @return the mean for this distribution.
     */
    public double getMean() {
        return this.mean;
    }

    /**
     * Access the standard deviation.
     * 
     * @return the standard deviation for this distribution.
     */
    public double getStandardDeviation() {
        return this.standardDeviation;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        final double x0 = x - this.mean;
        final double x1 = x0 / this.standardDeviation;
        return MathLib.exp(-HALF * x1 * x1) / (this.standardDeviation * SQRT2PI);
    }

    /**
     * {@inheritDoc}
     * 
     * If {@code x} is more than 40 standard deviations from the mean, 0 or 1
     * is returned, as in these cases the actual value is within {@code Double.MIN_VALUE} of 0 or 1.
     */
    @Override
    public double cumulativeProbability(final double x) {
        final double dev = x - this.mean;
        if (MathLib.abs(dev) > FOURTY * this.standardDeviation) {
            return dev < 0 ? 0.0d : 1.0d;
        }
        return HALF * (1 + Erf.erf(dev / (this.standardDeviation * SQRT2)));
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final double x0,
                              final double x1) {
        if (x0 > x1) {
            throw new NumberIsTooLargeException(PatriusMessages.LOWER_ENDPOINT_ABOVE_UPPER_ENDPOINT,
                x0, x1, true);
        }
        final double denom = this.standardDeviation * SQRT2;
        final double v0 = (x0 - this.mean) / denom;
        final double v1 = (x1 - this.mean) / denom;
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
     * For mean parameter {@code mu}, the mean is {@code mu}.
     */
    @Override
    public double getNumericalMean() {
        return this.getMean();
    }

    /**
     * {@inheritDoc}
     * 
     * For standard deviation parameter {@code s}, the variance is {@code s^2}.
     */
    @Override
    public double getNumericalVariance() {
        final double s = this.getStandardDeviation();
        return s * s;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always negative infinity
     * no matter the parameters.
     * 
     * @return lower bound of the support (always {@code Double.NEGATIVE_INFINITY})
     */
    @Override
    public double getSupportLowerBound() {
        return Double.NEGATIVE_INFINITY;
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

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return this.standardDeviation * this.random.nextGaussian() + this.mean;
    }
}
