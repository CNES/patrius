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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Base interface for distributions on the reals.
 * 
 * @version $Id: RealDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public interface RealDistribution {
    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(X = x)}. In other
     * words, this method represents the probability mass function (PMF)
     * for the distribution.
     * 
     * @param x
     *        the point at which the PMF is evaluated
     * @return the value of the probability mass function at point {@code x}
     */
    double probability(double x);

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(x0 < X <= x1)}.
     * 
     * @param x0
     *        Lower bound (excluded).
     * @param x1
     *        Upper bound (included).
     * @return the probability that a random variable with this distribution
     *         takes a value between {@code x0} and {@code x1}, excluding the lower
     *         and including the upper endpoint.
     * @throws NumberIsTooLargeException
     *         if {@code x0 > x1}.
     * 
     *         The default implementation uses the identity {@code P(x0 < X <= x1) = P(X <= x1) - P(X <= x0)}
     * 
     * @since 3.1
     */
    double probability(double x0, double x1);

    /**
     * Returns the probability density function (PDF) of this distribution
     * evaluated at the specified point {@code x}. In general, the PDF is
     * the derivative of the {@link #cumulativeProbability(double) CDF}.
     * If the derivative does not exist at {@code x}, then an appropriate
     * replacement should be returned, e.g. {@code Double.POSITIVE_INFINITY}, {@code Double.NaN}, or the limit inferior
     * or limit superior of the
     * difference quotient.
     * 
     * @param x
     *        the point at which the PDF is evaluated
     * @return the value of the probability density function at point {@code x}
     */
    double density(double x);

    /**
     * For a random variable {@code X} whose values are distributed according
     * to this distribution, this method returns {@code P(X <= x)}. In other
     * words, this method represents the (cumulative) distribution function
     * (CDF) for this distribution.
     * 
     * @param x
     *        the point at which the CDF is evaluated
     * @return the probability that a random variable with this
     *         distribution takes a value less than or equal to {@code x}
     */
    double cumulativeProbability(double x);

    /**
     * Computes the quantile function of this distribution. For a random
     * variable {@code X} distributed according to this distribution, the
     * returned value is
     * <ul>
     * <li><code>inf{x in R | P(X<=x) >= p}</code> for {@code 0 < p <= 1},</li>
     * <li><code>inf{x in R | P(X<=x) > 0}</code> for {@code p = 0}.</li>
     * </ul>
     * 
     * @param p
     *        the cumulative probability
     * @return the smallest {@code p}-quantile of this distribution
     *         (largest 0-quantile for {@code p = 0})
     * @throws OutOfRangeException
     *         if {@code p < 0} or {@code p > 1}
     */
    double inverseCumulativeProbability(double p);

    /**
     * Use this method to get the numerical value of the mean of this
     * distribution.
     * 
     * @return the mean or {@code Double.NaN} if it is not defined
     */
    double getNumericalMean();

    /**
     * Use this method to get the numerical value of the variance of this
     * distribution.
     * 
     * @return the variance (possibly {@code Double.POSITIVE_INFINITY} as
     *         for certain cases in {@link TDistribution}) or {@code Double.NaN} if it
     *         is not defined
     */
    double getNumericalVariance();

    /**
     * Access the lower bound of the support. This method must return the same
     * value as {@code inverseCumulativeProbability(0)}. In other words, this
     * method must return
     * <p>
     * <code>inf {x in R | P(X <= x) > 0}</code>.
     * </p>
     * 
     * @return lower bound of the support (might be {@code Double.NEGATIVE_INFINITY})
     */
    double getSupportLowerBound();

    /**
     * Access the upper bound of the support. This method must return the same
     * value as {@code inverseCumulativeProbability(1)}. In other words, this
     * method must return
     * <p>
     * <code>inf {x in R | P(X <= x) = 1}</code>.
     * </p>
     * 
     * @return upper bound of the support (might be {@code Double.POSITIVE_INFINITY})
     */
    double getSupportUpperBound();

    /**
     * Use this method to get information about whether the support is connected,
     * i.e. whether all values between the lower and upper bound of the support
     * are included in the support.
     * 
     * @return whether the support is connected or not
     */
    boolean isSupportConnected();

    /**
     * Reseed the random generator used to generate samples.
     * 
     * @param seed
     *        the new seed
     */
    void reseedRandomGenerator(long seed);

    /**
     * Generate a random value sampled from this distribution.
     * 
     * @return a random value.
     */
    double sample();

    /**
     * Generate a random sample from the distribution.
     * 
     * @param sampleSize
     *        the number of random values to generate
     * @return an array representing the random sample
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if {@code sampleSize} is not positive
     */
    double[] sample(int sampleSize);
}
