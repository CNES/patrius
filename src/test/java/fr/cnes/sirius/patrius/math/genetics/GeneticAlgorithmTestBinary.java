/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * This is also an example of usage.
 */
public class GeneticAlgorithmTestBinary {

    // parameters for the GA
    private static final int DIMENSION = 50;
    private static final int POPULATION_SIZE = 50;
    private static final int NUM_GENERATIONS = 50;
    private static final double ELITISM_RATE = 0.2;
    private static final double CROSSOVER_RATE = 1;
    private static final double MUTATION_RATE = 0.1;
    private static final int TOURNAMENT_ARITY = 2;

    @Test
    public void test() {
        // to test a stochastic algorithm is hard, so this will rather be an usage example

        // initialize a new genetic algorithm
        final GeneticAlgorithm ga = new GeneticAlgorithm(
            new OnePointCrossover<Integer>(),
            CROSSOVER_RATE, // all selected chromosomes will be recombined (=crosssover)
            new BinaryMutation(),
            MUTATION_RATE,
            new TournamentSelection(TOURNAMENT_ARITY)
            );

        Assert.assertEquals(0, ga.getGenerationsEvolved());

        // initial population
        final Population initial = randomPopulation();
        // stopping conditions
        final StoppingCondition stopCond = new FixedGenerationCount(NUM_GENERATIONS);

        // best initial chromosome
        final Chromosome bestInitial = initial.getFittestChromosome();

        // run the algorithm
        final Population finalPopulation = ga.evolve(initial, stopCond);

        // best chromosome from the final population
        final Chromosome bestFinal = finalPopulation.getFittestChromosome();

        // the only thing we can test is whether the final solution is not worse than the initial one
        // however, for some implementations of GA, this need not be true :)

        Assert.assertTrue(bestFinal.compareTo(bestInitial) > 0);
        Assert.assertEquals(NUM_GENERATIONS, ga.getGenerationsEvolved());

    }

    /**
     * Initializes a random population.
     */
    private static ElitisticListPopulation randomPopulation() {
        final List<Chromosome> popList = new LinkedList<Chromosome>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            final BinaryChromosome randChrom = new FindOnes(BinaryChromosome.randomBinaryRepresentation(DIMENSION));
            popList.add(randChrom);
        }
        return new ElitisticListPopulation(popList, popList.size(), ELITISM_RATE);
    }

    /**
     * Chromosomes represented by a binary chromosome.
     * 
     * The goal is to set all bits (genes) to 1.
     */
    private static class FindOnes extends BinaryChromosome {

        public FindOnes(final List<Integer> representation) {
            super(representation);
        }

        /**
         * Returns number of elements != 0
         */
        @Override
        public double fitness() {
            int num = 0;
            for (final int val : this.getRepresentation()) {
                if (val != 0) {
                    num++;
                }
            }
            // number of elements >= 0
            return num;
        }

        @Override
        public AbstractListChromosome<Integer> newFixedLengthChromosome(final List<Integer> chromosomeRepresentation) {
            return new FindOnes(chromosomeRepresentation);
        }

    }
}
