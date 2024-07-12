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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import java.util.Collections;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Population of chromosomes which uses elitism (certain percentage of the best
 * chromosomes is directly copied to the next generation).
 * 
 * @version $Id: ElitisticListPopulation.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class ElitisticListPopulation extends ListPopulation {

    /** Default percentage of chromosomes copied to the next generation. */
    private static final double DEFAULT_ELITISM_RATE = 0.9;

    /** percentage of chromosomes copied to the next generation. */
    private double elitismRate = DEFAULT_ELITISM_RATE;

    /**
     * Creates a new {@link ElitisticListPopulation} instance.
     * 
     * @param chromosomes
     *        list of chromosomes in the population
     * @param populationLimit
     *        maximal size of the population
     * @param elitismRateIn
     *        how many best chromosomes will be directly transferred to the next generation [in %]
     * @throws NullArgumentException
     *         if the list of chromosomes is {@code null}
     * @throws NotPositiveException
     *         if the population limit is not a positive number (&lt; 1)
     * @throws NumberIsTooLargeException
     *         if the list of chromosomes exceeds the population limit
     * @throws OutOfRangeException
     *         if the elitism rate is outside the [0, 1] range
     */
    public ElitisticListPopulation(final List<Chromosome> chromosomes, final int populationLimit,
        final double elitismRateIn) {

        super(chromosomes, populationLimit);
        this.setElitismRate(elitismRateIn);

    }

    /**
     * Creates a new {@link ElitisticListPopulation} instance and initializes its inner chromosome list.
     * 
     * @param populationLimit
     *        maximal size of the population
     * @param elitismRateIn
     *        how many best chromosomes will be directly transferred to the next generation [in %]
     * @throws NotPositiveException
     *         if the population limit is not a positive number (&lt; 1)
     * @throws OutOfRangeException
     *         if the elitism rate is outside the [0, 1] range
     */
    public ElitisticListPopulation(final int populationLimit, final double elitismRateIn) {

        super(populationLimit);
        this.setElitismRate(elitismRateIn);

    }

    /**
     * Start the population for the next generation. The <code>{@link #elitismRate}</code> percents of the best
     * chromosomes are directly copied to the next generation.
     * 
     * @return the beginnings of the next generation.
     */
    @Override
    public Population nextGeneration() {
        // initialize a new generation with the same parameters
        final ElitisticListPopulation nextGeneration =
            new ElitisticListPopulation(this.getPopulationLimit(), this.getElitismRate());

        final List<Chromosome> oldChromosomes = this.getChromosomeList();
        Collections.sort(oldChromosomes);

        // index of the last "not good enough" chromosome
        final int boundIndex = (int) MathLib.ceil((1.0 - this.getElitismRate()) * oldChromosomes.size());
        for (int i = boundIndex; i < oldChromosomes.size(); i++) {
            nextGeneration.addChromosome(oldChromosomes.get(i));
        }
        return nextGeneration;
    }

    /**
     * Sets the elitism rate, i.e. how many best chromosomes will be directly transferred to the next generation [in %].
     * 
     * @param elitismRateIn
     *        how many best chromosomes will be directly transferred to the next generation [in %]
     * @throws OutOfRangeException
     *         if the elitism rate is outside the [0, 1] range
     */
    public void setElitismRate(final double elitismRateIn) {
        if (elitismRateIn < 0 || elitismRateIn > 1) {
            throw new OutOfRangeException(PatriusMessages.ELITISM_RATE, elitismRateIn, 0, 1);
        }
        this.elitismRate = elitismRateIn;
    }

    /**
     * Access the elitism rate.
     * 
     * @return the elitism rate
     */
    public double getElitismRate() {
        return this.elitismRate;
    }

}
