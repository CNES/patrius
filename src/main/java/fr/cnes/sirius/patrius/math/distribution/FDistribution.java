/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.special.Beta;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the F-distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/F-distribution">F-distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/F-Distribution.html">F-distribution (MathWorld)</a>
 * @version $Id: FDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class FDistribution extends AbstractRealDistribution {
    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    /** 0.5. */
    private static final double HALF = 0.5;
    /** Serializable UID. */
    private static final long serialVersionUID = -8516354193418641566L;
    /** The numerator degrees of freedom. */
    private final double numeratorDegreesOfFreedom;
    /** The numerator degrees of freedom. */
    private final double denominatorDegreesOfFreedom;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;
    /** Cached numerical variance */
    private double numericalVariance = Double.NaN;
    /** Whether or not the numerical variance has been calculated */
    private boolean numericalVarianceIsCalculated = false;

    /**
     * Creates an F distribution using the given degrees of freedom.
     * 
     * @param numeratorDegreesOfFreedomIn
     *        Numerator degrees of freedom.
     * @param denominatorDegreesOfFreedomIn
     *        Denominator degrees of freedom.
     * @throws NotStrictlyPositiveException
     *         if {@code numeratorDegreesOfFreedom <= 0} or {@code denominatorDegreesOfFreedom <= 0}.
     */
    public FDistribution(final double numeratorDegreesOfFreedomIn,
        final double denominatorDegreesOfFreedomIn) {
        this(numeratorDegreesOfFreedomIn, denominatorDegreesOfFreedomIn,
            DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Creates an F distribution using the given degrees of freedom
     * and inverse cumulative probability accuracy.
     * 
     * @param numeratorDegreesOfFreedomIn
     *        Numerator degrees of freedom.
     * @param denominatorDegreesOfFreedomIn
     *        Denominator degrees of freedom.
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates.
     * @throws NotStrictlyPositiveException
     *         if {@code numeratorDegreesOfFreedom <= 0} or {@code denominatorDegreesOfFreedom <= 0}.
     * @since 2.1
     */
    public FDistribution(final double numeratorDegreesOfFreedomIn,
        final double denominatorDegreesOfFreedomIn,
        final double inverseCumAccuracy) {
        this(new Well19937c(), numeratorDegreesOfFreedomIn,
            denominatorDegreesOfFreedomIn, inverseCumAccuracy);
    }

    /**
     * Creates an F distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param numeratorDegreesOfFreedomIn
     *        Numerator degrees of freedom.
     * @param denominatorDegreesOfFreedomIn
     *        Denominator degrees of freedom.
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates.
     * @throws NotStrictlyPositiveException
     *         if {@code numeratorDegreesOfFreedom <= 0} or {@code denominatorDegreesOfFreedom <= 0}.
     * @since 3.1
     */
    public FDistribution(final RandomGenerator rng,
        final double numeratorDegreesOfFreedomIn,
        final double denominatorDegreesOfFreedomIn,
        final double inverseCumAccuracy) {
        super(rng);

        if (numeratorDegreesOfFreedomIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.DEGREES_OF_FREEDOM,
                numeratorDegreesOfFreedomIn);
        }
        if (denominatorDegreesOfFreedomIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.DEGREES_OF_FREEDOM,
                denominatorDegreesOfFreedomIn);
        }
        this.numeratorDegreesOfFreedom = numeratorDegreesOfFreedomIn;
        this.denominatorDegreesOfFreedom = denominatorDegreesOfFreedomIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.1
     */
    @Override
    public double density(final double x) {
        // Init output variable
        final double res;
        // Local variables
        final double nhalf = this.numeratorDegreesOfFreedom / 2;
        final double mhalf = this.denominatorDegreesOfFreedom / 2;
        final double logx = MathLib.log(x);
        final double logn = MathLib.log(this.numeratorDegreesOfFreedom);
        final double logm = MathLib.log(this.denominatorDegreesOfFreedom);
        final double lognxm = MathLib.log(this.numeratorDegreesOfFreedom * x + this.denominatorDegreesOfFreedom);
        // Compute res
        if (!Double.isNaN(nhalf) && !Double.isNaN(mhalf)) {
            final double tempRes = nhalf * logn + nhalf * logx - logx + mhalf * logm - nhalf
                * lognxm - mhalf * lognxm - Beta.logBeta(nhalf, mhalf);
            if (!Double.isNaN(tempRes)) {
                res = MathLib.exp(tempRes);
            } else {
                // Computation unsuccessful
                res = Double.NaN;
            }
        } else {
            // Computation unsuccessful
            res = Double.NaN;
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * 
     * The implementation of this method is based on
     * <ul>
     * <li>
     * <a href="http://mathworld.wolfram.com/F-Distribution.html"> F-Distribution</a>, equation (4).</li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(final double x) {
        final double ret;
        if (x <= 0) {
            ret = 0;
        } else {
            final double n = this.numeratorDegreesOfFreedom;
            final double m = this.denominatorDegreesOfFreedom;

            ret = Beta.regularizedBeta((n * x) / (m + n * x),
                HALF * n,
                HALF * m);
        }
        return ret;
    }

    /**
     * Access the numerator degrees of freedom.
     * 
     * @return the numerator degrees of freedom.
     */
    public double getNumeratorDegreesOfFreedom() {
        return this.numeratorDegreesOfFreedom;
    }

    /**
     * Access the denominator degrees of freedom.
     * 
     * @return the denominator degrees of freedom.
     */
    public double getDenominatorDegreesOfFreedom() {
        return this.denominatorDegreesOfFreedom;
    }

    /** {@inheritDoc} */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For denominator degrees of freedom parameter {@code b}, the mean is
     * <ul>
     * <li>if {@code b > 2} then {@code b / (b - 2)},</li>
     * <li>else undefined ({@code Double.NaN}).
     * </ul>
     */
    @Override
    public double getNumericalMean() {
        final double denominatorDF = this.getDenominatorDegreesOfFreedom();

        if (denominatorDF > 2) {
            return denominatorDF / (denominatorDF - 2);
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     * 
     * For numerator degrees of freedom parameter {@code a} and denominator
     * degrees of freedom parameter {@code b}, the variance is
     * <ul>
     * <li>
     * if {@code b > 4} then {@code [2 * b^2 * (a + b - 2)] / [a * (b - 2)^2 * (b - 4)]},</li>
     * <li>else undefined ({@code Double.NaN}).
     * </ul>
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
        final double denominatorDF = this.getDenominatorDegreesOfFreedom();

        if (denominatorDF > 4) {
            final double numeratorDF = this.getNumeratorDegreesOfFreedom();
            final double denomDFMinusTwo = denominatorDF - 2;

            return (2 * (denominatorDF * denominatorDF) * (numeratorDF + denominatorDF - 2)) /
                ((numeratorDF * (denomDFMinusTwo * denomDFMinusTwo) * (denominatorDF - 4)));
        }

        return Double.NaN;
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
