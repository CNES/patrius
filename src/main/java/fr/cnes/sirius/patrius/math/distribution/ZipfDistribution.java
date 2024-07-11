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
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the Zipf distribution.
 * 
 * @see <a href="http://mathworld.wolfram.com/ZipfDistribution.html">Zipf distribution (MathWorld)</a>
 * @version $Id: ZipfDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ZipfDistribution extends AbstractIntegerDistribution {
    /** Serializable version identifier. */
    private static final long serialVersionUID = -140627372283420404L;
    /** Number of elements. */
    private final int numberOfElements;
    /** Exponent parameter of the distribution. */
    private final double exponent;
    /** Cached numerical mean */
    private double numericalMean = Double.NaN;
    /** Whether or not the numerical mean has been calculated */
    private boolean numericalMeanIsCalculated = false;
    /** Cached numerical variance */
    private double numericalVariance = Double.NaN;
    /** Whether or not the numerical variance has been calculated */
    private boolean numericalVarianceIsCalculated = false;

    /**
     * Create a new Zipf distribution with the given number of elements and
     * exponent.
     * 
     * @param numberOfElementsIn
     *        Number of elements.
     * @param exponentIn
     *        Exponent.
     * @exception NotStrictlyPositiveException
     *            if {@code numberOfElements <= 0} or {@code exponent <= 0}.
     */
    public ZipfDistribution(final int numberOfElementsIn, final double exponentIn) {
        this(new Well19937c(), numberOfElementsIn, exponentIn);
    }

    /**
     * Creates a Zipf distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param numberOfElementsIn
     *        Number of elements.
     * @param exponentIn
     *        Exponent.
     * @exception NotStrictlyPositiveException
     *            if {@code numberOfElements <= 0} or {@code exponent <= 0}.
     * @since 3.1
     */
    public ZipfDistribution(final RandomGenerator rng,
        final int numberOfElementsIn,
        final double exponentIn) {
        super(rng);

        if (numberOfElementsIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.DIMENSION,
                numberOfElementsIn);
        }
        if (exponentIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.EXPONENT,
                exponentIn);
        }

        this.numberOfElements = numberOfElementsIn;
        this.exponent = exponentIn;
    }

    /**
     * Get the number of elements (e.g. corpus size) for the distribution.
     * 
     * @return the number of elements
     */
    public int getNumberOfElements() {
        return this.numberOfElements;
    }

    /**
     * Get the exponent characterizing the distribution.
     * 
     * @return the exponent
     */
    public double getExponent() {
        return this.exponent;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final int x) {
        if (x <= 0 || x > this.numberOfElements) {
            return 0.0;
        }

        return (1.0 / MathLib.pow(x, this.exponent)) / this.generalizedHarmonic(this.numberOfElements, this.exponent);
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        final double res;
        if (x <= 0) {
            res = 0.0;
        } else if (x >= this.numberOfElements) {
            res = 1.0;
        } else {
            res =
                this.generalizedHarmonic(x, this.exponent)
                    / this.generalizedHarmonic(this.numberOfElements, this.exponent);
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * 
     * For number of elements {@code N} and exponent {@code s}, the mean is {@code Hs1 / Hs}, where
     * <ul>
     * <li>{@code Hs1 = generalizedHarmonic(N, s - 1)},</li>
     * <li>{@code Hs = generalizedHarmonic(N, s)}.</li>
     * </ul>
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
     * Used by {@link #getNumericalMean()}.
     * 
     * @return the mean of this distribution
     */
    protected double calculateNumericalMean() {
        final int n = this.getNumberOfElements();
        final double s = this.getExponent();

        final double hs1 = this.generalizedHarmonic(n, s - 1);
        final double hs = this.generalizedHarmonic(n, s);

        return hs1 / hs;
    }

    /**
     * {@inheritDoc}
     * 
     * For number of elements {@code N} and exponent {@code s}, the mean is {@code (Hs2 / Hs) - (Hs1^2 / Hs^2)}, where
     * <ul>
     * <li>{@code Hs2 = generalizedHarmonic(N, s - 2)},</li>
     * <li>{@code Hs1 = generalizedHarmonic(N, s - 1)},</li>
     * <li>{@code Hs = generalizedHarmonic(N, s)}.</li>
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
     * Used by {@link #getNumericalVariance()}.
     * 
     * @return the variance of this distribution
     */
    protected double calculateNumericalVariance() {
        final int n = this.getNumberOfElements();
        final double s = this.getExponent();

        final double hs2 = this.generalizedHarmonic(n, s - 2);
        final double hs1 = this.generalizedHarmonic(n, s - 1);
        final double hs = this.generalizedHarmonic(n, s);

        return (hs2 / hs) - ((hs1 * hs1) / (hs * hs));
    }

    /**
     * Calculates the Nth generalized harmonic number. See
     * <a href="http://mathworld.wolfram.com/HarmonicSeries.html">Harmonic
     * Series</a>.
     * 
     * @param n
     *        Term in the series to calculate (must be larger than 1)
     * @param m
     *        Exponent (special case {@code m = 1} is the harmonic series).
     * @return the n<sup>th</sup> generalized harmonic number.
     */
    private double generalizedHarmonic(final int n, final double m) {
        double value = 0;
        for (int k = n; k > 0; --k) {
            value += 1.0 / MathLib.pow(k, m);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 1 no matter the parameters.
     * 
     * @return lower bound of the support (always 1)
     */
    @Override
    public int getSupportLowerBound() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is the number of elements.
     * 
     * @return upper bound of the support
     */
    @Override
    public int getSupportUpperBound() {
        return this.getNumberOfElements();
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
