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

/**
 * Base interface for multivariate distributions on the reals.
 * 
 * This is based largely on the RealDistribution interface, but cumulative
 * distribution functions are not required because they are often quite
 * difficult to compute for multivariate distributions.
 * 
 * @version $Id: MultivariateRealDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public interface MultivariateRealDistribution {
    /**
     * Returns the probability density function (PDF) of this distribution
     * evaluated at the specified point {@code x}. In general, the PDF is the
     * derivative of the cumulative distribution function. If the derivative
     * does not exist at {@code x}, then an appropriate replacement should be
     * returned, e.g. {@code Double.POSITIVE_INFINITY}, {@code Double.NaN}, or
     * the limit inferior or limit superior of the difference quotient.
     * 
     * @param x
     *        Point at which the PDF is evaluated.
     * @return the value of the probability density function at point {@code x}.
     */
    double density(double[] x);

    /**
     * Reseeds the random generator used to generate samples.
     * 
     * @param seed
     *        Seed with which to initialize the random number generator.
     */
    void reseedRandomGenerator(long seed);

    /**
     * Gets the number of random variables of the distribution.
     * It is the size of the array returned by the {@link #sample() sample} method.
     * 
     * @return the number of variables.
     */
    int getDimension();

    /**
     * Generates a random value vector sampled from this distribution.
     * 
     * @return a random value vector.
     */
    double[] sample();

    /**
     * Generates a list of a random value vectors from the distribution.
     * 
     * @param sampleSize
     *        the number of random vectors to generate.
     * @return an array representing the random samples.
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if {@code sampleSize} is not positive.
     * 
     * @see #sample()
     */
    double[][] sample(int sampleSize);
}
