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
 * VERSION:4.5:DM:DM-2253:27/05/2020:Implementation classe distribution de melange de Gaussiennes 
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.distribution;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.Pair;

/**
 *
 **/
/**
 * Class that implements a mixture of Gaussian ditributions.
 */

public class MixtureMultivariateNormalDistribution
    extends MixtureMultivariateRealDistribution<MultivariateNormalDistribution> {

    /**
     * Creates a multivariate normal mixture distribution.
     * <p>
     * <b>Note:</b> this constructor will implicitly create an instance of
     * {@link Well19937c} as random generator to be used for sampling only
     * (see {@link #sample()} and {@link #sample(int)}). In case no sampling is needed for the created distribution, it
     * is advised to pass {@code null} as random generator via the appropriate constructors to avoid the additional
     * initialisation overhead.
     *
     * @param weights Weights of each component.
     * @param means Mean vector for each component.
     * @param covariances Covariance matrix for each component.
     */
    public MixtureMultivariateNormalDistribution(final double[] weights, final double[][] means,
                                                 final double[][][] covariances) {
        this(createComponents(weights, means, covariances));
    }

    /**
     * Creates a mixture model from a list of distributions and their
     * associated weights.
     * <p>
     * <b>Note:</b> this constructor will implicitly create an instance of
     * {@link Well19937c} as random generator to be used for sampling only
     * (see {@link #sample()} and {@link #sample(int)}). In case no sampling is needed for the created distribution, it
     * is advised to pass {@code null} as random generator via the appropriate constructors to avoid the additional
     * initialisation overhead.
     *
     * @param components List of (weight, distribution) pairs from which to sample.
     */
    public MixtureMultivariateNormalDistribution(final List<Pair<Double, MultivariateNormalDistribution>> components) {
        super(components);
    }

    /**
     * Creates a mixture model from a list of distributions and their
     * associated weights.
     *
     * @param rng Random number generator.
     * @param components Distributions from which to sample.
     * @throws NotPositiveException if any of the weights is negative.
     * @throws DimensionMismatchException if not all components have the same
     *         number of variables.
     */
    public MixtureMultivariateNormalDistribution(final RandomGenerator rng,
                                                 final List<Pair<Double, MultivariateNormalDistribution>> components)
        throws NotPositiveException, DimensionMismatchException {
        super(rng, components);
    }

    /**
     * Creates a mixture of Gaussian distributions.
     * 
     * @param weights Weights of each component.
     * @param means Mean vector for each component.
     * @param covariances Covariance matrix for each component.
     * @return the mixture distribution.
     */
    public static List<Pair<Double, MultivariateNormalDistribution>> createComponents(final double[] weights,
            final double[][] means, final double[][][] covariances) {
        final List<Pair<Double, MultivariateNormalDistribution>> mvns = new ArrayList<>(weights.length);

        for (int i = 0; i < weights.length; i++) {
            final MultivariateNormalDistribution dist = new MultivariateNormalDistribution(means[i], covariances[i]);

            mvns.add(new Pair<>(weights[i], dist));
        }

        return mvns;
    }
}
