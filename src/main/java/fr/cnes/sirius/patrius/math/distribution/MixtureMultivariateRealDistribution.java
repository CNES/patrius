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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class for representing <a href="http://en.wikipedia.org/wiki/Mixture_model">
 * mixture model</a> distributions.
 * 
 * @param <T>
 *        Type of the mixture components.
 * 
 * @version $Id: MixtureMultivariateRealDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class MixtureMultivariateRealDistribution<T extends MultivariateRealDistribution>
    extends AbstractMultivariateRealDistribution {
    /** Normalized weight of each mixture component. */
    private final double[] weight;
    /** Mixture components. */
    private final List<T> distribution;

    /**
     * Creates a mixture model from a list of distributions and their
     * associated weights.
     * 
     * @param components
     *        List of (weight, distribution) pairs from which to sample.
     */
    public MixtureMultivariateRealDistribution(final List<Pair<Double, T>> components) {
        this(new Well19937c(), components);
    }

    /**
     * Creates a mixture model from a list of distributions and their
     * associated weights.
     * 
     * @param rng
     *        Random number generator.
     * @param components
     *        Distributions from which to sample.
     * @throws NotPositiveException
     *         if any of the weights is negative.
     * @throws DimensionMismatchException
     *         if not all components have the same
     *         number of variables.
     */
    public MixtureMultivariateRealDistribution(final RandomGenerator rng,
        final List<Pair<Double, T>> components) {
        super(rng, components.get(0).getSecond().getDimension());

        final int numComp = components.size();
        final int dim = this.getDimension();
        double weightSum = 0;
        for (int i = 0; i < numComp; i++) {
            final Pair<Double, T> comp = components.get(i);
            if (comp.getSecond().getDimension() != dim) {
                throw new DimensionMismatchException(comp.getSecond().getDimension(), dim);
            }
            if (comp.getFirst() < 0) {
                throw new NotPositiveException(comp.getFirst());
            }
            weightSum += comp.getFirst();
        }

        // Check for overflow.
        if (Double.isInfinite(weightSum)) {
            throw new MathArithmeticException(PatriusMessages.OVERFLOW);
        }

        // Store each distribution and its normalized weight.
        this.distribution = new ArrayList<T>();
        this.weight = new double[numComp];
        for (int i = 0; i < numComp; i++) {
            final Pair<Double, T> comp = components.get(i);
            this.weight[i] = comp.getFirst() / weightSum;
            this.distribution.add(comp.getSecond());
        }
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double[] values) {
        double p = 0;
        for (int i = 0; i < this.weight.length; i++) {
            p += this.weight[i] * this.distribution.get(i).density(values);
        }
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public double[] sample() {
        // Sampled values.
        double[] vals = null;

        // Determine which component to sample from.
        final double randomValue = this.random.nextDouble();
        double sum = 0;

        for (int i = 0; i < this.weight.length; i++) {
            sum += this.weight[i];
            if (randomValue <= sum) {
                // pick model i
                vals = this.distribution.get(i).sample();
                break;
            }
        }

        if (vals == null) {
            // This should never happen, but it ensures we won't return a null in
            // case the loop above has some floating point inequality problem on
            // the final iteration.
            vals = this.distribution.get(this.weight.length - 1).sample();
        }

        return vals;
    }

    /** {@inheritDoc} */
    @Override
    public void reseedRandomGenerator(final long seed) {
        // Seed needs to be propagated to underlying components
        // in order to maintain consistency between runs.
        super.reseedRandomGenerator(seed);

        for (int i = 0; i < this.distribution.size(); i++) {
            // Make each component's seed different in order to avoid
            // using the same sequence of random numbers.
            this.distribution.get(i).reseedRandomGenerator(i + 1 + seed);
        }
    }

    /**
     * Gets the distributions that make up the mixture model.
     * 
     * @return the component distributions and associated weights.
     */
    public List<Pair<Double, T>> getComponents() {
        final List<Pair<Double, T>> list = new ArrayList<Pair<Double, T>>();

        for (int i = 0; i < this.weight.length; i++) {
            list.add(new Pair<Double, T>(this.weight[i], this.distribution.get(i)));
        }

        return list;
    }
}
