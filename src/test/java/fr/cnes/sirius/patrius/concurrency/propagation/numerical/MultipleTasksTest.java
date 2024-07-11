/**
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
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.cnes.sirius.patrius.tools.parallel.ParallelRunner;

/**
 * Eph Events Parallel and Extrapol Test.
 */
public class MultipleTasksTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ALL_PARALLEL
         * 
         * @featureDescription All tasks running in a multithreaded context
         * 
         * @coveredRequirements
         */
        ALL_PARALLEL
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#ALL_PARALLEL}
     * 
     * @testedMethod {@link EphEventsTaskFactory#EphEventsTaskFactory()}
     * 
     * @description Five instances of the EphEventTask run in parallel.
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
    public void allTest() throws InterruptedException {
        final ParallelRunner runner = new ParallelRunner(100);
        runner.addTask(new EphEventsTaskFactory(), 95);
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE1), 5);
        final boolean rez = runner.runAll();
        System.out.println(runner.getResultSummary());
        Assert.assertTrue(rez, runner.getResultSummary());
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#ALL_PARALLEL}
     * 
     * @testedMethod {@link EphEventsTaskFactory#EphEventsTaskFactory()}
     * 
     * @description Mix different tasks (3 extrapolation, one event search) and executes
     *              8 computations (2 each) on 4 different threads
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
    public void extrapolDifferentModesTest() throws InterruptedException {
        final ParallelRunner runner = new ParallelRunner(4);
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE3), 2);
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE2), 2);
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE1), 2);
        runner.addTask(new EphEventsTaskFactory(), 2);
        final boolean rez = runner.runAll();
        System.out.println(runner.getResultSummary());
        Assert.assertTrue(rez, runner.getResultSummary());
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#ALL_PARALLEL}
     * 
     * @testedMethod {@link EphEventsTaskFactory#EphEventsTaskFactory()}
     * 
     * @description Mix different tasks (2 extrapolations). Lesser variation of extrapolDifferentModesTest().
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
    public void extrapolDifferentModes2Test() throws InterruptedException {
        final ParallelRunner runner = new ParallelRunner(4);
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE3, 1), 1);
        runner.addTask(new ExtrapolTaskFactory(ExtrapolTask.Mode.MODE1, 1), 1);
        final boolean rez = runner.runAll();
        System.out.println(runner.getResultSummary());
        Assert.assertTrue(rez, runner.getResultSummary());
    }

}
