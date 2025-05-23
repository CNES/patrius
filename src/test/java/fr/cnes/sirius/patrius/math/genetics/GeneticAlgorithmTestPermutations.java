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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This is also an example of usage.
 * 
 * This algorithm does "stochastic sorting" of a sequence 0,...,N.
 * 
 */
public class GeneticAlgorithmTestPermutations {

    // parameters for the GA
    private static final int DIMENSION = 20;
    private static final int POPULATION_SIZE = 80;
    private static final int NUM_GENERATIONS = 200;
    private static final double ELITISM_RATE = 0.2;
    private static final double CROSSOVER_RATE = 1;
    private static final double MUTATION_RATE = 0.08;
    private static final int TOURNAMENT_ARITY = 2;

    // numbers from 0 to N-1
    private static final List<Integer> sequence = new ArrayList<>();
    static {
        for (int i = 0; i < DIMENSION; i++) {
            sequence.add(i);
        }
    }

    @Test
    public void test() {
        // to test a stochastic algorithm is hard, so this will rather be an usage example

        // initialize a new genetic algorithm
        final GeneticAlgorithm ga = new GeneticAlgorithm(
            new OnePointCrossover<Integer>(),
            CROSSOVER_RATE,
            new RandomKeyMutation(),
            MUTATION_RATE,
            new TournamentSelection(TOURNAMENT_ARITY)
            );

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

        // System.out.println(bestInitial);
        // System.out.println(bestFinal);
    }

    /**
     * Initializes a random population
     */
    private static ElitisticListPopulation randomPopulation() {
        final List<Chromosome> popList = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            final Chromosome randChrom = new MinPermutations(RandomKey.randomPermutation(DIMENSION));
            popList.add(randChrom);
        }
        return new ElitisticListPopulation(popList, popList.size(), ELITISM_RATE);
    }

    /**
     * Chromosomes representing a permutation of (0,1,2,...,DIMENSION-1).
     * 
     * The goal is to sort the sequence.
     */
    private static class MinPermutations extends RandomKey<Integer> {

        public MinPermutations(final List<Double> representation) {
            super(representation);
        }

        @Override
        public double fitness() {
            int res = 0;
            final List<Integer> decoded = this.decode(sequence);
            for (int i = 0; i < decoded.size(); i++) {
                final int value = decoded.get(i);
                if (value != i) {
                    // bad position found
                    res += MathLib.abs(value - i);
                }
            }
            // the most fitted chromosome is the one with minimal error
            // therefore we must return negative value
            return -res;
        }

        @Override
        public AbstractListChromosome<Double> newFixedLengthChromosome(final List<Double> chromosomeRepresentation) {
            return new MinPermutations(chromosomeRepresentation);
        }
    }
}
