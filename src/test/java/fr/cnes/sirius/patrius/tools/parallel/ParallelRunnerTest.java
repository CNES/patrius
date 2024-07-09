/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
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
package fr.cnes.sirius.patrius.tools.parallel;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for ParallelRunner.
 * 
 * @author cardosop
 * 
 * @version $Id: ParallelRunnerTest.java 17915 2017-09-11 12:35:44Z bignon $
 * 
 * @since 2.0
 * 
 */
public class ParallelRunnerTest {
    /** Features description. */
    enum features {
        /**
         * @featureTitle ParallelRunner test
         * 
         * @featureDescription ParallelRunner test
         * 
         * @coveredRequirements NA
         */
        PARALLEL_RUNNER
    }

    /**
     * Nominal run test.
     * 
     * @throws InterruptedException
     *         in case of thread pool interruption
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#addTask(ParallelTaskFactory, int)}
     * @testedMethod {@link ParallelRunner#runAll()}
     * @testedMethod {@link ParallelRunner#getResultSummary()}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void nominalRunTest() throws InterruptedException {
        // Instantiate the tasks runner (entirely generic)
        final ParallelRunner runner = new ParallelRunner(5);
        // Add task factories to the runner
        runner.addTask(new TaskOneFactory(), 20);
        runner.addTask(new TaskTwoFactory(), 20);
        // Launch all tasks
        final boolean result = runner.runAll();
        // Result assertion
        Assert.assertTrue(runner.getResultSummary(), result);
        // Print result summary if all is OK.
        System.out.println(runner.getResultSummary());
    }

    /**
     * Update run test.
     * 
     * @throws InterruptedException
     *         in case of thread pool interruption
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#addTask(ParallelTaskFactory, int)}
     * @testedMethod {@link ParallelRunner#runAll()}
     * @testedMethod {@link ParallelRunner#getResultSummary()}
     * @testedMethod {@link ParallelRunner#resizeThreadPool(int)}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void updateRunTest() throws InterruptedException {
        // Instantiate the tasks runner (entirely generic)
        final ParallelRunner runner = new ParallelRunner(5);
        // Add task factories to the runner
        final TaskOneFactory tf1 = new TaskOneFactory();
        final TaskTwoFactory antenne2 = new TaskTwoFactory();
        runner.addTask(tf1, 5);
        runner.addTask(antenne2, 5);
        // Launch all tasks
        System.out.println("---RUN ONE---");
        boolean result = runner.runAll();
        // Result assertion
        Assert.assertTrue(runner.getResultSummary(), result);
        // Print result summary if all is OK.
        System.out.println(runner.getResultSummary());
        // Update the number of threads
        runner.resizeThreadPool(8);
        // Update the number of runs
        runner.addTask(tf1, 6);
        runner.addTask(antenne2, 6);
        // Relaunch all tasks
        System.out.println("---RUN TWO---");
        result = runner.runAll();
        // Result assertion
        Assert.assertTrue(runner.getResultSummary(), result);
        // Print result summary if all is OK.
        System.out.println(runner.getResultSummary());

    }

    /**
     * Wrong results test.
     * 
     * @throws InterruptedException
     *         in case of thread pool interruption
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#addTask(ParallelTaskFactory, int)}
     * @testedMethod {@link ParallelRunner#runAll()}
     * @testedMethod {@link ParallelRunner#getResultSummary()}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void wrongResultsTest() throws InterruptedException {
        // Instantiate the tasks runner (entirely generic)
        final ParallelRunner runner = new ParallelRunner(5);
        // Add task factories to the runner
        runner.addTask(new BadRezTaskOneFactory(), 20);
        runner.addTask(new TaskTwoFactory(), 20);
        // Launch all tasks
        final boolean result = runner.runAll();
        // Result assertion - should fail
        Assert.assertFalse(result);
        // Print result summary
        System.out.println(runner.getResultSummary());
    }

    /**
     * Time out test.
     * 
     * @throws InterruptedException
     *         in case of thread pool interruption
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#addTask(ParallelTaskFactory, int)}
     * @testedMethod {@link ParallelRunner#runAll()}
     * @testedMethod {@link ParallelRunner#getResultSummary()}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void timeoutTest() throws InterruptedException {
        // Instantiate the tasks runner (entirely generic)
        final ParallelRunner runner = new ParallelRunner(5);
        // Add task factories to the runner
        runner.addTask(new TaskOneFactory(), 20);
        runner.addTask(new TimeoutTaskTwoFactory(), 20);
        // Launch all tasks
        final boolean result = runner.runAll();
        // Print result summary
        System.out.println(runner.getResultSummary());
        // Result assertion - should fail
        Assert.assertFalse(result);
    }

    /**
     * Constructors test.
     * Test method for {@link fr.cnes.sirius.patrius.tools.parallel.ParallelRunner#ParallelRunner()} and
     * {@link fr.cnes.sirius.patrius.tools.parallel.ParallelRunner#ParallelRunner(int)}.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#ParallelRunner()}
     * @testedMethod {@link ParallelRunner#ParallelRunner(int)}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void constructorsTest() {
        // Instantiate the tasks runners
        final ParallelRunner runner1 = new ParallelRunner();
        final ParallelRunner runner2 = new ParallelRunner(435);
        final ParallelRunner runner3 = new ParallelRunner(-6);
        Assert.assertNotNull(runner1);
        Assert.assertNotNull(runner2);
        Assert.assertNotNull(runner3);
    }

    /**
     * Error test.
     * 
     * @throws InterruptedException
     *         in case of thread pool interruption
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#ParallelRunner()}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = ParallelException.class)
    public void errorOneTest() throws InterruptedException {
        // Instantiate the tasks runner (entirely generic)
        final ParallelRunner runner = new ParallelRunner();
        // Nothing to run --> exception
        runner.runAll();
    }

    /**
     * Error test.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#PARALLEL_RUNNER}
     * 
     * @testedMethod {@link ParallelRunner#ParallelRunner()}
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = ParallelException.class)
    public void errorTwoTest() {
        // Instantiate the tasks runner (entirely generic)
        final ParallelRunner runner = new ParallelRunner();
        // Null factory --> exception
        runner.addTask(null, 10);
    }

}
