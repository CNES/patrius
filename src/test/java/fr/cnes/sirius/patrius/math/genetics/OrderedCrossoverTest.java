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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;

public class OrderedCrossoverTest {

    @Test
    public void testCrossover() {
        final Integer[] p1 = new Integer[] { 8, 4, 7, 3, 6, 2, 5, 1, 9, 0 };
        final Integer[] p2 = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final DummyListChromosome p1c = new DummyListChromosome(p1);
        final DummyListChromosome p2c = new DummyListChromosome(p2);

        final CrossoverPolicy cp = new OrderedCrossover<Integer>();

        for (int i = 0; i < 20; i++) {
            final Set<Integer> parentSet1 = new HashSet<>(Arrays.asList(p1));
            final Set<Integer> parentSet2 = new HashSet<>(Arrays.asList(p2));

            final ChromosomePair pair = cp.crossover(p1c, p2c);

            final Integer[] c1 = ((DummyListChromosome) pair.getFirst()).getRepresentation().toArray(
                new Integer[p1.length]);
            final Integer[] c2 = ((DummyListChromosome) pair.getSecond()).getRepresentation().toArray(
                new Integer[p2.length]);

            Assert.assertNotSame(p1c, pair.getFirst());
            Assert.assertNotSame(p2c, pair.getSecond());

            // make sure that the children have exactly the same elements as their parents
            for (int j = 0; j < c1.length; j++) {
                Assert.assertTrue(parentSet1.contains(c1[j]));
                parentSet1.remove(c1[j]);
                Assert.assertTrue(parentSet2.contains(c2[j]));
                parentSet2.remove(c2[j]);
            }
        }
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCrossoverDimensionMismatchException() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 0, 1, 0, 1, 1 };
        final Integer[] p2 = new Integer[] { 0, 1, 1, 0, 1 };

        final BinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final BinaryChromosome p2c = new DummyBinaryChromosome(p2);

        final CrossoverPolicy cp = new OrderedCrossover<Integer>();
        cp.crossover(p1c, p2c);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testCrossoverInvalidFixedLengthChromosomeFirst() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 0, 1, 0, 1, 1 };
        final BinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final Chromosome p2c = new Chromosome(){
            @Override
            public double fitness() {
                // Not important
                return 0;
            }
        };

        final CrossoverPolicy cp = new OrderedCrossover<Integer>();
        cp.crossover(p1c, p2c);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testCrossoverInvalidFixedLengthChromosomeSecond() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 0, 1, 0, 1, 1 };
        final BinaryChromosome p2c = new DummyBinaryChromosome(p1);
        final Chromosome p1c = new Chromosome(){
            @Override
            public double fitness() {
                // Not important
                return 0;
            }
        };

        final CrossoverPolicy cp = new OrderedCrossover<Integer>();
        cp.crossover(p1c, p2c);
    }
}
