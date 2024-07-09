/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
        public BitRandom() {
            super();
        }

        public int nextBits(final int bits) {
            return this.next(bits);
        }
    }

}
