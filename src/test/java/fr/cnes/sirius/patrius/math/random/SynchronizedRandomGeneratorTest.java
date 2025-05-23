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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

public class SynchronizedRandomGeneratorTest {
    private final int numberOfThreads = 5;
    private final int numberOfGenerators = 5;
    private final int numberOfSamples = 100000;

    @Test
    public void testAdapter() {
        final int seed = 12345;
        final RandomGenerator orig = new MersenneTwister(seed);
        final RandomGenerator wrap = new SynchronizedRandomGenerator(new MersenneTwister(seed));

        final int bSize = 67;
        final byte[] bOrig = new byte[bSize];
        final byte[] bWrap = new byte[bSize];

        for (int i = 0; i < 100; i++) {
            orig.nextBytes(bOrig);
            wrap.nextBytes(bWrap);
            for (int k = 0; k < bSize; k++) {
                Assert.assertEquals(bOrig[k], bWrap[k]);
            }

            Assert.assertEquals(orig.nextInt(), wrap.nextInt());

            final int range = (i + 1) * 89;
            Assert.assertEquals(orig.nextInt(range), wrap.nextInt(range));

            Assert.assertEquals(orig.nextLong(), wrap.nextLong());
            Assert.assertEquals(orig.nextBoolean(), wrap.nextBoolean());
            Assert.assertEquals(orig.nextFloat(), wrap.nextFloat(), 0);
            Assert.assertEquals(orig.nextDouble(), wrap.nextDouble(), 0);
            Assert.assertEquals(orig.nextGaussian(), wrap.nextGaussian(), 0);

        }
    }

    @Test
    public void testMath899Sync() throws Throwable {
        try {
            // Running the test several times in order to decrease the
            // probability that a non-thread-safe code did not trigger
            // a concurrency problem.
            for (int i = 0; i < 10; i++) {
                doTestMath899(true, this.numberOfThreads, this.numberOfGenerators, this.numberOfSamples);
            }
        } catch (final InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (final ExecutionException e) {
            throw e.getCause();
        }
    }

    /**
     * @param sync
     *        Whether to use a synchronizing wrapper.
     */
    private static double[] doTestMath899(final boolean sync, final int numThreads, final int numGenerators,
                                          final int numSamples) throws InterruptedException, ExecutionException {
        final RandomGenerator rng = new MersenneTwister();
        final RandomGenerator wrapper = sync ? new SynchronizedRandomGenerator(rng) : rng;

        final List<Callable<Double>> tasks = new ArrayList<>();
        for (int i = 0; i < numGenerators; i++) {
            tasks.add(new Callable<Double>(){
                @Override
                public Double call() {
                    Double lastValue = 0d;
                    for (int j = 0; j < numSamples; j++) {
                        lastValue = wrapper.nextGaussian();
                    }
                    return lastValue;
                }
            });
        }

        final ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        final List<Future<Double>> results = exec.invokeAll(tasks);

        final double[] values = new double[numGenerators];
        for (int i = 0; i < numGenerators; i++) {
            values[i] = results.get(i).get();
        }
        return values;
    }
}
