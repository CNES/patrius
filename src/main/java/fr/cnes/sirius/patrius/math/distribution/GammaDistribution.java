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
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of the Gamma distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Gamma_distribution">Gamma distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/GammaDistribution.html">Gamma distribution (MathWorld)</a>
 * @version $Id: GammaDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GammaDistribution extends AbstractRealDistribution {

    /**
     * Default inverse cumulative probability accuracy.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;

    /** Coefficient. */
    private static final double COEFFICIENT = 0.0331;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 0.333333333333333333. */
    private static final double ONE_THIRD = 0.333333333333333333;

    /** Serializable UID. */
    private static final long serialVersionUID = 20120524L;
    /** The shape parameter. */
    private final double shape;
    /** The scale parameter. */
    private final double scale;
    /**
     * The constant value of {@code shape + g + 0.5}, where {@code g} is the
     * Lanczos constant {@link Gamma#LANCZOS_G}.
     */
    private final double shiftedShape;
    /**
     * The constant value of {@code shape / scale * sqrt(e / (2 * pi * (shape + g + 0.5))) / L(shape)},
     * where {@code L(shape)} is the Lanczos approximation returned by {@link Gamma#lanczos(double)}. This prefactor is
     * used in {@link #density(double)}, when no overflow occurs with the natural
     * calculation.
     */
    private final double densityPrefactor1;
    /**
     * The constant value of {@code shape * sqrt(e / (2 * pi * (shape + g + 0.5))) / L(shape)},
     * where {@code L(shape)} is the Lanczos approximation returned by {@link Gamma#lanczos(double)}. This prefactor is
     * used in {@link #density(double)}, when overflow occurs with the natural
     * calculation.
     */
    private final double densityPrefactor2;
    /**
     * Lower bound on {@code y = x / scale} for the selection of the computation
     * method in {@link #density(double)}. For {@code y <= minY}, the natural
     * calculation overflows.
     */
    private final double minY;
    /**
     * Upper bound on {@code log(y)} ({@code y = x / scale}) for the selection
     * of the computation method in {@link #density(double)}. For {@code log(y) >= maxLogY}, the natural calculation
     * overflows.
     */
    private final double maxLogY;
    /** Inverse cumulative probability accuracy. */
    private final double solverAbsoluteAccuracy;

    /**
     * Creates a new gamma distribution with specified values of the shape and
     * scale parameters.
     * 
     * @param shapeIn
     *        the shape parameter
     * @param scaleIn
     *        the scale parameter
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0} or {@code scale <= 0}.
     */
    public GammaDistribution(final double shapeIn, final double scaleIn) {
        this(shapeIn, scaleIn, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Creates a new gamma distribution with specified values of the shape and
     * scale parameters.
     * 
     * @param shapeIn
     *        the shape parameter
     * @param scaleIn
     *        the scale parameter
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0} or {@code scale <= 0}.
     * @since 2.1
     */
    public GammaDistribution(final double shapeIn, final double scaleIn, final double inverseCumAccuracy) {
        this(new Well19937c(), shapeIn, scaleIn, inverseCumAccuracy);
    }

    /**
     * Creates a Gamma distribution.
     * 
     * @param rng
     *        Random number generator.
     * @param shapeIn
     *        the shape parameter
     * @param scaleIn
     *        the scale parameter
     * @param inverseCumAccuracy
     *        the maximum absolute error in inverse
     *        cumulative probability estimates (defaults to {@link #DEFAULT_INVERSE_ABSOLUTE_ACCURACY}).
     * @throws NotStrictlyPositiveException
     *         if {@code shape <= 0} or {@code scale <= 0}.
     * @since 3.1
     */
    public GammaDistribution(final RandomGenerator rng,
        final double shapeIn,
        final double scaleIn,
        final double inverseCumAccuracy) {
        super(rng);

        if (shapeIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SHAPE, shapeIn);
        }
        if (scaleIn <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.SCALE, scaleIn);
        }

        this.shape = shapeIn;
        this.scale = scaleIn;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
        this.shiftedShape = shapeIn + Gamma.LANCZOS_G + HALF;
        final double aux = FastMath.E / (2.0 * FastMath.PI * this.shiftedShape);
        this.densityPrefactor2 = shapeIn * MathLib.sqrt(aux) / Gamma.lanczos(shapeIn);
        this.densityPrefactor1 = this.densityPrefactor2 / scaleIn *
            MathLib.pow(this.shiftedShape, -shapeIn) *
            MathLib.exp(shapeIn + Gamma.LANCZOS_G);
        this.minY = shapeIn + Gamma.LANCZOS_G - MathLib.log(Double.MAX_VALUE);
        this.maxLogY = MathLib.log(Double.MAX_VALUE) / (shapeIn - 1.0);
    }

    /**
     * Returns the shape parameter of {@code this} distribution.
     * 
     * @return the shape parameter
     * @since 3.1
     */
    public double getShape() {
        return this.shape;
    }

    /**
     * Returns the scale parameter of {@code this} distribution.
     * 
     * @return the scale parameter
     * @since 3.1
     */
    public double getScale() {
        return this.scale;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double density(final double x) {
        // CHECKSTYLE: resume ReturnCount check
        /*
         * The present method must return the value of
         * 1 x a - x
         * ---------- (-) exp(---)
         * x Gamma(a) b b
         * where a is the shape parameter, and b the scale parameter.
         * Substituting the Lanczos approximation of Gamma(a) leads to the
         * following expression of the density
         * a e 1 y a
         * - sqrt(------------------) ---- (-----------) exp(a - y + g),
         * x 2 pi (a + g + 0.5) L(a) a + g + 0.5
         * where y = x / b. The above formula is the "natural" computation, which
         * is implemented when no overflow is likely to occur. If overflow occurs
         * with the natural computation, the following identity is used. It is
         * based on the BOOST library
         * http://www.boost.org/doc/libs/1_35_0/libs/math/doc/sf_and_dist/html/math_toolkit/special/sf_gamma/igamma.html
         * Formula (15) needs adaptations, which are detailed below.
         * y a
         * (-----------) exp(a - y + g)
         * a + g + 0.5
         * y - a - g - 0.5 y (g + 0.5)
         * = exp(a log1pm(---------------) - ----------- + g),
         * a + g + 0.5 a + g + 0.5
         * where log1pm(z) = log(1 + z) - z. Therefore, the value to be
         * returned is
         * a e 1
         * - sqrt(------------------) ----
         * x 2 pi (a + g + 0.5) L(a)
         * y - a - g - 0.5 y (g + 0.5)
         * * exp(a log1pm(---------------) - ----------- + g).
         * a + g + 0.5 a + g + 0.5
         */
        final double res;
        if (x < 0) {
            res = 0;
        } else {
            final double y = x / this.scale;
            if ((y <= this.minY) || (MathLib.log(y) >= this.maxLogY)) {
                /*
                 * Overflow.
                 */
                final double aux1 = (y - this.shiftedShape) / this.shiftedShape;
                final double aux2 = this.shape * (MathLib.log1p(aux1) - aux1);
                final double aux3 = -y * (Gamma.LANCZOS_G + HALF) / this.shiftedShape +
                    Gamma.LANCZOS_G + aux2;
                if (!Double.isNaN(aux3)) {
                    res = this.densityPrefactor2 / x * MathLib.exp(aux3);
                } else {
                    res = Double.NaN;
                }
            } else {
                /*
                 * Natural calculation.
                 */
                res = this.densityPrefactor1 * MathLib.exp(-y) *
                    MathLib.pow(y, this.shape - 1);
            }
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * 
     * The implementation of this method is based on:
     * <ul>
     * <li>
     * <a href="http://mathworld.wolfram.com/Chi-SquaredDistribution.html"> 
     * Chi-Squared Distribution</a>, equation (9).</li>
     * <li>Casella, G., & Berger, R. (1990). <i>Statistical Inference</i>. Belmont, CA: Duxbury Press.</li>
     * </ul>
     */
    @Override
    public double cumulativeProbability(final double x) {
        final double ret;

        if (x <= 0) {
            ret = 0;
        } else {
            ret = Gamma.regularizedGammaP(this.shape, x / this.scale);
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
     * For shape parameter {@code alpha} and scale parameter {@code beta}, the
     * mean is {@code alpha * beta}.
     */
    @Override
    public double getNumericalMean() {
        return this.shape * this.scale;
    }

    /**
     * {@inheritDoc}
     * 
     * For shape parameter {@code alpha} and scale parameter {@code beta}, the
     * variance is {@code alpha * beta^2}.
     * 
     * @return {@inheritDoc}
     */
    @Override
    public double getNumericalVariance() {
        return this.shape * this.scale * this.scale;
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

    /**
     * <p>
     * This implementation uses the following algorithms:
     * </p>
     * 
     * <p>
     * For 0 < shape < 1: <br/>
     * Ahrens, J. H. and Dieter, U., <i>Computer methods for sampling from gamma, beta, Poisson and binomial
     * distributions.</i> Computing, 12, 223-246, 1974.
     * </p>
     * 
     * <p>
     * For shape >= 1: <br/>
     * Marsaglia and Tsang, <i>A Simple Method for Generating Gamma Variables.</i> ACM Transactions on Mathematical
     * Software, Volume 26 Issue 3, September, 2000.
     * </p>
     * 
     * @return random value sampled from the Gamma(shape, scale) distribution
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public double sample() {
        // CHECKSTYLE: resume ReturnCount check
        if (this.shape < 1) {
            // [1]: p. 228, Algorithm GS

            while (true) {
                // Step 1:
                final double u = this.random.nextDouble();
                final double bGS = 1 + this.shape / FastMath.E;
                final double p = bGS * u;

                if (p <= 1) {
                    // Step 2:

                    final double x = MathLib.pow(p, 1 / this.shape);
                    final double u2 = this.random.nextDouble();

                    if (u2 > MathLib.exp(-x)) {
                        // Reject
                        continue;
                    }
                    return this.scale * x;
                }

                // Step 3:

                final double x = -1 * MathLib.log((bGS - p) / this.shape);
                final double u2 = this.random.nextDouble();

                if (u2 > MathLib.pow(x, this.shape - 1)) {
                    // Reject
                    continue;
                }

                return this.scale * x;
            }
        }

        // Now shape >= 1

        final double d = this.shape - ONE_THIRD;
        final double c = 1 / (3 * MathLib.sqrt(d));

        while (true) {
            final double x = this.random.nextGaussian();
            final double v = (1 + c * x) * (1 + c * x) * (1 + c * x);

            if (v <= 0) {
                continue;
            }

            final double x2 = x * x;
            final double u = this.random.nextDouble();

            // Squeeze
            if (u < 1 - COEFFICIENT * x2 * x2) {
                return this.scale * d * v;
            }

            if (MathLib.log(u) < HALF * x2 + d * (1 - v + MathLib.log(v))) {
                return this.scale * d * v;
            }
        }
    }
}
