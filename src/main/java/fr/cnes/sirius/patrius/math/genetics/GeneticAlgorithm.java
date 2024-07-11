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
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of a genetic algorithm. All factors that govern the operation
 * of the algorithm can be configured for a specific problem.
 * 
 * @since 2.0
 * @version $Id: GeneticAlgorithm.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GeneticAlgorithm {

    /**
     * Static random number generator shared by GA implementation classes. Set the randomGenerator seed to get
     * reproducible results. Use {@link #setRandomGenerator(RandomGenerator)} to supply an alternative to the default
     * JDK-provided PRNG.
     */
    // @GuardedBy("this")
    private static RandomGenerator randomGenerator = new JDKRandomGenerator();

    /** the crossover policy used by the algorithm. */
    private final CrossoverPolicy crossoverPolicy;

    /** the rate of crossover for the algorithm. */
    private final double crossoverRate;

    /** the mutation policy used by the algorithm. */
    private final MutationPolicy mutationPolicy;

    /** the rate of mutation for the algorithm. */
    private final double mutationRate;

    /** the selection policy used by the algorithm. */
    private final SelectionPolicy selectionPolicy;

    /** the number of generations evolved to reach {@link StoppingCondition} in the last run. */
    private int generationsEvolved = 0;

    /**
     * Create a new genetic algorithm.
     * 
     * @param crossoverPolicyIn
     *        The {@link CrossoverPolicy}
     * @param crossoverRateIn
     *        The crossover rate as a percentage (0-1 inclusive)
     * @param mutationPolicyIn
     *        The {@link MutationPolicy}
     * @param mutationRateIn
     *        The mutation rate as a percentage (0-1 inclusive)
     * @param selectionPolicyIn
     *        The {@link SelectionPolicy}
     * @throws OutOfRangeException
     *         if the crossover or mutation rate is outside the [0, 1] range
     */
    public GeneticAlgorithm(final CrossoverPolicy crossoverPolicyIn,
        final double crossoverRateIn,
        final MutationPolicy mutationPolicyIn,
        final double mutationRateIn,
        final SelectionPolicy selectionPolicyIn) {

        if (crossoverRateIn < 0 || crossoverRateIn > 1) {
            throw new OutOfRangeException(PatriusMessages.CROSSOVER_RATE,
                crossoverRateIn, 0, 1);
        }
        if (mutationRateIn < 0 || mutationRateIn > 1) {
            throw new OutOfRangeException(PatriusMessages.MUTATION_RATE,
                mutationRateIn, 0, 1);
        }
        this.crossoverPolicy = crossoverPolicyIn;
        this.crossoverRate = crossoverRateIn;
        this.mutationPolicy = mutationPolicyIn;
        this.mutationRate = mutationRateIn;
        this.selectionPolicy = selectionPolicyIn;
    }

    /**
     * Set the (static) random generator.
     * 
     * @param random
     *        random generator
     */
    public static synchronized void setRandomGenerator(final RandomGenerator random) {
        randomGenerator = random;
    }

    /**
     * Returns the (static) random generator.
     * 
     * @return the static random generator shared by GA implementation classes
     */
    public static synchronized RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    /**
     * Evolve the given population. Evolution stops when the stopping condition
     * is satisfied. Updates the {@link #getGenerationsEvolved() generationsEvolved} property with the number of
     * generations evolved before the StoppingCondition
     * is satisfied.
     * 
     * @param initial
     *        the initial, seed population.
     * @param condition
     *        the stopping condition used to stop evolution.
     * @return the population that satisfies the stopping condition.
     */
    public Population evolve(final Population initial, final StoppingCondition condition) {
        Population current = initial;
        this.generationsEvolved = 0;
        while (!condition.isSatisfied(current)) {
            current = this.nextGeneration(current);
            this.generationsEvolved++;
        }
        return current;
    }

    /**
     * Evolve the given population into the next generation.
     * <ol>
     * <li>Get nextGeneration population to fill from <code>current</code> generation, using its 
     * nextGeneration method</li>
     * <li>Loop until new generation is filled:</li>
     * <ul>
     * <li>Apply configured SelectionPolicy to select a pair of parents from <code>current</code></li>
     * <li>With probability = {@link #getCrossoverRate()}, apply configured {@link CrossoverPolicy} to parents</li>
     * <li>With probability = {@link #getMutationRate()}, apply configured {@link MutationPolicy} to each of the
     * offspring</li>
     * <li>Add offspring individually to nextGeneration, space permitting</li>
     * </ul>
     * <li>Return nextGeneration</li>
     * </ol>
     * 
     * @param current
     *        the current population.
     * @return the population for the next generation.
     */
    public Population nextGeneration(final Population current) {
        final Population nextGeneration = current.nextGeneration();

        final RandomGenerator randGen = getRandomGenerator();

        while (nextGeneration.getPopulationSize() < nextGeneration.getPopulationLimit()) {
            // select parent chromosomes
            ChromosomePair pair = this.getSelectionPolicy().select(current);

            // crossover?
            if (randGen.nextDouble() < this.getCrossoverRate()) {
                // apply crossover policy to create two offspring
                pair = this.getCrossoverPolicy().crossover(pair.getFirst(), pair.getSecond());
            }

            // mutation?
            if (randGen.nextDouble() < this.getMutationRate()) {
                // apply mutation policy to the chromosomes
                pair = new ChromosomePair(
                    this.getMutationPolicy().mutate(pair.getFirst()),
                    this.getMutationPolicy().mutate(pair.getSecond()));
            }

            // add the first chromosome to the population
            nextGeneration.addChromosome(pair.getFirst());
            // is there still a place for the second chromosome?
            if (nextGeneration.getPopulationSize() < nextGeneration.getPopulationLimit()) {
                // add the second chromosome to the population
                nextGeneration.addChromosome(pair.getSecond());
            }
        }

        return nextGeneration;
    }

    /**
     * Returns the crossover policy.
     * 
     * @return crossover policy
     */
    public CrossoverPolicy getCrossoverPolicy() {
        return this.crossoverPolicy;
    }

    /**
     * Returns the crossover rate.
     * 
     * @return crossover rate
     */
    public double getCrossoverRate() {
        return this.crossoverRate;
    }

    /**
     * Returns the mutation policy.
     * 
     * @return mutation policy
     */
    public MutationPolicy getMutationPolicy() {
        return this.mutationPolicy;
    }

    /**
     * Returns the mutation rate.
     * 
     * @return mutation rate
     */
    public double getMutationRate() {
        return this.mutationRate;
    }

    /**
     * Returns the selection policy.
     * 
     * @return selection policy
     */
    public SelectionPolicy getSelectionPolicy() {
        return this.selectionPolicy;
    }

    /**
     * Returns the number of generations evolved to reach {@link StoppingCondition} in the last run.
     * 
     * @return number of generations evolved
     * @since 2.1
     */
    public int getGenerationsEvolved() {
        return this.generationsEvolved;
    }

}
