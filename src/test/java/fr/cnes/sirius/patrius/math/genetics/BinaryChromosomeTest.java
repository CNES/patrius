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

import org.junit.Assert;
import org.junit.Test;

public class BinaryChromosomeTest {

    @Test
    public void testInvalidConstructor() {
        final Integer[][] reprs = new Integer[][] {
            new Integer[] { 0, 1, 0, 1, 2 },
            new Integer[] { 0, 1, 0, 1, -1 }
        };

        for (final Integer[] repr : reprs) {
            try {
                new DummyBinaryChromosome(repr);
                Assert.fail("Exception not caught");
            } catch (final IllegalArgumentException e) {
                // Expected
            }
        }
    }

    @Test
    public void testRandomConstructor() {
        for (int i = 0; i < 20; i++) {
            new DummyBinaryChromosome(BinaryChromosome.randomBinaryRepresentation(10));
        }
    }

    @Test
    public void testIsSame() {
        final Chromosome c1 = new DummyBinaryChromosome(new Integer[] { 0, 1, 0, 1, 0, 1 });
        final Chromosome c2 = new DummyBinaryChromosome(new Integer[] { 0, 1, 1, 0, 1 });
        final Chromosome c3 = new DummyBinaryChromosome(new Integer[] { 0, 1, 0, 1, 0, 1, 1 });
        final Chromosome c4 = new DummyBinaryChromosome(new Integer[] { 1, 1, 0, 1, 0, 1 });
        final Chromosome c5 = new DummyBinaryChromosome(new Integer[] { 0, 1, 0, 1, 0, 0 });
        final Chromosome c6 = new DummyBinaryChromosome(new Integer[] { 0, 1, 0, 1, 0, 1 });

        Assert.assertFalse(c1.isSame(c2));
        Assert.assertFalse(c1.isSame(c3));
        Assert.assertFalse(c1.isSame(c4));
        Assert.assertFalse(c1.isSame(c5));
        Assert.assertTrue(c1.isSame(c6));
    }

}
