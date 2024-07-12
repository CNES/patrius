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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

public class UniformCrossoverTest {
    private static final int LEN = 10000;
    private static final List<Integer> p1 = new ArrayList<>(LEN);
    private static final List<Integer> p2 = new ArrayList<>(LEN);

    @BeforeClass
    public static void setUpBeforeClass() {
        for (int i = 0; i < LEN; i++) {
            p1.add(0);
            p2.add(1);
        }
    }

    @Test(expected = OutOfRangeException.class)
    public void testRatioTooLow() {
        new UniformCrossover<Integer>(-0.5d);
    }

    @Test(expected = OutOfRangeException.class)
    public void testRatioTooHigh() {
        new UniformCrossover<Integer>(1.5d);
    }

    @Test
    public void testCrossover() {
        // test crossover with different ratios
        performCrossover(0.5);
        performCrossover(0.7);
        performCrossover(0.2);
    }

    private static void performCrossover(final double ratio) {
        final DummyBinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final DummyBinaryChromosome p2c = new DummyBinaryChromosome(p2);

        final CrossoverPolicy cp = new UniformCrossover<Integer>(ratio);

        // make a number of rounds
        for (int i = 0; i < 20; i++) {
            final ChromosomePair pair = cp.crossover(p1c, p2c);

            final List<Integer> c1 = ((DummyBinaryChromosome) pair.getFirst()).getRepresentation();
            final List<Integer> c2 = ((DummyBinaryChromosome) pair.getSecond()).getRepresentation();

            int from1 = 0;
            int from2 = 0;

            // check first child
            for (final int val : c1) {
                if (val == 0) {
                    from1++;
                } else {
                    from2++;
                }
            }

            Assert.assertEquals(1.0 - ratio, Double.valueOf((double) from1 / LEN), 0.1);
            Assert.assertEquals(ratio, Double.valueOf((double) from2 / LEN), 0.1);

            from1 = 0;
            from2 = 0;

            // check second child
            for (final int val : c2) {
                if (val == 0) {
                    from1++;
                } else {
                    from2++;
                }
            }

            Assert.assertEquals(ratio, Double.valueOf((double) from1 / LEN), 0.1);
            Assert.assertEquals(1.0 - ratio, Double.valueOf((double) from2 / LEN), 0.1);
        }
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCrossoverDimensionMismatchException() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 0, 1, 0, 1, 1 };
        final Integer[] p2 = new Integer[] { 0, 1, 1, 0, 1 };

        final BinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final BinaryChromosome p2c = new DummyBinaryChromosome(p2);

        final CrossoverPolicy cp = new UniformCrossover<Integer>(0.5d);
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

        final CrossoverPolicy cp = new UniformCrossover<Integer>(0.5d);
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

        final CrossoverPolicy cp = new UniformCrossover<Integer>(0.5d);
        cp.crossover(p1c, p2c);
    }
}
