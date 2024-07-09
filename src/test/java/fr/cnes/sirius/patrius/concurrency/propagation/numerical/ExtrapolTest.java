/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * 
 */
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.tools.parallel.ParallelRunner;

/**
 * This tests propagations in a multithreading context.
 * Five instances of the ExtrapolTask run in parallel.
 * They are expected to produce exactly the same results,
 * and to take less time running in parallel than sequentially.
 */
public class ExtrapolTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EXTRAPOL_PARALLEL
         * 
         * @featureDescription Extrapolation running in a multithreaded context
         * 
         * @coveredRequirements
         */
        EXTRAPOL_PARALLEL
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#EXTRAPOL_PARALLEL}
     * 
     * @testedMethod {@link EphEventsTaskFactory#EphEventsTaskFactory()}
     * 
     * @description Five instances of the ExtrapolTask run in parallel.
     * 
     * @input misc
     * 
     * @output boolean
     * 
     * @testPassCriteria returns true
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws InterruptedException
     *         should not happen
     */
    @Test
    public void extrapolTest() throws InterruptedException {
        final ParallelRunner runner = new ParallelRunner();
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE1), 5);
        final boolean rez = runner.runAll();
        System.out.println(runner.getResultSummary());
        Assert.assertTrue(rez, runner.getResultSummary());
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#EXTRAPOL_PARALLEL}
     * 
     * @testedMethod {@link EphEventsTaskFactory#EphEventsTaskFactory()}
     * 
     * @description Five instances of the ExtrapolTask run in parallel, with a different date.
     * 
     * @input misc
     * 
     * @output boolean
     * 
     * @testPassCriteria returns true
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws InterruptedException
     *         should not happen
     */
    @Test
    public void extrapolMode2Test() throws InterruptedException {
        final ParallelRunner runner = new ParallelRunner();
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE2), 5);
        final boolean rez = runner.runAll();
        System.out.println(runner.getResultSummary());
        Assert.assertTrue(rez, runner.getResultSummary());
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#EXTRAPOL_PARALLEL}
     * 
     * @testedMethod {@link EphEventsTaskFactory#EphEventsTaskFactory()}
     * 
     * @description Five instances of the ExtrapolTask run in parallel, with different parameters.
     * 
     * @input misc
     * 
     * @output boolean
     * 
     * @testPassCriteria returns true
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws InterruptedException
     *         should not happen
     */
    @Test
    public void extrapolMode3Test() throws InterruptedException {
        final ParallelRunner runner = new ParallelRunner();
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE3), 5);
        final boolean rez = runner.runAll();
        System.out.println(runner.getResultSummary());
        Assert.assertTrue(rez, runner.getResultSummary());
    }
}
