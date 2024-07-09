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
package fr.cnes.sirius.patrius.math.genetics;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;

/**
 * A collection of chromosomes that facilitates generational evolution.
 * 
 * @since 2.0
 * @version $Id: Population.java 18108 2017-10-04 06:45:27Z bignon $
 */
public interface Population extends Iterable<Chromosome> {
    /**
     * Access the current population size.
     * 
     * @return the current population size.
     */
    int getPopulationSize();

    /**
     * Access the maximum population size.
     * 
     * @return the maximum population size.
     */
    int getPopulationLimit();

    /**
     * Start the population for the next generation.
     * 
     * @return the beginnings of the next generation.
     */
    Population nextGeneration();

    /**
     * Add the given chromosome to the population.
     * 
     * @param chromosome
     *        the chromosome to add.
     * @throws NumberIsTooLargeException
     *         if the population would exceed the population limit when adding
     *         this chromosome
     */
    void addChromosome(Chromosome chromosome);

    /**
     * Access the fittest chromosome in this population.
     * 
     * @return the fittest chromosome.
     */
    Chromosome getFittestChromosome();
}
