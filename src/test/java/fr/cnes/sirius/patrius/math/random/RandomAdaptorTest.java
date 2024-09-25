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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the RandomAdaptor class
 * 
 * @version $Id: RandomAdaptorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public class RandomAdaptorTest {

    @Test
    public void testAdaptor() {
        final ConstantGenerator generator = new ConstantGenerator();
        final Random random = RandomAdaptor.createAdaptor(generator);
        this.checkConstant(random);
        final RandomAdaptor randomAdaptor = new RandomAdaptor(generator);
        this.checkConstant(randomAdaptor);
    }

    private void checkConstant(final Random random) {
        final byte[] bytes = new byte[] { 0 };
        random.nextBytes(bytes);
        Assert.assertEquals(0, bytes[0]);
        Assert.assertEquals(false, random.nextBoolean());
        Assert.assertEquals(0, random.nextDouble(), 0);
        Assert.assertEquals(0, random.nextFloat(), 0);
        Assert.assertEquals(0, random.nextGaussian(), 0);
        Assert.assertEquals(0, random.nextInt());
        Assert.assertEquals(0, random.nextInt(1));
        Assert.assertEquals(0, random.nextLong());
        random.setSeed(100);
        Assert.assertEquals(0, random.nextDouble(), 0);
    }

    /*
     * "Constant" generator to test Adaptor delegation.
     * "Powered by Eclipse ;-)"
     */
    public static class ConstantGenerator implements RandomGenerator {

        private final double value;

        public ConstantGenerator() {
            this.value = 0;
        }

        public ConstantGenerator(final double value) {
            this.value = value;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public void nextBytes(final byte[] bytes) {
        }

        @Override
        public double nextDouble() {
            return this.value;
        }

        @Override
        public float nextFloat() {
            return (float) this.value;
        }

        @Override
        public double nextGaussian() {
            return this.value;
        }

        @Override
        public int nextInt() {
            return (int) this.value;
        }

        @Override
        public int nextInt(final int n) {
            return (int) this.value;
        }

        @Override
        public long nextLong() {
            return (int) this.value;
        }

        @Override
        public void setSeed(final int seed) {
        }

        @Override
        public void setSeed(final int[] seed) {
        }

        @Override
        public void setSeed(final long seed) {
        }

    }
}
