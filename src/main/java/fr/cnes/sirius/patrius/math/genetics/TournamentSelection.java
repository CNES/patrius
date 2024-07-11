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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Tournament selection scheme. Each of the two selected chromosomes is selected
 * based on n-ary tournament -- this is done by drawing {@link #arity} random
 * chromosomes without replacement from the population, and then selecting the
 * fittest chromosome among them.
 * 
 * @since 2.0
 * @version $Id: TournamentSelection.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class TournamentSelection implements SelectionPolicy {

    /** number of chromosomes included in the tournament selections */
    private int arity;

    /**
     * Creates a new TournamentSelection instance.
     * 
     * @param arityIn
     *        how many chromosomes will be drawn to the tournament
     */
    public TournamentSelection(final int arityIn) {
        this.arity = arityIn;
    }

    /**
     * Select two chromosomes from the population. Each of the two selected
     * chromosomes is selected based on n-ary tournament -- this is done by
     * drawing {@link #arity} random chromosomes without replacement from the
     * population, and then selecting the fittest chromosome among them.
     * 
     * @param population
     *        the population from which the chromosomes are chosen.
     * @return the selected chromosomes.
     * @throws MathIllegalArgumentException
     *         if the tournament arity is bigger than the population size
     */
    @Override
    public ChromosomePair select(final Population population) {
        return new ChromosomePair(this.tournament((ListPopulation) population),
            this.tournament((ListPopulation) population));
    }

    /**
     * Helper for {@link #select(Population)}. Draw {@link #arity} random chromosomes without replacement from the
     * population, and then select the fittest chromosome among them.
     * 
     * @param population
     *        the population from which the chromosomes are choosen.
     * @return the selected chromosome.
     * @throws MathIllegalArgumentException
     *         if the tournament arity is bigger than the population size
     */
    private Chromosome tournament(final ListPopulation population) {
        if (population.getPopulationSize() < this.arity) {
            throw new MathIllegalArgumentException(PatriusMessages.TOO_LARGE_TOURNAMENT_ARITY,
                this.arity, population.getPopulationSize());
        }
        // auxiliary population
        final ListPopulation tournamentPopulation = new ListPopulation(this.arity){
            /** {@inheritDoc} */
            @Override
            public Population nextGeneration() {
                // not useful here
                return null;
            }
        };

        // create a copy of the chromosome list
        final List<Chromosome> chromosomes = new ArrayList<Chromosome>(population.getChromosomes());
        for (int i = 0; i < this.arity; i++) {
            // select a random individual and add it to the tournament
            final int rind = GeneticAlgorithm.getRandomGenerator().nextInt(chromosomes.size());
            tournamentPopulation.addChromosome(chromosomes.get(rind));
            // do not select it again
            chromosomes.remove(rind);
        }
        // the winner takes it all
        return tournamentPopulation.getFittestChromosome();
    }

    /**
     * Gets the arity (number of chromosomes drawn to the tournament).
     * 
     * @return arity of the tournament
     */
    public int getArity() {
        return this.arity;
    }

    /**
     * Sets the arity (number of chromosomes drawn to the tournament).
     * 
     * @param arityIn
     *        arity of the tournament
     */
    public void setArity(final int arityIn) {
        this.arity = arityIn;
    }

}
