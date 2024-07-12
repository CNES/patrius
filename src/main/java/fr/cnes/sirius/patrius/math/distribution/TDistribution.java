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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Beta;
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of Student's t-distribution.
 * 
 * @see "<a href='http://en.wikipedia.org/wiki/Student&apos;s_t-distribution'>Student's t-distribution (Wikipedia)</a>"
 * @see "<a href='http://mathworld.wolfram.com/Studentst-Distribution.html'>Student's t-distribution (MathWorld)</a>"
 * @version $Id: TDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class TDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    /** Serializable UID. */
    private static final long serialVersionUID = -5852615386664158222L;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** The degrees of freedom. */
    private final double degreesOfFreedom;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Create a t distribution using the given degrees of freedom.
     * 
     * @param degreesOfFreedomIn
     *        Degrees of freedom.
     * @throws NotStrictlyPositiveException
     *         if {@code degreesOfFreedom <= 0}
     */
    public TDistribution(final double degreesOfFreedomIn) {
        this(degreesOfFreedomIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Create a t distribution using the given degrees of freedom and the
     * specified inverse cumulative probability absolute accuracy.
     * 
     * @param degreesOfFreedomIn
     *        Degrees of freedom.
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates
     *        (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code degreesOfFreedom <= 0}
     * @since 2.1
     */
    public TDistribution(final double degreesOfFreedomIn, final double inverseCumAccuracy) {
        this(new Well19937c(), degreesOfFreedomIn, inverseCumAccuracy);
    }

    /**
     * Creates a t distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param degreesOfFreedomIn
     *        Degrees of freedom.
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates
     *        (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code degreesOfFreedom <= 0}
     * @since 3.1
     */
    public TDistribution(final RandomGenerator rng,
        final double degreesOfFreedomIn,
        final double inverseCumAccuracy) {
        super(rng);

        if (degreesOfFreedomIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.DEGREES_OF_FREEDOM,
                degreesOfFreedomIn);
        }
        this.degreesOfFreedom = degreesOfFreedomIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * Access the degrees of freedom.
     * 
     * @return the degrees of freedom.
     */
    public double getDegreesOfFreedom() {
        return this.degreesOfFreedom;
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double x) {
        final double n = this.degreesOfFreedom;
        final double nPlus1Over2 = (n + 1) / 2;
        return MathLib.exp(Gamma.logGamma(nPlus1Over2) -
            HALF * (MathLib.log(FastMath.PI) +
            MathLib.log(n)) -
            Gamma.logGamma(n / 2) -
            nPlus1Over2 * MathLib.log(1 + x * x / n));
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final double x) {
        final double ret;
        if (x == 0) {
            ret = HALF;
        } else {
            final double t =
                Beta.regularizedBeta(
                    this.degreesOfFreedom / (this.degreesOfFreedom + (x * x)),
                    0.5 * this.degreesOfFreedom,
                    0.5);
            if (x < 0.0) {
                ret = HALF * t;
            } else {
                ret = 1.0 - HALF * t;
            }
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
     * For degrees of freedom parameter {@code df}, the mean is
     * <ul>
     * <li>if {@code df > 1} then {@code 0},</li>
     * <li>else undefined ({@code Double.NaN}).</li>
     * </ul>
     */
    @Override
    public double getNumericalMean() {
        final double df = this.getDegreesOfFreedom();

        if (df > 1) {
            return 0;
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     * 
     * For degrees of freedom parameter {@code df}, the variance is
     * <ul>
     * <li>if {@code df > 2} then {@code df / (df - 2)},</li>
     * <li>if {@code 1 < df <= 2} then positive infinity ({@code Double.POSITIVE_INFINITY}),</li>
     * <li>else undefined ({@code Double.NaN}).</li>
     * </ul>
     */
    @Override
    public double getNumericalVariance() {
        final double df = this.getDegreesOfFreedom();

        final double res;
        if (df > 2) {
            res = df / (df - 2);
        } else if (df > 1 && df <= 2) {
            res = Double.POSITIVE_INFINITY;
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always negative infinity no matter the
     * parameters.
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
     * The upper bound of the support is always positive infinity no matter the
     * parameters.
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
}
