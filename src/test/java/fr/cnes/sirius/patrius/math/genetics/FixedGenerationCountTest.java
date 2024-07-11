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

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class FixedGenerationCountTest {

    @Test
    public void testIsSatisfied() {
        final FixedGenerationCount fgc = new FixedGenerationCount(20);

        int cnt = 0;
        final Population pop = new Population(){
            @Override
            public void addChromosome(final Chromosome chromosome) {
                // unimportant
            }

            @Override
            public Chromosome getFittestChromosome() {
                // unimportant
                return null;
            }

            @Override
            public int getPopulationLimit() {
                // unimportant
                return 0;
            }

            @Override
            public int getPopulationSize() {
                // unimportant
                return 0;
            }

            @Override
            public Population nextGeneration() {
                // unimportant
                return null;
            }

            @Override
            public Iterator<Chromosome> iterator() {
                // unimportant
                return null;
            }
        };

        while (!fgc.isSatisfied(pop)) {
            cnt++;
        }
        Assert.assertEquals(20, cnt);
    }

}
