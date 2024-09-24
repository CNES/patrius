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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class FitnessCachingTest {

    // parameters for the GA
    private static final int DIMENSION = 50;
    private static final double CROSSOVER_RATE = 1;
    private static final double MUTATION_RATE = 0.1;
    private static final int TOURNAMENT_ARITY = 5;

    private static final int POPULATION_SIZE = 10;
    private static final int NUM_GENERATIONS = 50;
    private static final double ELITISM_RATE = 0.2;

    // how many times was the fitness computed
    private static int fitnessCalls = 0;

    @Test
    public void testFitnessCaching() {
        // initialize a new genetic algorithm
        final GeneticAlgorithm ga = new GeneticAlgorithm(
            new OnePointCrossover<Integer>(),
            CROSSOVER_RATE, // all selected chromosomes will be recombined (=crosssover)
            new BinaryMutation(),
            MUTATION_RATE, // no mutation
            new TournamentSelection(TOURNAMENT_ARITY)
            );

        // initial population
        final Population initial = randomPopulation();
        // stopping conditions
        final StoppingCondition stopCond = new FixedGenerationCount(NUM_GENERATIONS);

        // run the algorithm
        ga.evolve(initial, stopCond);

        final int neededCalls =
            POPULATION_SIZE /* initial population */+
                (NUM_GENERATIONS - 1) /* for each population */* (int) (POPULATION_SIZE * (1.0 - ELITISM_RATE)) /*
                                                                                                                 * some
                                                                                                                 * chromosomes
                                                                                                                 * are
                                                                                                                 * copied
                                                                                                                 */
        ;
        Assert.assertTrue(fitnessCalls <= neededCalls); // some chromosomes after crossover may be the same os old ones
    }

    /**
     * Initializes a random population.
     */
    private static ElitisticListPopulation randomPopulation() {
        final List<Chromosome> popList = new LinkedList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            final BinaryChromosome randChrom = new DummyCountingBinaryChromosome(
                BinaryChromosome.randomBinaryRepresentation(DIMENSION));
            popList.add(randChrom);
        }
        return new ElitisticListPopulation(popList, popList.size(), ELITISM_RATE);
    }

    private static class DummyCountingBinaryChromosome extends DummyBinaryChromosome {

        public DummyCountingBinaryChromosome(final List<Integer> representation) {
            super(representation);
        }

        @Override
        public double fitness() {
            fitnessCalls++;
            return 0;
        }
    }
}
