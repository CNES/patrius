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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ChromosomeTest {

    @Test
    public void testCompareTo() {
        final Chromosome c1 = new Chromosome(){
            @Override
            public double fitness() {
                return 0;
            }
        };
        final Chromosome c2 = new Chromosome(){
            @Override
            public double fitness() {
                return 10;
            }
        };
        final Chromosome c3 = new Chromosome(){
            @Override
            public double fitness() {
                return 10;
            }
        };

        Assert.assertTrue(c1.compareTo(c2) < 0);
        Assert.assertTrue(c2.compareTo(c1) > 0);
        Assert.assertEquals(0, c3.compareTo(c2));
        Assert.assertEquals(0, c2.compareTo(c3));
    }

    private abstract static class DummyChromosome extends Chromosome {
        private final int repr;

        public DummyChromosome(final int repr) {
            this.repr = repr;
        }

        @Override
        protected boolean isSame(final Chromosome another) {
            return ((DummyChromosome) another).repr == this.repr;
        }
    }

    @Test
    public void testFindSameChromosome() {
        final Chromosome c1 = new DummyChromosome(1){
            @Override
            public double fitness() {
                return 1;
            }
        };
        final Chromosome c2 = new DummyChromosome(2){
            @Override
            public double fitness() {
                return 2;
            }
        };
        final Chromosome c3 = new DummyChromosome(3){
            @Override
            public double fitness() {
                return 3;
            }
        };
        final Chromosome c4 = new DummyChromosome(1){
            @Override
            public double fitness() {
                return 5;
            }
        };
        final Chromosome c5 = new DummyChromosome(15){
            @Override
            public double fitness() {
                return 15;
            }
        };

        final List<Chromosome> popChr = new ArrayList<Chromosome>();
        popChr.add(c1);
        popChr.add(c2);
        popChr.add(c3);
        final Population pop = new ListPopulation(popChr, 3){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        Assert.assertNull(c5.findSameChromosome(pop));
        Assert.assertEquals(c1, c4.findSameChromosome(pop));

        c4.searchForFitnessUpdate(pop);
        Assert.assertEquals(1, c4.getFitness(), 0);
    }

}
