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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import org.junit.Assert;
import org.junit.Test;

public class TournamentSelectionTest {

    private static int counter = 0;

    @Test
    public void testSelect() {
        final TournamentSelection ts = new TournamentSelection(2);
        final ElitisticListPopulation pop = new ElitisticListPopulation(100, 0.203);

        for (int i = 0; i < pop.getPopulationLimit(); i++) {
            pop.addChromosome(new DummyChromosome());
        }
        // how to write a test for stochastic method?
        for (int i = 0; i < 20; i++) {
            final ChromosomePair pair = ts.select(pop);
            // the worst chromosome should NEVER be selected
            Assert.assertTrue(pair.getFirst().getFitness() > 0);
            Assert.assertTrue(pair.getSecond().getFitness() > 0);
        }
    }

    private static class DummyChromosome extends Chromosome {
        private final int fitness;

        public DummyChromosome() {
            this.fitness = counter;
            counter++;
        }

        @Override
        public double fitness() {
            return this.fitness;
        }
    }

}
