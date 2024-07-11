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

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class implements a normalized uniform random generator.
 * <p>
 * Since it is a normalized random generator, it generates values from a uniform distribution with mean equal to 0 and
 * standard deviation equal to 1. Generated values fall in the range [-&#x0221A;3, +&#x0221A;3].
 * </p>
 * 
 * @since 1.2
 * 
 * @version $Id: UniformRandomGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class UniformRandomGenerator implements NormalizedRandomGenerator {

    /** Square root of three. */
    private static final double SQRT3 = MathLib.sqrt(3.0);

    /** Underlying generator. */
    private final RandomGenerator generator;

    /**
     * Create a new generator.
     * 
     * @param generatorIn
     *        underlying random generator to use
     */
    public UniformRandomGenerator(final RandomGenerator generatorIn) {
        this.generator = generatorIn;
    }

    /**
     * Generate a random scalar with null mean and unit standard deviation.
     * <p>
     * The number generated is uniformly distributed between -&sqrt;(3) and +&sqrt;(3).
     * </p>
     * 
     * @return a random scalar with null mean and unit standard deviation
     */
    @Override
    public double nextNormalizedDouble() {
        return SQRT3 * (2 * this.generator.nextDouble() - 1.0);
    }

}
