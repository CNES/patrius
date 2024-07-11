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

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the triangular real distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Triangular_distribution"> Triangular distribution (Wikipedia)</a>
 * 
 * @version $Id: TriangularDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class TriangularDistribution extends AbstractRealDistribution {

    /** 18. */
    private static final double EIGHTEEN = 18;

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20120112L;
    /** Lower limit of this distribution (inclusive). */
    private final double a;
    /** Upper limit of this distribution (inclusive). */
    private final double b;
    /** Mode of this distribution. */
    private final double c;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Creates a triangular real distribution using the given lower limit,
     * upper limit, and mode.
     * 
     * @param aIn
     *        Lower limit of this distribution (inclusive).
     * @param bIn
     *        Upper limit of this distribution (inclusive).
     * @param cIn
     *        Mode of this distribution.
     * @throws NumberIsTooLargeException
     *         if {@code a >= b} or if {@code c > b}.
     * @throws NumberIsTooSmallException
     *         if {@code c < a}.
     */
    public TriangularDistribution(final double aIn, final double cIn, final double bIn) {
        this(new Well19937c(), aIn, cIn, bIn);
    }

    /**
     * Creates a triangular distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param aIn
     *        Lower limit of this distribution (inclusive).
     * @param bIn
     *        Upper limit of this distribution (inclusive).
     * @param cIn
     *        Mode of this distribution.
     * @throws NumberIsTooLargeException
     *         if {@code a >= b} or if {@code c > b}.
     * @throws NumberIsTooSmallException
     *         if {@code c < a}.
     * @since 3.1
     */
    public TriangularDistribution(final RandomGenerator rng,
        final double aIn,
        final double cIn,
        final double bIn) {
        super(rng);

        if (aIn >= bIn) {
            throw new NumberIsTooLargeException(
                PatriusMessages.LOWER_BOUND_NOT_BELOW_UPPER_BOUND,
                aIn, bIn, false);
        }
        if (cIn < aIn) {
            throw new NumberIsTooSmallException(
                PatriusMessages.NUMBER_TOO_SMALL, cIn, aIn, true);
        }
        if (cIn > bIn) {
            throw new NumberIsTooLargeException(
                PatriusMessages.NUMBER_TOO_LARGE, cIn, bIn, true);
        }

        this.a = aIn;
        this.c = cIn;
        this.b = bIn;
        this.solverAbsoluteAccuracy = MathLib.max(MathLib.ulp(aIn), MathLib.ulp(bIn));
    }

    /**
     * Returns the mode {@code c} of this distribution.
     * 
     * @return the mode {@code c} of this distribution
     */
    public double getMode() {
        return this.c;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * For this distribution, the returned value is not really meaningful, since exact formulas are implemented for the
     * computation of the {@link #inverseCumulativeProbability(double)} (no solver is invoked).
     * </p>
     * <p>
     * For lower limit {@code a} and upper limit {@code b}, the current implementation returns
     * {@code max(ulp(a), ulp(b)}.
     * </p>
     */
    @Override
    protected double getSolverAbsoluteAccuracy() {
        return this.solverAbsoluteAccuracy;
    }

    /**
     * {@inheritDoc}
     * 
     * For lower limit {@code a}, upper limit {@code b} and mode {@code c}, the
     * PDF is given by
     * <ul>
     * <li>{@code 2 * (x - a) / [(b - a) * (c - a)]} if {@code a <= x < c},</li>
     * <li>{@code 2 / (b - a)} if {@code x = c},</li>
     * <li>{@code 2 * (b - x) / [(b - a) * (b - c)]} if {@code c < x <= b},</li>
     * <li>{@code 0} otherwise.
     * </ul>
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double density(final double x) {
        // CHECKSTYLE: resume ReturnCount check
        if (x < this.a) {
            return 0;
        }
        if (this.a <= x && x < this.c) {
            final double divident = 2 * (x - this.a);
            final double divisor = (this.b - this.a) * (this.c - this.a);
            return divident / divisor;
        }
        if (x == this.c) {
            return 2 / (this.b - this.a);
        }
        if (this.c < x && x <= this.b) {
            // General case
            //
            final double divident = 2 * (this.b - x);
            final double divisor = (this.b - this.a) * (this.b - this.c);
            return divident / divisor;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * For lower limit {@code a}, upper limit {@code b} and mode {@code c}, the
     * CDF is given by
     * <ul>
     * <li>{@code 0} if {@code x < a},</li>
     * <li>{@code (x - a)^2 / [(b - a) * (c - a)]} if {@code a <= x < c},</li>
     * <li>{@code (c - a) / (b - a)} if {@code x = c},</li>
     * <li>{@code 1 - (b - x)^2 / [(b - a) * (b - c)]} if {@code c < x <= b},</li>
     * <li>{@code 1} if {@code x > b}.</li>
     * </ul>
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double cumulativeProbability(final double x) {
        // CHECKSTYLE: resume ReturnCount check
        if (x < this.a) {
            // Probability is 0
            return 0;
        }
        if (this.a <= x && x < this.c) {
            final double divident = (x - this.a) * (x - this.a);
            final double divisor = (this.b - this.a) * (this.c - this.a);
            return divident / divisor;
        }
        if (x == this.c) {
            // Immediate return
            return (this.c - this.a) / (this.b - this.a);
        }
        if (this.c < x && x <= this.b) {
            final double divident = (this.b - x) * (this.b - x);
            final double divisor = (this.b - this.a) * (this.b - this.c);
            return 1 - (divident / divisor);
        }
        // Ending probability
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * For lower limit {@code a}, upper limit {@code b}, and mode {@code c},
     * the mean is {@code (a + b + c) / 3}.
     */
    @Override
    public double getNumericalMean() {
        return (this.a + this.b + this.c) / 3;
    }

    /**
     * {@inheritDoc}
     * 
     * For lower limit {@code a}, upper limit {@code b}, and mode {@code c},
     * the variance is {@code (a^2 + b^2 + c^2 - a * b - a * c - b * c) / 18}.
     */
    @Override
    public double getNumericalVariance() {
        return (this.a * this.a + this.b * this.b + this.c * this.c - this.a * this.b - this.a * this.c - this.b
            * this.c)
            / EIGHTEEN;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is equal to the lower limit parameter {@code a} of the distribution.
     * 
     * @return lower bound of the support
     */
    @Override
    public double getSupportLowerBound() {
        return this.a;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is equal to the upper limit parameter {@code b} of the distribution.
     * 
     * @return upper bound of the support
     */
    @Override
    public double getSupportUpperBound() {
        return this.b;
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

    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    /** {@inheritDoc} */
    @Override
    public double inverseCumulativeProbability(final double p) {
        // CHECKSTYLE: resume ReturnCount check
        if (p < 0 || p > 1) {
            throw new OutOfRangeException(p, 0, 1);
        }
        if (p == 0) {
            return this.a;
        }
        if (p == 1) {
            return this.b;
        }
        if (p < (this.c - this.a) / (this.b - this.a)) {
            return this.a + MathLib.sqrt(p * (this.b - this.a) * (this.c - this.a));
        }
        return this.b - MathLib.sqrt((1 - p) * (this.b - this.a) * (this.b - this.c));
    }
}
