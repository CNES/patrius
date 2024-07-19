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
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Implementation of the Poisson distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Poisson_distribution">Poisson distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution (MathWorld)</a>
 * @version $Id: PoissonDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PoissonDistribution extends AbstractIntegerDistribution {
    /**
     * Default maximum number of iterations for cumulative probability calculations.
     * 
     * @since 2.1
     */
    public static final int DEFAULT_MAX_ITERATIONS = 10000000;
    /**
     * Default convergence criterion.
     * 
     * @since 2.1
     */
    public static final double DEFAULT_EPSILON = 1e-12;

    /** 32. */
    private static final double THIRTYTWO = 32.;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** 1000. */
    private static final double ONE_THOUSAND = 1000.;

    /** Serializable UID. */
    private static final long serialVersionUID = -3349935121172596109L;
    /** Distribution used to compute normal approximation. */
    private final NormalDistribution normal;
    /** Distribution needed for the {@link #sample()} method. */
    private final ExponentialDistribution exponential;
    /** Mean of the distribution. */
    private final double mean;

    /**
     * Maximum number of iterations for cumulative probability. Cumulative
     * probabilities are estimated using either Lanczos series approximation
     * of {@link Gamma#regularizedGammaP(double, double, double, int)} or continued fraction approximation of
     * {@link Gamma#regularizedGammaQ(double, double, double, int)}.
     */
    private final int maxIterations;

    /** Convergence criterion for cumulative probability. */
    private final double epsilon;

    /**
     * Creates a new Poisson distribution with specified mean.
     * 
     * @param p
     *        the Poisson mean
     * @throws NotStrictlyPositiveException
     *         if {@code p <= 0}.
     */
    public PoissonDistribution(final double p) {
        this(p, DEFAULT_EPSILON, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Creates a new Poisson distribution with specified mean, convergence
     * criterion and maximum number of iterations.
     * 
     * @param p
     *        Poisson mean.
     * @param epsilonIn
     *        Convergence criterion for cumulative probabilities.
     * @param maxIterationsIn
     *        the maximum number of iterations for cumulative
     *        probabilities.
     * @throws NotStrictlyPositiveException
     *         if {@code p <= 0}.
     * @since 2.1
     */
    public PoissonDistribution(final double p, final double epsilonIn, final int maxIterationsIn) {
        this(new Well19937c(), p, epsilonIn, maxIterationsIn);
    }

    /**
     * Creates a new Poisson distribution with specified mean, convergence
     * criterion and maximum number of iterations.
     * 
     * @param rng
     *        Random number generator.
     * @param p
     *        Poisson mean.
     * @param epsilonIn
     *        Convergence criterion for cumulative probabilities.
     * @param maxIterationsIn
     *        the maximum number of iterations for cumulative
     *        probabilities.
     * @throws NotStrictlyPositiveException
     *         if {@code p <= 0}.
     * @since 3.1
     */
    public PoissonDistribution(final RandomGenerator rng,
        final double p,
        final double epsilonIn,
        final int maxIterationsIn) {
        super(rng);

        if (p <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.MEAN, p);
        }
        this.mean = p;
        this.epsilon = epsilonIn;
        this.maxIterations = maxIterationsIn;

        // Use the same RNG instance as the parent class.
        this.normal = new NormalDistribution(rng, p, MathLib.sqrt(p),
            NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
        this.exponential = new ExponentialDistribution(rng, 1,
            ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }

    /**
     * Creates a new Poisson distribution with the specified mean and
     * convergence criterion.
     * 
     * @param p
     *        Poisson mean.
     * @param epsilonIn
     *        Convergence criterion for cumulative probabilities.
     * @throws NotStrictlyPositiveException
     *         if {@code p <= 0}.
     * @since 2.1
     */
    public PoissonDistribution(final double p, final double epsilonIn) {
        this(p, epsilonIn, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Creates a new Poisson distribution with the specified mean and maximum
     * number of iterations.
     * 
     * @param p
     *        Poisson mean.
     * @param maxIterationsIn
     *        Maximum number of iterations for cumulative
     *        probabilities.
     * @since 2.1
     */
    public PoissonDistribution(final double p, final int maxIterationsIn) {
        this(p, DEFAULT_EPSILON, maxIterationsIn);
    }

    /**
     * Get the mean for the distribution.
     * 
     * @return the mean for the distribution.
     */
    public double getMean() {
        return this.mean;
    }

    /** {@inheritDoc} */
    @Override
    public double probability(final int x) {
        final double ret;
        if (x < 0 || x == Integer.MAX_VALUE) {
            ret = 0.0;
        } else if (x == 0) {
            ret = MathLib.exp(-this.mean);
        } else {
            ret = MathLib.exp(-SaddlePointExpansion.getStirlingError(x) -
                SaddlePointExpansion.getDeviancePart(x, this.mean)) /
                MathLib.sqrt(MathUtils.TWO_PI * x);
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public double cumulativeProbability(final int x) {
        final double res;
        if (x < 0) {
            res = 0;
        } else if (x == Integer.MAX_VALUE) {
            res = 1;
        } else {
            res = Gamma.regularizedGammaQ((double) x + 1, this.mean, this.epsilon,
                this.maxIterations);
        }
        return res;
    }

    /**
     * Calculates the Poisson distribution function using a normal
     * approximation. The {@code N(mean, sqrt(mean))} distribution is used
     * to approximate the Poisson distribution. The computation uses
     * "half-correction" (evaluating the normal distribution function at {@code x + 0.5}).
     * 
     * @param x
     *        Upper bound, inclusive.
     * @return the distribution function value calculated using a normal
     *         approximation.
     */
    public double normalApproximateProbability(final int x) {
        // calculate the probability using half-correction
        return this.normal.cumulativeProbability(x + HALF);
    }

    /**
     * {@inheritDoc}
     * 
     * For mean parameter {@code p}, the mean is {@code p}.
     */
    @Override
    public double getNumericalMean() {
        return this.getMean();
    }

    /**
     * {@inheritDoc}
     * 
     * For mean parameter {@code p}, the variance is {@code p}.
     */
    @Override
    public double getNumericalVariance() {
        return this.getMean();
    }

    /**
     * {@inheritDoc}
     * 
     * The lower bound of the support is always 0 no matter the mean parameter.
     * 
     * @return lower bound of the support (always 0)
     */
    @Override
    public int getSupportLowerBound() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * The upper bound of the support is positive infinity,
     * regardless of the parameter values. There is no integer infinity,
     * so this method returns {@code Integer.MAX_VALUE}.
     * 
     * @return upper bound of the support (always {@code Integer.MAX_VALUE} for
     *         positive infinity)
     */
    @Override
    public int getSupportUpperBound() {
        return Integer.MAX_VALUE;
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
     * {@inheritDoc}
     * <p>
     * <strong>Algorithm Description</strong>:
     * <ul>
     * <li>For small means, uses simulation of a Poisson process using Uniform deviates, as described <a
     * href="http://irmi.epfl.ch/cmos/Pmmi/interactive/rng7.htm"> here</a>. The Poisson process (and hence value
     * returned) is bounded by 1000 * mean.</li>
     * <li>For large means, uses the rejection algorithm described in <quote> Devroye, Luc. (1981).<i>The Computer
     * Generation of Poisson Random Variables</i> <strong>Computing</strong> vol. 26 pp. 197-207. </quote></li>
     * </ul>
     * </p>
     * 
     * @return a random value.
     * @since 2.2
     */
    @Override
    public int sample() {
        return (int) MathLib.min(this.nextPoisson(this.mean), Integer.MAX_VALUE);
    }

    /**
     * @param meanPoisson
     *        Mean of the Poisson distribution.
     * @return the next sample.
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    private long nextPoisson(final double meanPoisson) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        final double pivot = 40.0d;
        if (meanPoisson < pivot) {
            final double p = MathLib.exp(-meanPoisson);
            long n = 0;
            double r = 1.0d;
            double rnd = 1.0d;

            while (n < ONE_THOUSAND * meanPoisson) {
                rnd = this.random.nextDouble();
                r = r * rnd;
                if (r >= p) {
                    n++;
                } else {
                    return n;
                }
            }
            return n;
        } else {
            final double lambda = MathLib.floor(meanPoisson);
            final double lambdaFractional = meanPoisson - lambda;
            final double logLambda = MathLib.log(lambda);
            final double logLambdaFactorial = ArithmeticUtils.factorialLog((int) lambda);
            final long y2 = lambdaFractional < Double.MIN_VALUE ? 0 : this.nextPoisson(lambdaFractional);
            final double delta = MathLib.sqrt(lambda * MathLib.log(THIRTYTWO * lambda / FastMath.PI + 1));
            final double halfDelta = delta / 2;
            final double twolpd = 2 * lambda + delta;
            final double a1 = MathLib.sqrt(FastMath.PI * twolpd) * MathLib.exp(1 / 8 * lambda);
            final double a2 = (twolpd / delta) * MathLib.exp(-delta * (1 + delta) / twolpd);
            final double aSum = a1 + a2 + 1;
            final double p1 = a1 / aSum;
            final double p2 = a2 / aSum;
            final double c1 = 1 / (8 * lambda);

            double x = 0;
            double y = 0;
            double v = 0;
            int a = 0;
            double t = 0;
            double qr = 0;
            double qa = 0;
            for (;;) {
                final double u = this.random.nextDouble();
                if (u <= p1) {
                    final double n = this.random.nextGaussian();
                    x = n * MathLib.sqrt(lambda + halfDelta) - HALF;
                    if (x > delta || x < -lambda) {
                        continue;
                    }
                    y = x < 0 ? MathLib.floor(x) : MathLib.ceil(x);
                    final double e = this.exponential.sample();
                    v = -e - (n * n / 2) + c1;
                } else {
                    if (u > p1 + p2) {
                        y = lambda;
                        break;
                    } else {
                        x = delta + (twolpd / delta) * this.exponential.sample();
                        y = MathLib.ceil(x);
                        v = -this.exponential.sample() - delta * (x + 1) / twolpd;
                    }
                }
                a = x < 0 ? 1 : 0;
                t = y * (y + 1) / (2 * lambda);
                if (v < -t && a == 0) {
                    y = lambda + y;
                    break;
                }
                qr = t * ((2 * y + 1) / (6 * lambda) - 1);
                qa = qr - (t * t) / (3 * (lambda + a * (y + 1)));
                if (v < qa) {
                    y = lambda + y;
                    break;
                }
                if (v > qr) {
                    continue;
                }
                if (v < y * logLambda - ArithmeticUtils.factorialLog((int) (y + lambda)) + logLambdaFactorial) {
                    y = lambda + y;
                    break;
                }
            }
            return y2 + (long) y;
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
