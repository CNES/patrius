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
package fr.cnes.sirius.patrius.math.analysis.integration.gauss;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Pair;

/**
 * Test for {@link BaseRuleFactory}.
 * 
 * @version $Id: BaseRuleFactoryTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class BaseRuleFactoryTest {
    /**
     * Tests that a given rule rule will be computed and added once to the cache
     * whatever the number of times this rule is called concurrently.
     */
    @Test
    public void testConcurrentCreation() throws InterruptedException,
                                        ExecutionException {
        // Number of times the same rule will be called.
        final int numTasks = 20;

        final ThreadPoolExecutor exec = new ThreadPoolExecutor(3, numTasks, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(2));

        final List<Future<Pair<double[], double[]>>> results = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            results.add(exec.submit(new RuleBuilder()));
        }

        // Ensure that all computations have completed.
        for (final Future<Pair<double[], double[]>> f : results) {
            f.get();
        }

        // Assertion would fail if "getRuleInternal" were not "synchronized".
        final int n = RuleBuilder.getNumberOfCalls();
        Assert.assertEquals("Rule computation was called " + n + " times", 1, n);
    }
}

class RuleBuilder implements Callable<Pair<double[], double[]>> {
    private static final DummyRuleFactory factory = new DummyRuleFactory();

    @Override
    public Pair<double[], double[]> call() {
        final int dummy = 2; // Always request the same rule.
        return factory.getRule(dummy);
    }

    public static int getNumberOfCalls() {
        return factory.getNumberOfCalls();
    }
}

class DummyRuleFactory extends BaseRuleFactory<Double> {
    /** Rule computations counter. */
    private static AtomicInteger nCalls = new AtomicInteger();

    @Override
    protected Pair<Double[], Double[]> computeRule(final int order) {
        // Tracks whether this computation has been called more than once.
        nCalls.getAndIncrement();

        try {
            // Sleep to simulate computation time.
            Thread.sleep(20);
        } catch (final InterruptedException e) {
            Assert.fail("Unexpected interruption");
        }

        // Dummy rule (but contents must exist).
        final Double[] p = new Double[order];
        final Double[] w = new Double[order];
        for (int i = 0; i < order; i++) {
            p[i] = new Double(i);
            w[i] = new Double(i);
        }
        return new Pair<>(p, w);
    }

    public int getNumberOfCalls() {
        return nCalls.get();
    }
}
