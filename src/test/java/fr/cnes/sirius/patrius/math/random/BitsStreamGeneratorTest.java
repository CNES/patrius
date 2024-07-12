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
package fr.cnes.sirius.patrius.math.random;

import java.util.Random;

/**
 * Test cases for the BitStreamGenerator class
 * 
 * @version $Id: BitsStreamGeneratorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class BitsStreamGeneratorTest extends RandomGeneratorAbstractTest {

    public BitsStreamGeneratorTest() {
        super();
    }

    @Override
    protected RandomGenerator makeGenerator() {
        final RandomGenerator generator = new TestBitStreamGenerator();
        generator.setSeed(1000);
        return generator;
    }

    /**
     * Test BitStreamGenerator using a Random as bit source.
     */
    static class TestBitStreamGenerator extends BitsStreamGenerator {

        /** Serializable UID. */
        private static final long serialVersionUID = 5755603601456614286L;
        private final BitRandom ran = new BitRandom();

        @Override
        public void setSeed(final int seed) {
            this.ran.setSeed(seed);
            this.clear();
        }

        @Override
        public void setSeed(final int[] seed) {
            this.ran.setSeed(seed[0]);
        }

        @Override
        public void setSeed(final long seed) {
            this.ran.setSeed((int) seed);

        }

        @Override
        protected int next(final int bits) {
            return this.ran.nextBits(bits);
        }
    }

    /**
     * Extend Random to expose next(bits)
     */
    @SuppressWarnings("serial")
    static class BitRandom extends Random {
        /** Serializable UID. */
        private static final long serialVersionUID = 495280235629774336L;

        public BitRandom() {
            super();
        }

        public int nextBits(final int bits) {
            return this.next(bits);
        }
    }
}
