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
package fr.cnes.sirius.patrius.math.random;

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * A {@link RandomVectorGenerator} that generates vectors with uncorrelated
 * components. Components of generated vectors follow (independent) Gaussian
 * distributions, with parameters supplied in the constructor.
 * 
 * @version $Id: UncorrelatedRandomVectorGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public class UncorrelatedRandomVectorGenerator
    implements RandomVectorGenerator {

    /** Underlying scalar generator. */
    private final NormalizedRandomGenerator generator;

    /** Mean vector. */
    private final double[] mean;

    /** Standard deviation vector. */
    private final double[] standardDeviation;

    /**
     * Simple constructor.
     * <p>
     * Build an uncorrelated random vector generator from its mean and standard deviation vectors.
     * </p>
     * 
     * @param meanIn
     *        expected mean values for each component
     * @param standardDeviationIn
     *        standard deviation for each component
     * @param generatorIn
     *        underlying generator for uncorrelated normalized
     *        components
     */
    public UncorrelatedRandomVectorGenerator(final double[] meanIn,
        final double[] standardDeviationIn,
        final NormalizedRandomGenerator generatorIn) {
        if (meanIn.length != standardDeviationIn.length) {
            throw new DimensionMismatchException(meanIn.length, standardDeviationIn.length);
        }
        this.mean = meanIn.clone();
        this.standardDeviation = standardDeviationIn.clone();
        this.generator = generatorIn;
    }

    /**
     * Simple constructor.
     * <p>
     * Build a null mean random and unit standard deviation uncorrelated vector generator
     * </p>
     * 
     * @param dimension
     *        dimension of the vectors to generate
     * @param generatorIn
     *        underlying generator for uncorrelated normalized
     *        components
     */
    public UncorrelatedRandomVectorGenerator(final int dimension,
        final NormalizedRandomGenerator generatorIn) {
        this.mean = new double[dimension];
        this.standardDeviation = new double[dimension];
        Arrays.fill(this.standardDeviation, 1.0);
        this.generator = generatorIn;
    }

    /**
     * Generate an uncorrelated random vector.
     * 
     * @return a random vector as a newly built array of double
     */
    @Override
    public double[] nextVector() {

        final double[] random = new double[this.mean.length];
        for (int i = 0; i < random.length; ++i) {
            random[i] = this.mean[i] + this.standardDeviation[i] * this.generator.nextNormalizedDouble();
        }

        return random;

    }

}
