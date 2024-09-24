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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Population of chromosomes represented by a {@link List}.
 * 
 * @since 2.0
 * @version $Id: ListPopulation.java 18108 2017-10-04 06:45:27Z bignon $
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class ListPopulation implements Population {
    // CHECKSTYLE: resume AbstractClassName check

    /** List of chromosomes */
    private final List<Chromosome> chromosomes;

    /** maximal size of the population */
    private int populationLimit;

    /**
     * Creates a new ListPopulation instance and initializes its inner chromosome list.
     * 
     * @param populationLimitIn
     *        maximal size of the population
     * @throws NotPositiveException
     *         if the population limit is not a positive number (&lt; 1)
     */
    public ListPopulation(final int populationLimitIn) {
        this(Collections.<Chromosome>emptyList(), populationLimitIn);
    }

    /**
     * Creates a new ListPopulation instance.
     * <p>
     * Note: the chromosomes of the specified list are added to the population.
     * 
     * @param chromosomesIn
     *        list of chromosomes to be added to the population
     * @param populationLimitIn
     *        maximal size of the population
     * @throws NullArgumentException
     *         if the list of chromosomes is {@code null}
     * @throws NotPositiveException
     *         if the population limit is not a positive number (&lt; 1)
     * @throws NumberIsTooLargeException
     *         if the list of chromosomes exceeds the population limit
     */
    public ListPopulation(final List<Chromosome> chromosomesIn, final int populationLimitIn) {

        if (chromosomesIn == null) {
            throw new NullArgumentException();
        }
        if (populationLimitIn <= 0) {
            throw new NotPositiveException(PatriusMessages.POPULATION_LIMIT_NOT_POSITIVE, populationLimitIn);
        }
        if (chromosomesIn.size() > populationLimitIn) {
            throw new NumberIsTooLargeException(PatriusMessages.LIST_OF_CHROMOSOMES_BIGGER_THAN_POPULATION_SIZE,
                chromosomesIn.size(), populationLimitIn, false);
        }
        this.populationLimit = populationLimitIn;
        this.chromosomes = new ArrayList<>(populationLimitIn);
        this.chromosomes.addAll(chromosomesIn);
    }

    /**
     * Add a {@link Collection} of chromosomes to this {@link Population}.
     * 
     * @param chromosomeColl
     *        a {@link Collection} of chromosomes
     * @throws NumberIsTooLargeException
     *         if the population would exceed the population limit when
     *         adding this chromosome
     * @since 3.1
     */
    public void addChromosomes(final Collection<Chromosome> chromosomeColl) {
        if (this.chromosomes.size() + chromosomeColl.size() > this.populationLimit) {
            throw new NumberIsTooLargeException(PatriusMessages.LIST_OF_CHROMOSOMES_BIGGER_THAN_POPULATION_SIZE,
                this.chromosomes.size(), this.populationLimit, false);
        }
        this.chromosomes.addAll(chromosomeColl);
    }

    /**
     * Returns an unmodifiable list of the chromosomes in this population.
     * 
     * @return the unmodifiable list of chromosomes
     */
    public List<Chromosome> getChromosomes() {
        return Collections.unmodifiableList(this.chromosomes);
    }

    /**
     * Access the list of chromosomes.
     * 
     * @return the list of chromosomes
     * @since 3.1
     */
    protected List<Chromosome> getChromosomeList() {
        return this.chromosomes;
    }

    /**
     * Add the given chromosome to the population.
     * 
     * @param chromosome
     *        the chromosome to add.
     * @throws NumberIsTooLargeException
     *         if the population would exceed the {@code populationLimit} after
     *         adding this chromosome
     */
    @Override
    public void addChromosome(final Chromosome chromosome) {
        if (this.chromosomes.size() >= this.populationLimit) {
            throw new NumberIsTooLargeException(PatriusMessages.LIST_OF_CHROMOSOMES_BIGGER_THAN_POPULATION_SIZE,
                this.chromosomes.size(), this.populationLimit, false);
        }
        this.chromosomes.add(chromosome);
    }

    /**
     * Access the fittest chromosome in this population.
     * 
     * @return the fittest chromosome.
     */
    @Override
    public Chromosome getFittestChromosome() {
        // best so far
        Chromosome bestChromosome = this.chromosomes.get(0);
        for (final Chromosome chromosome : this.chromosomes) {
            if (chromosome.compareTo(bestChromosome) > 0) {
                // better chromosome found
                bestChromosome = chromosome;
            }
        }
        return bestChromosome;
    }

    /**
     * Access the maximum population size.
     * 
     * @return the maximum population size.
     */
    @Override
    public int getPopulationLimit() {
        return this.populationLimit;
    }

    /**
     * Sets the maximal population size.
     * 
     * @param populationLimitIn
     *        maximal population size.
     * @throws NotPositiveException
     *         if the population limit is not a positive number (&lt; 1)
     * @throws NumberIsTooSmallException
     *         if the new population size is smaller than the current number
     *         of chromosomes in the population
     */
    public void setPopulationLimit(final int populationLimitIn) {
        if (populationLimitIn <= 0) {
            throw new NotPositiveException(PatriusMessages.POPULATION_LIMIT_NOT_POSITIVE, populationLimitIn);
        }
        if (populationLimitIn < this.chromosomes.size()) {
            throw new NumberIsTooSmallException(populationLimitIn, this.chromosomes.size(), true);
        }
        this.populationLimit = populationLimitIn;
    }

    /**
     * Access the current population size.
     * 
     * @return the current population size.
     */
    @Override
    public int getPopulationSize() {
        return this.chromosomes.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.chromosomes.toString();
    }

    /**
     * Returns an iterator over the unmodifiable list of chromosomes.
     * <p>
     * Any call to {@link Iterator#remove()} will result in a {@link UnsupportedOperationException}.
     * </p>
     * 
     * @return chromosome iterator
     */
    @Override
    public Iterator<Chromosome> iterator() {
        return this.getChromosomes().iterator();
    }
}
