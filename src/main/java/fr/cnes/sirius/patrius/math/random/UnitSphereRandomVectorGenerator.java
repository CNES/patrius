/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.random;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Generate random vectors isotropically located on the surface of a sphere.
 * 
 * @since 2.1
 * @version $Id: UnitSphereRandomVectorGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class UnitSphereRandomVectorGenerator
    implements RandomVectorGenerator {
    /**
     * RNG used for generating the individual components of the vectors.
     */
    private final RandomGenerator rand;
    /**
     * Space dimension.
     */
    private final int dimension;

    /**
     * @param dimensionIn
     *        Space dimension.
     * @param randIn
     *        RNG for the individual components of the vectors.
     */
    public UnitSphereRandomVectorGenerator(final int dimensionIn,
        final RandomGenerator randIn) {
        this.dimension = dimensionIn;
        this.rand = randIn;
    }

    /**
     * Create an object that will use a default RNG ({@link MersenneTwister}),
     * in order to generate the individual components.
     * 
     * @param dimensionIn
     *        Space dimension.
     */
    public UnitSphereRandomVectorGenerator(final int dimensionIn) {
        this(dimensionIn, new MersenneTwister());
    }

    /** {@inheritDoc} */
    @Override
    public double[] nextVector() {

        // Initialization
        final double[] v = new double[this.dimension];

        // Loop on vector dimension
        double normSq;
        do {
            normSq = 0;
            for (int i = 0; i < this.dimension; i++) {
                final double comp = 2 * this.rand.nextDouble() - 1;
                v[i] = comp;
                normSq += comp * comp;
            }
        } while (normSq > 1);

        // Normalize vector
        final double f = 1 / MathLib.sqrt(normSq);
        for (int i = 0; i < this.dimension; i++) {
            v[i] *= f;
        }

        // Return result
        return v;

    }

}
