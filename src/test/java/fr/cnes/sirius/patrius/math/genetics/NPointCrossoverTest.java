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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;

public class NPointCrossoverTest {

    @Test(expected = DimensionMismatchException.class)
    public void testCrossoverDimensionMismatchException() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 0, 1, 0, 1, 1 };
        final Integer[] p2 = new Integer[] { 0, 1, 1, 0, 1 };

        final BinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final BinaryChromosome p2c = new DummyBinaryChromosome(p2);

        final CrossoverPolicy cp = new NPointCrossover<Integer>(1);
        cp.crossover(p1c, p2c);
    }

    @Test(expected = NumberIsTooLargeException.class)
    public void testNumberIsTooLargeException() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 0, 1, 0, 1, 1 };
        final Integer[] p2 = new Integer[] { 0, 1, 1, 0, 1, 0, 1, 1, 1 };

        final BinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final BinaryChromosome p2c = new DummyBinaryChromosome(p2);

        final CrossoverPolicy cp = new NPointCrossover<Integer>(15);
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

        final CrossoverPolicy cp = new NPointCrossover<Integer>(1);
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

        final CrossoverPolicy cp = new NPointCrossover<Integer>(1);
        cp.crossover(p1c, p2c);
    }

    @Test
    public void testCrossover() {
        final Integer[] p1 = new Integer[] { 1, 0, 1, 0, 1, 0, 1, 0, 1 };
        final Integer[] p2 = new Integer[] { 0, 1, 0, 1, 0, 1, 0, 1, 0 };

        final BinaryChromosome p1c = new DummyBinaryChromosome(p1);
        final BinaryChromosome p2c = new DummyBinaryChromosome(p2);

        final int order = 3;
        final NPointCrossover<Integer> npc = new NPointCrossover<Integer>(order);

        // the two parent chromosomes are different at each position, so it is easy to detect
        // the number of crossovers that happened for each child
        for (int i = 0; i < 20; i++) {
            final ChromosomePair pair = npc.crossover(p1c, p2c);

            Integer[] c1 = new Integer[p1.length];
            Integer[] c2 = new Integer[p2.length];

            c1 = ((BinaryChromosome) pair.getFirst()).getRepresentation().toArray(c1);
            c2 = ((BinaryChromosome) pair.getSecond()).getRepresentation().toArray(c2);

            Assert.assertEquals(order, this.detectCrossoverPoints(p1c, p2c, (BinaryChromosome) pair.getFirst()));
            Assert.assertEquals(order, this.detectCrossoverPoints(p2c, p1c, (BinaryChromosome) pair.getSecond()));
        }
    }

    private int detectCrossoverPoints(final BinaryChromosome p1, final BinaryChromosome p2, final BinaryChromosome c) {
        int crossovers = 0;
        final int length = p1.getLength();

        final List<Integer> p1Rep = p1.getRepresentation();
        final List<Integer> p2Rep = p2.getRepresentation();
        final List<Integer> cRep = c.getRepresentation();

        List<Integer> rep = p1Rep;
        for (int i = 0; i < length; i++) {
            if (rep.get(i) != cRep.get(i)) {
                crossovers++;
                rep = rep == p1Rep ? p2Rep : p1Rep;
            }
        }

        return crossovers;
    }

}
