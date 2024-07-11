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

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Base class for multivariate probability distributions.
 * 
 * @version $Id: AbstractMultivariateRealDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public abstract class AbstractMultivariateRealDistribution
    implements MultivariateRealDistribution {
    /** RNG instance used to generate samples from the distribution. */
    protected final RandomGenerator random;
    /** The number of dimensions or columns in the multivariate distribution. */
    private final int dimension;

    /**
     * @param rng
     *        Random number generator.
     * @param n
     *        Number of dimensions.
     */
    protected AbstractMultivariateRealDistribution(final RandomGenerator rng,
        final int n) {
        this.random = rng;
        this.dimension = n;
    }

    /** {@inheritDoc} */
    @Override
    public void reseedRandomGenerator(final long seed) {
        this.random.setSeed(seed);
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return this.dimension;
    }

    /** {@inheritDoc} */
    @Override
    public abstract double[] sample();

    /** {@inheritDoc} */
    @Override
    public double[][] sample(final int sampleSize) {
        if (sampleSize <= 0) {
            throw new NotStrictlyPositiveException(PatriusMessages.NUMBER_OF_SAMPLES,
                sampleSize);
        }
        final double[][] out = new double[sampleSize][this.dimension];
        for (int i = 0; i < sampleSize; i++) {
            out[i] = this.sample();
        }
        return out;
    }
}
