/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history creation 18/10/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * Benchmark feature test class.
 * 
 * @author bonitt
 */
public class TimeItTest {

    /** Sleep duration for X function [s]. */
    private final static double SLEEP_X = 0.01;

    /** Sleep duration for Y function [s]. */
    private final static double SLEEP_Y = 0.1;

    /** Seconds to milliseconds factor. */
    private final static double SEC_TO_MILLISEC = 1e3;

    // /** Seconds to microseconds factor. */
    // private final static double SEC_TO_MICROSEC = 1e6;

    /** Seconds to nanoseconds factor. */
    private final static double SEC_TO_NANOSEC = 1e9;

    /**
     * @description Test the benchmark analysis main values output
     * 
     * @testedMethod {@link TimeIt#TimeIt(Runnable)}
     * @testedMethod {@link TimeIt#TimeIt(Runnable, long, int, long)}
     * @testedMethod {@link TimeIt#TimeIt(Runnable, long, int, boolean)}
     * @testedMethod {@link TimeIt#getMeanTime()}
     * @testedMethod {@link TimeIt#getMinTime()}
     * @testedMethod {@link TimeIt#getMaxTime()}
     * @testedMethod {@link TimeIt#getStandardDeviationTime()}
     * @testedMethod {@link TimeIt#getTimes()}
     * @testedMethod {@link TimeIt#loopsPerSecondEstimator(Runnable)}
     * 
     * @testPassCriteria The benchmark analysis produce the expected values (inside a acceptance bound)
     */
    @Test
    public void timeItTest() {

        // Initialize the functions to evaluate
        final Runnable fctX = new Function((long) (SLEEP_X * SEC_TO_NANOSEC));
        final Runnable fctY = new Function((long) (SLEEP_Y * SEC_TO_NANOSEC));

        // Run the benchmarks
        final TimeIt process1 = new TimeIt(fctX);
        final TimeIt process2 = new TimeIt(fctY, 20, 3, true);

        // Evaluate the mean, min, max times consistency between themselves
        Assert.assertTrue(0. < process1.getMinTime());
        Assert.assertTrue(process1.getMinTime() < process1.getMeanTime());
        Assert.assertTrue(process1.getMeanTime() < process1.getMaxTime());

        Assert.assertTrue(0. < process2.getMinTime());
        Assert.assertTrue(process2.getMinTime() < process2.getMeanTime());
        Assert.assertTrue(process2.getMeanTime() < process2.getMaxTime());

        final double[] times1 = process1.getTimes();
        final double[] times2 = process2.getTimes();

        // The expected arrays length for the 1st process is the default number of runs which should be 7
        Assert.assertEquals(7, times1.length);
        for (int i = 0; i < times1.length; i++) {
            Assert.assertTrue(process1.getMinTime() <= times1[i]);
            Assert.assertTrue(times1[i] <= process1.getMaxTime());
        }

        // The second is expected to be 3
        Assert.assertEquals(3, times2.length);
        for (int i = 0; i < times2.length; i++) {
            Assert.assertTrue(process2.getMinTime() <= times2[i]);
            Assert.assertTrue(times2[i] <= process2.getMaxTime());
        }

        // Evaluate the standard deviation time: should be close to 0 as the function duration is supposed to be
        // constant (threshold defined to be valid)
        Assert.assertEquals(0., process1.getStandardDeviationTime(), 1e-2);
        Assert.assertEquals(0., process2.getStandardDeviationTime(), 1e-2);

        // Evaluate the loops per second estimators
        final double loopsX = TimeIt.loopsPerSecondEstimator(fctX);
        Assert.assertTrue(0. < loopsX);
        Assert.assertTrue(loopsX <= 1. / SLEEP_X);

        final double loopsY = TimeIt.loopsPerSecondEstimator(fctY);
        Assert.assertTrue(0. < loopsY);
        Assert.assertTrue(loopsY <= 1. / SLEEP_Y);
    }

    /**
     * @description Test the String representation of a benchmark analysis
     * 
     * @testedMethod {@link TimeIt#toString()}
     * 
     * @testPassCriteria The benchmark analysis produce the expected String representations
     *                   Note: we can't check the duration values which can change from one execution to an other
     */
    @Test
    public void testToString() {

        // Initialize the functions to evaluate
        final Runnable fctSec = new Function((long) (1.1 * SEC_TO_NANOSEC));
        final Runnable fctMilliSec = new Function((long) (1.1 * SEC_TO_MILLISEC));
        // final Runnable fctMicroSec = new Function((long) (1.1 * SEC_TO_MICROSEC));
        // final Runnable fctNanoSec = new Function(200L);

        final long nbLoops = 3L;
        final int nbRuns = 2;
        final boolean warmup = false;

        // Run the benchmarks
        final TimeIt processSec = new TimeIt(fctSec, nbLoops, nbRuns, warmup);
        final TimeIt processMilliSec = new TimeIt(fctMilliSec, nbLoops, nbRuns, warmup);
        // final TimeIt processMicroSec = new TimeIt(fctMicroSec, nbLoops, nbRuns, warmup);
        // final TimeIt processNanoSec = new TimeIt(fctNanoSec, nbLoops, nbRuns, warmup);

        // Evaluate the toString composition
        // Note: we can't check the duration values which can change from one execution to an other
        String txt;

        txt = processSec.toString();
        Assert.assertTrue(txt.contains(" ± ") && txt.contains(" s (min=") && txt.contains("s; max=")
                && txt.contains("s) per loop (2 runs, 3 loops each)"));

        txt = processMilliSec.toString();
        Assert.assertTrue(txt.contains(" ± ") && txt.contains(" ms (min=") && txt.contains("ms; max=")
                && txt.contains("ms) per loop (2 runs, 3 loops each)"));

        // Note: the function can't run as quick as micro/nano seconds
        // txt = processMicroSec.toString();
        // Assert.assertTrue(txt.contains(" ± ") && txt.contains(" µs (min=") && txt.contains("µs; max=")
        // && txt.contains("µs) per loop (2 runs, 3 loops each)"));
        //
        // txt = processNanoSec.toString();
        // Assert.assertTrue(txt.contains(" ± ") && txt.contains(" ns (min=") && txt.contains("ns; max=")
        // && txt.contains("ns) per loop (2 runs, 3 loops each)"));

    }

    /** Runnable function used for benchmark evaluation. */
    private class Function implements Runnable {

        /** Sleep duration for the function [ns]. */
        private final long sleepDuration;

        /**
         * Constructor.
         * 
         * @param sleepDuration
         *        Sleep duration for the function [ns]
         */
        public Function(final long sleepDuration) {
            this.sleepDuration = sleepDuration;
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            try {
                TimeUnit.NANOSECONDS.sleep(this.sleepDuration);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
