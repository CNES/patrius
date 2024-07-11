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
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;

public class ListPopulationTest {

    @Test
    public void testGetFittestChromosome() {
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
                return 15;
            }
        };

        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(c1);
        chromosomes.add(c2);
        chromosomes.add(c3);

        final ListPopulation population = new ListPopulation(chromosomes, 10){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        Assert.assertEquals(c3, population.getFittestChromosome());
    }

    @Test
    public void testChromosomes() {
        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));

        final ListPopulation population = new ListPopulation(10){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        population.addChromosomes(chromosomes);

        Assert.assertEquals(chromosomes, population.getChromosomes());
        Assert.assertEquals(chromosomes.toString(), population.toString());

        population.setPopulationLimit(50);
        Assert.assertEquals(50, population.getPopulationLimit());
    }

    @Test(expected = NotPositiveException.class)
    public void testSetPopulationLimit() {
        final ListPopulation population = new ListPopulation(10){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        population.setPopulationLimit(-50);
    }

    @Test(expected = NotPositiveException.class)
    public void testConstructorPopulationLimitNotPositive() {
        new ListPopulation(-10){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };
    }

    @Test(expected = NotPositiveException.class)
    public void testChromosomeListConstructorPopulationLimitNotPositive() {
        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        new ListPopulation(chromosomes, -10){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };
    }

    @Test(expected = NumberIsTooLargeException.class)
    public void testConstructorListOfChromosomesBiggerThanPopulationSize() {
        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        new ListPopulation(chromosomes, 1){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };
    }

    @Test(expected = NumberIsTooLargeException.class)
    public void testAddTooManyChromosomes() {
        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));

        final ListPopulation population = new ListPopulation(2){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        population.addChromosomes(chromosomes);
    }

    @Test(expected = NumberIsTooLargeException.class)
    public void testAddTooManyChromosomesSingleCall() {

        final ListPopulation population = new ListPopulation(2){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        for (int i = 0; i <= population.getPopulationLimit(); i++) {
            population.addChromosome(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIterator() {
        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));

        final ListPopulation population = new ListPopulation(10){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        population.addChromosomes(chromosomes);

        final Iterator<Chromosome> iter = population.iterator();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testSetPopulationLimitTooSmall() {
        final ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));
        chromosomes.add(new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(3)));

        final ListPopulation population = new ListPopulation(chromosomes, 3){
            @Override
            public Population nextGeneration() {
                // not important
                return null;
            }
        };

        population.setPopulationLimit(2);
    }

}
