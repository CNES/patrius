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
package fr.cnes.sirius.patrius.math.genetics;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

/**
 * Stops after a fixed number of generations. Each time {@link #isSatisfied(Population)} is invoked, a generation
 * counter is incremented. Once the counter reaches the configured <code>maxGenerations</code> value,
 * {@link #isSatisfied(Population)} returns true.
 * 
 * @version $Id: FixedGenerationCount.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class FixedGenerationCount implements StoppingCondition {
    /** Number of generations that have passed */
    private int numGenerations = 0;

    /** Maximum number of generations (stopping criteria) */
    private final int maxGenerations;

    /**
     * Create a new FixedGenerationCount instance.
     * 
     * @param maxGenerationsIn
     *        number of generations to evolve
     * @throws NumberIsTooSmallException
     *         if the number of generations is &lt; 1
     */
    public FixedGenerationCount(final int maxGenerationsIn) {
        if (maxGenerationsIn <= 0) {
            throw new NumberIsTooSmallException(maxGenerationsIn, 1, true);
        }
        this.maxGenerations = maxGenerationsIn;
    }

    /**
     * Determine whether or not the given number of generations have passed. Increments the number of generations
     * counter if the maximum has not been reached.
     * 
     * @param population
     *        ignored (no impact on result)
     * @return <code>true</code> IFF the maximum number of generations has been exceeded
     */
    @Override
    public boolean isSatisfied(final Population population) {
        if (this.numGenerations < this.maxGenerations) {
            this.numGenerations++;
            return false;
        }
        return true;
    }

    /**
     * Returns the number of generations that have already passed.
     * 
     * @return the number of generations that have passed
     */
    public int getNumGenerations() {
        return this.numGenerations;
    }

}
